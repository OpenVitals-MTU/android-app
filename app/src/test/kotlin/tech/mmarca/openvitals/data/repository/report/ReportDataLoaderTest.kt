package tech.mmarca.openvitals.data.repository.report

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.DailySleepDuration
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.ReportGranularity
import tech.mmarca.openvitals.domain.model.ReportMetric
import tech.mmarca.openvitals.domain.model.ReportMetricStatus
import tech.mmarca.openvitals.domain.model.ReportRequest
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class ReportDataLoaderTest {

    private val end = LocalDate.of(2026, 7, 31)
    private val start = end.minusDays(89)

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val distancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val sleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val heartRatePermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val nutritionPermission = HealthPermission.getReadPermission(NutritionRecord::class)
    private val bloodPressurePermission = HealthPermission.getReadPermission(BloodPressureRecord::class)
    private val historyPermission = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

    private val everyPermission = ReportMetric.entries
        .flatMapTo(mutableSetOf()) { metricPermissions(it) }

    private lateinit var hc: HealthConnectManager
    private lateinit var activity: ActivityRepository
    private lateinit var sleep: SleepRepository
    private lateinit var nutrition: NutritionRepository
    private lateinit var hydration: HydrationRepository
    private lateinit var body: BodyRepository
    private lateinit var heart: HeartRepository
    private lateinit var vitals: VitalsRepository
    private lateinit var mindfulness: MindfulnessRepository

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        activity = mockk(relaxed = true)
        sleep = mockk(relaxed = true)
        nutrition = mockk(relaxed = true)
        hydration = mockk(relaxed = true)
        body = mockk(relaxed = true)
        heart = mockk(relaxed = true)
        vitals = mockk(relaxed = true)
        mindfulness = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun metricPermissions(metric: ReportMetric): Set<String> {
        // Mirrors rawPermissionsFor without a loader instance: the dashboard
        // mapping plus the steps hard-requirement for the DailySteps family.
        val loader = ReportDataLoader(
            hc = mockk(relaxed = true),
            activityRepository = mockk(relaxed = true),
            sleepRepository = mockk(relaxed = true),
            nutritionRepository = mockk(relaxed = true),
            hydrationRepository = mockk(relaxed = true),
            bodyRepository = mockk(relaxed = true),
            heartRepository = mockk(relaxed = true),
            vitalsRepository = mockk(relaxed = true),
            mindfulnessRepository = mockk(relaxed = true),
        )
        return loader.rawPermissionsFor(metric)
    }

    private fun loader(
        granted: Set<String> = everyPermission,
        managed: Set<String> = everyPermission + historyPermission,
        additional: Set<String> = setOf(historyPermission),
    ): ReportDataLoader {
        hc = mockk()
        coEvery { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns granted
        every { hc.managedPermissions } returns managed
        every { hc.additionalDataAccessPermissions } returns additional
        return ReportDataLoader(
            hc = hc,
            activityRepository = activity,
            sleepRepository = sleep,
            nutritionRepository = nutrition,
            hydrationRepository = hydration,
            bodyRepository = body,
            heartRepository = heart,
            vitalsRepository = vitals,
            mindfulnessRepository = mindfulness,
        )
    }

    private fun request(vararg metrics: ReportMetric) = ReportRequest(
        metrics = metrics.toSet(),
        granularity = ReportGranularity.DAILY,
        start = start,
        end = end,
    )

    @Test fun `the whole steps family rides on ONE loadDailySteps read`() = runTest {
        coEvery { activity.loadDailySteps(any(), any(), any()) } returns listOf(
            DailySteps(
                date = end.minusDays(1),
                steps = 8_000L,
                distanceMeters = 6_000.0,
                wheelchairPushes = 40L,
                floorsClimbed = 10,
                activeCaloriesKcal = 300.0,
                elevationGainedMeters = 25.0,
            ),
        )

        val data = loader(additional = emptySet()).load(
            request(
                ReportMetric.STEPS,
                ReportMetric.DISTANCE,
                ReportMetric.ACTIVE_CALORIES,
                ReportMetric.FLOORS,
                ReportMetric.ELEVATION,
                ReportMetric.WHEELCHAIR_PUSHES,
            ),
        )

        coVerify(exactly = 1) { activity.loadDailySteps(start, end, includeWheelchairPushes = true) }
        assertTrue(data.results.all { it.status == ReportMetricStatus.OK })
        assertEquals(8_000.0, data.results.first { it.metric == ReportMetric.STEPS }.points.single().value, 1e-9)
        assertEquals(40.0, data.results.first { it.metric == ReportMetric.WHEELCHAIR_PUSHES }.points.single().value, 1e-9)
    }

    @Test fun `pushes stay out of the steps read when not selected`() = runTest {
        coEvery { activity.loadDailySteps(any(), any(), any()) } returns emptyList()

        loader(additional = emptySet()).load(request(ReportMetric.STEPS))

        coVerify(exactly = 1) { activity.loadDailySteps(start, end, includeWheelchairPushes = false) }
    }

    @Test fun `one macros read serves the five nutrition metrics`() = runTest {
        coEvery { nutrition.loadDailyMacros(any(), any()) } returns emptyList()

        val data = loader(additional = emptySet()).load(
            request(
                ReportMetric.CALORIES_IN,
                ReportMetric.PROTEIN,
                ReportMetric.CARBS,
                ReportMetric.FAT,
                ReportMetric.CAFFEINE,
            ),
        )

        coVerify(exactly = 1) { nutrition.loadDailyMacros(start, end) }
        assertTrue(data.results.all { it.status == ReportMetricStatus.EMPTY })
    }

    @Test fun `an ungranted metric is MISSING_PERMISSION and its read never goes out`() = runTest {
        val data = loader(
            granted = everyPermission - sleepPermission,
            additional = emptySet(),
        ).load(request(ReportMetric.SLEEP, ReportMetric.HYDRATION))

        assertEquals(
            ReportMetricStatus.MISSING_PERMISSION,
            data.results.first { it.metric == ReportMetric.SLEEP }.status,
        )
        assertEquals(setOf(ReportMetric.SLEEP), data.missingPermissions)
        coVerify(exactly = 0) { sleep.loadDailySleepDurations(any(), any(), any()) }
        coVerify(exactly = 1) { hydration.loadDailyHydration(any(), any()) }
    }

    @Test fun `a metric outside the provider's managed permissions counts as missing`() = runTest {
        val data = loader(
            managed = everyPermission - distancePermission,
            additional = emptySet(),
        ).load(request(ReportMetric.DISTANCE))

        assertEquals(setOf(ReportMetric.DISTANCE), data.missingPermissions)
        coVerify(exactly = 0) { activity.loadDailySteps(any(), any(), any()) }
    }

    @Test fun `missing history permission clamps the range to 30 days`() = runTest {
        coEvery { hydration.loadDailyHydration(any(), any()) } returns emptyList()

        val data = loader(granted = everyPermission - historyPermission)
            .load(request(ReportMetric.HYDRATION))

        assertEquals(end.minusDays(29), data.effectiveStart)
        assertEquals(30, data.truncatedToDays)
        assertTrue(data.historyPermissionMissing)
        coVerify(exactly = 1) { hydration.loadDailyHydration(end.minusDays(29), end) }
    }

    @Test fun `no clamp when the provider does not define the history permission`() = runTest {
        coEvery { hydration.loadDailyHydration(any(), any()) } returns emptyList()

        val data = loader(granted = everyPermission - historyPermission, additional = emptySet())
            .load(request(ReportMetric.HYDRATION))

        assertEquals(start, data.effectiveStart)
        assertNull(data.truncatedToDays)
        assertFalse(data.historyPermissionMissing)
    }

    @Test fun `no clamp when the range already fits inside 30 days`() = runTest {
        coEvery { hydration.loadDailyHydration(any(), any()) } returns emptyList()

        val data = loader(granted = everyPermission - historyPermission).load(
            ReportRequest(
                metrics = setOf(ReportMetric.HYDRATION),
                granularity = ReportGranularity.DAILY,
                start = end.minusDays(10),
                end = end,
            ),
        )

        assertEquals(end.minusDays(10), data.effectiveStart)
        assertNull(data.truncatedToDays)
        assertFalse(data.historyPermissionMissing)
    }

    @Test fun `a failing group costs its own metrics, not the report`() = runTest {
        coEvery { activity.loadDailySteps(any(), any(), any()) } throws IllegalStateException("boom")
        coEvery { sleep.loadDailySleepDurations(any(), any(), any()) } returns listOf(
            DailySleepDuration(date = end.minusDays(1), durationMs = 7 * 3_600_000L),
        )

        val data = loader(additional = emptySet())
            .load(request(ReportMetric.STEPS, ReportMetric.DISTANCE, ReportMetric.SLEEP))

        assertEquals(ReportMetricStatus.FAILED, data.results.first { it.metric == ReportMetric.STEPS }.status)
        assertEquals(ReportMetricStatus.FAILED, data.results.first { it.metric == ReportMetric.DISTANCE }.status)
        assertEquals(ReportMetricStatus.OK, data.results.first { it.metric == ReportMetric.SLEEP }.status)
        assertFalse(data.cancelled)
    }

    @Test fun `cancelling mid-build marks the remaining metrics SKIPPED`() = runTest {
        val cancellation = ReportCancellation()
        coEvery { activity.loadDailySteps(any(), any(), any()) } coAnswers {
            cancellation.cancel()
            emptyList()
        }

        val data = loader(additional = emptySet()).load(
            request(ReportMetric.STEPS, ReportMetric.SLEEP, ReportMetric.HYDRATION),
            cancellation = cancellation,
        )

        assertTrue(data.cancelled)
        assertEquals(ReportMetricStatus.EMPTY, data.results.first { it.metric == ReportMetric.STEPS }.status)
        assertEquals(ReportMetricStatus.SKIPPED, data.results.first { it.metric == ReportMetric.SLEEP }.status)
        assertEquals(ReportMetricStatus.SKIPPED, data.results.first { it.metric == ReportMetric.HYDRATION }.status)
        coVerify(exactly = 0) { sleep.loadDailySleepDurations(any(), any(), any()) }
        coVerify(exactly = 0) { hydration.loadDailyHydration(any(), any()) }
    }

    @Test fun `progress only ever moves forward and ends at the readable count`() = runTest {
        coEvery { activity.loadDailySteps(any(), any(), any()) } returns emptyList()
        coEvery { sleep.loadDailySleepDurations(any(), any(), any()) } returns emptyList()
        coEvery { hydration.loadDailyHydration(any(), any()) } returns emptyList()
        val seen = mutableListOf<ReportProgress>()

        loader(additional = emptySet()).load(
            request(ReportMetric.STEPS, ReportMetric.DISTANCE, ReportMetric.SLEEP, ReportMetric.HYDRATION),
            onProgress = { seen += it },
        )

        assertTrue(seen.isNotEmpty())
        assertTrue(seen.zipWithNext().all { (a, b) -> b.completed >= a.completed })
        assertEquals(4, seen.last().total)
        assertEquals(4, seen.last().completed)
    }

    @Test fun `sleep drops zero-duration days instead of reporting them as data`() = runTest {
        coEvery { sleep.loadDailySleepDurations(any(), any(), any()) } returns listOf(
            DailySleepDuration(date = end.minusDays(2), durationMs = 0L),
            DailySleepDuration(date = end.minusDays(1), durationMs = 6 * 3_600_000L),
        )

        val data = loader(additional = emptySet()).load(request(ReportMetric.SLEEP))

        val result = data.results.single()
        assertEquals(ReportMetricStatus.OK, result.status)
        assertEquals(1, result.points.size)
        assertEquals(360.0, result.points.single().value, 1e-9)
    }

    @Test fun `blood pressure reads raw entries and keeps every reading in the detail`() = runTest {
        val zone = java.time.ZoneId.systemDefault()
        val day = end.minusDays(1)
        coEvery { vitals.loadBloodPressure(any(), any()) } returns listOf(
            tech.mmarca.openvitals.domain.model.BloodPressureEntry(
                time = day.atTime(8, 0).atZone(zone).toInstant(),
                systolicMmHg = 110,
                diastolicMmHg = 70,
                source = "test",
            ),
            tech.mmarca.openvitals.domain.model.BloodPressureEntry(
                time = day.atTime(21, 0).atZone(zone).toInstant(),
                systolicMmHg = 130,
                diastolicMmHg = 84,
                source = "test",
            ),
            // An exact duplicate of the first record (same data written twice
            // in Health Connect) must not skew the chart or double the list.
            tech.mmarca.openvitals.domain.model.BloodPressureEntry(
                time = day.atTime(8, 0).atZone(zone).toInstant(),
                systolicMmHg = 110,
                diastolicMmHg = 70,
                source = "another-app",
            ),
        )

        val data = loader(additional = emptySet()).load(request(ReportMetric.BLOOD_PRESSURE))

        coVerify(exactly = 1) { vitals.loadBloodPressure(start, end) }
        assertEquals(2, (data.results.single().detail as tech.mmarca.openvitals.domain.model.ReportBloodPressureDetail).readings.size)
        coVerify(exactly = 0) { vitals.loadDailyBloodPressure(any(), any()) }
        val result = data.results.single()
        // Two same-day readings collapse to ONE chart point with real extremes...
        val point = result.points.single()
        assertEquals(120.0, point.value, 1e-9)
        assertEquals(110.0, point.min, 1e-9)
        assertEquals(130.0, point.max, 1e-9)
        assertEquals(77.0, point.secondaryValue!!, 1e-9)
        // ...while the detail keeps both readings and splits the components.
        val detail = result.detail as tech.mmarca.openvitals.domain.model.ReportBloodPressureDetail
        assertEquals(2, detail.readings.size)
        assertEquals(110.0, detail.systolic.min, 1e-9)
        assertEquals(130.0, detail.systolic.max, 1e-9)
        assertEquals(70.0, detail.diastolic.min, 1e-9)
        assertEquals(84.0, detail.diastolic.max, 1e-9)
    }

    @Test fun `glucose reads raw entries and attaches the context detail`() = runTest {
        val zone = java.time.ZoneId.systemDefault()
        coEvery { vitals.loadBloodGlucose(any(), any()) } returns listOf(
            tech.mmarca.openvitals.domain.model.BloodGlucoseEntry(
                time = end.minusDays(1).atTime(8, 0).atZone(zone).toInstant(),
                millimolesPerLiter = 5.0,
                specimenSource = 0,
                mealType = 0,
                relationToMeal = tech.mmarca.openvitals.domain.model.GlucoseRecordValues.RELATION_TO_MEAL_FASTING,
                source = "test",
            ),
            tech.mmarca.openvitals.domain.model.BloodGlucoseEntry(
                time = end.minusDays(1).atTime(21, 0).atZone(zone).toInstant(),
                millimolesPerLiter = 7.4,
                specimenSource = 0,
                mealType = 0,
                relationToMeal = tech.mmarca.openvitals.domain.model.GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL,
                source = "test",
            ),
        )

        val data = loader(additional = emptySet()).load(request(ReportMetric.BLOOD_GLUCOSE))

        coVerify(exactly = 1) { vitals.loadBloodGlucose(start, end) }
        coVerify(exactly = 0) { vitals.loadDailyVitals(any(), any(), any()) }
        val result = data.results.single()
        val point = result.points.single()
        assertEquals(6.2, point.value, 1e-9)
        assertEquals(5.0, point.min, 1e-9)
        assertEquals(7.4, point.max, 1e-9)
        val detail = result.detail as tech.mmarca.openvitals.domain.model.ReportGlucoseDetail
        assertEquals(2, detail.readings.size)
    }

    @Test fun `a CGM-sized glucose range falls back to the daily chart without a detail`() = runTest {
        val zone = java.time.ZoneId.systemDefault()
        coEvery { vitals.loadBloodGlucose(any(), any()) } returns (0 until 600).map { index ->
            tech.mmarca.openvitals.domain.model.BloodGlucoseEntry(
                time = end.minusDays(1).atTime(0, 0).atZone(zone).toInstant().plusSeconds(index * 120L),
                millimolesPerLiter = 5.5,
                specimenSource = 0,
                mealType = 0,
                relationToMeal = tech.mmarca.openvitals.domain.model.GlucoseRecordValues.RELATION_TO_MEAL_GENERAL,
                source = "cgm",
            )
        }
        coEvery { vitals.loadDailyVitals(any(), any(), any()) } returns listOf(
            tech.mmarca.openvitals.domain.model.DailyVitalPoint(end.minusDays(1), 5.5, 600),
        )

        val data = loader(additional = emptySet()).load(request(ReportMetric.BLOOD_GLUCOSE))

        val result = data.results.single()
        assertEquals(ReportMetricStatus.OK, result.status)
        assertNull(result.detail)
        coVerify(exactly = 1) {
            vitals.loadDailyVitals(tech.mmarca.openvitals.data.repository.VitalsPeriodMetric.BLOOD_GLUCOSE, start, end)
        }
    }

    @Test fun `the workout group reads sessions with metrics and attaches the detail`() = runTest {
        val zone = java.time.ZoneId.systemDefault()
        val startTime = end.minusDays(2).atTime(18, 0).atZone(zone).toInstant()
        coEvery { activity.loadWorkoutsWithMetrics(any(), any()) } returns listOf(
            tech.mmarca.openvitals.domain.model.ExerciseData(
                id = "w1",
                title = null,
                exerciseType = 56,
                startTime = startTime,
                endTime = startTime.plusSeconds(1_800),
                durationMs = 1_800_000,
                source = "test",
                totalDistanceMeters = 5_000.0,
            ),
        )

        val data = loader(additional = emptySet()).load(request(ReportMetric.WORKOUT))

        coVerify(exactly = 1) { activity.loadWorkoutsWithMetrics(start, end) }
        coVerify(exactly = 0) { activity.loadWorkouts(any(), any()) }
        val result = data.results.single()
        assertEquals(30.0, result.points.single().value, 1e-9)
        val detail = result.detail as tech.mmarca.openvitals.domain.model.ReportWorkoutsDetail
        assertEquals(5_000.0, detail.sessions.single().distanceMeters!!, 1e-9)
    }

    @Test fun `the sleep group also reads sessions for the schedule detail`() = runTest {
        val zone = java.time.ZoneId.systemDefault()
        val bedtime = end.minusDays(1).atTime(23, 0).atZone(zone).toInstant()
        coEvery { sleep.loadDailySleepDurations(any(), any(), any()) } returns listOf(
            DailySleepDuration(date = end, durationMs = 8 * 3_600_000L),
        )
        coEvery { sleep.loadSleepSessions(any(), any()) } returns listOf(
            tech.mmarca.openvitals.domain.model.SleepData(
                id = "n1",
                startTime = bedtime,
                endTime = bedtime.plusSeconds(8 * 3_600),
                durationMs = 8 * 3_600_000L,
                source = "test",
            ),
        )

        val data = loader(additional = emptySet()).load(request(ReportMetric.SLEEP))

        coVerify(exactly = 1) { sleep.loadSleepSessions(start, end) }
        val detail = data.results.single().detail as tech.mmarca.openvitals.domain.model.ReportSleepDetail
        assertEquals(1, detail.nightsWithData)
    }

    @Test fun `sparse body temperature gets a readings detail, a dense range does not`() = runTest {
        val zone = java.time.ZoneId.systemDefault()
        coEvery { vitals.loadDailyVitals(any(), any(), any()) } returns listOf(
            tech.mmarca.openvitals.domain.model.DailyVitalPoint(end.minusDays(1), 36.8, 2),
        )
        coEvery { vitals.loadBodyTemperature(any(), any()) } returns listOf(
            tech.mmarca.openvitals.domain.model.BodyTempEntry(
                time = end.minusDays(1).atTime(8, 0).atZone(zone).toInstant(),
                temperatureCelsius = 36.8,
                source = "test",
            ),
        )

        val sparse = loader(additional = emptySet()).load(request(ReportMetric.BODY_TEMPERATURE))
        val sparseDetail = sparse.results.single().detail as tech.mmarca.openvitals.domain.model.ReportReadingsDetail
        assertEquals(1, sparseDetail.readings.size)

        coEvery { vitals.loadBodyTemperature(any(), any()) } returns (0 until 300).map { index ->
            tech.mmarca.openvitals.domain.model.BodyTempEntry(
                time = end.minusDays(1).atStartOfDay(zone).toInstant().plusSeconds(index * 200L),
                temperatureCelsius = 36.5,
                source = "wearable",
            )
        }
        val dense = loader(additional = emptySet()).load(request(ReportMetric.BODY_TEMPERATURE))
        assertNull(dense.results.single().detail)
        assertEquals(ReportMetricStatus.OK, dense.results.single().status)
    }

    @Test fun `heart summaries carry their real daily extremes into the points`() = runTest {
        coEvery { heart.loadDailyHeartRateSummaries(any(), any()) } returns listOf(
            HeartRateSummary(date = end.minusDays(1), avgBpm = 70L, minBpm = 45L, maxBpm = 160L),
        )

        val data = loader(additional = emptySet()).load(request(ReportMetric.AVG_HEART_RATE))

        val point = data.results.single().points.single()
        assertEquals(70.0, point.value, 1e-9)
        assertEquals(45.0, point.min, 1e-9)
        assertEquals(160.0, point.max, 1e-9)
    }

    @Test fun `supportedReportMetrics drops metrics the provider cannot serve`() = runTest {
        val loader = loader(managed = everyPermission - bloodPressurePermission)

        val supported = loader.supportedReportMetrics()

        assertFalse(ReportMetric.BLOOD_PRESSURE in supported)
        assertTrue(ReportMetric.STEPS in supported)
        assertTrue(ReportMetric.SLEEP in supported)
    }

    @Test fun `requestablePermissionsFor intersects with the provider's managed set`() = runTest {
        val loader = loader(managed = everyPermission - distancePermission)

        val permissions = loader.requestablePermissionsFor(setOf(ReportMetric.DISTANCE, ReportMetric.STEPS))

        assertTrue(stepsPermission in permissions)
        assertFalse(distancePermission in permissions)
    }

    @Test fun `unavailable Health Connect grants nothing and reads nothing`() = runTest {
        val loader = loader()
        coEvery { hc.availability() } returns HealthConnectAvailability.NOT_SUPPORTED

        val data = loader.load(request(ReportMetric.STEPS, ReportMetric.HYDRATION))

        assertTrue(data.results.all { it.status == ReportMetricStatus.MISSING_PERMISSION })
        coVerify(exactly = 0) { activity.loadDailySteps(any(), any(), any()) }
    }
}
