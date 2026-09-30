package tech.mmarca.openvitals.features.medical

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.medical.ManualRecordKind
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.usecase.MedicalExportScope
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.AppBarAction
import tech.mmarca.openvitals.ui.components.DeclareAppBar
import tech.mmarca.openvitals.ui.components.EmptyState
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.OpenVitalsListRow
import tech.mmarca.openvitals.ui.components.ScreenAppBar
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** One category's records, newest first. Each row: title, then date, source and status. */
@Composable
fun MedicalCategoryScreen(
    viewModel: MedicalCategoryViewModel,
    exportViewModel: MedicalExportViewModel,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenRecord: (MedicalRecordRef) -> Unit,
    onAddRecord: (ManualRecordKind) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val category = state.category
    // A record deleted on its detail screen, or in Health Connect, must not linger here.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    MedicalExportDialog(exportViewModel)
    if (category != null) {
        val title = stringResource(category.titleRes)
        // Vaccines, allergies, medications and conditions can also be typed in.
        val kind = ManualRecordKind.ofCategory(category)
        DeclareAppBar(
            remember(title) {
                ScreenAppBar(
                    title = title,
                    actions = listOfNotNull(
                        kind?.let { AppBarAction(Icons.Outlined.Add, entryTitleRes(it, edit = false)) { onAddRecord(it) } },
                        AppBarAction(Icons.Outlined.Share, R.string.medical_export_action) {
                            exportViewModel.export(MedicalExportScope.Category(category))
                        },
                    ),
                )
            },
        )
    }
    WithHealthConnectFeatureScreen(feature = HealthConnectFeature.MEDICAL_RECORDS_BROWSE) {
        val error = state.error
        val declinedNote = if (state.readable) null else stringResource(R.string.medical_records_declined_note)
        when {
            state.isLoading && state.rows.isEmpty() -> FullScreenLoading()
            error != null -> ScreenErrorContent(error)
            state.rows.isEmpty() -> EmptyState(
                message = stringResource(R.string.medical_records_empty_category),
                icon = category?.icon,
                detail = declinedNote,
            )
            else -> LazyColumn(contentPadding = PaddingValues(vertical = Spacing.sm)) {
                if (declinedNote != null) {
                    item {
                        Text(
                            text = declinedNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.sm),
                        )
                    }
                }
                items(state.rows, key = { it.ref.toString() }) { row ->
                    OpenVitalsListRow(
                        title = row.title ?: stringResource(R.string.medical_records_untitled),
                        icon = category?.icon,
                        supporting = supportingText(row, dateTimeFormatterProvider),
                        onClick = { onOpenRecord(row.ref) },
                        modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.xs),
                    )
                }
            }
        }
    }
}

/** Date, source and status. An untitled record names its FHIR type first. */
@Composable
private fun supportingText(row: MedicalRecordRow, dateTimeFormatterProvider: DateTimeFormatterProvider): String =
    listOfNotNull(
        row.ref.resourceType.takeIf { row.title == null },
        row.date?.displayText(dateTimeFormatterProvider.mediumDate()),
        row.sourceName,
        row.status?.let { statusText(it) },
    ).joinToString(" · ")

@Composable
internal fun statusText(code: String): String = fhirStatusLabelRes(code)?.let { stringResource(it) } ?: code
