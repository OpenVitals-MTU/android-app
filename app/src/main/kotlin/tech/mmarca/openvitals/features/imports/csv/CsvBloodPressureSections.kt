package tech.mmarca.openvitals.features.imports.csv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.util.Locale
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.BpBodyPositionLabels
import tech.mmarca.openvitals.core.presentation.BpMeasurementLocationLabels
import tech.mmarca.openvitals.domain.model.BpRecordValues
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OptionDropdown
import tech.mmarca.openvitals.ui.theme.Spacing

private val BodyPositions = BpBodyPositionLabels.keys.toList()

private val CuffLocations = BpMeasurementLocationLabels.keys.toList()

/** The body position and cuff location a reading takes when its file does not say. Mapping step only. */
@Composable
internal fun CsvBloodPressureDefaultsCard(
    settings: CsvBloodPressureSettings,
    onBodyPositionChange: (Int) -> Unit,
    onCuffLocationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = stringResource(R.string.settings_csv_import_bp_defaults_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.settings_csv_import_bp_defaults_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            OptionDropdown(
                label = stringResource(R.string.vitals_entry_bp_position_label),
                options = BodyPositions,
                selected = settings.defaultBodyPosition.takeIf { it in BpBodyPositionLabels },
                optionText = { stringResource(BpBodyPositionLabels.getValue(it)) },
                enabled = true,
                onSelect = { onBodyPositionChange(it ?: BpRecordValues.BODY_POSITION_UNKNOWN) },
            )
            Spacer(Modifier.height(Spacing.sm))
            OptionDropdown(
                label = stringResource(R.string.vitals_entry_bp_location_label),
                options = CuffLocations,
                selected = settings.defaultCuffLocation.takeIf { it in BpMeasurementLocationLabels },
                optionText = { stringResource(BpMeasurementLocationLabels.getValue(it)) },
                enabled = true,
                onSelect = { onCuffLocationChange(it ?: BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN) },
            )
        }
    }
}

/** The systolic and diastolic ranges across the sample, as they will be written. Confirm step only. */
@Composable
internal fun CsvBloodPressureRangeLines(sample: CsvSample, mapping: CsvImportMapping) {
    val readings = remember(sample, mapping) { previewBloodPressureValues(rows = sample.dataRows, mapping = mapping) }
    if (readings.isEmpty()) return

    CsvBloodPressureRangeLine(CsvBloodPressurePart.SYSTOLIC, readings.map { it.first })
    CsvBloodPressureRangeLine(CsvBloodPressurePart.DIASTOLIC, readings.map { it.second })
}

@Composable
private fun CsvBloodPressureRangeLine(part: CsvBloodPressurePart, values: List<Double>) {
    Text(
        text = stringResource(
            R.string.settings_csv_import_confirm_range,
            csvBloodPressurePartLabel(part),
            String.format(Locale.US, "%.0f", values.min()),
            String.format(Locale.US, "%.0f", values.max()),
        ),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = Spacing.xs),
    )
}
