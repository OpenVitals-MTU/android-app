package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Locale
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton

/**
 * The Body Energy setup gate and settings card: the heart-zone ladder, the birth
 * year automatic zones are derived from, and any personal gain tuning.
 *
 * v11 removed the manual maximum and resting heart rate boxes. Both are measured
 * from the user's own data now, and a typed maximum used to be the ONLY route to
 * high confidence — which had it backwards, since an observed maximum has to
 * clear a plausibility floor before the model will use it and a typed one
 * cleared nothing.
 *
 * What is left needs the birth year: with no typed maximum, Tanaka-from-age is
 * the only estimate, and without it the model falls back to resting + 70, which
 * for a resting 60 claims a maximum of 130 and reads a brisk walk as zone 5. So
 * Save refuses rather than producing a confidently wrong score.
 */
@Composable
fun BodyEnergyCalibrationCard(
    calibration: BodyEnergyCalibration,
    bodyProfile: BodyProfile,
    modifier: Modifier = Modifier,
    showBirthYear: Boolean = true,
    showSkipAction: Boolean = false,
    onSave: (BodyEnergyCalibration, Int?) -> Unit,
    onResetPersonalTuning: () -> Unit,
    onSkip: () -> Unit = {},
) {
    val signature = calibration.zoneSignature()
    var useManualZones by rememberSaveable(signature) { mutableStateOf(calibration.useManualZones) }
    var zone1 by rememberSaveable(signature) { mutableStateOf(calibration.zoneText { it.zone1LowerBpm }) }
    var zone2 by rememberSaveable(signature) { mutableStateOf(calibration.zoneText { it.zone2LowerBpm }) }
    var zone3 by rememberSaveable(signature) { mutableStateOf(calibration.zoneText { it.zone3LowerBpm }) }
    var zone4 by rememberSaveable(signature) { mutableStateOf(calibration.zoneText { it.zone4LowerBpm }) }
    var zone5 by rememberSaveable(signature) { mutableStateOf(calibration.zoneText { it.zone5LowerBpm }) }
    var birthYear by rememberSaveable(bodyProfile.birthYear) {
        mutableStateOf(bodyProfile.birthYear?.toString().orEmpty())
    }
    var birthYearMissing by rememberSaveable { mutableStateOf(false) }

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

            if (showBirthYear) {
                CalibrationNumberField(
                    value = birthYear,
                    onValueChange = {
                        birthYear = it
                        birthYearMissing = false
                    },
                    label = stringResource(R.string.body_energy_calibration_birth_year),
                )
            }
            if (birthYearMissing) {
                Text(
                    text = stringResource(R.string.body_energy_calibration_birth_year_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

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
                    onCheckedChange = {
                        useManualZones = it
                        birthYearMissing = false
                    },
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

            BodyEnergyPersonalTuningSection(
                calibration = calibration,
                onReset = onResetPersonalTuning,
            )

            OpenVitalsButton(
                onClick = {
                    val typedBirthYear = birthYear.toOptionalInt()
                    // Manual zones ARE the ladder, so they need no age. Automatic
                    // zones cannot be derived without one.
                    val missing = !useManualZones && !isUsableBirthYear(typedBirthYear, showBirthYear, bodyProfile)
                    birthYearMissing = missing
                    if (missing) return@OpenVitalsButton
                    onSave(
                        BodyEnergyCalibration(
                            manualZoneThresholdsBpm = HeartZoneThresholds(
                                zone1LowerBpm = zone1.toOptionalInt() ?: 0,
                                zone2LowerBpm = zone2.toOptionalInt() ?: 0,
                                zone3LowerBpm = zone3.toOptionalInt() ?: 0,
                                zone4LowerBpm = zone4.toOptionalInt() ?: 0,
                                zone5LowerBpm = zone5.toOptionalInt() ?: 0,
                            ),
                            useManualZones = useManualZones,
                            sleepChargeGain = calibration.sleepChargeGain,
                            activityDrainGain = calibration.activityDrainGain,
                            basalDrainGain = calibration.basalDrainGain,
                            stressDrainGain = calibration.stressDrainGain,
                        ).normalized(),
                        typedBirthYear.takeIf { showBirthYear },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
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

/**
 * The personal gains, as four plain multipliers.
 *
 * Shown only when they differ from neutral: the retired watch integration was
 * the sole learner, so an install that never fitted them has nothing to show
 * or reset, while one that did keeps its tuning visible and reversible.
 */
@Composable
private fun BodyEnergyPersonalTuningSection(
    calibration: BodyEnergyCalibration,
    onReset: () -> Unit,
) {
    if (calibration.hasPersonalGains) {
        HorizontalDivider()
        Text(
            text = stringResource(R.string.body_energy_personalization_title),
            style = MaterialTheme.typography.bodyMedium,
        )
        GainRow(
            label = stringResource(R.string.body_energy_gain_activity),
            value = calibration.activityDrainGain,
        )
        GainRow(
            label = stringResource(R.string.body_energy_gain_sleep),
            value = calibration.sleepChargeGain,
        )
        GainRow(
            label = stringResource(R.string.body_energy_gain_basal),
            value = calibration.basalDrainGain,
        )
        GainRow(
            label = stringResource(R.string.body_energy_gain_stress),
            value = calibration.stressDrainGain,
        )
        OpenVitalsTextButton(onClick = onReset) {
            Text(stringResource(R.string.body_energy_personalization_reset))
        }
    }
}

@Composable
private fun GainRow(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = String.format(LocalLocale.current.platformLocale, "%.2f×", value),
            style = MaterialTheme.typography.bodySmall,
        )
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

/**
 * Whether automatic zones have an age to work from. Where this card does not own
 * the field, the stored profile is the answer — the requirement does not go away
 * just because the box is somewhere else.
 */
private fun isUsableBirthYear(
    typed: Int?,
    showBirthYear: Boolean,
    bodyProfile: BodyProfile,
): Boolean {
    val year = if (showBirthYear) typed else bodyProfile.birthYear
    return year != null && year in BodyProfile.MinBirthYear..LocalDate.now().year
}

private inline fun BodyEnergyCalibration.zoneText(value: (HeartZoneThresholds) -> Int): String =
    manualZoneThresholdsBpm?.let { value(it).toString() }.orEmpty()

private fun String.toOptionalInt(): Int? =
    trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
