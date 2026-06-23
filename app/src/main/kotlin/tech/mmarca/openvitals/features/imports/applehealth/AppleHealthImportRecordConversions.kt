package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Volume
import androidx.health.connect.client.units.celsius
import androidx.health.connect.client.units.kilograms
import androidx.health.connect.client.units.meters
import androidx.health.connect.client.units.percent

internal fun AppleHealthImportConverter.convertWeight(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertHeight(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertBodyFat(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertLeanMass(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertBoneMass(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertBodyWaterMass(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertHydration(
    record: AppleRecord,
    interval: AppleInterval,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertOxygenSaturation(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertRespiratoryRate(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertBodyTemperature(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertBloodGlucose(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertVo2Max(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertBasalBodyTemperature(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertMindfulness(
    record: AppleRecord,
    interval: AppleInterval,
    metadata: Metadata,
): ConvertedAppleRecord? {
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

internal fun AppleHealthImportConverter.convertCycleCategory(
    record: AppleRecord,
    start: AppleDateTime,
    metadata: Metadata,
): ConvertedAppleRecord? {
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
