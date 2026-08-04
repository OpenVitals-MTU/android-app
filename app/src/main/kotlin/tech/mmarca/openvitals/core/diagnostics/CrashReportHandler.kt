package tech.mmarca.openvitals.core.diagnostics

import android.content.Context
import kotlin.system.exitProcess

object CrashReportHandler {
    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is OpenVitalsCrashHandler) return
        Thread.setDefaultUncaughtExceptionHandler(
            OpenVitalsCrashHandler(
                appContext = context.applicationContext,
                previous = previous,
            )
        )
    }

    private class OpenVitalsCrashHandler(
        private val appContext: Context,
        private val previous: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            runCatching {
                CrashReportStore.writeLastCrashReport(appContext, thread, throwable)
            }
            previous?.uncaughtException(thread, throwable) ?: exitProcess(10)
        }
    }
}
