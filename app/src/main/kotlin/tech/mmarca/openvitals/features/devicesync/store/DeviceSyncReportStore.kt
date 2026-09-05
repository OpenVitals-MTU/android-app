package tech.mmarca.openvitals.features.devicesync.store

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DispatcherProvider

/** Persists the last sync report to a file: no size ceiling, and it survives a restart. */
@Singleton
class DeviceSyncReportStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun writeReport(reportText: String) = withContext(dispatchers.io) {
        // Best-effort: the report feeds Copy/Share, not correctness.
        runCatching {
            val directory = File(context.filesDir, REPORT_DIRECTORY).apply { mkdirs() }
            File(directory, REPORT_FILE_NAME).writeText(reportText)
        }
    }

    suspend fun readReport(): String = withContext(dispatchers.io) {
        runCatching {
            File(File(context.filesDir, REPORT_DIRECTORY), REPORT_FILE_NAME)
                .takeIf { it.exists() && it.isFile }
                ?.readText()
                .orEmpty()
        }.getOrDefault("")
    }

    private companion object {
        const val REPORT_DIRECTORY = "device_sync"
        const val REPORT_FILE_NAME = "sync_report.txt"
    }
}
