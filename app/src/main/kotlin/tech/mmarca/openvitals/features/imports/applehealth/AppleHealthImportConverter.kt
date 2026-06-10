package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.Volume
import androidx.health.connect.client.units.celsius
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.kilograms
import androidx.health.connect.client.units.meters
import androidx.health.connect.client.units.percent
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToLong

internal data class AppleHealthConversionResult(
    val converted: List<ConvertedAppleRecord>,
    val diagnostics: List<AppleHealthImportDiagnostic>,
    val typeStats: MutableMap<String, MutableAppleImportTypeStats>,
)

internal data class MutableAppleImportTypeStats(
    var parsed: Int = 0,
    var converted: Int = 0,
    var imported: Int = 0,
    var duplicateSkipped: Int = 0,
    var unsupported: Int = 0,
    var skipped: Int = 0,
    var failed: Int = 0,
)

internal class AppleHealthImportConverter(
    private val trackCycle: Boolean,
    private val mindfulnessAvailable: Boolean,
) {
    private val diagnostics = mutableListOf<AppleHealthImportDiagnostic>()
    private val typeStats = linkedMapOf<String, MutableAppleImportTypeStats>()
    private val consumedRecordFingerprints = mutableSetOf<String>()

    fun convert(export: AppleParsedExport): AppleHealthConversionResult {
        export.parsedTypeCounts.forEach { (type, count) ->
            typeStats.getOrPut(type) { MutableAppleImportTypeStats() }.parsed += count
        }

        val converted = buildList {
            addAll(convertBloodPressureCorrelations(export.correlations))
            addAll(convertStandaloneBloodPressure(export.records))
            addAll(convertSleep(export.records))
            addAll(convertNutrition(export.records))
            addAll(convertWorkouts(export.workouts, export.records))
            export.records.forEach { record ->
                if (record.sourceFingerprint in consumedRecordFingerprints) return@forEach
                convertSingleRecord(record)?.let(::add)
            }
            export.correlations
                .filterNot { it.type == AppleBloodPressureCorrelation }
                .forEach { correlation ->
                    unsupported(
                        appleType = correlation.type,
                        detail = "Correlation type has no direct Health Connect import mapping.",
                        timeRange = correlation.timeRangeOrNull()?.toString(),
                    )
                }
            if (export.parsedActivitySummaries > 0) {
                unsupported(
                    appleType = "ActivitySummary",
                    detail = "Apple activity rings and stand hours have no direct writable Health Connect record.",
                    timeRange = null,
                )
            }
        }

        return AppleHealthConversionResult(
            converted = converted,
            diagnostics = diagnostics.toList(),
            typeStats = typeStats,
        )
    }

    private fun convertSingleRecord(record: AppleRecord): ConvertedAppleRecord? {
        val start = record.startDate ?: return invalid(record, "Record is missing startDate.")
        val end = record.endDate ?: start
        val interval = interval(start, end)
        val value = record.numericValue

        fun metadata(targetType: String): Metadata = appleMetadata(record, targetType, record.sourceFingerprint)
        fun converted(targetType: String, fingerprint: String, healthRecord: Record): ConvertedAppleRecord {
            markConverted(record.type)
            return ConvertedAppleRecord(
                appleType = record.type,
                targetType = targetType,
                fingerprint = fingerprint,
                recordType = healthRecord::class,
                record = healthRecord,
                sourceTimeRange = AppleImportTimeRange(interval.start.instant, interval.end.instant),
                unit = record.unit,
                value = record.valueForReport,
            )
        }

        return when (record.type) {
            AppleStepCount -> {
                val count = value?.roundToLong()?.takeIf { it > 0 } ?: return invalid(record, "Step count is missing or not positive.")
                val fingerprint = record.stableClientRecordId("steps")
                converted(
                    "StepsRecord",
                    fingerprint,
                    StepsRecord(
                        startTime = interval.start.instant,
                        startZoneOffset = interval.start.offset,
                        endTime = interval.end.instant,
                        endZoneOffset = interval.end.offset,
                        count = count,
                        metadata = metadata("StepsRecord"),
                    ),
                )
            }

            AppleDistanceWalkingRunning,
            AppleDistanceCycling,
            AppleDistanceSwimming,
            AppleDistanceWheelchair,
            -> {
                val meters = value?.toMeters(record.unit)?.takeIf { it > 0.0 }
                    ?: return invalid(record, "Distance is missing, unsupported unit, or not positive.")
                val fingerprint = record.stableClientRecordId("distance")
                converted(
                    "DistanceRecord",
                    fingerprint,
                    DistanceRecord(
                        startTime = interval.start.instant,
                        startZoneOffset = interval.start.offset,
                        endTime = interval.end.instant,
                        endZoneOffset = interval.end.offset,
                        distance = meters.meters,
                        metadata = metadata("DistanceRecord"),
                    ),
                )
            }

            AppleActiveEnergyBurned -> {
                val kilocalories = value?.toKilocalories(record.unit)?.takeIf { it > 0.0 }
                    ?: return invalid(record, "Active energy is missing, unsupported unit, or not positive.")
                val fingerprint = record.stableClientRecordId("active_calories")
                converted(
                    "ActiveCaloriesBurnedRecord",
                    fingerprint,
                    ActiveCaloriesBurnedRecord(
                        startTime = interval.start.instant,
                        startZoneOffset = interval.start.offset,
                        endTime = interval.end.instant,
                        endZoneOffset = interval.end.offset,
                        energy = kilocalories.kilocalories,
                        metadata = metadata("ActiveCaloriesBurnedRecord"),
                    ),
                )
            }

            AppleBasalEnergyBurned -> {
                val kilocalories = value?.toKilocalories(record.unit)?.takeIf { it > 0.0 }
                    ?: return invalid(record, "Basal energy is missing, unsupported unit, or not positive.")
                val durationSeconds = Duration.between(interval.start.instant, interval.end.instant).seconds.takeIf { it > 0 }
                    ?: return invalid(record, "Basal energy record has no positive duration.")
                val kcalPerDay = kilocalories * 86_400.0 / durationSeconds
                val fingerprint = record.stableClientRecordId("bmr")
                converted(
                    "BasalMetabolicRateRecord",
                    fingerprint,
                    BasalMetabolicRateRecord(
                        time = interval.start.instant,
                        zoneOffset = interval.start.offset,
                        basalMetabolicRate = Power.kilocaloriesPerDay(kcalPerDay),
                        metadata = metadata("BasalMetabolicRateRecord"),
                    ),
                )
            }

            AppleFlightsClimbed -> {
                val floors = value?.takeIf { it > 0.0 } ?: return invalid(record, "Flights climbed is missing or not positive.")
                val fingerprint = record.stableClientRecordId("floors")
                converted(
                    "FloorsClimbedRecord",
                    fingerprint,
                    FloorsClimbedRecord(
                        startTime = interval.start.instant,
                        startZoneOffset = interval.start.offset,
                        endTime = interval.end.instant,
                        endZoneOffset = interval.end.offset,
                        floors = floors,
                        metadata = metadata("FloorsClimbedRecord"),
                    ),
                )
            }

            AppleElevationAscended -> {
                val meters = value?.toMeters(record.unit)?.takeIf { it > 0.0 }
                    ?: return invalid(record, "Elevation is missing, unsupported unit, or not positive.")
                val fingerprint = record.stableClientRecordId("elevation")
                converted(
                    "ElevationGainedRecord",
                    fingerprint,
                    ElevationGainedRecord(
                        startTime = interval.start.instant,
                        startZoneOffset = interval.start.offset,
                        endTime = interval.end.instant,
                        endZoneOffset = interval.end.offset,
                        elevation = meters.meters,
                        metadata = metadata("ElevationGainedRecord"),
                    ),
                )
            }

            ApplePushCount -> {
                val count = value?.roundToLong()?.takeIf { it > 0 } ?: return invalid(record, "Wheelchair pushes is missing or not positive.")
                val fingerprint = record.stableClientRecordId("wheelchair_pushes")
                converted(
                    "WheelchairPushesRecord",
                    fingerprint,
                    WheelchairPushesRecord(
                        startTime = interval.start.instant,
                        startZoneOffset = interval.start.offset,
                        endTime = interval.end.instant,
                        endZoneOffset = interval.end.offset,
                        count = count,
                        metadata = metadata("WheelchairPushesRecord"),
                    ),
                )
            }

            AppleHeartRate -> {
                val bpm = value?.roundToLong()?.takeIf { it in 1..300 } ?: return invalid(record, "Heart rate is outside 1..300 bpm.")
                val fingerprint = record.stableClientRecordId("heart_rate")
                converted(
                    "HeartRateRecord",
                    fingerprint,
                    HeartRateRecord(
                        startTime = interval.start.instant,
                        startZoneOffset = interval.start.offset,
                        endTime = interval.end.instant,
                        endZoneOffset = interval.end.offset,
                        samples = listOf(HeartRateRecord.Sample(time = start.instant, beatsPerMinute = bpm)),
                        metadata = metadata("HeartRateRecord"),
                    ),
                )
            }

            AppleRestingHeartRate -> {
                val bpm = value?.roundToLong()?.takeIf { it in 1..300 } ?: return invalid(record, "Resting heart rate is outside 1..300 bpm.")
                val fingerprint = record.stableClientRecordId("resting_hr")
                converted(
                    "RestingHeartRateRecord",
                    fingerprint,
                    RestingHeartRateRecord(
                        time = start.instant,
                        zoneOffset = start.offset,
                        beatsPerMinute = bpm,
                        metadata = metadata("RestingHeartRateRecord"),
                    ),
                )
            }

            AppleBodyMass -> convertWeight(record, start, metadata("WeightRecord"))
            AppleHeight -> convertHeight(record, start, metadata("HeightRecord"))
            AppleBodyFatPercentage -> convertBodyFat(record, start, metadata("BodyFatRecord"))
            AppleLeanBodyMass -> convertLeanMass(record, start, metadata("LeanBodyMassRecord"))
            AppleBoneMass -> convertBoneMass(record, start, metadata("BoneMassRecord"))
            AppleBodyWaterMass -> convertBodyWaterMass(record, start, metadata("BodyWaterMassRecord"))
            AppleDietaryWater -> convertHydration(record, interval, metadata("HydrationRecord"))
            AppleOxygenSaturation -> convertOxygenSaturation(record, start, metadata("OxygenSaturationRecord"))
            AppleRespiratoryRate -> convertRespiratoryRate(record, start, metadata("RespiratoryRateRecord"))
            AppleBodyTemperature -> convertBodyTemperature(record, start, metadata("BodyTemperatureRecord"))
            AppleBloodGlucose -> convertBloodGlucose(record, start, metadata("BloodGlucoseRecord"))
            AppleVo2Max -> convertVo2Max(record, start, metadata("Vo2MaxRecord"))
            AppleBasalBodyTemperature -> convertBasalBodyTemperature(record, start, metadata("BasalBodyTemperatureRecord"))
            in AppleCycleCategoryTypes -> convertCycleCategory(record, start, metadata(record.type.substringAfterLast("Identifier")))
            AppleMindfulSession -> convertMindfulness(record, interval, metadata("MindfulnessSessionRecord"))
            AppleHeartRateVariabilitySdnn -> unsupportedNull(record, "Apple exports HRV as SDNN; Health Connect record in this SDK is RMSSD, so this is not imported.")
            else -> unsupportedNull(record, "No direct Health Connect mapping is implemented for this Apple record type.")
        }
    }

    private fun convertWeight(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        val kg = record.numericValue?.toKilograms(record.unit)?.takeIf { it > 0.0 }
            ?: return invalid(record, "Weight is missing, unsupported unit, or not positive.")
        val fingerprint = record.stableClientRecordId("weight")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "WeightRecord",
            fingerprint,
            WeightRecord::class,
            WeightRecord(start.instant, start.offset, kg.kilograms, metadata),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertHeight(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        val meters = record.numericValue?.toMeters(record.unit)?.takeIf { it > 0.0 }
            ?: return invalid(record, "Height is missing, unsupported unit, or not positive.")
        val fingerprint = record.stableClientRecordId("height")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "HeightRecord",
            fingerprint,
            HeightRecord::class,
            HeightRecord(start.instant, start.offset, meters.meters, metadata),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertBodyFat(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        val percent = record.numericValue?.toPercentage(record.unit)?.takeIf { it in 0.0..100.0 }
            ?: return invalid(record, "Body fat is missing, unsupported unit, or outside 0..100%.")
        val fingerprint = record.stableClientRecordId("body_fat")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "BodyFatRecord",
            fingerprint,
            BodyFatRecord::class,
            BodyFatRecord(start.instant, start.offset, percent.percent, metadata),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertLeanMass(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        val kg = record.numericValue?.toKilograms(record.unit)?.takeIf { it > 0.0 }
            ?: return invalid(record, "Lean body mass is missing, unsupported unit, or not positive.")
        val fingerprint = record.stableClientRecordId("lean_mass")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "LeanBodyMassRecord",
            fingerprint,
            LeanBodyMassRecord::class,
            LeanBodyMassRecord(start.instant, start.offset, kg.kilograms, metadata),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertBoneMass(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        val kg = record.numericValue?.toKilograms(record.unit)?.takeIf { it > 0.0 }
            ?: return invalid(record, "Bone mass is missing, unsupported unit, or not positive.")
        val fingerprint = record.stableClientRecordId("bone_mass")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "BoneMassRecord",
            fingerprint,
            BoneMassRecord::class,
            BoneMassRecord(start.instant, start.offset, kg.kilograms, metadata),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertBodyWaterMass(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        val kg = record.numericValue?.toKilograms(record.unit)?.takeIf { it > 0.0 }
            ?: return invalid(record, "Body water mass is missing, unsupported unit, or not positive.")
        val fingerprint = record.stableClientRecordId("body_water_mass")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "BodyWaterMassRecord",
            fingerprint,
            BodyWaterMassRecord::class,
            BodyWaterMassRecord(start.instant, start.offset, kg.kilograms, metadata),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertHydration(record: AppleRecord, interval: AppleInterval, metadata: Metadata): ConvertedAppleRecord? {
        val milliliters = record.numericValue?.toMilliliters(record.unit)?.takeIf { it > 0.0 }
            ?: return invalid(record, "Hydration is missing, unsupported unit, or not positive.")
        val fingerprint = record.stableClientRecordId("hydration")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "HydrationRecord",
            fingerprint,
            HydrationRecord::class,
            HydrationRecord(
                startTime = interval.start.instant,
                startZoneOffset = interval.start.offset,
                endTime = interval.end.instant,
                endZoneOffset = interval.end.offset,
                volume = Volume.milliliters(milliliters),
                metadata = metadata,
            ),
            AppleImportTimeRange(interval.start.instant, interval.end.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertOxygenSaturation(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        val percent = record.numericValue?.toPercentage(record.unit)?.takeIf { it in 0.0..100.0 }
            ?: return invalid(record, "Oxygen saturation is missing, unsupported unit, or outside 0..100%.")
        val fingerprint = record.stableClientRecordId("spo2")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "OxygenSaturationRecord",
            fingerprint,
            OxygenSaturationRecord::class,
            OxygenSaturationRecord(start.instant, start.offset, percent.percent, metadata),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertRespiratoryRate(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        val rate = record.numericValue?.takeIf { it > 0.0 }
            ?: return invalid(record, "Respiratory rate is missing or not positive.")
        val fingerprint = record.stableClientRecordId("respiratory_rate")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "RespiratoryRateRecord",
            fingerprint,
            RespiratoryRateRecord::class,
            RespiratoryRateRecord(start.instant, start.offset, rate, metadata),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertBodyTemperature(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        val celsius = record.numericValue?.toCelsius(record.unit)
            ?: return invalid(record, "Body temperature is missing or has an unsupported unit.")
        val fingerprint = record.stableClientRecordId("body_temperature")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "BodyTemperatureRecord",
            fingerprint,
            BodyTemperatureRecord::class,
            BodyTemperatureRecord(
                time = start.instant,
                zoneOffset = start.offset,
                temperature = celsius.celsius,
                metadata = metadata,
            ),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertBloodGlucose(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        val glucose = record.numericValue?.toBloodGlucose(record.unit)
            ?: return invalid(record, "Blood glucose is missing or has an unsupported unit.")
        val fingerprint = record.stableClientRecordId("blood_glucose")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "BloodGlucoseRecord",
            fingerprint,
            BloodGlucoseRecord::class,
            BloodGlucoseRecord(
                time = start.instant,
                zoneOffset = start.offset,
                metadata = metadata,
                level = glucose,
                specimenSource = BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN,
                mealType = MealType.MEAL_TYPE_UNKNOWN,
                relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN,
            ),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertVo2Max(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        val vo2 = record.numericValue?.takeIf { it in 1.0..100.0 }
            ?: return invalid(record, "VO2 max is missing or outside 1..100 mL/kg/min.")
        val fingerprint = record.stableClientRecordId("vo2_max")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "Vo2MaxRecord",
            fingerprint,
            Vo2MaxRecord::class,
            Vo2MaxRecord(
                time = start.instant,
                zoneOffset = start.offset,
                metadata = metadata,
                vo2MillilitersPerMinuteKilogram = vo2,
                measurementMethod = Vo2MaxRecord.MEASUREMENT_METHOD_OTHER,
            ),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertBasalBodyTemperature(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        if (!trackCycle) return skippedNull(record, "cycle_disabled", "Cycle tracking is disabled.")
        val celsius = record.numericValue?.toCelsius(record.unit)
            ?: return invalid(record, "Basal body temperature is missing or has an unsupported unit.")
        val fingerprint = record.stableClientRecordId("basal_body_temperature")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "BasalBodyTemperatureRecord",
            fingerprint,
            BasalBodyTemperatureRecord::class,
            BasalBodyTemperatureRecord(
                time = start.instant,
                zoneOffset = start.offset,
                metadata = metadata,
                temperature = celsius.celsius,
            ),
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertMindfulness(record: AppleRecord, interval: AppleInterval, metadata: Metadata): ConvertedAppleRecord? {
        if (!mindfulnessAvailable) {
            return skippedNull(record, "feature_unavailable", "Mindfulness sessions are not available in this Health Connect provider.")
        }
        val fingerprint = record.stableClientRecordId("mindfulness")
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            "MindfulnessSessionRecord",
            fingerprint,
            MindfulnessSessionRecord::class,
            MindfulnessSessionRecord(
                startTime = interval.start.instant,
                startZoneOffset = interval.start.offset,
                endTime = interval.end.instant,
                endZoneOffset = interval.end.offset,
                metadata = metadata,
                mindfulnessSessionType = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNKNOWN,
                title = "Apple Health mindfulness",
            ),
            AppleImportTimeRange(interval.start.instant, interval.end.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertCycleCategory(record: AppleRecord, start: AppleDateTime, metadata: Metadata): ConvertedAppleRecord? {
        if (!trackCycle) return skippedNull(record, "cycle_disabled", "Cycle tracking is disabled.")
        val fingerprint = record.stableClientRecordId("cycle")
        val rawValue = record.rawValue.orEmpty()
        val convertedRecord: Record =
            when (record.type) {
                AppleMenstrualFlow -> MenstruationFlowRecord(
                    time = start.instant,
                    zoneOffset = start.offset,
                    metadata = metadata,
                    flow = rawValue.toMenstrualFlow(),
                )
                AppleOvulationTest -> OvulationTestRecord(
                    time = start.instant,
                    zoneOffset = start.offset,
                    result = rawValue.toOvulationResult(),
                    metadata = metadata,
                )
                AppleCervicalMucus -> CervicalMucusRecord(
                    time = start.instant,
                    zoneOffset = start.offset,
                    metadata = metadata,
                    appearance = rawValue.toCervicalMucusAppearance(),
                    sensation = CervicalMucusRecord.SENSATION_UNKNOWN,
                )
                AppleIntermenstrualBleeding -> IntermenstrualBleedingRecord(
                    time = start.instant,
                    zoneOffset = start.offset,
                    metadata = metadata,
                )
                AppleSexualActivity -> SexualActivityRecord(
                    time = start.instant,
                    zoneOffset = start.offset,
                    metadata = metadata,
                    protectionUsed = record.metadata.toProtectionUsed(),
                )
                else -> return unsupportedNull(record, "No direct cycle mapping is implemented for this Apple record type.")
            }
        markConverted(record.type)
        return ConvertedAppleRecord(
            record.type,
            convertedRecord::class.simpleName ?: "Record",
            fingerprint,
            convertedRecord::class,
            convertedRecord,
            AppleImportTimeRange(start.instant, start.instant),
            record.unit,
            record.valueForReport,
        )
    }

    private fun convertBloodPressureCorrelations(correlations: List<AppleCorrelation>): List<ConvertedAppleRecord> =
        correlations
            .filter { it.type == AppleBloodPressureCorrelation }
            .mapNotNull { correlation ->
                val systolic = correlation.records.firstOrNull { it.type == AppleBloodPressureSystolic }
                val diastolic = correlation.records.firstOrNull { it.type == AppleBloodPressureDiastolic }
                if (systolic == null || diastolic == null) {
                    invalid(
                        appleType = correlation.type,
                        detail = "Blood pressure correlation is missing systolic or diastolic child record.",
                        timeRange = correlation.timeRangeOrNull()?.toString(),
                    )
                    return@mapNotNull null
                }
                consumedRecordFingerprints += systolic.sourceFingerprint
                consumedRecordFingerprints += diastolic.sourceFingerprint
                buildBloodPressureRecord(
                    appleType = correlation.type,
                    start = correlation.startDate ?: systolic.startDate ?: diastolic.startDate,
                    sourceEnd = correlation.endDate ?: systolic.endDate ?: diastolic.endDate,
                    sourceName = correlation.sourceName ?: systolic.sourceName ?: diastolic.sourceName,
                    unit = systolic.unit ?: diastolic.unit,
                    value = "${systolic.rawValue}/${diastolic.rawValue}",
                    systolic = systolic.numericValue,
                    diastolic = diastolic.numericValue,
                    stableParts = listOf("bp_correlation", correlation.stableParts(), systolic.stableParts(), diastolic.stableParts()),
                )
            }

    private fun convertStandaloneBloodPressure(records: List<AppleRecord>): List<ConvertedAppleRecord> {
        val grouped = records
            .filter { it.type == AppleBloodPressureSystolic || it.type == AppleBloodPressureDiastolic }
            .groupBy { listOf(it.sourceName.orEmpty(), it.creationDate?.instant?.toString().orEmpty(), it.startDate?.instant?.toString().orEmpty(), it.endDate?.instant?.toString().orEmpty()) }

        return grouped.mapNotNull { (_, group) ->
            val systolic = group.firstOrNull { it.type == AppleBloodPressureSystolic }
            val diastolic = group.firstOrNull { it.type == AppleBloodPressureDiastolic }
            if (systolic == null || diastolic == null) {
                group.forEach {
                    if (it.sourceFingerprint !in consumedRecordFingerprints) {
                        invalid(it, "Standalone blood pressure value could not be paired with systolic and diastolic values.")
                    }
                }
                return@mapNotNull null
            }
            if (systolic.sourceFingerprint in consumedRecordFingerprints || diastolic.sourceFingerprint in consumedRecordFingerprints) {
                return@mapNotNull null
            }
            consumedRecordFingerprints += systolic.sourceFingerprint
            consumedRecordFingerprints += diastolic.sourceFingerprint
            buildBloodPressureRecord(
                appleType = AppleBloodPressureCorrelation,
                start = systolic.startDate ?: diastolic.startDate,
                sourceEnd = systolic.endDate ?: diastolic.endDate,
                sourceName = systolic.sourceName ?: diastolic.sourceName,
                unit = systolic.unit ?: diastolic.unit,
                value = "${systolic.rawValue}/${diastolic.rawValue}",
                systolic = systolic.numericValue,
                diastolic = diastolic.numericValue,
                stableParts = listOf("bp_pair", systolic.stableParts(), diastolic.stableParts()),
            )
        }
    }

    private fun buildBloodPressureRecord(
        appleType: String,
        start: AppleDateTime?,
        sourceEnd: AppleDateTime?,
        sourceName: String?,
        unit: String?,
        value: String?,
        systolic: Double?,
        diastolic: Double?,
        stableParts: List<String>,
    ): ConvertedAppleRecord? {
        val time = start ?: return invalid(
            appleType = appleType,
            detail = "Blood pressure is missing measurement time.",
            timeRange = null,
        )
        val sys = systolic?.takeIf { it in 20.0..300.0 } ?: return invalid(
            appleType = appleType,
            detail = "Systolic value is missing or outside supported range.",
            timeRange = time.instant.toString(),
        )
        val dia = diastolic?.takeIf { it in 10.0..180.0 } ?: return invalid(
            appleType = appleType,
            detail = "Diastolic value is missing or outside supported range.",
            timeRange = time.instant.toString(),
        )
        val fingerprint = buildStableClientRecordId("blood_pressure", stableParts + sourceName.orEmpty())
        markConverted(appleType)
        return ConvertedAppleRecord(
            appleType = appleType,
            targetType = "BloodPressureRecord",
            fingerprint = fingerprint,
            recordType = BloodPressureRecord::class,
            record = BloodPressureRecord(
                time = time.instant,
                zoneOffset = time.offset,
                metadata = appleMetadata("BloodPressureRecord", fingerprint),
                systolic = Pressure.millimetersOfMercury(sys),
                diastolic = Pressure.millimetersOfMercury(dia),
            ),
            sourceTimeRange = AppleImportTimeRange(time.instant, sourceEnd?.instant ?: time.instant),
            unit = unit,
            value = value,
        )
    }

    private fun convertSleep(records: List<AppleRecord>): List<ConvertedAppleRecord> {
        val sleepRecords = records.filter { it.type == AppleSleepAnalysis }
        if (sleepRecords.isEmpty()) return emptyList()
        sleepRecords.forEach { consumedRecordFingerprints += it.sourceFingerprint }

        val groups = sleepRecords
            .mapNotNull { record ->
                val start = record.startDate ?: return@mapNotNull null.also { invalid(record, "Sleep record is missing startDate.") }
                val end = record.endDate ?: return@mapNotNull null.also { invalid(record, "Sleep record is missing endDate.") }
                val stage = record.rawValue.toSleepStageType()
                    ?: return@mapNotNull null.also { invalid(record, "Sleep stage value is unsupported.") }
                SleepStageCandidate(
                    record = record,
                    start = start,
                    end = end,
                    stage = stage,
                    inBedOnly = record.rawValue == AppleSleepInBed,
                )
            }
            .groupBy { "${it.record.sourceName.orEmpty()}|${it.record.device.orEmpty()}" }

        return groups.values.flatMap { candidates ->
            candidates.sortedBy { it.start.instant }.splitSleepSessions().mapNotNull { session ->
                val sessionStart = session.minOf { it.start.instant }
                val sessionEnd = session.maxOf { it.end.instant }
                if (!sessionEnd.isAfter(sessionStart)) {
                    invalid(AppleSleepAnalysis, "Sleep session has no positive duration.", "$sessionStart..$sessionEnd")
                    return@mapNotNull null
                }
                val detailedStages = session.filterNot { it.inBedOnly }.takeIf { it.isNotEmpty() } ?: session
                val stages = detailedStages
                    .sortedBy { it.start.instant }
                    .fold(mutableListOf<SleepSessionRecord.Stage>()) { acc, candidate ->
                        val clippedStart = maxOf(candidate.start.instant, acc.lastOrNull()?.endTime ?: sessionStart)
                        val clippedEnd = minOf(candidate.end.instant, sessionEnd)
                        if (clippedEnd.isAfter(clippedStart)) {
                            acc += SleepSessionRecord.Stage(
                                startTime = clippedStart,
                                endTime = clippedEnd,
                                stage = candidate.stage,
                            )
                        }
                        acc
                    }
                if (stages.isEmpty()) {
                    invalid(AppleSleepAnalysis, "Sleep session did not contain any valid non-overlapping stages.", "$sessionStart..$sessionEnd")
                    return@mapNotNull null
                }
                val first = session.first().record
                val fingerprint = buildStableClientRecordId(
                    "sleep",
                    listOf("sleep", sessionStart.toString(), sessionEnd.toString(), session.joinToString(";") { it.record.stableParts() }),
                )
                markConverted(AppleSleepAnalysis)
                ConvertedAppleRecord(
                    appleType = AppleSleepAnalysis,
                    targetType = "SleepSessionRecord",
                    fingerprint = fingerprint,
                    recordType = SleepSessionRecord::class,
                    record = SleepSessionRecord(
                        startTime = sessionStart,
                        startZoneOffset = first.startDate?.offset,
                        endTime = sessionEnd,
                        endZoneOffset = first.endDate?.offset ?: first.startDate?.offset,
                        metadata = appleMetadata("SleepSessionRecord", fingerprint),
                        title = "Apple Health sleep",
                        notes = null,
                        stages = stages,
                    ),
                    sourceTimeRange = AppleImportTimeRange(sessionStart, sessionEnd),
                    unit = null,
                    value = "stages=${stages.size}",
                )
            }
        }
    }

    private fun convertNutrition(records: List<AppleRecord>): List<ConvertedAppleRecord> {
        val nutritionRecords = records.filter { it.type in AppleNutritionTypes && it.type != AppleDietaryWater }
        if (nutritionRecords.isEmpty()) return emptyList()

        val grouped = nutritionRecords.groupBy { record ->
            listOf(
                record.sourceName.orEmpty(),
                record.startDate?.instant?.toString().orEmpty(),
                record.endDate?.instant?.toString().orEmpty(),
                record.metadata["HKMetadataKeyFoodType"].orEmpty(),
            ).joinToString("|")
        }

        return grouped.values.mapNotNull { group ->
            val start = group.mapNotNull { it.startDate }.minByOrNull { it.instant }
            val end = group.mapNotNull { it.endDate ?: it.startDate }.maxByOrNull { it.instant }
            if (start == null) {
                group.forEach { invalid(it, "Nutrition record is missing startDate.") }
                return@mapNotNull null
            }
            val interval = interval(start, end ?: start)
            val nutrients = NutritionValues()
            group.forEach { record ->
                val value = record.numericValue
                val applied = value != null && nutrients.apply(record.type, value, record.unit)
                if (applied) {
                    consumedRecordFingerprints += record.sourceFingerprint
                } else {
                    invalid(record, "Nutrition value is missing or has an unsupported unit.")
                }
            }
            if (!nutrients.hasAny) return@mapNotNull null
            val fingerprint = buildStableClientRecordId("nutrition", group.map { it.stableParts() })
            markConverted(AppleNutritionSyntheticType)
            ConvertedAppleRecord(
                appleType = AppleNutritionSyntheticType,
                targetType = "NutritionRecord",
                fingerprint = fingerprint,
                recordType = NutritionRecord::class,
                record = NutritionRecord(
                    startTime = interval.start.instant,
                    startZoneOffset = interval.start.offset,
                    endTime = interval.end.instant,
                    endZoneOffset = interval.end.offset,
                    metadata = appleMetadata("NutritionRecord", fingerprint),
                    biotin = nutrients.biotin,
                    caffeine = nutrients.caffeine,
                    calcium = nutrients.calcium,
                    energy = nutrients.energy,
                    energyFromFat = nutrients.energyFromFat,
                    cholesterol = nutrients.cholesterol,
                    chromium = nutrients.chromium,
                    copper = nutrients.copper,
                    dietaryFiber = nutrients.dietaryFiber,
                    folate = nutrients.folate,
                    iodine = nutrients.iodine,
                    iron = nutrients.iron,
                    magnesium = nutrients.magnesium,
                    manganese = nutrients.manganese,
                    molybdenum = nutrients.molybdenum,
                    monounsaturatedFat = nutrients.monounsaturatedFat,
                    niacin = nutrients.niacin,
                    pantothenicAcid = nutrients.pantothenicAcid,
                    phosphorus = nutrients.phosphorus,
                    polyunsaturatedFat = nutrients.polyunsaturatedFat,
                    potassium = nutrients.potassium,
                    protein = nutrients.protein,
                    riboflavin = nutrients.riboflavin,
                    saturatedFat = nutrients.saturatedFat,
                    selenium = nutrients.selenium,
                    sodium = nutrients.sodium,
                    sugar = nutrients.sugar,
                    thiamin = nutrients.thiamin,
                    totalCarbohydrate = nutrients.totalCarbohydrate,
                    totalFat = nutrients.totalFat,
                    transFat = nutrients.transFat,
                    vitaminA = nutrients.vitaminA,
                    vitaminB12 = nutrients.vitaminB12,
                    vitaminB6 = nutrients.vitaminB6,
                    vitaminC = nutrients.vitaminC,
                    vitaminD = nutrients.vitaminD,
                    vitaminE = nutrients.vitaminE,
                    vitaminK = nutrients.vitaminK,
                    zinc = nutrients.zinc,
                    name = group.firstNotNullOfOrNull { it.metadata["HKMetadataKeyFoodType"] },
                    mealType = MealType.MEAL_TYPE_UNKNOWN,
                ),
                sourceTimeRange = AppleImportTimeRange(interval.start.instant, interval.end.instant),
                unit = null,
                value = "nutrients=${group.size}",
            )
        }
    }

    private fun convertWorkouts(workouts: List<AppleWorkout>, records: List<AppleRecord>): List<ConvertedAppleRecord> =
        workouts.flatMap { workout ->
            val start = workout.startDate ?: return@flatMap emptyList<ConvertedAppleRecord>().also {
                invalid(workout.workoutActivityType, "Workout is missing startDate.", null)
            }
            val end = workout.endDate ?: return@flatMap emptyList<ConvertedAppleRecord>().also {
                invalid(workout.workoutActivityType, "Workout is missing endDate.", start.instant.toString())
            }
            val interval = interval(start, end)
            val fingerprint = buildStableClientRecordId("workout", workout.stableParts())
            val session = ExerciseSessionRecord(
                startTime = interval.start.instant,
                startZoneOffset = interval.start.offset,
                endTime = interval.end.instant,
                endZoneOffset = interval.end.offset,
                metadata = appleMetadata("ExerciseSessionRecord", fingerprint),
                exerciseType = workout.workoutActivityType.toExerciseType(),
                title = workout.workoutActivityType.removePrefix("HKWorkoutActivityType").ifBlank { "Apple Health workout" },
            )
            val convertedSession = ConvertedAppleRecord(
                appleType = workout.workoutActivityType,
                targetType = "ExerciseSessionRecord",
                fingerprint = fingerprint,
                recordType = ExerciseSessionRecord::class,
                record = session,
                sourceTimeRange = AppleImportTimeRange(interval.start.instant, interval.end.instant),
                unit = workout.durationUnit,
                value = workout.duration?.toString(),
            )
            markConverted(workout.workoutActivityType)
            buildList {
                add(convertedSession)
                if (!records.hasOverlapping(workout, AppleDistanceTypes)) {
                    workout.totalDistance
                        ?.toMeters(workout.totalDistanceUnit)
                        ?.takeIf { it > 0.0 }
                        ?.let { meters ->
                            val distanceFingerprint = buildStableClientRecordId("workout_distance", workout.stableParts() + "|distance")
                            add(
                                ConvertedAppleRecord(
                                    appleType = workout.workoutActivityType,
                                    targetType = "DistanceRecord",
                                    fingerprint = distanceFingerprint,
                                    recordType = DistanceRecord::class,
                                    record = DistanceRecord(
                                        startTime = interval.start.instant,
                                        startZoneOffset = interval.start.offset,
                                        endTime = interval.end.instant,
                                        endZoneOffset = interval.end.offset,
                                        distance = meters.meters,
                                        metadata = appleMetadata("DistanceRecord", distanceFingerprint),
                                    ),
                                    sourceTimeRange = AppleImportTimeRange(interval.start.instant, interval.end.instant),
                                    unit = workout.totalDistanceUnit,
                                    value = workout.totalDistance.toString(),
                                ),
                            )
                        }
                }
                if (!records.hasOverlapping(workout, setOf(AppleActiveEnergyBurned))) {
                    workout.totalEnergyBurned
                        ?.toKilocalories(workout.totalEnergyBurnedUnit)
                        ?.takeIf { it > 0.0 }
                        ?.let { kcal ->
                            val energyFingerprint = buildStableClientRecordId("workout_active_calories", workout.stableParts() + "|energy")
                            add(
                                ConvertedAppleRecord(
                                    appleType = workout.workoutActivityType,
                                    targetType = "ActiveCaloriesBurnedRecord",
                                    fingerprint = energyFingerprint,
                                    recordType = ActiveCaloriesBurnedRecord::class,
                                    record = ActiveCaloriesBurnedRecord(
                                        startTime = interval.start.instant,
                                        startZoneOffset = interval.start.offset,
                                        endTime = interval.end.instant,
                                        endZoneOffset = interval.end.offset,
                                        energy = kcal.kilocalories,
                                        metadata = appleMetadata("ActiveCaloriesBurnedRecord", energyFingerprint),
                                    ),
                                    sourceTimeRange = AppleImportTimeRange(interval.start.instant, interval.end.instant),
                                    unit = workout.totalEnergyBurnedUnit,
                                    value = workout.totalEnergyBurned.toString(),
                                ),
                            )
                        }
                }
            }
        }

    private fun invalid(record: AppleRecord, detail: String): Nothing? =
        invalid(record.type, detail, record.timeRangeOrNull()?.toString(), record.unit, record.valueForReport)

    private fun invalid(
        appleType: String,
        detail: String,
        timeRange: String?,
        unit: String? = null,
        value: String? = null,
    ): Nothing? {
        diagnostics += AppleHealthImportDiagnostic(appleType, null, "invalid", timeRange, unit, value, detail)
        typeStats.getOrPut(appleType) { MutableAppleImportTypeStats() }.failed += 1
        return null
    }

    private fun unsupportedNull(record: AppleRecord, detail: String): Nothing? =
        unsupported(record.type, detail, record.timeRangeOrNull()?.toString(), record.unit, record.valueForReport)

    private fun unsupported(
        appleType: String,
        detail: String,
        timeRange: String?,
        unit: String? = null,
        value: String? = null,
    ): Nothing? {
        diagnostics += AppleHealthImportDiagnostic(appleType, null, "unsupported", timeRange, unit, value, detail)
        typeStats.getOrPut(appleType) { MutableAppleImportTypeStats() }.unsupported += 1
        return null
    }

    private fun skippedNull(record: AppleRecord, reasonCode: String, detail: String): Nothing? {
        diagnostics += AppleHealthImportDiagnostic(record.type, null, reasonCode, record.timeRangeOrNull()?.toString(), record.unit, record.valueForReport, detail)
        typeStats.getOrPut(record.type) { MutableAppleImportTypeStats() }.skipped += 1
        return null
    }

    private fun markConverted(appleType: String) {
        typeStats.getOrPut(appleType) { MutableAppleImportTypeStats() }.converted += 1
    }
}

private data class AppleInterval(
    val start: AppleDateTime,
    val end: AppleDateTime,
)

private fun interval(start: AppleDateTime, end: AppleDateTime): AppleInterval {
    val adjustedEnd =
        if (end.instant.isAfter(start.instant)) {
            end
        } else {
            end.copy(instant = start.instant.plusSeconds(1), offset = end.offset ?: start.offset)
        }
    return AppleInterval(start = start, end = adjustedEnd)
}

private data class SleepStageCandidate(
    val record: AppleRecord,
    val start: AppleDateTime,
    val end: AppleDateTime,
    val stage: Int,
    val inBedOnly: Boolean,
)

private fun List<SleepStageCandidate>.splitSleepSessions(): List<List<SleepStageCandidate>> {
    val sessions = mutableListOf<MutableList<SleepStageCandidate>>()
    forEach { candidate ->
        val current = sessions.lastOrNull()
        if (current == null || Duration.between(current.maxOf { it.end.instant }, candidate.start.instant) > SleepSessionGap) {
            sessions += mutableListOf(candidate)
        } else {
            current += candidate
        }
    }
    return sessions
}

private class NutritionValues {
    var biotin: Mass? = null
    var caffeine: Mass? = null
    var calcium: Mass? = null
    var energy: Energy? = null
    var energyFromFat: Energy? = null
    var cholesterol: Mass? = null
    var chromium: Mass? = null
    var copper: Mass? = null
    var dietaryFiber: Mass? = null
    var folate: Mass? = null
    var iodine: Mass? = null
    var iron: Mass? = null
    var magnesium: Mass? = null
    var manganese: Mass? = null
    var molybdenum: Mass? = null
    var monounsaturatedFat: Mass? = null
    var niacin: Mass? = null
    var pantothenicAcid: Mass? = null
    var phosphorus: Mass? = null
    var polyunsaturatedFat: Mass? = null
    var potassium: Mass? = null
    var protein: Mass? = null
    var riboflavin: Mass? = null
    var saturatedFat: Mass? = null
    var selenium: Mass? = null
    var sodium: Mass? = null
    var sugar: Mass? = null
    var thiamin: Mass? = null
    var totalCarbohydrate: Mass? = null
    var totalFat: Mass? = null
    var transFat: Mass? = null
    var vitaminA: Mass? = null
    var vitaminB12: Mass? = null
    var vitaminB6: Mass? = null
    var vitaminC: Mass? = null
    var vitaminD: Mass? = null
    var vitaminE: Mass? = null
    var vitaminK: Mass? = null
    var zinc: Mass? = null
    var hasAny: Boolean = false

    fun apply(type: String, value: Double, unit: String?): Boolean {
        val applied = when (type) {
            AppleDietaryEnergyConsumed -> value.toKilocalories(unit)?.let { energy = it.kilocalories } != null
            AppleDietaryFatTotal -> value.toMass(unit)?.let { totalFat = it } != null
            AppleDietaryFatSaturated -> value.toMass(unit)?.let { saturatedFat = it } != null
            AppleDietaryFatTrans -> value.toMass(unit)?.let { transFat = it } != null
            AppleDietaryFatMonounsaturated -> value.toMass(unit)?.let { monounsaturatedFat = it } != null
            AppleDietaryFatPolyunsaturated -> value.toMass(unit)?.let { polyunsaturatedFat = it } != null
            AppleDietaryCholesterol -> value.toMass(unit)?.let { cholesterol = it } != null
            AppleDietarySodium -> value.toMass(unit)?.let { sodium = it } != null
            AppleDietaryCarbohydrates -> value.toMass(unit)?.let { totalCarbohydrate = it } != null
            AppleDietaryFiber -> value.toMass(unit)?.let { dietaryFiber = it } != null
            AppleDietarySugar -> value.toMass(unit)?.let { sugar = it } != null
            AppleDietaryProtein -> value.toMass(unit)?.let { protein = it } != null
            AppleDietaryCaffeine -> value.toMass(unit)?.let { caffeine = it } != null
            AppleDietaryCalcium -> value.toMass(unit)?.let { calcium = it } != null
            AppleDietaryIron -> value.toMass(unit)?.let { iron = it } != null
            AppleDietaryThiamin -> value.toMass(unit)?.let { thiamin = it } != null
            AppleDietaryRiboflavin -> value.toMass(unit)?.let { riboflavin = it } != null
            AppleDietaryNiacin -> value.toMass(unit)?.let { niacin = it } != null
            AppleDietaryFolate -> value.toMass(unit)?.let { folate = it } != null
            AppleDietaryBiotin -> value.toMass(unit)?.let { biotin = it } != null
            AppleDietaryPantothenicAcid -> value.toMass(unit)?.let { pantothenicAcid = it } != null
            AppleDietaryPhosphorus -> value.toMass(unit)?.let { phosphorus = it } != null
            AppleDietaryIodine -> value.toMass(unit)?.let { iodine = it } != null
            AppleDietaryMagnesium -> value.toMass(unit)?.let { magnesium = it } != null
            AppleDietaryZinc -> value.toMass(unit)?.let { zinc = it } != null
            AppleDietarySelenium -> value.toMass(unit)?.let { selenium = it } != null
            AppleDietaryCopper -> value.toMass(unit)?.let { copper = it } != null
            AppleDietaryManganese -> value.toMass(unit)?.let { manganese = it } != null
            AppleDietaryChromium -> value.toMass(unit)?.let { chromium = it } != null
            AppleDietaryMolybdenum -> value.toMass(unit)?.let { molybdenum = it } != null
            AppleDietaryPotassium -> value.toMass(unit)?.let { potassium = it } != null
            AppleDietaryVitaminA -> value.toMass(unit)?.let { vitaminA = it } != null
            AppleDietaryVitaminB6 -> value.toMass(unit)?.let { vitaminB6 = it } != null
            AppleDietaryVitaminB12 -> value.toMass(unit)?.let { vitaminB12 = it } != null
            AppleDietaryVitaminC -> value.toMass(unit)?.let { vitaminC = it } != null
            AppleDietaryVitaminD -> value.toMass(unit)?.let { vitaminD = it } != null
            AppleDietaryVitaminE -> value.toMass(unit)?.let { vitaminE = it } != null
            AppleDietaryVitaminK -> value.toMass(unit)?.let { vitaminK = it } != null
            else -> false
        }
        if (applied) hasAny = true
        return applied
    }
}

private fun appleMetadata(record: AppleRecord, targetType: String, sourceFingerprint: String): Metadata =
    appleMetadata(targetType, record.stableClientRecordId(targetType.toStableIdSegment(), sourceFingerprint))

private fun appleMetadata(targetType: String, fingerprint: String): Metadata =
    Metadata.manualEntry(
        device = Device(type = Device.TYPE_PHONE),
        clientRecordId = fingerprint.ifBlank {
            buildStableClientRecordId(targetType.toStableIdSegment(), listOf(targetType, Instant.now().toString()))
        },
    )

private fun buildStableClientRecordId(prefix: String, parts: Any): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(parts.toString().toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(32)
    return "apple_health_${prefix.toStableIdSegment()}_$digest"
}

private fun AppleRecord.stableClientRecordId(prefix: String, extra: Any = stableParts()): String =
    buildStableClientRecordId(prefix, extra)

private val AppleRecord.sourceFingerprint: String
    get() = stableParts()

private fun AppleRecord.stableParts(): String =
    listOf(
        type,
        sourceName.orEmpty(),
        sourceVersion.orEmpty(),
        device.orEmpty(),
        creationDate?.instant?.toString().orEmpty(),
        startDate?.instant?.toString().orEmpty(),
        endDate?.instant?.toString().orEmpty(),
        unit.orEmpty(),
        rawValue.orEmpty(),
        correlationType.orEmpty(),
        metadata.toSortedMap().entries.joinToString(";") { "${it.key}=${it.value}" },
    ).joinToString("|")

private fun AppleWorkout.stableParts(): String =
    listOf(
        workoutActivityType,
        sourceName.orEmpty(),
        sourceVersion.orEmpty(),
        device.orEmpty(),
        creationDate?.instant?.toString().orEmpty(),
        startDate?.instant?.toString().orEmpty(),
        endDate?.instant?.toString().orEmpty(),
        duration?.toString().orEmpty(),
        durationUnit.orEmpty(),
        totalDistance?.toString().orEmpty(),
        totalDistanceUnit.orEmpty(),
        totalEnergyBurned?.toString().orEmpty(),
        totalEnergyBurnedUnit.orEmpty(),
        metadata.toSortedMap().entries.joinToString(";") { "${it.key}=${it.value}" },
    ).joinToString("|")

private fun AppleCorrelation.stableParts(): String =
    listOf(
        type,
        sourceName.orEmpty(),
        sourceVersion.orEmpty(),
        device.orEmpty(),
        creationDate?.instant?.toString().orEmpty(),
        startDate?.instant?.toString().orEmpty(),
        endDate?.instant?.toString().orEmpty(),
        records.joinToString(";") { it.stableParts() },
    ).joinToString("|")

private fun AppleRecord.timeRangeOrNull(): AppleImportTimeRange? {
    val start = startDate?.instant ?: return null
    val end = endDate?.instant ?: start
    return AppleImportTimeRange(start, end)
}

private fun AppleCorrelation.timeRangeOrNull(): AppleImportTimeRange? {
    val start = startDate?.instant ?: records.mapNotNull { it.startDate?.instant }.minOrNull() ?: return null
    val end = endDate?.instant ?: records.mapNotNull { it.endDate?.instant ?: it.startDate?.instant }.maxOrNull() ?: start
    return AppleImportTimeRange(start, end)
}

private fun List<AppleRecord>.hasOverlapping(workout: AppleWorkout, types: Set<String>): Boolean {
    val workoutStart = workout.startDate?.instant ?: return false
    val workoutEnd = workout.endDate?.instant ?: return false
    return any { record ->
        record.type in types &&
            (record.sourceName == null || workout.sourceName == null || record.sourceName == workout.sourceName) &&
            record.startDate?.instant?.isBefore(workoutEnd) == true &&
            (record.endDate?.instant ?: record.startDate.instant).isAfter(workoutStart)
    }
}

private fun String?.toSleepStageType(): Int? =
    when (this) {
        AppleSleepInBed -> SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED
        AppleSleepAsleep,
        AppleSleepAsleepUnspecified,
        -> SleepSessionRecord.STAGE_TYPE_SLEEPING
        AppleSleepAsleepCore -> SleepSessionRecord.STAGE_TYPE_LIGHT
        AppleSleepAsleepDeep -> SleepSessionRecord.STAGE_TYPE_DEEP
        AppleSleepAsleepRem -> SleepSessionRecord.STAGE_TYPE_REM
        AppleSleepAwake -> SleepSessionRecord.STAGE_TYPE_AWAKE
        else -> null
    }

private fun String.toExerciseType(): Int {
    val type = removePrefix("HKWorkoutActivityType").lowercase(Locale.US)
    return when {
        "running" in type -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        "cycling" in type || "biking" in type -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
        "walking" in type -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
        "hiking" in type -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
        "wheelchair" in type -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
        "rowing" in type -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING
        "paddle" in type || "kayak" in type -> ExerciseSessionRecord.EXERCISE_TYPE_PADDLING
        "ski" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
        "snowboard" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING
        "snow" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING
        "skating" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
        "sailing" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SAILING
        "surf" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
        "swim" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
        "golf" in type -> ExerciseSessionRecord.EXERCISE_TYPE_GOLF
        "yoga" in type -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
        "pilates" in type -> ExerciseSessionRecord.EXERCISE_TYPE_PILATES
        "elliptical" in type -> ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
        "strength" in type || "traditionalstrengthtraining" in type -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
        "stair" in type -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING
        else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
    }
}

private fun String?.toMenstrualFlow(): Int =
    when (this) {
        "HKCategoryValueMenstrualFlowLight" -> MenstruationFlowRecord.FLOW_LIGHT
        "HKCategoryValueMenstrualFlowMedium" -> MenstruationFlowRecord.FLOW_MEDIUM
        "HKCategoryValueMenstrualFlowHeavy" -> MenstruationFlowRecord.FLOW_HEAVY
        else -> MenstruationFlowRecord.FLOW_UNKNOWN
    }

private fun String?.toOvulationResult(): Int =
    when (this) {
        "HKCategoryValueOvulationTestResultPositive" -> OvulationTestRecord.RESULT_POSITIVE
        "HKCategoryValueOvulationTestResultNegative" -> OvulationTestRecord.RESULT_NEGATIVE
        "HKCategoryValueOvulationTestResultLuteinizingHormoneSurge" -> OvulationTestRecord.RESULT_HIGH
        else -> OvulationTestRecord.RESULT_INCONCLUSIVE
    }

private fun String?.toCervicalMucusAppearance(): Int =
    when (this) {
        "HKCategoryValueCervicalMucusQualityDry" -> CervicalMucusRecord.APPEARANCE_DRY
        "HKCategoryValueCervicalMucusQualitySticky" -> CervicalMucusRecord.APPEARANCE_STICKY
        "HKCategoryValueCervicalMucusQualityCreamy" -> CervicalMucusRecord.APPEARANCE_CREAMY
        "HKCategoryValueCervicalMucusQualityWatery" -> CervicalMucusRecord.APPEARANCE_WATERY
        "HKCategoryValueCervicalMucusQualityEggWhite" -> CervicalMucusRecord.APPEARANCE_EGG_WHITE
        else -> CervicalMucusRecord.APPEARANCE_UNKNOWN
    }

private fun Map<String, String>.toProtectionUsed(): Int {
    val value = this["HKSexualActivityProtectionUsed"] ?: this["HKMetadataKeySexualActivityProtectionUsed"]
    return when (value?.lowercase(Locale.US)) {
        "true", "1", "yes" -> SexualActivityRecord.PROTECTION_USED_PROTECTED
        "false", "0", "no" -> SexualActivityRecord.PROTECTION_USED_UNPROTECTED
        else -> SexualActivityRecord.PROTECTION_USED_UNKNOWN
    }
}

private fun Double.toMeters(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "m", "meter", "meters" -> this
        "km", "kilometer", "kilometers" -> this * 1_000.0
        "cm", "centimeter", "centimeters" -> this / 100.0
        "mm", "millimeter", "millimeters" -> this / 1_000.0
        "mi", "mile", "miles" -> this * 1_609.344
        "yd", "yard", "yards" -> this * 0.9144
        "ft", "foot", "feet" -> this * 0.3048
        "in", "inch", "inches" -> this * 0.0254
        else -> null
    }

private fun Double.toKilograms(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "kg", "kilogram", "kilograms" -> this
        "g", "gram", "grams" -> this / 1_000.0
        "lb", "lbs", "pound", "pounds" -> this * 0.45359237
        "oz", "ounce", "ounces" -> this * 0.028349523125
        "st", "stone", "stones" -> this * 6.35029318
        else -> null
    }

private fun Double.toKilocalories(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "kcal", "cal", "calorie", "calories", "calories/hour", "calories/hr" -> this
        "kj", "kilojoule", "kilojoules" -> this / 4.184
        "j", "joule", "joules" -> this / 4_184.0
        else -> null
    }

private fun Double.toMilliliters(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "ml", "milliliter", "milliliters" -> this
        "l", "liter", "liters" -> this * 1_000.0
        "fl_oz_us", "floz", "fl oz", "oz" -> this * 29.5735295625
        else -> null
    }

private fun Double.toPercentage(unit: String?): Double? =
    when (unit) {
        "%" -> if (this <= 1.0) this * 100.0 else this
        else -> null
    }

private fun Double.toCelsius(unit: String?): Double? =
    when (unit) {
        "degC", "\u00B0C" -> this
        "degF", "\u00B0F" -> (this - 32.0) * 5.0 / 9.0
        else -> null
    }

private fun Double.toMass(unit: String?): Mass? =
    when (unit?.lowercase(Locale.US)) {
        "kg", "kilogram", "kilograms" -> Mass.kilograms(this)
        "g", "gram", "grams" -> Mass.grams(this)
        "mg", "milligram", "milligrams" -> Mass.milligrams(this)
        "mcg", "ug", "\u00B5g", "microgram", "micrograms" -> Mass.micrograms(this)
        "oz", "ounce", "ounces" -> Mass.ounces(this)
        "lb", "lbs", "pound", "pounds" -> Mass.pounds(this)
        else -> null
    }

private fun Double.toBloodGlucose(unit: String?): BloodGlucose? =
    when (unit?.lowercase(Locale.US)) {
        "mg/dl", "mg/dL".lowercase(Locale.US) -> BloodGlucose.milligramsPerDeciliter(this)
        "mmol/l", "mmol/L".lowercase(Locale.US) -> BloodGlucose.millimolesPerLiter(this)
        else -> null
    }

private fun String.toStableIdSegment(): String =
    lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "record" }

private val SleepSessionGap: Duration = Duration.ofHours(2)

private const val AppleStepCount = "HKQuantityTypeIdentifierStepCount"
private const val AppleDistanceWalkingRunning = "HKQuantityTypeIdentifierDistanceWalkingRunning"
private const val AppleDistanceCycling = "HKQuantityTypeIdentifierDistanceCycling"
private const val AppleDistanceSwimming = "HKQuantityTypeIdentifierDistanceSwimming"
private const val AppleDistanceWheelchair = "HKQuantityTypeIdentifierDistanceWheelchair"
private const val AppleActiveEnergyBurned = "HKQuantityTypeIdentifierActiveEnergyBurned"
private const val AppleBasalEnergyBurned = "HKQuantityTypeIdentifierBasalEnergyBurned"
private const val AppleFlightsClimbed = "HKQuantityTypeIdentifierFlightsClimbed"
private const val AppleElevationAscended = "HKQuantityTypeIdentifierElevationAscended"
private const val ApplePushCount = "HKQuantityTypeIdentifierPushCount"
private const val AppleHeartRate = "HKQuantityTypeIdentifierHeartRate"
private const val AppleRestingHeartRate = "HKQuantityTypeIdentifierRestingHeartRate"
private const val AppleHeartRateVariabilitySdnn = "HKQuantityTypeIdentifierHeartRateVariabilitySDNN"
private const val AppleBodyMass = "HKQuantityTypeIdentifierBodyMass"
private const val AppleHeight = "HKQuantityTypeIdentifierHeight"
private const val AppleBodyFatPercentage = "HKQuantityTypeIdentifierBodyFatPercentage"
private const val AppleLeanBodyMass = "HKQuantityTypeIdentifierLeanBodyMass"
private const val AppleBoneMass = "HKQuantityTypeIdentifierBoneMass"
private const val AppleBodyWaterMass = "HKQuantityTypeIdentifierBodyWaterMass"
private const val AppleDietaryWater = "HKQuantityTypeIdentifierDietaryWater"
private const val AppleOxygenSaturation = "HKQuantityTypeIdentifierOxygenSaturation"
private const val AppleRespiratoryRate = "HKQuantityTypeIdentifierRespiratoryRate"
private const val AppleBodyTemperature = "HKQuantityTypeIdentifierBodyTemperature"
private const val AppleBloodGlucose = "HKQuantityTypeIdentifierBloodGlucose"
private const val AppleVo2Max = "HKQuantityTypeIdentifierVO2Max"
private const val AppleBasalBodyTemperature = "HKQuantityTypeIdentifierBasalBodyTemperature"
private const val AppleBloodPressureSystolic = "HKQuantityTypeIdentifierBloodPressureSystolic"
private const val AppleBloodPressureDiastolic = "HKQuantityTypeIdentifierBloodPressureDiastolic"
private const val AppleBloodPressureCorrelation = "HKCorrelationTypeIdentifierBloodPressure"
private const val AppleSleepAnalysis = "HKCategoryTypeIdentifierSleepAnalysis"
private const val AppleSleepInBed = "HKCategoryValueSleepAnalysisInBed"
private const val AppleSleepAsleep = "HKCategoryValueSleepAnalysisAsleep"
private const val AppleSleepAsleepUnspecified = "HKCategoryValueSleepAnalysisAsleepUnspecified"
private const val AppleSleepAsleepCore = "HKCategoryValueSleepAnalysisAsleepCore"
private const val AppleSleepAsleepDeep = "HKCategoryValueSleepAnalysisAsleepDeep"
private const val AppleSleepAsleepRem = "HKCategoryValueSleepAnalysisAsleepREM"
private const val AppleSleepAwake = "HKCategoryValueSleepAnalysisAwake"
private const val AppleMindfulSession = "HKCategoryTypeIdentifierMindfulSession"
private const val AppleMenstrualFlow = "HKCategoryTypeIdentifierMenstrualFlow"
private const val AppleOvulationTest = "HKCategoryTypeIdentifierOvulationTestResult"
private const val AppleCervicalMucus = "HKCategoryTypeIdentifierCervicalMucusQuality"
private const val AppleIntermenstrualBleeding = "HKCategoryTypeIdentifierIntermenstrualBleeding"
private const val AppleSexualActivity = "HKCategoryTypeIdentifierSexualActivity"

private const val AppleDietaryEnergyConsumed = "HKQuantityTypeIdentifierDietaryEnergyConsumed"
private const val AppleDietaryFatTotal = "HKQuantityTypeIdentifierDietaryFatTotal"
private const val AppleDietaryFatSaturated = "HKQuantityTypeIdentifierDietaryFatSaturated"
private const val AppleDietaryFatTrans = "HKQuantityTypeIdentifierDietaryFatTrans"
private const val AppleDietaryFatMonounsaturated = "HKQuantityTypeIdentifierDietaryFatMonounsaturated"
private const val AppleDietaryFatPolyunsaturated = "HKQuantityTypeIdentifierDietaryFatPolyunsaturated"
private const val AppleDietaryCholesterol = "HKQuantityTypeIdentifierDietaryCholesterol"
private const val AppleDietarySodium = "HKQuantityTypeIdentifierDietarySodium"
private const val AppleDietaryCarbohydrates = "HKQuantityTypeIdentifierDietaryCarbohydrates"
private const val AppleDietaryFiber = "HKQuantityTypeIdentifierDietaryFiber"
private const val AppleDietarySugar = "HKQuantityTypeIdentifierDietarySugar"
private const val AppleDietaryProtein = "HKQuantityTypeIdentifierDietaryProtein"
private const val AppleDietaryCaffeine = "HKQuantityTypeIdentifierDietaryCaffeine"
private const val AppleDietaryCalcium = "HKQuantityTypeIdentifierDietaryCalcium"
private const val AppleDietaryIron = "HKQuantityTypeIdentifierDietaryIron"
private const val AppleDietaryThiamin = "HKQuantityTypeIdentifierDietaryThiamin"
private const val AppleDietaryRiboflavin = "HKQuantityTypeIdentifierDietaryRiboflavin"
private const val AppleDietaryNiacin = "HKQuantityTypeIdentifierDietaryNiacin"
private const val AppleDietaryFolate = "HKQuantityTypeIdentifierDietaryFolate"
private const val AppleDietaryBiotin = "HKQuantityTypeIdentifierDietaryBiotin"
private const val AppleDietaryPantothenicAcid = "HKQuantityTypeIdentifierDietaryPantothenicAcid"
private const val AppleDietaryPhosphorus = "HKQuantityTypeIdentifierDietaryPhosphorus"
private const val AppleDietaryIodine = "HKQuantityTypeIdentifierDietaryIodine"
private const val AppleDietaryMagnesium = "HKQuantityTypeIdentifierDietaryMagnesium"
private const val AppleDietaryZinc = "HKQuantityTypeIdentifierDietaryZinc"
private const val AppleDietarySelenium = "HKQuantityTypeIdentifierDietarySelenium"
private const val AppleDietaryCopper = "HKQuantityTypeIdentifierDietaryCopper"
private const val AppleDietaryManganese = "HKQuantityTypeIdentifierDietaryManganese"
private const val AppleDietaryChromium = "HKQuantityTypeIdentifierDietaryChromium"
private const val AppleDietaryMolybdenum = "HKQuantityTypeIdentifierDietaryMolybdenum"
private const val AppleDietaryPotassium = "HKQuantityTypeIdentifierDietaryPotassium"
private const val AppleDietaryVitaminA = "HKQuantityTypeIdentifierDietaryVitaminA"
private const val AppleDietaryVitaminB6 = "HKQuantityTypeIdentifierDietaryVitaminB6"
private const val AppleDietaryVitaminB12 = "HKQuantityTypeIdentifierDietaryVitaminB12"
private const val AppleDietaryVitaminC = "HKQuantityTypeIdentifierDietaryVitaminC"
private const val AppleDietaryVitaminD = "HKQuantityTypeIdentifierDietaryVitaminD"
private const val AppleDietaryVitaminE = "HKQuantityTypeIdentifierDietaryVitaminE"
private const val AppleDietaryVitaminK = "HKQuantityTypeIdentifierDietaryVitaminK"
private const val AppleNutritionSyntheticType = "AppleHealthNutritionGroup"

private val AppleDistanceTypes =
    setOf(
        AppleDistanceWalkingRunning,
        AppleDistanceCycling,
        AppleDistanceSwimming,
        AppleDistanceWheelchair,
    )

private val AppleNutritionTypes =
    setOf(
        AppleDietaryEnergyConsumed,
        AppleDietaryFatTotal,
        AppleDietaryFatSaturated,
        AppleDietaryFatTrans,
        AppleDietaryFatMonounsaturated,
        AppleDietaryFatPolyunsaturated,
        AppleDietaryCholesterol,
        AppleDietarySodium,
        AppleDietaryCarbohydrates,
        AppleDietaryFiber,
        AppleDietarySugar,
        AppleDietaryProtein,
        AppleDietaryCaffeine,
        AppleDietaryCalcium,
        AppleDietaryIron,
        AppleDietaryThiamin,
        AppleDietaryRiboflavin,
        AppleDietaryNiacin,
        AppleDietaryFolate,
        AppleDietaryBiotin,
        AppleDietaryPantothenicAcid,
        AppleDietaryPhosphorus,
        AppleDietaryIodine,
        AppleDietaryMagnesium,
        AppleDietaryZinc,
        AppleDietarySelenium,
        AppleDietaryCopper,
        AppleDietaryManganese,
        AppleDietaryChromium,
        AppleDietaryMolybdenum,
        AppleDietaryPotassium,
        AppleDietaryVitaminA,
        AppleDietaryVitaminB6,
        AppleDietaryVitaminB12,
        AppleDietaryVitaminC,
        AppleDietaryVitaminD,
        AppleDietaryVitaminE,
        AppleDietaryVitaminK,
    )

private val AppleCycleCategoryTypes =
    setOf(
        AppleMenstrualFlow,
        AppleOvulationTest,
        AppleCervicalMucus,
        AppleIntermenstrualBleeding,
        AppleSexualActivity,
    )
