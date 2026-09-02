package tech.mmarca.openvitals.features.imports.csv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

/** One CSV column's role and value-interpretation editor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CsvColumnMappingTile(
    header: String,
    samples: List<String>,
    mapping: CsvColumnMapping,
    onSetRole: (CsvColumnRole, CsvImportMetric?) -> Unit,
    onSetInterpretation: (CsvValueInterpretation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = mapping.metric?.let { CsvMetricCatalog[it] }

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(header, style = MaterialTheme.typography.titleSmall)
            if (samples.isNotEmpty()) {
                Text(
                    text = samples.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))

            var roleExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = roleValueLabel(mapping),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = roleExpanded,
                    onDismissRequest = { roleExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_csv_import_role_ignore)) },
                        onClick = {
                            roleExpanded = false
                            onSetRole(CsvColumnRole.IGNORE, null)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_csv_import_role_timestamp)) },
                        onClick = {
                            roleExpanded = false
                            onSetRole(CsvColumnRole.TIMESTAMP, null)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_csv_import_role_end_timestamp)) },
                        onClick = {
                            roleExpanded = false
                            onSetRole(CsvColumnRole.END_TIMESTAMP, null)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    CsvImportMetric.entries.forEach { metric ->
                        DropdownMenuItem(
                            text = { Text(csvMetricLabel(metric)) },
                            onClick = {
                                roleExpanded = false
                                onSetRole(CsvColumnRole.METRIC, metric)
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            if (spec != null) {
                Spacer(Modifier.height(8.dp))
                var valueExpanded by remember { mutableStateOf(false) }
                val effective = mapping.effectiveInterpretation ?: spec.defaultInterpretation
                ExposedDropdownMenuBox(
                    expanded = valueExpanded,
                    onExpandedChange = { valueExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = csvInterpretationLabel(effective),
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.settings_csv_import_value_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = valueExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = valueExpanded,
                        onDismissRequest = { valueExpanded = false },
                    ) {
                        spec.interpretations.forEach { interpretation ->
                            DropdownMenuItem(
                                text = { Text(csvInterpretationLabel(interpretation)) },
                                onClick = {
                                    valueExpanded = false
                                    onSetInterpretation(interpretation)
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
                if (mapping.effectiveInterpretation?.needsRowWeight == true) {
                    Text(
                        text = stringResource(R.string.settings_csv_import_mass_share_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (spec.isInterval) {
                    Text(
                        text = stringResource(R.string.settings_csv_import_interval_end_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun roleValueLabel(mapping: CsvColumnMapping): String = when (mapping.role) {
    CsvColumnRole.TIMESTAMP -> stringResource(R.string.settings_csv_import_role_timestamp)
    CsvColumnRole.END_TIMESTAMP -> stringResource(R.string.settings_csv_import_role_end_timestamp)
    CsvColumnRole.METRIC -> mapping.metric?.let { csvMetricLabel(it) }
        ?: stringResource(R.string.settings_csv_import_role_ignore)
    CsvColumnRole.IGNORE -> stringResource(R.string.settings_csv_import_role_ignore)
}
