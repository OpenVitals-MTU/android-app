package tech.mmarca.openvitals.data.model

import java.time.Instant

data class BloodPressureEntry(
    val time: Instant,
    val systolicMmHg: Int,
    val diastolicMmHg: Int,
    val source: String,
)

data class SpO2Entry(
    val time: Instant,
    val percent: Double,
    val source: String,
)

data class RespiratoryRateEntry(
    val time: Instant,
    val breathsPerMinute: Double,
    val source: String,
)

data class BodyTempEntry(
    val time: Instant,
    val temperatureCelsius: Double,
    val source: String,
)

data class Vo2MaxEntry(
    val time: Instant,
    val vo2MaxMlPerKgPerMin: Double,
    val source: String,
)
