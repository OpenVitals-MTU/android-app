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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

/** The date/time format, time-zone and live-echo controls for the mapping step. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CsvDateTimeSection(
    sample: CsvSample,
    mapping: CsvImportMapping,
    onSettingsChange: (CsvDateTimeSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = mapping.dateTime
    val timestampColumn = mapping.timestampColumn

    val firstValue = timestampColumn?.let { sample.columnValues(it.columnIndex).firstOrNull() }
    val fromFile = firstValue != null && csvTimestampHasExplicitOffset(firstValue)
    val resolved = firstValue?.let { resolveCsvInstant(it, settings) }

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_csv_import_date_time_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))

            var formatExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = formatExpanded,
                onExpandedChange = { formatExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = csvDateFormatLabel(settings.format),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_csv_import_date_format_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = formatExpanded,
                    onDismissRequest = { formatExpanded = false },
                ) {
                    CsvDateTimeFormat.entries.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(csvDateFormatLabel(format)) },
                            onClick = {
                                formatExpanded = false
                                onSettingsChange(settings.copy(format = format))
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            if (settings.format == CsvDateTimeFormat.CUSTOM) {
                Spacer(Modifier.height(8.dp))
                var pattern by rememberSaveable { mutableStateOf(settings.customPattern.orEmpty()) }
                OutlinedTextField(
                    value = pattern,
                    onValueChange = {
                        pattern = it
                        onSettingsChange(settings.copy(customPattern = it))
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_csv_import_date_format_custom)) },
                    placeholder = { Text(stringResource(R.string.settings_csv_import_date_format_custom_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(8.dp))
            if (fromFile) {
                Text(
                    text = stringResource(R.string.settings_csv_import_time_zone_from_file),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                var zoneExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = zoneExpanded,
                    onExpandedChange = { zoneExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = csvTimeZoneLabel(settings.zone),
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.settings_csv_import_time_zone_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = zoneExpanded,
                        onDismissRequest = { zoneExpanded = false },
                    ) {
                        CsvTimeZoneMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(csvTimeZoneLabel(mode)) },
                                onClick = {
                                    zoneExpanded = false
                                    onSettingsChange(settings.copy(zone = mode))
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }

            if (!fromFile && settings.zone == CsvTimeZoneMode.FIXED_OFFSET) {
                Spacer(Modifier.height(8.dp))
                var offsetText by rememberSaveable { mutableStateOf("") }
                OutlinedTextField(
                    value = offsetText,
                    onValueChange = { value ->
                        offsetText = value
                        parseOffset(value)?.let { onSettingsChange(settings.copy(fixedOffset = it)) }
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_csv_import_time_zone_offset_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // The live echo: a dd/MM mistake is obvious here, before anything is written.
            if (resolved != null) {
                Spacer(Modifier.height(12.dp))
                val locale = LocalLocale.current.platformLocale
                val formatter = remember(locale) {
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withLocale(locale)
                }
                val wallClock = LocalDateTime.ofInstant(resolved.utc, resolved.offset)
                Text(
                    text = stringResource(
                        R.string.settings_csv_import_example_row,
                        formatter.format(wallClock),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private val OffsetRegex = Regex("""^([+-]?)(\d{1,2}):?(\d{2})$""")

private fun parseOffset(value: String): ZoneOffset? {
    val match = OffsetRegex.find(value.trim()) ?: return null
    val sign = if (match.groupValues[1] == "-") -1 else 1
    val hours = match.groupValues[2].toIntOrNull() ?: return null
    val minutes = match.groupValues[3].toIntOrNull() ?: return null
    return runCatching {
        ZoneOffset.ofHoursMinutes(sign * hours, sign * minutes)
    }.getOrNull()
}
