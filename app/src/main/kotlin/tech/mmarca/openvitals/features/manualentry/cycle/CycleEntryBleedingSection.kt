package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.ui.theme.Spacing

private val DropHeavy: Dp = 22.dp
private val DropMedium: Dp = 19.dp
private val DropLight: Dp = 16.dp
private val DropSpotting: Dp = 13.dp
private val DashSize: Dp = 16.dp

/**
 * The bleeding scale, two chips per row so the graduated drop has room beside
 * its label. The drop grows across the scale; the check keeps selection off
 * colour alone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CycleEntryBleedingSection(
    selected: BleedingOption?,
    enabled: Boolean,
    foreignFlowLevel: Int?,
    foreignSpotting: Boolean,
    onSelect: (BleedingOption?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(text = stringResource(R.string.cycle_entry_section_flow), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            maxItemsInEachRow = 2,
        ) {
            BleedingChip(
                label = stringResource(R.string.cycle_bleeding_not_recorded),
                option = null,
                selected = selected == null,
                enabled = enabled,
                onClick = { onSelect(null) },
                modifier = Modifier.weight(1f),
            )
            BleedingOption.entries.forEach { option ->
                BleedingChip(
                    label = bleedingOptionLabel(option),
                    option = option,
                    selected = selected == option,
                    enabled = enabled,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        foreignBleedingNote(foreignFlowLevel, foreignSpotting)?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BleedingChip(
    label: String,
    option: BleedingOption?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dropSize = when (option) {
        BleedingOption.HEAVY -> DropHeavy
        BleedingOption.MEDIUM -> DropMedium
        BleedingOption.LIGHT -> DropLight
        BleedingOption.SPOTTING -> DropSpotting
        else -> null
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, maxLines = 2) },
        leadingIcon = when {
            dropSize != null -> {
                { Icon(Icons.Outlined.WaterDrop, contentDescription = null, modifier = Modifier.size(dropSize)) }
            }
            option == BleedingOption.NONE -> {
                { Icon(Icons.Outlined.Remove, contentDescription = null, modifier = Modifier.size(DashSize)) }
            }
            else -> null
        },
        trailingIcon = if (selected && option != null) {
            { Icon(Icons.Outlined.Check, contentDescription = null) }
        } else {
            null
        },
        // One choice among six, not six checkboxes.
        modifier = modifier.semantics { role = Role.RadioButton },
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
internal fun bleedingOptionLabel(option: BleedingOption): String = stringResource(
    when (option) {
        BleedingOption.NONE -> R.string.cycle_bleeding_none
        BleedingOption.SPOTTING -> R.string.cycle_entry_section_spotting
        BleedingOption.LIGHT -> R.string.cycle_flow_light
        BleedingOption.MEDIUM -> R.string.cycle_flow_medium
        BleedingOption.HEAVY -> R.string.cycle_flow_heavy
    },
)

/** What another app recorded that day. Read-only, so the user knows why the day already counts. */
@Composable
private fun foreignBleedingNote(foreignFlowLevel: Int?, foreignSpotting: Boolean): String? = when {
    foreignFlowLevel != null -> stringResource(
        R.string.cycle_entry_foreign_flow,
        stringResource(
            when (foreignFlowLevel) {
                CycleRecordValues.FLOW_LIGHT -> R.string.cycle_flow_light
                CycleRecordValues.FLOW_HEAVY -> R.string.cycle_flow_heavy
                else -> R.string.cycle_flow_medium
            },
        ),
    )
    foreignSpotting -> stringResource(R.string.cycle_entry_foreign_spotting)
    else -> null
}
