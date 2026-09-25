package tech.mmarca.openvitals.data.sync

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Once per app open, after the first load settles, one after another: drains
 * the vitals change token, warms the Body Energy chain, runs the opt-in
 * step-distance backfill. Incremental only.
 */
@Singleton
class HistorySyncScheduler @Inject constructor(
    private val vitalsSync: VitalsHistorySyncService,
    private val bodyEnergyChainSync: BodyEnergyChainSyncService,
    private val stepDistanceSync: StepDistanceBackfillService,
) {
    private val drained = AtomicBoolean(false)

    suspend fun drainIncrementalOnce() {
        if (!drained.compareAndSet(false, true)) return
        drain { vitalsSync.syncIncremental() }
        // After the foreground load: the chain warm is the most read-hungry drain.
        drain { bodyEnergyChainSync.syncAll() }
        // Last: the only drain that writes to Health Connect. Off unless opted in.
        drain { stepDistanceSync.syncIncremental() }
    }

    /** Runs one drain, letting it fail alone; the once-per-open latch is already claimed. */
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
