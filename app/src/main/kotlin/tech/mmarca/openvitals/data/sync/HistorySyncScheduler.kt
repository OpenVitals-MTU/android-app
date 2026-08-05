package tech.mmarca.openvitals.data.sync

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Drains the caches' changes tokens once per app open, AFTER the first
 * foreground load settles — Health Connect serializes reads, so a drain running
 * beside a screen's own load makes both slower. The services run sequentially
 * for the same reason. Incremental-only: a metric that never full-synced stays
 * untouched until its screen kicks a [VitalsHistorySyncService.syncAll].
 */
@Singleton
class HistorySyncScheduler @Inject constructor(
    private val vitalsSync: VitalsHistorySyncService,
    private val caloriesSync: CaloriesHistorySyncService,
    private val bodyEnergyChainSync: BodyEnergyChainSyncService,
    private val stepDistanceSync: StepDistanceBackfillService,
) {
    private val drained = AtomicBoolean(false)

    suspend fun drainIncrementalOnce() {
        if (!drained.compareAndSet(false, true)) return
        drain { vitalsSync.syncIncremental() }
        drain { caloriesSync.syncIncremental() }
        // After the foreground load has settled: warming the chain is
        // the most read-hungry of the drains, and Health Connect serializes reads
        // — running it beside a screen's own load makes both slower. Its own
        // 30-minute throttle keeps repeat opens cheap.
        drain { bodyEnergyChainSync.syncAll() }
        // Last: the only drain that WRITES to Health Connect. Off unless the
        // user opted into the distance backfill; throttled like the others.
        drain { stepDistanceSync.syncIncremental() }
    }

    /**
     * Runs one drain, letting it fail alone.
     *
     * The drains are independent caches that merely share a slot, and the
     * once-per-open latch is already claimed by the time the first one runs. So
     * an unguarded throw here would not just abandon this open's remaining
     * drains — it would abandon them for the life of the process, leaving two
     * caches stale until the app is killed because a third had a bad day.
     * Cancellation is not a bad day, and must still unwind.
     */
    private suspend fun drain(block: suspend () -> Unit) {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Log.w(TAG, "History drain failed; continuing with the rest", t)
        }
    }

    private companion object {
        private const val TAG = "HistorySyncScheduler"
    }
}
