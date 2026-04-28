package tech.mmarca.openvitals.data.model

import java.time.LocalDate

data class DashboardData(
    val date: LocalDate,
    val steps: Long = 0L,
    val distanceMeters: Double = 0.0,
    val caloriesKcal: Double = 0.0,
    val hydrationLiters: Double = 0.0,
    val workout: ExerciseData? = null,
    val sleep: SleepData? = null,
    val weightKg: Double = 0.0,
    val avgHeartRateBpm: Long = 0,
    val restingHeartRateBpm: Long = 0,
    val bodyFatPercent: Double = 0.0,
    val caloriesInKcal: Double? = null,
    val latestSystolicMmHg: Int? = null,
    val latestDiastolicMmHg: Int? = null,
    val latestSpO2Percent: Double? = null,
    val latestVo2Max: Double? = null,
    val floorsClimbed: Int? = null,
    val elevationGainedMeters: Double? = null,
    val mindfulnessMinutes: Int? = null,
    val missingPermissions: Set<String> = emptySet(),
)
