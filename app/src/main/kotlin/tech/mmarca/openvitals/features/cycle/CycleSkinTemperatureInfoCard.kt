package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.cycle.CycleSource
import tech.mmarca.openvitals.domain.cycle.CycleSources
import tech.mmarca.openvitals.ui.components.AccentIconChip
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.ReferenceLinkButton
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

private val SkinTemperatureSources: List<CycleSource> = listOf(
    CycleSources.WANG_WRIST,
    CycleSources.THIGPEN_RING,
    CycleSources.MAIJALA_RING,
    CycleSources.SHILAIH_WRIST,
    CycleSources.ZHU_WRIST,
    CycleSources.DITTMAR_OC_WRIST,
    CycleSources.SALKELD_OC_TEMPERATURE,
    CycleSources.CHARKOUDIAN_THERMOREGULATION,
    CycleSources.RIVERA_HORMONAL_MECHANISM,
)

/**
 * What night-time skin temperature from a watch or ring can and cannot say about
 * the cycle. The estimates do not use it yet, and the card says so. A hormonal method
 * (any kind, also in the 60 days after it was stopped) or a declared PCOS context adds the note
 * that matters for that reader, always visible;
 * the numbers and the papers sit behind a toggle.
 */
@Composable
internal fun CycleSkinTemperatureInfoCard(
    hormonalInfluencing: Boolean,
    pcosDeclared: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccentIconChip(icon = Icons.Outlined.DeviceThermostat, color = CycleColor)
                Text(
                    text = stringResource(R.string.cycle_skin_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = Spacing.md),
                )
            }
            Text(text = stringResource(R.string.cycle_skin_intro), style = MaterialTheme.typography.bodyMedium)
            if (hormonalInfluencing) {
                SkinNote(R.string.cycle_skin_note_hormonal)
            }
            if (pcosDeclared) {
                SkinNote(R.string.cycle_skin_note_pcos)
            }
            if (expanded) {
                SkinDetails(hormonalInfluencing = hormonalInfluencing)
            }
            Text(
                text = stringResource(R.string.cycle_skin_not_a_method),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OpenVitalsTextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.cycle_skin_hide_details else R.string.cycle_skin_show_details))
            }
        }
    }
}

@Composable
private fun SkinNote(textRes: Int) {
    Text(text = stringResource(textRes), style = MaterialTheme.typography.bodyMedium, color = CycleColor)
}

@Composable
private fun SkinDetails(hormonalInfluencing: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(text = stringResource(R.string.cycle_skin_accuracy), style = MaterialTheme.typography.bodyMedium)
        Text(text = stringResource(R.string.cycle_skin_limits), style = MaterialTheme.typography.bodyMedium)
        if (hormonalInfluencing) {
            Text(text = stringResource(R.string.cycle_skin_hormonal_details), style = MaterialTheme.typography.bodyMedium)
        }
        SkinTemperatureSources.forEach { source ->
            ReferenceLinkButton(title = source.label, url = source.url)
        }
    }
}
