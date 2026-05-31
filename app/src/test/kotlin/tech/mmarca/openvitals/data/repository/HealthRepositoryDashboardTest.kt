package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.DashboardMetric
import tech.mmarca.openvitals.data.model.DashboardQuery
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.HeightEntry
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class HealthRepositoryDashboardTest {

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val distancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val sleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val exercisePermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val menstruationPermission = HealthPermission.getReadPermission(MenstruationPeriodRecord::class)
    private val weightPermission = HealthPermission.getReadPermission(WeightRecord::class)
    private val heightPermission = HealthPermission.getReadPermission(HeightRecord::class)

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

        val data = HealthRepository(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.STEPS, DashboardMetric.DISTANCE),
            )
        )

        assertEquals(0L, data.steps)
        assertEquals(1234.0, data.distanceMeters, 0.01)
        assertNull(data.workout)
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
        coEvery { hc.readSleepSessions(any(), any()) } returns listOf(nextDaySleep, eveningSleep)

        val data = HealthRepository(hc).loadDashboard(
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

    @Test fun `loadDashboard skips hidden dashboard metrics`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.requestableAllPermissions } returns setOf(stepsPermission, distancePermission)
        coEvery { hc.grantedPermissions() } returns setOf(stepsPermission, distancePermission)
        coEvery { hc.readSteps(date) } returns 9876L

        val data = HealthRepository(hc).loadDashboard(
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

        val data = HealthRepository(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.STEPS, DashboardMetric.DISTANCE),
            )
        )

        assertEquals(setOf(distancePermission), data.missingPermissions)
        assertEquals(9876L, data.steps)
        coVerify(exactly = 0) { hc.readSleepSessions(any(), any()) }
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

        val data = HealthRepository(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.WORKOUT),
            )
        )

        assertEquals(listOf(latestWorkout, earlierWorkout), data.workouts)
        assertEquals(latestWorkout, data.workout)
        assertEquals(setOf(DashboardMetric.WORKOUT), data.loadedMetrics)
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

        val data = HealthRepository(hc).loadDashboard(
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

        val data = HealthRepository(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.HEIGHT),
            )
        )

        assertEquals(178.0, data.heightCm!!, 0.01)
        assertEquals(heightTime, data.heightTime)
    }

    @Test fun `loadDashboard skips cycle reads when cycle tracking is disabled`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.requestableAllPermissions } returns setOf(menstruationPermission)
        coEvery { hc.grantedPermissions() } returns setOf(menstruationPermission)

        val data = HealthRepository(hc).loadDashboard(
            DashboardQuery(
                date = date,
                visibleMetrics = setOf(DashboardMetric.CYCLE),
                trackCycle = false,
            )
        )

        assertEquals(emptySet<DashboardMetric>(), data.loadedMetrics)
        coVerify(exactly = 0) { hc.readMenstruationPeriods(any(), any()) }
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
