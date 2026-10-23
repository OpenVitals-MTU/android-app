package tech.mmarca.openvitals.core.diagnostics

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.export.stageSoleExport

suspend fun Context.shareDebugDiagnosticsLog() {
    val shareIntent = withContext(Dispatchers.IO) {
        // One export at a time: an earlier one goes, and app start sweeps a day-old one.
        val exportFile = File(cacheDir, DiagnosticsExportCacheDirectory).stageSoleExport(DiagnosticsExportFileName) { output ->
            PrivacySafeDebugLogExporter.writeCurrentProcessLogcatBlocking(this@shareDebugDiagnosticsLog, output)
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

internal const val DiagnosticsExportCacheDirectory = "diagnostics_exports"
internal const val DiagnosticsExportFileName = "openvitals-diagnostics-logs.txt"
private const val DiagnosticsMimeType = "text/plain"
