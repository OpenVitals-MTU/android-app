package tech.mmarca.openvitals.devices.garmin

import java.time.Instant
import java.time.ZoneId

/**
 * The GFDI messages a read-only FIT sync and the notification service need.
 * Pure: bytes to typed values and back, no I/O. Parsing is one `when` in
 * [decodeGarminMessage]; each outgoing message is a small builder.
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

    // Notification service (GNCS). See GarminNotificationMessages.kt.
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

/** GFDI status codes. Only ACK means the request proceeded. */
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

/** System events the sync sends. Ordinal is the wire value; do not reorder. */
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

/** A parsed inbound message. Unknown types decode to [GarminUnhandledMessage]. */
sealed class GarminInboundMessage

/** A plain ACK/NAK envelope (type 5000) for a message we sent. */
data class GarminGenericStatus(
    val originalMessageType: Int,
    val status: GarminStatus,
) : GarminInboundMessage()

/** Response to a download request. [maxFileSize] is the total byte length. */
data class GarminDownloadRequestStatus(
    val status: GarminStatus,
    val downloadStatus: GarminDownloadStatus,
    val maxFileSize: Long,
) : GarminInboundMessage() {
    val canProceed: Boolean
        get() = status == GarminStatus.ACK && downloadStatus == GarminDownloadStatus.OK
}

/** One chunk of a download (type 5004). [crc] is the running CRC up to this chunk. */
class GarminFileTransferData(
    val dataOffset: Long,
    val crc: Int,
    val data: ByteArray,
) : GarminInboundMessage()

/** The watch introducing itself (type 5024). [maxPacketSize] caps a single write. */
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
 * The auth challenge (type 5101). GFDI has no real authentication; the
 * Bluetooth bond is the security boundary.
 */
data class GarminAuthNegotiation(
    val unknown: Int,
    val authFlags: Long,
) : GarminInboundMessage()

/** The `(dataType, subType)` pairs the watch holds. */
data class GarminSupportedFileTypes(
    val status: GarminStatus,
    /** Every advertised pair, including unmapped ones, for diagnosis. */
    val types: List<GarminSupportedFileType>,
) : GarminInboundMessage()

data class GarminSupportedFileType(
    val dataType: Int,
    val subType: Int,
    val name: String,
)

/**
 * The watch announcing what it holds (type 5037), as a bitmask. Answered with
 * [buildFilterMessage] before the directory is fetched.
 */
data class GarminSynchronization(
    val syncType: Int,
    val bitmask: Long,
) : GarminInboundMessage() {

    private fun has(ordinal: Int): Boolean = (bitmask shr ordinal) and 1L == 1L

    /** Whether the announcement contains anything this app would want. */
    val shouldProceed: Boolean
        get() = has(WORKOUTS) || has(ACTIVITIES) || has(ACTIVITY_SUMMARY) || has(SLEEP)

    /** The set bits, for the log. */
    val setBits: List<Int>
        get() = (0 until 64).filter { has(it) }

    private companion object {
        /** Ordinals of the categories worth acting on. */
        const val WORKOUTS = 3
        const val ACTIVITIES = 5
        const val ACTIVITY_SUMMARY = 21
        const val SLEEP = 26
    }
}

/**
 * The capabilities exchange (type 5050). The watch expects our capabilities
 * back; a bare ACK left it re-sending and never listing files.
 */
class GarminConfiguration(
    /** The raw bitmap, one bit per capability ordinal. */
    val capabilityBits: ByteArray,
) : GarminInboundMessage()

/**
 * The watch asking whether to route notifications (type 5036). Needs the
 * four-byte status reply, or it retransmits every second.
 */
data class GarminNotificationSubscription(
    val enable: Boolean,
    val unknown: Int,
) : GarminInboundMessage()

/**
 * The watch asking about a notification (type 5034). Which fields are set
 * depends on [command]: attributes for GET_*_ATTRIBUTES, action fields for
 * PERFORM_*_ACTION.
 */
data class GarminNotificationControl(
    val command: GarminNotificationCommand,
    val notificationId: Long = 0,
    /** Requested attribute to max length, in the watch's order. The answer keeps it. */
    val attributes: Map<GarminNotificationAttribute, Int> = emptyMap(),
    val appIdentifier: String? = null,
    val appAttributes: List<Int> = emptyList(),
    val actionCode: Int? = null,
    val actionText: String? = null,
) : GarminInboundMessage()

/** The watch's verdict on one attribute chunk. [canProceed] gates the next chunk. */
data class GarminNotificationDataStatus(
    val status: GarminStatus,
    val transferStatus: GarminNotificationTransferStatus,
) : GarminInboundMessage() {
    val canProceed: Boolean
        get() = status == GarminStatus.ACK &&
            transferStatus == GarminNotificationTransferStatus.OK
}

/** The watch asking for the time. */
data class GarminCurrentTimeRequest(
    /** Echoed back in the response. */
    val referenceId: Long,
) : GarminInboundMessage()

/** The watch asking for weather: when the glance opens, and periodically. */
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

/** The watch announcing a file it just wrote, while a link is open. */
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
    // Unknown types take the unhandled path: an ack and a log.
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
                // An unknown attribute may or may not carry a length, so stop here.
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
            // Non-reply actions carry no text on recent firmware.
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
        // RESEND is recoverable, a CRC mismatch is not. No transfer byte means OK:
        // only our own final ack bounces back in that shape.
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

// Outgoing message builders. Each returns a GFDI frame ready to COBS.

/** Whether a download starts fresh or continues (`REQUEST_TYPE` ordinal). */
enum class GarminDownloadRequestType { CONTINUE_TRANSFER, FRESH }

/** Requests file [fileIndex]. A fresh download leaves offset, size and CRC seed at 0. */
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
 * Acknowledges an inbound message. Gadgetbridge acks every message; without
 * it the watch retransmits and never moves on.
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
 * Configures the watch over DEVICE_SETTINGS (5026). Sent every connection:
 * WEATHER_CONDITIONS_ENABLED switches on the watch's whole weather feature.
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
 * Answers the time request with the time, the UTC offset and the next two DST
 * transitions. Without Garmin Connect this is the watch's only clock source.
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
 * Acknowledges a protobuf message, chunked or not. A generic ACK covers the
 * frame; without this one the watch retransmits every five seconds, and a
 * chunked message never gets its next piece. [dataOffset] is the offset
 * received, not the next expected.
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
 * Types answered with their own response envelope, so a generic ACK would be
 * a duplicate. RESPONSE is here because an ack must never be acked.
 */
val garminSelfAcknowledgedTypes: Set<Int> = setOf(
    GarminMessageId.RESPONSE,
    GarminMessageId.CURRENT_TIME_REQUEST,
    GarminMessageId.DEVICE_INFORMATION,
    GarminMessageId.AUTH_NEGOTIATION,
    GarminMessageId.FILE_TRANSFER_DATA,
    GarminMessageId.NOTIFICATION_SUBSCRIPTION,
    GarminMessageId.NOTIFICATION_CONTROL,
    // Acked by the protobuf transport, which knows the request id and offset.
    GarminMessageId.PROTOBUF_REQUEST,
    GarminMessageId.PROTOBUF_RESPONSE,
)

/** Acknowledges a file-transfer chunk with the offset reached. */
fun buildFileTransferDataAck(dataOffsetReached: Int): ByteArray {
    val writer = GarminByteWriter()
        .writeShort(GarminMessageId.FILE_TRANSFER_DATA)
        .writeByte(GarminStatus.ACK.code)
        .writeByte(0) // TransferStatus.OK
        .writeInt(dataOffsetReached)
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}

/** File flag bits: `1 shl ordinal` in Garmin's table, so ARCHIVE is 0x10. */
enum class GarminFileFlag(val bit: Int) {
    ARCHIVE(0x10),
    DELETE(0x20),
}

/** Marks a downloaded file archived so the watch does not re-offer it. */
fun buildSetFileFlags(fileIndex: Int, flag: GarminFileFlag): ByteArray {
    val writer = GarminByteWriter()
        .writeShort(fileIndex)
        .writeByte(flag.bit)
    return GarminGfdiFrame.build(GarminMessageId.SET_FILE_FLAGS, writer.toBytes())
}

/** A system event with a single byte value. */
fun buildSystemEvent(event: GarminSystemEventType, value: Int = 0): ByteArray {
    val writer = GarminByteWriter()
        .writeByte(event.ordinal)
        .writeByte(value)
    return GarminGfdiFrame.build(GarminMessageId.SYSTEM_EVENT, writer.toBytes())
}

/** Asks the watch for the file types it supports. No payload. */
fun buildSupportedFileTypesRequest(): ByteArray =
    GarminGfdiFrame.build(GarminMessageId.SUPPORTED_FILE_TYPES_REQUEST, ByteArray(0))

/**
 * Our half of the device-information exchange: ACK plus a description of
 * this phone. The sentinel values are Gadgetbridge's and known to be accepted.
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
 * This app's capability bitmap, Gadgetbridge's OUR_CAPABILITIES. Claiming
 * unimplemented capabilities is deliberate: a narrower claim makes watches
 * withhold data.
 */
val garminOurCapabilities: ByteArray = byteArrayOf(
    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
    0xFF.toByte(), 0x00, 0x03,
)

/** Sends our capabilities back. A CONFIGURATION message, sent alongside the plain ACK. */
fun buildConfigurationResponse(): ByteArray {
    val writer = GarminByteWriter()
        .writeByte(garminOurCapabilities.size)
        .writeBytes(garminOurCapabilities)
    return GarminGfdiFrame.build(GarminMessageId.CONFIGURATION, writer.toBytes())
}

/**
 * Answers a notification-subscription request. [enabled] turns forwarding on;
 * until then the watch sends no control requests. The watch's flag and unknown
 * byte are echoed back.
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
 * Announces a notification (5033): id, category and counters, no text. The
 * watch asks for the words separately. [count] feeds the per-category badge.
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

/** One chunk of an attribute blob (5035). [runningCrc] covers everything sent so far. */
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

/** Acknowledges a notification control request: three payload bytes, not one. */
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

/** Tells the watch the attribute blob is fully sent. Without it the transfer stays open. */
fun buildNotificationDataFinalAck(): ByteArray {
    val transferStatusOk = 0
    val writer = GarminByteWriter()
        .writeShort(GarminMessageId.NOTIFICATION_DATA)
        .writeByte(GarminStatus.ACK.code)
        .writeByte(transferStatusOk)
    return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
}

/**
 * Answers a [GarminSynchronization]. The byte is Gadgetbridge's
 * FilterType.UNK_3, the value a real watch accepts.
 */
fun buildFilterMessage(): ByteArray {
    val filterTypeUnk3 = 3
    val writer = GarminByteWriter().writeByte(filterTypeUnk3)
    return GarminGfdiFrame.build(GarminMessageId.FILTER, writer.toBytes())
}

/** Answers the auth challenge with ACK + GUESS_OK, echoing the watch's bytes. */
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
