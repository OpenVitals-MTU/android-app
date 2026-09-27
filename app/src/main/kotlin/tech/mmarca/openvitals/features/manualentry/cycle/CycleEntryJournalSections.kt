package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.cycle.CycleSymptomGroup
import tech.mmarca.openvitals.domain.model.HcgTestResult
import tech.mmarca.openvitals.features.cycle.cycleSymptomLabelRes
import tech.mmarca.openvitals.ui.theme.Spacing

/** Pain, mood and energy. Blank means not recorded; the copy states each scale's direction. */
@Composable
internal fun CycleEntryScales(
    form: CycleDayForm,
    enabled: Boolean,
    onPain: (Int?) -> Unit,
    onMood: (Int?) -> Unit,
    onEnergy: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pain = scaleDescriptions(R.string.cycle_entry_pain_value)
    val mood = scaleDescriptions(R.string.cycle_entry_mood_value)
    val energy = scaleDescriptions(R.string.cycle_entry_energy_value)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        CycleObservationScale(
            label = stringResource(R.string.cycle_entry_pain),
            supportingText = stringResource(R.string.cycle_entry_pain_scale),
            value = form.pain,
            type = ObservationScaleType.PAIN,
            valueDescription = pain::getValue,
            onValueChange = onPain,
            enabled = enabled,
        )
        CycleObservationScale(
            label = stringResource(R.string.cycle_entry_mood),
            supportingText = stringResource(R.string.cycle_entry_mood_scale),
            value = form.mood,
            type = ObservationScaleType.MOOD,
            valueDescription = mood::getValue,
            onValueChange = onMood,
            enabled = enabled,
        )
        CycleObservationScale(
            label = stringResource(R.string.cycle_entry_energy),
            supportingText = stringResource(R.string.cycle_entry_energy_scale),
            value = form.energy,
            type = ObservationScaleType.ENERGY,
            valueDescription = energy::getValue,
            onValueChange = onEnergy,
            enabled = enabled,
        )
    }
}

@Composable
private fun scaleDescriptions(resourceId: Int): Map<Int, String> =
    (1..5).associateWith { value -> stringResource(resourceId, value) }

/** Symptom chips in three groups, with a one-tap copy of yesterday's. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CycleEntrySymptoms(
    selected: Set<CycleSymptom>,
    offered: List<CycleSymptom>,
    previousDay: Set<CycleSymptom>,
    enabled: Boolean,
    onToggle: (CycleSymptom) -> Unit,
    onCopyPreviousDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember(offered) {
        CycleSymptomGroup.entries.mapNotNull { group ->
            val inGroup = offered.filter { it.group == group }
            if (inGroup.isEmpty()) null else group to inGroup
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(text = stringResource(R.string.cycle_entry_symptoms), style = MaterialTheme.typography.titleSmall)
        if (previousDay.isNotEmpty() && !selected.containsAll(previousDay)) {
            FilterChip(
                selected = false,
                enabled = enabled,
                onClick = onCopyPreviousDay,
                label = { Text(stringResource(R.string.cycle_entry_copy_yesterday, previousDay.size)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                shape = MaterialTheme.shapes.small,
            )
        }
        groups.forEach { (group, symptoms) ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(symptomGroupLabelRes(group)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    symptoms.forEach { symptom ->
                        val isSelected = symptom in selected
                        FilterChip(
                            selected = isSelected,
                            enabled = enabled,
                            onClick = { onToggle(symptom) },
                            label = { Text(stringResource(cycleSymptomLabelRes(symptom)), maxLines = 2) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Outlined.Check, contentDescription = null) }
                            } else {
                                null
                            },
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                }
            }
        }
    }
}

private fun symptomGroupLabelRes(group: CycleSymptomGroup): Int = when (group) {
    CycleSymptomGroup.PAIN -> R.string.cycle_symptom_group_pain
    CycleSymptomGroup.PHYSICAL -> R.string.cycle_symptom_group_physical
    CycleSymptomGroup.MOOD_ENERGY -> R.string.cycle_symptom_group_mood_energy
}

/** Private notes. They never reach a widget, a notification or the report unless asked. */
@Composable
internal fun CycleEntryNotes(
    notes: String,
    enabled: Boolean,
    onNotesChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChanged,
        enabled = enabled,
        label = { Text(stringResource(R.string.cycle_entry_notes)) },
        placeholder = { Text(stringResource(R.string.cycle_entry_notes_hint)) },
        minLines = 3,
        maxLines = 6,
        modifier = modifier.fillMaxWidth(),
    )
}

/** A pregnancy test result. It stays in the journal: Health Connect has no record type for it. */
@Composable
internal fun CycleEntryHcgTest(
    selected: HcgTestResult?,
    enabled: Boolean,
    onSelect: (HcgTestResult?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        HcgTestResult.NEGATIVE to stringResource(R.string.cycle_hcg_negative),
        HcgTestResult.POSITIVE to stringResource(R.string.cycle_hcg_positive),
        HcgTestResult.FAINT_UNCERTAIN to stringResource(R.string.cycle_hcg_faint),
    )
    tech.mmarca.openvitals.ui.components.OptionDropdown(
        label = stringResource(R.string.cycle_entry_section_hcg),
        options = options,
        selected = options.firstOrNull { it.first == selected },
        optionText = { it.second },
        enabled = enabled,
        onSelect = { onSelect(it?.first) },
        modifier = modifier,
    )
}
