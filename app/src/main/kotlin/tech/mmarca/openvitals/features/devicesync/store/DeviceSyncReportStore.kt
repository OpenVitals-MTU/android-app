package tech.mmarca.openvitals.features.devicesync.store

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DispatcherProvider

/**
 * Persists the last phone-to-phone sync report to a FILE (not preferences),
 * mirroring [tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportReportStore]:
 * a sync of a large dataset can produce a long per-type report, and a file has
 * no size ceiling and survives an app restart so the user can still copy or
 * share it later.
 */
@Singleton
class DeviceSyncReportStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun writeReport(reportText: String) = withContext(dispatchers.io) {
        // Persisting the report is best-effort — it feeds Copy/Share, not
        // correctness — so an I/O failure degrades silently.
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
