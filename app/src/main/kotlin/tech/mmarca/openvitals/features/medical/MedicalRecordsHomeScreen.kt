package tech.mmarca.openvitals.features.medical

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.medical.ManualRecordKind
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalCategoryBlock
import tech.mmarca.openvitals.domain.usecase.MedicalExportScope
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.AppBarAction
import tech.mmarca.openvitals.ui.components.DeclareAppBar
import tech.mmarca.openvitals.ui.components.EmptyState
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.LocalHealthConnectGrantAccess
import tech.mmarca.openvitals.ui.components.OpenVitalsListRow
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.ScreenAppBar
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * The records home. The first time it opens, it asks for all thirteen medical
 * permissions in one request, and never again by itself. After that, its callout is
 * the one way to ask for what is still missing, or to reach Health Connect's settings.
 */
@Composable
fun MedicalRecordsHomeScreen(
    viewModel: MedicalRecordsHomeViewModel,
    exportViewModel: MedicalExportViewModel,
    onOpenCategory: (MedicalCategory) -> Unit,
    onOpenImport: () -> Unit,
    onOpenSources: () -> Unit,
    onAddRecord: (ManualRecordKind) -> Unit,
    onOpenDocuments: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var choosingKind by rememberSaveable { mutableStateOf(false) }
    MedicalExportDialog(exportViewModel)
    if (state.available) {
        DeclareAppBar(
            remember { ScreenAppBar(actions = listOf(AppBarAction(Icons.Outlined.Add, R.string.medical_entry_add_action) { choosingKind = true })) },
        )
    }
    if (choosingKind) {
        AddRecordChooser(
            onChoose = { kind ->
                choosingKind = false
                onAddRecord(kind)
            },
            onDismiss = { choosingKind = false },
        )
    }
    // Counts change after an import or a grant made elsewhere, so they are read again on every return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    WithHealthConnectFeatureScreen(feature = HealthConnectFeature.MEDICAL_RECORDS) { hcUx ->
        val grantAccess = LocalHealthConnectGrantAccess.current
        val context = LocalContext.current
        val access = rememberMedicalAccessAction(hcUx.missingReadPermissions, state.firstRequestDone)
        LaunchedEffect(hcUx.isLoading, hcUx.grantedPermissions) {
            if (hcUx.isLoading) return@LaunchedEffect
            val firstVisit = !state.firstRequestDone && hcUx.availability == HealthConnectAvailability.AVAILABLE
            if (firstVisit && hcUx.missingReadPermissions.isNotEmpty() && grantAccess != null) {
                viewModel.onFirstRequestLaunched()
                grantAccess.onGrant(hcUx.missingReadPermissions)
            }
        }
        when {
            !state.available -> EmptyState(
                message = stringResource(R.string.screen_error_feature_unavailable),
                detail = stringResource(R.string.medical_records_unavailable),
                icon = Icons.Outlined.MedicalInformation,
            )
            state.isLoading && state.rows.isEmpty() -> FullScreenLoading()
            else -> MedicalRecordsHomeContent(
                state = state,
                // Before the first request goes out, and while access is read, there is nothing to offer yet.
                access = if (state.firstRequestDone && !hcUx.isLoading) access else MedicalAccessAction.None,
                actions = MedicalHomeActions(
                    onOpenCategory = onOpenCategory,
                    onOpenImport = onOpenImport,
                    onExportAll = { exportViewModel.export(MedicalExportScope.All) },
                    onOpenSources = onOpenSources,
                    onOpenDocuments = onOpenDocuments,
                    onAsk = { permissions -> grantAccess?.onGrant(permissions) },
                    onOpenSettings = { openHealthConnectPermissionSettings(context) },
                ),
            )
        }
    }
}

internal class MedicalHomeActions(
    val onOpenCategory: (MedicalCategory) -> Unit = {},
    val onOpenImport: () -> Unit = {},
    val onExportAll: () -> Unit = {},
    val onOpenSources: () -> Unit = {},
    val onOpenDocuments: () -> Unit = {},
    val onAsk: (Set<String>) -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)

/** The four kinds a person can type in. Each opens its own form. */
@Composable
private fun AddRecordChooser(onChoose: (ManualRecordKind) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.medical_entry_add_action)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                ManualRecordKind.entries.forEach { kind ->
                    OpenVitalsListRow(
                        title = stringResource(entryTitleRes(kind, edit = false)),
                        icon = kind.category.icon,
                        onClick = { onChoose(kind) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { OpenVitalsTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** The home's list, without the shell. Internal for the screenshot tests. */
@Composable
internal fun MedicalRecordsHomeContent(state: MedicalRecordsHomeUiState, access: MedicalAccessAction, actions: MedicalHomeActions) {
    val rowModifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.xs)
    LazyColumn(contentPadding = PaddingValues(vertical = Spacing.sm)) {
        if (access != MedicalAccessAction.None) {
            item(key = "access") {
                MedicalAccessCallout(access, onAsk = actions.onAsk, onOpenSettings = actions.onOpenSettings, modifier = rowModifier)
            }
        }
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = rowModifier.fillMaxWidth()) {
                OpenVitalsOutlinedButton(onClick = actions.onOpenImport, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.medical_records_import))
                }
                OpenVitalsOutlinedButton(onClick = actions.onExportAll, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.medical_records_export_all))
                }
            }
        }
        MedicalCategoryBlock.entries.forEach { block ->
            val blockRows = state.rows.filter { it.category.block == block }
            if (blockRows.isEmpty()) return@forEach
            item(key = block) { SectionHeader(stringResource(block.titleRes)) }
            items(blockRows, key = { it.category }) { row ->
                OpenVitalsListRow(
                    title = stringResource(row.category.titleRes),
                    icon = row.category.icon,
                    supporting = countText(row),
                    note = if (!row.readable && !row.noAccess) stringResource(R.string.medical_records_declined_note) else null,
                    onClick = { actions.onOpenCategory(row.category) },
                    modifier = rowModifier,
                )
            }
        }
        if (state.ownSourceCount > 0 || state.documentCount > 0) {
            item(key = "sources-header") { SectionHeader(stringResource(R.string.medical_sources_title)) }
        }
        if (state.ownSourceCount > 0) {
            item(key = "sources") {
                OpenVitalsListRow(
                    title = pluralStringResource(R.plurals.medical_sources_count, state.ownSourceCount, state.ownSourceCount),
                    icon = Icons.Outlined.FolderOpen,
                    supporting = stringResource(R.string.medical_sources_row_body),
                    onClick = actions.onOpenSources,
                    modifier = rowModifier,
                )
            }
        }
        if (state.documentCount > 0) {
            item(key = "documents") {
                OpenVitalsListRow(
                    title = pluralStringResource(R.plurals.medical_documents_count, state.documentCount, state.documentCount),
                    icon = Icons.Outlined.Description,
                    supporting = stringResource(R.string.medical_documents_row_body, Formatter.formatShortFileSize(LocalContext.current, state.documentsBytes)),
                    onClick = actions.onOpenDocuments,
                    modifier = rowModifier,
                )
            }
        }
    }
}

@Composable
private fun countText(row: MedicalCategoryRow): String {
    val count = row.count
    return when {
        row.noAccess -> stringResource(R.string.medical_records_no_access)
        count == null -> stringResource(R.string.medical_records_count_failed)
        count == 0 -> stringResource(R.string.medical_records_none)
        else -> pluralStringResource(R.plurals.medical_records_count, count, count)
    }
}
