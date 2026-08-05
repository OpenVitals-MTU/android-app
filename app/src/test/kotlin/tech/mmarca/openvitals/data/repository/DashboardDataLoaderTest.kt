package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.WeightRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.dashboard.DashboardDataLoader
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.CaloriesBurnedValue
import tech.mmarca.openvitals.domain.model.DailySleepDuration
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.RestingHeartRateSample
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepReadData
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class DashboardDataLoaderTest {

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val distancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val sleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val exercisePermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val totalCaloriesPermission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    private val activeCaloriesPermission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val bmrPermission = HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
    private val menstruationPermission = HealthPermission.getReadPermission(MenstruationPeriodRecord::class)
    private val weightPermission = HealthPermission.getReadPermission(WeightRecord::class)
    private val heightPermission = HealthPermission.getReadPermission(HeightRecord::class)
    private val heartRatePermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val restingHeartRatePermission = HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    private val hrvPermission = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    private val nutritionPermission = HealthPermission.getReadPermission(NutritionRecord::class)
    private val spO2Permission = HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    private val respiratoryRatePermission = HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    private val skinTemperaturePermission = HealthPermission.getReadPermission(SkinTemperatureRecord::class)
    private val bloodPressurePermission = HealthPermission.getReadPermission(BloodPressureRecord::class)
    private val bodyTemperaturePermission = HealthPermission.getReadPermission(BodyTemperatureRecord::class)
    private val bloodGlucosePermission = HealthPermission.getReadPermission(BloodGlucoseRecord::class)
    private val vo2MaxPermission = HealthPermission.getReadPermission(Vo2MaxRecord::class)

    /** The five vitals measured while asleep, attributed to the night's wake-up day. */
    private val overnightMetrics = setOf(
        DashboardMetric.HRV,
        DashboardMetric.RESTING_HEART_RATE,
        DashboardMetric.SPO2,
        DashboardMetric.RESPIRATORY_RATE,
        DashboardMetric.SKIN_TEMPERATURE,
    )
    private val overnightPermissions = setOf(
        hrvPermission,
        restingHeartRatePermission,
        spO2Permission,
        respiratoryRatePermission,
        skinTemperaturePermission,
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test fun `loadDashboard keeps successful metrics when another metric is rate limited`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(stepsPermission, distancePermission)
        coEvery { hc.grantedPermissions() } returns setOf(stepsPermission, distancePermission)
        coEvery { hc.readSteps(date) } throws RuntimeException(
            "Request rejected. Rate limited request quota has been exceeded.",
        )
        coEvery { hc.readDistanceMeters(date) } returns 1234.0

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.STEPS, DashboardMetric.DISTANCE),
            )
        )

        assertEquals(0L, data.steps)
        assertEquals(1234.0, data.distanceMeters, 0.01)
        assertNull(data.workout)
    }

    @Test fun `average heart rate dashboard metric uses aggregate without raw samples`() = runTest {
        val date = LocalDate.of(2026, 6, 27)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(heartRatePermission)
        coEvery { hc.grantedPermissions() } returns setOf(heartRatePermission)
        coEvery { hc.readAvgHeartRate(date) } returns 72L

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.AVG_HEART_RATE),
            )
        )

        assertEquals(72L, data.avgHeartRateBpm)
        assertEquals(0, data.heartRateSampleCount)
        assertNull(data.heartRateSampleStartTime)
        assertNull(data.heartRateSampleEndTime)
        coVerify(exactly = 1) { hc.readAvgHeartRate(date) }
        coVerify(exactly = 0) { hc.readRawHeartRateSamples(any(), any()) }
        coVerify(exactly = 0) { hc.readHeartRateSamples(any(), any()) }
    }

    @Test fun `dashboard metric cancellation propagates`() = runTest {
        val date = LocalDate.of(2026, 6, 27)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(heartRatePermission)
        coEvery { hc.grantedPermissions() } returns setOf(heartRatePermission)
        coEvery { hc.readAvgHeartRate(date) } throws CancellationException("cancelled")

        try {
            dashboardDataLoader(hc).loadDashboard(
                DashboardQuery(
                    date = date,
                    visibleMetrics = setOf(DashboardMetric.AVG_HEART_RATE),
                )
            )
            fail("Expected dashboard load cancellation to propagate")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }
    }

    @Test fun `one loadDashboard call reads granted permissions once`() = runTest {
        val date = LocalDate.of(2026, 6, 27)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(stepsPermission)
        coEvery { hc.grantedPermissions() } returns setOf(stepsPermission)
        coEvery { hc.readSteps(date) } returns 8_765L

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.STEPS),
            )
        )

        assertEquals(8_765L, data.steps)
        coVerify(exactly = 1) { hc.grantedPermissions() }
    }

    @Test fun `loadDashboard reads Health Connect on repeat loads`() = runTest {
        val date = LocalDate.of(2026, 6, 23)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(stepsPermission)
        coEvery { hc.grantedPermissions() } returns setOf(stepsPermission)
        coEvery { hc.readSteps(date) } returns 8_000L
        val query = DashboardQuery(
            date = date,
            visibleMetrics = setOf(DashboardMetric.STEPS),
        )
        val loader = dashboardDataLoader(hc = hc)

        val first = loader.loadDashboard(query)
        val second = loader.loadDashboard(query)

        assertEquals(8_000L, first.steps)
        assertEquals(8_000L, second.steps)
        coVerify(exactly = 2) { hc.readSteps(date) }
    }

    @Test fun `loadDashboard combines sleep sessions with selected sleep range mode`() = runTest {
        val date = LocalDate.of(2026, 5, 4)
        val eveningSleep = sleep(
            id = "evening",
            start = "2026-05-03T21:46:00Z",
            end = "2026-05-03T22:22:00Z",
            duration = Duration.ofMinutes(36),
        )
        val nextDaySleep = sleep(
            id = "next-day",
            start = "2026-05-04T01:11:00Z",
            end = "2026-05-04T08:13:00Z",
            duration = Duration.ofHours(7).plusMinutes(3),
        )
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(sleepPermission)
        coEvery { hc.grantedPermissions() } returns setOf(sleepPermission)
        coEvery { hc.readSleepData(any(), any(), any()) } returns SleepReadData(
            sessions = listOf(nextDaySleep, eveningSleep),
            dailyAggregateDurations = emptyList(),
        )

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                sleepWindow = SleepWindow.Default,
                visibleMetrics = setOf(DashboardMetric.SLEEP),
            )
        )

        assertNotNull(data.sleep)
        assertEquals(eveningSleep.startTime, data.sleep!!.startTime)
        assertEquals(nextDaySleep.endTime, data.sleep.endTime)
        assertEquals(Duration.ofHours(7).plusMinutes(38).toMillis(), data.sleep.durationMs)
    }

    @Test fun `loadDashboard prefers Health Connect aggregate sleep duration`() = runTest {
        val date = LocalDate.of(2026, 5, 4)
        val fitbitSleep = sleep(
            id = "fitbit",
            start = "2026-05-03T22:00:00Z",
            end = "2026-05-04T06:00:00Z",
            duration = Duration.ofHours(8),
        )
        val googleFitSleep = sleep(
            id = "google-fit",
            start = "2026-05-03T22:05:00Z",
            end = "2026-05-04T06:05:00Z",
            duration = Duration.ofHours(8),
        )
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(sleepPermission)
        coEvery { hc.grantedPermissions() } returns setOf(sleepPermission)
        coEvery { hc.readSleepData(any(), any(), any()) } returns SleepReadData(
            sessions = listOf(fitbitSleep, googleFitSleep),
            dailyAggregateDurations = listOf(
                DailySleepDuration(
                    date = date,
                    durationMs = Duration.ofHours(8).toMillis(),
                )
            ),
        )

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                sleepWindow = SleepWindow.Default,
                visibleMetrics = setOf(DashboardMetric.SLEEP),
            )
        )

        assertEquals(Duration.ofHours(8).toMillis(), data.sleep!!.durationMs)
    }

    @Test fun `loadDashboard skips hidden dashboard metrics`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(stepsPermission, distancePermission)
        coEvery { hc.grantedPermissions() } returns setOf(stepsPermission, distancePermission)
        coEvery { hc.readSteps(date) } returns 9876L

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.STEPS),
            )
        )

        assertEquals(9876L, data.steps)
        assertEquals(setOf(DashboardMetric.STEPS), data.loadedMetrics)
        coVerify(exactly = 0) { hc.readDistanceMeters(any()) }
    }

    @Test fun `loadDashboard reports missing permissions only for visible metrics`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(stepsPermission, distancePermission, sleepPermission)
        coEvery { hc.grantedPermissions() } returns setOf(stepsPermission)
        coEvery { hc.readSteps(date) } returns 9876L

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.STEPS, DashboardMetric.DISTANCE),
            )
        )

        assertEquals(setOf(distancePermission), data.missingPermissions)
        assertEquals(9876L, data.steps)
        coVerify(exactly = 0) { hc.readSleepData(any(), any(), any()) }
    }

    @Test fun `loadDashboard loads all workouts for selected day`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        val latestWorkout = workout(
            id = "run-2",
            start = "2026-05-16T18:00:00Z",
            end = "2026-05-16T18:45:00Z",
            duration = Duration.ofMinutes(45),
        )
        val earlierWorkout = workout(
            id = "walk-1",
            start = "2026-05-16T08:00:00Z",
            end = "2026-05-16T08:30:00Z",
            duration = Duration.ofMinutes(30),
        )
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(exercisePermission)
        coEvery { hc.grantedPermissions() } returns setOf(exercisePermission)
        coEvery { hc.readExerciseSessions(any(), any()) } returns listOf(latestWorkout, earlierWorkout)

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.WORKOUT),
            )
        )

        assertEquals(listOf(latestWorkout, earlierWorkout), data.workouts)
        assertEquals(latestWorkout, data.workout)
        assertEquals(setOf(DashboardMetric.WORKOUT), data.loadedMetrics)
    }

    @Test fun `loadDashboard reads plain Health Connect total calories by default`() = runTest {
        val date = LocalDate.of(2026, 6, 5)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(
            totalCaloriesPermission,
            activeCaloriesPermission,
            bmrPermission,
        )
        coEvery { hc.grantedPermissions() } returns setOf(
            totalCaloriesPermission,
            activeCaloriesPermission,
            bmrPermission,
        )
        coEvery {
            hc.readCaloriesBurned(date = date, includeEstimatedCalories = false)
        } returns CaloriesBurnedValue(123.0, CaloriesBurnedSource.RECORDED_TOTAL)

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.CALORIES_OUT),
            )
        )

        assertEquals(123.0, data.caloriesKcal, 0.01)
        assertEquals(CaloriesBurnedSource.RECORDED_TOTAL, data.caloriesKcalSource)
        coVerify(exactly = 0) {
            hc.readCaloriesBurned(date = date, includeEstimatedCalories = true)
        }
    }

    @Test fun `loadDashboard reads caffeine from daily macros when requested`() = runTest {
        val date = LocalDate.of(2026, 6, 5)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(nutritionPermission)
        coEvery { hc.grantedPermissions() } returns setOf(nutritionPermission)
        coEvery { hc.readDailyMacros(date, date) } returns listOf(
            DailyMacros(
                date = date,
                nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 0.095),
            )
        )

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.CAFFEINE),
            )
        )

        assertEquals(0.095, data.caffeineGrams ?: 0.0, 0.0001)
        assertEquals(setOf(DashboardMetric.CAFFEINE), data.loadedMetrics)
        coVerify(exactly = 1) { hc.readDailyMacros(date, date) }
    }

    @Test fun `loadDashboard enables OpenVitals calorie calculations when preference is on`() = runTest {
        val date = LocalDate.of(2026, 6, 5)
        val hc = mockk<HealthConnectManager>()
        val prefs = mockk<PreferencesRepository>()
        every { prefs.showOpenVitalsCalculatedCalories } returns true
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(
            totalCaloriesPermission,
            activeCaloriesPermission,
            bmrPermission,
        )
        coEvery { hc.grantedPermissions() } returns setOf(
            totalCaloriesPermission,
            activeCaloriesPermission,
            bmrPermission,
        )
        coEvery {
            hc.readCaloriesBurned(date = date, includeEstimatedCalories = true)
        } returns CaloriesBurnedValue(456.0, CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR)

        val data = dashboardDataLoader(
            hc = hc,
            preferencesRepository = prefs,
        ).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.CALORIES_OUT),
            )
        )

        assertEquals(456.0, data.caloriesKcal, 0.01)
        assertEquals(CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR, data.caloriesKcalSource)
    }

    @Test fun `loadDashboard reports active calories and BMR permissions when OpenVitals calorie calculations are on`() = runTest {
        val date = LocalDate.of(2026, 6, 5)
        val hc = mockk<HealthConnectManager>()
        val prefs = mockk<PreferencesRepository>()
        every { prefs.showOpenVitalsCalculatedCalories } returns true
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(
            totalCaloriesPermission,
            activeCaloriesPermission,
            bmrPermission,
        )
        coEvery { hc.grantedPermissions() } returns setOf(totalCaloriesPermission)
        coEvery {
            hc.readCaloriesBurned(date = date, includeEstimatedCalories = false)
        } returns null

        val data = dashboardDataLoader(
            hc = hc,
            preferencesRepository = prefs,
        ).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.CALORIES_OUT),
            )
        )

        assertEquals(setOf(activeCaloriesPermission, bmrPermission), data.missingPermissions)
    }

    @Test fun `loadDashboard shows latest weight even when no selected-day weight exists`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        val weightTime = Instant.parse("2026-04-02T08:30:00Z")
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(weightPermission)
        coEvery { hc.grantedPermissions() } returns setOf(weightPermission)
        coEvery { hc.readLatestWeight() } returns WeightEntry(
            time = weightTime,
            weightKg = 82.4,
            source = "test",
        )

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.WEIGHT),
            )
        )

        assertEquals(82.4, data.weightKg!!, 0.01)
        assertEquals(weightTime, data.weightTime)
        coVerify(exactly = 0) { hc.readLatestWeight(date) }
    }

    @Test fun `loadDashboard shows latest height with measurement time`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        val heightTime = Instant.parse("2025-12-10T07:45:00Z")
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(heightPermission)
        coEvery { hc.grantedPermissions() } returns setOf(heightPermission)
        coEvery { hc.readLatestHeightEntry() } returns HeightEntry(
            time = heightTime,
            heightCm = 178.0,
            source = "test",
        )

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.HEIGHT),
            )
        )

        assertEquals(178.0, data.heightCm!!, 0.01)
        assertEquals(heightTime, data.heightTime)
    }

    @Test fun `loadDashboard reads cycle metric when requested and permitted`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        val start = Instant.parse("2026-05-16T05:00:00Z")
        val end = Instant.parse("2026-05-17T05:00:00Z")
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(menstruationPermission)
        coEvery { hc.grantedPermissions() } returns setOf(menstruationPermission)
        coEvery { hc.readMenstruationPeriods(any(), any()) } returns listOf(
            MenstruationPeriodEntry(
                startTime = start,
                endTime = end,
                source = "test",
            ),
        )

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.CYCLE),
            )
        )

        assertEquals(setOf(DashboardMetric.CYCLE), data.loadedMetrics)
        assertEquals(2, data.menstruationPeriodDays)
        // Two reads: the day window for the tile, the 45-day probe for the
        // recent-history demotion signal.
        coVerify(exactly = 2) { hc.readMenstruationPeriods(any(), any()) }
        assertEquals(setOf(DashboardMetric.CYCLE), data.recentHistoryMetrics)
    }

    @Test fun `weekly cardio load uses rolling last seven days`() = runTest {
        val date = LocalDate.of(2026, 6, 2)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(stepsPermission, distancePermission)
        coEvery { hc.grantedPermissions() } returns setOf(stepsPermission, distancePermission)
        coEvery {
            hc.readDailySteps(
                startDate = any(),
                endDate = any(),
                includeDistance = any(),
                includeFloors = any(),
                includeActiveCalories = any(),
                includeElevation = any(),
            )
        } returns (0..6).map { offset ->
            DailySteps(
                date = date.minusDays(offset.toLong()),
                steps = 3_000L,
                distanceMeters = 0.0,
            )
        }
        val repository = dashboardDataLoader(hc)
        val query = DashboardQuery(
            date = date,
            activityWeekMode = ActivityWeekMode.LAST_7_DAYS,
            visibleMetrics = setOf(DashboardMetric.WEEKLY_CARDIO_LOAD),
        )

        val data = repository.loadDashboard(query)
        assertEquals(7, data.weeklyCardioLoad?.currentScore)
        assertEquals(1, data.weeklyCardioLoad?.todayScore)
        coVerify {
            hc.readDailySteps(
                startDate = date.minusDays(34),
                endDate = date,
                includeDistance = true,
                includeFloors = false,
                includeActiveCalories = false,
                includeElevation = false,
            )
        }
    }

    @Test fun `weekly cardio reads heart rate samples for two week window`() = runTest {
        val date = LocalDate.of(2026, 6, 2)
        val zone = ZoneId.systemDefault()
        val heartRateSampleStart = date.minusDays(13)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(
            stepsPermission,
            distancePermission,
            heartRatePermission,
        )
        coEvery { hc.grantedPermissions() } returns setOf(
            stepsPermission,
            distancePermission,
            heartRatePermission,
        )
        coEvery {
            hc.readDailySteps(
                startDate = any(),
                endDate = any(),
                includeDistance = any(),
                includeFloors = any(),
                includeActiveCalories = any(),
                includeElevation = any(),
            )
        } returns (0..34).map { offset ->
            DailySteps(
                date = date.minusDays(offset.toLong()),
                steps = 3_000L,
                distanceMeters = 0.0,
            )
        }
        coEvery { hc.readHeartRateSamples(any(), any()) } returns emptyList()
        coEvery { hc.readExerciseSessions(any(), any()) } returns emptyList()
        coEvery { hc.readDailyRestingHR(any(), any()) } returns emptyList()
        dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                activityWeekMode = ActivityWeekMode.LAST_7_DAYS,
                visibleMetrics = setOf(DashboardMetric.WEEKLY_CARDIO_LOAD),
            )
        )

        val expectedStart = heartRateSampleStart.atStartOfDay(zone).toInstant()
        val expectedEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
        coVerify(exactly = 1) {
            hc.readHeartRateSamples(expectedStart, expectedEnd)
        }
    }

    @Test fun `weekly intensity minutes use heart rate reserve`() = runTest {
        val date = LocalDate.of(2026, 6, 2)
        val start = Instant.parse("2026-06-02T10:00:00Z")
        val workout = workout(
            id = "run-1",
            start = start.toString(),
            end = start.plusSeconds(30 * 60L).toString(),
            duration = Duration.ofMinutes(30),
        )
        val samples = listOf(
            HeartRateSample(
                time = start.minusSeconds(60 * 60L),
                beatsPerMinute = 180L,
                source = "watch",
            ),
        ) + (0..30).map { minute ->
            HeartRateSample(
                time = start.plusSeconds(minute * 60L),
                beatsPerMinute = 120L,
                source = "watch",
            )
        }
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(
            stepsPermission,
            distancePermission,
            activeCaloriesPermission,
            exercisePermission,
            heartRatePermission,
            restingHeartRatePermission,
        )
        coEvery { hc.grantedPermissions() } returns setOf(
            stepsPermission,
            distancePermission,
            activeCaloriesPermission,
            exercisePermission,
            heartRatePermission,
            restingHeartRatePermission,
        )
        coEvery {
            hc.readDailySteps(
                startDate = any(),
                endDate = any(),
                includeDistance = any(),
                includeFloors = any(),
                includeActiveCalories = any(),
                includeElevation = any(),
            )
        } returns emptyList()
        coEvery { hc.readHeartRateSamples(any(), any()) } returns samples
        coEvery { hc.readDailyRestingHR(any(), any()) } returns listOf(DailyRestingHR(date, 60L))
        coEvery { hc.readExerciseSessions(any(), any()) } returns listOf(workout)
        val repository = dashboardDataLoader(hc)
        val query = DashboardQuery(
            date = date,
            activityWeekMode = ActivityWeekMode.LAST_7_DAYS,
            visibleMetrics = setOf(DashboardMetric.INTENSITY_MINUTES),
        )

        val data = repository.loadDashboard(query)
        assertEquals(30, data.weeklyIntensityMinutes?.moderateEquivalentMinutes)
        assertEquals(30, data.weeklyIntensityMinutes?.todayModerateEquivalentMinutes)
        assertEquals(setOf(DashboardMetric.INTENSITY_MINUTES), data.loadedMetrics)
    }

    @Test fun `loadDashboard reads personal baselines for resting heart rate and HRV`() = runTest {
        val date = LocalDate.of(2026, 6, 10)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(restingHeartRatePermission, hrvPermission)
        coEvery { hc.grantedPermissions() } returns setOf(restingHeartRatePermission, hrvPermission)
        coEvery { hc.readRestingHeartRateSamples(any(), any()) } returns listOf(
            RestingHeartRateSample(
                time = Instant.parse("2026-06-10T05:00:00Z"),
                beatsPerMinute = 58,
                source = "watch",
            ),
        )
        coEvery { hc.readHrvSamples(any(), any()) } returns listOf(
            HrvSample(
                time = Instant.parse("2026-06-10T05:00:00Z"),
                rmssdMs = 46.0,
                source = "watch",
            ),
            HrvSample(
                time = Instant.parse("2026-06-10T06:00:00Z"),
                rmssdMs = 50.0,
                source = "watch",
            ),
        )
        coEvery { hc.readDailyRestingHR(date.minusDays(28), date.minusDays(1)) } returns listOf(
            DailyRestingHR(date.minusDays(3), 56),
            DailyRestingHR(date.minusDays(2), 57),
            DailyRestingHR(date.minusDays(1), 60),
        )
        coEvery { hc.readDailyHRV(date.minusDays(28), date.minusDays(1)) } returns listOf(
            DailyHrv(date.minusDays(3), 42.0),
            DailyHrv(date.minusDays(2), 50.0),
            DailyHrv(date.minusDays(1), 56.0),
        )

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.RESTING_HEART_RATE, DashboardMetric.HRV),
            )
        )

        assertEquals(58L, data.restingHeartRateBpm)
        assertEquals(57L, data.restingHeartRateBaselineBpm)
        assertEquals(48.0, data.hrvRmssdMs ?: 0.0, 0.01)
        assertEquals(50.0, data.hrvBaselineRmssdMs ?: 0.0, 0.01)
        assertEquals(2, data.hrvSampleCount)
        assertEquals(Instant.parse("2026-06-10T05:00:00Z"), data.hrvSampleStartTime)
        assertEquals(Instant.parse("2026-06-10T06:00:00Z"), data.hrvSampleEndTime)
    }

    @Test fun `loadDashboard calculates BMI from latest health connect body entries`() = runTest {
        val date = LocalDate.of(2026, 6, 23)
        val time = Instant.parse("2026-06-23T08:00:00Z")
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(weightPermission, heightPermission)
        coEvery { hc.grantedPermissions() } returns setOf(weightPermission, heightPermission)
        coEvery { hc.readLatestWeight() } returns WeightEntry(
            time = time,
            weightKg = 80.0,
            source = "scale",
        )
        coEvery { hc.readLatestHeightEntry() } returns HeightEntry(
            time = time,
            heightCm = 200.0,
            source = "manual",
        )
        val repository = dashboardDataLoader(hc)
        val query = DashboardQuery(
            date = date,
            visibleMetrics = setOf(DashboardMetric.BMI),
        )

        val data = repository.loadDashboard(query)

        assertEquals(20.0, data.bmi ?: 0.0, 0.01)
    }

    // ─── Night-window attribution for the overnight vitals ────────────────────

    @Test fun `overnight vitals read back to the night-window start`() = runTest {
        val date = LocalDate.of(2026, 7, 24)
        val nightStart = atLocal(date.minusDays(1), 18)
        val dayEnd = atLocal(date.plusDays(1), 0)
        // Stamped 23:00 the previous evening: inside the night window, outside
        // [00:00, 24:00) — the read that used to leave the tiles empty.
        val hc = overnightVitalsHc(listOf(VitalsReading(atLocal(date.minusDays(1), 23), 50.0)))

        val data = loadOvernightVitals(hc, date)

        coVerify(exactly = 1) { hc.readHrvSamples(nightStart, dayEnd) }
        coVerify(exactly = 1) { hc.readRestingHeartRateSamples(nightStart, dayEnd) }
        coVerify(exactly = 1) { hc.readLatestSpO2InWindow(nightStart, dayEnd) }
        coVerify(exactly = 1) { hc.readRespiratoryRateEntries(nightStart, dayEnd) }
        coVerify(exactly = 1) { hc.readSkinTemperatureEntries(nightStart, dayEnd) }
        assertEquals(50.0, data.hrvRmssdMs ?: 0.0, 0.01)
        assertEquals(50L, data.restingHeartRateBpm)
        assertEquals(50.0, data.latestSpO2Percent ?: 0.0, 0.01)
        assertEquals(50.0, data.avgRespiratoryRate ?: 0.0, 0.01)
        assertEquals(50.0, data.latestSkinTemperatureDeltaCelsius ?: 0.0, 0.01)
    }

    @Test fun `an overnight sample after midnight stays with the day it wakes on`() = runTest {
        val date = LocalDate.of(2026, 7, 24)
        val hc = overnightVitalsHc(listOf(VitalsReading(atLocal(date, 3), 47.0)))

        val data = loadOvernightVitals(hc, date)

        assertEquals(47.0, data.hrvRmssdMs ?: 0.0, 0.01)
        assertEquals(47L, data.restingHeartRateBpm)
        assertEquals(47.0, data.latestSpO2Percent ?: 0.0, 0.01)
        assertEquals(47.0, data.avgRespiratoryRate ?: 0.0, 0.01)
        assertEquals(47.0, data.latestSkinTemperatureDeltaCelsius ?: 0.0, 0.01)
    }

    @Test fun `a non-default night window moves the attribution boundary`() = runTest {
        val date = LocalDate.of(2026, 7, 24)
        val window = SleepWindow(startHour = 21, endHour = 8)

        val tooEarly = loadOvernightVitals(
            overnightVitalsHc(listOf(VitalsReading(atLocal(date.minusDays(1), 20), 44.0))),
            date,
            window,
        )
        assertNull(tooEarly.hrvRmssdMs)
        assertEquals(0L, tooEarly.restingHeartRateBpm)
        assertNull(tooEarly.latestSpO2Percent)

        val hc = overnightVitalsHc(listOf(VitalsReading(atLocal(date.minusDays(1), 22), 55.0)))
        val included = loadOvernightVitals(hc, date, window)

        coVerify(exactly = 1) { hc.readHrvSamples(atLocal(date.minusDays(1), 21), any()) }
        assertEquals(55.0, included.hrvRmssdMs ?: 0.0, 0.01)
        assertEquals(55L, included.restingHeartRateBpm)
        assertEquals(55.0, included.latestSpO2Percent ?: 0.0, 0.01)
    }

    @Test fun `a later daytime measurement still wins for latest-sample vitals`() = runTest {
        val date = LocalDate.of(2026, 7, 24)
        val hc = overnightVitalsHc(
            listOf(
                VitalsReading(atLocal(date.minusDays(1), 23), 40.0),
                VitalsReading(atLocal(date, 14), 60.0),
            )
        )

        val data = loadOvernightVitals(hc, date)

        assertEquals(60L, data.restingHeartRateBpm)
        assertEquals(60.0, data.latestSpO2Percent ?: 0.0, 0.01)
        assertEquals(60.0, data.latestSkinTemperatureDeltaCelsius ?: 0.0, 0.01)
        // HRV and respiratory rate average their window instead of taking the last.
        assertEquals(50.0, data.hrvRmssdMs ?: 0.0, 0.01)
        assertEquals(50.0, data.avgRespiratoryRate ?: 0.0, 0.01)
    }

    @Test fun `day-scoped vitals keep the calendar-day clamp`() = runTest {
        val date = LocalDate.of(2026, 7, 24)
        val dayStart = atLocal(date, 0)
        val dayEnd = atLocal(date.plusDays(1), 0)
        val time = atLocal(date, 9)
        val permissions = setOf(
            heartRatePermission,
            bloodPressurePermission,
            vo2MaxPermission,
            bodyTemperaturePermission,
            bloodGlucosePermission,
        )
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns permissions
        coEvery { hc.grantedPermissions() } returns permissions
        coEvery { hc.readAvgHeartRate(date) } returns 68L
        coEvery { hc.readLatestBloodPressure(date) } returns BloodPressureEntry(
            time = time,
            systolicMmHg = 118,
            diastolicMmHg = 76,
            source = "cuff",
        )
        coEvery { hc.readLatestVo2Max(date) } returns Vo2MaxEntry(
            time = time,
            vo2MaxMlPerKgPerMin = 48.0,
            source = "watch",
        )
        coEvery { hc.readBodyTemperatureEntries(any(), any()) } returns listOf(
            BodyTempEntry(time = time, temperatureCelsius = 36.7, source = "thermometer"),
        )
        coEvery { hc.readBloodGlucoseEntries(any(), any()) } returns listOf(
            BloodGlucoseEntry(
                time = time,
                millimolesPerLiter = 5.2,
                specimenSource = 0,
                mealType = 0,
                relationToMeal = 0,
                source = "meter",
            ),
        )

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(
                    DashboardMetric.AVG_HEART_RATE,
                    DashboardMetric.BLOOD_PRESSURE,
                    DashboardMetric.VO2_MAX,
                    DashboardMetric.BODY_TEMPERATURE,
                    DashboardMetric.BLOOD_GLUCOSE,
                ),
                includeHistoricalBaselines = false,
                includeWeeklyTrainingSignals = false,
            )
        )

        coVerify(exactly = 1) { hc.readBodyTemperatureEntries(dayStart, dayEnd) }
        coVerify(exactly = 1) { hc.readBloodGlucoseEntries(dayStart, dayEnd) }
        assertEquals(68L, data.avgHeartRateBpm)
        assertEquals(118, data.latestSystolicMmHg ?: 0)
        assertEquals(48.0, data.latestVo2Max ?: 0.0, 0.01)
        assertEquals(36.7, data.latestBodyTemperatureCelsius ?: 0.0, 0.01)
        assertEquals(5.2, data.latestBloodGlucoseMillimolesPerLiter ?: 0.0, 0.01)
    }

    private suspend fun loadOvernightVitals(
        hc: HealthConnectManager,
        date: LocalDate,
        sleepWindow: SleepWindow = SleepWindow.Default,
    ) = dashboardDataLoader(hc).loadDashboard(
        DashboardQuery(
            date = date,
            sleepWindow = sleepWindow,
            visibleMetrics = overnightMetrics,
            includeHistoricalBaselines = false,
            includeWeeklyTrainingSignals = false,
        )
    )

    /**
     * A manager whose five overnight reads honor the window they are handed, so
     * a day-clamped read returns nothing: these tests fail on the attribution
     * rule itself rather than on an argument match alone. One [VitalsReading]
     * feeds every metric so a single expected value covers all five tiles.
     */
    private fun overnightVitalsHc(readings: List<VitalsReading>): HealthConnectManager {
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.isSkinTemperatureAvailable() } returns true
        every { hc.managedPermissions } returns overnightPermissions
        coEvery { hc.grantedPermissions() } returns overnightPermissions

        fun inWindow(start: Instant, end: Instant): List<VitalsReading> = readings
            .filter { !it.time.isBefore(start) && it.time.isBefore(end) }
            .sortedBy { it.time }

        coEvery { hc.readHrvSamples(any(), any()) } answers {
            inWindow(firstArg(), secondArg()).map { HrvSample(it.time, it.value, "watch") }
        }
        coEvery { hc.readRestingHeartRateSamples(any(), any()) } answers {
            inWindow(firstArg(), secondArg()).map {
                RestingHeartRateSample(it.time, it.value.toLong(), "watch")
            }
        }
        coEvery { hc.readLatestSpO2InWindow(any(), any()) } answers {
            inWindow(firstArg(), secondArg()).lastOrNull()?.let { SpO2Entry(it.time, it.value, "watch") }
        }
        coEvery { hc.readRespiratoryRateEntries(any(), any()) } answers {
            inWindow(firstArg(), secondArg()).map { RespiratoryRateEntry(it.time, it.value, "watch") }
        }
        coEvery { hc.readSkinTemperatureEntries(any(), any()) } answers {
            inWindow(firstArg(), secondArg()).map {
                SkinTemperatureEntry(
                    startTime = it.time,
                    endTime = it.time,
                    baselineCelsius = null,
                    averageDeltaCelsius = it.value,
                    minDeltaCelsius = null,
                    maxDeltaCelsius = null,
                    measurementLocation = 0,
                    source = "watch",
                )
            }
        }
        return hc
    }

    private fun atLocal(date: LocalDate, hour: Int): Instant =
        date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant()

    private fun sleep(
        id: String,
        start: String,
        end: String,
        duration: Duration,
    ) = SleepData(
        id = id,
        startTime = Instant.parse(start),
        endTime = Instant.parse(end),
        durationMs = duration.toMillis(),
        source = "gadgetbridge",
    )

    private fun workout(
        id: String,
        start: String,
        end: String,
        duration: Duration,
    ) = ExerciseData(
        id = id,
        title = null,
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        startTime = Instant.parse(start),
        endTime = Instant.parse(end),
        durationMs = duration.toMillis(),
        source = "gadgetbridge",
    )
}

/** One overnight measurement, replayed into every overnight metric. */
private data class VitalsReading(val time: Instant, val value: Double)

private fun dashboardDataLoader(
    hc: HealthConnectManager,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    preferencesRepository: PreferencesRepository? = null,
): DashboardDataLoader =
    DashboardDataLoader(
        hc = hc,
        dispatchers = dispatchers,
        preferencesRepository = preferencesRepository,
    )
