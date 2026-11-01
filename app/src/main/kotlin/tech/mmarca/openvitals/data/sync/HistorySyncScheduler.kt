package tech.mmarca.openvitals.data.sync

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import tech.mmarca.openvitals.data.repository.contract.HealthRepository

/**
 * Once per app open, after the first load settles, one after another: drains
 * the vitals change token, warms the Body Energy chain, runs the opt-in
 * step-distance backfill and the opt-in BMR estimate. Incremental only.
 */
@Singleton
class HistorySyncScheduler @Inject constructor(
    private val vitalsSync: VitalsHistorySyncService,
    private val bodyEnergyChainSync: BodyEnergyChainSyncService,
    private val stepDistanceSync: StepDistanceBackfillService,
    private val bmrEstimateSync: BmrEstimateService,
    private val healthRepository: HealthRepository,
) {
    private val drained = AtomicBoolean(false)

    /**
     * [afterCleanLoad] is false when the load that just settled had reads
     * refused. The drains are the most read-hungry thing the app does without
     * being asked; started on top of a spent quota they only deepen the hole,
     * and the next resume finds nothing left for the screen. A deferred drain
     * leaves the once-per-open latch open, so the next settled load runs it.
     */
    suspend fun drainIncrementalOnce(afterCleanLoad: Boolean = true) {
        if (!afterCleanLoad || healthRepository.rateLimitRetryAfterMillis() > 0L) {
            Log.i(TAG, "History drains deferred: Health Connect refused reads of the load before them")
            return
        }
        if (!drained.compareAndSet(false, true)) return
        // The resume that follows an open reloads the screen; its reads come first.
        delay(SettleMillis)
        if (healthRepository.rateLimitRetryAfterMillis() > 0L) {
            Log.i(TAG, "History drains deferred: Health Connect refused reads while they waited")
            drained.set(false)
            return
        }
        drain { vitalsSync.syncIncremental() }
        // After the foreground load: the chain warm is the most read-hungry drain.
        drain { bodyEnergyChainSync.syncAll() }
        // Last: the two drains that write to Health Connect. Off unless opted in.
        drain { stepDistanceSync.syncIncremental() }
        drain { bmrEstimateSync.syncIncremental() }
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

        /** How long after the load settles the drains start. */
        private const val SettleMillis = 20_000L
    }
}
