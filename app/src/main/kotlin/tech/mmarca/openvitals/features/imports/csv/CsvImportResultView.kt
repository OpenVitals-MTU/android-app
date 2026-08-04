package tech.mmarca.openvitals.features.imports.csv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.StepBar

/** The finished-import view: the tally, why rows were rejected, and what to do next. */
@Composable
internal fun CsvImportResultView(
    result: CsvImportResult,
    onCopyReport: () -> Unit,
    onSaveReport: () -> Unit,
    onImportAnother: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            item {
                OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(
                                R.string.settings_csv_import_result,
                                result.progress.written,
                                result.progress.alreadyPresent,
                                result.progress.rejected,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (result.wroteNothing) {
                            Text(
                                text = stringResource(R.string.settings_csv_import_result_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        if (result.outcome == CsvImportOutcome.CANCELLED) {
                            Text(
                                text = stringResource(R.string.settings_csv_import_result_cancelled),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        if (result.outcome == CsvImportOutcome.RATE_LIMITED) {
                            Text(
                                text = stringResource(R.string.settings_csv_import_result_rate_limited),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        if (result.outcome == CsvImportOutcome.FAILED && result.error != null) {
                            Text(
                                text = stringResource(R.string.settings_csv_import_error, result.error),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }

            if (result.diagnosticCounts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.settings_csv_import_diagnostics_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(Modifier.height(8.dp))
                            result.diagnosticCounts.forEach { (reason, count) ->
                                Text(
                                    text = stringResource(
                                        R.string.settings_csv_import_diagnostic_line,
                                        csvDiagnosticReasonLabel(reason),
                                        count,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Row {
                    OpenVitalsOutlinedButton(
                        onClick = onCopyReport,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_csv_import_copy_report))
                    }
                    Spacer(Modifier.width(8.dp))
                    OpenVitalsOutlinedButton(
                        onClick = onSaveReport,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SaveAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_csv_import_save_report))
                    }
                }
            }
        }
        StepBar(
            nextLabel = stringResource(R.string.settings_csv_import_done),
            onNext = onDone,
            backLabel = stringResource(R.string.settings_csv_import_import_another),
            onBack = onImportAnother,
        )
    }
}
