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
 * Решает JS/AES challenge хостинга bober-api.gt.tc через WebView.
 *
 * Как работает challenge (из shared-client.js оригинала):
 *  1. Сервер возвращает HTML со скриптом, который:
 *     - расшифровывает AES-значение
 *     - ставит куку __test=<hex> (ДВОЙНОЕ подчёркивание)
 *     - делает GET-редирект на <url>?i=1
 *  2. Браузер/WebView выполняет JS, кука установлена, редирект на ?i=1 выполнен
 *  3. После этого все API-запросы к .php идут с ?i=2 (так делает оригинальный клиент)
 */
object ChallengeResolver {
    private const val TAG      = "BoberChallenge"
    const val DOMAIN           = "bober-api.gt.tc"
    const val BASE_URL         = "https://$DOMAIN"
    private const val TIMEOUT_MS = 12_000L
    private const val POLL_MS    = 250L

    // Кука называется __test (двойное подчёркивание) — из shared-client.js оригинала
    private const val COOKIE_NAME = "__test"

    /** Вызывается из фонового потока. Блокирует до завершения или таймаута. */
    fun resolve(context: Context, javaCookieManager: CookieManager): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.e(TAG, "resolve() must not be called on main thread")
            return false
        }
        val latch   = CountDownLatch(1)
        var success = false
        Handler(Looper.getMainLooper()).post {
            showOverlay(context, javaCookieManager) { ok ->
                success = ok
                latch.countDown()
            }
        }
        val completed = latch.await(TIMEOUT_MS + 3000, TimeUnit.MILLISECONDS)
        if (!completed) Log.w(TAG, "resolve() latch timed out")
        Log.d(TAG, "resolve done: success=$success")
        return success
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showOverlay(
        context: Context,
        javaCookieManager: CookieManager,
        onDone: (Boolean) -> Unit
    ) {
        val activity = context as? Activity ?: run { onDone(false); return }

        // ── Overlay ────────────────────────────────────────────
        val overlay = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#CC080416"))
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

        // ── Invisible WebView ──────────────────────────────────
        val webView = WebView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(1, 1)
            visibility = android.view.View.INVISIBLE
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Имитируем обычный браузер — некоторые хостинги проверяют UA
            userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }
        android.webkit.CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            // Удаляем старую сессию чтобы challenge не кешировался
            removeSessionCookies(null)
        }
        overlay.addView(webView)

        val decor = activity.window.decorView as? FrameLayout ?: run { onDone(false); return }
        decor.addView(overlay)

        // ── State ──────────────────────────────────────────────
        val mainHandler   = Handler(Looper.getMainLooper())
        var done          = false
        var elapsedMs     = 0L
        var finalPageSeen = false  // видели ли GET-редирект ?i=1

        fun finish(ok: Boolean) {
            if (done) return
            done = true
            android.webkit.CookieManager.getInstance().flush()
            val rawCookies = android.webkit.CookieManager.getInstance().getCookie(BASE_URL) ?: ""
            Log.d(TAG, "finish ok=$ok rawCookies=[$rawCookies]")
            val hasTest = rawCookies.contains("$COOKIE_NAME=")
            if (hasTest) transferCookies(rawCookies, javaCookieManager)
            else Log.w(TAG, "No $COOKIE_NAME cookie after challenge!")
            decor.removeView(overlay)
            webView.stopLoading(); webView.destroy()
            onDone(ok && hasTest)
        }

        // Polling — запасной
        val pollRunnable = object : Runnable {
            override fun run() {
                if (done) return
                elapsedMs += POLL_MS
                val raw = android.webkit.CookieManager.getInstance().getCookie(BASE_URL) ?: ""
                val has = raw.contains("$COOKIE_NAME=")
                Log.d(TAG, "poll ${elapsedMs}ms finalPage=$finalPageSeen has${COOKIE_NAME}=$has")
                when {
                    finalPageSeen && has    -> finish(true)
                    elapsedMs >= TIMEOUT_MS -> finish(has)
                    else                    -> mainHandler.postDelayed(this, POLL_MS)
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                Log.d(TAG, "WebView page: $url")
                if (done) return
                when {
                    // Финальная страница — JS сделал redirect на ?i=1
                    url.contains("i=1") || url.contains("i=2") -> {
                        finalPageSeen = true
                        // 600мс чтобы WebKit записал куки на диск
                        mainHandler.postDelayed({ finish(true) }, 600)
                    }
                    else -> {
                        // Первая страница загружена, JS выполняется — начинаем polling
                        mainHandler.postDelayed(pollRunnable, POLL_MS)
                    }
                }
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (!request.isForMainFrame || done) return
                Log.e(TAG, "WebView error ${request.url}: ${error.description}")
                // Ошибка на финальной странице (?i=1) — не страшно, кука уже стоит
                if (finalPageSeen) mainHandler.postDelayed({ finish(true) }, 400)
                else finish(false)
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onReceivedError(view: WebView, code: Int, desc: String, url: String) {
                if (done) return
                if (url.contains("i=1") || url.contains("i=2")) {
                    finalPageSeen = true
                    mainHandler.postDelayed({ finish(true) }, 400)
                }
            }
        }

        // Загружаем главную страницу сайта (не API endpoint!)
        // Это триггерит challenge как обычный браузер
        Log.d(TAG, "Loading $BASE_URL to trigger challenge JS")
        webView.loadUrl(BASE_URL)
    }

    private fun transferCookies(rawCookies: String, javaCookieManager: CookieManager) {
        try {
            val uri = URL(BASE_URL).toURI()
            rawCookies.split(";").map { it.trim() }.filter { it.contains("=") }.forEach { part ->
                val name  = part.substringBefore("=").trim()
                val value = part.substringAfter("=").trim()
                if (name.isNotEmpty()) {
                    val c = HttpCookie(name, value).apply {
                        path = "/"; domain = DOMAIN; maxAge = 21600
                    }
                    javaCookieManager.cookieStore.add(uri, c)
                }
            }
            Log.d(TAG, "Transferred: $rawCookies")
        } catch (e: Exception) {
            Log.e(TAG, "transferCookies error", e)
        }
    }

    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()
}
