package tech.mmarca.openvitals.data.repository.dashboard

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.data.repository.toCaffeineEntry
import tech.mmarca.openvitals.domain.insights.CaffeineInsightCalculator
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.mergeLoaded
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/**
 * The dashboard loader cases Flutter's `dashboard_data_loader_test.dart` pins
 * beyond the ones in `data/repository/DashboardDataLoaderTest.kt`: the
 * point-in-time active-caffeine read, the permission set the callout is computed
 * against, and the device-support gating (`supportedMetrics`) the tile mapper
 * now depends on.
 */
class DashboardDataLoaderParityTest {

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val distancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val nutritionPermission = HealthPermission.getReadPermission(NutritionRecord::class)
    private val wheelchairPushesPermission =
        HealthPermission.getReadPermission(WheelchairPushesRecord::class)
    private val mindfulnessPermission =
        HealthPermission.getReadPermission(MindfulnessSessionRecord::class)
    private val bloodGlucosePermission = HealthPermission.getReadPermission(BloodGlucoseRecord::class)
    private val skinTemperaturePermission =
        HealthPermission.getReadPermission(SkinTemperatureRecord::class)
    private val heightPermission = HealthPermission.getReadPermission(HeightRecord::class)

    /** Every read permission the dashboard's metric mapping can ask for. */
    private val allDashboardPermissions: Set<String> = setOf(
        stepsPermission,
        distancePermission,
        nutritionPermission,
        wheelchairPushesPermission,
        mindfulnessPermission,
        bloodGlucosePermission,
        skinTemperaturePermission,
        heightPermission,
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class),
        HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(BodyWaterMassRecord::class),
        HealthPermission.getReadPermission(BoneMassRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(FloorsClimbedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
        HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
        HealthPermission.getReadPermission(OvulationTestRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(Vo2MaxRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
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

    // ─── active caffeine (point-in-time decaying quantity) ────────────────────

    @Test fun `morning carryover from last night is reported for today`() = runTest {
        val entries = listOf(caffeineNutritionEntry(hoursAgo = 8, milligrams = 150.0))
        val hc = caffeineHealthConnect(entries)

        val data = DashboardDataLoader(hc).loadDashboard(caffeineQuery(LocalDate.now()))

        coVerify(exactly = 1) { hc.readNutritionEntries(any(), any()) }
        assertNotNull(data.activeCaffeineMg)
        assertTrue((data.activeCaffeineMg ?: 0.0) > 0.0)
    }

    @Test fun `active caffeine matches the caffeine screen's currentMg for the same inputs`() = runTest {
        val entries = listOf(
            caffeineNutritionEntry(hoursAgo = 8, milligrams = 150.0),
            caffeineNutritionEntry(hoursAgo = 2, milligrams = 80.0),
        )
        val hc = caffeineHealthConnect(entries)

        val data = DashboardDataLoader(hc).loadDashboard(caffeineQuery(LocalDate.now()))

        val today = LocalDate.now()
        val screenCurrentMg = CaffeineInsightCalculator.build(
            entries = entries.mapNotNull { it.toCaffeineEntry() },
            period = DatePeriod(today, today),
            preferences = CaffeinePreferences(),
            now = Instant.now(),
        ).currentMg
        // Active caffeine decays by well under 0.1 mg between the two "now"
        // samples, so the tile and the detail screen agree.
        assertEquals(screenCurrentMg, data.activeCaffeineMg ?: 0.0, 0.1)
    }

    @Test fun `a past day keeps intake semantics - no point-in-time read at all`() = runTest {
        val entries = listOf(caffeineNutritionEntry(hoursAgo = 8, milligrams = 150.0))
        val hc = caffeineHealthConnect(entries)

        val data = DashboardDataLoader(hc).loadDashboard(
            caffeineQuery(LocalDate.now().minusDays(1)),
        )

        coVerify(exactly = 0) { hc.readNutritionEntries(any(), any()) }
        assertNull(data.activeCaffeineMg)
    }

    @Test fun `a hidden metric or a missing permission skips the caffeine read`() = runTest {
        val entries = listOf(caffeineNutritionEntry(hoursAgo = 8, milligrams = 150.0))

        val hidden = caffeineHealthConnect(entries)
        DashboardDataLoader(hidden).loadDashboard(
            DashboardQuery(
                date = LocalDate.now(),
                visibleMetrics = setOf(DashboardMetric.STEPS),
                includeHistoricalBaselines = false,
                includeWeeklyTrainingSignals = false,
            ),
        )
        coVerify(exactly = 0) { hidden.readNutritionEntries(any(), any()) }

        val ungranted = caffeineHealthConnect(entries, granted = emptySet())
        val data = DashboardDataLoader(ungranted).loadDashboard(caffeineQuery(LocalDate.now()))

        coVerify(exactly = 0) { ungranted.readNutritionEntries(any(), any()) }
        assertNull(data.activeCaffeineMg)
    }

    @Test fun `a throwing caffeine read nulls the field, not the dashboard`() = runTest {
        val hc = caffeineHealthConnect(entries = emptyList())
        coEvery { hc.readNutritionEntries(any(), any()) } throws RuntimeException("rate limited")
        coEvery { hc.readSteps(any()) } returns 4_321L

        val data = DashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = LocalDate.now(),
                visibleMetrics = setOf(DashboardMetric.CAFFEINE, DashboardMetric.STEPS),
                includeHistoricalBaselines = false,
                includeWeeklyTrainingSignals = false,
            ),
        )

        assertNull(data.activeCaffeineMg)
        assertEquals(4_321L, data.steps)
    }

    @Test fun `mergeLoaded takes recent history from the pass that loaded the metric`() {
        val date = LocalDate.of(2026, 7, 24)
        val first = DashboardData(
            date = date,
            loadedMetrics = setOf(DashboardMetric.STEPS),
            recentHistoryMetrics = setOf(DashboardMetric.STEPS),
        )
        val second = DashboardData(
            date = date,
            loadedMetrics = setOf(DashboardMetric.SLEEP),
            recentHistoryMetrics = setOf(DashboardMetric.SLEEP),
        )

        // Metrics load a pass each, so a later pass's answer used to be dropped
        // entirely — and an empty-today tile whose metric HAS recent history
        // demoted to the back of the grid as though it never had any.
        assertEquals(
            setOf(DashboardMetric.STEPS, DashboardMetric.SLEEP),
            first.mergeLoaded(second).recentHistoryMetrics,
        )
    }

    @Test fun `mergeLoaded lets a reloaded metric drop its recent history`() {
        val date = LocalDate.of(2026, 7, 24)
        val stale = DashboardData(
            date = date,
            loadedMetrics = setOf(DashboardMetric.SLEEP),
            recentHistoryMetrics = setOf(DashboardMetric.SLEEP),
        )
        val fresh = DashboardData(date = date, loadedMetrics = setOf(DashboardMetric.SLEEP))

        assertEquals(emptySet<DashboardMetric>(), stale.mergeLoaded(fresh).recentHistoryMetrics)
    }

    @Test fun `mergeLoaded carries activeCaffeineMg across passes`() {
        val date = LocalDate.of(2026, 7, 24)
        val first = DashboardData(date = date, loadedMetrics = setOf(DashboardMetric.STEPS))
        val second = DashboardData(
            date = date,
            activeCaffeineMg = 21.0,
            loadedMetrics = setOf(DashboardMetric.CAFFEINE),
        )

        assertEquals(21.0, first.mergeLoaded(second).activeCaffeineMg ?: 0.0, 1e-9)
    }

    // ─── permissions and device support ───────────────────────────────────────

    @Test fun `omits permissions the installed provider cannot grant`() = runTest {
        // The provider does not define WHEELCHAIR_PUSHES (the app's connect
        // client is newer than it) and the mindfulness feature is unavailable —
        // neither permission can ever be granted, and `grantedPermissions()`
        // cannot even report them, so requiring them would strand the
        // dashboard's permission callout on an ungrantable set.
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns setOf(stepsPermission)
        coEvery { hc.grantedPermissions() } returns setOf(stepsPermission)
        coEvery { hc.readSteps(any()) } returns 1_000L

        val data = DashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = LocalDate.of(2026, 1, 2),
                visibleMetrics = setOf(
                    DashboardMetric.STEPS,
                    DashboardMetric.WHEELCHAIR_PUSHES,
                    DashboardMetric.MINDFULNESS,
                ),
                includeHistoricalBaselines = false,
                includeWeeklyTrainingSignals = false,
            ),
        )

        assertEquals(emptySet<String>(), data.missingPermissions)
    }

    @Test fun `supportedMetrics drops metrics the provider cannot serve`() = runTest {
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns allDashboardPermissions - setOf(
            wheelchairPushesPermission,
            bloodGlucosePermission,
            // Feature-flagged permissions are absent from `managedPermissions`
            // when the device does not offer them.
            skinTemperaturePermission,
            mindfulnessPermission,
        )
        coEvery { hc.grantedPermissions() } returns emptySet()

        val data = DashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = LocalDate.of(2026, 1, 2),
                visibleMetrics = setOf(DashboardMetric.STEPS),
                includeHistoricalBaselines = false,
                includeWeeklyTrainingSignals = false,
            ),
        )

        // Reported for every metric, not just the queried ones — the dashboard
        // uses it to decide which tiles exist at all.
        val supported = data.supportedMetrics
        assertNotNull(supported)
        assertTrue(DashboardMetric.STEPS in supported!!)
        assertTrue(DashboardMetric.DISTANCE in supported)
        assertFalse(DashboardMetric.WHEELCHAIR_PUSHES in supported)
        assertFalse(DashboardMetric.BLOOD_GLUCOSE in supported)
        assertFalse(DashboardMetric.SKIN_TEMPERATURE in supported)
        assertFalse(DashboardMetric.MINDFULNESS in supported)
    }

    @Test fun `a multi-permission metric needs all of its permissions supported`() = runTest {
        // BMI reads weight + height; dropping height alone must unsupport it.
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns allDashboardPermissions - heightPermission
        coEvery { hc.grantedPermissions() } returns emptySet()

        val data = DashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = LocalDate.of(2026, 1, 2),
                visibleMetrics = setOf(DashboardMetric.STEPS),
                includeHistoricalBaselines = false,
                includeWeeklyTrainingSignals = false,
            ),
        )

        val supported = data.supportedMetrics
        assertNotNull(supported)
        assertTrue(DashboardMetric.WEIGHT in supported!!)
        assertFalse(DashboardMetric.HEIGHT in supported)
        assertFalse(DashboardMetric.BMI in supported)
        assertFalse(DashboardMetric.FFMI in supported)
    }

    @Test fun `returns an empty granted set when Health Connect is unavailable`() = runTest {
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.NOT_SUPPORTED
        every { hc.managedPermissions } returns allDashboardPermissions

        val data = DashboardDataLoader(hc).loadDashboard(
            DashboardQuery(
                date = LocalDate.of(2026, 1, 2),
                visibleMetrics = setOf(DashboardMetric.STEPS),
                includeHistoricalBaselines = false,
                includeWeeklyTrainingSignals = false,
            ),
        )

        // The availability gate short-circuits granted permissions, so nothing
        // is read and every permission the day needs reads as missing.
        assertEquals(0L, data.steps)
        assertTrue(stepsPermission in data.missingPermissions)
        coVerify(exactly = 0) { hc.grantedPermissions() }
    }

    // ─── fixtures ─────────────────────────────────────────────────────────────

    private fun caffeineQuery(date: LocalDate) = DashboardQuery(
        date = date,
        visibleMetrics = setOf(DashboardMetric.CAFFEINE),
        includeHistoricalBaselines = false,
        includeWeeklyTrainingSignals = false,
    )

    private fun caffeineHealthConnect(
        entries: List<NutritionEntry>,
        granted: Set<String> = setOf(nutritionPermission, stepsPermission),
    ): HealthConnectManager {
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.managedPermissions } returns allDashboardPermissions
        coEvery { hc.grantedPermissions() } returns granted
        coEvery { hc.readNutritionEntries(any(), any()) } returns entries
        coEvery { hc.readDailyMacros(any(), any()) } returns emptyList()
        coEvery { hc.readSteps(any()) } returns 0L
        return hc
    }

    private fun caffeineNutritionEntry(hoursAgo: Long, milligrams: Double) = NutritionEntry(
        time = Instant.now().minus(Duration.ofHours(hoursAgo)),
        mealType = 0,
        name = "Coffee",
        energyKcal = null,
        proteinGrams = null,
        carbsGrams = null,
        fatGrams = null,
        fiberGrams = null,
        sugarGrams = null,
        source = "test",
        nutrientValues = mapOf(NutritionNutrient.CAFFEINE to milligrams / 1000.0),
        id = "caffeine-$hoursAgo",
    )
}
