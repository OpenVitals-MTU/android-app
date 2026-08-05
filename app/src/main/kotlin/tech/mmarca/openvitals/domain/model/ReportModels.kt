package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.preferences.UnitQuantity

/** The section a metric renders under in the health report. */
enum class ReportSection {
    ACTIVITY,
    SLEEP,
    NUTRITION,
    BODY,
    HEART,
    VITALS,
    MINDFULNESS,
}

/**
 * How daily values combine into a coarser bucket — and what the summary's
 * headline number means. SUM metrics (steps, calories) add up; AVERAGE metrics
 * (weight, heart rate) mean out, and a "total" would be nonsense.
 */
enum class ReportValueKind {
    SUM,
    AVERAGE,
}

/**
 * The metrics a health report can carry — [DashboardMetric] minus the five
 * that have no exportable daily series: BMI and FFMI are display-time
 * derivations with no stored history, WEEKLY_CARDIO_LOAD and INTENSITY_MINUTES
 * are estimates without a range read, and CYCLE is seven heterogeneous event
 * lists rather than a numeric series.
 *
 * Values stay in storage units (metric); formatting happens only at render,
 * via [unitQuantity] where a display override applies.
 */
enum class ReportMetric(
    val dashboardMetric: DashboardMetric,
    val section: ReportSection,
    val valueKind: ReportValueKind,
    val unitQuantity: UnitQuantity? = null,
) {
    STEPS(DashboardMetric.STEPS, ReportSection.ACTIVITY, ReportValueKind.SUM),
    DISTANCE(DashboardMetric.DISTANCE, ReportSection.ACTIVITY, ReportValueKind.SUM, UnitQuantity.DISTANCE),
    CALORIES_OUT(DashboardMetric.CALORIES_OUT, ReportSection.ACTIVITY, ReportValueKind.SUM),
    ACTIVE_CALORIES(DashboardMetric.ACTIVE_CALORIES, ReportSection.ACTIVITY, ReportValueKind.SUM),
    FLOORS(DashboardMetric.FLOORS, ReportSection.ACTIVITY, ReportValueKind.SUM),
    ELEVATION(DashboardMetric.ELEVATION, ReportSection.ACTIVITY, ReportValueKind.SUM, UnitQuantity.ELEVATION),
    WHEELCHAIR_PUSHES(DashboardMetric.WHEELCHAIR_PUSHES, ReportSection.ACTIVITY, ReportValueKind.SUM),
    WORKOUT(DashboardMetric.WORKOUT, ReportSection.ACTIVITY, ReportValueKind.SUM),
    SLEEP(DashboardMetric.SLEEP, ReportSection.SLEEP, ReportValueKind.AVERAGE),
    HYDRATION(DashboardMetric.HYDRATION, ReportSection.NUTRITION, ReportValueKind.SUM, UnitQuantity.HYDRATION),
    CALORIES_IN(DashboardMetric.CALORIES_IN, ReportSection.NUTRITION, ReportValueKind.SUM),
    PROTEIN(DashboardMetric.PROTEIN, ReportSection.NUTRITION, ReportValueKind.SUM),
    CARBS(DashboardMetric.CARBS, ReportSection.NUTRITION, ReportValueKind.SUM),
    FAT(DashboardMetric.FAT, ReportSection.NUTRITION, ReportValueKind.SUM),
    CAFFEINE(DashboardMetric.CAFFEINE, ReportSection.NUTRITION, ReportValueKind.SUM),
    WEIGHT(DashboardMetric.WEIGHT, ReportSection.BODY, ReportValueKind.AVERAGE, UnitQuantity.WEIGHT),
    HEIGHT(DashboardMetric.HEIGHT, ReportSection.BODY, ReportValueKind.AVERAGE, UnitQuantity.HEIGHT),
    BODY_FAT(DashboardMetric.BODY_FAT, ReportSection.BODY, ReportValueKind.AVERAGE),
    LEAN_MASS(DashboardMetric.LEAN_MASS, ReportSection.BODY, ReportValueKind.AVERAGE, UnitQuantity.WEIGHT),
    BMR(DashboardMetric.BMR, ReportSection.BODY, ReportValueKind.AVERAGE),
    BONE_MASS(DashboardMetric.BONE_MASS, ReportSection.BODY, ReportValueKind.AVERAGE, UnitQuantity.WEIGHT),
    BODY_WATER_MASS(DashboardMetric.BODY_WATER_MASS, ReportSection.BODY, ReportValueKind.AVERAGE, UnitQuantity.WEIGHT),
    AVG_HEART_RATE(DashboardMetric.AVG_HEART_RATE, ReportSection.HEART, ReportValueKind.AVERAGE),
    RESTING_HEART_RATE(DashboardMetric.RESTING_HEART_RATE, ReportSection.HEART, ReportValueKind.AVERAGE),
    HRV(DashboardMetric.HRV, ReportSection.HEART, ReportValueKind.AVERAGE),
    BLOOD_PRESSURE(DashboardMetric.BLOOD_PRESSURE, ReportSection.VITALS, ReportValueKind.AVERAGE),
    SPO2(DashboardMetric.SPO2, ReportSection.VITALS, ReportValueKind.AVERAGE),
    VO2_MAX(DashboardMetric.VO2_MAX, ReportSection.VITALS, ReportValueKind.AVERAGE),
    RESPIRATORY_RATE(DashboardMetric.RESPIRATORY_RATE, ReportSection.VITALS, ReportValueKind.AVERAGE),
    BODY_TEMPERATURE(DashboardMetric.BODY_TEMPERATURE, ReportSection.VITALS, ReportValueKind.AVERAGE, UnitQuantity.TEMPERATURE),
    BLOOD_GLUCOSE(DashboardMetric.BLOOD_GLUCOSE, ReportSection.VITALS, ReportValueKind.AVERAGE, UnitQuantity.BLOOD_GLUCOSE),
    SKIN_TEMPERATURE(DashboardMetric.SKIN_TEMPERATURE, ReportSection.VITALS, ReportValueKind.AVERAGE, UnitQuantity.TEMPERATURE),
    MINDFULNESS(DashboardMetric.MINDFULNESS, ReportSection.MINDFULNESS, ReportValueKind.SUM),
}

enum class ReportGranularity {
    DAILY,
    WEEKLY,
    MONTHLY,
}

/** What the user asked for. [start]..[end] are inclusive local dates. */
data class ReportRequest(
    val metrics: Set<ReportMetric>,
    val granularity: ReportGranularity,
    val start: LocalDate,
    val end: LocalDate,
    val sleepWindow: SleepWindow = SleepWindow.Default,
    val weekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
)

/**
 * One daily observation feeding the rollup, in storage units. [min]/[max]
 * carry a real intra-day spread where the source has one (heart rate);
 * elsewhere they stay null and the daily value stands alone. [secondaryValue]
 * carries diastolic for blood pressure and stays null everywhere else.
 */
data class ReportDailyValue(
    val date: LocalDate,
    val value: Double,
    val min: Double? = null,
    val max: Double? = null,
    val secondaryValue: Double? = null,
)

/**
 * One chart/table row: a calendar bucket the range actually has data in.
 * [bucketStart]/[bucketEnd] are clamped to the report range, so a monthly
 * bucket at the edge shows the dates it truly covers. Gap buckets are omitted
 * entirely, never zero-filled.
 */
data class ReportPoint(
    val bucketStart: LocalDate,
    val bucketEnd: LocalDate,
    val value: Double,
    val min: Double,
    val max: Double,
    val daysWithData: Int,
    val secondaryValue: Double? = null,
    val secondaryMin: Double? = null,
    val secondaryMax: Double? = null,
)

/**
 * The stats row under a chart. Always computed from DAILY values — bucketing
 * must not change what "min" or "average" means. [total] is null for AVERAGE
 * metrics, where a sum would be meaningless.
 */
data class ReportMetricSummary(
    val average: Double,
    val min: Double,
    val max: Double,
    val total: Double?,
    val daysWithData: Int,
    val secondaryAverage: Double? = null,
    val secondaryMin: Double? = null,
    val secondaryMax: Double? = null,
    /** Last daily value minus the first; null with fewer than two days. */
    val changeOverRange: Double? = null,
)

enum class ReportMetricStatus {
    /** Data present; points and summary are populated. */
    OK,

    /** Readable, but the range holds no data. */
    EMPTY,

    /** The metric's read permission is not granted; nothing was read. */
    MISSING_PERMISSION,

    /** The read failed or blew its budget; the report says so and moves on. */
    FAILED,

    /** The user cancelled before this metric's read started. */
    SKIPPED,
}

data class ReportMetricResult(
    val metric: ReportMetric,
    val status: ReportMetricStatus,
    val points: List<ReportPoint> = emptyList(),
    val summary: ReportMetricSummary? = null,
    val detail: ReportMetricDetail? = null,
)

/**
 * A metric's section body beyond the generic chart+stats+bucket-table. The
 * writer branches on the concrete type; null keeps the generic rendering.
 */
sealed interface ReportMetricDetail

/**
 * One raw blood-pressure measurement. [context] is the reading's meal
 * context: the user's explicit choice on OpenVitals-written records, a
 * time-of-day estimate ([contextEstimated] = true) on everything else.
 */
data class ReportBpReading(
    val time: Instant,
    val systolicMmHg: Int,
    val diastolicMmHg: Int,
    val context: BpMealContext,
    val contextEstimated: Boolean,
    /** [BpRecordValues] BODY_POSITION_*; UNKNOWN when the writer didn't say. */
    val bodyPosition: Int = BpRecordValues.BODY_POSITION_UNKNOWN,
    /** [BpRecordValues] MEASUREMENT_LOCATION_*; UNKNOWN when the writer didn't say. */
    val measurementLocation: Int = BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN,
)

/** Averages for one meal context; `context == null` is the all-readings total row. */
data class ReportBpSlotAverage(
    val context: BpMealContext?,
    val systolic: Double,
    val diastolic: Double,
    val readings: Int,
)

/**
 * Everything the report's blood-pressure section shows beyond the generic
 * chart: the full reading list, the time-of-day averages, and per-component
 * summaries — systolic and diastolic never share a stat again.
 */
data class ReportBloodPressureDetail(
    val readings: List<ReportBpReading>,
    val slotAverages: List<ReportBpSlotAverage>,
    val systolic: ReportMetricSummary,
    val diastolic: ReportMetricSummary,
) : ReportMetricDetail

/**
 * Health Connect's BloodGlucoseRecord.RELATION_TO_MEAL_* constants, mirrored
 * so the domain stays library-free — pinned against androidx by a test.
 */
object GlucoseRecordValues {
    const val RELATION_TO_MEAL_UNKNOWN = 0
    const val RELATION_TO_MEAL_GENERAL = 1
    const val RELATION_TO_MEAL_FASTING = 2
    const val RELATION_TO_MEAL_BEFORE_MEAL = 3
    const val RELATION_TO_MEAL_AFTER_MEAL = 4
}

/** One raw glucose measurement with its meal context. */
data class ReportGlucoseReading(
    val time: Instant,
    val millimolesPerLiter: Double,
    val relationToMeal: Int,
)

/** Averages for one meal context; `relationToMeal == null` is the total row. */
data class ReportGlucoseContextAverage(
    val relationToMeal: Int?,
    val average: Double,
    val min: Double,
    val max: Double,
    val readings: Int,
)

/**
 * The glucose section: every reading with its meal context, averaged by
 * relation to meal — fasting average is the number a doctor asks for first.
 */
data class ReportGlucoseDetail(
    val readings: List<ReportGlucoseReading>,
    val contextAverages: List<ReportGlucoseContextAverage>,
    val summary: ReportMetricSummary,
) : ReportMetricDetail

data class ReportWorkoutSession(
    val start: Instant,
    val exerciseType: Int,
    val title: String?,
    val durationMs: Long,
    val distanceMeters: Double?,
)

data class ReportWorkoutTypeTotal(
    val exerciseType: Int,
    val sessions: Int,
    val totalDurationMs: Long,
    val totalDistanceMeters: Double?,
)

/** The workout section: what was actually done, not just minutes per day. */
data class ReportWorkoutsDetail(
    val sessions: List<ReportWorkoutSession>,
    val byType: List<ReportWorkoutTypeTotal>,
) : ReportMetricDetail

data class ReportSleepNight(
    val date: LocalDate,
    val bedtime: Instant,
    val wake: Instant,
    val asleepMs: Long,
    val deepMs: Long?,
    val remMs: Long?,
)

/** Share of staged sleep time per stage family, in percent (0..100). */
data class ReportSleepStageMix(
    val deepPct: Double,
    val remPct: Double,
    val lightPct: Double,
    val awakePct: Double,
)

/**
 * The sleep section: schedule (circular-mean bedtime/wake), stage mix over
 * nights with reliable stage data, and one row per night. Naps (< 3 h) are
 * excluded throughout — the report is clinical, not a nap log.
 */
data class ReportSleepDetail(
    val nights: List<ReportSleepNight>,
    val averageBedtimeMinutes: Int?,
    val averageWakeMinutes: Int?,
    val stageMix: ReportSleepStageMix?,
    val nightsWithData: Int,
) : ReportMetricDetail

data class ReportReading(
    val time: Instant,
    val value: Double,
)

/** A plain readings list for sparse manual metrics (body temperature). */
data class ReportReadingsDetail(
    val readings: List<ReportReading>,
) : ReportMetricDetail

/**
 * Everything the PDF renderer consumes. [effectiveStart] is the range the
 * loader could actually serve — later than the requested start when the
 * history permission clamps reads, in which case [truncatedToDays] says what
 * the range shrank to and [historyPermissionMissing] is true.
 */
data class ReportData(
    val request: ReportRequest,
    val effectiveStart: LocalDate,
    val truncatedToDays: Int?,
    val missingPermissions: Set<ReportMetric>,
    val historyPermissionMissing: Boolean,
    val cancelled: Boolean,
    val results: List<ReportMetricResult>,
    val generatedAt: Instant,
)
