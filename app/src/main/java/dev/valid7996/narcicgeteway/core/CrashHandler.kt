package dev.valid7996.narcicgeteway.core

import android.content.Context
import android.os.Process
import android.util.Log
import java.io.File

object CrashHandler {
    private const val TAG = "AndroidRuntime"
    private const val FILE_NAME = "last_crash.log"

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        // Read and push previous crash to LogBus if present
        runCatching {
            val crashFile = File(context.filesDir, FILE_NAME)
            if (crashFile.exists() && crashFile.length() > 0) {
                val lines = crashFile.readLines()
                if (lines.isNotEmpty()) {
                    LogBus.appendAll(listOf("E/$TAG: --- [Previous Session Crash Dump] ---") + lines)
                }
            }
        }

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stack = Log.getStackTraceString(throwable)
                val crashLines = listOf(
                    "E/$TAG: FATAL EXCEPTION: ${thread.name}",
                    "E/$TAG: Process: ${context.packageName}, PID: ${Process.myPid()}",
                    "E/$TAG: $stack"
                )
                LogBus.appendAll(crashLines)

                // Persist crash to disk for next session inspection
                val crashFile = File(context.filesDir, FILE_NAME)
                crashFile.writeText(crashLines.joinToString("\n"))
            } catch (e: Throwable) {
                Log.e(TAG, "Failed in custom uncaught exception handler", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
