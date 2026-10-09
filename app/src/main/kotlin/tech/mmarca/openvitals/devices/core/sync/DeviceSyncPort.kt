package tech.mmarca.openvitals.devices.core.sync

import kotlin.time.Duration
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.model.BleSensorDevice

/** How far a sync has got. Device-agnostic. */
enum class DeviceSyncPhase { HANDSHAKE, LISTING, DOWNLOADING, COMPLETE, FAILED }

/** A progress tick: the [phase] and, while downloading, [filesDone] of [filesTotal]. */
data class DeviceSyncProgress(
    val phase: DeviceSyncPhase,
    val filesTotal: Int = 0,
    val filesDone: Int = 0,
)

/** The outcome of a whole sync-and-persist run. */
sealed class DeviceSyncResult {

    /**
     * The sync finished with [fileCount] files, or samples for an
     * integration that counts those, and [nightCount] estimated nights of
     * sleep written. 0 is a success.
     */
    data class Succeeded(val fileCount: Int, val nightCount: Int = 0) : DeviceSyncResult()

    /**
     * The sync failed. [error] is what the screen shows; an integration with
     * a sentence already in hand passes it as a [ScreenError.Message], one
     * with a typed cause passes a [ScreenError.Text] or [ScreenError.PermissionDenied].
     */
    data class Failed(val error: ScreenError) : DeviceSyncResult() {
        constructor(message: String) : this(ScreenError.Message(message))

        /** The rendered sentence, for a message-shaped error; the resource id's name otherwise. */
        val message: String
            get() = (error as? ScreenError.Message)?.text ?: error.toString()
    }
}

/**
 * The seam between generic sync orchestration and one integration. Owns
 * the whole operation: pull, import, persist, stamp. A port: only DI knows
 * which integration satisfies it.
 */
interface DeviceSyncPort {

    /** Whether this integration owns the sync-and-persist for [device]. */
    fun canSync(device: BleSensorDevice): Boolean

    /**
     * Runs the whole sequence for [device]. Never throws. [listenAfter] is
     * a diagnostic window held open after the sync.
     */
    suspend fun sync(
        device: BleSensorDevice,
        listenAfter: Duration = Duration.ZERO,
        onProgress: ((DeviceSyncProgress) -> Unit)? = null,
    ): DeviceSyncResult
}
