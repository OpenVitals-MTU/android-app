package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import java.time.LocalDate

object ActivityPresentationMapper {

    fun build(
        query: PeriodLoadQuery,
        metric: ActivityMetric,
        dailyGoal: Double,
        dailySteps: List<DailySteps>,
        previousDailySteps: List<DailySteps>,
        baselineDailySteps: List<DailySteps>,
        nutrition: List<DailyNutrition>,
        previousNutrition: List<DailyNutrition>,
        baselineNutrition: List<DailyNutrition>,
        activityProgress: List<ActivityProgressPoint>,
    ): ActivityDisplayState {
        val selectedPeriod = displayPeriodFor(
            range = query.range,
            anchorDate = query.selectedDate,
            weekPeriodMode = query.weekPeriodMode,
        )
        val metricDisplay = when (metric) {
            ActivityMetric.STEPS -> stepsDisplay(
                query = query,
                dailyGoal = dailyGoal,
                period = selectedPeriod,
                dailySteps = dailySteps,
                previousDailySteps = previousDailySteps,
                baselineDailySteps = baselineDailySteps,
                activityProgress = activityProgress,
            )
            ActivityMetric.DISTANCE -> distanceDisplay(
                query = query,
                dailyGoal = dailyGoal,
                period = selectedPeriod,
                dailySteps = dailySteps,
                previousDailySteps = previousDailySteps,
                baselineDailySteps = baselineDailySteps,
                activityProgress = activityProgress,
            )
            ActivityMetric.CALORIES_BURNED -> caloriesBurnedDisplay(
                query = query,
                dailyGoal = dailyGoal,
                period = selectedPeriod,
                nutrition = nutrition,
                previousNutrition = previousNutrition,
                baselineNutrition = baselineNutrition,
                activityProgress = activityProgress,
            )
            ActivityMetric.ACTIVE_CALORIES -> activeCaloriesDisplay(
                query = query,
                dailyGoal = dailyGoal,
                period = selectedPeriod,
                dailySteps = dailySteps,
                previousDailySteps = previousDailySteps,
                baselineDailySteps = baselineDailySteps,
                activityProgress = activityProgress,
            )
            ActivityMetric.FLOORS -> floorsDisplay(
                query = query,
                dailyGoal = dailyGoal,
                period = selectedPeriod,
                dailySteps = dailySteps,
                previousDailySteps = previousDailySteps,
                baselineDailySteps = baselineDailySteps,
                activityProgress = activityProgress,
            )
            ActivityMetric.ELEVATION -> elevationDisplay(
                query = query,
                dailyGoal = dailyGoal,
                period = selectedPeriod,
                dailySteps = dailySteps,
                previousDailySteps = previousDailySteps,
                baselineDailySteps = baselineDailySteps,
                activityProgress = activityProgress,
            )
            ActivityMetric.WHEELCHAIR_PUSHES -> wheelchairPushesDisplay(
                query = query,
                dailyGoal = dailyGoal,
                period = selectedPeriod,
                dailySteps = dailySteps,
                previousDailySteps = previousDailySteps,
                baselineDailySteps = baselineDailySteps,
                activityProgress = activityProgress,
            )
        }
        return ActivityDisplayState(
            selectedPeriod = selectedPeriod,
            metric = metricDisplay,
        )
    }
}

private fun stepsDisplay(
    query: PeriodLoadQuery,
    dailyGoal: Double,
    period: tech.mmarca.openvitals.core.period.DatePeriod,
    dailySteps: List<DailySteps>,
    previousDailySteps: List<DailySteps>,
    baselineDailySteps: List<DailySteps>,
    activityProgress: List<ActivityProgressPoint>,
): ActivityMetricDisplay {
    val values = dailySteps.map { it.steps.toDouble() }
    val goalValues = dailySteps.map { DailyGoalValue(it.date, it.steps.toDouble()) }
    val trackedDates = dailySteps.filter { it.steps > 0L }.map { it.date }
    val activeDays = values.count { it > 0.0 }
    val previousTotal = previousDailySteps.sumOf { it.steps }.toDouble()
    val baselineValues = baselineDailySteps.map { BaselineValue(it.date, it.steps.toDouble()) }
    val sampleCount = if (query.range == TimeRange.DAY) {
        activityProgress.count { it.totalSteps > 0L }
    } else {
        values.count { it > 0.0 }
    }
    val intradayPoints = activityProgress.map { ActivityIntradayPoint(it.time, it.totalSteps.toDouble()) }
    return metricDisplay(
        hasData = query.range == TimeRange.DAY || dailySteps.isNotEmpty(),
        values = values,
        goalValues = goalValues,
        trackedDates = trackedDates,
        sampleCount = sampleCount,
        previousTotal = previousTotal,
        baselineValues = baselineValues,
        activeDays = activeDays,
        dailyGoal = dailyGoal,
        period = period,
        metric = ActivityMetric.STEPS,
        intradayPoints = intradayPoints,
        dayTotal = dailySteps.firstOrNull()?.steps?.toDouble() ?: 0.0,
    )
}

private fun distanceDisplay(
    query: PeriodLoadQuery,
    dailyGoal: Double,
    period: tech.mmarca.openvitals.core.period.DatePeriod,
    dailySteps: List<DailySteps>,
    previousDailySteps: List<DailySteps>,
    baselineDailySteps: List<DailySteps>,
    activityProgress: List<ActivityProgressPoint>,
): ActivityMetricDisplay {
    val values = dailySteps.map { it.distanceMeters }
    val goalValues = dailySteps.map { DailyGoalValue(it.date, it.distanceMeters) }
    val trackedDates = dailySteps.filter { it.distanceMeters > 0.0 }.map { it.date }
    val activeDays = values.count { it > 0.0 }
    val previousTotal = previousDailySteps.sumOf { it.distanceMeters }
    val baselineValues = baselineDailySteps.map { BaselineValue(it.date, it.distanceMeters) }
    val sampleCount = if (query.range == TimeRange.DAY) {
        activityProgress.count { it.totalDistanceMeters != null }
    } else {
        values.count { it > 0.0 }
    }
    val intradayPoints = activityProgress.mapNotNull { point ->
        point.totalDistanceMeters?.let { ActivityIntradayPoint(point.time, it) }
    }
    return metricDisplay(
        hasData = query.range == TimeRange.DAY || dailySteps.any { it.distanceMeters > 0.0 },
        values = values,
        goalValues = goalValues,
        trackedDates = trackedDates,
        sampleCount = sampleCount,
        previousTotal = previousTotal,
        baselineValues = baselineValues,
        activeDays = activeDays,
        dailyGoal = dailyGoal,
        period = period,
        metric = ActivityMetric.DISTANCE,
        intradayPoints = intradayPoints,
        dayTotal = dailySteps.firstOrNull()?.distanceMeters ?: 0.0,
    )
}

private fun caloriesBurnedDisplay(
    query: PeriodLoadQuery,
    dailyGoal: Double,
    period: tech.mmarca.openvitals.core.period.DatePeriod,
    nutrition: List<DailyNutrition>,
    previousNutrition: List<DailyNutrition>,
    baselineNutrition: List<DailyNutrition>,
    activityProgress: List<ActivityProgressPoint>,
): ActivityMetricDisplay {
    val values = nutrition.map { it.caloriesBurnedKcal }
    val goalValues = nutrition.map { DailyGoalValue(it.date, it.caloriesBurnedKcal) }
    val trackedDates = nutrition.filter { it.hasCaloriesBurnedData }.map { it.date }
    val activeDays = values.count { it > 0.0 }
    val previousTotal = previousNutrition.sumOf { it.caloriesBurnedKcal }
    val baselineValues = baselineNutrition.map { BaselineValue(it.date, it.caloriesBurnedKcal) }
    val sampleCount = if (query.range == TimeRange.DAY) {
        activityProgress.count { it.totalCaloriesBurnedKcal != null }
    } else {
        nutrition.count { it.hasCaloriesBurnedData }
    }
    val intradayPoints = activityProgress.mapNotNull { point ->
        point.totalCaloriesBurnedKcal?.let { ActivityIntradayPoint(point.time, it) }
    }
    return metricDisplay(
        hasData = query.range == TimeRange.DAY || nutrition.any { it.hasCaloriesBurnedData },
        values = values,
        goalValues = goalValues,
        trackedDates = trackedDates,
        sampleCount = sampleCount,
        previousTotal = previousTotal,
        baselineValues = baselineValues,
        activeDays = activeDays,
        dailyGoal = dailyGoal,
        period = period,
        metric = ActivityMetric.CALORIES_BURNED,
        intradayPoints = intradayPoints,
        dayTotal = nutrition.firstOrNull()?.caloriesBurnedKcal ?: 0.0,
    )
}

private fun activeCaloriesDisplay(
    query: PeriodLoadQuery,
    dailyGoal: Double,
    period: tech.mmarca.openvitals.core.period.DatePeriod,
    dailySteps: List<DailySteps>,
    previousDailySteps: List<DailySteps>,
    baselineDailySteps: List<DailySteps>,
    activityProgress: List<ActivityProgressPoint>,
): ActivityMetricDisplay {
    val values = dailySteps.map { it.activeCaloriesKcal ?: 0.0 }
    val goalValues = dailySteps.map { DailyGoalValue(it.date, it.activeCaloriesKcal ?: 0.0) }
    val trackedDates = dailySteps.filter { (it.activeCaloriesKcal ?: 0.0) > 0.0 }.map { it.date }
    val activeDays = values.count { it > 0.0 }
    val previousTotal = previousDailySteps.sumOf { it.activeCaloriesKcal ?: 0.0 }
    val baselineValues = baselineDailySteps.map { BaselineValue(it.date, it.activeCaloriesKcal ?: 0.0) }
    val sampleCount = if (query.range == TimeRange.DAY) {
        activityProgress.count { it.totalActiveCaloriesKcal != null }
    } else {
        values.count { it > 0.0 }
    }
    val intradayPoints = activityProgress.mapNotNull { point ->
        point.totalActiveCaloriesKcal?.let { ActivityIntradayPoint(point.time, it) }
    }
    return metricDisplay(
        hasData = query.range == TimeRange.DAY || dailySteps.any { it.activeCaloriesKcal != null },
        values = values,
        goalValues = goalValues,
        trackedDates = trackedDates,
        sampleCount = sampleCount,
        previousTotal = previousTotal,
        baselineValues = baselineValues,
        activeDays = activeDays,
        dailyGoal = dailyGoal,
        period = period,
        metric = ActivityMetric.ACTIVE_CALORIES,
        intradayPoints = intradayPoints,
        dayTotal = dailySteps.firstOrNull()?.activeCaloriesKcal ?: 0.0,
    )
}

private fun floorsDisplay(
    query: PeriodLoadQuery,
    dailyGoal: Double,
    period: tech.mmarca.openvitals.core.period.DatePeriod,
    dailySteps: List<DailySteps>,
    previousDailySteps: List<DailySteps>,
    baselineDailySteps: List<DailySteps>,
    activityProgress: List<ActivityProgressPoint>,
): ActivityMetricDisplay {
    val values = dailySteps.map { (it.floorsClimbed ?: 0).toDouble() }
    val goalValues = dailySteps.map { DailyGoalValue(it.date, (it.floorsClimbed ?: 0).toDouble()) }
    val trackedDates = dailySteps.filter { (it.floorsClimbed ?: 0) > 0 }.map { it.date }
    val activeDays = values.count { it > 0.0 }
    val previousTotal = previousDailySteps.sumOf { (it.floorsClimbed ?: 0).toDouble() }
    val baselineValues = baselineDailySteps.map { BaselineValue(it.date, (it.floorsClimbed ?: 0).toDouble()) }
    val sampleCount = if (query.range == TimeRange.DAY) {
        activityProgress.count { it.totalFloorsClimbed != null }
    } else {
        values.count { it > 0.0 }
    }
    val intradayPoints = activityProgress.mapNotNull { point ->
        point.totalFloorsClimbed?.let { ActivityIntradayPoint(point.time, it.toDouble()) }
    }
    return metricDisplay(
        hasData = query.range == TimeRange.DAY || dailySteps.any { it.floorsClimbed != null },
        values = values,
        goalValues = goalValues,
        trackedDates = trackedDates,
        sampleCount = sampleCount,
        previousTotal = previousTotal,
        baselineValues = baselineValues,
        activeDays = activeDays,
        dailyGoal = dailyGoal,
        period = period,
        metric = ActivityMetric.FLOORS,
        intradayPoints = intradayPoints,
        dayTotal = (dailySteps.firstOrNull()?.floorsClimbed ?: 0).toDouble(),
    )
}

private fun elevationDisplay(
    query: PeriodLoadQuery,
    dailyGoal: Double,
    period: tech.mmarca.openvitals.core.period.DatePeriod,
    dailySteps: List<DailySteps>,
    previousDailySteps: List<DailySteps>,
    baselineDailySteps: List<DailySteps>,
    activityProgress: List<ActivityProgressPoint>,
): ActivityMetricDisplay {
    val values = dailySteps.map { it.elevationGainedMeters ?: 0.0 }
    val goalValues = dailySteps.map { DailyGoalValue(it.date, it.elevationGainedMeters ?: 0.0) }
    val trackedDates = dailySteps.filter { (it.elevationGainedMeters ?: 0.0) > 0.0 }.map { it.date }
    val activeDays = values.count { it > 0.0 }
    val previousTotal = previousDailySteps.sumOf { it.elevationGainedMeters ?: 0.0 }
    val baselineValues = baselineDailySteps.map { BaselineValue(it.date, it.elevationGainedMeters ?: 0.0) }
    val sampleCount = if (query.range == TimeRange.DAY) {
        activityProgress.count { it.totalElevationGainedMeters != null }
    } else {
        values.count { it > 0.0 }
    }
    val intradayPoints = activityProgress.mapNotNull { point ->
        point.totalElevationGainedMeters?.let { ActivityIntradayPoint(point.time, it) }
    }
    return metricDisplay(
        hasData = query.range == TimeRange.DAY || dailySteps.any { it.elevationGainedMeters != null },
        values = values,
        goalValues = goalValues,
        trackedDates = trackedDates,
        sampleCount = sampleCount,
        previousTotal = previousTotal,
        baselineValues = baselineValues,
        activeDays = activeDays,
        dailyGoal = dailyGoal,
        period = period,
        metric = ActivityMetric.ELEVATION,
        intradayPoints = intradayPoints,
        dayTotal = dailySteps.firstOrNull()?.elevationGainedMeters ?: 0.0,
    )
}

private fun wheelchairPushesDisplay(
    query: PeriodLoadQuery,
    dailyGoal: Double,
    period: tech.mmarca.openvitals.core.period.DatePeriod,
    dailySteps: List<DailySteps>,
    previousDailySteps: List<DailySteps>,
    baselineDailySteps: List<DailySteps>,
    activityProgress: List<ActivityProgressPoint>,
): ActivityMetricDisplay {
    val values = dailySteps.map { (it.wheelchairPushes ?: 0L).toDouble() }
    val goalValues = dailySteps.map { DailyGoalValue(it.date, (it.wheelchairPushes ?: 0L).toDouble()) }
    val trackedDates = dailySteps.filter { (it.wheelchairPushes ?: 0L) > 0L }.map { it.date }
    val activeDays = values.count { it > 0.0 }
    val previousTotal = previousDailySteps.sumOf { (it.wheelchairPushes ?: 0L).toDouble() }
    val baselineValues = baselineDailySteps.map { BaselineValue(it.date, (it.wheelchairPushes ?: 0L).toDouble()) }
    val sampleCount = if (query.range == TimeRange.DAY) {
        activityProgress.count { (it.totalWheelchairPushes ?: 0L) > 0L }
    } else {
        values.count { it > 0.0 }
    }
    val intradayPoints = activityProgress.mapNotNull { point ->
        point.totalWheelchairPushes?.let { ActivityIntradayPoint(point.time, it.toDouble()) }
    }
    return metricDisplay(
        hasData = query.range == TimeRange.DAY || dailySteps.any { it.wheelchairPushes != null },
        values = values,
        goalValues = goalValues,
        trackedDates = trackedDates,
        sampleCount = sampleCount,
        previousTotal = previousTotal,
        baselineValues = baselineValues,
        activeDays = activeDays,
        dailyGoal = dailyGoal,
        period = period,
        metric = ActivityMetric.WHEELCHAIR_PUSHES,
        intradayPoints = intradayPoints,
        dayTotal = (dailySteps.firstOrNull()?.wheelchairPushes ?: 0L).toDouble(),
    )
}

private fun metricDisplay(
    hasData: Boolean,
    values: List<Double>,
    goalValues: List<DailyGoalValue>,
    trackedDates: List<LocalDate>,
    sampleCount: Int,
    previousTotal: Double,
    baselineValues: List<BaselineValue>,
    activeDays: Int,
    dailyGoal: Double,
    period: tech.mmarca.openvitals.core.period.DatePeriod,
    metric: ActivityMetric,
    intradayPoints: List<ActivityIntradayPoint>,
    dayTotal: Double,
): ActivityMetricDisplay {
    val valueSum = values.sum()
    val goalProgress = dailyGoalProgress(
        values = goalValues,
        period = period,
        target = dailyGoal,
        direction = metric.dailyGoalKey.direction,
    )
    return ActivityMetricDisplay(
        hasData = hasData,
        values = values,
        goalValues = goalValues,
        trackedDates = trackedDates,
        sampleCount = sampleCount,
        previousTotal = previousTotal,
        baselineValues = baselineValues,
        activeDays = activeDays,
        goalProgress = goalProgress,
        periodComparison = periodComparison(
            currentValue = valueSum,
            previousValue = previousTotal,
        ),
        baselineCurrentValue = averageOrZero(valueSum, activeDays),
        intradayPoints = intradayPoints,
        dayTotal = dayTotal,
    )
}
