package tech.mmarca.openvitals.features.reports

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.ReportGranularity
import tech.mmarca.openvitals.domain.model.ReportMetric
import tech.mmarca.openvitals.domain.model.ReportSection
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher

private const val PdfMimeType = "application/pdf"

@Composable
fun ReportBuilderScreen(
    onDone: () -> Unit,
    viewModel: ReportBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.HEALTH_REPORT,
        modifier = Modifier.fillMaxSize(),
    ) { ux ->
        LaunchedEffect(ux.grantedPermissions, state.selectedMetrics) {
            viewModel.refreshMissingPermissions(ux.grantedPermissions)
        }
        when (state.step) {
            ReportBuilderStep.CONFIGURE -> ReportConfigureStep(
                state = state,
                metricTitle = viewModel::metricTitle,
                onToggleMetric = viewModel::toggleMetric,
                onSelectAll = viewModel::selectAllMetrics,
                onClear = viewModel::clearMetrics,
                onSetGranularity = viewModel::setGranularity,
                onSetLookback = viewModel::setLookback,
                onSetCustomStart = viewModel::setCustomStart,
                onSetCustomEnd = viewModel::setCustomEnd,
                onBuild = viewModel::buildReport,
            )
            ReportBuilderStep.BUILDING -> ReportBuildingStep(state = state, onCancel = viewModel::cancelBuild)
            ReportBuilderStep.DONE -> ReportDoneStep(state = state, onNewReport = viewModel::newReport, onDone = onDone)
        }
    }
}

// Configure.

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReportConfigureStep(
    state: ReportBuilderState,
    metricTitle: (ReportMetric) -> String,
    onToggleMetric: (ReportMetric) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onSetGranularity: (ReportGranularity) -> Unit,
    onSetLookback: (Int?) -> Unit,
    onSetCustomStart: (LocalDate) -> Unit,
    onSetCustomEnd: (LocalDate) -> Unit,
    onBuild: () -> Unit,
) {
    val permissionLauncher = rememberHealthConnectPermissionLauncher()
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.report_metrics_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        OpenVitalsTextButton(onClick = onSelectAll) {
                            Text(stringResource(R.string.report_metrics_select_all))
                        }
                        OpenVitalsTextButton(onClick = onClear) {
                            Text(stringResource(R.string.report_metrics_clear))
                        }
                    }
                    state.metricsBySection.forEach { (section, metrics) ->
                        Text(
                            text = stringResource(section.titleRes()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                        )
                        metrics.forEach { metric ->
                            val checked = metric in state.selectedMetrics
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = checked,
                                        role = Role.Checkbox,
                                        onValueChange = { onToggleMetric(metric) },
                                    ),
                            ) {
                                Checkbox(checked = checked, onCheckedChange = null)
                                Text(
                                    text = metricTitle(metric),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            OpenVitalsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.report_granularity_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportGranularity.entries.forEach { granularity ->
                            FilterChip(
                                selected = state.granularity == granularity,
                                onClick = { onSetGranularity(granularity) },
                                label = { Text(stringResource(granularity.titleRes())) },
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.report_lookback_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportLookbackPresets.forEach { days ->
                            FilterChip(
                                selected = state.lookbackDays == days,
                                onClick = { onSetLookback(days) },
                                label = { Text(stringResource(R.string.report_lookback_days, days)) },
                            )
                        }
                        FilterChip(
                            selected = state.lookbackDays == null,
                            onClick = { onSetLookback(null) },
                            label = { Text(stringResource(R.string.report_lookback_custom)) },
                        )
                    }

                    if (state.lookbackDays == null) {
                        val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            OpenVitalsOutlinedButton(
                                onClick = { showStartPicker = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.report_custom_start_date),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                    Text(dateFormat.format(state.customStart))
                                }
                            }
                            OpenVitalsOutlinedButton(
                                onClick = { showEndPicker = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.report_custom_end_date),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                    Text(dateFormat.format(state.customEnd))
                                }
                            }
                        }
                        if (!state.customRangeValid) {
                            Text(
                                text = stringResource(R.string.report_custom_range_invalid),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        if (state.missingPermissions.isNotEmpty() && state.selectedMetrics.isNotEmpty()) {
            item {
                PermissionCallout(
                    title = stringResource(R.string.message_missing_permissions_title),
                    body = stringResource(R.string.report_permission_callout_body),
                    onGrant = { permissionLauncher.launch(state.missingPermissions) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        }

        if (state.error) {
            item {
                Text(
                    text = stringResource(R.string.report_error_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item {
            OpenVitalsButton(
                onClick = onBuild,
                enabled = state.canBuild,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.widthIn(min = 6.dp))
                Text(stringResource(R.string.report_build_action))
            }
        }
    }

    if (showStartPicker) {
        HealthDatePickerDialog(
            selectedDate = state.customStart,
            onDismiss = { showStartPicker = false },
            onConfirm = { date ->
                showStartPicker = false
                onSetCustomStart(date)
            },
        )
    }
    if (showEndPicker) {
        HealthDatePickerDialog(
            selectedDate = state.customEnd,
            onDismiss = { showEndPicker = false },
            onConfirm = { date ->
                showEndPicker = false
                onSetCustomEnd(date)
            },
        )
    }
}

// Building.

@Composable
internal fun ReportBuildingStep(
    state: ReportBuilderState,
    onCancel: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.report_building_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val progress = state.progress
                    if (progress != null && progress.total > 0) {
                        LinearProgressIndicator(
                            progress = { progress.completed.toFloat() / progress.total },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        )
                    }
                    state.progressMetricTitle?.let { title ->
                        Text(
                            text = stringResource(R.string.report_building_metric, title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    OpenVitalsOutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
    }
}

// Done.

@Composable
internal fun ReportDoneStep(
    state: ReportBuilderState,
    onNewReport: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val file = state.stagedFile
    val savedToast = stringResource(R.string.report_saved_toast)
    val saveFailedToast = stringResource(R.string.report_save_failed_toast)
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(PdfMimeType),
    ) { destination ->
        if (destination != null && file != null) {
            val copied = copyReportTo(context, file, destination)
            Toast.makeText(context, if (copied) savedToast else saveFailedToast, Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.report_done_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = file?.name.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        OpenVitalsButton(
                            onClick = { file?.let { shareReport(context, it) } },
                            enabled = file != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.widthIn(min = 6.dp))
                            Text(stringResource(R.string.report_share_action))
                        }
                        OpenVitalsOutlinedButton(
                            onClick = { file?.let { saveLauncher.launch(it.name) } },
                            enabled = file != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.widthIn(min = 6.dp))
                            Text(stringResource(R.string.report_save_action))
                        }
                    }
                    OpenVitalsTextButton(
                        onClick = onNewReport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.report_new_action))
                    }
                    OpenVitalsTextButton(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            }
        }
    }
}

/** Same intent shape as the route share. No toast: the chooser is the feedback. */
private fun shareReport(context: Context, file: File): Result<Unit> = runCatching {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = PdfMimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, file.name, uri)
    }
    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.report_share_chooser_title)),
    )
}

private fun copyReportTo(context: Context, file: File, destination: Uri): Boolean =
    runCatching {
        context.contentResolver.openOutputStream(destination)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        } ?: error("no output stream for $destination")
    }.isSuccess

private fun ReportSection.titleRes(): Int = when (this) {
    ReportSection.ACTIVITY -> R.string.onboarding_hc_category_activity
    ReportSection.SLEEP -> R.string.onboarding_hc_category_sleep
    ReportSection.NUTRITION -> R.string.onboarding_hc_category_nutrition
    ReportSection.BODY -> R.string.onboarding_category_body
    ReportSection.HEART -> R.string.section_heart
    ReportSection.VITALS -> R.string.section_vitals
    ReportSection.MINDFULNESS -> R.string.onboarding_category_mindfulness
}

private fun ReportGranularity.titleRes(): Int = when (this) {
    ReportGranularity.DAILY -> R.string.report_granularity_daily
    ReportGranularity.WEEKLY -> R.string.report_granularity_weekly
    ReportGranularity.MONTHLY -> R.string.report_granularity_monthly
}
