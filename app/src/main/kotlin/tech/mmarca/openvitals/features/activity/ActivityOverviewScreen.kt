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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.CaloriesBurnedSource
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedDayTitle
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle

private val ActivityOverviewCardHeight = 132.dp
private val ActivityOverviewChartWidth = 152.dp
private val ActivityOverviewChartHeight = 58.dp
private val ActivityOverviewBarWidth = 10.dp
private val ActivityOverviewBarRadius = 8.dp
private val WeeklyExerciseMarkerSize = 42.dp

@Composable
fun ActivityOverviewScreen(
    viewModel: ActivityOverviewViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenCardioLoad: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenDistance: () -> Unit,
    onOpenEnergyBurned: () -> Unit,
    onOpenHrv: () -> Unit,
    onOpenActivity: (String) -> Unit,
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
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenCardioLoad = onOpenCardioLoad,
                onOpenSteps = onOpenSteps,
                onOpenDistance = onOpenDistance,
                onOpenEnergyBurned = onOpenEnergyBurned,
                onOpenHrv = onOpenHrv,
                onOpenActivity = onOpenActivity,
            )
        }
    }
}

@Composable
private fun ActivityOverviewContent(
    state: ActivityOverviewUiState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenCardioLoad: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenDistance: () -> Unit,
    onOpenEnergyBurned: () -> Unit,
    onOpenHrv: () -> Unit,
    onOpenActivity: (String) -> Unit,
) {
    val today = state.today
    val metricDays = state.metricDays
    val weeklyOverviewDays = metricDays

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
                WeeklyActivityOverviewSection(
                    days = weeklyOverviewDays,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenActivity = onOpenActivity,
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
                    chartValues = metricDays.map {
                        if (it.cardioLoadConfidence == CardioLoadConfidence.NO_DATA) 0.0 else it.cardioLoad.toDouble()
                    },
                    chartStyle = ActivityMetricChartStyle.LINE,
                    chartDays = metricDays.map { it.date },
                    modifier = metricCardModifier(),
                    onClick = onOpenCardioLoad,
                )
            }
            item {
                ActivityMetricCard(
                    title = stringResource(R.string.metric_energy_burned),
                    value = unitFormatter.energy(today.energyBurnedKcal),
                    subtitle = if (today.energyBurnedSource == CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR) {
                        stringResource(R.string.calories_estimated_active_bmr)
                    } else {
                        stringResource(R.string.period_today)
                    },
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
                ActivityMetricCard(
                    title = stringResource(R.string.metric_hrv),
                    value = today.hrvRmssdMs
                        ?.let(unitFormatter::hrv)
                        ?: DisplayValue(stringResource(R.string.no_data), ""),
                    subtitle = stringResource(R.string.period_today),
                    icon = Icons.Outlined.FavoriteBorder,
                    accentColor = HeartColor,
                    chartValues = metricDays.map { it.hrvRmssdMs ?: 0.0 },
                    chartStyle = ActivityMetricChartStyle.LINE,
                    chartDays = metricDays.map { it.date },
                    modifier = metricCardModifier(),
                    onClick = onOpenHrv,
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun WeeklyActivityOverviewSection(
    days: List<ActivityOverviewDay>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivity: (String) -> Unit,
) {
    val weekDays = days.sortedBy { it.date }
    val weekWorkouts = weekDays
        .flatMap { it.workouts }
        .distinctBy { it.id }
        .sortedByDescending { it.startTime }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.activities_weekly_overview_title))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column {
                WeeklyExerciseStrip(
                    days = weekDays,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                )
                if (weekWorkouts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.message_no_workouts_week),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.activities_this_week_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
                    )
                    weekWorkouts.forEachIndexed { index, workout ->
                        WeeklyWorkoutRow(
                            workout = workout,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onClick = { onOpenActivity(workout.id) },
                        )
                        if (index < weekWorkouts.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyExerciseStrip(
    days: List<ActivityOverviewDay>,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        days.forEach { day ->
            val workout = day.workouts.firstOrNull()
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier.size(WeeklyExerciseMarkerSize),
                    contentAlignment = Alignment.Center,
                ) {
                    if (workout != null) {
                        Box(
                            modifier = Modifier
                                .size(WeeklyExerciseMarkerSize)
                                .background(WorkoutColor, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = exerciseTypeIcon(workout.exerciseType),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(23.dp),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                    }
                }
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(1),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeeklyWorkoutRow(
    workout: ExerciseData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val start = workout.startTime.atZone(zone)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(WorkoutColor.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = exerciseTypeIcon(workout.exerciseType),
                contentDescription = null,
                tint = WorkoutColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workout.title ?: exerciseTypeLabel(workout.exerciseType),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${localizedDayTitle(start.toLocalDate())} / ${dateTimeFormatterProvider.shortTime().format(start)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = unitFormatter.duration(workout.durationMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.detail_duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
