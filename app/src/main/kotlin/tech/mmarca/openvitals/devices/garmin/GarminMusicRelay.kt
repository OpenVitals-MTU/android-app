package tech.mmarca.openvitals.devices.garmin

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.media.PhoneMediaCommand
import tech.mmarca.openvitals.devices.media.PhoneMediaSource
import tech.mmarca.openvitals.devices.media.PhoneMediaState

/**
 * Puts the phone's player on a Garmin watch's music controls: what is
 * playing goes to the wrist, and the wrist's buttons come back as commands.
 * It follows the player only while the per-watch switch is on and a Garmin
 * watch is paired. Everything rides the held link; without one, nothing is sent.
 */
@Singleton
class GarminMusicRelay(
    private val deviceRepository: BleDeviceRepository,
    private val stateStore: GarminDeviceStateStore,
    private val media: PhoneMediaSource,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
) : GarminMusicPort {

    @Inject
    constructor(
        deviceRepository: BleDeviceRepository,
        stateStore: GarminDeviceStateStore,
        media: PhoneMediaSource,
    ) : this(
        deviceRepository,
        stateStore,
        media,
        CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1)),
        SystemClock::elapsedRealtime,
    )

    private val policy = GarminMusicRelayPolicy()
    private var started = false

    /** Where a change goes: the bridge, which owns the link. */
    @Volatile
    var onState: ((GarminMusicState) -> Unit)? = null

    /** Starts following pairings. Called once from `onCreate`. */
    fun start() {
        if (started) return
        started = true
        // Pairing or forgetting a watch changes the answer as much as the switch.
        scope.launch { deviceRepository.devicesFlow.collect { syncSource() } }
    }

    /** Called when the switch changes. */
    fun onEnabledChanged(deviceId: String, enabled: Boolean) {
        stateStore.setMusicControls(deviceId, enabled)
        syncSource()
    }

    /** Whether Android lets this app see the phone's players. */
    fun hasAccess(): Boolean = media.hasAccess()

    /** Tries again after the user came back from Android's settings. */
    fun refreshAccess() = syncSource()

    override val enabled: Boolean
        get() = wantsMusicOnWatch()

    /** Never null while enabled: no player is an empty state, which clears the wrist. */
    override fun state(): GarminMusicState? {
        if (!wantsMusicOnWatch()) return null
        // The watch asking is a good moment to retry a source that had no access.
        syncSource()
        val state = media.current().toGarmin()
        policy.sent(state, clock())
        return state
    }

    override fun perform(command: GarminMusicCommand) {
        if (!wantsMusicOnWatch()) return
        media.perform(command.toPhone())
    }

    private fun syncSource() {
        if (!wantsMusicOnWatch()) {
            media.stop()
            policy.reset()
            return
        }
        media.start(::relay)
    }

    private fun relay(phone: PhoneMediaState?) {
        val state = phone.toGarmin()
        if (!policy.shouldSend(state, clock())) return
        GarminLog.log("[GARMIN-MUSIC] player changed; ${if (state.playing) "playing" else "paused"}")
        onState?.invoke(state)
    }

    private fun wantsMusicOnWatch(): Boolean {
        val watch = deviceRepository.devices.firstOrNull { it.isGarminGfdi } ?: return false
        return stateStore.musicControls(watch.id)
    }
}

/**
 * Decides which player changes reach the watch. A player reports its
 * position again and again; the watch runs its own clock from the last one,
 * so only a jump is news.
 */
internal class GarminMusicRelayPolicy(
    private val seekToleranceSeconds: Int = 3,
) {
    private var last: GarminMusicState? = null
    private var lastAtMillis: Long = 0

    @Synchronized
    fun shouldSend(state: GarminMusicState, nowMillis: Long): Boolean {
        val previous = last
        val changed = previous == null ||
            state.copy(positionSeconds = 0) != previous.copy(positionSeconds = 0) ||
            abs(state.positionSeconds - expectedPosition(previous, nowMillis)) > seekToleranceSeconds
        if (changed) sent(state, nowMillis)
        return changed
    }

    /** Records a state that went out some other way. */
    @Synchronized
    fun sent(state: GarminMusicState, nowMillis: Long) {
        last = state
        lastAtMillis = nowMillis
    }

    @Synchronized
    fun reset() {
        last = null
    }

    private fun expectedPosition(previous: GarminMusicState, nowMillis: Long): Int {
        if (!previous.playing) return previous.positionSeconds
        val elapsedSeconds = (nowMillis - lastAtMillis) / 1000f * previous.playbackRate
        return previous.positionSeconds + elapsedSeconds.toInt()
    }
}

private fun PhoneMediaState?.toGarmin(): GarminMusicState {
    if (this == null) return GarminMusicState()
    return GarminMusicState(
        playerName = playerName,
        artist = artist,
        album = album,
        title = title,
        durationSeconds = durationSeconds,
        playing = playing,
        playbackRate = playbackRate,
        positionSeconds = positionSeconds,
        volume = volume,
    )
}

private fun GarminMusicCommand.toPhone(): PhoneMediaCommand = when (this) {
    GarminMusicCommand.TOGGLE_PLAY_PAUSE -> PhoneMediaCommand.TOGGLE_PLAY_PAUSE
    GarminMusicCommand.SKIP_TO_NEXT_ITEM -> PhoneMediaCommand.NEXT
    GarminMusicCommand.SKIP_TO_PREVIOUS_ITEM -> PhoneMediaCommand.PREVIOUS
    GarminMusicCommand.VOLUME_UP -> PhoneMediaCommand.VOLUME_UP
    GarminMusicCommand.VOLUME_DOWN -> PhoneMediaCommand.VOLUME_DOWN
    GarminMusicCommand.PLAY -> PhoneMediaCommand.PLAY
    GarminMusicCommand.PAUSE -> PhoneMediaCommand.PAUSE
    GarminMusicCommand.SKIP_FORWARD -> PhoneMediaCommand.FAST_FORWARD
    GarminMusicCommand.SKIP_BACKWARDS -> PhoneMediaCommand.REWIND
}
