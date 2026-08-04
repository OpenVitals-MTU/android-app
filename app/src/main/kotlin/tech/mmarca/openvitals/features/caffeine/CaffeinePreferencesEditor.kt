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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeineAlcoholUse
import tech.mmarca.openvitals.domain.preferences.CaffeineGenotype
import tech.mmarca.openvitals.domain.preferences.CaffeineHabituation
import tech.mmarca.openvitals.domain.preferences.CaffeineHormonalStatus
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.CaffeineSleepSensitivity

/**
 * The four caffeine-model parameters shown under Settings > Nutrition.
 * The physiological factors live in [MetabolismEditor] (Settings > Body
 * profile) because they are facts about the person, not the model.
 */
@Composable
internal fun CaffeineModelEditor(
    preferences: CaffeinePreferences,
    bodyProfile: BodyProfile,
    onChange: (CaffeinePreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PreferenceNumberField(
            label = stringResource(R.string.caffeine_pref_half_life),
            value = preferences.halfLifeMinutes,
            suffix = stringResource(R.string.caffeine_pref_minutes_suffix),
            onValue = { onChange(preferences.copy(halfLifeMinutes = it)) },
        )
        PreferenceNumberField(
            label = stringResource(R.string.caffeine_pref_absorption),
            value = preferences.absorptionMinutes,
            suffix = stringResource(R.string.caffeine_pref_minutes_suffix),
            onValue = { onChange(preferences.copy(absorptionMinutes = it)) },
        )
        PreferenceNumberField(
            label = stringResource(R.string.caffeine_sleep_threshold),
            value = preferences.sleepThresholdMg,
            suffix = stringResource(R.string.caffeine_pref_milligrams_suffix),
            onValue = { onChange(preferences.copy(sleepThresholdMg = it)) },
        )
        PreferenceTimeField(
            label = stringResource(R.string.caffeine_pref_bedtime),
            value = preferences.bedtime,
            onValue = { onChange(preferences.copy(bedtime = it)) },
        )
        Text(
            text = stringResource(
                R.string.caffeine_pref_effective_half_life,
                preferences.effectiveHalfLifeMinutes(bodyProfile),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * The nine physiological factors that shift caffeine clearance. Shown under
 * Settings > Body profile > Metabolism; edits the same [CaffeinePreferences]
 * store as [CaffeineModelEditor].
 */
@Composable
internal fun MetabolismEditor(
    preferences: CaffeinePreferences,
    onChange: (CaffeinePreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PreferenceEnumDropdown(
            label = stringResource(R.string.settings_metabolism_sleep_sensitivity),
            selected = preferences.sleepSensitivity,
            values = CaffeineSleepSensitivity.entries,
            labelFor = { it.displayLabel() },
            onSelect = { onChange(preferences.copy(sleepSensitivity = it)) },
        )
        PreferenceEnumDropdown(
            label = stringResource(R.string.settings_metabolism_alcohol),
            selected = preferences.alcoholUse,
            values = CaffeineAlcoholUse.entries,
            labelFor = { it.displayLabel() },
            onSelect = { onChange(preferences.copy(alcoholUse = it)) },
        )
        PreferenceEnumDropdown(
            label = stringResource(R.string.settings_metabolism_habituation),
            selected = preferences.caffeineHabituation,
            values = CaffeineHabituation.entries,
            labelFor = { it.displayLabel() },
            onSelect = { onChange(preferences.copy(caffeineHabituation = it)) },
        )
        PreferenceEnumDropdown(
            label = stringResource(R.string.settings_metabolism_cyp1a2),
            selected = preferences.cyp1a2Genotype,
            values = CaffeineGenotype.entries,
            labelFor = { it.displayLabel() },
            onSelect = { onChange(preferences.copy(cyp1a2Genotype = it)) },
        )
        PreferenceEnumDropdown(
            label = stringResource(R.string.settings_metabolism_ahr),
            selected = preferences.ahrGenotype,
            values = CaffeineGenotype.entries,
            labelFor = { it.displayLabel() },
            onSelect = { onChange(preferences.copy(ahrGenotype = it)) },
        )
        PreferenceEnumDropdown(
            label = stringResource(R.string.settings_metabolism_hormonal_status),
            selected = preferences.hormonalStatus,
            values = CaffeineHormonalStatus.entries,
            labelFor = { it.displayLabel() },
            onSelect = { onChange(preferences.copy(hormonalStatus = it)) },
        )
        PreferenceSwitchRow(
            label = stringResource(R.string.settings_metabolism_smoker),
            checked = preferences.smoker,
            onCheckedChange = { onChange(preferences.copy(smoker = it)) },
        )
        PreferenceSwitchRow(
            label = stringResource(R.string.settings_metabolism_liver_impairment),
            checked = preferences.liverImpairment,
            onCheckedChange = { onChange(preferences.copy(liverImpairment = it)) },
        )
        PreferenceSwitchRow(
            label = stringResource(R.string.settings_metabolism_medication_interaction),
            checked = preferences.medicationInteraction,
            onCheckedChange = { onChange(preferences.copy(medicationInteraction = it)) },
        )
    }
}

/** Full editor: the caffeine model plus all metabolism factors, for the one-time setup card. */
@Composable
internal fun CaffeinePreferencesEditor(
    preferences: CaffeinePreferences,
    bodyProfile: BodyProfile,
    onChange: (CaffeinePreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        CaffeineModelEditor(
            preferences = preferences,
            bodyProfile = bodyProfile,
            onChange = onChange,
        )
        MetabolismEditor(
            preferences = preferences,
            onChange = onChange,
        )
    }
}

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
    labelFor: @Composable (T) -> String,
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

@Composable
internal fun CaffeineSleepSensitivity.displayLabel(): String = stringResource(
    when (this) {
        CaffeineSleepSensitivity.LOW -> R.string.settings_metabolism_sensitivity_low
        CaffeineSleepSensitivity.NORMAL -> R.string.settings_metabolism_sensitivity_normal
        CaffeineSleepSensitivity.HIGH -> R.string.settings_metabolism_sensitivity_high
        CaffeineSleepSensitivity.INSOMNIA -> R.string.settings_metabolism_sensitivity_insomnia
    },
)

@Composable
internal fun CaffeineAlcoholUse.displayLabel(): String = stringResource(
    when (this) {
        CaffeineAlcoholUse.NONE -> R.string.settings_metabolism_alcohol_none
        CaffeineAlcoholUse.OCCASIONAL -> R.string.settings_metabolism_alcohol_occasional
        CaffeineAlcoholUse.REGULAR -> R.string.settings_metabolism_alcohol_regular
    },
)

@Composable
internal fun CaffeineHabituation.displayLabel(): String = stringResource(
    when (this) {
        CaffeineHabituation.LOW -> R.string.settings_metabolism_habituation_low
        CaffeineHabituation.MODERATE -> R.string.settings_metabolism_habituation_moderate
        CaffeineHabituation.HIGH -> R.string.settings_metabolism_habituation_high
    },
)

@Composable
internal fun CaffeineGenotype.displayLabel(): String = stringResource(
    when (this) {
        CaffeineGenotype.UNKNOWN -> R.string.settings_metabolism_genotype_unknown
        CaffeineGenotype.FAST -> R.string.settings_metabolism_genotype_fast
        CaffeineGenotype.NORMAL -> R.string.settings_metabolism_genotype_normal
        CaffeineGenotype.SLOW -> R.string.settings_metabolism_genotype_slow
    },
)

@Composable
internal fun CaffeineHormonalStatus.displayLabel(): String = stringResource(
    when (this) {
        CaffeineHormonalStatus.NONE -> R.string.settings_metabolism_hormonal_none
        CaffeineHormonalStatus.ORAL_CONTRACEPTIVE -> R.string.settings_metabolism_hormonal_oral_contraceptive
        CaffeineHormonalStatus.PREGNANT -> R.string.settings_metabolism_hormonal_pregnant
    },
)
