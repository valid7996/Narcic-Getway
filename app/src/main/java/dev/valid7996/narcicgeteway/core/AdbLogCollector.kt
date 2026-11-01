package dev.valid7996.narcicgeteway.core

import android.content.Context
import android.os.Build
import dev.valid7996.narcicgeteway.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AdbLogCollector {
    suspend fun collect(context: Context): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        sb.appendLine("==================================================")
        sb.appendLine("📱 Narcic Getway - لاگ ADB و اطلاعات سیستم")
        sb.appendLine("==================================================")
        sb.appendLine("زمان استخراج: $timeStr")
        sb.appendLine("نسخه برنامه: ${BuildConfig.VERSION_NAME} (کد بیلد: ${BuildConfig.VERSION_CODE})")
        sb.appendLine("مدل دستگاه: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
        sb.appendLine("نسخه اندروید: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("معماری پردازنده (ABIs): ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        sb.appendLine("شناسه پردازه فعلی (PID): ${android.os.Process.myPid()}")
        val status = VpnManager.status.value
        sb.appendLine("وضعیت اتصال: ${status.state} (هسته: ${VpnManager.activeKind.value ?: "غیرفعال"})")
        if (status.error != null) {
            sb.appendLine("آخرین خطای VPN: ${status.error}")
        }

        val lastCrash = CrashReporter.lastCrash(context)
        if (!lastCrash.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("==================================================")
            sb.appendLine("🚨 آخرین خطای کرش ثبت‌شده (CRASH REPORT):")
            sb.appendLine("==================================================")
            sb.appendLine(lastCrash)
            sb.appendLine("==================================================")
        }

        sb.appendLine()
        sb.appendLine("==================================================")
        sb.appendLine("📋 خروجی لاگ سیستم (ADB LOGCAT):")
        sb.appendLine("==================================================")

        val logcatLines = fetchLogcat()
        for (line in logcatLines) {
            sb.appendLine(line)
        }

        sb.toString()
    }

    private fun fetchLogcat(): List<String> {
        val candidates = listOf(
            listOf("logcat", "-d", "-v", "time", "-t", "4000"),
            listOf("logcat", "-d", "-v", "time"),
            listOf("logcat", "-d"),
        )
        for (cmd in candidates) {
            val lines = runCatching {
                val proc = ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start()
                val list = proc.inputStream.bufferedReader().use { it.readLines() }
                proc.waitFor(4, TimeUnit.SECONDS)
                list
            }.getOrNull()
            if (!lines.isNullOrEmpty()) {
                return if (lines.size > 4000) lines.takeLast(4000) else lines
            }
        }

        val inApp = LogBus.lines.value
        if (inApp.isNotEmpty()) {
            return listOf("(logcat در دسترس نبود؛ بازگشت به لاگ‌های برنامه)") + inApp
        }
        return listOf("امکان خواندن logcat وجود نداشت.")
    }
}
