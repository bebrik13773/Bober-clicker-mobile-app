package com.bebrik.boberclicker

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.bebrik.boberclicker.data.GameRepository

class BoberApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GameRepository.init(this)
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val log = buildString {
                    appendLine("=== BOBER CRASH LOG ===")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Time: ${java.util.Date()}")
                    appendLine()
                    appendLine("ERROR: ${throwable.message}")
                    appendLine()
                    appendLine("STACKTRACE:")
                    appendLine(Log.getStackTraceString(throwable))
                    appendLine()
                    appendLine("USER: ${GameRepository.login} (id=${GameRepository.userId})")
                    appendLine("SCORE: ${GameRepository.score}")
                    appendLine("ONLINE: ${GameRepository.isOnline}")
                }
                Log.e("BOBER_CRASH", log)
                // Copy to clipboard so user can share
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Bober Crash Log", log))
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
