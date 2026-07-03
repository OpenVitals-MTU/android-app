package tech.mmarca.openvitals.features.caffeine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import tech.mmarca.openvitals.domain.preferences.CaffeineAlcoholUse
import tech.mmarca.openvitals.domain.preferences.CaffeineGenotype
import tech.mmarca.openvitals.domain.preferences.CaffeineHabituation
import tech.mmarca.openvitals.domain.preferences.CaffeineHormonalStatus
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.CaffeineSleepSensitivity
import tech.mmarca.openvitals.domain.preferences.UnitSystem

@Composable
internal fun CaffeinePreferencesEditor(
    preferences: CaffeinePreferences,
    unitSystem: UnitSystem,
    onChange: (CaffeinePreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PreferenceNumberField(
            label = "Half-life",
            value = preferences.halfLifeMinutes,
            suffix = "min",
            onValue = { onChange(preferences.copy(halfLifeMinutes = it)) },
        )
        PreferenceNumberField(
            label = "Absorption",
            value = preferences.absorptionMinutes,
            suffix = "min",
            onValue = { onChange(preferences.copy(absorptionMinutes = it)) },
        )
        PreferenceNumberField(
            label = "Sleep threshold",
            value = preferences.sleepThresholdMg,
            suffix = "mg",
            onValue = { onChange(preferences.copy(sleepThresholdMg = it)) },
        )
        PreferenceTimeField(
            label = "Bedtime",
            value = preferences.bedtime,
            onValue = { onChange(preferences.copy(bedtime = it)) },
        )
        PreferenceOptionalNumberField(
            label = "Age",
            value = preferences.ageYears,
            suffix = "years",
            onValue = { onChange(preferences.copy(ageYears = it)) },
        )
        PreferenceOptionalDecimalField(
            label = "Weight",
            value = displayWeightForUnitSystem(preferences.weightKg, unitSystem),
            suffix = weightSuffix(unitSystem),
            onValue = {
                onChange(preferences.copy(weightKg = storedWeightKgForUnitSystem(it, unitSystem)))
            },
        )
        PreferenceEnumDropdown(
            label = "Sleep sensitivity",
            selected = preferences.sleepSensitivity,
            values = CaffeineSleepSensitivity.entries,
            labelFor = CaffeineSleepSensitivity::displayLabel,
            onSelect = { onChange(preferences.copy(sleepSensitivity = it)) },
        )
        PreferenceEnumDropdown(
            label = "Alcohol",
            selected = preferences.alcoholUse,
            values = CaffeineAlcoholUse.entries,
            labelFor = CaffeineAlcoholUse::displayLabel,
            onSelect = { onChange(preferences.copy(alcoholUse = it)) },
        )
        PreferenceEnumDropdown(
            label = "Caffeine habituation",
            selected = preferences.caffeineHabituation,
            values = CaffeineHabituation.entries,
            labelFor = CaffeineHabituation::displayLabel,
            onSelect = { onChange(preferences.copy(caffeineHabituation = it)) },
        )
        PreferenceEnumDropdown(
            label = "CYP1A2",
            selected = preferences.cyp1a2Genotype,
            values = CaffeineGenotype.entries,
            labelFor = CaffeineGenotype::displayLabel,
            onSelect = { onChange(preferences.copy(cyp1a2Genotype = it)) },
        )
        PreferenceEnumDropdown(
            label = "AHR",
            selected = preferences.ahrGenotype,
            values = CaffeineGenotype.entries,
            labelFor = CaffeineGenotype::displayLabel,
            onSelect = { onChange(preferences.copy(ahrGenotype = it)) },
        )
        PreferenceEnumDropdown(
            label = "Hormonal status",
            selected = preferences.hormonalStatus,
            values = CaffeineHormonalStatus.entries,
            labelFor = CaffeineHormonalStatus::displayLabel,
            onSelect = { onChange(preferences.copy(hormonalStatus = it)) },
        )
        PreferenceSwitchRow(
            label = "Smoker",
            checked = preferences.smoker,
            onCheckedChange = { onChange(preferences.copy(smoker = it)) },
        )
        PreferenceSwitchRow(
            label = "Liver impairment",
            checked = preferences.liverImpairment,
            onCheckedChange = { onChange(preferences.copy(liverImpairment = it)) },
        )
        PreferenceSwitchRow(
            label = "Medication interaction",
            checked = preferences.medicationInteraction,
            onCheckedChange = { onChange(preferences.copy(medicationInteraction = it)) },
        )
        Text(
            text = "Effective half-life ${preferences.effectiveHalfLifeMinutes} min",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
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
private fun PreferenceNumberField(
    label: String,
    value: Int,
    suffix: String,
    onValue: (Int) -> Unit,
) {
    PreferenceOptionalNumberField(
        label = label,
        value = value,
        suffix = suffix,
        onValue = { next -> next?.let(onValue) },
    )
}

@Composable
private fun PreferenceOptionalNumberField(
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
private fun PreferenceOptionalDecimalField(
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

@Composable
private fun PreferenceTimeField(
    label: String,
    value: LocalTime,
    onValue: (LocalTime) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { next ->
            text = next.take(5)
            runCatching { LocalTime.parse(text) }.getOrNull()?.let(onValue)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Composable
private fun <T> PreferenceEnumDropdown(
    label: String,
    selected: T,
    values: List<T>,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = labelFor(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(labelFor(value)) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .clickable { expanded = true },
        )
    }
}

@Composable
private fun PreferenceSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    Spacer(Modifier.height(2.dp))
}

internal fun CaffeineSleepSensitivity.displayLabel(): String = when (this) {
    CaffeineSleepSensitivity.LOW -> "Low"
    CaffeineSleepSensitivity.NORMAL -> "Normal"
    CaffeineSleepSensitivity.HIGH -> "High"
    CaffeineSleepSensitivity.INSOMNIA -> "Insomnia"
}

internal fun CaffeineAlcoholUse.displayLabel(): String = when (this) {
    CaffeineAlcoholUse.NONE -> "None"
    CaffeineAlcoholUse.OCCASIONAL -> "Occasional"
    CaffeineAlcoholUse.REGULAR -> "Regular"
}

internal fun CaffeineHabituation.displayLabel(): String = when (this) {
    CaffeineHabituation.LOW -> "Low"
    CaffeineHabituation.MODERATE -> "Moderate"
    CaffeineHabituation.HIGH -> "High"
}

internal fun CaffeineGenotype.displayLabel(): String = when (this) {
    CaffeineGenotype.UNKNOWN -> "Unknown"
    CaffeineGenotype.FAST -> "Fast"
    CaffeineGenotype.NORMAL -> "Normal"
    CaffeineGenotype.SLOW -> "Slow"
}

internal fun CaffeineHormonalStatus.displayLabel(): String = when (this) {
    CaffeineHormonalStatus.NONE -> "None"
    CaffeineHormonalStatus.ORAL_CONTRACEPTIVE -> "Oral contraceptive"
    CaffeineHormonalStatus.PREGNANT -> "Pregnant"
}
