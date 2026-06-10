package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey

internal val ActivityMetric.dailyGoalKey: MetricDailyGoalKey
    get() = when (this) {
        ActivityMetric.STEPS -> MetricDailyGoalKey.STEPS
        ActivityMetric.DISTANCE -> MetricDailyGoalKey.DISTANCE_METERS
        ActivityMetric.CALORIES_BURNED -> MetricDailyGoalKey.CALORIES_OUT_KCAL
        ActivityMetric.ACTIVE_CALORIES -> MetricDailyGoalKey.ACTIVE_CALORIES_KCAL
        ActivityMetric.FLOORS -> MetricDailyGoalKey.FLOORS
        ActivityMetric.ELEVATION -> MetricDailyGoalKey.ELEVATION_METERS
        ActivityMetric.WHEELCHAIR_PUSHES -> MetricDailyGoalKey.WHEELCHAIR_PUSHES
    }
