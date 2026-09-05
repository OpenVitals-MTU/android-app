package tech.mmarca.openvitals.devices.garmin

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Garmin's notification service (GNCS) vocabulary and the encoder for an
 * attribute blob. GNCS is a pull protocol: the phone announces an id and a
 * category (5033), the watch asks for attributes (5034), then the text goes
 * across (5035). If the watch never asks, nothing is wrong.
 *
 * I/O-free. Framing and inbound decoders live in `GarminMessages.kt`.
 * Ported from Gadgetbridge (AGPLv3) via the Flutter build.
 */

/** Add, update or withdraw a notification. Ordinal is the wire value; do not reorder. */
enum class GarminNotificationUpdateType { ADD, MODIFY, REMOVE }

/**
 * The notification's kind. Ordinal is the wire value; do not reorder. The
 * watch groups by it, and some faces show a per-category count.
 */
enum class GarminNotificationCategory {
    OTHER, // 0
    INCOMING_CALL,
    MISSED_CALL,
    VOICEMAIL,
    SOCIAL,
    SCHEDULE,
    EMAIL,
    NEWS,
    HEALTH_AND_FITNESS,
    BUSINESS_AND_FINANCE,
    LOCATION,
    ENTERTAINMENT,
    SMS, // 12
}

/** Flags on an announcement's category byte. Bit is `1 shl ordinal`. */
enum class GarminNotificationFlag {
    BACKGROUND,
    FOREGROUND,
    UNK,
    ACTION_ACCEPT, // legacy actions only
    ACTION_DECLINE, // legacy actions only
    ;

    val bit: Int get() = 1 shl ordinal
}

/** What the phone can offer for this notification. Bit is `1 shl ordinal`. */
enum class GarminNotificationPhoneFlag {
    LEGACY_ACTIONS,
    NEW_ACTIONS,
    HAS_ATTACHMENTS,
    ;

    val bit: Int get() = 1 shl ordinal
}

/**
 * The category-flags byte. FOREGROUND and ACTION_DECLINE always, verbatim
 * from Gadgetbridge: background notifications "were generating bug reports".
 */
fun garminNotificationCategoryFlags(): Int =
    GarminNotificationFlag.FOREGROUND.bit or GarminNotificationFlag.ACTION_DECLINE.bit

/**
 * Commands on the notification control channel (5034). [GET_APP_ATTRIBUTES]
 * is untested and not acted on. Both action commands are honoured.
 */
enum class GarminNotificationCommand(val code: Int) {
    GET_NOTIFICATION_ATTRIBUTES(0),
    GET_APP_ATTRIBUTES(1),
    PERFORM_LEGACY_NOTIFICATION_ACTION(2),
    PERFORM_NOTIFICATION_ACTION(128),
    ;

    companion object {
        fun fromCode(code: Int): GarminNotificationCommand? =
            entries.firstOrNull { it.code == code }
    }
}

/**
 * One field the watch can ask for. [hasLengthParam] and
 * [hasAdditionalParams] say how many bytes follow the id in a request.
 * Codes 0-7 are ANCS; 127 and 128 are Garmin's.
 */
enum class GarminNotificationAttribute(
    val code: Int,
    /** A `u16` maximum length follows this attribute's id in a request. */
    val hasLengthParam: Boolean = false,
    /** A `u16` follows, then one unidentified byte. Gadgetbridge reads it too. */
    val hasAdditionalParams: Boolean = false,
) {
    APP_IDENTIFIER(0),
    TITLE(1, hasLengthParam = true),
    SUBTITLE(2, hasLengthParam = true),
    MESSAGE(3, hasLengthParam = true),
    MESSAGE_SIZE(4),
    DATE(5),
    NEGATIVE_ACTION_LABEL(7), // legacy actions only
    ACTIONS(127, hasAdditionalParams = true), // Garmin extension
    ATTACHMENTS(128), // Garmin extension
    ;

    companion object {
        fun fromCode(code: Int): GarminNotificationAttribute? =
            entries.firstOrNull { it.code == code }
    }
}

/** The watch's verdict on one chunk. Ordinal is the wire value; do not reorder. */
enum class GarminNotificationTransferStatus {
    OK,
    RESEND,
    ABORT,
    CRC_MISMATCH,
    OFFSET_MISMATCH,
    ;

    companion object {
        fun fromOrdinal(ordinal: Int): GarminNotificationTransferStatus =
            if (ordinal in entries.indices) entries[ordinal] else ABORT
    }
}

/**
 * What a wearer can do to a notification. The code is the wire value and
 * fixes where the watch draws the control.
 */
enum class GarminNotificationActionKind(
    val code: Int,
    val iconPosition: GarminActionIconPosition? = null,
) {
    CUSTOM_1(1),
    CUSTOM_2(2),
    CUSTOM_3(3),
    CUSTOM_4(4),
    CUSTOM_5(5),
    REPLY_INCOMING_CALL(94, GarminActionIconPosition.BOTTOM),
    REPLY(95, GarminActionIconPosition.BOTTOM),
    ACCEPT_CALL(96, GarminActionIconPosition.RIGHT),
    REJECT_CALL(97, GarminActionIconPosition.LEFT),
    DISMISS(98, GarminActionIconPosition.LEFT),
    BLOCK_APPLICATION(99),
    ;

    companion object {
        /** The five slots an app's own buttons are handed out from, in order. */
        val customSlots: List<GarminNotificationActionKind> =
            listOf(CUSTOM_1, CUSTOM_2, CUSTOM_3, CUSTOM_4, CUSTOM_5)

        fun fromCode(code: Int): GarminNotificationActionKind? =
            entries.firstOrNull { it.code == code }
    }
}

/**
 * Where the watch draws an action's control. Bit is `1 shl ordinal`.
 * Gadgetbridge's "educated guesses"; nobody has the documentation.
 */
enum class GarminActionIconPosition {
    BOTTOM,
    RIGHT,
    LEFT,
    ;

    val bit: Int get() = 1 shl ordinal
}

/** One action offered on the wrist. */
data class GarminNotificationAction(
    val kind: GarminNotificationActionKind,
    /** The posting app's label. Shown for custom actions only. */
    val label: String,
    /**
     * Index of the Android notification's own action, or -1 for one this app
     * synthesised, such as DISMISS.
     */
    val androidIndex: Int,
    /** Whether the watch should collect text before invoking it. */
    val isReply: Boolean = false,
) {
    val isSynthetic: Boolean get() = androidIndex < 0
}

/** What the wearer invoked, handed up for something platform-aware to perform. */
data class GarminNotificationActionRequest(
    val notificationId: Long,
    val action: GarminNotificationAction,
    /** What the wearer dictated or picked, for a reply. Null for a plain button. */
    val replyText: String? = null,
)

/**
 * Encodes the action list for attribute 127: a count byte, then per action
 * `{u8 code, u8 iconPositionBits, u8 labelByteLength, label}`. An empty
 * list is four zero bytes, Garmin's own sentinel.
 */
fun encodeGarminNotificationActions(
    actions: List<GarminNotificationAction>,
): ByteArray {
    if (actions.isEmpty()) return byteArrayOf(0, 0, 0, 0)
    val writer = GarminByteWriter().writeByte(actions.size)
    for (action in actions) {
        val label = action.label.toByteArray(Charsets.UTF_8)
        // One length byte: a longer label would corrupt every action after it.
        val trimmed = if (label.size > 255) label.copyOf(255) else label
        writer
            .writeByte(action.kind.code)
            .writeByte(action.kind.iconPosition?.bit ?: 0)
            .writeByte(trimmed.size)
            .writeBytes(trimmed)
    }
    return writer.toBytes()
}

/** One notification, as the phone holds it while the watch decides. */
data class GarminNotification(
    /** Announced as a `u32`. MODIFY and REMOVE are matched to an ADD by this alone. */
    val id: Long,
    /** The posting app's package, sent as APP_IDENTIFIER. Some faces resolve an icon from it. */
    val packageName: String,
    val title: String = "",
    val subtitle: String = "",
    val body: String = "",
    val category: GarminNotificationCategory = GarminNotificationCategory.OTHER,
    /** When the notification was posted, sent as DATE in the watch's local time. */
    val postedAt: LocalDateTime,
    /** What the wearer can do. Empty means the watch draws no actions. */
    val actions: List<GarminNotificationAction> = emptyList(),
) {
    val hasActions: Boolean get() = actions.isNotEmpty()
}

private val GNCS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

/** Formats a timestamp as GNCS wants it: `yyyyMMdd'T'HHmmss`, local time. */
fun garminNotificationDate(whenPosted: LocalDateTime): String =
    GNCS_DATE_FORMAT.format(whenPosted)

/**
 * The text sent for [attribute], before truncation. MESSAGE_SIZE is the
 * UTF-16 length, as Gadgetbridge sends it.
 */
fun garminNotificationAttributeText(
    notification: GarminNotification,
    attribute: GarminNotificationAttribute,
): String = when (attribute) {
    GarminNotificationAttribute.APP_IDENTIFIER -> notification.packageName
    GarminNotificationAttribute.TITLE -> notification.title
    GarminNotificationAttribute.SUBTITLE -> notification.subtitle
    GarminNotificationAttribute.MESSAGE -> notification.body
    GarminNotificationAttribute.MESSAGE_SIZE -> notification.body.length.toString()
    GarminNotificationAttribute.DATE -> garminNotificationDate(notification.postedAt)
    // Nothing can be declined.
    GarminNotificationAttribute.NEGATIVE_ACTION_LABEL -> ""
    // Bytes, not text; see [garminNotificationAttributeBytes].
    GarminNotificationAttribute.ACTIONS -> ""
    // Always none: pictures ride a protobuf channel this app does not implement.
    GarminNotificationAttribute.ATTACHMENTS -> "0"
}

/**
 * The bytes sent for [attribute], cut to [maxLength] characters before UTF-8
 * encoding, as Gadgetbridge does. The cut avoids splitting a surrogate pair,
 * which would make the attribute undecodable.
 */
fun garminNotificationAttributeBytes(
    notification: GarminNotification,
    attribute: GarminNotificationAttribute,
    maxLength: Int = 0,
): ByteArray {
    if (attribute == GarminNotificationAttribute.ACTIONS) {
        // Not truncated: this is a packed structure, not text.
        return encodeGarminNotificationActions(notification.actions)
    }
    val text = garminNotificationAttributeText(notification, attribute)
    if (maxLength <= 0 || text.length <= maxLength) {
        return text.toByteArray(Charsets.UTF_8)
    }
    var cut = maxLength
    if (text[cut - 1].isHighSurrogate()) cut -= 1
    return text.substring(0, cut).toByteArray(Charsets.UTF_8)
}

/**
 * Builds the blob answering one GET_NOTIFICATION_ATTRIBUTES request: the
 * command byte, the id, then each attribute as `{u8 code, u16 length, bytes}`.
 * [requested] keeps the watch's order, except MESSAGE_SIZE always goes last:
 * mid-blob, a watch renders an empty body.
 */
fun encodeGarminNotificationAttributes(
    notification: GarminNotification,
    requested: Map<GarminNotificationAttribute, Int>,
): ByteArray {
    val writer = GarminByteWriter()
        .writeByte(GarminNotificationCommand.GET_NOTIFICATION_ATTRIBUTES.code)
        .writeInt(notification.id)

    fun encode(attribute: GarminNotificationAttribute, maxLength: Int) {
        val value = garminNotificationAttributeBytes(
            notification,
            attribute,
            maxLength = maxLength,
        )
        writer
            .writeByte(attribute.code)
            .writeShort(value.size)
            .writeBytes(value)
    }

    var messageSizeMaxLength: Int? = null
    for ((attribute, maxLength) in requested) {
        if (attribute == GarminNotificationAttribute.MESSAGE_SIZE) {
            messageSizeMaxLength = maxLength
            continue
        }
        encode(attribute, maxLength)
    }
    messageSizeMaxLength?.let { encode(GarminNotificationAttribute.MESSAGE_SIZE, it) }
    return writer.toBytes()
}
