package tech.mmarca.openvitals.devices.core.sync

import kotlin.time.Duration
import tech.mmarca.openvitals.domain.model.BleSensorDevice

/**
 * How far a device sync has got, reported as it runs. Device-agnostic — the
 * integration maps its own protocol phases onto these.
 */
enum class DeviceSyncPhase { HANDSHAKE, LISTING, DOWNLOADING, COMPLETE, FAILED }

/**
 * A progress tick from an in-flight sync: the [phase] and, while downloading,
 * how many of [filesTotal] files are [filesDone].
 */
data class DeviceSyncProgress(
    val phase: DeviceSyncPhase,
    val filesTotal: Int = 0,
    val filesDone: Int = 0,
)

/** The outcome of a whole sync-and-persist run. */
sealed class DeviceSyncResult {

    /**
     * The sync finished; [fileCount] files were downloaded and handed on (0 is
     * a success — the watch simply had nothing new).
     */
    data class Succeeded(val fileCount: Int) : DeviceSyncResult()

    /**
     * The sync failed. [message] is already a rendered, integration-agnostic
     * string — the seam never leaks the integration's exception type.
     */
    data class Failed(val message: String) : DeviceSyncResult()
}

/**
 * The seam between the app's generic sync orchestration and one integration's
 * sync implementation. Owns the WHOLE operation for a device — pull, import,
 * persist, stamp — reporting progress as it goes, so a second integration
 * (WearOS, …) plugs in without the caller naming any protocol.
 *
 * A **port**, like [tech.mmarca.openvitals.devices.garmin.GarminTransportProbe]:
 * features depend on this type, and only DI knows which integration satisfies
 * it.
 */
interface DeviceSyncPort {

    /** Whether this integration owns the sync-and-persist for [device]. */
    fun canSync(device: BleSensorDevice): Boolean

    /**
     * Runs the whole pull → import → store → stamp sequence for [device],
     * reporting progress via [onProgress]. Never throws — a failed sync comes
     * back as [DeviceSyncResult.Failed].
     *
     * [listenAfter] is a diagnostic: hold the link open that long after the
     * sync to see what the watch sends unprompted.
     */
    suspend fun sync(
        device: BleSensorDevice,
        listenAfter: Duration = Duration.ZERO,
        onProgress: ((DeviceSyncProgress) -> Unit)? = null,
    ): DeviceSyncResult
}
