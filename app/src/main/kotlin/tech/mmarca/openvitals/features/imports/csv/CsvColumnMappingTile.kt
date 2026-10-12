package tech.mmarca.openvitals.features.imports.csv

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientGroup
import tech.mmarca.openvitals.features.nutrition.titleRes
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

/** One CSV column's role and value-interpretation editor. */
@Composable
internal fun CsvColumnMappingTile(
    header: String,
    samples: List<String>,
    mapping: CsvColumnMapping,
    onSetRole: (CsvColumnRole, CsvImportMetric?) -> Unit,
    onSetNutrient: (NutritionNutrient) -> Unit,
    onSetInterpretation: (CsvValueInterpretation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = mapping.metric?.takeIf { mapping.isMetric }?.let { CsvMetricCatalog[it] }
    val nutrient = mapping.nutrient?.takeIf { mapping.isNutrient }
    val interpretations = spec?.interpretations ?: nutrient?.let(::csvNutrientInterpretations)

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

            CsvColumnRoleField(
                mapping = mapping,
                onSetRole = onSetRole,
                onSetNutrient = onSetNutrient,
            )

            if (interpretations != null) {
                Spacer(Modifier.height(8.dp))
                CsvColumnValueField(
                    selected = mapping.effectiveInterpretation ?: interpretations.first(),
                    choices = interpretations,
                    onSelect = onSetInterpretation,
                )
                if (mapping.effectiveInterpretation?.needsRowWeight == true) {
                    CsvColumnHint(R.string.settings_csv_import_mass_share_hint)
                }
                if (spec?.isInterval == true) {
                    CsvColumnHint(R.string.settings_csv_import_interval_end_hint)
                }
                if (nutrient != null) {
                    CsvColumnHint(R.string.settings_csv_import_nutrient_hint)
                }
            }
        }
    }
}

/** The column's role: not imported, a timestamp, a metric, or a nutrient under its section. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvColumnRoleField(
    mapping: CsvColumnMapping,
    onSetRole: (CsvColumnRole, CsvImportMetric?) -> Unit,
    onSetNutrient: (NutritionNutrient) -> Unit,
) {
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
            HorizontalDivider()
            // The nutrients under the nutrition screen's own sections; the first is "Nutrition".
            NutritionNutrientGroup.entries.forEach { group ->
                CsvRoleMenuHeader(stringResource(group.titleRes()))
                if (group == NutritionNutrientGroup.OVERVIEW) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_csv_import_role_food_name)) },
                        onClick = {
                            roleExpanded = false
                            onSetRole(CsvColumnRole.FOOD_NAME, null)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
                NutritionNutrient.entries.filter { it.group == group }.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(stringResource(choice.titleRes())) },
                        onClick = {
                            roleExpanded = false
                            onSetNutrient(choice)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

/** How the column's numbers are read: their unit, or what they derive from. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvColumnValueField(
    selected: CsvValueInterpretation,
    choices: List<CsvValueInterpretation>,
    onSelect: (CsvValueInterpretation) -> Unit,
) {
    var valueExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = valueExpanded,
        onExpandedChange = { valueExpanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = csvInterpretationLabel(selected),
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
            choices.forEach { interpretation ->
                DropdownMenuItem(
                    text = { Text(csvInterpretationLabel(interpretation)) },
                    onClick = {
                        valueExpanded = false
                        onSelect(interpretation)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun CsvColumnHint(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** A section title in the role menu. Not a choice. */
@Composable
private fun CsvRoleMenuHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun roleValueLabel(mapping: CsvColumnMapping): String = when (mapping.role) {
    CsvColumnRole.TIMESTAMP -> stringResource(R.string.settings_csv_import_role_timestamp)
    CsvColumnRole.END_TIMESTAMP -> stringResource(R.string.settings_csv_import_role_end_timestamp)
    CsvColumnRole.METRIC -> mapping.metric?.let { csvMetricLabel(it) }
        ?: stringResource(R.string.settings_csv_import_role_ignore)
    CsvColumnRole.NUTRIENT -> stringResource(
        mapping.nutrient?.titleRes() ?: R.string.settings_csv_import_role_ignore,
    )
    CsvColumnRole.FOOD_NAME -> stringResource(R.string.settings_csv_import_role_food_name)
    CsvColumnRole.IGNORE -> stringResource(R.string.settings_csv_import_role_ignore)
}
