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
 * Решает JS/AES anti-bot challenge хостинга bober-api.gt.tc.
 *
 * Точная логика из shared-client.js:
 *   1. POST /api/foo.php → сервер отдаёт HTML с JS-скриптом
 *   2. JS ставит куку _test=<hex> и делает location.href = "...foo.php?i=1"
 *   3. Мы загружаем этот redirectUrl в WebView (это GET-запрос — именно так ожидает сервер)
 *   4. После загрузки ?i=1 кука установлена и принята сервером
 *   5. Retry: POST /api/foo.php?i=2 — сервер пропускает, отвечает JSON
 */
object ChallengeResolver {
    private const val TAG        = "BoberChallenge"
    const val DOMAIN             = "bober-api.gt.tc"
    const val BASE_URL           = "https://$DOMAIN"
    private const val TIMEOUT_MS = 12_000L
    private const val POLL_MS    = 300L

    /** Парсим HTML challenge и возвращаем redirectUrl (url?i=1) */
    fun parseRedirectUrl(html: String): String? {
        // location.href="https://bober-api.gt.tc/api/auth/login.php?i=1"
        // или: location.href= "https://..."  (с пробелом)
        val match = Regex("""location\.href\s*=\s*["']([^"']+)["']""").find(html)
        val url = match?.groupValues?.get(1)?.trim() ?: return null
        Log.d(TAG, "Parsed redirectUrl: $url")
        return url
    }

    /** Определяем имя куки challenge из HTML */
    fun parseCookieName(html: String): String {
        // document.cookie="_test=" или document.cookie="__test="
        val match = Regex("""document\.cookie\s*=\s*["'](_{1,2}test)=""").find(html)
        return match?.groupValues?.get(1) ?: "_test"
    }

    /**
     * Вызывается из фонового потока.
     * Загружает redirectUrl в WebView, ждёт куку, переносит в java.net.
     * Возвращает true если успешно.
     */
    fun resolve(
        context: Context,
        javaCookieManager: CookieManager,
        redirectUrl: String,
        cookieName: String = "_test"
    ): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.e(TAG, "resolve() must not run on main thread")
            return false
        }
        val latch   = CountDownLatch(1)
        var success = false
        Handler(Looper.getMainLooper()).post {
            showOverlay(context, javaCookieManager, redirectUrl, cookieName) { ok ->
                success = ok
                latch.countDown()
            }
        }
        val completed = latch.await(TIMEOUT_MS + 3000, TimeUnit.MILLISECONDS)
        if (!completed) Log.w(TAG, "resolve() timed out")
        Log.d(TAG, "resolve finished: success=$success")
        return success
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showOverlay(
        context: Context,
        javaCookieManager: CookieManager,
        redirectUrl: String,
        cookieName: String,
        onDone: (Boolean) -> Unit
    ) {
        val activity = context as? Activity ?: run { onDone(false); return }

        // ── Overlay UI ─────────────────────────────────────────
        val overlay = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
            setBackgroundColor(Color.parseColor("#CC080416"))
        }
        val card = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(dp(activity, 300), dp(activity, 170), Gravity.CENTER)
            setBackgroundColor(Color.parseColor("#1A1040"))
        }
        card.addView(TextView(activity).apply {
            text = "🔐 Проверка соединения…"
            textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
                .also { it.topMargin = dp(activity, 24) }
        })
        card.addView(ProgressBar(activity).apply {
            isIndeterminate = true
            layoutParams = FrameLayout.LayoutParams(dp(activity, 48), dp(activity, 48), Gravity.CENTER)
        })
        card.addView(TextView(activity).apply {
            text = "Обычно 1–3 секунды"
            textSize = 12f; setTextColor(Color.parseColor("#88AABB")); gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                .also { it.bottomMargin = dp(activity, 18) }
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
            userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }
        android.webkit.CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        overlay.addView(webView)

        val decor = activity.window.decorView as? FrameLayout ?: run { onDone(false); return }
        decor.addView(overlay)

        // ── State ──────────────────────────────────────────────
        val mainHandler = Handler(Looper.getMainLooper())
        var done        = false
        var elapsedMs   = 0L

        fun finish(ok: Boolean) {
            if (done) return
            done = true
            android.webkit.CookieManager.getInstance().flush()
            val raw = android.webkit.CookieManager.getInstance().getCookie(BASE_URL) ?: ""
            Log.d(TAG, "finish ok=$ok cookieName=$cookieName raw=[$raw]")
            val hasCookie = raw.contains("$cookieName=")
            if (hasCookie) {
                transferCookies(raw, javaCookieManager)
            } else {
                Log.w(TAG, "Cookie '$cookieName' not found in: $raw")
            }
            decor.removeView(overlay)
            webView.stopLoading()
            webView.destroy()
            onDone(ok && hasCookie)
        }

        // Polling (запасной вариант если onPageFinished не сработал)
        val poll = object : Runnable {
            override fun run() {
                if (done) return
                elapsedMs += POLL_MS
                val raw = android.webkit.CookieManager.getInstance().getCookie(BASE_URL) ?: ""
                val has = raw.contains("$cookieName=")
                Log.d(TAG, "poll ${elapsedMs}ms has=$has raw=${raw.take(80)}")
                when {
                    has                     -> { mainHandler.postDelayed({ finish(true) }, 400) }
                    elapsedMs >= TIMEOUT_MS -> finish(false)
                    else                    -> mainHandler.postDelayed(this, POLL_MS)
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                Log.d(TAG, "page: $url")
                if (done) return
                // После загрузки любой страницы — проверяем куку + ждём 500мс
                mainHandler.postDelayed({ finish(true) }, 500)
            }

            override fun onReceivedError(
                view: WebView, req: WebResourceRequest, error: WebResourceError
            ) {
                if (!req.isForMainFrame || done) return
                Log.e(TAG, "error ${req.url}: ${error.description}")
                // Ошибка HTTP — но кука уже могла быть установлена JS до редиректа
                mainHandler.postDelayed({ finish(true) }, 400)
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onReceivedError(v: WebView, code: Int, desc: String, url: String) {
                if (done) return
                Log.e(TAG, "legacy error $code $url")
                mainHandler.postDelayed({ finish(true) }, 400)
            }
        }

        // Загружаем redirectUrl (GET ?i=1) — именно это ожидает сервер
        Log.d(TAG, "WebView loading redirectUrl: $redirectUrl")
        webView.loadUrl(redirectUrl)

        // Запускаем polling как запасной вариант
        mainHandler.postDelayed(poll, 1000)
    }

    private fun transferCookies(raw: String, mgr: CookieManager) {
        try {
            val uri = URL(BASE_URL).toURI()
            raw.split(";").map { it.trim() }.filter { it.contains("=") }.forEach { part ->
                val name  = part.substringBefore("=").trim()
                val value = part.substringAfter("=").trim()
                if (name.isNotEmpty()) {
                    mgr.cookieStore.add(uri,
                        HttpCookie(name, value).apply { path = "/"; domain = DOMAIN; maxAge = 21600 }
                    )
                }
            }
            Log.d(TAG, "Transferred cookies: $raw")
        } catch (e: Exception) {
            Log.e(TAG, "transferCookies: $e")
        }
    }

    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP  = ViewGroup.LayoutParams.WRAP_CONTENT
}
