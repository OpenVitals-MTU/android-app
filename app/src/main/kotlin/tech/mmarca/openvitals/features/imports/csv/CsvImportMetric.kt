package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import kotlin.reflect.KClass

/**
 * What a CSV column can be mapped onto, and how its raw text becomes the
 * canonical (metric) value Health Connect stores.
 *
 * v1 covered instant-in-time measurements only — the metrics a smart scale or a
 * vitals export produces. Every one is a single measurement at a single moment,
 * so a mapping needs exactly one timestamp column and one column per metric.
 *
 * [STEPS] is the first interval record ([CsvMetricSpec.isInterval]): its value
 * covers a span of time, so a second timestamp — a [CsvColumnRole.END_TIMESTAMP]
 * column — can say where each row's span ends, with the existing timestamp
 * column as its start. The interval is exactly what the file says, whether
 * that is an hour or a whole day; a row that supplies no end falls back to a
 * one-minute span. Other interval records (sleep, workouts) remain absent:
 * they need more than a number per row.
 *
 * Blood pressure is absent on purpose: systolic and diastolic have to become
 * ONE record, which needs a two-columns-to-one-record rule the mapping model
 * does not have.
 *
 * NOTE: the enum order, the catalog contents and the unit conversions mirror
 * the Flutter build byte for byte — both apps must resolve the same file to the
 * same records so their deterministic clientRecordIds dedup against each other.
 * [STEPS] is appended AFTER that snapshot and defines the contract first: a
 * Flutter build adding it must derive the id from the interval's START instant
 * (`"StepsRecord|<startEpochMillis>"`), exactly like the instant metrics.
 */
enum class CsvImportMetric {
    WEIGHT,
    BODY_FAT,
    LEAN_BODY_MASS,
    BONE_MASS,
    BODY_WATER_MASS,
    HEIGHT,
    BASAL_METABOLIC_RATE,
    HEART_RATE,
    RESTING_HEART_RATE,
    HEART_RATE_VARIABILITY,
    OXYGEN_SATURATION,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
    BASAL_BODY_TEMPERATURE,
    BLOOD_GLUCOSE,
    VO2_MAX,
    STEPS,
}

/**
 * A unit a CSV column's numbers can be written in.
 *
 * This is the unit of the *file*, chosen by the user per column. It is not the
 * app's display unit system: a file can be in pounds while the app displays
 * kilograms, and neither implies the other. Nothing here may consult the unit
 * system preference.
 */
enum class CsvUnit {
    KILOGRAMS,
    POUNDS,
    STONES,
    GRAMS,
    PERCENT,
    FRACTION,
    CENTIMETERS,
    METERS,
    INCHES,
    FEET,
    KILOCALORIES_PER_DAY,
    KILOJOULES_PER_DAY,
    CELSIUS,
    FAHRENHEIT,
    BEATS_PER_MINUTE,
    MILLISECONDS,
    SECONDS,
    BREATHS_PER_MINUTE,
    MILLIMOLES_PER_LITER,
    MILLIGRAMS_PER_DECILITER,
    MILLILITERS_PER_KG_PER_MINUTE,
    COUNT,
}

// The same constants the manual-entry screens use, kept private per file as the
// house style has it.
private const val PoundsPerKilogram = 2.2046226218
private const val CentimetersPerInch = 2.54
private const val KilogramsPerStone = 6.35029318
private const val CentimetersPerFoot = 30.48
private const val KilojoulesPerKilocalorie = 4.184
private const val FahrenheitFreezingPoint = 32.0
private const val FahrenheitPerCelsius = 1.8

/**
 * Health Connect stores blood glucose in mmol/L; the US convention is mg/dL.
 * The factor matches the Flutter build's so both directions agree.
 */
private const val MilligramsPerDeciliterPerMillimolePerLiter = 18.0

/** How a column's raw number becomes the metric's canonical value. */
sealed interface CsvValueInterpretation {
    /** Whether resolving this needs the row's body weight as well as its own cell. */
    val needsRowWeight: Boolean get() = this is CsvMassShareOfWeight
}

/** The number in the cell IS the metric, expressed in [unit]. */
data class CsvDirectValue(val unit: CsvUnit) : CsvValueInterpretation

/**
 * The cell holds a **mass**, and the metric is that mass as a percentage of the
 * row's body weight.
 *
 * Scales export "Fat mass (kg)" while Health Connect's `BodyFatRecord` stores a
 * percentage, so the percentage has to be derived — and it can only be derived
 * from the weight measured at the same moment, i.e. the weight column of the
 * SAME row. A row missing that weight cannot produce this metric, which is why
 * [CsvValueInterpretation.needsRowWeight] exists: the mapping validator asks
 * the interpretation, not the metric, whether a weight column is required.
 */
data class CsvMassShareOfWeight(
    /** Always a mass unit — the catalog only offers mass units for this. */
    val unit: CsvUnit,
) : CsvValueInterpretation

/** Everything the importer needs to know about one metric. */
data class CsvMetricSpec(
    /**
     * The Health Connect record class name this produces. Load-bearing: it is a
     * segment of the deterministic clientRecordId, so it must match the Flutter
     * build's `targetType` strings exactly.
     */
    val targetType: String,

    /** The record class, for the existing-id lookup. */
    val recordType: KClass<out Record>,

    /** The single Health Connect write permission this metric needs. */
    val writePermission: String,

    /** Offered in the UI in this order; the first is the default. */
    val interpretations: List<CsvValueInterpretation>,

    /**
     * Bounds on the CANONICAL value, used to reject a row rather than write a
     * number Health Connect would happily store. A 900 kg weight is a mis-mapped
     * column, not a person.
     */
    val plausibleMin: Double,
    val plausibleMax: Double,

    /**
     * Whether the value covers a span of time rather than an instant. An
     * interval metric reads the mapping's [CsvColumnRole.END_TIMESTAMP] column
     * for each row's end; a row without one spans one minute from its start.
     */
    val isInterval: Boolean = false,
) {
    val defaultInterpretation: CsvValueInterpretation get() = interpretations.first()
}

/** The metric catalog. Adding a metric is an entry here plus a label string. */
val CsvMetricCatalog: Map<CsvImportMetric, CsvMetricSpec> = mapOf(
    CsvImportMetric.WEIGHT to CsvMetricSpec(
        targetType = "WeightRecord",
        recordType = WeightRecord::class,
        writePermission = HealthPermission.getWritePermission(WeightRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.KILOGRAMS),
            CsvDirectValue(CsvUnit.POUNDS),
            CsvDirectValue(CsvUnit.STONES),
            CsvDirectValue(CsvUnit.GRAMS),
        ),
        plausibleMin = 2.0,
        plausibleMax = 650.0,
    ),
    CsvImportMetric.BODY_FAT to CsvMetricSpec(
        targetType = "BodyFatRecord",
        recordType = BodyFatRecord::class,
        writePermission = HealthPermission.getWritePermission(BodyFatRecord::class),
        // The mass interpretations are what make a Withings-style export usable
        // without editing the file.
        interpretations = listOf(
            CsvDirectValue(CsvUnit.PERCENT),
            CsvDirectValue(CsvUnit.FRACTION),
            CsvMassShareOfWeight(CsvUnit.KILOGRAMS),
            CsvMassShareOfWeight(CsvUnit.POUNDS),
            CsvMassShareOfWeight(CsvUnit.GRAMS),
        ),
        // Below ~2% is not survivable and above ~75% is not anatomically
        // possible; both mean the weight column used for the derivation was the
        // wrong one.
        plausibleMin = 2.0,
        plausibleMax = 75.0,
    ),
    CsvImportMetric.LEAN_BODY_MASS to CsvMetricSpec(
        targetType = "LeanBodyMassRecord",
        recordType = LeanBodyMassRecord::class,
        writePermission = HealthPermission.getWritePermission(LeanBodyMassRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.KILOGRAMS),
            CsvDirectValue(CsvUnit.POUNDS),
            CsvDirectValue(CsvUnit.GRAMS),
        ),
        plausibleMin = 1.0,
        plausibleMax = 400.0,
    ),
    CsvImportMetric.BONE_MASS to CsvMetricSpec(
        targetType = "BoneMassRecord",
        recordType = BoneMassRecord::class,
        writePermission = HealthPermission.getWritePermission(BoneMassRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.KILOGRAMS),
            CsvDirectValue(CsvUnit.POUNDS),
            CsvDirectValue(CsvUnit.GRAMS),
        ),
        plausibleMin = 0.1,
        plausibleMax = 20.0,
    ),
    CsvImportMetric.BODY_WATER_MASS to CsvMetricSpec(
        targetType = "BodyWaterMassRecord",
        recordType = BodyWaterMassRecord::class,
        writePermission = HealthPermission.getWritePermission(BodyWaterMassRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.KILOGRAMS),
            CsvDirectValue(CsvUnit.POUNDS),
            CsvDirectValue(CsvUnit.GRAMS),
        ),
        plausibleMin = 0.5,
        plausibleMax = 400.0,
    ),
    CsvImportMetric.HEIGHT to CsvMetricSpec(
        targetType = "HeightRecord",
        recordType = HeightRecord::class,
        writePermission = HealthPermission.getWritePermission(HeightRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.CENTIMETERS),
            CsvDirectValue(CsvUnit.METERS),
            CsvDirectValue(CsvUnit.INCHES),
            CsvDirectValue(CsvUnit.FEET),
        ),
        // Canonical height is METRES.
        plausibleMin = 0.3,
        plausibleMax = 2.8,
    ),
    CsvImportMetric.BASAL_METABOLIC_RATE to CsvMetricSpec(
        targetType = "BasalMetabolicRateRecord",
        recordType = BasalMetabolicRateRecord::class,
        writePermission = HealthPermission.getWritePermission(BasalMetabolicRateRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.KILOCALORIES_PER_DAY),
            CsvDirectValue(CsvUnit.KILOJOULES_PER_DAY),
        ),
        plausibleMin = 200.0,
        plausibleMax = 12000.0,
    ),

    // ── Vitals ───────────────────────────────────────────────────────────────
    // Each is one number at one instant, which is the only shape the mapping
    // model expresses. Ranges are "survivable human", not "clinically normal":
    // rejecting a real fever or a real bradycardia would be worse than storing it.
    CsvImportMetric.HEART_RATE to CsvMetricSpec(
        targetType = "HeartRateRecord",
        recordType = HeartRateRecord::class,
        writePermission = HealthPermission.getWritePermission(HeartRateRecord::class),
        interpretations = listOf(CsvDirectValue(CsvUnit.BEATS_PER_MINUTE)),
        plausibleMin = 1.0,
        plausibleMax = 300.0,
    ),
    CsvImportMetric.RESTING_HEART_RATE to CsvMetricSpec(
        targetType = "RestingHeartRateRecord",
        recordType = RestingHeartRateRecord::class,
        writePermission = HealthPermission.getWritePermission(RestingHeartRateRecord::class),
        interpretations = listOf(CsvDirectValue(CsvUnit.BEATS_PER_MINUTE)),
        plausibleMin = 1.0,
        plausibleMax = 300.0,
    ),
    CsvImportMetric.HEART_RATE_VARIABILITY to CsvMetricSpec(
        targetType = "HeartRateVariabilityRmssdRecord",
        recordType = HeartRateVariabilityRmssdRecord::class,
        writePermission = HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.MILLISECONDS),
            CsvDirectValue(CsvUnit.SECONDS),
        ),
        // Health Connect rejects RMSSD outside 1..200 ms.
        plausibleMin = 1.0,
        plausibleMax = 200.0,
    ),
    CsvImportMetric.OXYGEN_SATURATION to CsvMetricSpec(
        targetType = "OxygenSaturationRecord",
        recordType = OxygenSaturationRecord::class,
        writePermission = HealthPermission.getWritePermission(OxygenSaturationRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.PERCENT),
            CsvDirectValue(CsvUnit.FRACTION),
        ),
        plausibleMin = 50.0,
        plausibleMax = 100.0,
    ),
    CsvImportMetric.RESPIRATORY_RATE to CsvMetricSpec(
        targetType = "RespiratoryRateRecord",
        recordType = RespiratoryRateRecord::class,
        writePermission = HealthPermission.getWritePermission(RespiratoryRateRecord::class),
        interpretations = listOf(CsvDirectValue(CsvUnit.BREATHS_PER_MINUTE)),
        plausibleMin = 1.0,
        plausibleMax = 100.0,
    ),
    CsvImportMetric.BODY_TEMPERATURE to CsvMetricSpec(
        targetType = "BodyTemperatureRecord",
        recordType = BodyTemperatureRecord::class,
        writePermission = HealthPermission.getWritePermission(BodyTemperatureRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.CELSIUS),
            CsvDirectValue(CsvUnit.FAHRENHEIT),
        ),
        plausibleMin = 25.0,
        plausibleMax = 45.0,
    ),
    CsvImportMetric.BASAL_BODY_TEMPERATURE to CsvMetricSpec(
        targetType = "BasalBodyTemperatureRecord",
        recordType = BasalBodyTemperatureRecord::class,
        writePermission = HealthPermission.getWritePermission(BasalBodyTemperatureRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.CELSIUS),
            CsvDirectValue(CsvUnit.FAHRENHEIT),
        ),
        plausibleMin = 25.0,
        plausibleMax = 45.0,
    ),
    CsvImportMetric.BLOOD_GLUCOSE to CsvMetricSpec(
        targetType = "BloodGlucoseRecord",
        recordType = BloodGlucoseRecord::class,
        writePermission = HealthPermission.getWritePermission(BloodGlucoseRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.MILLIMOLES_PER_LITER),
            CsvDirectValue(CsvUnit.MILLIGRAMS_PER_DECILITER),
        ),
        // Canonical is mmol/L: roughly 18..900 mg/dL.
        plausibleMin = 1.0,
        plausibleMax = 50.0,
    ),
    CsvImportMetric.VO2_MAX to CsvMetricSpec(
        targetType = "Vo2MaxRecord",
        recordType = Vo2MaxRecord::class,
        writePermission = HealthPermission.getWritePermission(Vo2MaxRecord::class),
        interpretations = listOf(
            CsvDirectValue(CsvUnit.MILLILITERS_PER_KG_PER_MINUTE),
        ),
        plausibleMin = 5.0,
        plausibleMax = 100.0,
    ),

    // ── Intervals ────────────────────────────────────────────────────────────
    // A steps cell counts what happened between the row's start and end
    // timestamps; without an END_TIMESTAMP column the span defaults to a minute.
    CsvImportMetric.STEPS to CsvMetricSpec(
        targetType = "StepsRecord",
        recordType = StepsRecord::class,
        writePermission = HealthPermission.getWritePermission(StepsRecord::class),
        interpretations = listOf(CsvDirectValue(CsvUnit.COUNT)),
        // Health Connect refuses a count below 1; 200,000 a day is past any
        // recorded ultramarathon, so beyond it the column is not steps.
        plausibleMin = 1.0,
        plausibleMax = 200000.0,
        isInterval = true,
    ),
)

/**
 * Converts [value] from [unit] to the metric's canonical unit — kg for masses,
 * metres for height, kcal/day for BMR, percent for body fat, mmol/L for
 * glucose, °C for temperatures, ms for HRV. Storage is always metric; the unit
 * describes the FILE, never the app's display preference.
 */
fun convertCsvValueToCanonical(value: Double, unit: CsvUnit): Double = when (unit) {
    CsvUnit.KILOGRAMS -> value
    CsvUnit.POUNDS -> value / PoundsPerKilogram
    CsvUnit.STONES -> value * KilogramsPerStone
    CsvUnit.GRAMS -> value / 1000
    CsvUnit.PERCENT -> value
    CsvUnit.FRACTION -> value * 100
    CsvUnit.CENTIMETERS -> value / 100
    CsvUnit.METERS -> value
    CsvUnit.INCHES -> value * CentimetersPerInch / 100
    CsvUnit.FEET -> value * CentimetersPerFoot / 100
    CsvUnit.KILOCALORIES_PER_DAY -> value
    CsvUnit.KILOJOULES_PER_DAY -> value / KilojoulesPerKilocalorie
    CsvUnit.CELSIUS -> value
    CsvUnit.FAHRENHEIT -> (value - FahrenheitFreezingPoint) / FahrenheitPerCelsius
    CsvUnit.BEATS_PER_MINUTE -> value
    CsvUnit.MILLISECONDS -> value
    CsvUnit.SECONDS -> value * 1000
    CsvUnit.BREATHS_PER_MINUTE -> value
    CsvUnit.MILLIMOLES_PER_LITER -> value
    CsvUnit.MILLIGRAMS_PER_DECILITER -> value / MilligramsPerDeciliterPerMillimolePerLiter
    CsvUnit.MILLILITERS_PER_KG_PER_MINUTE -> value
    CsvUnit.COUNT -> value
}

/**
 * Unit tokens recognised in a column header, longest first so `kcal` is not
 * matched by `cal` and `lbs` is not matched by `lb`.
 */
private val HeaderUnitTokens: List<Pair<String, CsvUnit>> = listOf(
    "kilograms" to CsvUnit.KILOGRAMS,
    "kilogram" to CsvUnit.KILOGRAMS,
    "pounds" to CsvUnit.POUNDS,
    "ml/kg/min" to CsvUnit.MILLILITERS_PER_KG_PER_MINUTE,
    "mmol/l" to CsvUnit.MILLIMOLES_PER_LITER,
    "mg/dl" to CsvUnit.MILLIGRAMS_PER_DECILITER,
    "breaths/min" to CsvUnit.BREATHS_PER_MINUTE,
    "brpm" to CsvUnit.BREATHS_PER_MINUTE,
    "kcal" to CsvUnit.KILOCALORIES_PER_DAY,
    "kj" to CsvUnit.KILOJOULES_PER_DAY,
    "bpm" to CsvUnit.BEATS_PER_MINUTE,
    "lbs" to CsvUnit.POUNDS,
    "lb" to CsvUnit.POUNDS,
    "st" to CsvUnit.STONES,
    "kg" to CsvUnit.KILOGRAMS,
    "cm" to CsvUnit.CENTIMETERS,
    "ms" to CsvUnit.MILLISECONDS,
    "°c" to CsvUnit.CELSIUS,
    "°f" to CsvUnit.FAHRENHEIT,
    "in" to CsvUnit.INCHES,
    "ft" to CsvUnit.FEET,
    "%" to CsvUnit.PERCENT,
    "c" to CsvUnit.CELSIUS,
    "f" to CsvUnit.FAHRENHEIT,
    "g" to CsvUnit.GRAMS,
    "m" to CsvUnit.METERS,
    "s" to CsvUnit.SECONDS,
)

private val HeaderUnitTailRegex = Regex("""\(([^()]*)\)\s*$""")

/**
 * The unit named in [header], or null when it names none.
 *
 * Reads a unit off a label the user has ALREADY chosen to map — it never maps a
 * header string to a metric, so this is a default, not a vendor preset. Only
 * the parenthesised tail is considered, so a column called "Weight in grams of
 * food" cannot be read as grams.
 */
fun detectCsvUnitInHeader(header: String): CsvUnit? {
    val match = HeaderUnitTailRegex.find(header.trim()) ?: return null
    val inner = match.groupValues[1].trim().lowercase()
    if (inner.isEmpty()) return null
    for ((token, unit) in HeaderUnitTokens) {
        if (inner == token) return unit
    }
    return null
}

/**
 * [spec]'s offered interpretation whose unit is [unit], preferring a direct
 * reading over a derived one. Null when the metric cannot express that unit.
 */
fun interpretationForUnit(spec: CsvMetricSpec, unit: CsvUnit): CsvValueInterpretation? {
    spec.interpretations.firstOrNull { it is CsvDirectValue && it.unit == unit }?.let { return it }
    return spec.interpretations.firstOrNull { it is CsvMassShareOfWeight && it.unit == unit }
}
