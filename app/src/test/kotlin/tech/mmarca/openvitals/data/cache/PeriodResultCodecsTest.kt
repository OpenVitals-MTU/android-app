package tech.mmarca.openvitals.data.cache

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.data.repository.ActivitiesPeriodData
import tech.mmarca.openvitals.data.repository.ActivityPeriodData
import tech.mmarca.openvitals.data.repository.BodyPeriodData
import tech.mmarca.openvitals.data.repository.CyclePeriodData
import tech.mmarca.openvitals.data.repository.HeartPeriodData
import tech.mmarca.openvitals.data.repository.HydrationPeriodData
import tech.mmarca.openvitals.data.repository.MindfulnessPeriodData
import tech.mmarca.openvitals.data.repository.NutritionPeriodData
import tech.mmarca.openvitals.data.repository.SleepPeriodData
import tech.mmarca.openvitals.data.repository.VitalsPeriodData
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.BodyWaterMassEntry
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BoneMassEntry
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.LeanBodyMassEntry
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.WeightEntry

class PeriodResultCodecsTest {
    private val date = LocalDate.of(2026, 6, 23)
    private val start = Instant.parse("2026-06-23T06:00:00Z")
    private val end = Instant.parse("2026-06-23T07:00:00Z")

    @Test
    fun `activity period codec round trips empty and populated data`() {
        assertRoundTrip(ActivityPeriodData(), ActivityPeriodDataCodec::encode, ActivityPeriodDataCodec::decode)

        val data = ActivityPeriodData(
            dailySteps = listOf(
                DailySteps(
                    date = date,
                    steps = 1234,
                    distanceMeters = 880.5,
                    wheelchairPushes = null,
                    floorsClimbed = 4,
                    activeCaloriesKcal = 90.0,
                    elevationGainedMeters = null,
                ),
            ),
            nutrition = listOf(DailyNutrition(date, hydrationLiters = 1.2, caloriesBurnedKcal = 2200.0)),
            activityProgress = listOf(
                ActivityProgressPoint(
                    time = start,
                    totalSteps = 800,
                    totalDistanceMeters = null,
                    totalCaloriesBurnedKcal = 120.0,
                    totalActiveCaloriesKcal = null,
                ),
            ),
        )

        assertRoundTrip(data, ActivityPeriodDataCodec::encode, ActivityPeriodDataCodec::decode)
    }

    @Test
    fun `activities period codec round trips planned and completed workouts`() {
        assertRoundTrip(ActivitiesPeriodData(), ActivitiesPeriodDataCodec::encode, ActivitiesPeriodDataCodec::decode)

        val data = ActivitiesPeriodData(
            workouts = listOf(
                ExerciseData(
                    id = "workout-1",
                    title = null,
                    exerciseType = 1,
                    startTime = start,
                    endTime = end,
                    durationMs = 3_600_000,
                    source = "test",
                    totalCaloriesKcal = 250.0,
                    steps = 4000,
                    totalCaloriesSource = CaloriesBurnedSource.RECORDED_TOTAL,
                ),
            ),
            plannedWorkouts = listOf(
                PlannedExerciseData(
                    id = "plan-1",
                    title = "Intervals",
                    exerciseType = 1,
                    startTime = start,
                    endTime = end,
                    hasExplicitTime = true,
                    completedExerciseSessionId = null,
                    notes = null,
                    blockCount = 1,
                    source = "test",
                    blocks = listOf(
                        PlannedExerciseBlockData(
                            repetitions = 2,
                            description = null,
                            steps = listOf(
                                PlannedExerciseStepData(
                                    exerciseType = 1,
                                    exercisePhase = 0,
                                    description = "work",
                                    completion = PlannedExerciseCompletion.DurationSeconds(60),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertRoundTrip(data, ActivitiesPeriodDataCodec::encode, ActivitiesPeriodDataCodec::decode)
    }

    @Test
    fun `sleep and heart period codecs round trip nullable fields`() {
        val sleep = SleepPeriodData(
            sessions = listOf(
                SleepData(
                    id = "sleep-1",
                    startTime = start,
                    endTime = end,
                    durationMs = 3_600_000,
                    source = "test",
                    title = null,
                    stages = listOf(SleepStage(start, end, stageType = 1)),
                ),
            ),
        )
        assertRoundTrip(SleepPeriodData(), SleepPeriodDataCodec::encode, SleepPeriodDataCodec::decode)
        assertRoundTrip(sleep, SleepPeriodDataCodec::encode, SleepPeriodDataCodec::decode)

        val heart = HeartPeriodData(
            daySamples = listOf(HeartRateSample(start, beatsPerMinute = 62, source = "test")),
            dailySummaries = listOf(HeartRateSummary(date, avgBpm = 70, minBpm = 55, maxBpm = 125)),
            dayRestingBpm = null,
            previousDayRestingBpm = 58,
            dayHrvMs = 45.5,
            dailyRestingHR = listOf(DailyRestingHR(date, bpm = 57)),
            dailyHrv = listOf(DailyHrv(date, rmssdMs = 52.2)),
        )
        assertRoundTrip(HeartPeriodData(), HeartPeriodDataCodec::encode, HeartPeriodDataCodec::decode)
        assertRoundTrip(heart, HeartPeriodDataCodec::encode, HeartPeriodDataCodec::decode)
    }

    @Test
    fun `body vitals hydration nutrition mindfulness and cycle codecs round trip`() {
        assertRoundTrip(
            BodyPeriodData(
                weightEntries = listOf(WeightEntry(start, weightKg = 70.5, source = "test", id = "w1", isOpenVitalsEntry = true)),
                latestWeightKg = 71.0,
                heightCm = null,
                heightEntries = listOf(HeightEntry(start, heightCm = 180.0, source = "test")),
                bodyFatEntries = listOf(BodyFatEntry(start, percent = 18.2, source = "test")),
                latestBodyFatPercent = 19.0,
                leanMassKg = 57.2,
                leanMassEntries = listOf(LeanBodyMassEntry(start, massKg = 57.2, source = "test")),
                bmrEntries = listOf(BmrEntry(start, kcalPerDay = 1600.0, source = "test")),
                boneMassEntries = listOf(BoneMassEntry(start, massKg = 3.1, source = "test")),
                bodyWaterMassEntries = listOf(BodyWaterMassEntry(start, massKg = 42.0, source = "test")),
            ),
            BodyPeriodDataCodec::encode,
            BodyPeriodDataCodec::decode,
        )
        assertRoundTrip(
            VitalsPeriodData(
                missingVitalsPermissions = setOf("spo2", "temperature"),
                bloodPressure = listOf(BloodPressureEntry(start, systolicMmHg = 120, diastolicMmHg = 80, source = "test")),
                spO2 = listOf(SpO2Entry(start, percent = 98.5, source = "test")),
                bodyTemperature = listOf(BodyTempEntry(start, temperatureCelsius = 36.6, source = "test")),
            ),
            VitalsPeriodDataCodec::encode,
            VitalsPeriodDataCodec::decode,
        )
        assertRoundTrip(
            HydrationPeriodData(
                dailyHydration = listOf(DailyHydration(date, liters = 2.1)),
                hydrationEntries = listOf(HydrationEntry(start, end, liters = 0.25, source = "test", id = "h1", isOpenVitalsEntry = true)),
            ),
            HydrationPeriodDataCodec::encode,
            HydrationPeriodDataCodec::decode,
        )
        assertRoundTrip(
            NutritionPeriodData(
                dailyMacros = listOf(DailyMacros(date, nutrientValues = mapOf(NutritionNutrient.ENERGY to 500.0))),
                entries = listOf(
                    NutritionEntry(
                        time = start,
                        mealType = 1,
                        name = null,
                        energyKcal = 500.0,
                        proteinGrams = null,
                        carbsGrams = 60.0,
                        fatGrams = 18.0,
                        fiberGrams = null,
                        sugarGrams = 12.0,
                        source = "test",
                        nutrientValues = mapOf(NutritionNutrient.SUGAR to 12.0),
                    ),
                ),
            ),
            NutritionPeriodDataCodec::encode,
            NutritionPeriodDataCodec::decode,
        )
        assertRoundTrip(
            MindfulnessPeriodData(
                sessions = listOf(MindfulnessSession("m1", title = null, startTime = start, endTime = end, durationMs = 3_600_000, source = "test")),
            ),
            MindfulnessPeriodDataCodec::encode,
            MindfulnessPeriodDataCodec::decode,
        )
        assertRoundTrip(
            CyclePeriodData(data = CycleData(), missingPermissions = setOf("cycle")),
            CyclePeriodDataCodec::encode,
            CyclePeriodDataCodec::decode,
        )
    }

    private fun <T> assertRoundTrip(
        value: T,
        encode: (T) -> String,
        decode: (String) -> T,
    ) {
        assertEquals(value, decode(encode(value)))
    }
}
