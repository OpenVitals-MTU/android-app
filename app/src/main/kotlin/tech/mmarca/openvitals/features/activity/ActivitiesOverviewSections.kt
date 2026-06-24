package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.metadata.Metadata
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.WorkoutGuidelineStatus
import tech.mmarca.openvitals.domain.insights.crossMetricInsight
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.domain.insights.workoutGuidelineProgress
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.MetricSparklineChart
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.SwipeToDeleteEntryRow
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedDayTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToLong

private val ActivityOverviewCardHeight = 132.dp
private val ActivityOverviewChartWidth = 152.dp
private val ActivityOverviewChartHeight = 58.dp
private val ActivityOverviewMarkerSize = 38.dp
private const val ActivityWorkoutListPageSize = 10

@Composable
internal fun activityPeriodTitle(
    selectedRange: TimeRange,
    activityWeekMode: ActivityWeekMode,
    period: DatePeriod,
): String =
    if (selectedRange == TimeRange.WEEK && activityWeekMode == ActivityWeekMode.LAST_7_DAYS) {
        stringResource(R.string.settings_activity_week_last_7_days)
    } else {
        localizedPeriodTitle(selectedRange, period)
    }

@Composable
internal fun ActivityOverviewPeriodCard(
    days: List<ActivityOverviewDay>,
    selectedRange: TimeRange,
    activityWeekMode: ActivityWeekMode,
    period: DatePeriod,
) {
    val stripBuckets = activityOverviewBuckets(
        days = days,
        selectedRange = selectedRange,
        maxBuckets = 7,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = activityPeriodTitle(selectedRange, activityWeekMode, period))
        Card(
            modifier = metricModifier(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column {
                ActivityOverviewStrip(
                    buckets = stripBuckets,
                    selectedRange = selectedRange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
internal fun ActivityWorkoutListCard(
    workouts: List<ExerciseData>,
    title: String,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onDeleteActivity: (String) -> Unit,
) {
    var visibleCount by remember(workouts) {
        mutableIntStateOf(workouts.size.coerceAtMost(ActivityWorkoutListPageSize))
    }
    val boundedVisibleCount = visibleCount.coerceAtMost(workouts.size)
    val visibleWorkouts = workouts.take(boundedVisibleCount)

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = stringResource(R.string.section_activities))
        Card(
            modifier = metricModifier(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            if (workouts.isEmpty()) {
                Text(
                    text = stringResource(R.string.message_no_activities_period),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
                return@Card
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
            )
            visibleWorkouts.forEachIndexed { index, workout ->
                ActivityOverviewWorkoutRow(
                    workout = workout,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onClick = { onOpenActivity(workout.id) },
                    onEdit = workout.editAction(onEditActivity),
                    onDelete = workout.deleteAction(onDeleteActivity),
                )
                if (index < visibleWorkouts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
            }
            if (boundedVisibleCount < workouts.size) {
                OutlinedButton(
                    onClick = {
                        visibleCount = (boundedVisibleCount + ActivityWorkoutListPageSize)
                            .coerceAtMost(workouts.size)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(stringResource(R.string.action_load_more_entries))
                }
            }
        }
    }
}

internal fun LazyListScope.plannedWorkoutListSection(
    plannedWorkouts: List<PlannedExerciseData>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (plannedWorkouts.isEmpty()) return

    item {
        PlannedWorkoutListCard(
            plannedWorkouts = plannedWorkouts,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
    }
}

@Composable
internal fun PlannedWorkoutListCard(
    plannedWorkouts: List<PlannedExerciseData>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = stringResource(R.string.section_planned_workouts))
        Card(
            modifier = metricModifier(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            plannedWorkouts.sortedBy { it.startTime }.forEachIndexed { index, plannedWorkout ->
                PlannedWorkoutRow(
                    plannedWorkout = plannedWorkout,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
                if (index < plannedWorkouts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlannedWorkoutRow(
    plannedWorkout: PlannedExerciseData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val zone = ZoneId.systemDefault()
    val start = plannedWorkout.startTime.atZone(zone)
    val end = plannedWorkout.endTime.atZone(zone)
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                imageVector = if (plannedWorkout.completedExerciseSessionId != null) {
                    Icons.Outlined.CheckCircle
                } else {
                    exerciseTypeIcon(plannedWorkout.exerciseType)
                },
                contentDescription = null,
                tint = WorkoutColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            AutoResizeText(
                text = plannedWorkout.title ?: exerciseTypeLabel(plannedWorkout.exerciseType),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            AutoResizeText(
                text = "${localizedDayTitle(start.toLocalDate())} / ${dateTimeFormatterProvider.shortTime().format(start)}" +
                    " - ${dateTimeFormatterProvider.shortTime().format(end)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (plannedWorkout.notes?.isNotBlank() == true) {
                AutoResizeText(
                    text = plannedWorkout.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = unitFormatter.duration(plannedWorkout.durationMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (plannedWorkout.completedExerciseSessionId != null) {
                    stringResource(R.string.planned_workout_completed)
                } else {
                    stringResource(R.string.planned_workout_blocks, plannedWorkout.blockCount)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ActivityOverviewStrip(
    buckets: List<ActivityOverviewBucket>,
    selectedRange: TimeRange,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        buckets.forEach { bucket ->
            val workout = bucket.workouts.firstOrNull()
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier.size(ActivityOverviewMarkerSize),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        workout != null -> {
                            Box(
                                modifier = Modifier
                                    .size(ActivityOverviewMarkerSize)
                                    .background(WorkoutColor, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = exerciseTypeIcon(workout.exerciseType),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        bucket.hasActivity -> {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(StepsColor.copy(alpha = 0.86f), CircleShape),
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            )
                        }
                    }
                }
                Text(
                    text = activityOverviewBucketLabel(bucket.date, selectedRange, locale),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
internal fun ActivityOverviewWorkoutRow(
    workout: ExerciseData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (onDelete != null) {
        SwipeToDeleteEntryRow(
            onDelete = onDelete,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
        ) {
            ActivityOverviewWorkoutRowContent(
                workout = workout,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onClick = onClick,
                onEdit = onEdit,
                opaqueBackground = true,
            )
        }
    } else {
        ActivityOverviewWorkoutRowContent(
            workout = workout,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onClick = onClick,
            onEdit = onEdit,
            modifier = modifier,
        )
    }
}

@Composable
internal fun ActivityOverviewWorkoutRowContent(
    workout: ExerciseData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    opaqueBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = workout.startTime.atZone(zone)
    val rowShape = RoundedCornerShape(8.dp)
    val rowModifier = if (opaqueBackground) {
        modifier.background(MaterialTheme.colorScheme.surfaceContainer, rowShape)
    } else {
        modifier
    }
    Row(
        modifier = rowModifier
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
            AutoResizeText(
                text = workout.title ?: exerciseTypeLabel(workout.exerciseType),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            AutoResizeText(
                text = "${localizedDayTitle(start.toLocalDate())} / ${dateTimeFormatterProvider.shortTime().format(start)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
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
        if (onEdit != null) {
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.cd_edit_entry),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ActivityMetricCard(
    title: String,
    value: DisplayValue,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    chartValues: List<Double>,
    chartDays: List<LocalDate>,
    selectedRange: TimeRange,
    modifier: Modifier = Modifier,
    subtitleColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp),
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
                    AutoResizeText(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        AutoResizeText(
                            text = value.value,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        if (value.unit.isNotBlank()) {
                            Spacer(Modifier.width(4.dp))
                            AutoResizeText(
                                text = value.unit,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 5.dp),
                                maxLines = 1,
                            )
                        }
                    }
                    AutoResizeText(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            ActivityMetricSparkline(
                values = chartValues,
                dates = chartDays,
                selectedRange = selectedRange,
                accentColor = accentColor,
            )
        }
    }
}

@Composable
internal fun ActivityMetricSparkline(
    values: List<Double>,
    dates: List<LocalDate>,
    selectedRange: TimeRange,
    accentColor: Color,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val maxValue = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val minValue = values.minOrNull()?.takeIf { it < maxValue } ?: 0.0
        MetricSparklineChart(
            values = values,
            accentColor = accentColor,
            modifier = Modifier
                .width(ActivityOverviewChartWidth)
                .height(ActivityOverviewChartHeight),
            minValue = minValue,
            baselineFraction = 0.72f,
            baselineAlpha = 0.55f,
            verticalScaleFraction = 0.78f,
            topPaddingFraction = 0.1f,
            pointRadius = 5.dp,
            pointStrokeWidth = 2.dp,
            pointFillRadius = 2.5.dp,
            singlePointLine = true,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.width(ActivityOverviewChartWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            dates.forEach { date ->
                Text(
                    text = activityOverviewBucketLabel(date, selectedRange, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

internal fun activityOverviewMetricSeries(
    days: List<ActivityOverviewDay>,
    selectedRange: TimeRange,
    aggregation: ActivityOverviewMetricAggregation,
    valueSelector: (ActivityOverviewDay) -> Double?,
): ActivityOverviewMetricSeries {
    val maxBuckets = if (selectedRange == TimeRange.YEAR) 12 else 7
    val buckets = activityOverviewBuckets(days, selectedRange, maxBuckets)
    return ActivityOverviewMetricSeries(
        dates = buckets.map { it.date },
        values = buckets.map { bucket ->
            val values = bucket.days.mapNotNull(valueSelector)
            when {
                values.isEmpty() -> 0.0
                aggregation == ActivityOverviewMetricAggregation.AVERAGE -> values.average()
                else -> values.sum()
            }
        },
    )
}

internal fun activityOverviewBuckets(
    days: List<ActivityOverviewDay>,
    selectedRange: TimeRange,
    maxBuckets: Int,
): List<ActivityOverviewBucket> {
    val sortedDays = days.sortedBy { it.date }
    val rawBuckets = when (selectedRange) {
        TimeRange.DAY,
        TimeRange.WEEK,
        TimeRange.MONTH -> sortedDays.map { day ->
            ActivityOverviewBucket(date = day.date, days = listOf(day))
        }

        TimeRange.YEAR -> sortedDays
            .groupBy { YearMonth.from(it.date) }
            .toSortedMap()
            .map { (_, monthDays) ->
                ActivityOverviewBucket(date = monthDays.first().date, days = monthDays)
            }
    }
    return rawBuckets.limitActivityOverviewBuckets(maxBuckets)
}

internal fun List<ActivityOverviewBucket>.limitActivityOverviewBuckets(maxBuckets: Int): List<ActivityOverviewBucket> {
    if (maxBuckets <= 0 || isEmpty()) return emptyList()
    if (size <= maxBuckets) return this

    val chunkSize = ceil(size.toDouble() / maxBuckets.toDouble()).toInt().coerceAtLeast(1)
    return chunked(chunkSize).map { bucketChunk ->
        ActivityOverviewBucket(
            date = bucketChunk.first().date,
            days = bucketChunk.flatMap { it.days },
        )
    }
}

internal fun activityOverviewBucketLabel(
    date: LocalDate,
    selectedRange: TimeRange,
    locale: java.util.Locale,
): String = when (selectedRange) {
    TimeRange.DAY,
    TimeRange.WEEK -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(1)
    TimeRange.MONTH -> date.dayOfMonth.toString()
    TimeRange.YEAR -> date.month.getDisplayName(TextStyle.SHORT, locale).take(1)
}

internal fun activityOverviewTotals(days: List<ActivityOverviewDay>): ActivityOverviewTotals {
    val hrvValues = days.mapNotNull { it.hrvRmssdMs }
    val cardioLoadDays = days.filter { it.cardioLoadConfidence != CardioLoadConfidence.NO_DATA }
    return ActivityOverviewTotals(
        steps = days.sumOf { it.steps },
        distanceMeters = days.sumOf { it.distanceMeters },
        energyBurnedKcal = days.sumOf { it.energyBurnedKcal },
        hasEnergyBurnedData = days.any { it.energyBurnedSource != CaloriesBurnedSource.NO_DATA },
        cardioLoad = cardioLoadDays.sumOf { it.cardioLoad },
        hasCardioLoadData = cardioLoadDays.isNotEmpty(),
        cardioLoadConfidence = aggregateCardioLoadConfidence(cardioLoadDays),
        hrvRmssdMs = hrvValues.takeIf { it.isNotEmpty() }?.average(),
    )
}

internal fun aggregateCardioLoadConfidence(days: List<ActivityOverviewDay>): CardioLoadConfidence =
    when {
        days.isEmpty() -> CardioLoadConfidence.NO_DATA
        days.any { it.cardioLoadConfidence == CardioLoadConfidence.LOW } -> CardioLoadConfidence.LOW
        days.any { it.cardioLoadConfidence == CardioLoadConfidence.MEDIUM } -> CardioLoadConfidence.MEDIUM
        else -> CardioLoadConfidence.HIGH
    }

@Composable
internal fun cardioLoadDisplayValue(
    score: Int,
    hasData: Boolean,
    unitFormatter: UnitFormatter,
): DisplayValue =
    if (hasData) {
        DisplayValue(unitFormatter.count(score), "")
    } else {
        DisplayValue(stringResource(R.string.no_data), "")
    }

@Composable
internal fun activityOverviewCardioLoadConfidenceLabel(confidence: CardioLoadConfidence): String =
    stringResource(
        when (confidence) {
            CardioLoadConfidence.HIGH -> R.string.cardio_load_confidence_high
            CardioLoadConfidence.MEDIUM -> R.string.cardio_load_confidence_medium
            CardioLoadConfidence.LOW -> R.string.cardio_load_confidence_low
            CardioLoadConfidence.NO_DATA -> R.string.cardio_load_confidence_no_data
        }
    )

internal enum class ActivityOverviewMetricAggregation {
    SUM,
    AVERAGE,
}

internal data class ActivityOverviewMetricSeries(
    val dates: List<LocalDate>,
    val values: List<Double>,
)

internal data class ActivityOverviewBucket(
    val date: LocalDate,
    val days: List<ActivityOverviewDay>,
) {
    val workouts: List<ExerciseData>
        get() = days.flatMap { it.workouts }.distinctBy { it.id }

    val hasActivity: Boolean
        get() = days.any { it.hasActivity }
}

internal data class ActivityOverviewTotals(
    val steps: Long,
    val distanceMeters: Double,
    val energyBurnedKcal: Double,
    val hasEnergyBurnedData: Boolean,
    val cardioLoad: Int,
    val hasCardioLoadData: Boolean,
    val cardioLoadConfidence: CardioLoadConfidence,
    val hrvRmssdMs: Double?,
)

internal fun metricCardModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)

internal fun LazyListScope.workoutDataConfidence(
    workouts: List<ExerciseData>,
    period: DatePeriod,
) {
    if (period.start == period.end) return

    val zone = ZoneId.systemDefault()
    item {
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates = workouts.map { it.startTime.atZone(zone).toLocalDate() },
                sampleCount = workouts.size,
                sources = workouts.map { it.source },
                valueKind = DataValueKind.MEASURED,
                manualEntryCount = workouts.count {
                    it.recordingMethod == Metadata.RECORDING_METHOD_MANUAL_ENTRY
                },
            ),
            accentColor = WorkoutColor,
            modifier = metricModifier(),
        )
    }
}

internal fun LazyListScope.workoutGuidelineContext(
    workouts: List<ExerciseData>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val totalLoggedMinutes = workouts.sumOf { it.durationMs.coerceAtLeast(0L) }.toDouble() / 60_000.0
    val useWeeklyAverage = selectedRange == TimeRange.MONTH || selectedRange == TimeRange.YEAR
    val guidelineMinutes = if (useWeeklyAverage) {
        totalLoggedMinutes / period.weekCount()
    } else {
        totalLoggedMinutes
    }
    val progress = workoutGuidelineProgress(guidelineMinutes) ?: return
    item { SectionHeader(stringResource(R.string.section_metric_context)) }
    item {
        MetricInterpretationCard(
            title = stringResource(R.string.interpretation_workout_title),
            status = when (progress.status) {
                WorkoutGuidelineStatus.NO_LOGGED_MINUTES -> stringResource(R.string.interpretation_workout_none)
                WorkoutGuidelineStatus.BELOW_REFERENCE -> stringResource(R.string.interpretation_workout_below)
                WorkoutGuidelineStatus.APPROACHING_REFERENCE -> stringResource(R.string.interpretation_workout_approaching)
                WorkoutGuidelineStatus.MEETS_REFERENCE -> stringResource(R.string.interpretation_workout_met)
            },
            body = stringResource(
                if (useWeeklyAverage) {
                    R.string.interpretation_workout_body_weekly_average
                } else {
                    R.string.interpretation_workout_body
                },
                unitFormatter.minutes(progress.loggedMinutes.roundToLong()).text,
                unitFormatter.percent(progress.percentOfReference, decimals = 0).text,
            ),
            source = stringResource(R.string.interpretation_workout_source),
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = WorkoutColor,
            severity = progress.severity,
            modifier = metricModifier(),
        )
    }
}

internal fun DatePeriod.weekCount(): Double {
    val days = ChronoUnit.DAYS.between(start, end).toDouble() + 1.0
    return (days / 7.0).coerceAtLeast(1.0 / 7.0)
}

internal fun LazyListScope.workoutGoal(
    state: ActivitiesUiState,
    period: DatePeriod,
    values: List<DailyGoalValue>,
    unitFormatter: UnitFormatter,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val goalKey = MetricDailyGoalKey.WORKOUT_MINUTES
    val progress = dailyGoalProgress(
        values = values,
        period = period,
        target = state.dailyGoalMinutes,
        direction = goalKey.direction,
    )
    item {
        DailyGoalCard(
            goal = unitFormatter.minutes(state.dailyGoalMinutes.roundToLong()),
            progress = progress,
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = WorkoutColor,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
            modifier = metricModifier(),
        )
    }
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        DailyGoalStatistics(
            progress = progress,
            averageGap = unitFormatter.minutes(progress.averageGapToGoal.roundToLong()),
            unitFormatter = unitFormatter,
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = WorkoutColor,
            modifier = metricModifier(),
        )
    }
}

internal fun LazyListScope.workoutStatistics(
    workouts: List<ExerciseData>,
    previousWorkouts: List<ExerciseData>,
    baselineWorkouts: List<ExerciseData>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        item { SectionHeader(stringResource(R.string.section_statistics)) }
    }
    item {
        val totalMs = workouts.sumOf { it.durationMs.coerceAtLeast(0L) }
        val averageMs = workouts.takeIf { it.isNotEmpty() }
            ?.let { totalMs / it.size }
            ?: 0L
        val longestMs = workouts.maxOfOrNull { it.durationMs.coerceAtLeast(0L) } ?: 0L
        val previousTotalMs = previousWorkouts.sumOf { it.durationMs.coerceAtLeast(0L) }
        val dailyMinutes = workoutDailyGoalValues(workouts).map { it.value }
        val baselineValues = workoutDailyGoalValues(baselineWorkouts)
            .map { BaselineValue(it.date, it.value) }

        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.stat_total),
                    value = unitFormatter.duration(totalMs),
                    unit = "",
                    icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                    accentColor = WorkoutColor,
                ),
                InsightStat(
                    title = stringResource(R.string.section_activities),
                    value = unitFormatter.count(workouts.size),
                    unit = "",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = WorkoutColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_average_duration),
                    value = unitFormatter.duration(averageMs),
                    unit = "",
                    icon = Icons.Outlined.Star,
                    accentColor = WorkoutColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_longest_workout),
                    value = unitFormatter.duration(longestMs),
                    unit = "",
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = WorkoutColor,
                ),
                previousPeriodInsightStat(
                    comparison = periodComparison(
                        currentValue = totalMs.toDouble(),
                        previousValue = previousTotalMs.toDouble(),
                    ),
                    selectedRange = selectedRange,
                    unitFormatter = unitFormatter,
                    valueFormatter = { DisplayValue(unitFormatter.duration(it.roundToLong()), "") },
                    accentColor = WorkoutColor,
                ),
            ) + personalBaselineInsightStats(
                insight = personalBaselineInsight(
                    currentValue = dailyMinutes.takeIf { it.isNotEmpty() }?.average() ?: 0.0,
                    values = baselineValues,
                    referenceDate = period.start.minusDays(1),
                ),
                unitFormatter = unitFormatter,
                valueFormatter = { unitFormatter.minutes(it.roundToLong()) },
                accentColor = WorkoutColor,
            ),
            modifier = metricModifier(),
        )
    }
}

internal fun workoutDailyGoalValues(workouts: List<ExerciseData>): List<DailyGoalValue> {
    val zone = ZoneId.systemDefault()
    return workouts
        .groupBy { it.startTime.atZone(zone).toLocalDate() }
        .map { (date, dayWorkouts) ->
            DailyGoalValue(
                date = date,
                value = dayWorkouts.sumOf { it.durationMs.coerceAtLeast(0L) }.toDouble() / 60_000.0,
            )
        }
}

internal fun LazyListScope.workoutRestingHrInsight(
    workouts: List<ExerciseData>,
    restingHr: List<DailyRestingHR>,
) {
    val insight = crossMetricInsight(
        primaryValues = workoutDailyGoalValues(workouts)
            .map { CrossMetricValue(it.date, it.value) },
        secondaryValues = restingHr.map { CrossMetricValue(it.date, it.bpm.toDouble()) },
    ) ?: return

    item { SectionHeader(stringResource(R.string.section_cross_metric_insights)) }
    item {
        CrossMetricInsightCard(
            insight = insight,
            title = stringResource(R.string.cross_workout_resting_hr_title),
            positiveMessage = stringResource(R.string.cross_workout_resting_hr_positive),
            negativeMessage = stringResource(R.string.cross_workout_resting_hr_negative),
            neutralMessage = stringResource(R.string.cross_workout_resting_hr_neutral),
            accentColor = WorkoutColor,
            modifier = metricModifier(),
        )
    }
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

internal fun ExerciseData.editAction(onEditActivity: (String) -> Unit): (() -> Unit)? =
    if (isOpenVitalsEntry && id.isNotBlank()) {
        { onEditActivity(id) }
    } else {
        null
    }

internal fun ExerciseData.deleteAction(onDeleteActivity: (String) -> Unit): (() -> Unit)? =
    if (isOpenVitalsEntry && id.isNotBlank()) {
        { onDeleteActivity(id) }
    } else {
        null
    }
