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
    /** Explicit meal context, present only on OpenVitals-written records. */
    val mealContext: BpMealContext? = null,
    /** [BpRecordValues] BODY_POSITION_* — native HC field, any app may set it. */
    val bodyPosition: Int = BpRecordValues.BODY_POSITION_UNKNOWN,
    /** [BpRecordValues] MEASUREMENT_LOCATION_* — native HC field. */
    val measurementLocation: Int = BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN,
)

/**
 * Health Connect's BloodPressureRecord position/location constants, mirrored
 * so the domain stays library-free — pinned against androidx by a test.
 */
object BpRecordValues {
    const val BODY_POSITION_UNKNOWN = 0
    const val BODY_POSITION_STANDING_UP = 1
    const val BODY_POSITION_SITTING_DOWN = 2
    const val BODY_POSITION_LYING_DOWN = 3
    const val BODY_POSITION_RECLINING = 4

    const val MEASUREMENT_LOCATION_UNKNOWN = 0
    const val MEASUREMENT_LOCATION_LEFT_WRIST = 1
    const val MEASUREMENT_LOCATION_RIGHT_WRIST = 2
    const val MEASUREMENT_LOCATION_LEFT_UPPER_ARM = 3
    const val MEASUREMENT_LOCATION_RIGHT_UPPER_ARM = 4
}

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
    HRV,
}

data class VitalsMeasurementWriteRequest(
    val type: VitalsMeasurementType,
    val time: Instant,
    val value: Double,
    val secondaryValue: Double? = null,
    /** Blood pressure only: when the reading was taken relative to meals. */
    val bpMealContext: BpMealContext? = null,
    /** Blood pressure only: [BpRecordValues] BODY_POSITION_*; null writes UNKNOWN. */
    val bpBodyPosition: Int? = null,
    /** Blood pressure only: [BpRecordValues] MEASUREMENT_LOCATION_*; null writes UNKNOWN. */
    val bpMeasurementLocation: Int? = null,
)

data class VitalsMeasurementEntry(
    val id: String,
    val type: VitalsMeasurementType,
    val time: Instant,
    val value: Double,
    val secondaryValue: Double? = null,
    val source: String,
    val isOpenVitalsEntry: Boolean,
    /** Blood pressure only: the meal context encoded on OpenVitals records. */
    val bpMealContext: BpMealContext? = null,
    /** Blood pressure only: BODY_POSITION_* value, null when unknown. */
    val bpBodyPosition: Int? = null,
    /** Blood pressure only: MEASUREMENT_LOCATION_* value, null when unknown. */
    val bpMeasurementLocation: Int? = null,
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
