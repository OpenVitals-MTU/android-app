package tech.mmarca.openvitals.features.recovery

import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.Icon
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
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.ReferenceLinkButton
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.OpenVitalsCardHorizontalPadding
import tech.mmarca.openvitals.ui.components.OpenVitalsSectionSpacing
import tech.mmarca.openvitals.ui.components.SharedMetricTile
import tech.mmarca.openvitals.ui.components.localizedDayTitle
import tech.mmarca.openvitals.ui.theme.SleepColor

private const val AasmSleepDurationUrl =
    "https://aasm.org/advocacy/position-statements/adult-sleep-duration-health-advisory/"
private const val NsfSleepDurationUrl =
    "https://doi.org/10.1016/j.sleh.2015.10.004"
private const val NsfSleepQualityUrl =
    "https://doi.org/10.1016/j.sleh.2016.11.006"
private const val GarminSleepScoreUrl =
    "https://www.garmin.com/en-US/blog/health/garmin-sleep-score-and-sleep-insights/"
private const val SleepHealthFrameworkUrl = "https://pubmed.ncbi.nlm.nih.gov/24470692/"
private const val SleepEfficiencyUrl = "https://www.ncbi.nlm.nih.gov/medgen/1669302"

@Composable
fun SleepScoreDetailScreen(
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
            state.error != null && state.days.isEmpty() -> ScreenErrorContent(
                state.error,
                fallbackMessage = stringResource(R.string.unknown_error),
            )
            else -> SleepScoreDetailContent(
                day = state.today,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
    }
}

@Composable
internal fun SleepScoreDetailContent(
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
                SleepScoreSummaryCard(
                    day = day,
                    unitFormatter = unitFormatter,
                    modifier = detailCardModifier(),
                )
            }
            item { SectionHeader(stringResource(R.string.sleep_score_calculation_title)) }
            item {
                SleepScoreExplanationCard(
                    expanded = showCalculation,
                    onToggleExpanded = { showCalculation = !showCalculation },
                    modifier = detailCardModifier(),
                )
            }
            item { SectionHeader(stringResource(R.string.sleep_score_day_numbers_title)) }
            item {
                SleepScoreNumbersCard(
                    day = day,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = detailCardModifier(),
                )
            }
            item { SectionHeader(stringResource(R.string.references_backed_links)) }
            item { SleepScoreReferencesCard(modifier = detailCardModifier()) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SleepScoreSummaryCard(
    day: RecoveryDay,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val estimate = day.sleepScore
    DetailCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.DarkMode,
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
                    text = stringResource(R.string.recovery_sleep_score),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = sleepScoreDisplayValue(estimate, unitFormatter).value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = sleepScoreConfidenceLabel(estimate.confidence),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.sleep_score_not_diagnostic),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SleepScoreExplanationCard(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.sleep_score_calculation_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.sleep_score_formula),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sleep_score_formula_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.sleep_score_components_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        OpenVitalsOutlinedButton(onClick = onToggleExpanded) {
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
private fun SleepScoreNumbersCard(
    day: RecoveryDay,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val estimate = day.sleepScore
    DetailCard(modifier = modifier) {
        DetailMetricGrid(
            items = buildList {
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_component_duration),
                        value = DisplayValue(unitFormatter.decimal(estimate.durationPoints, 1), "pts"),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_component_quality),
                        value = DisplayValue(unitFormatter.decimal(estimate.qualityPoints, 1), "pts"),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_component_recovery),
                        value = DisplayValue(unitFormatter.decimal(estimate.recoveryPoints, 1), "pts"),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_component_efficiency),
                        value = DisplayValue(unitFormatter.decimal(estimate.efficiencyPoints, 1), "pts"),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_component_continuity),
                        value = DisplayValue(unitFormatter.decimal(estimate.continuityPoints, 1), "pts"),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_component_stages),
                        value = DisplayValue(unitFormatter.decimal(estimate.stageBalancePoints, 1), "pts"),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_total_sleep),
                        value = DisplayValue(unitFormatter.duration((estimate.sleepDurationMinutes * 60_000).roundToLong()), ""),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_time_in_bed),
                        value = DisplayValue(unitFormatter.duration((estimate.timeInBedMinutes * 60_000).roundToLong()), ""),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_efficiency),
                        value = unitFormatter.percent(estimate.sleepEfficiencyPercent, 0),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_waso),
                        value = DisplayValue(unitFormatter.count(estimate.wakeAfterSleepOnsetMinutes.roundToLong()), "min"),
                    ),
                )
                estimate.deepSleepPercentOfSleep?.let { deep ->
                    add(
                        DetailMetric(
                            title = stringResource(R.string.sleep_score_deep_percent),
                            value = unitFormatter.percent(deep, 0),
                        ),
                    )
                }
                estimate.remSleepPercentOfSleep?.let { rem ->
                    add(
                        DetailMetric(
                            title = stringResource(R.string.sleep_score_rem_percent),
                            value = unitFormatter.percent(rem, 0),
                        ),
                    )
                }
                estimate.overnightHrvRmssdMs?.let { hrv ->
                    add(
                        DetailMetric(
                            title = stringResource(R.string.sleep_score_overnight_hrv),
                            value = DisplayValue(unitFormatter.decimal(hrv, 1), "ms"),
                        ),
                    )
                }
                estimate.overnightHrvBaselineRmssdMs?.let { baseline ->
                    add(
                        DetailMetric(
                            title = stringResource(R.string.sleep_score_hrv_baseline),
                            value = DisplayValue(unitFormatter.decimal(baseline, 1), "ms"),
                        ),
                    )
                }
                estimate.durationTarget?.let { target ->
                    add(
                        DetailMetric(
                            title = stringResource(R.string.sleep_score_duration_target),
                            value = DisplayValue(
                                "${unitFormatter.decimal(target.idealMinHours, 0)}–${unitFormatter.decimal(target.idealMaxHours, 0)}",
                                "h",
                            ),
                        ),
                    )
                }
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_regularity),
                        value = estimate.regularityDifferenceMinutes
                            ?.let { DisplayValue(unitFormatter.count(it.roundToLong()), "min") }
                            ?: DisplayValue(stringResource(R.string.no_data), ""),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_baseline_nights),
                        value = DisplayValue(unitFormatter.count(estimate.regularityBaselineNights), ""),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.sleep_score_stage_records),
                        value = DisplayValue(unitFormatter.count(estimate.sleepStageCount), ""),
                    ),
                )
                add(
                    DetailMetric(
                        title = stringResource(R.string.recovery_sleep_schedule),
                        value = DisplayValue(sleepScheduleText(day, dateTimeFormatterProvider), ""),
                    ),
                )
            }
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = sleepScoreDataQualityLabel(estimate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SleepScoreReferencesCard(
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        ReferenceLinkButton(
            title = stringResource(R.string.reference_garmin_sleep_score),
            url = GarminSleepScoreUrl,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        ReferenceLinkButton(
            title = stringResource(R.string.reference_nsf_sleep_duration),
            url = NsfSleepDurationUrl,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        ReferenceLinkButton(
            title = stringResource(R.string.reference_aasm_sleep_duration),
            url = AasmSleepDurationUrl,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        ReferenceLinkButton(
            title = stringResource(R.string.reference_nsf_sleep_quality),
            url = NsfSleepQualityUrl,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        ReferenceLinkButton(
            title = stringResource(R.string.reference_multidimensional_sleep_health),
            url = SleepHealthFrameworkUrl,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        ReferenceLinkButton(
            title = stringResource(R.string.reference_sleep_efficiency_definition),
            url = SleepEfficiencyUrl,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun DetailMetricGrid(
    items: List<DetailMetric>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    DetailMetricTile(
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
private fun DetailMetricTile(
    metric: DetailMetric,
    modifier: Modifier = Modifier,
) {
    SharedMetricTile(
        title = metric.title,
        value = metric.value,
        modifier = modifier,
    )
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
        ) {
        Column(
            modifier = Modifier.padding(OpenVitalsCardHorizontalPadding),
            content = content,
        )
    }
}

private data class DetailMetric(
    val title: String,
    val value: DisplayValue,
)

private fun detailCardModifier(): Modifier =
    Modifier.padding(horizontal = OpenVitalsCardHorizontalPadding, vertical = OpenVitalsSectionSpacing)

@Composable
private fun sleepScoreDisplayValue(estimate: SleepScoreEstimate, unitFormatter: UnitFormatter): DisplayValue =
    if (estimate.confidence == SleepScoreConfidence.NO_DATA) {
        DisplayValue(stringResource(R.string.no_data), "")
    } else {
        DisplayValue(unitFormatter.count(estimate.score), "")
    }

@Composable
private fun sleepScoreConfidenceLabel(confidence: SleepScoreConfidence): String =
    stringResource(
        when (confidence) {
            SleepScoreConfidence.HIGH -> R.string.sleep_score_confidence_high
            SleepScoreConfidence.MEDIUM -> R.string.sleep_score_confidence_medium
            SleepScoreConfidence.LOW -> R.string.sleep_score_confidence_low
            SleepScoreConfidence.NO_DATA -> R.string.sleep_score_confidence_no_data
        }
    )

@Composable
private fun sleepScoreDataQualityLabel(estimate: SleepScoreEstimate): String =
    when {
        estimate.confidence == SleepScoreConfidence.NO_DATA -> stringResource(R.string.sleep_score_quality_no_data)
        estimate.usesSleepStages && estimate.usesExplicitAwakeStages && estimate.usesOvernightHrv ->
            stringResource(R.string.sleep_score_quality_stage_awake_hrv)
        estimate.usesSleepStages && estimate.usesExplicitAwakeStages -> stringResource(R.string.sleep_score_quality_stage_awake)
        estimate.usesSleepStages -> stringResource(R.string.sleep_score_quality_stage_only)
        else -> stringResource(R.string.sleep_score_quality_session_only)
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
