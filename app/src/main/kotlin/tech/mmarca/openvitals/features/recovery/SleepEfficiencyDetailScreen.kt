package tech.mmarca.openvitals.features.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneId
import kotlin.math.roundToLong
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.domain.insights.SleepScoreEstimate
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedDayTitle
import tech.mmarca.openvitals.ui.theme.SleepColor

private const val NcbiSleepEfficiencyUrl = "https://www.ncbi.nlm.nih.gov/medgen/1669302"
private const val SleepEfficiencyDenominatorUrl = "https://pmc.ncbi.nlm.nih.gov/articles/PMC4751425/"
private const val SleepAssessmentMethodsUrl = "https://pmc.ncbi.nlm.nih.gov/articles/PMC5971842/"

@Composable
fun SleepEfficiencyDetailScreen(
    viewModel: RecoveryViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.isLoading && state.days.isNotEmpty(),
        onRefresh = { viewModel.load() },
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.days.isEmpty() -> FullScreenLoading()
            state.error != null && state.days.isEmpty() -> ErrorMessage(state.error ?: stringResource(R.string.unknown_error))
            else -> SleepEfficiencyDetailContent(
                day = state.today,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
    }
}

@Composable
private fun SleepEfficiencyDetailContent(
    day: RecoveryDay,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    var showCalculation by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                SleepEfficiencySummaryCard(
                    day = day,
                    unitFormatter = unitFormatter,
                    modifier = efficiencyDetailCardModifier(),
                )
            }
            item { SectionHeader(stringResource(R.string.sleep_efficiency_calculation_title)) }
            item {
                SleepEfficiencyExplanationCard(
                    expanded = showCalculation,
                    onToggleExpanded = { showCalculation = !showCalculation },
                    modifier = efficiencyDetailCardModifier(),
                )
            }
            item { SectionHeader(stringResource(R.string.sleep_efficiency_day_numbers_title)) }
            item {
                SleepEfficiencyNumbersCard(
                    day = day,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = efficiencyDetailCardModifier(),
                )
            }
            item { SectionHeader(stringResource(R.string.sleep_efficiency_references_title)) }
            item { SleepEfficiencyReferencesCard(modifier = efficiencyDetailCardModifier()) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SleepEfficiencySummaryCard(
    day: RecoveryDay,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val estimate = day.sleepScore
    EfficiencyDetailCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = null,
                tint = SleepColor,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedDayTitle(day.date),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.recovery_sleep_efficiency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = sleepEfficiencyDisplayValue(estimate, unitFormatter).value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = sleepEfficiencyConfidenceLabel(estimate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.sleep_efficiency_not_diagnostic),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SleepEfficiencyExplanationCard(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EfficiencyDetailCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.sleep_efficiency_calculation_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.sleep_efficiency_formula),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sleep_efficiency_formula_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.sleep_efficiency_data_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onToggleExpanded) {
            Text(
                text = stringResource(
                    if (expanded) {
                        R.string.action_hide_calculation
                    } else {
                        R.string.action_show_calculation
                    }
                )
            )
        }
    }
}

@Composable
private fun SleepEfficiencyNumbersCard(
    day: RecoveryDay,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val estimate = day.sleepScore
    EfficiencyDetailCard(modifier = modifier) {
        EfficiencyDetailMetricGrid(
            items = listOf(
                EfficiencyDetailMetric(
                    title = stringResource(R.string.sleep_score_efficiency),
                    value = sleepEfficiencyDisplayValue(estimate, unitFormatter),
                ),
                EfficiencyDetailMetric(
                    title = stringResource(R.string.sleep_score_total_sleep),
                    value = DisplayValue(unitFormatter.duration((estimate.sleepDurationMinutes * 60_000).roundToLong()), ""),
                ),
                EfficiencyDetailMetric(
                    title = stringResource(R.string.sleep_score_time_in_bed),
                    value = DisplayValue(unitFormatter.duration((estimate.timeInBedMinutes * 60_000).roundToLong()), ""),
                ),
                EfficiencyDetailMetric(
                    title = stringResource(R.string.sleep_score_waso),
                    value = DisplayValue(unitFormatter.count(estimate.wakeAfterSleepOnsetMinutes.roundToLong()), "min"),
                ),
                EfficiencyDetailMetric(
                    title = stringResource(R.string.recovery_sleep_schedule),
                    value = DisplayValue(sleepScheduleText(day, dateTimeFormatterProvider), ""),
                ),
                EfficiencyDetailMetric(
                    title = stringResource(R.string.sleep_score_stage_records),
                    value = DisplayValue(unitFormatter.count(estimate.sleepStageCount), ""),
                ),
            )
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = sleepEfficiencyDataQualityLabel(estimate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SleepEfficiencyReferencesCard(
    modifier: Modifier = Modifier,
) {
    EfficiencyDetailCard(modifier = modifier) {
        EfficiencyReferenceButton(
            title = stringResource(R.string.sleep_efficiency_reference_definition),
            url = NcbiSleepEfficiencyUrl,
        )
        EfficiencyReferenceButton(
            title = stringResource(R.string.sleep_efficiency_reference_denominator),
            url = SleepEfficiencyDenominatorUrl,
        )
        EfficiencyReferenceButton(
            title = stringResource(R.string.sleep_efficiency_reference_methods),
            url = SleepAssessmentMethodsUrl,
        )
    }
}

@Composable
private fun EfficiencyReferenceButton(
    title: String,
    url: String,
) {
    val uriHandler = LocalUriHandler.current
    OutlinedButton(
        onClick = { uriHandler.openUri(url) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        AutoResizeText(
            text = title,
            maxLines = 2,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun EfficiencyDetailMetricGrid(
    items: List<EfficiencyDetailMetric>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    EfficiencyDetailMetricTile(
                        metric = item,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EfficiencyDetailMetricTile(
    metric: EfficiencyDetailMetric,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AutoResizeText(
                text = metric.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                AutoResizeText(
                    text = metric.value.value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                if (metric.value.unit.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = metric.value.unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EfficiencyDetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

private data class EfficiencyDetailMetric(
    val title: String,
    val value: DisplayValue,
)

private fun efficiencyDetailCardModifier(): Modifier =
    Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

@Composable
private fun sleepEfficiencyDisplayValue(estimate: SleepScoreEstimate, unitFormatter: UnitFormatter): DisplayValue =
    if (estimate.confidence == SleepScoreConfidence.NO_DATA) {
        DisplayValue(stringResource(R.string.no_data), "")
    } else {
        unitFormatter.percent(estimate.sleepEfficiencyPercent, 0)
    }

@Composable
private fun sleepEfficiencyConfidenceLabel(estimate: SleepScoreEstimate): String =
    stringResource(
        when {
            estimate.confidence == SleepScoreConfidence.NO_DATA -> R.string.sleep_efficiency_confidence_no_data
            estimate.usesSleepStages -> R.string.sleep_efficiency_confidence_high
            else -> R.string.sleep_efficiency_confidence_low
        }
    )

@Composable
private fun sleepEfficiencyDataQualityLabel(estimate: SleepScoreEstimate): String =
    when {
        estimate.confidence == SleepScoreConfidence.NO_DATA -> stringResource(R.string.sleep_efficiency_quality_no_data)
        estimate.usesSleepStages -> stringResource(R.string.sleep_efficiency_quality_stage_based)
        else -> stringResource(R.string.sleep_efficiency_quality_session_only)
    }

@Composable
private fun sleepScheduleText(
    day: RecoveryDay,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String {
    val session = day.mainSleepSession ?: return stringResource(R.string.no_data)
    val zone = ZoneId.systemDefault()
    val formatter = dateTimeFormatterProvider.shortTime()
    val start = formatter.format(session.startTime.atZone(zone))
    val end = formatter.format(session.endTime.atZone(zone))
    return "$start - $end"
}
