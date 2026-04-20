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
 * Алгоритм:
 *  1. Открываем наложение-оверлей (поверх текущей Activity) с WebView.
 *  2. Загружаем challengeUrl в WebView — он запускает JS, ставит куку _test и
 *     делает redirect обратно на API.
 *  3. Polling каждые 200 мс: как только _test появился в WebView CookieManager —
 *     переносим ВСЕ куки сайта в java.net.CookieManager и закрываем оверлей.
 *  4. Таймаут — 10 секунд, после чего закрываем без гарантии.
 */
object ChallengeResolver {
    private const val TAG      = "BoberChallenge"
    private const val DOMAIN   = "bober-api.gt.tc"
    private const val BASE_URL = "https://$DOMAIN"
    private const val TIMEOUT_MS = 10_000L
    private const val POLL_MS    = 200L

    /**
     * Запускается из фонового потока.
     * Блокирует вызывающий поток до завершения (или таймаута).
     * Возвращает true если куки успешно получены.
     */
    fun resolve(
        context: Context,
        javaCookieManager: CookieManager,
        challengeUrl: String = "$BASE_URL/"
    ): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.e(TAG, "resolve() called on main thread — skip")
            return false
        }

        val latch = CountDownLatch(1)
        var success = false

        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            showChallengeOverlay(context, javaCookieManager, challengeUrl) { ok ->
                success = ok
                latch.countDown()
            }
        }

        latch.await(TIMEOUT_MS + 2000, TimeUnit.MILLISECONDS)
        Log.d(TAG, "resolve finished: success=$success")
        return success
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showChallengeOverlay(
        context: Context,
        javaCookieManager: CookieManager,
        challengeUrl: String,
        onDone: (Boolean) -> Unit
    ) {
        val activity = context as? Activity ?: run {
            Log.e(TAG, "Context is not Activity")
            onDone(false)
            return
        }

        // Root overlay — тёмный полупрозрачный фон
        val overlay = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#CC0A0618"))
        }

        // Карточка по центру
        val card = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(activity, 280), dpToPx(activity, 160),
                Gravity.CENTER
            )
            setBackgroundColor(Color.parseColor("#1A1040"))
            // rounded corners через outline
        }

        val label = TextView(activity).apply {
            text = "🔐 Проверка соединения…"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL or Gravity.TOP
            ).also { it.topMargin = dpToPx(activity, 24) }
        }

        val progress = ProgressBar(activity).apply {
            isIndeterminate = true
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(activity, 48), dpToPx(activity, 48), Gravity.CENTER
            ).also { it.topMargin = dpToPx(activity, 20) }
        }

        val subLabel = TextView(activity).apply {
            text = "Обычно занимает 1–3 секунды"
            textSize = 12f
            setTextColor(Color.parseColor("#88AABB"))
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            ).also { it.bottomMargin = dpToPx(activity, 20) }
        }

        card.addView(label)
        card.addView(progress)
        card.addView(subLabel)
        overlay.addView(card)

        // WebView — невидимый, 1×1 пикселей (за пределами экрана)
        val webView = WebView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(1, 1)
            visibility = android.view.View.INVISIBLE
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString   = "BoberClickerApp/4.0 Android"
        }
        // Синхронизируем CookieManager WebView
        android.webkit.CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        overlay.addView(webView)

        val decor = activity.window.decorView as? FrameLayout
        decor?.addView(overlay) ?: run { onDone(false); return }

        val mainHandler = Handler(Looper.getMainLooper())
        var elapsedMs = 0L
        var done = false

        // Polling — ждём появления _test куки
        val pollRunnable = object : Runnable {
            override fun run() {
                if (done) return
                elapsedMs += POLL_MS

                val rawCookies = android.webkit.CookieManager.getInstance()
                    .getCookie(BASE_URL) ?: ""
                Log.d(TAG, "poll ${elapsedMs}ms cookies: ${rawCookies.take(120)}")

                val hasCookie = rawCookies.contains("_test=")

                if (hasCookie || elapsedMs >= TIMEOUT_MS) {
                    done = true
                    val success = hasCookieTest(rawCookies)
                    if (success) {
                        transferCookies(rawCookies, javaCookieManager)
                        Log.d(TAG, "Challenge solved via WebView ✓")
                    } else {
                        Log.w(TAG, "Challenge timeout without _test cookie")
                    }
                    // Убираем оверлей
                    decor.removeView(overlay)
                    webView.destroy()
                    onDone(success)
                } else {
                    mainHandler.postDelayed(this, POLL_MS)
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                Log.d(TAG, "WebView page loaded: $url")
                // Start polling after first page load
                if (!done) mainHandler.postDelayed(pollRunnable, POLL_MS)
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Log.e(TAG, "WebView error: ${error.description}")
                    done = true
                    decor.removeView(overlay)
                    view.destroy()
                    onDone(false)
                }
            }
        }

        Log.d(TAG, "Loading challenge URL: $challengeUrl")
        webView.loadUrl(challengeUrl)
    }

    private fun hasCookieTest(raw: String) = raw.contains("_test=")

    /** Переносим куки из WebKit CookieManager в java.net.CookieManager */
    private fun transferCookies(rawCookies: String, javaCookieManager: CookieManager) {
        try {
            val uri = URL(BASE_URL).toURI()
            rawCookies.split(";").map { it.trim() }.filter { it.contains("=") }.forEach { part ->
                val name  = part.substringBefore("=").trim()
                val value = part.substringAfter("=").trim()
                if (name.isNotEmpty()) {
                    val cookie = HttpCookie(name, value).apply {
                        path   = "/"
                        domain = DOMAIN
                        maxAge = 21600
                    }
                    javaCookieManager.cookieStore.add(uri, cookie)
                }
            }
            Log.d(TAG, "Transferred cookies: $rawCookies")
        } catch (e: Exception) {
            Log.e(TAG, "transferCookies error", e)
        }
    }

    private fun dpToPx(ctx: Context, dp: Int) =
        (dp * ctx.resources.displayMetrics.density).toInt()
}
