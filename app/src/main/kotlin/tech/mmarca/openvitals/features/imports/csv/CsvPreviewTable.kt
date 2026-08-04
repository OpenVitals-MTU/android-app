package tech.mmarca.openvitals.features.imports.csv

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

/** The separator/header controls for the mapping step. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CsvDialectCard(
    sample: CsvSample,
    onDialectChange: (CsvDialect, Boolean?) -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = csvSeparatorLabel(sample.dialect.fieldDelimiter),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_csv_import_separator_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    CsvFieldDelimiters.forEach { delimiter ->
                        DropdownMenuItem(
                            text = { Text(csvSeparatorLabel(delimiter)) },
                            onClick = {
                                expanded = false
                                onDialectChange(sample.dialect.copy(fieldDelimiter = delimiter), null)
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_csv_import_has_header_label),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = sample.hasHeaderRow,
                    onCheckedChange = { onDialectChange(sample.dialect, it) },
                )
            }
        }
    }
}

/** The sample preview table. Scrolls inside itself; the page never scrolls sideways. */
@Composable
internal fun CsvPreviewTable(
    sample: CsvSample,
    modifier: Modifier = Modifier,
) {
    val rows = sample.dataRows.take(5)

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_csv_import_preview_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .width(IntrinsicSize.Max),
            ) {
                Row {
                    sample.headerRow.forEach { header ->
                        Text(
                            text = header,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(min = 72.dp, max = 160.dp)
                                .padding(end = 12.dp, bottom = 4.dp),
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                rows.forEach { row ->
                    Row {
                        for (index in 0 until sample.columnCount) {
                            Text(
                                text = row.getOrNull(index).orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(min = 72.dp, max = 160.dp)
                                    .padding(end = 12.dp, top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
