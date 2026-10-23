package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.cycle.HormonalContraception
import tech.mmarca.openvitals.domain.cycle.HormonalContraceptionKind
import tech.mmarca.openvitals.features.manualentry.ManualEntryPickerButton
import tech.mmarca.openvitals.features.manualentry.localizedDateText
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

internal fun hormonalKindLabelRes(kind: HormonalContraceptionKind): Int = when (kind) {
    HormonalContraceptionKind.NONE -> R.string.cycle_hormonal_kind_none
    HormonalContraceptionKind.PILL -> R.string.cycle_hormonal_kind_pill
    HormonalContraceptionKind.HORMONAL_IUD -> R.string.cycle_hormonal_kind_hormonal_iud
    HormonalContraceptionKind.IMPLANT -> R.string.cycle_hormonal_kind_implant
    HormonalContraceptionKind.RING_OR_PATCH -> R.string.cycle_hormonal_kind_ring_or_patch
    HormonalContraceptionKind.INJECTION -> R.string.cycle_hormonal_kind_injection
    HormonalContraceptionKind.OTHER_HORMONAL -> R.string.cycle_hormonal_kind_other
}

/**
 * Optional. The declared hormonal method, so later cycle features know whether
 * hormones shape the temperature rhythm. An active pill scheme sets the pill
 * itself and the picker steps aside. Context only, never advice.
 */
@Composable
internal fun CycleHormonalContraceptionCard(
    hormonal: HormonalContraception,
    setByPillScheme: Boolean,
    onSelectKind: (HormonalContraceptionKind) -> Unit,
    onSetStoppedOn: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingStop by remember { mutableStateOf(false) }
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(text = stringResource(R.string.cycle_hormonal_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.cycle_hormonal_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (setByPillScheme) {
                Text(
                    text = stringResource(R.string.cycle_hormonal_set_by_pill, stringResource(hormonalKindLabelRes(HormonalContraceptionKind.PILL))),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                HormonalKindPicker(selected = hormonal.kind, onSelectKind = onSelectKind)
                if (hormonal.isDeclared) {
                    val label = stringResource(R.string.cycle_hormonal_stopped_on)
                    ManualEntryPickerButton(
                        label = label,
                        value = hormonal.stoppedOn?.localizedDateText().orEmpty(),
                        icon = Icons.Outlined.CalendarMonth,
                        enabled = true,
                        onClick = { editingStop = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = label },
                    )
                    if (hormonal.stoppedOn != null) {
                        OpenVitalsOutlinedButton(onClick = { onSetStoppedOn(null) }) {
                            Text(stringResource(R.string.cycle_hormonal_stopped_clear))
                        }
                    }
                }
            }
        }
    }
    if (editingStop) {
        HealthDatePickerDialog(
            selectedDate = hormonal.stoppedOn ?: LocalDate.now(),
            onDismiss = { editingStop = false },
            onConfirm = { date ->
                editingStop = false
                onSetStoppedOn(date)
            },
        )
    }
}

@Composable
private fun HormonalKindPicker(selected: HormonalContraceptionKind, onSelectKind: (HormonalContraceptionKind) -> Unit) {
    Column(modifier = Modifier.selectableGroup()) {
        HormonalContraceptionKind.entries.forEach { kind ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected == kind, role = Role.RadioButton, onClick = { onSelectKind(kind) })
                    .padding(vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == kind, onClick = null)
                Text(
                    text = stringResource(hormonalKindLabelRes(kind)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
    }
}
