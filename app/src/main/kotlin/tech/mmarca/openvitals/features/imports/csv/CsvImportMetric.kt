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
 * What a CSV column can be mapped onto, and how its text becomes the
 * canonical value. Every metric is one number at one instant, except
 * [STEPS], the first interval record: an END_TIMESTAMP column ends each
 * row's span, and a missing end means one minute. Blood pressure is absent:
 * it needs two columns per record.
 *
 * The order, catalog and conversions mirror the Flutter build, so both apps
 * produce the same clientRecordIds. STEPS keys on the interval's start.
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

/** A unit a column's numbers are written in. The file's unit, not the app's display unit. */
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

// The same constants the manual-entry screens use.
private const val PoundsPerKilogram = 2.2046226218
private const val CentimetersPerInch = 2.54
private const val KilogramsPerStone = 6.35029318
private const val CentimetersPerFoot = 30.48
private const val KilojoulesPerKilocalorie = 4.184
private const val FahrenheitFreezingPoint = 32.0
private const val FahrenheitPerCelsius = 1.8

/** Health Connect stores glucose in mmol/L; the US convention is mg/dL. */
private const val MilligramsPerDeciliterPerMillimolePerLiter = 18.0

/** How a column's raw number becomes the metric's canonical value. */
sealed interface CsvValueInterpretation {
    /** Whether resolving this needs the row's body weight as well as its own cell. */
    val needsRowWeight: Boolean get() = this is CsvMassShareOfWeight
}

/** The number in the cell IS the metric, expressed in [unit]. */
data class CsvDirectValue(val unit: CsvUnit) : CsvValueInterpretation

/**
 * The cell holds a mass, and the metric is that mass as a share of the row's
 * body weight. Scales export "Fat mass (kg)" while Health Connect stores a
 * percentage, so the weight column of the same row is needed.
 */
data class CsvMassShareOfWeight(
    /** Always a mass unit. */
    val unit: CsvUnit,
) : CsvValueInterpretation

/** Everything the importer needs to know about one metric. */
data class CsvMetricSpec(
    /** The Health Connect record class name. Part of the clientRecordId; must match Flutter. */
    val targetType: String,

    /** The record class, for the existing-id lookup. */
    val recordType: KClass<out Record>,

    /** The single Health Connect write permission this metric needs. */
    val writePermission: String,

    /** Offered in the UI in this order; the first is the default. */
    val interpretations: List<CsvValueInterpretation>,

    /** Bounds on the canonical value. A 900 kg weight is a mis-mapped column. */
    val plausibleMin: Double,
    val plausibleMax: Double,

    /** Whether the value covers a span. Reads the END_TIMESTAMP column; one minute without it. */
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
        // The mass interpretations make a Withings-style export usable as is.
        interpretations = listOf(
            CsvDirectValue(CsvUnit.PERCENT),
            CsvDirectValue(CsvUnit.FRACTION),
            CsvMassShareOfWeight(CsvUnit.KILOGRAMS),
            CsvMassShareOfWeight(CsvUnit.POUNDS),
            CsvMassShareOfWeight(CsvUnit.GRAMS),
        ),
        // Below 2% or above 75% means the wrong weight column was used.
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

    // Vitals. Ranges are "survivable human", not "clinically normal".
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

    // Intervals. A steps cell counts what happened between start and end.
    CsvImportMetric.STEPS to CsvMetricSpec(
        targetType = "StepsRecord",
        recordType = StepsRecord::class,
        writePermission = HealthPermission.getWritePermission(StepsRecord::class),
        interpretations = listOf(CsvDirectValue(CsvUnit.COUNT)),
        // Health Connect refuses a count below 1; 200,000 a day is past any ultramarathon.
        plausibleMin = 1.0,
        plausibleMax = 200000.0,
        isInterval = true,
    ),
)

/** Converts [value] from [unit] to the metric's canonical (metric) unit. */
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

/** Unit tokens recognised in a header, longest first so `kcal` beats `cal`. */
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
 * The unit named in [header], or null. Only the parenthesised tail counts, so
 * "Weight in grams of food" is not read as grams.
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

/** [spec]'s interpretation for [unit], preferring direct over derived. Null if none. */
fun interpretationForUnit(spec: CsvMetricSpec, unit: CsvUnit): CsvValueInterpretation? {
    spec.interpretations.firstOrNull { it is CsvDirectValue && it.unit == unit }?.let { return it }
    return spec.interpretations.firstOrNull { it is CsvMassShareOfWeight && it.unit == unit }
}
