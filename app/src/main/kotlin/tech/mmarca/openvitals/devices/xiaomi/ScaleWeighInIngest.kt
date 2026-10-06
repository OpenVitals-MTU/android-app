package tech.mmarca.openvitals.devices.xiaomi

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.repository.contract.ScaleWeighInRepository

/** One broadcast as the scan delivered it: who sent it, and the `0xFE95` service data. */
class ScaleAdvert(val address: String, val serviceData: ByteArray)

/**
 * Turns the scale's broadcasts into weigh-ins: decode, fold into Room, then
 * write to Health Connect. Room comes first because the scale says each
 * weigh-in once: whatever stops the write, the reading is kept.
 *
 * The scale sends a weigh-in once it has finished measuring: two frames,
 * each repeated for about a second. A scan that is not running flat out can
 * catch one and miss the other. So each frame is folded in as it comes and
 * the weigh-in is written again if it grows. There is no waiting for it to
 * be complete.
 */
@Singleton
class ScaleWeighInIngest(
    private val store: XiaomiScaleStore,
    private val weighIns: ScaleWeighInRepository,
    private val writer: ScaleWeighInWriter,
    private val listener: XiaomiScaleListener,
    private val scope: CoroutineScope,
    private val now: () -> Instant,
) {

    @Inject
    constructor(
        store: XiaomiScaleStore,
        weighIns: ScaleWeighInRepository,
        writer: ScaleWeighInWriter,
        listener: XiaomiScaleListener,
        dispatchers: DispatcherProvider,
    ) : this(store, weighIns, writer, listener, CoroutineScope(SupervisorJob() + dispatchers.io), Instant::now)

    // Broadcasts arrive faster than a write finishes. One at a time, in order.
    private val ingesting = Mutex()
    private var lastServiceData: ByteArray? = null

    /**
     * The receiver's entry point. [onComplete] ends the broadcast: after the
     * work, or after [budgetMillis] if Health Connect is slow. The budget
     * stops the wait, never the write.
     */
    fun onAdverts(
        adverts: List<ScaleAdvert>,
        budgetMillis: Long = BroadcastBudgetMillis,
        onComplete: () -> Unit,
    ) {
        val work = scope.launch { ingest(adverts) }
        scope.launch {
            withTimeoutOrNull(budgetMillis) { work.join() }
            onComplete()
        }
    }

    suspend fun ingest(adverts: List<ScaleAdvert>) = ingesting.withLock {
        var grew = false
        for (advert in adverts) {
            if (fold(advert)) grew = true
        }
        // A repeat adds nothing, and retrying a refused write on each one would hammer Health Connect.
        if (grew) writer.writePending()
    }

    /** True when the broadcast added something to a weigh-in. */
    private suspend fun fold(advert: ScaleAdvert): Boolean {
        val key = store.bindKey() ?: return false
        val config = store.config.value
        if (config.address != null && !config.address.equals(advert.address, ignoreCase = true)) return false
        // The scale says everything many times over.
        if (advert.serviceData.contentEquals(lastServiceData)) return false
        lastServiceData = advert.serviceData

        val frame = when (val result = S400Beacon.decode(advert.serviceData, advert.address, key)) {
            is S400BeaconResult.Frame -> result.frame
            S400BeaconResult.BadTag -> {
                // The radio drops a corrupt broadcast before it gets here, so this is the wrong key:
                // a mistyped one, or the scale was paired again and given a new one.
                ScaleLog.log("frame heard: the key does not open it")
                store.setKeyRejected(true)
                return false
            }

            S400BeaconResult.NoReading, S400BeaconResult.NotS400 -> return false
        }
        store.setKeyRejected(false)
        if (frame.reading.isEmpty) return false
        ScaleLog.log(if (frame.reading.weightKg != null) "frame heard: weight" else "frame heard: second impedance")

        if (!config.isBound) {
            // The key opened it, so this is the user's scale and, stepping on it now, the user.
            store.bind(advert.address.uppercase(), frame.profile)
            // From here on only this scale's broadcasts need to wake the app.
            listener.arm(restart = true)
        } else if (frame.profile != config.profile) {
            // Someone else in the household. Their measurements are not this phone's to keep.
            store.noteIgnoredProfile(frame.profile, now().toEpochMilli())
            return false
        }
        if (frame.scaleTimestamp == config.deletedScaleTimestamp) return false

        val heardAt = now()
        return weighIns.merge(
            scaleTimestamp = frame.scaleTimestamp,
            profile = frame.profile,
            reading = frame.reading,
            time = scaleRecordTime(frame.scaleTimestamp, heardAt),
            now = heardAt,
        ) != null
    }

    companion object {
        /** A background broadcast may run for about 30 seconds. This leaves room for a cold start. */
        const val BroadcastBudgetMillis = 8_000L
    }
}
