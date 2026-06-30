package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

@Composable
fun BodyEnergyCalibrationCard(
    calibration: BodyEnergyCalibration,
    modifier: Modifier = Modifier,
    showSkipAction: Boolean = false,
    onSave: (BodyEnergyCalibration) -> Unit,
    onUseAutomatic: () -> Unit,
    onSkip: () -> Unit = {},
) {
    val signature = calibration.signature()
    var birthYear by rememberSaveable(signature) { mutableStateOf(calibration.birthYear?.toString().orEmpty()) }
    var maxHeartRate by rememberSaveable(signature) {
        mutableStateOf(calibration.manualMaxHeartRateBpm?.toString().orEmpty())
    }
    var restingHeartRate by rememberSaveable(signature) {
        mutableStateOf(calibration.manualRestingHeartRateBpm?.toString().orEmpty())
    }
    var useManualZones by rememberSaveable(signature) { mutableStateOf(calibration.useManualZones) }
    var zone1 by rememberSaveable(signature) {
        mutableStateOf(calibration.manualZoneThresholdsBpm?.zone1LowerBpm?.toString().orEmpty())
    }
    var zone2 by rememberSaveable(signature) {
        mutableStateOf(calibration.manualZoneThresholdsBpm?.zone2LowerBpm?.toString().orEmpty())
    }
    var zone3 by rememberSaveable(signature) {
        mutableStateOf(calibration.manualZoneThresholdsBpm?.zone3LowerBpm?.toString().orEmpty())
    }
    var zone4 by rememberSaveable(signature) {
        mutableStateOf(calibration.manualZoneThresholdsBpm?.zone4LowerBpm?.toString().orEmpty())
    }
    var zone5 by rememberSaveable(signature) {
        mutableStateOf(calibration.manualZoneThresholdsBpm?.zone5LowerBpm?.toString().orEmpty())
    }

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.body_energy_calibration_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.body_energy_calibration_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.body_energy_calibration_optional_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            CalibrationNumberField(
                value = birthYear,
                onValueChange = { birthYear = it },
                label = stringResource(R.string.body_energy_calibration_birth_year),
            )
            CalibrationNumberField(
                value = maxHeartRate,
                onValueChange = { maxHeartRate = it },
                label = stringResource(R.string.body_energy_calibration_max_hr),
            )
            CalibrationNumberField(
                value = restingHeartRate,
                onValueChange = { restingHeartRate = it },
                label = stringResource(R.string.body_energy_calibration_resting_hr),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.body_energy_calibration_manual_zones),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.body_energy_calibration_manual_zones_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = useManualZones,
                    onCheckedChange = { useManualZones = it },
                )
            }

            if (useManualZones) {
                CalibrationNumberField(
                    value = zone1,
                    onValueChange = { zone1 = it },
                    label = stringResource(R.string.body_energy_calibration_zone_1),
                )
                CalibrationNumberField(
                    value = zone2,
                    onValueChange = { zone2 = it },
                    label = stringResource(R.string.body_energy_calibration_zone_2),
                )
                CalibrationNumberField(
                    value = zone3,
                    onValueChange = { zone3 = it },
                    label = stringResource(R.string.body_energy_calibration_zone_3),
                )
                CalibrationNumberField(
                    value = zone4,
                    onValueChange = { zone4 = it },
                    label = stringResource(R.string.body_energy_calibration_zone_4),
                )
                CalibrationNumberField(
                    value = zone5,
                    onValueChange = { zone5 = it },
                    label = stringResource(R.string.body_energy_calibration_zone_5),
                )
            }

            OpenVitalsButton(
                onClick = {
                    onSave(
                        BodyEnergyCalibration(
                            birthYear = birthYear.toOptionalInt(),
                            manualMaxHeartRateBpm = maxHeartRate.toOptionalInt(),
                            manualRestingHeartRateBpm = restingHeartRate.toOptionalInt(),
                            manualZoneThresholdsBpm = HeartZoneThresholds(
                                zone1LowerBpm = zone1.toOptionalInt() ?: 0,
                                zone2LowerBpm = zone2.toOptionalInt() ?: 0,
                                zone3LowerBpm = zone3.toOptionalInt() ?: 0,
                                zone4LowerBpm = zone4.toOptionalInt() ?: 0,
                                zone5LowerBpm = zone5.toOptionalInt() ?: 0,
                            ),
                            useManualZones = useManualZones,
                        ).normalized()
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
            OpenVitalsOutlinedButton(
                onClick = onUseAutomatic,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.body_energy_calibration_use_auto))
            }
            if (showSkipAction) {
                OpenVitalsTextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.body_energy_calibration_skip))
                }
            }
        }
    }
}

@Composable
private fun CalibrationNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter(Char::isDigit).take(4)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun String.toOptionalInt(): Int? =
    trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
