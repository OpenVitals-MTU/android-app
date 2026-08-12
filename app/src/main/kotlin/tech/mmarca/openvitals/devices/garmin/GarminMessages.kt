package tech.mmarca.openvitals.devices.garmin

import java.time.Instant
import java.time.ZoneId

/**
 * The GFDI message vocabulary for the file-sync flow.
 *
 * A deliberately small slice of Gadgetbridge's ~30-message `GarminMessage`
 * enum: what a read-only FIT sync needs — request a download, receive its
 * chunks and acknowledge them, archive a finished file, and send system
 * events — plus the notification service (GNCS). Music, weather and uploads
 * remain out of scope.
 *
 * Not Gadgetbridge's reflection-dispatched class-per-message design: parsing
 * is one [decodeGarminMessage] `when` on the frame's type id, and each
 * outgoing message is a small builder. Everything here is pure — it turns
 * bytes into typed values and back, with no I/O — so the session logic above
 * it tests over an in-memory pipe.
 */
object GarminMessageId {
    const val RESPONSE = 5000 // status/ack envelope
    const val DOWNLOAD_REQUEST = 5002
    const val FILE_TRANSFER_DATA = 5004
    const val FILE_AVAILABLE = 5009
    const val FILTER = 5007
    const val FIT_DEFINITION = 5011
    const val FIT_DATA = 5012
    const val WEATHER_REQUEST = 5014
    const val SET_FILE_FLAGS = 5008
    const val DEVICE_INFORMATION = 5024
    const val DEVICE_SETTINGS = 5026
    const val SYSTEM_EVENT = 5030
    const val SUPPORTED_FILE_TYPES_REQUEST = 5031

    // The notification service (GNCS). UPDATE announces, CONTROL is the watch
    // asking, DATA carries the answer — see GarminNotificationMessages.kt.
    const val NOTIFICATION_UPDATE = 5033
    const val NOTIFICATION_CONTROL = 5034
    const val NOTIFICATION_DATA = 5035
    const val NOTIFICATION_SUBSCRIPTION = 5036
    const val SYNCHRONIZATION = 5037
    const val FIND_MY_PHONE_REQUEST = 5039
    const val FIND_MY_PHONE_CANCEL = 5040
    const val PROTOBUF_REQUEST = 5043
    const val PROTOBUF_RESPONSE = 5044
    const val CONFIGURATION = 5050
    const val CURRENT_TIME_REQUEST = 5052
    const val AUTH_NEGOTIATION = 5101
}

/**
 * GFDI status codes (`GFDIMessage.Status`). Only ACK matters to the sync; any
 * other code means "did not proceed".
 */
enum class GarminStatus(val code: Int) {
    ACK(0),
    NAK(1),
    UNSUPPORTED(2),
    DECODE_ERROR(3),
    CRC_ERROR(4),
    LENGTH_ERROR(5),
    ;

    companion object {
        fun fromCode(code: Int): GarminStatus =
            entries.firstOrNull { it.code == code } ?: NAK
    }
}

/** Per-file download outcome (`DownloadRequestStatusMessage.DownloadStatus`). */
enum class GarminDownloadStatus {
    OK,
    INDEX_UNKNOWN,
    INDEX_NOT_READABLE,
    NO_SPACE_LEFT,
    INVALID,
    NOT_READY,
    CRC_INCORRECT,
    ;

    companion object {
        fun fromOrdinal(ordinal: Int): GarminDownloadStatus =
            if (ordinal in entries.indices) entries[ordinal] else INVALID
    }
}

/**
 * System events the sync sends (`SystemEventMessage.GarminSystemEventType`).
 * Ordinal IS the wire value, so the order is load-bearing — do not reorder.
 */
enum class GarminSystemEventType {
    SYNC_COMPLETE, // 0
    SYNC_FAIL,
    FACTORY_RESET,
    PAIR_START,
    PAIR_COMPLETE,
    PAIR_FAIL,
    HOST_DID_ENTER_FOREGROUND,
    HOST_DID_ENTER_BACKGROUND,
    SYNC_READY, // 8
    NEW_DOWNLOAD_AVAILABLE,
    DEVICE_SOFTWARE_UPDATE,
    DEVICE_DISCONNECT,
    TUTORIAL_COMPLETE,
    SETUP_WIZARD_START,
    SETUP_WIZARD_COMPLETE,
    SETUP_WIZARD_SKIPPED,
    TIME_UPDATED,
}

/**
 * A parsed inbound GFDI message the sync acts on. Anything outside the sync
 * vocabulary decodes to [GarminUnhandledMessage] rather than throwing — the
 * watch chatters other messages a read-only sync simply ignores.
 */
sealed class GarminInboundMessage

/**
 * A status/ack envelope (type 5000) whose subject was not one the sync tracks
 * specially — a plain ACK/NAK for a message we sent.
 */
data class GarminGenericStatus(
    val originalMessageType: Int,
    val status: GarminStatus,
) : GarminInboundMessage()

/**
 * The response to a download request. When [canProceed], [maxFileSize] is the
 * total byte length the watch will stream.
 */
data class GarminDownloadRequestStatus(
    val status: GarminStatus,
    val downloadStatus: GarminDownloadStatus,
    val maxFileSize: Long,
) : GarminInboundMessage() {
    val canProceed: Boolean
        get() = status == GarminStatus.ACK && downloadStatus == GarminDownloadStatus.OK
}

/**
 * One chunk of a downloading file (type 5004). [dataOffset] is where this
 * chunk sits in the file; [crc] is the running CRC of everything up to and
 * including it, which the session verifies before appending.
 */
class GarminFileTransferData(
    val dataOffset: Long,
    val crc: Int,
    val data: ByteArray,
) : GarminInboundMessage()

/**
 * The watch introducing itself (type 5024). Sent unprompted on connect and
 * answered with [buildDeviceInformationResponse].
 *
 * [maxPacketSize] is the one field the transport needs: it caps how much of a
 * GFDI frame fits in a single write.
 */
data class GarminDeviceInformation(
    val protocolVersion: Int,
    val productNumber: Int,
    val unitNumber: Long,
    val softwareVersion: Int,
    val maxPacketSize: Int,
    val bluetoothFriendlyName: String,
    val deviceName: String,
    val deviceModel: String,
) : GarminInboundMessage() {
    /** e.g. `19.15` — major/minor split at 100, as Gadgetbridge renders it. */
    val softwareVersionText: String
        get() = "${softwareVersion / 100}." +
            (softwareVersion % 100).toString().padStart(2, '0')
}

/**
 * The watch's authentication challenge (type 5101).
 *
 * GFDI has no real authentication: the answer is "yes, fine" with the flags
 * echoed back. The Bluetooth bond is the actual security boundary, which is
 * why onboarding refuses to register a watch that would not bond.
 */
data class GarminAuthNegotiation(
    val unknown: Int,
    val authFlags: Long,
) : GarminInboundMessage()

/**
 * The watch's answer to [buildSupportedFileTypesRequest]: which
 * `(dataType, subType)` pairs it holds.
 */
data class GarminSupportedFileTypes(
    val status: GarminStatus,
    /**
     * Every advertised pair, including ones this app does not map — the raw
     * list is what makes an empty-vs-unmapped diagnosis possible.
     */
    val types: List<GarminSupportedFileType>,
) : GarminInboundMessage()

data class GarminSupportedFileType(
    val dataType: Int,
    val subType: Int,
    val name: String,
)

/**
 * The watch announcing what it has to offer (type 5037), as a bitmask over
 * `SynchronizationMessage.FileType` ordinals.
 *
 * Gadgetbridge answers this with a [buildFilterMessage] and only then
 * downloads the directory — which is the exchange that appears to make the
 * watch populate its listing at all.
 */
data class GarminSynchronization(
    val syncType: Int,
    val bitmask: Long,
) : GarminInboundMessage() {

    private fun has(ordinal: Int): Boolean = (bitmask shr ordinal) and 1L == 1L

    /** Whether the announcement contains anything this app would want. */
    val shouldProceed: Boolean
        get() = has(WORKOUTS) || has(ACTIVITIES) || has(ACTIVITY_SUMMARY) || has(SLEEP)

    /** The set bits, for the log — the raw evidence of what the watch is holding. */
    val setBits: List<Int>
        get() = (0 until 64).filter { has(it) }

    private companion object {
        /**
         * Ordinals of the categories worth acting on
         * (`SynchronizationMessage.shouldProceed`).
         */
        const val WORKOUTS = 3
        const val ACTIVITIES = 5
        const val ACTIVITY_SUMMARY = 21
        const val SLEEP = 26
    }
}

/**
 * The watch's capability bitmap (type 5050) — the capabilities exchange.
 *
 * This is not informational. The watch expects OUR capabilities back, and in
 * Gadgetbridge receiving it is what raises the event that completes
 * initialisation. Answering it with only a bare ACK left a real vívoactive 5
 * re-sending it and never populating its directory.
 */
class GarminConfiguration(
    /** The raw bitmap, one bit per capability ordinal. */
    val capabilityBits: ByteArray,
) : GarminInboundMessage()

/**
 * The watch asking whether to route phone notifications (type 5036).
 *
 * Needs a purpose-built status reply, not the generic ACK: the watch expects
 * four payload bytes after the message id and retransmits about once a second
 * until it gets them.
 */
data class GarminNotificationSubscription(
    val enable: Boolean,
    val unknown: Int,
) : GarminInboundMessage()

/**
 * The watch asking something about a notification (type 5034).
 *
 * Which fields are populated depends on [command]:
 * * `GET_NOTIFICATION_ATTRIBUTES` — [notificationId] and [attributes], the
 *   fields it wants and the maximum length it will accept for each (0 = no
 *   limit).
 * * `GET_APP_ATTRIBUTES` — [appIdentifier] and [appAttributes].
 * * either `PERFORM_…_ACTION` — [notificationId], [actionCode] and, for a
 *   reply, [actionText].
 */
data class GarminNotificationControl(
    val command: GarminNotificationCommand,
    val notificationId: Long = 0,
    /**
     * Requested attribute → maximum length, in the watch's own order. A
     * `LinkedHashMap` by construction, because that order is reproduced in
     * the answer.
     */
    val attributes: Map<GarminNotificationAttribute, Int> = emptyMap(),
    val appIdentifier: String? = null,
    val appAttributes: List<Int> = emptyList(),
    val actionCode: Int? = null,
    val actionText: String? = null,
) : GarminInboundMessage()

/**
 * The watch's verdict on one chunk of an attribute blob — a RESPONSE envelope
 * naming NOTIFICATION_DATA.
 *
 * [canProceed] is the flow control: the next chunk goes out only when the
 * watch has said it kept the last one.
 */
data class GarminNotificationDataStatus(
    val status: GarminStatus,
    val transferStatus: GarminNotificationTransferStatus,
) : GarminInboundMessage() {
    val canProceed: Boolean
        get() = status == GarminStatus.ACK &&
            transferStatus == GarminNotificationTransferStatus.OK
}

/**
 * A message outside the sync vocabulary. Carries its payload so an unexpected
 * message can be identified from a device log rather than vanishing — the
 * blind spot that hid whether the watch was talking to us at all.
 */
/** The watch asking the phone for the time (`CurrentTimeRequestMessage`). */
data class GarminCurrentTimeRequest(
    /** Echoed back verbatim in the response, pairing it to this ask. */
    val referenceId: Long,
) : GarminInboundMessage()

/**
 * The watch asking for weather (`WeatherMessage`) — sent when the glance
 * opens, and periodically while a link is held.
 */
data class GarminWeatherRequest(
    val format: Int,
    val latitudeSemicircles: Long,
    val longitudeSemicircles: Long,
    val hoursOfForecast: Int,
) : GarminInboundMessage()

/** The watch asking the phone to ring (`FindMyPhoneRequestMessage`). */
data class GarminFindMyPhoneRequest(
    val durationSeconds: Int,
) : GarminInboundMessage()

/** The user ended the find on the watch. */
class GarminFindMyPhoneCancel : GarminInboundMessage()

/**
 * The watch announcing a file it has just finished writing, while a link is
 * already open (`FileAvailableMessage`) — how a save that happens mid-listen
 * reaches the phone without waiting for the next directory fetch.
 */
data class GarminFileAvailable(
    val entry: GarminDirectoryEntry,
) : GarminInboundMessage()

class GarminUnhandledMessage(
    val messageType: Int,
    val payload: ByteArray,
) : GarminInboundMessage()

/** Parses a decoded [GarminGfdiFrame] into a typed [GarminInboundMessage]. */
fun decodeGarminMessage(frame: GarminGfdiFrame): GarminInboundMessage =
    when (frame.messageType) {
        GarminMessageId.RESPONSE -> decodeStatus(frame.payload)
        GarminMessageId.FILE_TRANSFER_DATA -> decodeFileTransferData(frame.payload)
        GarminMessageId.DEVICE_INFORMATION -> decodeDeviceInformation(frame.payload)
        GarminMessageId.AUTH_NEGOTIATION -> decodeAuthNegotiation(frame.payload)
        GarminMessageId.SYNCHRONIZATION -> decodeSynchronization(frame.payload)
        GarminMessageId.CONFIGURATION -> decodeConfiguration(frame.payload)
        GarminMessageId.NOTIFICATION_SUBSCRIPTION ->
            decodeNotificationSubscription(frame.payload)
        GarminMessageId.NOTIFICATION_CONTROL ->
            decodeNotificationControl(frame.payload)
        GarminMessageId.CURRENT_TIME_REQUEST -> decodeCurrentTimeRequest(frame.payload)
        GarminMessageId.WEATHER_REQUEST -> decodeWeatherRequest(frame.payload)
        GarminMessageId.FIND_MY_PHONE_REQUEST -> decodeFindMyPhoneRequest(frame.payload)
        GarminMessageId.FIND_MY_PHONE_CANCEL -> GarminFindMyPhoneCancel()
        GarminMessageId.FILE_AVAILABLE -> decodeFileAvailable(frame.payload)
        else -> GarminUnhandledMessage(frame.messageType, frame.payload)
    }

private fun decodeWeatherRequest(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    return GarminWeatherRequest(
        format = reader.readByte(),
        latitudeSemicircles = reader.readInt(),
        longitudeSemicircles = reader.readInt(),
        hoursOfForecast = reader.readByte(),
    )
}

private fun decodeCurrentTimeRequest(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    return GarminCurrentTimeRequest(referenceId = reader.readInt())
}

private fun decodeFindMyPhoneRequest(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    return GarminFindMyPhoneRequest(durationSeconds = reader.readByte())
}

/** Same 16-byte record layout as a directory listing entry. */
private fun decodeFileAvailable(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    val fileIndex = reader.readShort()
    val dataType = reader.readByte()
    val subType = reader.readByte()
    val fileNumber = reader.readShort()
    val specificFlags = reader.readByte()
    val fileFlags = reader.readByte()
    val fileSize = reader.readInt()
    val wireTimestamp = reader.readInt()
    // A type this app has no name for gets the unhandled path — an ack and a
    // log — the same treatment the directory parser gives such entries.
    val type = GarminFileType.fromCodes(dataType, subType)
        ?: return GarminUnhandledMessage(GarminMessageId.FILE_AVAILABLE, payload)
    return GarminFileAvailable(
        GarminDirectoryEntry(
            fileIndex = fileIndex,
            type = type,
            fileNumber = fileNumber,
            specificFlags = specificFlags,
            fileFlags = fileFlags,
            fileSize = fileSize,
            fileDate = if (wireTimestamp == 0L) null else GarminTime.toInstant(wireTimestamp),
        ),
    )
}

private fun decodeSynchronization(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    val syncType = reader.readByte()
    val size = reader.readByte()
    // The watch sends the bitmask as either 4 or 8 bytes.
    val bitmask = when (size) {
        8 -> reader.readLong()
        4 -> reader.readInt()
        else -> 0L
    }
    return GarminSynchronization(syncType = syncType, bitmask = bitmask)
}

private fun decodeDeviceInformation(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    return GarminDeviceInformation(
        protocolVersion = reader.readShort(),
        productNumber = reader.readShort(),
        unitNumber = reader.readInt(),
        softwareVersion = reader.readShort(),
        maxPacketSize = reader.readShort(),
        bluetoothFriendlyName = reader.readString(),
        deviceName = reader.readString(),
        deviceModel = reader.readString(),
    )
}

private fun decodeAuthNegotiation(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    return GarminAuthNegotiation(
        unknown = reader.readByte(),
        authFlags = reader.readInt(),
    )
}

private fun decodeConfiguration(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    val length = reader.readByte()
    val available = if (length > reader.remaining) reader.remaining else length
    return GarminConfiguration(reader.readBytes(available))
}

private fun decodeNotificationSubscription(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    val enable = reader.readByte() == 1
    val unknown = if (reader.remaining > 0) reader.readByte() else 0
    return GarminNotificationSubscription(enable = enable, unknown = unknown)
}

private fun decodeNotificationControl(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    val command = GarminNotificationCommand.fromCode(reader.readByte())
        ?: return GarminUnhandledMessage(GarminMessageId.NOTIFICATION_CONTROL, payload)
    return when (command) {
        GarminNotificationCommand.GET_NOTIFICATION_ATTRIBUTES -> {
            val notificationId = reader.readInt()
            val attributes = LinkedHashMap<GarminNotificationAttribute, Int>()
            while (reader.remaining > 0) {
                // An attribute this app does not know may or may not be
                // followed by a length, so there is no safe way to find the
                // next id. Stop and answer what was understood rather than
                // mis-parsing the rest as attributes.
                val attribute = GarminNotificationAttribute.fromCode(reader.readByte())
                    ?: break
                var maxLength = 0
                if (attribute.hasLengthParam) {
                    if (reader.remaining < 2) break
                    maxLength = reader.readShort()
                } else if (attribute.hasAdditionalParams) {
                    if (reader.remaining < 3) break
                    maxLength = reader.readShort()
                    reader.readByte() // Unidentified; read to stay in step.
                }
                attributes[attribute] = maxLength
            }
            GarminNotificationControl(
                command = command,
                notificationId = notificationId,
                attributes = attributes,
            )
        }

        GarminNotificationCommand.GET_APP_ATTRIBUTES -> {
            val appIdentifier = reader.readNullTerminatedString()
            val appAttributes = mutableListOf<Int>()
            while (reader.remaining > 0) {
                appAttributes.add(reader.readByte())
            }
            GarminNotificationControl(
                command = command,
                appIdentifier = appIdentifier,
                appAttributes = appAttributes,
            )
        }

        GarminNotificationCommand.PERFORM_LEGACY_NOTIFICATION_ACTION,
        GarminNotificationCommand.PERFORM_NOTIFICATION_ACTION,
        -> {
            val notificationId = reader.readInt()
            val actionCode = if (reader.remaining > 0) reader.readByte() else null
            // A non-reply action carries no text at all on recent firmware, so
            // its absence is normal rather than a short frame.
            val actionText =
                if (reader.remaining > 0) reader.readNullTerminatedString() else null
            GarminNotificationControl(
                command = command,
                notificationId = notificationId,
                actionCode = actionCode,
                actionText = actionText,
            )
        }
    }
}

private fun decodeSupportedFileTypes(reader: GarminByteReader): GarminInboundMessage {
    val status = GarminStatus.fromCode(reader.readByte())
    if (status != GarminStatus.ACK) {
        return GarminSupportedFileTypes(status = status, types = emptyList())
    }
    val count = reader.readByte()
    val types = mutableListOf<GarminSupportedFileType>()
    var i = 0
    while (i < count && reader.remaining >= 2) {
        types.add(
            GarminSupportedFileType(
                dataType = reader.readByte(),
                subType = reader.readByte(),
                name = reader.readString(),
            ),
        )
        i++
    }
    return GarminSupportedFileTypes(status = status, types = types)
}

private fun decodeStatus(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    val originalType = reader.readShort()
    if (originalType == GarminMessageId.SUPPORTED_FILE_TYPES_REQUEST) {
        return decodeSupportedFileTypes(reader)
    }
    if (originalType == GarminMessageId.DOWNLOAD_REQUEST) {
        val status = GarminStatus.fromCode(reader.readByte())
        if (status != GarminStatus.ACK) {
            return GarminDownloadRequestStatus(
                status = status,
                downloadStatus = GarminDownloadStatus.INVALID,
                maxFileSize = 0,
            )
        }
        val downloadStatus = GarminDownloadStatus.fromOrdinal(reader.readByte())
        val maxFileSize = reader.readInt()
        return GarminDownloadRequestStatus(
            status = status,
            downloadStatus = downloadStatus,
            maxFileSize = maxFileSize,
        )
    }
    if (originalType == GarminMessageId.NOTIFICATION_DATA) {
        val status = GarminStatus.fromCode(reader.readByte())
        // The watch names WHY it will not take the next chunk, and the upload
        // acts on the difference — a RESEND is recoverable, a CRC mismatch is
        // not. A status with no transfer byte is treated as OK: the only
        // observed sender of that shape is our own final acknowledgement
        // bouncing back.
        val transferStatus = if (reader.remaining > 0) {
            GarminNotificationTransferStatus.fromOrdinal(reader.readByte())
        } else {
            GarminNotificationTransferStatus.OK
        }
        return GarminNotificationDataStatus(
            status = status,
            transferStatus = transferStatus,
        )
    }
    // Generic ACK/NAK: a single status byte follows the original type.
    val status = if (reader.remaining > 0) {
        GarminStatus.fromCode(reader.readByte())
    } else {
        GarminStatus.ACK
    }
    return GarminGenericStatus(originalMessageType = originalType, status = status)
}

private fun decodeFileTransferData(payload: ByteArray): GarminInboundMessage {
    val reader = GarminByteReader(payload)
    reader.readByte() // flags — unused on the read path
    val crc = reader.readShort()
    val dataOffset = reader.readInt()
    val data = reader.readBytes(reader.remaining)
    return GarminFileTransferData(dataOffset = dataOffset, crc = crc, data = data)
}

// ── Outgoing message builders — each returns a ready-to-COBS GFDI frame ──────

/** Whether a download starts fresh or continues (`REQUEST_TYPE` ordinal). */
enum class GarminDownloadRequestType { CONTINUE_TRANSFER, FRESH }

/**
 * Requests file [fileIndex]. For a fresh whole-file download the watch fills
 * in the size, so [dataOffset]/[dataSize]/[crcSeed] are 0 — the shape
 * `initiateDownload`/`downloadDirectoryEntry` use.
 */
fun buildDownloadRequest(
    fileIndex: Int,
    requestType: GarminDownloadRequestType = GarminDownloadRequestType.FRESH,
    dataOffset: Long = 0,
    crcSeed: Int = 0,
    dataSize: Long = 0,
): ByteArray {
    val writer = GarminByteWriter()
        .writeShort(fileIndex)
        .writeInt(dataOffset)
        .writeByte(requestType.ordinal)
        .writeShort(crcSeed)
        .writeInt(dataSize)
    return GarminGfdiFrame.build(GarminMessageId.DOWNLOAD_REQUEST, writer.toBytes())
}

/**
 * Acknowledges any inbound message: a `RESPONSE` envelope naming what is
 * being acknowledged, plus ACK (`GenericStatusMessage`).
 *
 * Gadgetbridge sends one of these for EVERY message it receives
 * (`GarminSupport.onMessage` → `sendAck`). Without them the watch treats its
 * message as lost and retransmits, and will not move on — which is exactly
 * what a real vívoactive 5 did, re-sending its CONFIGURATION message on a
 * timer while its directory stayed empty.
 */
fun buildGenericAck(originalMessageType: Int): ByteArray {
    val writer = GarminByteWriter()
        .writeShort(originalMessageType)
        .writeByte(GarminStatus.ACK.code)
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}

/** Setting ordinals from `SetDeviceSettingsMessage.GarminDeviceSetting`. */
object GarminDeviceSetting {
    const val AUTO_UPLOAD_ENABLED = 6
    const val WEATHER_CONDITIONS_ENABLED = 7
    const val WEATHER_ALERTS_ENABLED = 8
}

/**
 * Configures the watch over `DEVICE_SETTINGS` (5026): `[u8 count]` then per
 * setting `[u8 ordinal][u8 length=1][u8 bool]`.
 *
 * Gadgetbridge sends this on EVERY connection, and it is not optional
 * housekeeping: `WEATHER_CONDITIONS_ENABLED` is the switch for the watch's
 * whole weather feature — without it the glance never fetches anything and
 * shows "reconnect to phone" forever, whatever the phone is ready to serve.
 */
fun buildDeviceSettings(settings: List<Pair<Int, Boolean>>): ByteArray {
    val writer = GarminByteWriter()
        .writeByte(settings.size)
    for ((ordinal, value) in settings) {
        writer
            .writeByte(ordinal)
            .writeByte(1) // a boolean is one byte
            .writeByte(if (value) 1 else 0)
    }
    return GarminGfdiFrame.build(GarminMessageId.DEVICE_SETTINGS, writer.toBytes())
}

/**
 * Answers the watch's time request (`CurrentTimeRequestMessage`): the ack
 * envelope grown by the time itself, the zone's total UTC offset, and the next
 * two DST transitions so the watch can flip its clock on the right night
 * without asking again.
 *
 * Without this reply the watch has NO clock source when Garmin Connect is not
 * installed — it drifts, and never follows DST or travel.
 */
fun buildCurrentTimeResponse(
    referenceId: Long,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): ByteArray {
    val rules = zone.rules
    val offsetSeconds = rules.getOffset(now).totalSeconds

    // Guarded like Gadgetbridge's (#5914): some zone databases throw here.
    val nextTransition = runCatching { rules.nextTransition(now) }.getOrNull()
    val transitionStarts = nextTransition?.let { GarminTime.fromInstant(it.instant) } ?: 0L
    val transitionEnds = nextTransition
        ?.let { runCatching { rules.nextTransition(it.instant) }.getOrNull() }
        ?.let { GarminTime.fromInstant(it.instant) }
        ?: 0L

    val writer = GarminByteWriter()
        .writeShort(GarminMessageId.CURRENT_TIME_REQUEST)
        .writeByte(GarminStatus.ACK.code)
        .writeInt(referenceId)
        .writeInt(GarminTime.fromInstant(now))
        .writeInt(offsetSeconds.toLong())
        .writeInt(transitionEnds)
        .writeInt(transitionStarts)
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}

/**
 * The acknowledgement a protobuf message needs — chunked or not.
 *
 * A generic ACK says the FRAME arrived; this says the protobuf message with
 * that request id was kept. Both are needed, and sending only the generic one
 * is why the watch retransmitted its own settings messages every five seconds
 * for as long as the link stayed open: at the protobuf layer they had never
 * been acknowledged at all. Gadgetbridge starts a complete message with a
 * generic status and then replaces it with this one the moment its handler
 * takes the message (`ProtocolBufferHandler.processIncoming`).
 *
 * For a chunk it also unblocks the next piece: without it the watch sends the
 * first 487 bytes of a 1013-byte screen and waits forever.
 *
 * Shape from Gadgetbridge's `ProtobufStatusMessage`: the usual response
 * envelope, then the request id, THE OFFSET THE MESSAGE DECLARED, and two
 * status bytes (kept = 0, no error = 0).
 *
 * [dataOffset] is the offset received, not the next one expected — echoing
 * the next left the watch resending chunk zero indefinitely.
 */
fun buildProtobufAck(
    originalMessageType: Int,
    requestId: Int,
    dataOffset: Long,
): ByteArray {
    val writer = GarminByteWriter()
        .writeShort(originalMessageType)
        .writeByte(GarminStatus.ACK.code)
        .writeShort(requestId)
        .writeInt(dataOffset)
        .writeByte(0) // ProtobufChunkStatus.KEPT
        .writeByte(0) // ProtobufStatusCode.NO_ERROR
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}

/**
 * Message types this app answers with their OWN response envelope, which
 * already serves as the acknowledgement — sending a second, generic one would
 * be a duplicate reply to the same message.
 *
 * `RESPONSE` itself is here because an ack must never be acked
 * (Gadgetbridge's "don't ack the ack"), which would otherwise bounce forever.
 */
val garminSelfAcknowledgedTypes: Set<Int> = setOf(
    GarminMessageId.RESPONSE,
    // Answered by a time-bearing response envelope; a bare ACK first would be
    // a second reply to the same ask.
    GarminMessageId.CURRENT_TIME_REQUEST,
    GarminMessageId.DEVICE_INFORMATION,
    GarminMessageId.AUTH_NEGOTIATION,
    GarminMessageId.FILE_TRANSFER_DATA,
    // Gets a purpose-built status carrying four extra payload bytes; a
    // generic ACK is too short and the watch keeps asking.
    GarminMessageId.NOTIFICATION_SUBSCRIPTION,
    // Likewise: a notification control request is answered by a three-byte
    // control status. Sending a generic ACK as well would be a second reply
    // to one question — the same double-reply that made the watch retransmit
    // its protobuf messages below.
    GarminMessageId.NOTIFICATION_CONTROL,
    // Acknowledged by the protobuf transport itself, which is the only thing
    // that knows the request id and offset a protobuf status has to name —
    // complete or chunked, both get one. Acking here as well sent TWO for
    // every message, and the watch answered that by retransmitting.
    GarminMessageId.PROTOBUF_REQUEST,
    GarminMessageId.PROTOBUF_RESPONSE,
)

/**
 * Acknowledges a received file-transfer chunk: `RESPONSE` envelope naming
 * FILE_TRANSFER_DATA, ACK + OK, and the offset reached
 * (`FileTransferDataStatusMessage`).
 */
fun buildFileTransferDataAck(dataOffsetReached: Int): ByteArray {
    val writer = GarminByteWriter()
        .writeShort(GarminMessageId.FILE_TRANSFER_DATA)
        .writeByte(GarminStatus.ACK.code)
        .writeByte(0) // TransferStatus.OK
        .writeInt(dataOffsetReached)
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}

/**
 * Bit for [GarminFileFlag.ARCHIVE]/`DELETE` — the value is `1 shl ordinal`
 * in Garmin's table, so ARCHIVE (ordinal 4) is 0x10, matching
 * `SetFileFlagsMessage.FileFlags`.
 */
enum class GarminFileFlag(val bit: Int) {
    ARCHIVE(0x10),
    DELETE(0x20),
}

/**
 * Marks a downloaded file archived, so the watch does not re-offer it next
 * sync (`SetFileFlagsMessage`).
 */
fun buildSetFileFlags(fileIndex: Int, flag: GarminFileFlag): ByteArray {
    val writer = GarminByteWriter()
        .writeShort(fileIndex)
        .writeByte(flag.bit)
    return GarminGfdiFrame.build(GarminMessageId.SET_FILE_FLAGS, writer.toBytes())
}

/**
 * A system event carrying a single byte value (0 for the sync lifecycle
 * events like SYNC_READY / SYNC_COMPLETE).
 */
fun buildSystemEvent(event: GarminSystemEventType, value: Int = 0): ByteArray {
    val writer = GarminByteWriter()
        .writeByte(event.ordinal)
        .writeByte(value)
    return GarminGfdiFrame.build(GarminMessageId.SYSTEM_EVENT, writer.toBytes())
}

/**
 * Asks the watch for the file types it supports (`SupportedFileTypesMessage`
 * — no payload).
 */
fun buildSupportedFileTypesRequest(): ByteArray =
    GarminGfdiFrame.build(GarminMessageId.SUPPORTED_FILE_TYPES_REQUEST, ByteArray(0))

/**
 * Our half of the device-information exchange: a RESPONSE envelope that both
 * ACKs the watch's message and describes this phone.
 *
 * The sentinel values are Gadgetbridge's, kept verbatim because they are what
 * a watch has been observed to accept: protocol 150, software 7791, and `-1`
 * (all-ones) for unit/product/max-packet, i.e. "unspecified".
 *
 * `protocolFlags` mirrors the watch's own protocol generation — 1 when it
 * reports a 1xx protocol, else 0.
 */
fun buildDeviceInformationResponse(
    incoming: GarminDeviceInformation,
    bluetoothName: String,
    manufacturer: String,
    model: String,
): ByteArray {
    val ourProtocolVersion = 150
    val ourSoftwareVersion = 7791
    val unspecifiedShort = 0xFFFF
    val unspecifiedInt = 0xFFFFFFFFL

    val writer = GarminByteWriter()
        .writeShort(GarminMessageId.DEVICE_INFORMATION)
        .writeByte(GarminStatus.ACK.code)
        .writeShort(ourProtocolVersion)
        .writeShort(unspecifiedShort) // product number
        .writeInt(unspecifiedInt) // unit number
        .writeShort(ourSoftwareVersion)
        .writeShort(unspecifiedShort) // max packet size
        .writeString(bluetoothName)
        .writeString(manufacturer)
        .writeString(model)
        .writeByte(if (incoming.protocolVersion / 100 == 1) 1 else 0)
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}

/**
 * This app's capability bitmap, the reply to a [GarminConfiguration].
 *
 * One bit per `GarminCapability` ordinal, 120 capabilities in 15 bytes —
 * exactly the length a real vívoactive 5 sends. The value is Gadgetbridge's
 * `OUR_CAPABILITIES`: everything set except `UNK_104..UNK_111` and
 * `UNK_114..UNK_119`, which its authors note have never been seen in a Garmin
 * Connect dump.
 *
 * Claiming capabilities this app does not implement (music, LiveTrack,
 * ConnectIQ) is deliberate and matches Gadgetbridge: the watch uses the
 * bitmap to decide what it may OFFER, and a narrower claim has been observed
 * to make devices withhold data. Nothing is obliged to act on an offer it
 * never accepts.
 */
val garminOurCapabilities: ByteArray = byteArrayOf(
    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
    0xFF.toByte(), 0x00, 0x03,
)

/**
 * Sends our capabilities back (`ConfigurationMessage`). Note this is a
 * CONFIGURATION message in its own right, not a RESPONSE envelope — the watch
 * gets both this and a plain ACK, as Gadgetbridge sends both.
 */
fun buildConfigurationResponse(): ByteArray {
    val writer = GarminByteWriter()
        .writeByte(garminOurCapabilities.size)
        .writeBytes(garminOurCapabilities)
    return GarminGfdiFrame.build(GarminMessageId.CONFIGURATION, writer.toBytes())
}

/**
 * Answers a notification-subscription request
 * (`NotificationSubscriptionStatusMessage`).
 *
 * [enabled] is what actually turns forwarding on: until the watch has been
 * told ENABLED it sends no control requests at all, so nothing else in the
 * notification path can happen. A session with no notifications handler
 * answers DISABLED, which is what every sync, find and settings session does
 * and did before this existed.
 *
 * The watch's own flag and unknown byte are echoed back, as Gadgetbridge
 * does.
 */
fun buildNotificationSubscriptionStatus(
    incoming: GarminNotificationSubscription,
    enabled: Boolean,
): ByteArray {
    // `NotificationStatus` ordinals: ENABLED is 0, DISABLED is 1.
    val notificationStatusEnabled = 0
    val notificationStatusDisabled = 1
    val writer = GarminByteWriter()
        .writeShort(GarminMessageId.NOTIFICATION_SUBSCRIPTION)
        .writeByte(GarminStatus.ACK.code)
        .writeByte(if (enabled) notificationStatusEnabled else notificationStatusDisabled)
        .writeByte(if (incoming.enable) 1 else 0)
        .writeByte(incoming.unknown)
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}

/**
 * Announces a notification to the watch (`NotificationUpdateMessage`, 5033).
 *
 * Carries NO text — only an id, a category and some counters. The watch
 * decides from this whether it wants the notification at all, and asks for
 * the words separately. See `GarminNotificationMessages.kt`.
 *
 * [count] is how many notifications of the same category are currently
 * outstanding, which is what a watch face's per-category badge shows.
 */
fun buildNotificationUpdate(
    updateType: GarminNotificationUpdateType,
    category: GarminNotificationCategory,
    count: Int,
    notificationId: Long,
    hasActions: Boolean = false,
    hasAttachments: Boolean = false,
): ByteArray {
    var phoneFlags = 0
    if (hasActions) phoneFlags = phoneFlags or GarminNotificationPhoneFlag.NEW_ACTIONS.bit
    if (hasAttachments) {
        phoneFlags = phoneFlags or GarminNotificationPhoneFlag.HAS_ATTACHMENTS.bit
    }
    val writer = GarminByteWriter()
        .writeByte(updateType.ordinal)
        .writeByte(garminNotificationCategoryFlags())
        .writeByte(category.ordinal)
        .writeByte(count)
        .writeInt(notificationId)
        .writeByte(phoneFlags)
    return GarminGfdiFrame.build(GarminMessageId.NOTIFICATION_UPDATE, writer.toBytes())
}

/**
 * One chunk of an attribute blob (`NotificationDataMessage`, 5035).
 *
 * [runningCrc] is cumulative over everything sent so far, not over this chunk
 * alone — the same running-CRC scheme the download path verifies on the way
 * in, just run in the other direction.
 */
fun buildNotificationData(
    chunk: ByteArray,
    totalSize: Int,
    dataOffset: Int,
    runningCrc: Int,
): ByteArray {
    val writer = GarminByteWriter()
        .writeShort(totalSize)
        .writeShort(runningCrc)
        .writeShort(dataOffset)
        .writeBytes(chunk)
    return GarminGfdiFrame.build(GarminMessageId.NOTIFICATION_DATA, writer.toBytes())
}

/**
 * Acknowledges a notification control request
 * (`NotificationControlStatusMessage`).
 *
 * Three payload bytes after the message id, not the one a generic ACK carries
 * — which is why 5034 is in [garminSelfAcknowledgedTypes].
 */
fun buildNotificationControlStatus(ok: Boolean = true): ByteArray {
    val chunkStatusOk = 0
    val chunkStatusError = 1
    val statusCodeNoError = 0
    val statusCodeUnknownCommand = 160
    val writer = GarminByteWriter()
        .writeShort(GarminMessageId.NOTIFICATION_CONTROL)
        .writeByte(GarminStatus.ACK.code)
        .writeByte(if (ok) chunkStatusOk else chunkStatusError)
        .writeByte(if (ok) statusCodeNoError else statusCodeUnknownCommand)
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}

/**
 * Tells the watch the attribute blob is fully sent — the phone's own
 * `NotificationDataStatusMessage`, ACK + OK.
 *
 * Sent once the last chunk has been acknowledged. Without it the watch keeps
 * the transfer open waiting for more.
 */
fun buildNotificationDataFinalAck(): ByteArray {
    val transferStatusOk = 0
    val writer = GarminByteWriter()
        .writeShort(GarminMessageId.NOTIFICATION_DATA)
        .writeByte(GarminStatus.ACK.code)
        .writeByte(transferStatusOk)
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}

/**
 * Answers a [GarminSynchronization] announcement (`FilterMessage`).
 *
 * The single payload byte is `FilterType.UNK_3` — Gadgetbridge's name for it,
 * meaning nobody has worked out what the other values do. It is sent verbatim
 * because it is what a real watch is known to accept.
 */
fun buildFilterMessage(): ByteArray {
    val filterTypeUnk3 = 3
    val writer = GarminByteWriter().writeByte(filterTypeUnk3)
    return GarminGfdiFrame.build(GarminMessageId.FILTER, writer.toBytes())
}

/**
 * Answers the auth challenge with ACK + `GUESS_OK`, echoing the watch's own
 * unknown byte and flags back at it — the "no authentication" handshake.
 */
fun buildAuthNegotiationResponse(incoming: GarminAuthNegotiation): ByteArray {
    val guessOk = 0
    val writer = GarminByteWriter()
        .writeShort(GarminMessageId.AUTH_NEGOTIATION)
        .writeByte(GarminStatus.ACK.code)
        .writeByte(guessOk)
        .writeByte(incoming.unknown)
        .writeInt(incoming.authFlags)
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}
