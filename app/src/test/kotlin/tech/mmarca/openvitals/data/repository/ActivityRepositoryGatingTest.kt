package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/** The permission gating around the activity reads: which reads run, and which optional aggregates they ask for. */
class ActivityRepositoryGatingTest {

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val distancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val floorsPermission = HealthPermission.getReadPermission(FloorsClimbedRecord::class)
    private val elevationPermission = HealthPermission.getReadPermission(ElevationGainedRecord::class)
    private val wheelchairPermission = HealthPermission.getReadPermission(WheelchairPushesRecord::class)
    private val exercisePermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val speedPermission = HealthPermission.getReadPermission(SpeedRecord::class)
    private val historyPermission = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // Daily steps.

    @Test
    fun `loadDailySteps scans from the full start when history access is not gated`() = runTest {
        // The provider does not require the history permission, so there is no 30-day clamp.
        val start = LocalDate.of(2009, 1, 1)
        val end = LocalDate.of(2026, 7, 10)
        val hc = hc(granted = setOf(stepsPermission), additionalDataAccessPermissions = emptySet())
        val capturedStart = slot<LocalDate>()

        ActivityRepositoryImpl(hc).loadDailySteps(start, end)

        coVerify {
            hc.readDailySteps(
                startDate = capture(capturedStart),
                endDate = end,
                includeSteps = any(),
                includeDistance = any(),
                includeWheelchairPushes = any(),
                includeFloors = any(),
                includeActiveCalories = any(),
                includeElevation = any(),
            )
        }
        assertEquals(start, capturedStart.captured)
    }

    @Test
    fun `loadDailySteps requests floors when the floors permission is granted`() = runTest {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 7)
        val hc = hc(granted = setOf(stepsPermission, floorsPermission))
        val capturedFloors = slot<Boolean>()

        ActivityRepositoryImpl(hc).loadDailySteps(start, end)

        coVerify {
            hc.readDailySteps(
                startDate = any(),
                endDate = any(),
                includeSteps = any(),
                includeDistance = any(),
                includeWheelchairPushes = any(),
                includeFloors = capture(capturedFloors),
                includeActiveCalories = any(),
                includeElevation = any(),
            )
        }
        assertTrue(capturedFloors.captured)
    }

    @Test
    fun `loadDailySteps returns empty without the steps permission`() = runTest {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 7)
        val hc = hc(granted = emptySet())

        val result = ActivityRepositoryImpl(hc).loadDailySteps(start, end)

        assertEquals(emptyList<Any>(), result)
        // ...and it never queries the data source.
        coVerify(exactly = 0) {
            hc.readDailySteps(
                startDate = any(),
                endDate = any(),
                includeSteps = any(),
                includeDistance = any(),
                includeWheelchairPushes = any(),
                includeFloors = any(),
                includeActiveCalories = any(),
                includeElevation = any(),
            )
        }
    }

    // Period flags.

    @Test
    fun `period read requests floors and elevation when granted`() = runTest {
        val hc = hc(granted = setOf(stepsPermission, floorsPermission, elevationPermission))

        ActivityRepositoryImpl(hc).loadActivityPeriod(
            query = dayQuery(),
            includeSteps = true,
            includeNutrition = false,
        )

        coVerify(atLeast = 1) {
            hc.readDailySteps(
                startDate = any(),
                endDate = any(),
                includeSteps = any(),
                includeDistance = any(),
                includeWheelchairPushes = any(),
                includeFloors = true,
                includeActiveCalories = any(),
                includeElevation = true,
            )
        }
        coVerify(exactly = 0) {
            hc.readDailySteps(
                startDate = any(),
                endDate = any(),
                includeSteps = any(),
                includeDistance = any(),
                includeWheelchairPushes = any(),
                includeFloors = false,
                includeActiveCalories = any(),
                includeElevation = any(),
            )
        }
    }

    @Test
    fun `period read forwards wheelchair pushes when asked and granted`() = runTest {
        val hc = hc(granted = setOf(stepsPermission, wheelchairPermission))

        ActivityRepositoryImpl(hc).loadActivityPeriod(
            query = dayQuery(),
            includeSteps = false,
            includeNutrition = false,
            includeWheelchairPushes = true,
        )

        coVerify(atLeast = 1) {
            hc.readDailySteps(
                startDate = any(),
                endDate = any(),
                includeSteps = false,
                includeDistance = any(),
                includeWheelchairPushes = true,
                includeFloors = any(),
                includeActiveCalories = any(),
                includeElevation = any(),
            )
        }
        coVerify(exactly = 0) {
            hc.readDailySteps(
                startDate = any(),
                endDate = any(),
                includeSteps = any(),
                includeDistance = any(),
                includeWheelchairPushes = false,
                includeFloors = any(),
                includeActiveCalories = any(),
                includeElevation = any(),
            )
        }
    }

    // Intraday progress opt-out.

    @Test
    fun `includeActivityProgress false skips the intraday read on Day`() = runTest {
        val hc = hc(granted = setOf(stepsPermission))

        ActivityRepositoryImpl(hc).loadActivityPeriod(
            query = dayQuery(),
            includeSteps = true,
            includeNutrition = false,
            includeActivityProgress = false,
        )

        coVerify(exactly = 0) {
            hc.readRawActivityProgress(
                date = any(),
                includeSteps = any(),
                includeDistance = any(),
                includeCalories = any(),
                includeActiveCalories = any(),
                includeCaloriesEstimate = any(),
                includeWheelchairPushes = any(),
                includeFloors = any(),
                includeElevation = any(),
            )
        }
    }

    // Workouts with metrics.

    @Test
    fun `loadWorkoutsWithMetrics forwards both metrics when distance and speed are granted`() = runTest {
        val hc = hc(granted = setOf(exercisePermission, distancePermission, speedPermission))

        val workouts = ActivityRepositoryImpl(hc).loadWorkoutsWithMetrics(workoutStart, workoutEnd)

        assertTrue(capturedIncludeDistance!!)
        assertTrue(capturedIncludeSpeed!!)
        assertEquals(5000.0, workouts.single().totalDistanceMeters!!, 0.0)
        assertEquals(3.2, workouts.single().averageSpeedMetersPerSecond!!, 0.0)
    }

    @Test
    fun `loadWorkoutsWithMetrics degrades to null metrics when distance and speed are not granted`() = runTest {
        val hc = hc(granted = setOf(exercisePermission))

        val workouts = ActivityRepositoryImpl(hc).loadWorkoutsWithMetrics(workoutStart, workoutEnd)

        // The read still happens; only the aggregate metrics are dropped.
        assertEquals(1, withMetricsCalls)
        assertEquals(false, capturedIncludeDistance)
        assertEquals(false, capturedIncludeSpeed)
        assertNull(workouts.single().totalDistanceMeters)
        assertNull(workouts.single().averageSpeedMetersPerSecond)
    }

    @Test
    fun `loadWorkoutsWithMetrics gates distance and speed independently`() = runTest {
        val hc = hc(granted = setOf(exercisePermission, distancePermission))

        ActivityRepositoryImpl(hc).loadWorkoutsWithMetrics(workoutStart, workoutEnd)

        assertEquals(true, capturedIncludeDistance)
        assertEquals(false, capturedIncludeSpeed)
    }

    @Test
    fun `loadWorkoutsWithMetrics skips the read entirely without the exercise permission`() = runTest {
        val hc = hc(granted = setOf(distancePermission, speedPermission))

        val workouts = ActivityRepositoryImpl(hc).loadWorkoutsWithMetrics(workoutStart, workoutEnd)

        assertEquals(emptyList<ExerciseData>(), workouts)
        assertEquals(0, withMetricsCalls)
    }

    @Test
    fun `loadWorkoutsWithMetrics reads the local-day span of the requested range`() = runTest {
        val hc = hc(granted = setOf(exercisePermission))
        val zone = ZoneId.systemDefault()

        ActivityRepositoryImpl(hc).loadWorkoutsWithMetrics(workoutStart, workoutEnd)

        assertEquals(LocalDate.of(2026, 7, 1).atStartOfDay(zone).toInstant(), capturedStart)
        // Inclusive end day: through the start of the following day.
        assertEquals(LocalDate.of(2026, 7, 8).atStartOfDay(zone).toInstant(), capturedEnd)
    }

    // Planned workouts.

    @Test
    fun `loadPlannedWorkout throws SecurityException when planned exercise is unavailable`() = runTest {
        val hc = hc(granted = setOf(exercisePermission))

        val error = runCatching { ActivityRepositoryImpl(hc).loadPlannedWorkout("plan-1") }.exceptionOrNull()

        assertTrue(error is SecurityException)
        coVerify(exactly = 0) { hc.readPlannedExerciseSession(any()) }
    }

    @Test
    fun `deletePlannedWorkout throws SecurityException without the planned write permission`() = runTest {
        val readPlanned = HealthPermission.getReadPermission(PlannedExerciseSessionRecord::class)
        val hc = hc(granted = setOf(readPlanned))
        every { hc.isPlannedExerciseAvailable() } returns true

        val error = runCatching { ActivityRepositoryImpl(hc).deletePlannedWorkout("plan-1") }.exceptionOrNull()

        assertTrue(error is SecurityException)
        coVerify(exactly = 0) { hc.deletePlannedExerciseSession(any()) }
    }

    @Test
    fun `deletePlannedWorkout forwards to Health Connect with the planned write permission`() = runTest {
        val writePlanned = HealthPermission.getWritePermission(PlannedExerciseSessionRecord::class)
        val hc = hc(granted = setOf(writePlanned))
        every { hc.isPlannedExerciseAvailable() } returns true
        coEvery { hc.deletePlannedExerciseSession("plan-1") } returns Unit

        ActivityRepositoryImpl(hc).deletePlannedWorkout("plan-1")

        coVerify(exactly = 1) { hc.deletePlannedExerciseSession("plan-1") }
    }

    // Fixtures.

    private val workoutStart = LocalDate.of(2026, 7, 1)
    private val workoutEnd = LocalDate.of(2026, 7, 7)

    private var withMetricsCalls = 0
    private var capturedIncludeDistance: Boolean? = null
    private var capturedIncludeSpeed: Boolean? = null
    private var capturedStart: Instant? = null
    private var capturedEnd: Instant? = null

    private fun dayQuery(): PeriodLoadQuery {
        val today = LocalDate.now()
        return PeriodLoadQuery(range = TimeRange.DAY, anchorDate = today, today = today)
    }

    private fun hc(
        granted: Set<String>,
        additionalDataAccessPermissions: Set<String> = setOf(historyPermission),
    ): HealthConnectManager = mockk<HealthConnectManager>().also { hc ->
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.additionalDataAccessPermissions } returns additionalDataAccessPermissions
        every { hc.isPlannedExerciseAvailable() } returns false
        coEvery { hc.grantedPermissions() } returns granted
        coEvery {
            hc.readDailySteps(
                startDate = any(),
                endDate = any(),
                includeSteps = any(),
                includeDistance = any(),
                includeWheelchairPushes = any(),
                includeFloors = any(),
                includeActiveCalories = any(),
                includeElevation = any(),
            )
        } returns emptyList()
        coEvery {
            hc.readRawActivityProgress(
                date = any(),
                includeSteps = any(),
                includeDistance = any(),
                includeCalories = any(),
                includeActiveCalories = any(),
                includeCaloriesEstimate = any(),
                includeWheelchairPushes = any(),
                includeFloors = any(),
                includeElevation = any(),
            )
        } returns emptyList()
        coEvery {
            hc.readExerciseSessionsWithMetrics(
                start = any(),
                end = any(),
                includeDistance = any(),
                includeSpeed = any(),
            )
        } answers {
            withMetricsCalls++
            capturedStart = arg(0)
            capturedEnd = arg(1)
            capturedIncludeDistance = arg(2)
            capturedIncludeSpeed = arg(3)
            val includeDistance: Boolean = arg(2)
            val includeSpeed: Boolean = arg(3)
            listOf(
                ExerciseData(
                    id = "ex-1",
                    title = "Morning run",
                    exerciseType = 56,
                    startTime = arg(0),
                    endTime = arg(1),
                    durationMs = (arg<Instant>(1).toEpochMilli() - arg<Instant>(0).toEpochMilli()),
                    source = "provider",
                    totalDistanceMeters = if (includeDistance) 5000.0 else null,
                    averageSpeedMetersPerSecond = if (includeSpeed) 3.2 else null,
                ),
            )
        }
    }
}
