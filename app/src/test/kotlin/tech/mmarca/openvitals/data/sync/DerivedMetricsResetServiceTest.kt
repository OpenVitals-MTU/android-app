package tech.mmarca.openvitals.data.sync

import android.content.Context
import android.util.Log
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.local.bodyenergy.FakeBodyEnergyTimelineDao
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.repository.BodyEnergyBaselineCacheStore
import tech.mmarca.openvitals.data.repository.BodyEnergyTimelineStore
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.inMemoryPreferences
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds

/** The reset leaves nothing to seed from, leaves everything the user typed, and kicks the rebuilds itself. */
class DerivedMetricsResetServiceTest {

    private val manualZones = HeartZoneThresholds(
        zone1LowerBpm = 100,
        zone2LowerBpm = 120,
        zone3LowerBpm = 140,
        zone4LowerBpm = 160,
        zone5LowerBpm = 175,
    )

    private lateinit var dao: FakeBodyEnergyTimelineDao
    private lateinit var store: BodyEnergyTimelineStore
    private lateinit var baselines: BodyEnergyBaselineCacheStore
    private lateinit var vitalsCache: VitalsDailyCacheDao
    private lateinit var prefs: PreferencesRepository
    private lateinit var caloriesSync: CaloriesHistorySyncService
    private lateinit var chainSync: BodyEnergyChainSyncService
    private val purgedMetrics = mutableListOf<String>()
    private val rebuildOrder = mutableListOf<String>()
    private var widgetRefreshes = 0

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        dao = FakeBodyEnergyTimelineDao()
        store = BodyEnergyTimelineStore(dao)
        baselines = mockk { every { clearBaselines() } just Runs }
        vitalsCache = mockk {
            coEvery { purgeMetric(any()) } answers { purgedMetrics += firstArg<String>() }
        }
        prefs = inMemoryPreferences(
            calibration = BodyEnergyCalibration(
                manualZoneThresholdsBpm = manualZones,
                useManualZones = true,
                setupCompleted = true,
                sleepChargeGain = 1.3,
                activityDrainGain = 0.8,
                basalDrainGain = 1.1,
                stressDrainGain = 0.9,
                watchObservationCount = 12,
            ),
        )
        prefs.bodyEnergyGainsAlgorithmVersion = 11
        prefs.bodyEnergyWatchFitEpoch = 2
        prefs.bodyEnergyWatchFitWatermarkMillis = 1_700_000_000_000L
        prefs.bodyEnergyChainSeedMirror = "20600|62|55|1"
        prefs.bodyEnergyPermissionSignature = 4242
        caloriesSync = mockk { coEvery { syncAll() } answers { rebuildOrder += "calories" } }
        chainSync = mockk { coEvery { syncAll(force = true) } answers { rebuildOrder += "chain" } }
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun kotlinx.coroutines.test.TestScope.service() = DerivedMetricsResetService(
        context = mockk<Context>(),
        timelineStore = store,
        baselineStore = baselines,
        vitalsCacheDao = vitalsCache,
        preferencesRepository = prefs,
        caloriesSync = caloriesSync,
        bodyEnergyChainSync = chainSync,
        rebuildScope = this,
        refreshWidgets = { widgetRefreshes++ },
    )

    @Test
    fun `wipes the chain, its cursor and the baselines`() = runTest {
        store.writeGlobalSignature("v11|abc|def|ghi")
        val service = service()

        service.reset()

        assertNull(store.storedGlobalSignature())
        assertEquals(0, dao.countDays())
        coVerify(exactly = 1) { baselines.clearBaselines() }
    }

    @Test
    fun `purges the calories cache under its current and legacy keys, nothing else`() = runTest {
        service().reset()

        assertEquals(
            VitalsCacheKeys.LEGACY_CALORIES_BURNED + VitalsCacheKeys.CALORIES_BURNED,
            purgedMetrics,
        )
    }

    @Test
    fun `learned tuning goes back to a fresh install but the user's own settings stay`() = runTest {
        service().reset()

        val calibration = prefs.bodyEnergyCalibration()
        assertFalse(calibration.hasPersonalGains)
        assertFalse(calibration.hasWatchObservations)
        assertEquals(manualZones, calibration.manualZoneThresholdsBpm)
        assertTrue(calibration.useManualZones)
        assertTrue(calibration.setupCompleted)

        assertEquals(0, prefs.bodyEnergyGainsAlgorithmVersion)
        assertEquals(0, prefs.bodyEnergyWatchFitEpoch)
        assertEquals(0L, prefs.bodyEnergyWatchFitWatermarkMillis)
        assertNull(prefs.bodyEnergyChainSeedMirror)
        assertNull(prefs.bodyEnergyPermissionSignature)
    }

    @Test
    fun `rebuilds calories then the chain, forced, and repaints the widgets`() = runTest {
        val service = service()

        val rebuild = service.reset()
        assertTrue(rebuildOrder.isEmpty())
        advanceUntilIdle()
        rebuild.join()

        assertEquals(listOf("calories", "chain"), rebuildOrder)
        assertEquals(1, widgetRefreshes)
    }

    @Test
    fun `a failed rebuild step does not stop the others`() = runTest {
        coEvery { caloriesSync.syncAll() } throws IllegalStateException("rate limited")
        val service = service()

        service.reset().join()

        assertEquals(listOf("chain"), rebuildOrder)
        assertEquals(1, widgetRefreshes)
    }
}
