package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.cycle.CyclePhase
import tech.mmarca.openvitals.domain.cycle.SymptomPattern
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** Symptom counts by phase. Descriptive only; the copy says so. */
@Composable
internal fun CyclePatternsCard(patterns: List<SymptomPattern>, modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = stringResource(R.string.cycle_patterns_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (patterns.isEmpty()) {
                Text(text = stringResource(R.string.cycle_patterns_empty_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.cycle_patterns_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            patterns.forEachIndexed { index, pattern ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PatternRow(pattern)
            }
        }
    }
}

@Composable
private fun PatternRow(pattern: SymptomPattern) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(cycleSymptomLabelRes(pattern.symptom)), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(
                    R.string.cycle_patterns_total,
                    pluralStringResource(R.plurals.cycle_patterns_occurrences, pattern.totalOccurrences, pattern.totalOccurrences),
                    pluralStringResource(R.plurals.cycle_patterns_cycles, pattern.cycleCount, pattern.cycleCount),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(
                R.string.cycle_patterns_breakdown,
                pattern.phaseBreakdown[CyclePhase.MENSTRUAL] ?: 0,
                pattern.phaseBreakdown[CyclePhase.FOLLICULAR] ?: 0,
                pattern.phaseBreakdown[CyclePhase.OVULATORY] ?: 0,
                pattern.phaseBreakdown[CyclePhase.LUTEAL] ?: 0,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        pattern.mostFrequentPhase?.let { phase ->
            Text(
                text = stringResource(
                    R.string.cycle_patterns_most_frequent,
                    stringResource(cyclePhaseLabelRes(phase)),
                    pattern.phaseBreakdown[phase] ?: 0,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = cyclePhaseColor(phase),
            )
        }
    }
}
