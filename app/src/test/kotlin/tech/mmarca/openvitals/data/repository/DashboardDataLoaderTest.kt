package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
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
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
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
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepReadData
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
        every { hc.requestableAllPermissions } returns setOf(stepsPermission, distancePermission)
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
        every { hc.requestableAllPermissions } returns setOf(heartRatePermission)
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
        every { hc.requestableAllPermissions } returns setOf(heartRatePermission)
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
        every { hc.requestableAllPermissions } returns setOf(stepsPermission)
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
        every { hc.requestableAllPermissions } returns setOf(stepsPermission)
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
        every { hc.requestableAllPermissions } returns setOf(sleepPermission)
        coEvery { hc.grantedPermissions() } returns setOf(sleepPermission)
        coEvery { hc.readSleepData(any(), any(), any()) } returns SleepReadData(
            sessions = listOf(nextDaySleep, eveningSleep),
            dailyAggregateDurations = emptyList(),
        )

        val data = dashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = date,
                sleepRangeMode = SleepRangeMode.EVENING_18H,
                visibleMetrics = setOf(DashboardMetric.SLEEP),
            )
        )

        assertNotNull(data.sleep)
        assertEquals(eveningSleep.startTime, data.sleep!!.startTime)
        assertEquals(nextDaySleep.endTime, data.sleep.endTime)
        assertEquals(Duration.ofHours(7).plusMinutes(39).toMillis(), data.sleep.durationMs)
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
        every { hc.requestableAllPermissions } returns setOf(sleepPermission)
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
                sleepRangeMode = SleepRangeMode.EVENING_18H,
                visibleMetrics = setOf(DashboardMetric.SLEEP),
            )
        )

        assertEquals(Duration.ofHours(8).toMillis(), data.sleep!!.durationMs)
    }

    @Test fun `loadDashboard skips hidden dashboard metrics`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.requestableAllPermissions } returns setOf(stepsPermission, distancePermission)
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
        every { hc.requestableAllPermissions } returns setOf(stepsPermission, distancePermission, sleepPermission)
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
        every { hc.requestableAllPermissions } returns setOf(exercisePermission)
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
        every { hc.requestableAllPermissions } returns setOf(
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
        every { hc.requestableAllPermissions } returns setOf(nutritionPermission)
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
        every { hc.requestableAllPermissions } returns setOf(
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
        every { hc.requestableAllPermissions } returns setOf(
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
        every { hc.requestableAllPermissions } returns setOf(weightPermission)
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
        every { hc.requestableAllPermissions } returns setOf(heightPermission)
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
        every { hc.requestableAllPermissions } returns setOf(menstruationPermission)
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
        coVerify(exactly = 1) { hc.readMenstruationPeriods(any(), any()) }
    }

    @Test fun `weekly cardio load uses rolling last seven days`() = runTest {
        val date = LocalDate.of(2026, 6, 2)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.requestableAllPermissions } returns setOf(stepsPermission, distancePermission)
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
        every { hc.requestableAllPermissions } returns setOf(
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
        every { hc.requestableAllPermissions } returns setOf(
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
        every { hc.requestableAllPermissions } returns setOf(restingHeartRatePermission, hrvPermission)
        coEvery { hc.grantedPermissions() } returns setOf(restingHeartRatePermission, hrvPermission)
        coEvery { hc.readRestingHeartRate(date) } returns 58L
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
        every { hc.requestableAllPermissions } returns setOf(weightPermission, heightPermission)
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
