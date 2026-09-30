package tech.mmarca.openvitals.features.medical

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.medical.ManualFhirWriter
import tech.mmarca.openvitals.domain.medical.ManualRecordKind
import tech.mmarca.openvitals.domain.medical.SummaryField
import tech.mmarca.openvitals.domain.model.MedicalDocument
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.usecase.MedicalExportScope
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.healthconnect.openHealthConnectDataSettings
import tech.mmarca.openvitals.ui.components.AppBarAction
import tech.mmarca.openvitals.ui.components.DeclareAppBar
import tech.mmarca.openvitals.ui.components.DetailRow
import tech.mmarca.openvitals.ui.components.DetailSectionCard
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.ScreenAppBar
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * One record, shown as received: curated fields, the source, and the raw FHIR. Values are never
 * interpreted. It exports as a FHIR file, and a record this app wrote can be deleted.
 */
@Composable
fun MedicalRecordDetailScreen(
    viewModel: MedicalRecordDetailViewModel,
    exportViewModel: MedicalExportViewModel,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onDeleted: () -> Unit,
    onEdit: (ManualRecordKind, String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    // An edit made on the entry screen shows on return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    MedicalExportDialog(exportViewModel)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    DetailAppBar(
        state,
        onExport = { ref -> exportViewModel.export(MedicalExportScope.Record(ref)) },
        onDelete = { confirmDelete = true },
        onEdit = onEdit,
        onOpenDocument = { document ->
            scope.launch { viewModel.documentFile()?.let { openMedicalDocument(context, it, document.mimeType) } }
        },
    )
    DeleteEffects(state, onDeleted = onDeleted, onErrorShown = viewModel::consumeDeleteError)
    if (confirmDelete) {
        DeleteRecordDialog(
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
        )
    }
    WithHealthConnectFeatureScreen(feature = HealthConnectFeature.MEDICAL_RECORDS_BROWSE) {
        val error = state.error
        when {
            state.isLoading -> FullScreenLoading()
            error != null -> ScreenErrorContent(error)
            else -> MedicalRecordDetailContent(state, dateTimeFormatterProvider)
        }
    }
}

/** The record's cards, without the shell. Internal for the screenshot tests. */
@Composable
internal fun MedicalRecordDetailContent(
    state: MedicalRecordDetailUiState,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    var showRaw by rememberSaveable { mutableStateOf(false) }
    val sectionModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.xs)
    LazyColumn(contentPadding = PaddingValues(vertical = Spacing.sm)) {
        item {
            OpenVitalsCard(modifier = sectionModifier) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text(
                        text = state.title?.let { valueText(SummaryField.TYPE, it.copy(caption = null), dateTimeFormatterProvider) }
                            ?: stringResource(R.string.medical_records_untitled),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = listOfNotNull(state.resourceType, state.title?.caption?.let(::codeSystemLabel)).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm),
                    )
                    state.status?.let { DetailRow(stringResource(R.string.medical_record_status), statusText(it)) }
                    state.date?.let {
                        DetailRow(stringResource(R.string.medical_record_date), it.displayText(dateTimeFormatterProvider.mediumDate()))
                    }
                    state.value?.let { value ->
                        // The flag is the one the source set. Nothing is computed here.
                        DetailRow(stringResource(R.string.medical_record_value), listOfNotNull(value, state.flag).joinToString(" · "))
                    }
                }
            }
        }
        if (state.details.isNotEmpty()) {
            item {
                DetailSectionCard(title = stringResource(R.string.medical_record_details), modifier = sectionModifier) {
                    state.details.forEach { row ->
                        DetailRow(
                            label = row.label ?: stringResource(row.field.labelRes),
                            value = valueText(row.field, row.value, dateTimeFormatterProvider),
                        )
                    }
                }
            }
        }
        item {
            DetailSectionCard(title = stringResource(R.string.medical_record_source), modifier = sectionModifier) {
                Text(text = state.sourceName ?: "—", style = MaterialTheme.typography.bodyMedium)
                state.sourceUri?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // What OpenVitals knows about how the record got here, beyond its source.
                listOfNotNull(
                    R.string.medical_record_card_unverified.takeIf { state.unverifiedCard },
                    R.string.medical_record_converted.takeIf { state.convertedFromDstu2 },
                ).forEach { note ->
                    Text(text = stringResource(note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!state.writtenByThisApp) {
                    Text(
                        text = stringResource(R.string.medical_record_other_app),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val context = LocalContext.current
                    OpenVitalsTextButton(onClick = { openHealthConnectDataSettings(context) }) {
                        Text(stringResource(R.string.medical_record_open_health_connect))
                    }
                }
            }
        }
        item {
            DetailSectionCard(title = stringResource(R.string.medical_record_raw_fhir), modifier = sectionModifier) {
                OpenVitalsOutlinedButton(onClick = { showRaw = !showRaw }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (showRaw) R.string.medical_record_hide_raw else R.string.medical_record_show_raw))
                }
                if (showRaw) {
                    SelectionContainer {
                        Text(text = state.rawJson, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

/** Export once the record has loaded. Delete only for a record this app wrote; edit only for one typed in here. */
@Composable
private fun DetailAppBar(
    state: MedicalRecordDetailUiState,
    onExport: (MedicalRecordRef) -> Unit,
    onDelete: () -> Unit,
    onEdit: (ManualRecordKind, String) -> Unit,
    onOpenDocument: (MedicalDocument) -> Unit,
) {
    val ref = state.ref.takeIf { !state.isLoading && state.error == null }
    val document = state.document.takeIf { ref != null }
    val manualKind = ref?.takeIf { state.writtenByThisApp && state.sourceUri == ManualFhirWriter.SourceBaseUri }
        ?.let { ManualRecordKind.ofResourceType(it.resourceType) }
    DeclareAppBar(
        remember(ref, state.writtenByThisApp, manualKind, document) {
            ScreenAppBar(
                actions = listOfNotNull(
                    document?.let { AppBarAction(Icons.Outlined.Description, R.string.medical_documents_open_original) { onOpenDocument(it) } },
                    if (ref != null && manualKind != null) {
                        AppBarAction(Icons.Outlined.Edit, R.string.medical_entry_edit_action) { onEdit(manualKind, ref.resourceId) }
                    } else {
                        null
                    },
                    ref?.let { AppBarAction(Icons.Outlined.Share, R.string.medical_export_action) { onExport(it) } },
                    if (ref != null && state.writtenByThisApp) AppBarAction(Icons.Outlined.Delete, R.string.medical_record_delete_action, onClick = onDelete) else null,
                ),
            )
        },
    )
}

@Composable
private fun DeleteEffects(state: MedicalRecordDetailUiState, onDeleted: () -> Unit, onErrorShown: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    val failed = stringResource(R.string.medical_record_delete_failed)
    val reason = state.deleteError.resolve()
    LaunchedEffect(state.deleteError) {
        if (state.deleteError != null) {
            Toast.makeText(context, listOfNotNull(failed, reason).joinToString(" "), Toast.LENGTH_LONG).show()
            onErrorShown()
        }
    }
}

@Composable
private fun DeleteRecordDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.medical_record_delete_title)) },
        text = { Text(stringResource(R.string.medical_record_delete_body)) },
        confirmButton = { OpenVitalsTextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { OpenVitalsTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** A value as words: a code gets its label, a date the locale's format, and a bare code its system's short name. */
@Composable
private fun valueText(field: SummaryField, value: MedicalValue, formatters: DateTimeFormatterProvider): String {
    value.needsAccess?.let { return stringResource(R.string.medical_record_needs_access, stringResource(it.titleRes)) }
    val text = value.text.orEmpty()
    val label = fhirCodeLabelRes(field, text)?.let { stringResource(it) } ?: dateFieldText(field, text, formatters)
    return listOfNotNull(label, value.caption?.let(::codeSystemLabel)).joinToString(" · ")
}
