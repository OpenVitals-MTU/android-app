package tech.mmarca.openvitals.features.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.ReportMetric
import tech.mmarca.openvitals.domain.model.ReportSection
import tech.mmarca.openvitals.features.nutrition.titleRes
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton

/**
 * The metrics card: every metric the provider serves, by section. The nutrient breakdown sits
 * under the nutrition screen's own headings, and ticking a heading ticks its whole group.
 */
@Composable
internal fun ReportMetricChecklist(
    state: ReportBuilderState,
    metricTitle: (ReportMetric) -> String,
    onToggleMetric: (ReportMetric) -> Unit,
    onToggleMetrics: (List<ReportMetric>) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
) {
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.report_metrics_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                OpenVitalsTextButton(onClick = onSelectAll) {
                    Text(stringResource(R.string.report_metrics_select_all))
                }
                OpenVitalsTextButton(onClick = onClear) {
                    Text(stringResource(R.string.report_metrics_clear))
                }
            }
            state.metricsBySection.forEach { (section, metrics) ->
                Text(
                    text = stringResource(section.titleRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                )
                metrics.filterNot { it.isNutrientBreakdown }.forEach { metric ->
                    ReportMetricRow(
                        title = metricTitle(metric),
                        checked = metric in state.selectedMetrics,
                        onToggle = { onToggleMetric(metric) },
                    )
                }
                val breakdown = metrics.filter { it.isNutrientBreakdown }
                if (breakdown.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.report_metrics_nutrient_breakdown_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                breakdown.groupBy { it.nutrient!!.group }.forEach { (group, groupMetrics) ->
                    ReportMetricGroupRow(
                        title = stringResource(group.titleRes()),
                        state = groupMetrics.toggleableState(state.selectedMetrics),
                        onToggle = { onToggleMetrics(groupMetrics) },
                    )
                    groupMetrics.forEach { metric ->
                        ReportMetricRow(
                            title = metricTitle(metric),
                            checked = metric in state.selectedMetrics,
                            onToggle = { onToggleMetric(metric) },
                            modifier = Modifier.padding(start = 24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetricRow(
    title: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** A breakdown heading: checked when its whole group is, mixed when part of it is. */
@Composable
private fun ReportMetricGroupRow(
    title: String,
    state: ToggleableState,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .triStateToggleable(state = state, role = Role.Checkbox, onClick = onToggle),
    ) {
        TriStateCheckbox(state = state, onClick = null)
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

private fun List<ReportMetric>.toggleableState(selected: Set<ReportMetric>): ToggleableState {
    val ticked = count { it in selected }
    return when (ticked) {
        0 -> ToggleableState.Off
        size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
}

private fun ReportSection.titleRes(): Int = when (this) {
    ReportSection.ACTIVITY -> R.string.onboarding_hc_category_activity
    ReportSection.SLEEP -> R.string.onboarding_hc_category_sleep
    ReportSection.NUTRITION -> R.string.onboarding_hc_category_nutrition
    ReportSection.BODY -> R.string.onboarding_category_body
    ReportSection.HEART -> R.string.section_heart
    ReportSection.VITALS -> R.string.section_vitals
    ReportSection.MINDFULNESS -> R.string.onboarding_category_mindfulness
    ReportSection.CYCLE -> R.string.onboarding_category_cycle_tracking
}
