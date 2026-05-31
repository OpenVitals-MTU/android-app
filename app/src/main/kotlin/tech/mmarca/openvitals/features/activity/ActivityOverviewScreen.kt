package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.CardioLoadConfidence
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedDayTitle
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import java.time.LocalDate
import java.time.format.TextStyle

private val ActivityOverviewCardHeight = 132.dp
private val ActivityOverviewChartWidth = 152.dp
private val ActivityOverviewChartHeight = 58.dp
private val ActivityOverviewBarWidth = 10.dp
private val ActivityOverviewBarRadius = 8.dp

@Composable
fun ActivityOverviewScreen(
    viewModel: ActivityOverviewViewModel,
    unitFormatter: UnitFormatter,
    onOpenCardioLoad: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenDistance: () -> Unit,
    onOpenEnergyBurned: () -> Unit,
    onOpenHrv: () -> Unit,
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
            else -> ActivityOverviewContent(
                state = state,
                unitFormatter = unitFormatter,
                onLoadMore = viewModel::loadMoreRecentActivities,
                onOpenCardioLoad = onOpenCardioLoad,
                onOpenSteps = onOpenSteps,
                onOpenDistance = onOpenDistance,
                onOpenEnergyBurned = onOpenEnergyBurned,
                onOpenHrv = onOpenHrv,
            )
        }
    }
}

@Composable
private fun ActivityOverviewContent(
    state: ActivityOverviewUiState,
    unitFormatter: UnitFormatter,
    onLoadMore: () -> Unit,
    onOpenCardioLoad: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenDistance: () -> Unit,
    onOpenEnergyBurned: () -> Unit,
    onOpenHrv: () -> Unit,
) {
    val today = state.today
    val metricDays = state.metricDays
    val cardioMetricDays = metricDays.filter { it.cardioLoadConfidence != CardioLoadConfidence.NO_DATA }

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
                RecentActivitiesSection(
                    activities = state.visibleRecentActivities,
                    canLoadMore = state.canLoadMoreRecentActivities,
                    unitFormatter = unitFormatter,
                    onLoadMore = onLoadMore,
                )
            }

            item {
                SectionHeader(
                    text = stringResource(R.string.activities_key_metrics),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            item {
                ActivityMetricCard(
                    title = stringResource(R.string.metric_cardio_load),
                    value = cardioLoadDisplayValue(today, unitFormatter),
                    subtitle = "${stringResource(R.string.period_today)} / ${cardioLoadConfidenceLabel(today.cardioLoadConfidence)}",
                    icon = Icons.Outlined.Favorite,
                    accentColor = HeartColor,
                    chartValues = cardioMetricDays.map { it.cardioLoad.toDouble() },
                    chartStyle = ActivityMetricChartStyle.LINE,
                    chartDays = cardioMetricDays.map { it.date },
                    modifier = metricCardModifier(),
                    onClick = onOpenCardioLoad,
                )
            }
            item {
                ActivityMetricCard(
                    title = stringResource(R.string.metric_energy_burned),
                    value = unitFormatter.energy(today.energyBurnedKcal),
                    subtitle = stringResource(R.string.period_today),
                    icon = Icons.Outlined.LocalFireDepartment,
                    accentColor = CaloriesColor,
                    chartValues = metricDays.map { it.energyBurnedKcal },
                    chartStyle = ActivityMetricChartStyle.BAR,
                    chartDays = metricDays.map { it.date },
                    modifier = metricCardModifier(),
                    onClick = onOpenEnergyBurned,
                )
            }
            item {
                ActivityMetricCard(
                    title = stringResource(R.string.metric_steps),
                    value = DisplayValue(unitFormatter.count(today.steps), ""),
                    subtitle = stringResource(R.string.period_today),
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    accentColor = StepsColor,
                    chartValues = metricDays.map { it.steps.toDouble() },
                    chartStyle = ActivityMetricChartStyle.BAR,
                    chartDays = metricDays.map { it.date },
                    modifier = metricCardModifier(),
                    onClick = onOpenSteps,
                )
            }
            item {
                ActivityMetricCard(
                    title = stringResource(R.string.metric_distance),
                    value = unitFormatter.distance(today.distanceMeters),
                    subtitle = stringResource(R.string.period_today),
                    icon = Icons.Outlined.Straighten,
                    accentColor = DistanceColor,
                    chartValues = metricDays.map { it.distanceMeters },
                    chartStyle = ActivityMetricChartStyle.BAR,
                    chartDays = metricDays.map { it.date },
                    modifier = metricCardModifier(),
                    onClick = onOpenDistance,
                )
            }
            item {
                val hrvMetricDays = metricDays.filter { it.hrvRmssdMs != null }
                ActivityMetricCard(
                    title = stringResource(R.string.metric_hrv),
                    value = today.hrvRmssdMs
                        ?.let(unitFormatter::hrv)
                        ?: DisplayValue(stringResource(R.string.no_data), ""),
                    subtitle = stringResource(R.string.period_today),
                    icon = Icons.Outlined.FavoriteBorder,
                    accentColor = HeartColor,
                    chartValues = hrvMetricDays.map { it.hrvRmssdMs ?: 0.0 },
                    chartStyle = ActivityMetricChartStyle.LINE,
                    chartDays = hrvMetricDays.map { it.date },
                    modifier = metricCardModifier(),
                    onClick = onOpenHrv,
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RecentActivitiesSection(
    activities: List<ActivityOverviewDay>,
    canLoadMore: Boolean,
    unitFormatter: UnitFormatter,
    onLoadMore: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.activities_recent_title))
        if (activities.isEmpty()) {
            Text(
                text = stringResource(R.string.message_no_recent_activity),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            activities.forEach { day ->
                RecentActivityRow(
                    day = day,
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (canLoadMore) {
                OutlinedButton(
                    onClick = onLoadMore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Text(stringResource(R.string.action_load_more))
                }
            }
        }
    }
}

@Composable
private fun RecentActivityRow(
    day: ActivityOverviewDay,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(StepsColor.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    contentDescription = null,
                    tint = StepsColor,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedDayTitle(day.date),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.activity_overview_daily_activity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = recentActivitySummary(day, unitFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val cardioLoad = cardioLoadDisplayValue(day, unitFormatter)
                Text(
                    text = cardioLoad.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.metric_cardio_load),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = cardioLoadConfidenceLabel(day.cardioLoadConfidence),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActivityMetricCard(
    title: String,
    value: DisplayValue,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    chartValues: List<Double>,
    chartStyle: ActivityMetricChartStyle,
    chartDays: List<LocalDate>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .height(ActivityOverviewCardHeight)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
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
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = value.value,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        if (value.unit.isNotBlank()) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = value.unit,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 5.dp),
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ActivityMetricSparkline(
                values = chartValues,
                dates = chartDays,
                style = chartStyle,
                accentColor = accentColor,
            )
        }
    }
}

@Composable
private fun ActivityMetricSparkline(
    values: List<Double>,
    dates: List<LocalDate>,
    style: ActivityMetricChartStyle,
    accentColor: Color,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (style) {
            ActivityMetricChartStyle.BAR -> ActivityMiniBarChart(values, accentColor)
            ActivityMetricChartStyle.LINE -> ActivityMiniLineChart(values, accentColor)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.width(ActivityOverviewChartWidth),
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
private fun ActivityMiniBarChart(values: List<Double>, accentColor: Color) {
    val maxValue = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
    Row(
        modifier = Modifier
            .width(ActivityOverviewChartWidth)
            .height(ActivityOverviewChartHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { value ->
            val fraction = (value / maxValue).toFloat().coerceIn(0.12f, 1f)
            Box(
                modifier = Modifier
                    .width(ActivityOverviewBarWidth)
                    .height(ActivityOverviewChartHeight * fraction)
                    .background(accentColor.copy(alpha = 0.82f), RoundedCornerShape(ActivityOverviewBarRadius)),
            )
        }
    }
}

@Composable
private fun ActivityMiniLineChart(values: List<Double>, accentColor: Color) {
    Canvas(
        modifier = Modifier
            .width(ActivityOverviewChartWidth)
            .height(ActivityOverviewChartHeight),
    ) {
        val maxValue = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val minValue = values.minOrNull()?.takeIf { it < maxValue } ?: 0.0
        val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width
        val points = values.mapIndexed { index, value ->
            val yFraction = ((value - minValue) / range).toFloat().coerceIn(0f, 1f)
            Offset(
                x = index * stepX,
                y = size.height - (yFraction * (size.height * 0.78f)) - (size.height * 0.1f),
            )
        }
        drawLine(
            color = accentColor.copy(alpha = 0.55f),
            start = Offset(0f, size.height * 0.72f),
            end = Offset(size.width, size.height * 0.72f),
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
        points.forEach { point ->
            drawCircle(
                color = accentColor,
                radius = 5.dp.toPx(),
                center = point,
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = accentColor,
                radius = 2.5.dp.toPx(),
                center = point,
            )
        }
    }
}

private enum class ActivityMetricChartStyle {
    BAR,
    LINE,
}

private fun metricCardModifier(): Modifier =
    Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

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
private fun recentActivitySummary(day: ActivityOverviewDay, unitFormatter: UnitFormatter): String =
    listOf(
        "${unitFormatter.count(day.steps)} ${stringResource(R.string.unit_steps)}",
        unitFormatter.distance(day.distanceMeters).text,
        unitFormatter.energy(day.energyBurnedKcal).text,
    ).joinToString(" / ")
