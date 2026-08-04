package tech.mmarca.openvitals.features.activity

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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Favorite
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.CardioLoadEstimate
import tech.mmarca.openvitals.domain.insights.CardioLoadMethod
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.OpenVitalsCardHorizontalPadding
import tech.mmarca.openvitals.ui.components.OpenVitalsSectionSpacing
import tech.mmarca.openvitals.ui.components.SharedMetricTile
import tech.mmarca.openvitals.ui.components.localizedDayTitle
import tech.mmarca.openvitals.ui.theme.HeartColor
import kotlin.math.roundToLong

private const val BanisterTrimpUrl = "https://pmc.ncbi.nlm.nih.gov/articles/PMC6561225/"
private const val TrainingLoadReviewUrl = "https://pmc.ncbi.nlm.nih.gov/articles/PMC4213373/"
private const val HealthConnectWorkoutUrl =
    "https://developer.android.com/health-and-fitness/health-connect/experiences/workouts"

@Composable
fun CardioLoadDetailScreen(
    viewModel: ActivityOverviewViewModel,
    unitFormatter: UnitFormatter,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.isLoading && state.days.isNotEmpty(),
        onRefresh = { viewModel.load() },
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.days.isEmpty() -> FullScreenLoading()
            state.error != null && state.days.isEmpty() -> ErrorMessage(state.error?.resolve() ?: stringResource(R.string.unknown_error))
            else -> CardioLoadDetailContent(
                day = state.today,
                unitFormatter = unitFormatter,
            )
        }
    }
}

@Composable
private fun CardioLoadDetailContent(
    day: ActivityOverviewDay,
    unitFormatter: UnitFormatter,
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
                CardioLoadSummaryCard(
                    day = day,
                    unitFormatter = unitFormatter,
                    modifier = detailCardModifier(),
                )
            }
            item { SectionHeader(stringResource(R.string.cardio_load_calculation_title)) }
            item {
                ExplanationCard(
                    expanded = showCalculation,
                    onToggleExpanded = { showCalculation = !showCalculation },
                    modifier = detailCardModifier(),
                )
            }
            item { SectionHeader(stringResource(R.string.cardio_load_day_numbers_title)) }
            item {
                CardioLoadNumbersCard(
                    day = day,
                    unitFormatter = unitFormatter,
                    modifier = detailCardModifier(),
                )
            }
            item { SectionHeader(stringResource(R.string.cardio_load_references_title)) }
            item {
                ReferencesCard(
                    modifier = detailCardModifier(),
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CardioLoadSummaryCard(
    day: ActivityOverviewDay,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val estimate = day.cardioLoadScore
    DetailCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = null,
                tint = HeartColor,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedDayTitle(day.date),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.metric_cardio_load),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = cardioLoadDisplayValue(day, unitFormatter).value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = cardioLoadConfidenceLabel(estimate.confidence),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = cardioLoadMethodLabel(estimate.method),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExplanationCard(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.cardio_load_calculation_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.cardio_load_formula),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.cardio_load_formula_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.cardio_load_mapping_body),
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
private fun CardioLoadNumbersCard(
    day: ActivityOverviewDay,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val estimate = day.cardioLoadScore
    DetailCard(modifier = modifier) {
        DetailMetricGrid(
            items = listOf(
                DetailMetric(
                    title = stringResource(R.string.cardio_load_method),
                    value = DisplayValue(cardioLoadMethodLabel(estimate.method), ""),
                ),
                DetailMetric(
                    title = stringResource(R.string.cardio_load_trimp_score),
                    value = estimate.trimpScore
                        ?.let { DisplayValue(unitFormatter.decimal(it, 1), "") }
                        ?: DisplayValue(stringResource(R.string.no_data), ""),
                ),
                DetailMetric(
                    title = stringResource(R.string.cardio_load_hr_coverage),
                    value = DisplayValue(unitFormatter.decimal(estimate.coveredMinutes, 1), "min"),
                ),
                DetailMetric(
                    title = stringResource(R.string.cardio_load_expected_coverage),
                    value = DisplayValue(unitFormatter.decimal(estimate.expectedMinutes, 1), "min"),
                ),
                DetailMetric(
                    title = stringResource(R.string.cardio_load_resting_hr),
                    value = estimate.restingHeartRateBpm
                        ?.let { DisplayValue(unitFormatter.count(it), "bpm") }
                        ?: DisplayValue(stringResource(R.string.no_data), ""),
                ),
                DetailMetric(
                    title = stringResource(R.string.cardio_load_max_hr),
                    value = estimate.maxHeartRateBpm
                        ?.let { DisplayValue(unitFormatter.count(it), "bpm") }
                        ?: DisplayValue(stringResource(R.string.no_data), ""),
                ),
                DetailMetric(
                    title = stringResource(R.string.cardio_load_hr_samples),
                    value = DisplayValue(unitFormatter.count(estimate.heartRateSampleCount), ""),
                ),
                DetailMetric(
                    title = stringResource(R.string.cardio_load_activity_windows),
                    value = DisplayValue(unitFormatter.count(estimate.activityWindowCount), ""),
                ),
                DetailMetric(
                    title = stringResource(R.string.cardio_load_activity_minutes),
                    value = DisplayValue(unitFormatter.count(estimate.activityWindowMinutes.roundToLong()), "min"),
                ),
                DetailMetric(
                    title = stringResource(R.string.cardio_load_movement_fallback),
                    value = DisplayValue(unitFormatter.count(estimate.movementFallbackScore), ""),
                ),
                DetailMetric(
                    title = stringResource(R.string.metric_steps),
                    value = DisplayValue(unitFormatter.count(day.steps), stringResource(R.string.unit_steps)),
                ),
                DetailMetric(
                    title = stringResource(R.string.metric_active_calories),
                    value = day.activeCaloriesKcal
                        ?.let(unitFormatter::energy)
                        ?: DisplayValue(stringResource(R.string.no_data), ""),
                ),
            )
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = calibrationLabel(estimate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReferencesCard(
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        ReferenceButton(
            title = stringResource(R.string.cardio_load_reference_banister),
            url = BanisterTrimpUrl,
        )
        ReferenceButton(
            title = stringResource(R.string.cardio_load_reference_training_load),
            url = TrainingLoadReviewUrl,
        )
        ReferenceButton(
            title = stringResource(R.string.cardio_load_reference_health_connect),
            url = HealthConnectWorkoutUrl,
        )
    }
}

@Composable
private fun ReferenceButton(
    title: String,
    url: String,
) {
    val uriHandler = LocalUriHandler.current
    OpenVitalsOutlinedButton(
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
private fun cardioLoadDisplayValue(day: ActivityOverviewDay, unitFormatter: UnitFormatter): DisplayValue =
    if (day.cardioLoadConfidence == CardioLoadConfidence.NO_DATA) {
        DisplayValue(stringResource(R.string.no_data), "")
    } else {
        DisplayValue(unitFormatter.count(day.cardioLoad), "")
    }

@Composable
private fun cardioLoadConfidenceLabel(confidence: CardioLoadConfidence): String =
    stringResource(
        when (confidence) {
            CardioLoadConfidence.HIGH -> R.string.cardio_load_confidence_high
            CardioLoadConfidence.MEDIUM -> R.string.cardio_load_confidence_medium
            CardioLoadConfidence.LOW -> R.string.cardio_load_confidence_low
            CardioLoadConfidence.NO_DATA -> R.string.cardio_load_confidence_no_data
        }
    )

@Composable
private fun cardioLoadMethodLabel(method: CardioLoadMethod): String =
    stringResource(
        when (method) {
            CardioLoadMethod.TRIMP_ACTIVITY_WINDOWS -> R.string.cardio_load_method_activity_windows
            CardioLoadMethod.TRIMP_ELEVATED_HEART_RATE -> R.string.cardio_load_method_elevated_hr
            CardioLoadMethod.MOVEMENT_FALLBACK -> R.string.cardio_load_method_movement_fallback
            CardioLoadMethod.NO_DATA -> R.string.cardio_load_method_no_data
        }
    )

@Composable
private fun calibrationLabel(estimate: CardioLoadEstimate): String {
    val resting = if (estimate.restingHeartRateObserved) {
        stringResource(R.string.cardio_load_calibration_observed_resting)
    } else {
        stringResource(R.string.cardio_load_calibration_estimated_resting)
    }
    val max = if (estimate.maxHeartRateObserved) {
        stringResource(R.string.cardio_load_calibration_observed_max)
    } else {
        stringResource(R.string.cardio_load_calibration_estimated_max)
    }
    return "$resting / $max"
}
