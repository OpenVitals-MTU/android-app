package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton

@Composable
internal fun BodyProfileCard(
    profile: BodyProfile,
    unitSystem: UnitSystem,
    onSave: (BodyProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(profile) { mutableStateOf(profile) }
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.settings_body_profile_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = stringResource(R.string.settings_body_profile_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            BodyProfileNumberField(
                label = stringResource(R.string.body_energy_calibration_birth_year),
                value = draft.birthYear,
                suffix = "",
                onValue = { draft = draft.copy(birthYear = it) },
            )
            BodyProfileDecimalField(
                label = stringResource(R.string.settings_body_profile_weight),
                value = displayWeightForUnitSystem(draft.weightKg, unitSystem),
                suffix = weightSuffix(unitSystem),
                onValue = { draft = draft.copy(weightKg = storedWeightKgForUnitSystem(it, unitSystem)) },
            )
            BodyProfileNumberField(
                label = stringResource(R.string.body_energy_calibration_resting_hr),
                value = draft.restingHeartRateBpm,
                suffix = "bpm",
                onValue = { draft = draft.copy(restingHeartRateBpm = it) },
            )
            BodyProfileNumberField(
                label = stringResource(R.string.body_energy_calibration_max_hr),
                value = draft.maxHeartRateBpm,
                suffix = "bpm",
                onValue = { draft = draft.copy(maxHeartRateBpm = it) },
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                OpenVitalsTonalButton(onClick = { onSave(draft.normalized()) }) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

internal fun displayWeightForUnitSystem(weightKg: Double?, unitSystem: UnitSystem): Double? =
    weightKg?.let { kg ->
        when (unitSystem) {
            UnitSystem.METRIC -> kg
            UnitSystem.IMPERIAL -> kg * PoundsPerKilogram
        }
    }

internal fun storedWeightKgForUnitSystem(weight: Double?, unitSystem: UnitSystem): Double? =
    weight?.let { value ->
        when (unitSystem) {
            UnitSystem.METRIC -> value
            UnitSystem.IMPERIAL -> value / PoundsPerKilogram
        }
    }

private fun weightSuffix(unitSystem: UnitSystem): String =
    when (unitSystem) {
        UnitSystem.METRIC -> "kg"
        UnitSystem.IMPERIAL -> "lb"
    }

private const val PoundsPerKilogram = 2.2046226218

@Composable
private fun BodyProfileNumberField(
    label: String,
    value: Int?,
    suffix: String,
    onValue: (Int?) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value?.toString().orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = { next ->
            text = next.filter(Char::isDigit).take(4)
            onValue(text.toIntOrNull())
        },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Composable
private fun BodyProfileDecimalField(
    label: String,
    value: Double?,
    suffix: String,
    onValue: (Double?) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value?.let { "%.1f".format(it) }.orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = { next ->
            text = next.filter { it.isDigit() || it == '.' }.take(5)
            onValue(text.toDoubleOrNull())
        },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}
