package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.meters
import java.time.Duration
import kotlin.math.roundToLong

internal fun AppleHealthImportConverter.convertSingleRecord(record: AppleRecord): ConvertedAppleRecord? {
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
