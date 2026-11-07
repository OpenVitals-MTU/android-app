package tech.mmarca.openvitals.devices.xiaomi

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.repository.contract.ScaleWeighInRepository
import tech.mmarca.openvitals.devices.core.pairing.CompanionDevice
import tech.mmarca.openvitals.devices.core.pairing.CompanionDevicePairing
import tech.mmarca.openvitals.devices.core.pairing.CompanionPresenceObserver

/**
 * Keeps the phone listening for the scale. The scan itself lives in the
 * system and outlives the process, but a reboot or Bluetooth going off and
 * on drops it, and no broadcast tells a closed app that Bluetooth is back.
 * So the scan is armed again at every process start and by a periodic
 * worker, which is what catches the morning after a night in airplane mode.
 *
 * A closed app's scan is too slow to catch a result that is on the air for
 * two seconds. So Android is asked to watch for the scale as a companion
 * device: the scale advertises from the moment someone steps on, Android
 * wakes the app, and for the seconds until the result the app holds a
 * foreground service, which is what makes its scan run full time.
 */
@Singleton
class XiaomiScaleListener(
    private val context: Context,
    private val store: XiaomiScaleStore,
    private val radio: ScaleScanRadio,
    private val writer: ScaleWeighInWriter,
    private val companion: CompanionDevicePairing,
    private val weighIns: ScaleWeighInRepository,
    private val foreground: ScaleForeground,
    private val scope: CoroutineScope,
) : CompanionPresenceObserver {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        store: XiaomiScaleStore,
        radio: ScaleScanRadio,
        writer: ScaleWeighInWriter,
        companion: CompanionDevicePairing,
        weighIns: ScaleWeighInRepository,
        foreground: ScaleForeground,
        dispatchers: DispatcherProvider,
    ) : this(
        context,
        store,
        radio,
        writer,
        companion,
        weighIns,
        foreground,
        CoroutineScope(SupervisorJob() + dispatchers.io),
    ) {
        // The app's one instance: the pipeline's debug trail goes to logcat from here on.
        ScaleLog.installLogcatSink()
    }

    /** The weigh-in being waited for, from the scale waking to its result. */
    private var weighInWindow: Job? = null

    private val _status = MutableStateFlow(ScaleListenerStatus.OFF)
    val status: StateFlow<ScaleListenerStatus> = _status.asStateFlow()

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /** Every process start, the one a scale broadcast caused included. Without a scale it does nothing. */
    fun onAppStart() {
        if (!store.config.value.isSetUp) return
        // Runs from Application.onCreate. Not worth failing app start over.
        runCatching {
            arm()
            retryPending()
        }.onFailure { Log.w(TAG, "Could not start listening for the scale: $it") }
    }

    /**
     * Makes sure the scan is running, and says whether it is. Cheap when it
     * already is. [restart] starts it afresh: the system slows a scan that
     * has run for a while, and the Scales screen wants it at full speed.
     */
    fun arm(restart: Boolean = false): ScaleListenerStatus {
        val config = store.config.value
        if (!config.isSetUp) return ScaleListenerStatus.OFF
        val status = radio.arm(config.address, restart)
        _status.value = status
        // Kept whatever the status: with Bluetooth off, the next run is the retry.
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ScaleListenerWorker>(REARM_MINUTES, TimeUnit.MINUTES).build(),
        )
        return status
    }

    /**
     * Android's companion dialog, listing the S400 scales it hears right now:
     * the one the user picks is associated and watched for presence. Null
     * when none is found (the scale must be awake) or the user declines.
     */
    suspend fun findScale(): CompanionDevice? =
        companion.discover(S400Beacon.SERVICE_UUID, S400Beacon.discoveryFilters)

    /** Adds the scale the dialog found and starts listening for it. */
    fun setUp(found: CompanionDevice, name: String, key: ByteArray) {
        store.setUp(found.address, name, key)
        arm(restart = true)
    }

    /** A new key for the same scale, after it was paired again in Xiaomi Home. */
    fun changeKey(key: ByteArray) {
        store.changeKey(key)
    }

    fun rename(name: String) {
        store.rename(name)
    }

    /**
     * Whether Android itself watches for the scale and wakes the app when
     * someone steps on it. Without that the app hears the scale only while it
     * is open: a weigh-in is on the air for two seconds, and a closed app's
     * scan listens a twentieth of the time.
     */
    fun isWokenBySystem(): Boolean = store.config.value.address?.let(companion::isAssociated) ?: false

    /**
     * Asks Android to watch for the scale, through its companion-device
     * dialog. The scale must be advertising for Android to find it, so this
     * follows a weigh-in. False when the user declines or the scale is asleep.
     */
    suspend fun allowSystemWake(): Boolean {
        val address = store.config.value.address ?: return false
        return companion.associate(address)
    }

    /**
     * Android saw the scale start advertising: someone stepped on, and the
     * result follows in seconds. Until it is in, or [WEIGH_IN_WINDOW_MILLIS]
     * pass, the app stays in the foreground and scans afresh, so the scan
     * runs at foreground speed from its first moment.
     */
    override fun onCompanionDeviceAppeared(address: String) {
        if (!address.equals(store.config.value.address, ignoreCase = true)) return
        // Android may report an awake scale more than once. One window per weigh-in.
        if (weighInWindow?.isActive == true) return
        weighInWindow = scope.launch {
            val before = weighIns.latest.first()
            val raised = foreground.raise()
            ScaleLog.log("scale awake: window open, foreground service started=$raised")
            try {
                val status = arm(restart = true)
                ScaleLog.log("scan restarted: $status")
                val complete = withTimeoutOrNull(WEIGH_IN_WINDOW_MILLIS) {
                    // Both frames heard. With socks on the second never comes, and the window runs out.
                    weighIns.latest.first { it != null && it != before && it.reading.impedanceHighOhm != null }
                }
                ScaleLog.log(if (complete != null) "window closed: weigh-in complete" else "window closed: timed out")
            } finally {
                foreground.lower()
            }
        }
    }

    /** Stops listening and forgets the scale and its key. The weigh-ins already saved stay. */
    fun forget() {
        store.config.value.address?.let(companion::disassociate)
        radio.disarm()
        workManager.cancelUniqueWork(WORK_NAME)
        store.clear()
        _status.value = ScaleListenerStatus.OFF
    }

    /** Tries again to save the weigh-ins Health Connect has not taken yet. */
    fun retryPending() {
        scope.launch { writer.writePending() }
    }

    companion object {
        private const val TAG = "ScaleListener"
        const val WORK_NAME = "scale-listener"

        /** How long a dropped scan can go unnoticed while the app stays closed. */
        const val REARM_MINUTES = 30L

        /** A weigh-in takes about twenty seconds from stepping on to the result. This is that, twice over. */
        const val WEIGH_IN_WINDOW_MILLIS = 45_000L
    }
}
