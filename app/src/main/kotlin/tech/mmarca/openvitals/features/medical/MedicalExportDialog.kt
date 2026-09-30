package tech.mmarca.openvitals.features.medical

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.performance.offMainIo
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.Spacing

private const val FhirJsonMimeType = "application/json"

/** Shows an export while it is built, then offers share or save. The file holds medical records, so it says so. */
@Composable
internal fun MedicalExportDialog(viewModel: MedicalExportViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved = stringResource(R.string.medical_export_saved)
    val saveFailed = stringResource(R.string.medical_export_save_failed)
    val ready = state as? MedicalExportUiState.Ready
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(FhirJsonMimeType)) { uri ->
        val file = ready?.file
        if (uri != null && file != null) {
            scope.launch {
                val copied = copyExportTo(context, file, uri)
                Toast.makeText(context, if (copied) saved else saveFailed, Toast.LENGTH_SHORT).show()
                if (copied) viewModel.dismiss()
            }
        }
    }
    when (val current = state) {
        null -> Unit
        MedicalExportUiState.Building -> ExportAlert(
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(Spacing.xxl))
                    Text(stringResource(R.string.medical_export_building))
                }
            },
            onDismiss = viewModel::dismiss,
        )
        is MedicalExportUiState.Failed -> ExportAlert(text = { Text(current.error.resolve().orEmpty()) }, onDismiss = viewModel::dismiss)
        is MedicalExportUiState.Ready -> if (current.recordCount == 0) {
            ExportAlert(text = { Text(stringResource(R.string.medical_export_empty)) }, onDismiss = viewModel::dismiss)
        } else {
            ExportAlert(
                text = { ReadyText(current) },
                onDismiss = viewModel::dismiss,
                actions = {
                    OpenVitalsTextButton(onClick = { saver.launch(current.file.name) }) { Text(stringResource(R.string.action_save)) }
                    OpenVitalsTextButton(onClick = {
                        shareExport(context, current.file)
                        viewModel.dismiss()
                    }) { Text(stringResource(R.string.medical_export_share)) }
                },
            )
        }
    }
}

@Composable
private fun ExportAlert(text: @Composable () -> Unit, onDismiss: () -> Unit, actions: (@Composable () -> Unit)? = null) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.medical_export_title)) },
        text = text,
        confirmButton = { Row { actions?.invoke() } },
        dismissButton = {
            OpenVitalsTextButton(onClick = onDismiss) {
                Text(stringResource(if (actions == null) R.string.action_close else R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun ReadyText(ready: MedicalExportUiState.Ready) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(pluralStringResource(R.plurals.medical_export_body, ready.recordCount, ready.recordCount))
        Text(stringResource(R.string.medical_export_warning), color = MaterialTheme.colorScheme.error)
        if (ready.ownOnly.isNotEmpty()) {
            Text(stringResource(R.string.medical_export_own_only, categoryNames(ready.ownOnly)), style = MaterialTheme.typography.bodySmall)
        }
        if (ready.leftOut.isNotEmpty()) {
            Text(stringResource(R.string.medical_export_left_out, categoryNames(ready.leftOut)), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun categoryNames(categories: Set<MedicalCategory>): String =
    categories.sortedBy { it.ordinal }.map { stringResource(it.titleRes) }.joinToString(", ")

/** No toast: the chooser is the feedback. */
private fun shareExport(context: Context, file: File): Result<Unit> = runCatching {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = FhirJsonMimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, file.name, uri)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.medical_export_chooser_title)))
}

private suspend fun copyExportTo(context: Context, file: File, destination: Uri): Boolean =
    offMainIo {
        context.contentResolver.openOutputStream(destination)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        } ?: error("no output stream for $destination")
    }.isSuccess
