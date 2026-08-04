package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate

data class BloodPressureEntry(
    val time: Instant,
    val systolicMmHg: Int,
    val diastolicMmHg: Int,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class SpO2Entry(
    val time: Instant,
    val percent: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class RespiratoryRateEntry(
    val time: Instant,
    val breathsPerMinute: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class BodyTempEntry(
    val time: Instant,
    val temperatureCelsius: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class BloodGlucoseEntry(
    val time: Instant,
    val millimolesPerLiter: Double,
    val specimenSource: Int,
    val mealType: Int,
    val relationToMeal: Int,
    val source: String,
)

data class SkinTemperatureEntry(
    val startTime: Instant,
    val endTime: Instant,
    val baselineCelsius: Double?,
    val averageDeltaCelsius: Double?,
    val minDeltaCelsius: Double?,
    val maxDeltaCelsius: Double?,
    val measurementLocation: Int,
    val source: String,
) {
    val time: Instant get() = endTime
}

data class Vo2MaxEntry(
    val time: Instant,
    val vo2MaxMlPerKgPerMin: Double,
    val source: String,
)

enum class VitalsMeasurementType {
    BLOOD_PRESSURE,
    SPO2,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
}

data class VitalsMeasurementWriteRequest(
    val type: VitalsMeasurementType,
    val time: Instant,
    val value: Double,
    val secondaryValue: Double? = null,
)

data class VitalsMeasurementEntry(
    val id: String,
    val type: VitalsMeasurementType,
    val time: Instant,
    val value: Double,
    val secondaryValue: Double? = null,
    val source: String,
    val isOpenVitalsEntry: Boolean,
)

/**
 * One vitals chart point per local day, aggregated natively so long ranges do
 * not hold a season of raw records in memory. [count] is the number of readings
 * the day's [value] averages, so period averages can stay count-weighted.
 */
data class DailyVitalPoint(
    val date: LocalDate,
    val value: Double,
    val count: Int,
)

/** Blood pressure carries two values per day, so it gets its own point type. */
data class DailyBloodPressurePoint(
    val date: LocalDate,
    val systolic: Double,
    val diastolic: Double,
    val count: Int,
)
