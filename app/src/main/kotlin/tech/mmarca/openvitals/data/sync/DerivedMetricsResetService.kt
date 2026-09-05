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
 * The "start over" reset for everything OpenVitals derives on its own and keeps
 * outside Health Connect: the Body Energy chain, the baselines and learned
 * gains it is tuned by, and the expenditure day cache. Recovery and readiness
 * have no storage of their own — they are recomputed from these plus live
 * Health Connect reads — so wiping this set is what puts them back to a fresh
 * install.
 *
 * Deliberately NOT touched, because the user typed them rather than the app
 * learning them: the body profile, manual heart zones, the setup-completed flag,
 * daily goals, and the unrelated vitals caches (SpO2, blood pressure, ...).
 * Nothing here writes to Health Connect, and the raw watch wellness samples
 * (source data the app cannot re-read from anywhere) stay too.
 *
 * [reset] wipes synchronously and then kicks the rebuilds on the service's own
 * scope: a full calories rebuild is a two-year Health Connect read, and it must
 * outlive the settings screen that asked for it. The rebuild is best-effort like
 * every other history sync — the screens recompute on open regardless, so a
 * failed rebuild only costs the first open its cache.
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

    /**
     * Wipes the derived state and returns once the wipe has landed; the rebuild
     * runs on afterwards. The returned [Job] is the rebuild, for callers (tests)
     * that want to wait for it.
     */
    suspend fun reset(): Job {
        // The chain first: its cursor row is also the warm-pass throttle and the
        // stored global signature, so dropping it is what makes the next pass
        // a full one.
        timelineStore.purgeAll()
        baselineStore.clearBaselines()
        for (key in VitalsCacheKeys.LEGACY_CALORIES_BURNED + VitalsCacheKeys.CALORIES_BURNED) {
            vitalsCacheDao.purgeMetric(key)
        }
        resetLearnedPreferences()
        return rebuildScope.launch { rebuild() }
    }

    /**
     * Back to what a fresh install reads: neutral gains, no watch evidence
     * consumed, no mirrored seed, no remembered permission set. The version and
     * epoch go to zero rather than to the current values so the repository
     * stamps them itself on the next load, exactly as it does on first run.
     */
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

    /**
     * Called directly rather than through [HistorySyncScheduler]: its
     * once-per-open latch has already fired, and it is incremental-only anyway
     * — a cache with no cursor stays empty until something asks for a full
     * sync. Sequential, because Health Connect serializes reads.
     */
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
