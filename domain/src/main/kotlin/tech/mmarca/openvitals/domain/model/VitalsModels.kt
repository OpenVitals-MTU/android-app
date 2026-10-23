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
    /** The pulse saved with the reading, from OpenVitals-written records only. */
    val pulseBpm: Int? = null,
)

/** Health Connect's BloodPressureRecord constants, mirrored so the domain stays library-free. */
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
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

/** The app's mg/dL factor. What the screens show in mg/dL is what the entry form reads back. */
const val GlucoseMgPerDlPerMmolPerL = 18.0

/** One timed variation inside a skin temperature record. */
data class SkinTemperatureDeltaSample(
    val time: Instant,
    val deltaCelsius: Double,
)

/**
 * A Health Connect skin temperature record: a signed variation from an
 * optional absolute [baselineCelsius] the device set. A record can span hours
 * with a sample a minute; [deltas] keeps them where a day view needs the
 * shape, and is empty where only the summary fields travel.
 */
data class SkinTemperatureEntry(
    val startTime: Instant,
    val endTime: Instant,
    val baselineCelsius: Double?,
    val averageDeltaCelsius: Double?,
    val minDeltaCelsius: Double?,
    val maxDeltaCelsius: Double?,
    val measurementLocation: Int,
    val source: String,
    val deltas: List<SkinTemperatureDeltaSample> = emptyList(),
) {
    val time: Instant get() = endTime
}

data class Vo2MaxEntry(
    val time: Instant,
    val vo2MaxMlPerKgPerMin: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

/** Health Connect's Vo2MaxRecord MEASUREMENT_METHOD_* constants, mirrored so the domain stays library-free. */
object Vo2MaxRecordValues {
    const val MEASUREMENT_METHOD_OTHER = 0
    const val MEASUREMENT_METHOD_METABOLIC_CART = 1
    const val MEASUREMENT_METHOD_HEART_RATE_RATIO = 2
    const val MEASUREMENT_METHOD_COOPER_TEST = 3
    const val MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST = 4
    const val MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST = 5
}

enum class VitalsMeasurementType {
    BLOOD_PRESSURE,
    SPO2,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
    HRV,
    BLOOD_GLUCOSE,
    VO2_MAX,
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
    /**
     * Blood pressure only: the pulse the cuff showed, in beats per minute. Written as a
     * heart rate record at the reading's time under [bpPulseClientRecordId]; null writes
     * none and, on an edit, removes the one a previous save left.
     */
    val bpPulseBpm: Int? = null,
    /** Blood glucose only: the reading's [GlucoseRecordValues] fields; null writes UNKNOWN. */
    val glucose: GlucoseContext = GlucoseContext(),
    /** VO2 max only: [Vo2MaxRecordValues] MEASUREMENT_METHOD_*; null writes OTHER. */
    val vo2MaxMeasurementMethod: Int? = null,
)

/** What a glucose reading says about itself besides the level. Null is Health Connect's UNKNOWN. */
data class GlucoseContext(
    /** RELATION_TO_MEAL_*. */
    val relationToMeal: Int? = null,
    /** MEAL_TYPE_*, the meal [relationToMeal] is before or after. */
    val mealType: Int? = null,
    /** SPECIMEN_SOURCE_*. */
    val specimenSource: Int? = null,
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
    /** Blood pressure only: the pulse saved with the reading, null when none was. */
    val bpPulseBpm: Int? = null,
    /** Blood glucose only. */
    val glucose: GlucoseContext = GlucoseContext(),
    /** VO2 max only: MEASUREMENT_METHOD_* value, null when other. */
    val vo2MaxMeasurementMethod: Int? = null,
)

/** One chart point per local day. [count] keeps period averages count-weighted. */
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
