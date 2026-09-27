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
    HEART_RATE_RECOVERY("detail_range_heart_rate_recovery", TimeRange.MONTH),
    BODY("detail_range_body", TimeRange.MONTH),
    HYDRATION("detail_range_hydration", TimeRange.WEEK),
    NUTRITION("detail_range_nutrition", TimeRange.WEEK),
    MINDFULNESS("detail_range_mindfulness", TimeRange.WEEK),
    CYCLE("detail_range_cycle", TimeRange.MONTH),
    // The day first: active caffeine and tonight's forecast are why the screen is opened.
    CAFFEINE("detail_range_caffeine", TimeRange.DAY),
}
