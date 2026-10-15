package tech.mmarca.openvitals.features.imports.csv

import androidx.annotation.StringRes
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

/**
 * The finished-import view: the tally, why rows were rejected, and what to do
 * next. The tally keeps rows, records and journal days apart, one number per
 * line: a row can produce several records or none, so the counts do not add up
 * across sections and are not laid out as if they did.
 */
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
                            text = stringResource(outcomeTitleRes(result.outcome)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        CsvImportTally(progress = result.progress)
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

@StringRes
private fun outcomeTitleRes(outcome: CsvImportOutcome): Int = when (outcome) {
    CsvImportOutcome.COMPLETED -> R.string.settings_csv_import_outcome_completed
    CsvImportOutcome.CANCELLED -> R.string.settings_csv_import_outcome_cancelled
    CsvImportOutcome.RATE_LIMITED -> R.string.settings_csv_import_outcome_rate_limited
    CsvImportOutcome.FAILED -> R.string.settings_csv_import_outcome_failed
}

/** Rows, records and, for a drip export, journal days, each as a labelled column of numbers. */
@Composable
private fun CsvImportTally(progress: CsvImportProgress) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        TallyTitle(stringResource(R.string.settings_csv_import_tally_rows_title))
        TallyRow(stringResource(R.string.settings_csv_import_tally_rows_read), progress.rowsRead)
        TallyRow(stringResource(R.string.settings_csv_import_tally_rows_imported), progress.rowsImported)
        TallyRow(stringResource(R.string.settings_csv_import_tally_rows_rejected), progress.rowsRejected)
        TallyRow(stringResource(R.string.settings_csv_import_tally_rows_empty), progress.emptyRows)
        TallyNote(stringResource(R.string.settings_csv_import_tally_rows_note))

        Spacer(Modifier.height(12.dp))
        TallyTitle(stringResource(R.string.settings_csv_import_tally_records_title))
        TallyRow(stringResource(R.string.settings_csv_import_tally_records_written), progress.written)
        TallyRow(stringResource(R.string.settings_csv_import_tally_records_replaced), progress.alreadyPresent)
        if (progress.presenceUnchecked > 0) {
            TallyRow(stringResource(R.string.settings_csv_import_tally_records_unchecked), progress.presenceUnchecked)
            TallyNote(stringResource(R.string.settings_csv_import_tally_records_unchecked_note))
        }

        if (progress.journalDays > 0) {
            Spacer(Modifier.height(12.dp))
            TallyTitle(stringResource(R.string.settings_csv_import_tally_journal_title))
            TallyRow(stringResource(R.string.settings_csv_import_tally_journal_days), progress.journalDays)
            TallyRow(stringResource(R.string.settings_csv_import_tally_journal_updated), progress.journalDaysUpdated)
        }
    }
}

@Composable
private fun TallyTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun TallyRow(label: String, value: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = value.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TallyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}
