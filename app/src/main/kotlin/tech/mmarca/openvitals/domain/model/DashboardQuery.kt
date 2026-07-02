package tech.mmarca.openvitals.domain.model

import java.time.LocalDate
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode

data class DashboardQuery(
    val date: LocalDate = LocalDate.now(),
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    val visibleMetrics: Set<DashboardMetric> = DashboardMetric.entries.toSet(),
    val refreshMode: RefreshMode = RefreshMode.NORMAL,
    val includeHistoricalBaselines: Boolean = true,
    val includeWeeklyTrainingSignals: Boolean = true,
)

enum class DashboardMetric {
    STEPS,
    DISTANCE,
    CALORIES_OUT,
    ACTIVE_CALORIES,
    FLOORS,
    ELEVATION,
    WHEELCHAIR_PUSHES,
    WORKOUT,
    SLEEP,
    HYDRATION,
    CALORIES_IN,
    PROTEIN,
    CARBS,
    FAT,
    CAFFEINE,
    WEIGHT,
    HEIGHT,
    BMI,
    FFMI,
    BODY_FAT,
    LEAN_MASS,
    BMR,
    BONE_MASS,
    BODY_WATER_MASS,
    AVG_HEART_RATE,
    RESTING_HEART_RATE,
    HRV,
    BLOOD_PRESSURE,
    SPO2,
    VO2_MAX,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
    BLOOD_GLUCOSE,
    SKIN_TEMPERATURE,
    WEEKLY_CARDIO_LOAD,
    INTENSITY_MINUTES,
    MINDFULNESS,
    CYCLE,
}
