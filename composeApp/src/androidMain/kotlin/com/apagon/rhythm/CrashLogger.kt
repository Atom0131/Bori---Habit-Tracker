package com.apagon.rhythm
import com.apagon.rhythm.core.time.*

import android.content.Context
import android.content.Intent
import java.io.File
import kotlinx.datetime.LocalDateTime

object CrashLogger {
    private const val LOG_FILE = "crash_log.txt"
    private const val MAX_LOG_BYTES = 64 * 1024
    private const val PREFS_NAME = "crash_logger_prefs"
    private const val KEY_ACTIVE = "active"

    // Secret passcode entered via Settings > About's 7-tap version-number gesture
    // (same entry point as the VIP promo code) to turn capture on.
    const val DEBUG_PASSCODE = "RHY-LOG-DBG"

    fun isActive(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)

    fun setActive(context: Context, active: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ACTIVE, active).apply()
        if (!active) clear(context)
    }

    fun install(context: Context) {
        val appContext = context.applicationContext
        // Not actively diagnosing — make sure nothing lingers from a previous active session.
        if (!isActive(appContext)) clear(appContext)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                if (isActive(appContext)) {
                    val entry = buildString {
                        append("=== CRASH ${LocalDateTime.now()} ===\n")
                        append("Thread: ${thread.name}\n\n")
                        append(throwable.stackTraceToString())
                        append("\n\n")
                    }
                    val file = File(appContext.filesDir, LOG_FILE)
                    file.appendText(entry)
                    if (file.length() > MAX_LOG_BYTES) {
                        val trimmed = file.readText().takeLast(MAX_LOG_BYTES)
                        file.writeText(trimmed)
                    }
                }
            } catch (_: Exception) { }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? {
        val file = File(context.filesDir, LOG_FILE)
        return if (file.exists() && file.length() > 0) file.readText() else null
    }

    fun clear(context: Context) {
        File(context.filesDir, LOG_FILE).delete()
    }

    fun share(context: Context) {
        val text = read(context) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Rhythm Crash Log")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Crash Log"))
    }
}
