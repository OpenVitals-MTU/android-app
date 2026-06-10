package tech.mmarca.openvitals.core.period

enum class PeriodRangePreferenceKey(
    val storageKey: String,
    val defaultRange: TimeRange,
) {
    STEPS("detail_range_steps", TimeRange.WEEK),
    CALORIES("detail_range_calories", TimeRange.WEEK),
    ACTIVITIES("detail_range_activities", TimeRange.WEEK),
    SLEEP("detail_range_sleep", TimeRange.WEEK),
    HEART("detail_range_heart", TimeRange.WEEK),
    BODY("detail_range_body", TimeRange.MONTH),
    HYDRATION("detail_range_hydration", TimeRange.WEEK),
    NUTRITION("detail_range_nutrition", TimeRange.WEEK),
    MINDFULNESS("detail_range_mindfulness", TimeRange.WEEK),
    CYCLE("detail_range_cycle", TimeRange.MONTH),
}
