package tech.mmarca.openvitals.features.cycle

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.cycle.NightlyEvidence
import tech.mmarca.openvitals.domain.cycle.NightlySeries
import tech.mmarca.openvitals.domain.cycle.NightlySignal
import tech.mmarca.openvitals.domain.cycle.NightlyVitals
import tech.mmarca.openvitals.domain.cycle.NightlyVitalsEvidence
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.ReferenceLinkButton
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

@StringRes
private fun signalLabelRes(signal: NightlySignal): Int = when (signal) {
    NightlySignal.RESTING_HEART_RATE -> R.string.cycle_nightly_signal_pulse
    NightlySignal.HRV_RMSSD -> R.string.cycle_nightly_signal_hrv
    NightlySignal.RESPIRATORY_RATE -> R.string.cycle_nightly_signal_breathing
}

@StringRes
private fun signalRangeRes(signal: NightlySignal): Int = when (signal) {
    NightlySignal.RESTING_HEART_RATE -> R.string.cycle_nightly_range_pulse
    NightlySignal.HRV_RMSSD -> R.string.cycle_nightly_range_hrv
    NightlySignal.RESPIRATORY_RATE -> R.string.cycle_nightly_range_breathing
}

@StringRes
private fun signalMissingRes(signal: NightlySignal): Int = when (signal) {
    NightlySignal.RESTING_HEART_RATE -> R.string.cycle_nightly_missing_pulse
    NightlySignal.HRV_RMSSD -> R.string.cycle_nightly_missing_hrv
    NightlySignal.RESPIRATORY_RATE -> R.string.cycle_nightly_missing_breathing
}

@StringRes
private fun evidenceRes(id: String): Int = when (id) {
    "goodale_pulse" -> R.string.cycle_nightly_evidence_goodale_pulse
    "jasinski_days" -> R.string.cycle_nightly_evidence_jasinski_days
    "lin_phase" -> R.string.cycle_nightly_evidence_lin_phase
    "de_jager_hrv" -> R.string.cycle_nightly_evidence_de_jager_hrv
    "gonzalez_sleep" -> R.string.cycle_nightly_evidence_gonzalez_sleep
    "pietila_alcohol" -> R.string.cycle_nightly_evidence_pietila_alcohol
    "shi_small_gain" -> R.string.cycle_nightly_evidence_shi_small_gain
    else -> error("No copy for nightly evidence $id")
}

/**
 * The current cycle's nightly resting pulse, HRV and breathing rate by cycle day. Information
 * only: the card never names a phase, a fertile day or a method, and nothing it shows feeds
 * an estimate. A signal the device or the permissions do not give is named as missing and
 * is not filled in. Each signal has a chip that shows or hides its line.
 */
@Composable
internal fun CycleNightlyVitalsCard(
    nightly: NightlyVitals?,
    modifier: Modifier = Modifier,
    /** The optional overnight reads not granted yet. While not empty the card offers a button. */
    missingPermissions: Set<String> = emptySet(),
    onAllowReading: () -> Unit = {},
) {
    var showDetails by rememberSaveable { mutableStateOf(false) }
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LayoutMetrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.cycle_nightly_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                nightly == null -> Muted(stringResource(R.string.cycle_nightly_loading))
                nightly.nightCount == 0 -> Muted(stringResource(R.string.cycle_nightly_no_sleep))
                else -> CycleNightlyBody(nightly)
            }
            if (missingPermissions.isNotEmpty()) {
                Muted(stringResource(R.string.cycle_nightly_permission_needed))
                OpenVitalsOutlinedButton(onClick = onAllowReading, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cycle_nightly_allow_reading))
                }
            }
            TextButton(onClick = { showDetails = !showDetails }) {
                Text(
                    stringResource(
                        if (showDetails) R.string.cycle_nightly_details_hide else R.string.cycle_nightly_details_show,
                    ),
                )
            }
            if (showDetails) CycleNightlyDetails()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CycleNightlyBody(nightly: NightlyVitals) {
    val present = remember(nightly) { nightly.series.filterNot { it.isMissing } }
    // One switch per signal; a rotation or a return to the screen keeps them.
    val shown = NightlySignal.entries.associateWith { signal ->
        rememberSaveable(signal.name) { mutableStateOf(true) }
    }
    Muted(stringResource(R.string.cycle_nightly_nights, nightly.nightCount))
    if (present.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            present.forEach { series ->
                val state = shown.getValue(series.signal)
                FilterChip(
                    selected = state.value,
                    onClick = { state.value = !state.value },
                    label = { Text(stringResource(signalLabelRes(series.signal))) },
                    leadingIcon = { Dot(nightlySignalColor(series.signal)) },
                )
            }
        }
        val visible = present.filter { shown.getValue(it.signal).value }
        val dailyValues = visible.sumOf { it.dailyValueCount }
        if (visible.isEmpty()) {
            Muted(stringResource(R.string.cycle_nightly_all_hidden))
        } else {
            val description = visible.map { series ->
                stringResource(
                    R.string.cycle_nightly_chart_description,
                    stringResource(signalLabelRes(series.signal)),
                    series.points.size,
                    rangeText(series),
                )
            }.joinToString(", ")
            CycleNightlyChart(visible = visible, contentDescription = description)
            visible.forEach { series -> LegendRow(series) }
            // A daily value is the device's own once-a-day record, drawn hollow, never passed off as the night.
            if (dailyValues > 0) Muted(stringResource(R.string.cycle_nightly_daily_value))
        }
    }
    // The gaps are named, never filled.
    nightly.series.filter { it.isMissing }.forEach { series ->
        Muted(
            stringResource(
                if (series.permissionDenied) R.string.cycle_nightly_permission_needed else signalMissingRes(series.signal),
            ),
        )
    }
}

@Composable
private fun rangeText(series: NightlySeries): String {
    val (low, high) = remember(series) {
        formatNightlyValue(series.signal, series.points.minOf { it.value }) to
            formatNightlyValue(series.signal, series.points.maxOf { it.value })
    }
    return stringResource(signalRangeRes(series.signal), low, high)
}

@Composable
private fun LegendRow(series: NightlySeries) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Dot(nightlySignalColor(series.signal))
            Text(
                text = stringResource(R.string.cycle_nightly_legend, stringResource(signalLabelRes(series.signal)), rangeText(series)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val source = series.source
        if (source != null) {
            Muted(
                if (series.otherSources.isEmpty()) {
                    stringResource(R.string.cycle_nightly_source, source)
                } else {
                    stringResource(R.string.cycle_nightly_source_others, source, series.otherSources.sorted().joinToString(", "))
                },
            )
        }
    }
}

/** Why the card exists, what each study found, and how its own numbers are made. */
@Composable
private fun CycleNightlyDetails() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(R.string.cycle_nightly_why_intro), style = MaterialTheme.typography.bodyMedium)
        NightlyVitalsEvidence.ALL.forEach { evidence -> EvidenceItem(evidence) }
        Text(stringResource(R.string.cycle_nightly_method), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.cycle_nightly_caveats), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EvidenceItem(evidence: NightlyEvidence) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = stringResource(evidenceRes(evidence.id)), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = stringResource(R.string.cycle_source_label, evidence.source.label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReferenceLinkButton(title = stringResource(R.string.cycle_view_source), url = evidence.source.url)
    }
}

@Composable
private fun Muted(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}
