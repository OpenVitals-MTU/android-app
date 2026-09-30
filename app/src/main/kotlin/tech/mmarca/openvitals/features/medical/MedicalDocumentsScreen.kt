package tech.mmarca.openvitals.features.medical

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.MedicalDocument
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.EmptyState
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** What a delete dialog is about: one document, or all of them. */
private sealed interface PendingDelete {
    data class One(val document: MedicalDocument) : PendingDelete
    data object All : PendingDelete
}

/** The files kept from imports: open, save, share or delete each. Their records stay when a file goes. */
@Composable
fun MedicalDocumentsScreen(viewModel: MedicalDocumentsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<PendingDelete?>(null) }
    var saving by remember { mutableStateOf<MedicalDocument?>(null) }
    val saved = stringResource(R.string.medical_export_saved)
    val saveFailed = stringResource(R.string.medical_export_save_failed)
    val saver = rememberLauncherForActivityResult(CreateTypedDocument()) { uri ->
        val document = saving
        saving = null
        if (uri != null && document != null) {
            scope.launch {
                val copied = viewModel.file(document.id)?.let { copyMedicalDocument(context, it, uri) } == true
                Toast.makeText(context, if (copied) saved else saveFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val actions = DocumentActions(
        onOpen = { document -> scope.launch { viewModel.file(document.id)?.let { openMedicalDocument(context, it, document.mimeType) } } },
        onSave = { document ->
            saving = document
            saver.launch(document.fileName to document.mimeType)
        },
        onShare = { document ->
            scope.launch { viewModel.file(document.id)?.let { shareMedicalDocument(context, it, document.mimeType, document.fileName) } }
        },
        onDelete = { pending = PendingDelete.One(it) },
    )
    pending?.let { target ->
        DeleteDocumentDialog(
            target = target,
            onDismiss = { pending = null },
            onConfirm = {
                pending = null
                when (target) {
                    is PendingDelete.One -> viewModel.delete(target.document.id)
                    PendingDelete.All -> viewModel.deleteAll()
                }
            },
        )
    }
    WithHealthConnectFeatureScreen(feature = HealthConnectFeature.MEDICAL_RECORDS_BROWSE) {
        val error = state.error
        when {
            state.isLoading && state.documents.isEmpty() -> FullScreenLoading()
            error != null -> ScreenErrorContent(error)
            state.documents.isEmpty() -> EmptyState(message = stringResource(R.string.medical_documents_empty), icon = Icons.Outlined.Description)
            else -> LazyColumn(contentPadding = PaddingValues(vertical = Spacing.sm)) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.xs),
                    ) {
                        Text(
                            text = stringResource(R.string.medical_documents_total, Formatter.formatShortFileSize(context, state.totalBytes)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        OpenVitalsTextButton(onClick = { pending = PendingDelete.All }) {
                            Text(stringResource(R.string.medical_documents_delete_all))
                        }
                    }
                }
                items(state.documents, key = { it.id }) { document -> DocumentCard(document, actions) }
            }
        }
    }
}

private class DocumentActions(
    val onOpen: (MedicalDocument) -> Unit,
    val onSave: (MedicalDocument) -> Unit,
    val onShare: (MedicalDocument) -> Unit,
    val onDelete: (MedicalDocument) -> Unit,
)

@Composable
private fun DocumentCard(document: MedicalDocument, actions: DocumentActions) {
    val context = LocalContext.current
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(document.importedAt.atZone(ZoneId.systemDefault()))
    OpenVitalsCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.xs),
    ) {
        Column(modifier = Modifier.padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.md, bottom = Spacing.xs)) {
            Text(text = document.fileName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = listOfNotNull(
                    date,
                    Formatter.formatShortFileSize(context, document.sizeBytes),
                    pluralStringResource(R.plurals.medical_records_count, document.recordCount, document.recordCount),
                    document.sourceName,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                DocumentAction(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(R.string.medical_documents_open_action, document.fileName)) { actions.onOpen(document) }
                DocumentAction(Icons.Outlined.Save, stringResource(R.string.medical_documents_save_action, document.fileName)) { actions.onSave(document) }
                DocumentAction(Icons.Outlined.Share, stringResource(R.string.medical_documents_share_action, document.fileName)) { actions.onShare(document) }
                DocumentAction(Icons.Outlined.Delete, stringResource(R.string.medical_sources_delete_action, document.fileName)) { actions.onDelete(document) }
            }
        }
    }
}

@Composable
private fun DocumentAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(imageVector = icon, contentDescription = description) }
}

/** Says the records stay, because they do. */
@Composable
private fun DeleteDocumentDialog(target: PendingDelete, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (target) {
                    is PendingDelete.One -> stringResource(R.string.medical_sources_delete_title, target.document.fileName)
                    PendingDelete.All -> stringResource(R.string.medical_documents_delete_all_title)
                },
            )
        },
        text = {
            Text(
                stringResource(
                    if (target is PendingDelete.All) R.string.medical_documents_delete_all_body else R.string.medical_documents_delete_body,
                ),
            )
        },
        confirmButton = { OpenVitalsTextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { OpenVitalsTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
