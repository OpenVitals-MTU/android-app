package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.StrideLength
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton

/** The "distance from steps" toggle and stride length. Stored in meters, shown in cm or inches. */
@Composable
internal fun StepDistanceBackfillCard(
    enabled: Boolean,
    strideLengthMeters: Double,
    unitSystem: UnitSystem,
    onSave: (enabled: Boolean, strideMeters: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draftEnabled by remember(enabled) { mutableStateOf(enabled) }
    var draftStrideDisplay by remember(strideLengthMeters, unitSystem) {
        mutableStateOf<Double?>(displayStrideForUnitSystem(strideLengthMeters, unitSystem))
    }

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.settings_step_distance_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_step_distance_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Switch(
                    checked = draftEnabled,
                    onCheckedChange = { draftEnabled = it },
                )
            }

            StrideLengthField(
                value = draftStrideDisplay,
                suffix = strideSuffix(unitSystem),
                enabled = draftEnabled,
                onValue = { draftStrideDisplay = it },
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                OpenVitalsTonalButton(
                    onClick = {
                        val strideMeters = storedStrideMetersForUnitSystem(draftStrideDisplay, unitSystem)
                            ?: strideLengthMeters
                        onSave(draftEnabled, StrideLength.normalize(strideMeters))
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun StrideLengthField(
    value: Double?,
    suffix: String,
    enabled: Boolean,
    onValue: (Double?) -> Unit,
) {
    // Keyed on the unit, not on the value: a value key rewrote the text on every keystroke.
    var text by remember(suffix) { mutableStateOf(decimalFieldText(value)) }
    LaunchedEffect(value) {
        if (decimalFieldTextIsStale(text, value)) text = decimalFieldText(value)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { next ->
            text = filterDecimalFieldInput(next)
            onValue(parseDecimalFieldText(text))
        },
        label = { Text(stringResource(R.string.settings_step_distance_stride_label)) },
        suffix = { Text(suffix) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

internal fun displayStrideForUnitSystem(strideMeters: Double, unitSystem: UnitSystem): Double =
    when (unitSystem) {
        UnitSystem.METRIC -> strideMeters * CentimetersPerMeter
        UnitSystem.IMPERIAL -> strideMeters * InchesPerMeter
    }

internal fun storedStrideMetersForUnitSystem(display: Double?, unitSystem: UnitSystem): Double? =
    display?.let { value ->
        when (unitSystem) {
            UnitSystem.METRIC -> value / CentimetersPerMeter
            UnitSystem.IMPERIAL -> value / InchesPerMeter
        }
    }

private fun strideSuffix(unitSystem: UnitSystem): String =
    when (unitSystem) {
        UnitSystem.METRIC -> "cm"
        UnitSystem.IMPERIAL -> "in"
    }

private const val CentimetersPerMeter = 100.0
private const val InchesPerMeter = 39.3700787402
