package tech.mmarca.openvitals.features.imports.csv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.StepBar

/**
 * The two steps a drip export takes in place of the column mapping: what to
 * import, then a confirmation. The importing and result steps are shared.
 */

/** One switch per [DripImportOption], and what each brings over. */
@Composable
internal fun DripOptionsStep(
    state: CsvImportState,
    viewModel: CsvImportViewModel,
) {
    val drip = state.drip ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_csv_import_drip_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.settings_csv_import_drip_body, state.fileName.orEmpty()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        DripImportOption.entries.forEach { option ->
                            DripOptionRow(
                                option = option,
                                checked = option in drip.enabled,
                                onCheckedChange = { viewModel.setDripOption(option, it) },
                            )
                        }
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
private fun DripOptionRow(
    option: DripImportOption,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dripOptionLabel(option),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = dripOptionDescription(option),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** The confirm step for a drip export: the sampled days and their span, and the permissions. */
@Composable
internal fun DripConfirmStep(
    state: CsvImportState,
    viewModel: CsvImportViewModel,
    onGrantPermissions: () -> Unit,
) {
    val drip = state.drip ?: return
    val sample = state.sample ?: return
    val missing = state.missingPermissions
    val daysWithContent = remember(sample, drip) { dripSampleDaysWithContent(sample.dataRows, drip) }
    val range = remember(sample, drip) { dripSampleDateRange(sample.dataRows, drip) }
    val locale = LocalLocale.current.platformLocale
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale) }

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
                                R.string.settings_csv_import_drip_confirm_summary,
                                state.fileName.orEmpty(),
                                daysWithContent,
                                sample.dataRows.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        if (range != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(
                                    R.string.settings_csv_import_confirm_dates,
                                    format.format(range.first),
                                    format.format(range.second),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        DripImportOption.entries.filter { it in drip.enabled }.forEach { option ->
                            Text(
                                text = dripOptionLabel(option),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
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

@Composable
internal fun dripOptionLabel(option: DripImportOption): String = stringResource(
    when (option) {
        DripImportOption.BLEEDING -> R.string.settings_csv_import_drip_bleeding
        DripImportOption.TEMPERATURE -> R.string.settings_csv_import_drip_temperature
        DripImportOption.CERVICAL_MUCUS -> R.string.settings_csv_import_drip_mucus
        DripImportOption.SEXUAL_ACTIVITY -> R.string.settings_csv_import_drip_sex
        DripImportOption.JOURNAL -> R.string.settings_csv_import_drip_journal
    },
)

@Composable
private fun dripOptionDescription(option: DripImportOption): String = stringResource(
    when (option) {
        DripImportOption.BLEEDING -> R.string.settings_csv_import_drip_bleeding_body
        DripImportOption.TEMPERATURE -> R.string.settings_csv_import_drip_temperature_body
        DripImportOption.CERVICAL_MUCUS -> R.string.settings_csv_import_drip_mucus_body
        DripImportOption.SEXUAL_ACTIVITY -> R.string.settings_csv_import_drip_sex_body
        DripImportOption.JOURNAL -> R.string.settings_csv_import_drip_journal_body
    },
)
