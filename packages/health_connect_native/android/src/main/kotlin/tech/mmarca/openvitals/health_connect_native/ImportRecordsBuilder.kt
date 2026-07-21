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
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
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
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.TemperatureDelta
import androidx.health.connect.client.units.Velocity
import androidx.health.connect.client.units.Volume
import java.time.Instant
import java.time.ZoneOffset

/**
 * Builds Health Connect records from typed [ImportRecordMsg]s (Apple Health
 * import). Replaces the JSON import serializer + `jsonToRecord`. Records are
 * created as manual entries carrying the `apple_health_*` clientRecordId for
 * idempotent re-import dedup.
 */
internal object ImportRecordsBuilder {
  fun build(msg: ImportRecordMsg): Record {
    val start = Instant.ofEpochMilli(msg.startEpochMs)
    val end = msg.endEpochMs?.let { Instant.ofEpochMilli(it) }
    val sZone = msg.startZoneOffsetSeconds?.let { ZoneOffset.ofTotalSeconds(it.toInt()) }
    val eZone = msg.endZoneOffsetSeconds?.let { ZoneOffset.ofTotalSeconds(it.toInt()) }
    val meta = Metadata.manualEntry(clientRecordId = msg.clientRecordId)
    fun d(key: String): Double? = msg.doubleFields[key]
    fun req(key: String): Double = msg.doubleFields[key]
      ?: throw IllegalArgumentException("import ${msg.recordType} missing $key")
    fun i(key: String): Int? = msg.intFields[key]?.toInt()

    return when (msg.recordType) {
      "Steps" -> StepsRecord(start, sZone, end!!, eZone, req("count").toLong(), meta)
      "Distance" -> DistanceRecord(start, sZone, end!!, eZone, Length.meters(req("distanceMeters")), meta)
      "ActiveCaloriesBurned" ->
        ActiveCaloriesBurnedRecord(start, sZone, end!!, eZone, Energy.kilocalories(req("energyKcal")), meta)
      "TotalCaloriesBurned" ->
        TotalCaloriesBurnedRecord(start, sZone, end!!, eZone, Energy.kilocalories(req("energyKcal")), meta)
      "StepsCadence" -> StepsCadenceRecord(
        startTime = start,
        startZoneOffset = sZone,
        endTime = end!!,
        endZoneOffset = eZone,
        samples = msg.samples.map {
          StepsCadenceRecord.Sample(Instant.ofEpochMilli(it.timeEpochMs), it.value)
        },
        metadata = meta,
      )
      "CyclingPedalingCadence" -> CyclingPedalingCadenceRecord(
        startTime = start,
        startZoneOffset = sZone,
        endTime = end!!,
        endZoneOffset = eZone,
        samples = msg.samples.map {
          CyclingPedalingCadenceRecord.Sample(Instant.ofEpochMilli(it.timeEpochMs), it.value)
        },
        metadata = meta,
      )
      "Power" -> PowerRecord(
        startTime = start,
        startZoneOffset = sZone,
        endTime = end!!,
        endZoneOffset = eZone,
        samples = msg.samples.map {
          PowerRecord.Sample(Instant.ofEpochMilli(it.timeEpochMs), Power.watts(it.value))
        },
        metadata = meta,
      )
      "BasalMetabolicRate" -> BasalMetabolicRateRecord(
        time = start,
        zoneOffset = sZone,
        basalMetabolicRate = Power.kilocaloriesPerDay(req("kcalPerDay")),
        metadata = meta,
      )
      "FloorsClimbed" -> FloorsClimbedRecord(start, sZone, end!!, eZone, req("floors"), meta)
      "ElevationGained" ->
        ElevationGainedRecord(start, sZone, end!!, eZone, Length.meters(req("elevationMeters")), meta)
      "WheelchairPushes" -> WheelchairPushesRecord(start, sZone, end!!, eZone, req("count").toLong(), meta)
      "Speed" -> SpeedRecord(
        startTime = start,
        startZoneOffset = sZone,
        endTime = end!!,
        endZoneOffset = eZone,
        samples = msg.samples.map {
          SpeedRecord.Sample(Instant.ofEpochMilli(it.timeEpochMs), Velocity.metersPerSecond(it.value))
        },
        metadata = meta,
      )
      "HeartRate" -> HeartRateRecord(
        startTime = start,
        startZoneOffset = sZone,
        endTime = end!!,
        endZoneOffset = eZone,
        samples = msg.samples.map {
          HeartRateRecord.Sample(Instant.ofEpochMilli(it.timeEpochMs), it.value.toLong())
        },
        metadata = meta,
      )
      "RestingHeartRate" -> RestingHeartRateRecord(start, sZone, req("bpm").toLong(), meta)
      "HeartRateVariabilityRmssd" -> HeartRateVariabilityRmssdRecord(
        time = start,
        zoneOffset = sZone,
        heartRateVariabilityMillis = req("rmssdMillis"),
        metadata = meta,
      )
      "Weight" -> WeightRecord(start, sZone, Mass.kilograms(req("weightKg")), meta)
      "Height" -> HeightRecord(start, sZone, Length.meters(req("heightMeters")), meta)
      "BodyFat" -> BodyFatRecord(start, sZone, Percentage(req("percentage")), meta)
      "LeanBodyMass" -> LeanBodyMassRecord(start, sZone, Mass.kilograms(req("massKg")), meta)
      "BoneMass" -> BoneMassRecord(start, sZone, Mass.kilograms(req("massKg")), meta)
      "BodyWaterMass" -> BodyWaterMassRecord(start, sZone, Mass.kilograms(req("massKg")), meta)
      "Hydration" -> HydrationRecord(start, sZone, end!!, eZone, Volume.liters(req("volumeLiters")), meta)
      "OxygenSaturation" -> OxygenSaturationRecord(start, sZone, Percentage(req("percentage")), meta)
      "RespiratoryRate" -> RespiratoryRateRecord(start, sZone, req("rate"), meta)
      "BodyTemperature" -> BodyTemperatureRecord(
        time = start,
        zoneOffset = sZone,
        temperature = Temperature.celsius(req("temperatureCelsius")),
        metadata = meta,
      )
      "BasalBodyTemperature" -> BasalBodyTemperatureRecord(
        time = start,
        zoneOffset = sZone,
        temperature = Temperature.celsius(req("temperatureCelsius")),
        metadata = meta,
      )
      "BloodGlucose" -> BloodGlucoseRecord(
        time = start,
        zoneOffset = sZone,
        metadata = meta,
        level = BloodGlucose.millimolesPerLiter(req("levelMmolL")),
      )
      "Vo2Max" -> Vo2MaxRecord(
        time = start,
        zoneOffset = sZone,
        vo2MillilitersPerMinuteKilogram = req("vo2MillilitersPerMinuteKilogram"),
        metadata = meta,
      )
      "BloodPressure" -> BloodPressureRecord(
        time = start,
        zoneOffset = sZone,
        metadata = meta,
        systolic = Pressure.millimetersOfMercury(req("systolicMmHg")),
        diastolic = Pressure.millimetersOfMercury(req("diastolicMmHg")),
      )
      "Sleep" -> SleepSessionRecord(
        startTime = start,
        startZoneOffset = sZone,
        endTime = end!!,
        endZoneOffset = eZone,
        metadata = meta,
        title = msg.name,
        notes = msg.notes,
        stages = msg.sleepStages.map {
          SleepSessionRecord.Stage(
            startTime = Instant.ofEpochMilli(it.startEpochMs),
            endTime = Instant.ofEpochMilli(it.endEpochMs),
            stage = it.stage.toInt(),
          )
        },
      )
      "MindfulnessSession" -> MindfulnessSessionRecord(
        startTime = start,
        startZoneOffset = sZone,
        endTime = end!!,
        endZoneOffset = eZone,
        metadata = meta,
        mindfulnessSessionType = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNKNOWN,
        title = msg.name,
        notes = msg.notes,
      )
      "MenstruationFlow" -> MenstruationFlowRecord(
        time = start,
        zoneOffset = sZone,
        flow = i("flow") ?: MenstruationFlowRecord.FLOW_UNKNOWN,
        metadata = meta,
      )
      "OvulationTest" -> OvulationTestRecord(
        time = start,
        zoneOffset = sZone,
        result = i("result") ?: OvulationTestRecord.RESULT_INCONCLUSIVE,
        metadata = meta,
      )
      "CervicalMucus" -> CervicalMucusRecord(
        time = start,
        zoneOffset = sZone,
        appearance = i("appearance") ?: CervicalMucusRecord.APPEARANCE_UNKNOWN,
        sensation = i("sensation") ?: CervicalMucusRecord.SENSATION_UNKNOWN,
        metadata = meta,
      )
      "MenstruationPeriod" ->
        MenstruationPeriodRecord(start, sZone, end!!, eZone, meta)
      "SkinTemperature" -> SkinTemperatureRecord(
        startTime = start,
        startZoneOffset = sZone,
        endTime = end!!,
        endZoneOffset = eZone,
        deltas = msg.samples.map {
          SkinTemperatureRecord.Delta(
            Instant.ofEpochMilli(it.timeEpochMs),
            TemperatureDelta.celsius(it.value),
          )
        },
        baseline = d("baselineCelsius")?.let { Temperature.celsius(it) },
        measurementLocation = i("measurementLocation")
          ?: SkinTemperatureRecord.MEASUREMENT_LOCATION_UNKNOWN,
        metadata = meta,
      )
      "IntermenstrualBleeding" -> IntermenstrualBleedingRecord(start, sZone, meta)
      "SexualActivity" -> SexualActivityRecord(
        time = start,
        zoneOffset = sZone,
        protectionUsed = i("protectionUsed") ?: SexualActivityRecord.PROTECTION_USED_UNKNOWN,
        metadata = meta,
      )
      "Nutrition" -> buildNutrition(msg, start, sZone, end!!, eZone, meta)
      "ExerciseSession" -> buildExercise(msg, start, sZone, end!!, eZone, meta)
      "PlannedExerciseSession" -> PlannedExerciseSessionRecord(
        startTime = start,
        startZoneOffset = sZone,
        endTime = end!!,
        endZoneOffset = eZone,
        metadata = meta,
        exerciseType = i("exerciseType") ?: ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
        title = msg.name,
        notes = msg.notes,
        blocks = msg.plannedBlocks.map { it.toRecord() },
      )
      else -> throw IllegalArgumentException("Unknown import record type: ${msg.recordType}")
    }
  }

  private fun buildNutrition(
    msg: ImportRecordMsg,
    start: Instant,
    sZone: ZoneOffset?,
    end: Instant,
    eZone: ZoneOffset?,
    meta: Metadata,
  ): NutritionRecord {
    fun grams(key: String): Mass? = msg.doubleFields[key]?.let { Mass.grams(it) }
    return NutritionRecord(
      startTime = start,
      startZoneOffset = sZone,
      endTime = end,
      endZoneOffset = eZone,
      metadata = meta,
      name = msg.name,
      mealType = MealType.MEAL_TYPE_UNKNOWN,
      energy = msg.doubleFields["energyKcal"]?.let { Energy.kilocalories(it) },
      biotin = grams("biotin"),
      caffeine = grams("caffeine"),
      calcium = grams("calcium"),
      chloride = grams("chloride"),
      cholesterol = grams("cholesterol"),
      chromium = grams("chromium"),
      copper = grams("copper"),
      dietaryFiber = grams("dietaryFiber") ?: grams("fiber"),
      folate = grams("folate"),
      folicAcid = grams("folicAcid"),
      iodine = grams("iodine"),
      iron = grams("iron"),
      magnesium = grams("magnesium"),
      manganese = grams("manganese"),
      molybdenum = grams("molybdenum"),
      monounsaturatedFat = grams("monounsaturatedFat"),
      niacin = grams("niacin"),
      pantothenicAcid = grams("pantothenicAcid"),
      phosphorus = grams("phosphorus"),
      polyunsaturatedFat = grams("polyunsaturatedFat"),
      potassium = grams("potassium"),
      protein = grams("protein"),
      riboflavin = grams("riboflavin"),
      saturatedFat = grams("saturatedFat"),
      selenium = grams("selenium"),
      sodium = grams("sodium"),
      sugar = grams("sugar"),
      thiamin = grams("thiamin"),
      totalCarbohydrate = grams("totalCarbohydrate"),
      totalFat = grams("totalFat"),
      transFat = grams("transFat"),
      unsaturatedFat = grams("unsaturatedFat"),
      vitaminA = grams("vitaminA"),
      vitaminB12 = grams("vitaminB12"),
      vitaminB6 = grams("vitaminB6"),
      vitaminC = grams("vitaminC"),
      vitaminD = grams("vitaminD"),
      vitaminE = grams("vitaminE"),
      vitaminK = grams("vitaminK"),
      zinc = grams("zinc"),
    )
  }

  private fun buildExercise(
    msg: ImportRecordMsg,
    start: Instant,
    sZone: ZoneOffset?,
    end: Instant,
    eZone: ZoneOffset?,
    meta: Metadata,
  ): ExerciseSessionRecord {
    val route: ExerciseRoute? = msg.routePoints
      .takeIf { it.isNotEmpty() }
      ?.map { p ->
        ExerciseRoute.Location(
          time = Instant.ofEpochMilli(p.timeEpochMs),
          latitude = p.latitude,
          longitude = p.longitude,
          horizontalAccuracy = p.horizontalAccuracyMeters?.let { Length.meters(it) },
          verticalAccuracy = p.verticalAccuracyMeters?.let { Length.meters(it) },
          altitude = p.altitudeMeters?.let { Length.meters(it) },
        )
      }
      ?.let { ExerciseRoute(it) }
    val segments = msg.segments.map {
      ExerciseSegment(
        startTime = Instant.ofEpochMilli(it.startEpochMs),
        endTime = Instant.ofEpochMilli(it.endEpochMs),
        segmentType = it.segmentType.toInt(),
        repetitions = it.repetitions.toInt(),
      )
    }
    val laps = msg.laps.map {
      ExerciseLap(
        startTime = Instant.ofEpochMilli(it.startEpochMs),
        endTime = Instant.ofEpochMilli(it.endEpochMs),
        length = it.lengthMeters?.let { m -> Length.meters(m) },
      )
    }
    return ExerciseSessionRecord(
      startTime = start,
      startZoneOffset = sZone,
      endTime = end,
      endZoneOffset = eZone,
      metadata = meta,
      exerciseType = (msg.intFields["exerciseType"]?.toInt())
        ?: ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
      title = msg.name,
      notes = msg.notes,
      segments = segments,
      laps = laps,
      exerciseRoute = route,
      plannedExerciseSessionId = msg.plannedExerciseId,
    )
  }
}
