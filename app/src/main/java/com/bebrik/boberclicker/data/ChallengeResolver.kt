package com.bebrik.boberclicker.data

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.net.CookieManager
import java.net.HttpCookie
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Решает JS/AES challenge хостинга через невидимый WebView.
 *
 * Поток:
 *  1. Показываем оверлей "Проверка соединения…"
 *  2. Невидимый WebView загружает BASE URL
 *  3. JS на странице расшифровывает AES, ставит куку _test, делает GET-редирект на ?i=1
 *  4. Мы перехватываем onPageFinished с url=?i=1 → даём 400мс на flush cookies
 *  5. Переносим ВСЕ куки из WebKit CookieManager в java.net.CookieManager
 *  6. Закрываем оверлей, разблокируем фоновый поток
 */
object ChallengeResolver {
    private const val TAG      = "BoberChallenge"
    private const val DOMAIN   = "bober-api.gt.tc"
    private const val BASE_URL = "https://$DOMAIN"
    private const val TIMEOUT_MS = 12_000L
    private const val POLL_MS    = 300L

    /**
     * Вызывается из фонового потока. Блокирует до завершения.
     * Возвращает true если кука _test успешно получена.
     */
    fun resolve(
        context: Context,
        javaCookieManager: CookieManager,
        challengeUrl: String = BASE_URL
    ): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.e(TAG, "resolve() must not be called on main thread")
            return false
        }
        val latch   = CountDownLatch(1)
        var success = false
        Handler(Looper.getMainLooper()).post {
            showOverlay(context, javaCookieManager, challengeUrl) { ok ->
                success = ok
                latch.countDown()
            }
        }
        latch.await(TIMEOUT_MS + 3000, TimeUnit.MILLISECONDS)
        Log.d(TAG, "resolve done: success=$success")
        return success
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showOverlay(
        context: Context,
        javaCookieManager: CookieManager,
        challengeUrl: String,
        onDone: (Boolean) -> Unit
    ) {
        val activity = context as? Activity ?: run { onDone(false); return }

        // ── Overlay UI ─────────────────────────────────────────
        val overlay = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#CC0A0618"))
        }

        val card = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(dp(activity, 300), dp(activity, 170), Gravity.CENTER)
            setBackgroundColor(Color.parseColor("#1A1040"))
        }
        card.addView(TextView(activity).apply {
            text = "🔐 Проверка соединения…"
            textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).also { it.topMargin = dp(activity, 24) }
        })
        card.addView(ProgressBar(activity).apply {
            isIndeterminate = true
            layoutParams = FrameLayout.LayoutParams(dp(activity, 48), dp(activity, 48), Gravity.CENTER)
        })
        card.addView(TextView(activity).apply {
            text = "Обычно 1–3 секунды"
            textSize = 12f; setTextColor(Color.parseColor("#88AABB")); gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).also { it.bottomMargin = dp(activity, 20) }
        })
        overlay.addView(card)

        // ── Invisible WebView ───────────────────────────────────
        val webView = WebView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(1, 1)
            visibility = android.view.View.INVISIBLE
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36 BoberApp/4.0"
        }
        // Убедимся что WebKit CookieManager включён
        android.webkit.CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        overlay.addView(webView)

        val decor = activity.window.decorView as? FrameLayout ?: run { onDone(false); return }
        decor.addView(overlay)

        // ── State ───────────────────────────────────────────────
        val mainHandler   = Handler(Looper.getMainLooper())
        var done          = false
        var elapsedMs     = 0L
        var seenFinalPage = false // видели ли редирект ?i=1

        fun finish(ok: Boolean) {
            if (done) return
            done = true
            // Flush WebKit cookie store
            android.webkit.CookieManager.getInstance().flush()
            val rawCookies = android.webkit.CookieManager.getInstance().getCookie(BASE_URL) ?: ""
            Log.d(TAG, "finish ok=$ok rawCookies=$rawCookies")
            val hasTest = rawCookies.contains("_test=")
            if (hasTest) transferCookies(rawCookies, javaCookieManager)
            decor.removeView(overlay)
            webView.stopLoading(); webView.destroy()
            onDone(ok && hasTest)
        }

        // Polling — запасной на случай если ?i=1 уже был в кеше и onPageFinished не сработал
        val pollRunnable = object : Runnable {
            override fun run() {
                if (done) return
                elapsedMs += POLL_MS
                val raw = android.webkit.CookieManager.getInstance().getCookie(BASE_URL) ?: ""
                Log.d(TAG, "poll ${elapsedMs}ms seenFinal=$seenFinalPage cookies=${raw.take(60)}")
                when {
                    seenFinalPage && raw.contains("_test=") -> finish(true)
                    elapsedMs >= TIMEOUT_MS                  -> finish(raw.contains("_test="))
                    else                                     -> mainHandler.postDelayed(this, POLL_MS)
                }
            }
        }

        // ── WebViewClient ────────────────────────────────────────
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                Log.d(TAG, "page=$url")
                if (done) return
                if (url.contains("i=1")) {
                    // Финальный редирект — сервер принял куку
                    seenFinalPage = true
                    // Дополнительные 500мс чтобы WebKit записал куки на диск
                    mainHandler.postDelayed({ finish(true) }, 500)
                } else {
                    // Первая страница загружена — начинаем polling
                    mainHandler.postDelayed(pollRunnable, POLL_MS)
                }
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (!request.isForMainFrame || done) return
                Log.e(TAG, "WebView error url=${request.url}: ${error.description}")
                // Если ошибка пришла уже после ?i=1 — не страшно, кука есть
                if (seenFinalPage) {
                    mainHandler.postDelayed({ finish(true) }, 300)
                } else {
                    finish(false)
                }
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onReceivedError(view: WebView, code: Int, desc: String, url: String) {
                if (done) return
                Log.e(TAG, "WebView legacy error $code url=$url")
                if (seenFinalPage) mainHandler.postDelayed({ finish(true) }, 300)
                else finish(false)
            }
        }

        Log.d(TAG, "Loading: $challengeUrl")
        webView.loadUrl(challengeUrl)
    }

    // ── Helpers ─────────────────────────────────────────────────

    private fun transferCookies(rawCookies: String, javaCookieManager: CookieManager) {
        try {
            val uri = URL(BASE_URL).toURI()
            rawCookies.split(";").map { it.trim() }.filter { it.contains("=") }.forEach { part ->
                val name  = part.substringBefore("=").trim()
                val value = part.substringAfter("=").trim()
                if (name.isNotEmpty()) {
                    val c = HttpCookie(name, value).apply { path = "/"; domain = DOMAIN; maxAge = 21600 }
                    javaCookieManager.cookieStore.add(uri, c)
                }
            }
            Log.d(TAG, "Cookies transferred to java.net: $rawCookies")
        } catch (e: Exception) {
            Log.e(TAG, "transferCookies error", e)
        }
    }

    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()
}
