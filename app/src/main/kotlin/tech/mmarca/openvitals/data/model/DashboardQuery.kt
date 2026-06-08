package tech.mmarca.openvitals.data.model

import java.time.LocalDate
import tech.mmarca.openvitals.core.performance.RefreshMode
import tech.mmarca.openvitals.core.preferences.ActivityWeekMode
import tech.mmarca.openvitals.core.preferences.SleepRangeMode

data class DashboardQuery(
    val date: LocalDate = LocalDate.now(),
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    val visibleMetrics: Set<DashboardMetric> = DashboardMetric.entries.toSet(),
    val trackCycle: Boolean = false,
    val refreshMode: RefreshMode = RefreshMode.NORMAL,
)

enum class DashboardMetric {
    STEPS,
    DISTANCE,
    CALORIES_OUT,
    ACTIVE_CALORIES,
    FLOORS,
    ELEVATION,
    WORKOUT,
    SLEEP,
    HYDRATION,
    CALORIES_IN,
    PROTEIN,
    CARBS,
    FAT,
    WEIGHT,
    HEIGHT,
    BMI,
    BODY_FAT,
    LEAN_MASS,
    BMR,
    BONE_MASS,
    AVG_HEART_RATE,
    RESTING_HEART_RATE,
    HRV,
    BLOOD_PRESSURE,
    SPO2,
    VO2_MAX,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
    WEEKLY_CARDIO_LOAD,
    MINDFULNESS,
    CYCLE,
}
