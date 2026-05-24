package tech.mmarca.openvitals.features.nutrition

import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey

internal val NutritionMetric.dailyGoalKey: MetricDailyGoalKey
    get() = when (this) {
        NutritionMetric.CALORIES_IN -> MetricDailyGoalKey.CALORIES_IN_KCAL
        NutritionMetric.PROTEIN -> MetricDailyGoalKey.PROTEIN_GRAMS
        NutritionMetric.CARBS -> MetricDailyGoalKey.CARBS_GRAMS
        NutritionMetric.FAT -> MetricDailyGoalKey.FAT_GRAMS
    }
