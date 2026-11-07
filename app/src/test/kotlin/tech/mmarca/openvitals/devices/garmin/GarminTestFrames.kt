package tech.mmarca.openvitals.devices.garmin

/** Frames a fake watch sends, shared by the session, protobuf and FileSync tests. */

/** A complete protobuf request from the watch: `[u16 id][u32 offset][u32 total][u32 length][bytes]`. */
internal fun protobufRequest(requestId: Int, payload: ByteArray): ByteArray = GarminGfdiFrame.build(
    GarminMessageId.PROTOBUF_REQUEST,
    GarminByteWriter()
        .writeShort(requestId)
        .writeInt(0)
        .writeInt(payload.size)
        .writeInt(payload.size)
        .writeBytes(payload)
        .toBytes(),
)

/** A FileSyncService message in its Smart envelope. */
internal fun smartFileSync(field: Int, body: ByteArray = ProtobufWriter().varint(2, 24).toBytes()): ByteArray =
    GarminFileSyncProtocol.smart(ProtobufWriter().nested(field, body).toBytes())

/** The newer firmware's announcement: a FileSyncService "new files" notice, as a protobuf request. */
internal fun fileSyncAnnouncement(requestId: Int = 9): ByteArray =
    protobufRequest(requestId = requestId, payload = smartFileSync(12))

/** The legacy announcement that the watch holds data. Bit 26 is SLEEP. */
internal fun syncAnnouncement(bits: Long = 1L shl 26): ByteArray = GarminGfdiFrame.build(
    GarminMessageId.SYNCHRONIZATION,
    GarminByteWriter().writeByte(2).writeByte(8).writeLong(bits).toBytes(),
)
