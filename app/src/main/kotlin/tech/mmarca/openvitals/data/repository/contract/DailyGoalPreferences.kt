package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey

/** The daily target a metric screen measures against. Each key normalizes its own value. */
interface DailyGoalPreferences {

    fun dailyGoalFor(key: MetricDailyGoalKey): Double

    fun setDailyGoalFor(key: MetricDailyGoalKey, value: Double)
}
