package tech.mmarca.openvitals.devices.garmin

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The vocabulary of Garmin's notification service (GNCS) and the pure encoder
 * for a notification's attribute blob.
 *
 * GNCS is Garmin's rendering of Apple's ANCS — Gadgetbridge's enums still
 * carry `//was AncsCommand` / `//was AncsAttribute` comments — and it is a
 * **pull** protocol. The phone announces only that a notification exists
 * (message 5033: an id, a category and some flags, and NO text). The watch
 * then asks for the attributes it wants (5034), and only then does the text
 * go across (5035).
 *
 * So there is no "send a notification" call anywhere: there is an
 * announcement, and there is an answer to a question that may never be asked.
 * If the watch never asks — because notifications are switched off on the
 * wrist, or the wearer never looks — the text stays on the phone and nothing
 * has gone wrong.
 *
 * This file is deliberately I/O-free and frame-free: it turns a notification
 * into the bytes of one attribute blob, and nothing else. The GFDI framing
 * and the inbound decoders live in `GarminMessages.kt`; the chunked upload is
 * the notifications handler's job (sub-milestone 7e).
 *
 * Ported from Gadgetbridge's `NotificationsHandler`,
 * `NotificationUpdateMessage` and `NotificationControlMessage` (AGPLv3), via
 * the Flutter build's `garmin_notification_messages.dart`.
 */

/**
 * Whether an announcement adds a notification, updates one already sent, or
 * withdraws it. Ordinal IS the wire value — do not reorder.
 */
enum class GarminNotificationUpdateType { ADD, MODIFY, REMOVE }

/**
 * The notification's kind, as the watch understands it. Ordinal IS the wire
 * value, so the order is load-bearing — do not reorder or remove.
 *
 * The watch groups and prioritises by this, and some faces show a
 * per-category count, which is what the `count` field of an announcement
 * feeds.
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

/**
 * Flags describing what the PHONE can offer for this notification. Bit is
 * `1 shl ordinal`.
 */
enum class GarminNotificationPhoneFlag {
    LEGACY_ACTIONS,
    NEW_ACTIONS,
    HAS_ATTACHMENTS,
    ;

    val bit: Int get() = 1 shl ordinal
}

/**
 * The category-flags byte for an announcement.
 *
 * `FOREGROUND` is set unconditionally, and `ACTION_DECLINE` always
 * accompanies it — both verbatim from Gadgetbridge, whose comment is worth
 * keeping because the reason is not guessable from the wire: marking
 * notifications as background "was generating bug reports", since most people
 * expect every notification to raise the watch rather than sit silently in a
 * list.
 */
fun garminNotificationCategoryFlags(): Int =
    GarminNotificationFlag.FOREGROUND.bit or GarminNotificationFlag.ACTION_DECLINE.bit

/**
 * The commands a watch can send on the notification control channel (5034).
 *
 * [GET_APP_ATTRIBUTES] is the only one not acted on — Gadgetbridge marks it
 * "unknown/untested" and no watch here has ever sent one. Both action
 * commands are honoured: [PERFORM_NOTIFICATION_ACTION] carries an action code
 * this app chose, and [PERFORM_LEGACY_NOTIFICATION_ACTION] is the older
 * accept/refuse pair a watch sends for the control it draws from the
 * ACTION_DECLINE category flag.
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
 * One field of a notification the watch can ask for.
 *
 * [hasLengthParam] and [hasAdditionalParams] are not cosmetic: they say how
 * many bytes follow the attribute's own id in a REQUEST, so getting one wrong
 * desynchronises the rest of the request. Codes 0–7 are ANCS's; 127 and 128
 * are Garmin's own additions.
 */
enum class GarminNotificationAttribute(
    val code: Int,
    /** A `u16` maximum length follows this attribute's id in a request. */
    val hasLengthParam: Boolean = false,
    /**
     * A `u16` follows, and then one more byte nobody has identified.
     * Gadgetbridge reads it and marks the read `//TODO this is wrong`; it is
     * reproduced here because a request that includes attribute 127 has to be
     * walked past correctly whether or not the byte means anything.
     */
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

/**
 * The watch's verdict on one chunk of an attribute blob. Ordinal IS the wire
 * value — do not reorder.
 */
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
 * What a wearer can do to a notification, as the watch understands it.
 *
 * The code IS the wire value and also decides where the watch draws the
 * control, which is why the icon position is fixed per code rather than
 * chosen: on a vívoactive the left button is the dismiss position, and
 * putting a reply there would be wrong however it is labelled.
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
 *
 * Gadgetbridge's values, and its comment is worth keeping: these are
 * "educated guesses based on the icons' positions on vívomove style". Nobody
 * has the documentation.
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
    /**
     * What the posting app called it. Shown on the watch for custom actions;
     * the call and dismiss controls are drawn as icons and ignore it.
     */
    val label: String,
    /**
     * Which of the Android notification's own actions this is, so one invoked
     * from the wrist resolves back without re-deriving anything.
     *
     * -1 for an action this app synthesised —
     * [GarminNotificationActionKind.DISMISS] is ours, not the posting app's,
     * and is performed by clearing the notification rather than by firing one
     * of its intents.
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
 * Encodes the action list for attribute 127.
 *
 * Layout: a count byte, then per action `{u8 code, u8 iconPositionBits,
 * u8 labelByteLength, label}`.
 *
 * An empty list encodes as four zero bytes rather than a bare zero — Garmin's
 * own "no actions" sentinel, which Gadgetbridge sends verbatim.
 */
fun encodeGarminNotificationActions(
    actions: List<GarminNotificationAction>,
): ByteArray {
    if (actions.isEmpty()) return byteArrayOf(0, 0, 0, 0)
    val writer = GarminByteWriter().writeByte(actions.size)
    for (action in actions) {
        val label = action.label.toByteArray(Charsets.UTF_8)
        // One length byte, so a long label would wrap and corrupt every
        // action after it.
        val trimmed = if (label.size > 255) label.copyOf(255) else label
        writer
            .writeByte(action.kind.code)
            .writeByte(action.kind.iconPosition?.bit ?: 0)
            .writeByte(trimmed.size)
            .writeBytes(trimmed)
    }
    return writer.toBytes()
}

/**
 * One notification, as the phone holds it while the watch decides whether to
 * ask about it.
 */
data class GarminNotification(
    /**
     * Announced as a `u32`. Stable for the lifetime of the notification on
     * the phone, because a MODIFY and a REMOVE are matched to an ADD by this
     * alone.
     */
    val id: Long,
    /**
     * The posting app's package name, sent as APP_IDENTIFIER. Some watch
     * faces resolve an icon from it.
     */
    val packageName: String,
    val title: String = "",
    val subtitle: String = "",
    val body: String = "",
    val category: GarminNotificationCategory = GarminNotificationCategory.OTHER,
    /** When the notification was posted, sent as DATE in the watch's local time. */
    val postedAt: LocalDateTime,
    /**
     * What the wearer can do to it. Empty means the watch is told there are
     * no actions, and draws none.
     */
    val actions: List<GarminNotificationAction> = emptyList(),
) {
    val hasActions: Boolean get() = actions.isNotEmpty()
}

private val GNCS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

/**
 * Formats a timestamp the way GNCS wants it: `yyyyMMdd'T'HHmmss`, local time.
 *
 * A pure function taking the value rather than reading the clock, so a test
 * can assert the exact bytes without a fixed-clock harness.
 */
fun garminNotificationDate(whenPosted: LocalDateTime): String =
    GNCS_DATE_FORMAT.format(whenPosted)

/**
 * The text this app sends for [attribute], before any truncation.
 *
 * MESSAGE_SIZE is the *character* count of the body, not its byte count —
 * Gadgetbridge sends `String.length`, which is UTF-16 code units, and
 * Kotlin's `String.length` is the same measure, so the two agree by
 * construction.
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
    // Actions are not implemented as declinable, so nothing can be declined.
    GarminNotificationAttribute.NEGATIVE_ACTION_LABEL -> ""
    // Handled as bytes, not text — see [garminNotificationAttributeBytes].
    GarminNotificationAttribute.ACTIONS -> ""
    // The number of attachments, as text. Always none: pictures ride a
    // separate protobuf channel this app does not implement.
    GarminNotificationAttribute.ATTACHMENTS -> "0"
}

/**
 * The bytes this app sends for [attribute], truncated to [maxLength] when the
 * watch named one.
 *
 * [maxLength] is a count of CHARACTERS, not bytes — it is applied to the
 * string before UTF-8 encoding, as Gadgetbridge does, because that is what
 * the watch has been observed to expect. The cut is nudged back by one when
 * it would land between the halves of a surrogate pair, which would otherwise
 * emit a lone surrogate and make the whole attribute undecodable.
 * (Gadgetbridge does not do this, and an emoji at the truncation point is
 * enough to hit it.)
 */
fun garminNotificationAttributeBytes(
    notification: GarminNotification,
    attribute: GarminNotificationAttribute,
    maxLength: Int = 0,
): ByteArray {
    if (attribute == GarminNotificationAttribute.ACTIONS) {
        // NOT truncated to maxLength. The watch names a limit for TEXT it
        // will render; this value is a packed structure, and cutting it
        // mid-record would leave the watch parsing a label as an action code.
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
 * Builds the attribute blob that answers one GET_NOTIFICATION_ATTRIBUTES
 * request. The result is what gets chunked into 5035 messages.
 *
 * Layout: the command byte, the notification id, then each requested
 * attribute as `{u8 code, u16 byteLength, bytes}`.
 *
 * [requested] must preserve the watch's own order (a `LinkedHashMap`, which
 * is what the control decoder builds) with one exception: **MESSAGE_SIZE is
 * always encoded last**, wherever the watch asked for it. Gadgetbridge does
 * the same, and a watch that receives it mid-blob has been observed to render
 * an empty body.
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
