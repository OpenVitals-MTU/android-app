package tech.mmarca.openvitals.features.imports.csv

import android.content.ClipData
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsFilledButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.StepBar
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher

private val CsvMimeTypes = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/csv",
    "application/octet-stream",
)

/**
 * The CSV importer: pick, map, confirm, import, result. A pushed route,
 * since a mapping editor cannot live in a settings card list. Permissions
 * are asked at the confirm step, once the mapping says which.
 */
@Composable
fun CsvImportScreen(
    onDone: () -> Unit,
    viewModel: CsvImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        // Cancelling the picker is not an error.
        if (uri != null) viewModel.pickFile(uri)
    }

    val permissionLauncher = rememberHealthConnectPermissionLauncher(
        onResult = { viewModel.refreshPermissions() },
    )

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.CSV_IMPORT,
        modifier = Modifier.fillMaxSize(),
    ) { _ ->
        when (state.step) {
            CsvImportStep.PICK -> CsvPickStep(
                state = state,
                onPick = { filePicker.launch(CsvMimeTypes) },
            )
            CsvImportStep.MAPPING -> CsvMappingStep(state = state, viewModel = viewModel)
            CsvImportStep.CONFIRM -> CsvConfirmStep(
                state = state,
                viewModel = viewModel,
                onGrantPermissions = { permissionLauncher.launch(state.missingPermissions) },
            )
            CsvImportStep.IMPORTING -> CsvImportingStep(state = state, onCancel = viewModel::cancelImport)
            CsvImportStep.DONE -> CsvDoneStep(state = state, viewModel = viewModel, onDone = onDone)
        }
    }
}

@Composable
internal fun CsvPickStep(
    state: CsvImportState,
    onPick: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_csv_import_pick_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_csv_import_pick_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    state.error?.let { message ->
                        Text(
                            text = stringResource(R.string.settings_csv_import_unreadable_file, message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    if (state.isLoadingFile) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        )
                        Text(
                            text = stringResource(R.string.settings_csv_import_loading),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    OpenVitalsFilledButton(
                        onClick = onPick,
                        enabled = !state.isLoadingFile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(stringResource(R.string.settings_csv_import_pick_action))
                    }
                }
            }
        }
    }
}

@Composable
internal fun CsvMappingStep(
    state: CsvImportState,
    viewModel: CsvImportViewModel,
) {
    val sample = state.sample
    val mapping = state.mapping
    if (sample == null || sample.isEmpty || mapping == null) {
        CsvEmptyFileBody(onBack = viewModel::reset)
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(
                        R.string.settings_csv_import_file_label,
                        state.fileName.orEmpty(),
                        sample.columnCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                CsvDialectCard(
                    sample = sample,
                    onDialectChange = { dialect, hasHeaderRow ->
                        viewModel.setDialect(dialect, hasHeaderRow)
                    },
                )
                Spacer(Modifier.height(12.dp))
                CsvPreviewTable(sample = sample)
                Spacer(Modifier.height(12.dp))
                CsvDateTimeSection(
                    sample = sample,
                    mapping = mapping,
                    onSettingsChange = viewModel::setDateTimeSettings,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_csv_import_columns_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
            }
            items(count = sample.columnCount) { index ->
                CsvColumnMappingTile(
                    header = sample.headerRow[index],
                    samples = sample.columnValues(index).take(3),
                    mapping = mapping.columns.firstOrNull { it.columnIndex == index }
                        ?: CsvColumnMapping(columnIndex = index),
                    onSetRole = { role, metric -> viewModel.setColumnRole(index, role, metric) },
                    onSetInterpretation = { viewModel.setColumnInterpretation(index, it) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (state.issues.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    state.issues.forEach { issue ->
                        Text(
                            text = csvIssueLabel(issue),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }
        }
        StepBar(
            nextLabel = stringResource(R.string.settings_csv_import_continue),
            onNext = if (state.canContinue) {
                { viewModel.goToStep(CsvImportStep.CONFIRM) }
            } else {
                null
            },
            backLabel = stringResource(R.string.settings_csv_import_back),
            onBack = viewModel::reset,
        )
    }
}

@Composable
internal fun CsvEmptyFileBody(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.settings_csv_import_empty_file),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        OpenVitalsOutlinedButton(onClick = onBack) {
            Text(stringResource(R.string.settings_csv_import_back))
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun CsvConfirmStep(
    state: CsvImportState,
    viewModel: CsvImportViewModel,
    onGrantPermissions: () -> Unit,
) {
    val mapping = state.mapping ?: return
    val sample = state.sample ?: return
    val missing = state.missingPermissions

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_csv_import_confirm_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_csv_import_confirm_summary,
                                state.fileName.orEmpty(),
                                mapping.metricColumns.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        // The date span is the last guard against a day/month mix-up.
                        CsvDateRangeLine(sample = sample, mapping = mapping)
                        // The per-metric range catches a bad derivation as 3% or 150%.
                        mapping.metricColumns.forEach { column ->
                            CsvMetricRangeLine(column = column, sample = sample, mapping = mapping)
                        }
                    }
                }
                if (missing.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    PermissionCallout(
                        title = stringResource(R.string.settings_csv_import_permission_title),
                        body = stringResource(R.string.settings_csv_import_permission_body),
                        onGrant = onGrantPermissions,
                    )
                }
            }
        }
        StepBar(
            nextLabel = stringResource(R.string.settings_csv_import_start),
            onNext = viewModel::startImport,
            backLabel = stringResource(R.string.settings_csv_import_back),
            onBack = { viewModel.goToStep(CsvImportStep.MAPPING) },
        )
    }
}

/** The first and last sampled dates in the locale's long format, so the interpretation is legible. */
@Composable
private fun CsvDateRangeLine(sample: CsvSample, mapping: CsvImportMapping) {
    val range = previewInstantRange(rows = sample.dataRows, mapping = mapping) ?: return
    val locale: Locale = LocalLocale.current.platformLocale
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
    Text(
        text = stringResource(
            R.string.settings_csv_import_confirm_dates,
            format.format(range.first),
            format.format(range.second),
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** One metric's observed range across the sample, in its canonical unit. */
@Composable
private fun CsvMetricRangeLine(
    column: CsvColumnMapping,
    sample: CsvSample,
    mapping: CsvImportMapping,
) {
    val metric = column.metric ?: return
    val values = previewCanonicalValues(rows = sample.dataRows, mapping = mapping, metric = metric)
    if (values.isEmpty()) return
    val sorted = values.sorted()

    Text(
        text = stringResource(
            R.string.settings_csv_import_confirm_range,
            csvMetricLabel(metric),
            String.format(Locale.US, "%.1f", sorted.first()),
            String.format(Locale.US, "%.1f", sorted.last()),
        ),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun CsvImportingStep(
    state: CsvImportState,
    onCancel: () -> Unit,
) {
    val progress = state.progress ?: CsvImportProgress()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        val fraction = progress.fraction
        if (fraction != null) {
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.settings_csv_import_progress,
                progress.rowsRead,
                progress.written,
                progress.alreadyPresent,
                progress.rejected,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.weight(1f))
        OpenVitalsOutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_csv_import_cancel))
        }
    }
}

@Composable
private fun CsvDoneStep(
    state: CsvImportState,
    viewModel: CsvImportViewModel,
    onDone: () -> Unit,
) {
    val result = state.result ?: return
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val reportCopied = stringResource(R.string.settings_csv_import_report_copied)
    val reportSaved = stringResource(R.string.settings_csv_import_report_saved)
    val reportSaveFailed = stringResource(R.string.settings_csv_import_report_save_failed)

    val reportSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            val text = viewModel.reportText() ?: return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(text.toByteArray())
                } ?: error("Unable to open destination.")
            }.fold(
                onSuccess = { Toast.makeText(context, reportSaved, Toast.LENGTH_SHORT).show() },
                onFailure = { Toast.makeText(context, reportSaveFailed, Toast.LENGTH_SHORT).show() },
            )
        }
    }

    CsvImportResultView(
        result = result,
        onCopyReport = {
            val text = viewModel.reportText() ?: return@CsvImportResultView
            scope.launch {
                clipboard.setClipEntry(ClipData.newPlainText("OpenVitals", text).toClipEntry())
                Toast.makeText(context, reportCopied, Toast.LENGTH_SHORT).show()
            }
        },
        onSaveReport = { reportSaver.launch(CSV_IMPORT_REPORT_FILE_NAME) },
        onImportAnother = viewModel::reset,
        onDone = onDone,
        modifier = Modifier.fillMaxSize(),
    )
}
