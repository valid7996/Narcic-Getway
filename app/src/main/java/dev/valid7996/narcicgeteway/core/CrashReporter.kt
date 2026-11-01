package dev.valid7996.narcicgeteway.core

import android.content.Context
import android.os.Build
import dev.valid7996.narcicgeteway.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {
    private const val CRASH_FILE = "last_crash.txt"
    private const val CRASH_HISTORY = "crash_history.log"
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { recordCrash(context, thread, throwable) }
            prevHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun recordCrash(context: Context, thread: Thread, throwable: Throwable) {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val sb = StringBuilder()
        sb.appendLine("---------------- CRASH REPORT ----------------")
        sb.appendLine("Time: $timeStr")
        sb.appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
        sb.appendLine("Android OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        sb.appendLine("Thread: ${thread.name} (id: ${thread.id})")
        sb.appendLine("Exception: ${throwable.javaClass.name}: ${throwable.message}")
        sb.appendLine("Stack Trace:")
        sb.appendLine(throwable.stackTraceToString())
        var cause = throwable.cause
        var depth = 1
        while (cause != null && depth <= 5) {
            sb.appendLine("Caused by ($depth): ${cause.javaClass.name}: ${cause.message}")
            sb.appendLine(cause.stackTraceToString())
            cause = cause.cause
            depth++
        }
        sb.appendLine("----------------------------------------------")

        val report = sb.toString()
        runCatching {
            File(context.filesDir, CRASH_FILE).writeText(report)
            File(context.filesDir, CRASH_HISTORY).appendText("$report\n\n")
        }
    }

    fun lastCrash(context: Context): String? = runCatching {
        val file = File(context.filesDir, CRASH_FILE)
        if (file.exists() && file.length() > 0) file.readText() else null
    }.getOrNull()

    fun hasCrash(context: Context): Boolean = !lastCrash(context).isNullOrBlank()

    fun clearCrash(context: Context) {
        runCatching { File(context.filesDir, CRASH_FILE).delete() }
    }
}
