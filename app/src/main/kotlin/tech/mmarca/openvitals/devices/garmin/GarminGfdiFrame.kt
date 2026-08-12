package tech.mmarca.openvitals.devices.garmin

/**
 * A decoded GFDI frame: a message type and its raw payload.
 *
 * The frame is what COBS wraps. Layout (little-endian), from Gadgetbridge's
 * `GFDIMessage`:
 *
 *   `[u16 length][u16 messageType][payload…][u16 crc]`
 *
 * where `length` is the WHOLE frame (its own 2 bytes, the type, the payload
 * and the CRC), and `crc` covers everything before it.
 *
 * The `messageType` high-bit convention is handled here so the message layer
 * sees a single flat id: an incoming type with `0x8000` set is a status/ack
 * response, remapped to `(type and 0xff) + 5000` exactly as
 * `GFDIMessage.parseIncoming` does.
 */
class GarminGfdiFrame(val messageType: Int, val payload: ByteArray) {

    companion object {
        private const val STATUS_FLAG = 0x8000
        private const val STATUS_BASE = 5000

        /**
         * Parses one COBS-decoded frame. Throws [GarminGfdiFrameException] on
         * a length or CRC mismatch — a corrupt frame must not be mistaken for
         * a short one and silently mis-dispatched.
         */
        fun parse(bytes: ByteArray): GarminGfdiFrame {
            if (bytes.size < 6) {
                throw GarminGfdiFrameException("Frame too short: ${bytes.size} bytes")
            }
            val reader = GarminByteReader(bytes)
            val length = reader.readShort()
            if (length != bytes.size) {
                throw GarminGfdiFrameException(
                    "Frame length field $length != actual ${bytes.size}",
                )
            }
            val expectedCrc = GarminCrc.compute(bytes, length = length - 2)
            val actualCrc = (bytes[length - 2].toInt() and 0xFF) or
                ((bytes[length - 1].toInt() and 0xFF) shl 8)
            if (expectedCrc != actualCrc) {
                throw GarminGfdiFrameException(
                    "Frame CRC $actualCrc != computed $expectedCrc",
                )
            }

            var messageType = reader.readShort()
            if ((messageType and STATUS_FLAG) != 0) {
                messageType = (messageType and 0xFF) + STATUS_BASE
            }
            // Payload is everything between the type and the CRC.
            val payload = bytes.copyOfRange(4, length - 2)
            return GarminGfdiFrame(messageType, payload)
        }

        /**
         * Builds a wire frame for [messageType] carrying [payload]: writes a
         * placeholder length, the type, the payload, backfills the real
         * length, then appends the CRC over everything so far.
         */
        fun build(messageType: Int, payload: ByteArray): ByteArray {
            val writer = GarminByteWriter(payload.size + 8)
                .writeShort(0) // Length placeholder, patched below.
                .writeShort(messageType)
                .writeBytes(payload)
            val length = writer.length + 2 // + the CRC about to be written.
            writer.patchShort(0, length)
            val crc = GarminCrc.compute(writer.toBytes())
            writer.writeShort(crc)
            return writer.toBytes()
        }
    }
}

class GarminGfdiFrameException(message: String) : Exception(message)
