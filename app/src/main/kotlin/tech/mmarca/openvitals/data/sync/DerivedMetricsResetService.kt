package tech.mmarca.openvitals.data.sync

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.repository.BodyEnergyBaselineCacheStore
import tech.mmarca.openvitals.data.repository.BodyEnergyTimelineStore
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.features.homewidgets.refreshPlacedHomeWidgets

/**
 * The "start over" reset for everything derived and kept outside Health
 * Connect: the Body Energy chain, its baselines and gains, the expenditure
 * cache. Typed values (profile, zones, goals) and raw watch samples stay.
 * [reset] wipes, then rebuilds on its own scope, best-effort.
 */
@Singleton
class DerivedMetricsResetService(
    private val context: Context,
    private val timelineStore: BodyEnergyTimelineStore,
    private val baselineStore: BodyEnergyBaselineCacheStore,
    private val vitalsCacheDao: VitalsDailyCacheDao,
    private val preferencesRepository: PreferencesRepository,
    private val caloriesSync: CaloriesHistorySyncService,
    private val bodyEnergyChainSync: BodyEnergyChainSyncService,
    private val rebuildScope: CoroutineScope,
    private val refreshWidgets: (Context) -> Unit,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        timelineStore: BodyEnergyTimelineStore,
        baselineStore: BodyEnergyBaselineCacheStore,
        vitalsCacheDao: VitalsDailyCacheDao,
        preferencesRepository: PreferencesRepository,
        caloriesSync: CaloriesHistorySyncService,
        bodyEnergyChainSync: BodyEnergyChainSyncService,
    ) : this(
        context = context,
        timelineStore = timelineStore,
        baselineStore = baselineStore,
        vitalsCacheDao = vitalsCacheDao,
        preferencesRepository = preferencesRepository,
        caloriesSync = caloriesSync,
        bodyEnergyChainSync = bodyEnergyChainSync,
        rebuildScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        refreshWidgets = ::refreshPlacedHomeWidgets,
    )

    /** Wipes and returns once landed. The returned [Job] is the rebuild. */
    suspend fun reset(): Job {
        // The chain first: its cursor row is also the throttle and the global signature.
        timelineStore.purgeAll()
        baselineStore.clearBaselines()
        for (key in VitalsCacheKeys.LEGACY_CALORIES_BURNED + VitalsCacheKeys.CALORIES_BURNED) {
            vitalsCacheDao.purgeMetric(key)
        }
        resetLearnedPreferences()
        return rebuildScope.launch { rebuild() }
    }

    /** Back to a fresh install: neutral gains, no evidence, no mirror. Version and epoch go to zero. */
    private fun resetLearnedPreferences() {
        val current = preferencesRepository.bodyEnergyCalibration()
        preferencesRepository.setBodyEnergyCalibration(
            current.copy(
                sleepChargeGain = 1.0,
                activityDrainGain = 1.0,
                basalDrainGain = 1.0,
                stressDrainGain = 1.0,
                watchObservationCount = 0,
            ),
        )
        preferencesRepository.bodyEnergyGainsAlgorithmVersion = 0
        preferencesRepository.bodyEnergyWatchFitEpoch = 0
        preferencesRepository.bodyEnergyWatchFitWatermarkMillis = 0L
        preferencesRepository.bodyEnergyChainSeedMirror = null
        preferencesRepository.bodyEnergyPermissionSignature = null
    }

    /** Called directly, not via the scheduler: its latch has fired. Sequential for Health Connect. */
    private suspend fun rebuild() {
        rebuildStep("calories") { caloriesSync.syncAll() }
        rebuildStep("body energy chain") { bodyEnergyChainSync.syncAll(force = true) }
        runCatching { refreshWidgets(context) }
    }

    private suspend fun rebuildStep(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Log.w(TAG, "Rebuild of $name after the derived-metrics reset failed", t)
        }
    }

    private companion object {
        const val TAG = "DerivedMetricsReset"
    }
}
