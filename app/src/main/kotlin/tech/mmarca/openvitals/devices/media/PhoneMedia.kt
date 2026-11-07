package tech.mmarca.openvitals.devices.media

/** What the phone's foremost player is doing. Device-agnostic: a watch integration maps it. */
data class PhoneMediaState(
    /** The player app's name, or null when it cannot be read. */
    val playerName: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val title: String? = null,
    val durationSeconds: Int? = null,
    val playing: Boolean = false,
    /** 1.0 is normal speed. */
    val playbackRate: Float = 1f,
    /** Where playback is now, not where the player last reported it. */
    val positionSeconds: Int = 0,
    /** Media volume, 0.0 to 1.0. */
    val volume: Float? = null,
)

/** What a wrist can ask of the phone's player. */
enum class PhoneMediaCommand {
    TOGGLE_PLAY_PAUSE,
    NEXT,
    PREVIOUS,
    VOLUME_UP,
    VOLUME_DOWN,
    PLAY,
    PAUSE,
    FAST_FORWARD,
    REWIND,
}

/**
 * The phone's media sessions, behind an interface so the relay is
 * JVM-testable. Reading them needs Android's notification access.
 */
interface PhoneMediaSource {

    /** Whether Android granted notification access, which media sessions sit behind. */
    fun hasAccess(): Boolean

    /**
     * Follows the foremost player and reports every change. False when
     * access is missing; the caller may try again later. Idempotent.
     */
    fun start(onChanged: (PhoneMediaState?) -> Unit): Boolean

    fun stop()

    /** What is playing now, or null when no player has a session. */
    fun current(): PhoneMediaState?

    fun perform(command: PhoneMediaCommand)
}
