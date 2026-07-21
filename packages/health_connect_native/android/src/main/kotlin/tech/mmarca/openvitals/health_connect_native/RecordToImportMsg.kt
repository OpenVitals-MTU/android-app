package tech.mmarca.openvitals.health_connect_native

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
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneOffset

/**
 * The exact reverse of [ImportRecordsBuilder]: converts an AndroidX Health
 * Connect [Record] read from the device into a typed [ImportRecordMsg] using the
 * SAME field-key conventions, so a record round-trips write → read → write
 * losslessly. Powers the plugin's generic `readImportRecords`.
 *
 * [convert] returns null for record types with no import representation (only
 * PlannedExerciseSession today), which the caller filters out.
 */
internal object RecordToImportMsg {

  fun convert(record: Record): ImportRecordMsg? = when (record) {
    // ── Activity (interval) ────────────────────────────────────────────────
    is StepsRecord -> interval(record, "Steps", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      doubles = mapOf("count" to record.count.toDouble()))
    is DistanceRecord -> interval(record, "Distance", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      doubles = mapOf("distanceMeters" to record.distance.inMeters))
    is ActiveCaloriesBurnedRecord -> interval(record, "ActiveCaloriesBurned", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      doubles = mapOf("energyKcal" to record.energy.inKilocalories))
    is TotalCaloriesBurnedRecord -> interval(record, "TotalCaloriesBurned", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      doubles = mapOf("energyKcal" to record.energy.inKilocalories))
    is FloorsClimbedRecord -> interval(record, "FloorsClimbed", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      doubles = mapOf("floors" to record.floors))
    is ElevationGainedRecord -> interval(record, "ElevationGained", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      doubles = mapOf("elevationMeters" to record.elevation.inMeters))
    is WheelchairPushesRecord -> interval(record, "WheelchairPushes", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      doubles = mapOf("count" to record.count.toDouble()))

    // ── Series (interval) ──────────────────────────────────────────────────
    is SpeedRecord -> interval(record, "Speed", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      samples = record.samples.map { ImportSampleMsg(it.time.toEpochMilli(), it.speed.inMetersPerSecond) })
    is StepsCadenceRecord -> interval(record, "StepsCadence", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      samples = record.samples.map { ImportSampleMsg(it.time.toEpochMilli(), it.rate) })
    is CyclingPedalingCadenceRecord -> interval(record, "CyclingPedalingCadence", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      samples = record.samples.map { ImportSampleMsg(it.time.toEpochMilli(), it.revolutionsPerMinute) })
    is PowerRecord -> interval(record, "Power", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      samples = record.samples.map { ImportSampleMsg(it.time.toEpochMilli(), it.power.inWatts) })
    is HeartRateRecord -> interval(record, "HeartRate", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      samples = record.samples.map { ImportSampleMsg(it.time.toEpochMilli(), it.beatsPerMinute.toDouble()) })

    // ── Heart (instant) ────────────────────────────────────────────────────
    is RestingHeartRateRecord -> instant(record, "RestingHeartRate", record.time, record.zoneOffset,
      doubles = mapOf("bpm" to record.beatsPerMinute.toDouble()))
    is HeartRateVariabilityRmssdRecord -> instant(record, "HeartRateVariabilityRmssd", record.time, record.zoneOffset,
      doubles = mapOf("rmssdMillis" to record.heartRateVariabilityMillis))

    // ── Body (instant) ─────────────────────────────────────────────────────
    is WeightRecord -> instant(record, "Weight", record.time, record.zoneOffset, doubles = mapOf("weightKg" to record.weight.inKilograms))
    is HeightRecord -> instant(record, "Height", record.time, record.zoneOffset, doubles = mapOf("heightMeters" to record.height.inMeters))
    is BodyFatRecord -> instant(record, "BodyFat", record.time, record.zoneOffset, doubles = mapOf("percentage" to record.percentage.value))
    is LeanBodyMassRecord -> instant(record, "LeanBodyMass", record.time, record.zoneOffset, doubles = mapOf("massKg" to record.mass.inKilograms))
    is BoneMassRecord -> instant(record, "BoneMass", record.time, record.zoneOffset, doubles = mapOf("massKg" to record.mass.inKilograms))
    is BodyWaterMassRecord -> instant(record, "BodyWaterMass", record.time, record.zoneOffset, doubles = mapOf("massKg" to record.mass.inKilograms))
    is BasalMetabolicRateRecord -> instant(record, "BasalMetabolicRate", record.time, record.zoneOffset,
      doubles = mapOf("kcalPerDay" to record.basalMetabolicRate.inKilocaloriesPerDay))

    // ── Hydration / Nutrition (interval) ───────────────────────────────────
    is HydrationRecord -> interval(record, "Hydration", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      doubles = mapOf("volumeLiters" to record.volume.inLiters))
    is NutritionRecord -> nutrition(record)

    // ── Vitals (instant, unless noted) ─────────────────────────────────────
    is OxygenSaturationRecord -> instant(record, "OxygenSaturation", record.time, record.zoneOffset, doubles = mapOf("percentage" to record.percentage.value))
    is RespiratoryRateRecord -> instant(record, "RespiratoryRate", record.time, record.zoneOffset, doubles = mapOf("rate" to record.rate))
    is BodyTemperatureRecord -> instant(record, "BodyTemperature", record.time, record.zoneOffset, doubles = mapOf("temperatureCelsius" to record.temperature.inCelsius))
    is BasalBodyTemperatureRecord -> instant(record, "BasalBodyTemperature", record.time, record.zoneOffset, doubles = mapOf("temperatureCelsius" to record.temperature.inCelsius))
    is BloodGlucoseRecord -> instant(record, "BloodGlucose", record.time, record.zoneOffset, doubles = mapOf("levelMmolL" to record.level.inMillimolesPerLiter))
    is Vo2MaxRecord -> instant(record, "Vo2Max", record.time, record.zoneOffset, doubles = mapOf("vo2MillilitersPerMinuteKilogram" to record.vo2MillilitersPerMinuteKilogram))
    is BloodPressureRecord -> instant(record, "BloodPressure", record.time, record.zoneOffset,
      doubles = mapOf("systolicMmHg" to record.systolic.inMillimetersOfMercury, "diastolicMmHg" to record.diastolic.inMillimetersOfMercury))
    is SkinTemperatureRecord -> interval(record, "SkinTemperature", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      doubles = record.baseline?.let { mapOf("baselineCelsius" to it.inCelsius) } ?: emptyMap(),
      ints = mapOf("measurementLocation" to record.measurementLocation.toLong()),
      samples = record.deltas.map { ImportSampleMsg(it.time.toEpochMilli(), it.delta.inCelsius) })

    // ── Sleep / Mindfulness (interval) ─────────────────────────────────────
    is SleepSessionRecord -> interval(record, "Sleep", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      name = record.title, notes = record.notes,
      sleepStages = record.stages.map { ImportSleepStageMsg(it.startTime.toEpochMilli(), it.endTime.toEpochMilli(), it.stage.toLong()) })
    is MindfulnessSessionRecord -> interval(record, "MindfulnessSession", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset,
      name = record.title, notes = record.notes)

    // ── Cycle ──────────────────────────────────────────────────────────────
    is MenstruationFlowRecord -> instant(record, "MenstruationFlow", record.time, record.zoneOffset, ints = mapOf("flow" to record.flow.toLong()))
    is MenstruationPeriodRecord -> interval(record, "MenstruationPeriod", record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset)
    is OvulationTestRecord -> instant(record, "OvulationTest", record.time, record.zoneOffset, ints = mapOf("result" to record.result.toLong()))
    is CervicalMucusRecord -> instant(record, "CervicalMucus", record.time, record.zoneOffset,
      ints = mapOf("appearance" to record.appearance.toLong(), "sensation" to record.sensation.toLong()))
    is IntermenstrualBleedingRecord -> instant(record, "IntermenstrualBleeding", record.time, record.zoneOffset)
    is SexualActivityRecord -> instant(record, "SexualActivity", record.time, record.zoneOffset, ints = mapOf("protectionUsed" to record.protectionUsed.toLong()))

    // ── Exercise (interval) ────────────────────────────────────────────────
    is ExerciseSessionRecord -> exercise(record)

    else -> null // PlannedExerciseSession (structured plan) — not yet imported.
  }

  private fun instant(
    record: Record,
    type: String,
    time: Instant,
    zone: ZoneOffset?,
    doubles: Map<String, Double> = emptyMap(),
    ints: Map<String, Long> = emptyMap(),
  ) = base(record, type, time.toEpochMilli(), null, zone, null, doubles = doubles, ints = ints)

  private fun interval(
    record: Record,
    type: String,
    start: Instant,
    sZone: ZoneOffset?,
    end: Instant,
    eZone: ZoneOffset?,
    doubles: Map<String, Double> = emptyMap(),
    ints: Map<String, Long> = emptyMap(),
    name: String? = null,
    notes: String? = null,
    samples: List<ImportSampleMsg> = emptyList(),
    sleepStages: List<ImportSleepStageMsg> = emptyList(),
  ) = base(record, type, start.toEpochMilli(), end.toEpochMilli(), sZone, eZone,
    doubles = doubles, ints = ints, name = name, notes = notes, samples = samples, sleepStages = sleepStages)

  private fun base(
    record: Record,
    type: String,
    startMs: Long,
    endMs: Long?,
    sZone: ZoneOffset?,
    eZone: ZoneOffset?,
    doubles: Map<String, Double> = emptyMap(),
    ints: Map<String, Long> = emptyMap(),
    name: String? = null,
    notes: String? = null,
    samples: List<ImportSampleMsg> = emptyList(),
    sleepStages: List<ImportSleepStageMsg> = emptyList(),
    routePoints: List<ExerciseRoutePointMsg> = emptyList(),
    segments: List<ExerciseSegmentMsg> = emptyList(),
    laps: List<ExerciseLapMsg> = emptyList(),
    plannedId: String? = null,
  ) = ImportRecordMsg(
    recordType = type,
    clientRecordId = record.metadata.clientRecordId ?: "",
    startEpochMs = startMs,
    endEpochMs = endMs,
    startZoneOffsetSeconds = sZone?.totalSeconds?.toLong(),
    endZoneOffsetSeconds = eZone?.totalSeconds?.toLong(),
    doubleFields = doubles,
    intFields = ints,
    name = name,
    samples = samples,
    sleepStages = sleepStages,
    routePoints = routePoints,
    dataOriginPackage = record.metadata.dataOrigin.packageName,
    notes = notes,
    segments = segments,
    laps = laps,
    plannedExerciseId = plannedId,
    plannedBlocks = emptyList(),
  )

  private fun exercise(r: ExerciseSessionRecord): ImportRecordMsg {
    val route = (r.exerciseRouteResult as? ExerciseRouteResult.Data)?.exerciseRoute
    val points = route?.route?.map {
      ExerciseRoutePointMsg(
        timeEpochMs = it.time.toEpochMilli(),
        latitude = it.latitude,
        longitude = it.longitude,
        altitudeMeters = it.altitude?.inMeters,
        horizontalAccuracyMeters = it.horizontalAccuracy?.inMeters,
        verticalAccuracyMeters = it.verticalAccuracy?.inMeters,
      )
    } ?: emptyList()
    return ImportRecordMsg(
      recordType = "ExerciseSession",
      clientRecordId = r.metadata.clientRecordId ?: "",
      startEpochMs = r.startTime.toEpochMilli(),
      endEpochMs = r.endTime.toEpochMilli(),
      startZoneOffsetSeconds = r.startZoneOffset?.totalSeconds?.toLong(),
      endZoneOffsetSeconds = r.endZoneOffset?.totalSeconds?.toLong(),
      doubleFields = emptyMap(),
      intFields = mapOf("exerciseType" to r.exerciseType.toLong()),
      name = r.title,
      samples = emptyList(),
      sleepStages = emptyList(),
      routePoints = points,
      dataOriginPackage = r.metadata.dataOrigin.packageName,
      notes = r.notes,
      segments = r.segments.map {
        ExerciseSegmentMsg(it.startTime.toEpochMilli(), it.endTime.toEpochMilli(), it.segmentType.toLong(), it.repetitions.toLong(), null)
      },
      laps = r.laps.map {
        ExerciseLapMsg(it.startTime.toEpochMilli(), it.endTime.toEpochMilli(), it.length?.inMeters)
      },
      plannedExerciseId = r.plannedExerciseSessionId,
      plannedBlocks = emptyList(),
    )
  }

  private fun nutrition(r: NutritionRecord): ImportRecordMsg {
    val doubles = LinkedHashMap<String, Double>()
    r.energy?.let { doubles["energyKcal"] = it.inKilocalories }
    fun g(key: String, mass: Mass?) { mass?.let { doubles[key] = it.inGrams } }
    g("biotin", r.biotin); g("caffeine", r.caffeine); g("calcium", r.calcium)
    g("chloride", r.chloride); g("cholesterol", r.cholesterol); g("chromium", r.chromium)
    g("copper", r.copper); g("dietaryFiber", r.dietaryFiber); g("folate", r.folate)
    g("folicAcid", r.folicAcid); g("iodine", r.iodine); g("iron", r.iron)
    g("magnesium", r.magnesium); g("manganese", r.manganese); g("molybdenum", r.molybdenum)
    g("monounsaturatedFat", r.monounsaturatedFat); g("niacin", r.niacin)
    g("pantothenicAcid", r.pantothenicAcid); g("phosphorus", r.phosphorus)
    g("polyunsaturatedFat", r.polyunsaturatedFat); g("potassium", r.potassium)
    g("protein", r.protein); g("riboflavin", r.riboflavin); g("saturatedFat", r.saturatedFat)
    g("selenium", r.selenium); g("sodium", r.sodium); g("sugar", r.sugar)
    g("thiamin", r.thiamin); g("totalCarbohydrate", r.totalCarbohydrate)
    g("totalFat", r.totalFat); g("transFat", r.transFat); g("unsaturatedFat", r.unsaturatedFat)
    g("vitaminA", r.vitaminA); g("vitaminB12", r.vitaminB12); g("vitaminB6", r.vitaminB6)
    g("vitaminC", r.vitaminC); g("vitaminD", r.vitaminD); g("vitaminE", r.vitaminE)
    g("vitaminK", r.vitaminK); g("zinc", r.zinc)
    return base(r, "Nutrition", r.startTime.toEpochMilli(), r.endTime.toEpochMilli(),
      r.startZoneOffset, r.endZoneOffset, doubles = doubles, name = r.name)
  }
}
