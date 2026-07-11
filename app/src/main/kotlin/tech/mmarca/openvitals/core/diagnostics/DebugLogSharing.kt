package tech.mmarca.openvitals.core.diagnostics

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.R

suspend fun Context.shareDebugDiagnosticsLog() {
    val shareIntent = withContext(Dispatchers.IO) {
        val exportDirectory = File(cacheDir, DiagnosticsExportCacheDirectory).apply {
            check(mkdirs() || isDirectory) { "Unable to create diagnostics export directory." }
        }
        val exportFile = File(exportDirectory, DiagnosticsExportFileName)
        exportFile.outputStream().use { output ->
            PrivacySafeDebugLogExporter.writeCurrentProcessLogcat(this@shareDebugDiagnosticsLog, output)
        }
        val uri = FileProvider.getUriForFile(
            this@shareDebugDiagnosticsLog,
            "$packageName.fileprovider",
            exportFile,
        )

        Intent(Intent.ACTION_SEND).apply {
            type = DiagnosticsMimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(contentResolver, exportFile.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    startActivity(
        Intent.createChooser(
            shareIntent,
            getString(R.string.settings_debug_logs_share_chooser_title),
        )
    )
}

private const val DiagnosticsExportCacheDirectory = "diagnostics_exports"
private const val DiagnosticsExportFileName = "openvitals-diagnostics-logs.txt"
private const val DiagnosticsMimeType = "text/plain"
