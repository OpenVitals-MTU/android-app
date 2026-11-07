package tech.mmarca.openvitals.devices.garmin

import java.util.Locale

/**
 * What the wrist can ask of the phone's player. The order is the wire
 * format, from upstream's `GarminMusicControlCommand` (AGPLv3).
 */
enum class GarminMusicCommand {
    TOGGLE_PLAY_PAUSE,
    SKIP_TO_NEXT_ITEM,
    SKIP_TO_PREVIOUS_ITEM,
    VOLUME_UP,
    VOLUME_DOWN,
    PLAY,
    PAUSE,
    SKIP_FORWARD,
    SKIP_BACKWARDS,
}

/** What the phone is playing, as the watch's music controls show it. */
data class GarminMusicState(
    val playerName: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val title: String? = null,
    val durationSeconds: Int? = null,
    val playing: Boolean = false,
    /** 1.0 is normal speed. Ignored while paused. */
    val playbackRate: Float = 1f,
    val positionSeconds: Int = 0,
    /** 0.0 to 1.0, or null when unknown. */
    val volume: Float? = null,
)

/** What a session's owner provides for the watch's music controls. */
interface GarminMusicPort {
    /** False answers the watch with no commands, so it offers no controls. */
    val enabled: Boolean

    /** What is playing now, or null when nothing is. */
    fun state(): GarminMusicState?

    fun perform(command: GarminMusicCommand)
}

/** The watch asking which commands the phone takes (5042). */
class GarminMusicCapabilitiesRequest : GarminInboundMessage()

/** A button pressed on the wrist (5041). Null is a command this build does not know. */
data class GarminMusicControl(val command: GarminMusicCommand?) : GarminInboundMessage()

internal fun decodeMusicControl(payload: ByteArray): GarminInboundMessage {
    val ordinal = payload.firstOrNull()?.toInt()?.and(0xFF)
    return GarminMusicControl(ordinal?.let(GarminMusicCommand.entries::getOrNull))
}

/** The answer to 5042: an ACK that carries the commands. A bare ACK offers none. */
fun buildMusicCapabilitiesResponse(commands: List<GarminMusicCommand>): ByteArray {
    val writer = GarminByteWriter()
        .writeShort(GarminMessageId.MUSIC_CONTROL_CAPABILITIES)
        .writeByte(GarminStatus.ACK.code)
        .writeByte(commands.size)
    commands.forEach { writer.writeByte(it.ordinal) }
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}

/**
 * The player, its playback and the track, as 5049 attributes. The layout
 * follows Apple's media service: entity, attribute, flags, then text.
 */
fun buildMusicEntityUpdate(state: GarminMusicState): ByteArray {
    val rate = if (state.playing) state.playbackRate.takeIf { it > 0f } ?: 1f else 0f
    val attributes = buildList {
        add(MusicAttribute(EntityPlayer, PlayerName, state.playerName.orEmpty()))
        add(
            MusicAttribute(
                EntityPlayer, PlayerPlaybackInfo,
                String.format(
                    Locale.ROOT, "%d,%.1f,%.3f",
                    if (state.playing) 1 else 0, rate, state.positionSeconds.toFloat(),
                ),
            ),
        )
        state.volume?.let {
            add(MusicAttribute(EntityPlayer, PlayerVolume, String.format(Locale.ROOT, "%.2f", it.coerceIn(0f, 1f))))
        }
        add(MusicAttribute(EntityTrack, TrackArtist, state.artist.orEmpty()))
        add(MusicAttribute(EntityTrack, TrackAlbum, state.album.orEmpty()))
        add(MusicAttribute(EntityTrack, TrackTitle, state.title.orEmpty()))
        add(MusicAttribute(EntityTrack, TrackDuration, (state.durationSeconds ?: 0).toString()))
    }
    val writer = GarminByteWriter()
    attributes.forEach { attribute ->
        val text = attribute.text.utf8Prefix(MaxAttributeBytes)
        writer.writeByte(text.size + 3) // the three bytes that follow, then the text
            .writeByte(attribute.entity)
            .writeByte(attribute.attribute)
            .writeByte(0) // flags
            .writeBytes(text)
    }
    return GarminGfdiFrame.build(GarminMessageId.MUSIC_CONTROL_ENTITY_UPDATE, writer.toBytes())
}

private class MusicAttribute(val entity: Int, val attribute: Int, val text: String)

/** UTF-8 bytes cut to [max] without splitting a character. */
private fun String.utf8Prefix(max: Int): ByteArray {
    val bytes = toByteArray(Charsets.UTF_8)
    if (bytes.size <= max) return bytes
    var end = max
    // A continuation byte at the cut means the character started before it.
    while (end > 0 && (bytes[end].toInt() and 0xC0) == 0x80) end--
    return bytes.copyOf(end)
}

private const val EntityPlayer = 0
private const val EntityTrack = 2

private const val PlayerName = 0
private const val PlayerPlaybackInfo = 1
private const val PlayerVolume = 2

private const val TrackArtist = 0
private const val TrackAlbum = 1
private const val TrackTitle = 2
private const val TrackDuration = 3

/** The length byte counts three header bytes too. */
private const val MaxAttributeBytes = 252
