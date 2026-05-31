package tech.mmarca.openvitals.features.recovery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.SleepColor

private val RecoveryTopCardHeight = 124.dp
private val RecoveryMetricCardHeight = 112.dp
private val RecoveryChartWidth = 168.dp
private val RecoveryChartHeight = 48.dp
@Composable
fun RecoveryScreen(
    viewModel: RecoveryViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepScore: () -> Unit,
    onOpenSleepEfficiency: () -> Unit,
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
            else -> RecoveryContent(
                state = state,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenSleepScore = onOpenSleepScore,
                onOpenSleepEfficiency = onOpenSleepEfficiency,
            )
        }
    }
}

@Composable
private fun RecoveryContent(
    state: RecoveryUiState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepScore: () -> Unit,
    onOpenSleepEfficiency: () -> Unit,
) {
    val today = state.today
    val metricDays = state.metricDays

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        ) {
            item {
                RecoveryTopCards(
                    today = today,
                    unitFormatter = unitFormatter,
                    onOpenSleepScore = onOpenSleepScore,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                SectionHeader(
                    text = stringResource(R.string.activities_key_metrics),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            item {
                RecoveryMetricCard(
                    title = stringResource(R.string.recovery_sleep_schedule),
                    value = sleepScheduleOrNoData(today, dateTimeFormatterProvider),
                    subtitle = stringResource(R.string.period_today),
                    icon = Icons.Outlined.Schedule,
                    chartValues = emptyList(),
                    dates = metricDays.map { it.date },
                    valueEmphasis = RecoveryValueEmphasis.Small,
                    modifier = metricCardModifier(),
                    chartContent = null,
                )
            }
            item {
                RecoveryMetricCard(
                    title = stringResource(R.string.recovery_rem_sleep),
                    value = durationOrNoData(today.remDurationMs, unitFormatter),
                    subtitle = stringResource(R.string.period_today),
                    icon = Icons.Outlined.DarkMode,
                    chartValues = metricDays.map { it.remDurationMs.toDouble() },
                    dates = metricDays.map { it.date },
                    modifier = metricCardModifier(),
                )
            }
            item {
                RecoveryMetricCard(
                    title = stringResource(R.string.recovery_deep_sleep),
                    value = durationOrNoData(today.deepDurationMs, unitFormatter),
                    subtitle = stringResource(R.string.period_today),
                    icon = Icons.Outlined.Bed,
                    chartValues = metricDays.map { it.deepDurationMs.toDouble() },
                    dates = metricDays.map { it.date },
                    modifier = metricCardModifier(),
                )
            }
            item {
                RecoveryMetricCard(
                    title = stringResource(R.string.recovery_sleep_efficiency),
                    value = sleepEfficiencyOrNoData(today, unitFormatter),
                    subtitle = stringResource(R.string.period_today),
                    icon = Icons.Outlined.Speed,
                    chartValues = metricDays.map { it.sleepScore.sleepEfficiencyPercent },
                    dates = metricDays.map { it.date },
                    modifier = metricCardModifier(),
                    onClick = onOpenSleepEfficiency,
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RecoveryTopCards(
    today: RecoveryDay,
    unitFormatter: UnitFormatter,
    onOpenSleepScore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RecoveryTopCard(
            title = stringResource(R.string.recovery_sleep_score),
            value = sleepScoreOrNoData(today, unitFormatter),
            subtitle = sleepScoreConfidenceLabel(today.sleepScore.confidence),
            icon = Icons.Outlined.DarkMode,
            onClick = onOpenSleepScore,
            modifier = Modifier.weight(1f),
        )
        RecoveryTopCard(
            title = stringResource(R.string.recovery_sleep_duration),
            value = durationOrNoData(today.sleepDurationMs, unitFormatter),
            subtitle = stringResource(R.string.period_today),
            icon = Icons.Outlined.Bed,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RecoveryTopCard(
    title: String,
    value: DisplayValue,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .height(RecoveryTopCardHeight)
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(SleepColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SleepColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                RecoveryValue(
                    value = value,
                    emphasis = RecoveryValueEmphasis.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RecoveryMetricCard(
    title: String,
    value: DisplayValue,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    chartValues: List<Double>,
    dates: List<LocalDate>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    valueEmphasis: RecoveryValueEmphasis = RecoveryValueEmphasis.Large,
    chartContent: (@Composable () -> Unit)? = {
        RecoverySparkline(
            values = chartValues,
            dates = dates,
            accentColor = SleepColor,
        )
    },
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(RecoveryMetricCardHeight)
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SleepColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(12.dp))
                RecoveryValue(
                    value = value,
                    emphasis = valueEmphasis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            chartContent?.invoke()
        }
    }
}

@Composable
private fun RecoverySparkline(
    values: List<Double>,
    dates: List<LocalDate>,
    accentColor: Color,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .width(RecoveryChartWidth)
                .height(RecoveryChartHeight),
        ) {
            val maxValue = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
            val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width
            val points = values.mapIndexed { index, value ->
                val yFraction = (value / maxValue).toFloat().coerceIn(0f, 1f)
                Offset(
                    x = index * stepX,
                    y = size.height - (yFraction * (size.height * 0.72f)) - (size.height * 0.14f),
                )
            }
            drawLine(
                color = accentColor.copy(alpha = 0.22f),
                start = Offset(0f, size.height * 0.75f),
                end = Offset(size.width, size.height * 0.75f),
                strokeWidth = 2.dp.toPx(),
            )
            points.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = accentColor,
                    start = start,
                    end = end,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.width(RecoveryChartWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            dates.forEach { date ->
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecoveryValue(
    value: DisplayValue,
    emphasis: RecoveryValueEmphasis = RecoveryValueEmphasis.Large,
) {
    val valueStyle = when (emphasis) {
        RecoveryValueEmphasis.Large -> MaterialTheme.typography.headlineMedium
        RecoveryValueEmphasis.Medium -> MaterialTheme.typography.headlineSmall
        RecoveryValueEmphasis.Small -> MaterialTheme.typography.titleLarge
    }
    val unitStyle = when (emphasis) {
        RecoveryValueEmphasis.Large -> MaterialTheme.typography.bodyMedium
        RecoveryValueEmphasis.Medium,
        RecoveryValueEmphasis.Small -> MaterialTheme.typography.bodySmall
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value.value,
            style = valueStyle,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (value.unit.isNotBlank()) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = value.unit,
                style = unitStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

@Composable
private fun durationOrNoData(durationMs: Long, unitFormatter: UnitFormatter): DisplayValue =
    if (durationMs > 0L) {
        DisplayValue(unitFormatter.duration(durationMs), "")
    } else {
        DisplayValue(stringResource(R.string.no_data), "")
    }

@Composable
private fun sleepScheduleOrNoData(
    day: RecoveryDay,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): DisplayValue {
    val session = day.mainSleepSession ?: return DisplayValue(stringResource(R.string.no_data), "")
    val zone = ZoneId.systemDefault()
    val formatter = dateTimeFormatterProvider.shortTime()
    val start = formatter.format(session.startTime.atZone(zone))
    val end = formatter.format(session.endTime.atZone(zone))
    return DisplayValue("$start - $end", "")
}

@Composable
private fun sleepScoreOrNoData(day: RecoveryDay, unitFormatter: UnitFormatter): DisplayValue =
    if (day.sleepScore.confidence == SleepScoreConfidence.NO_DATA) {
        DisplayValue(stringResource(R.string.no_data), "")
    } else {
        DisplayValue(unitFormatter.count(day.sleepScore.score), "")
    }

@Composable
private fun sleepEfficiencyOrNoData(day: RecoveryDay, unitFormatter: UnitFormatter): DisplayValue =
    if (day.sleepScore.confidence == SleepScoreConfidence.NO_DATA) {
        DisplayValue(stringResource(R.string.no_data), "")
    } else {
        unitFormatter.percent(day.sleepScore.sleepEfficiencyPercent, 0)
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

private fun metricCardModifier(): Modifier =
    Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

private enum class RecoveryValueEmphasis {
    Large,
    Medium,
    Small,
}
