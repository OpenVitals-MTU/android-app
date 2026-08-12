package tech.mmarca.openvitals.devices.garmin

/**
 * A minimal protobuf writer/reader, for the handful of Garmin messages this
 * app sends and reads.
 *
 * Deliberately hand-rolled rather than generated. The alternative is a
 * protobuf runtime plus a `protoc` step in the build, to encode messages that
 * amount to a nested field and an integer — a code generator, a toolchain
 * dependency and a build phase, for a few dozen bytes. Everything here is
 * wire types 0 (varint) and 2 (length-delimited); nothing Garmin sends on the
 * paths this app uses needs more.
 *
 * Field numbers come from Gadgetbridge's `.proto` files (AGPLv3), named at
 * each call site so a reader can check them against the schema.
 */

/** Builds one protobuf message. */
class ProtobufWriter {

    private val bytes = mutableListOf<Byte>()

    /** A varint field (wire type 0) — the only numeric encoding used here. */
    fun varint(field: Int, value: Long): ProtobufWriter {
        key(field, 0)
        varintValue(value)
        return this
    }

    fun varint(field: Int, value: Int): ProtobufWriter = varint(field, value.toLong())

    /** A length-delimited field (wire type 2): a nested message, or bytes. */
    fun nested(field: Int, value: ByteArray): ProtobufWriter {
        key(field, 2)
        varintValue(value.size.toLong())
        for (b in value) bytes.add(b)
        return this
    }

    /**
     * A nested message with no fields set.
     *
     * Not the same as omitting it: protobuf distinguishes an absent field
     * from a present-but-empty one, and Garmin uses the empty message as the
     * whole request for actions that take no arguments (cancelling a find,
     * for one).
     */
    fun emptyMessage(field: Int): ProtobufWriter = nested(field, ByteArray(0))

    fun string(field: Int, value: String): ProtobufWriter =
        nested(field, value.toByteArray(Charsets.ISO_8859_1))

    /** A float field (wire type 5, fixed 32-bit little-endian). */
    fun fixed32(field: Int, value: Float): ProtobufWriter {
        key(field, 5)
        val bits = java.lang.Float.floatToIntBits(value)
        for (i in 0 until 4) bytes.add(((bits shr (8 * i)) and 0xFF).toByte())
        return this
    }

    /** A sint32 field: zigzag-encoded varint, for values that can go negative. */
    fun sint32(field: Int, value: Int): ProtobufWriter {
        key(field, 0)
        varintValue(((value shl 1) xor (value shr 31)).toLong() and 0xFFFFFFFFL)
        return this
    }

    private fun key(field: Int, wireType: Int) =
        varintValue(((field shl 3) or wireType).toLong())

    private fun varintValue(value: Long) {
        var v = value
        while (v >= 0x80 || v < 0) {
            bytes.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        bytes.add(v.toByte())
    }

    fun toBytes(): ByteArray = bytes.toByteArray()
}

/**
 * One decoded protobuf field: its number, and its value in whichever form its
 * wire type carried.
 */
class ProtobufField(
    val field: Int,
    val wireType: Int,
    val varint: Long? = null,
    val bytes: ByteArray? = null,
)

/**
 * Reads the top-level fields of a protobuf message.
 *
 * Shallow on purpose: nesting is resolved by calling this again on a field's
 * bytes, which keeps the reader honest about not knowing the schema.
 */
fun readProtobuf(data: ByteArray): List<ProtobufField> {
    val out = mutableListOf<ProtobufField>()
    var i = 0

    fun readVarint(): Long? {
        var result = 0L
        var shift = 0
        while (i < data.size) {
            val byte = data[i++].toInt() and 0xFF
            result = result or ((byte and 0x7F).toLong() shl shift)
            if (byte and 0x80 == 0) return result
            shift += 7
            // A varint longer than ten bytes is corrupt, not a big number.
            if (shift > 63) return null
        }
        return null
    }

    while (i < data.size) {
        val key = readVarint() ?: break
        val field = (key shr 3).toInt()
        val wireType = (key and 0x07).toInt()
        when (wireType) {
            0 -> {
                val value = readVarint() ?: return out
                out.add(ProtobufField(field = field, wireType = 0, varint = value))
            }
            2 -> {
                val length = readVarint()?.toInt()
                if (length == null || length < 0 || i + length > data.size) return out
                out.add(
                    ProtobufField(
                        field = field,
                        wireType = 2,
                        bytes = data.copyOfRange(i, i + length),
                    ),
                )
                i += length
            }
            5 -> {
                if (i + 4 > data.size) return out
                i += 4
            }
            1 -> {
                if (i + 8 > data.size) return out
                i += 8
            }
            else ->
                // An unknown wire type means the rest cannot be located; stop
                // rather than walk off into noise.
                return out
        }
    }
    return out
}

/** The first field numbered [field], or null. */
fun protobufField(fields: List<ProtobufField>, field: Int): ProtobufField? =
    fields.firstOrNull { it.field == field }

/**
 * Field numbers in Garmin's top-level `Smart` message
 * (`gdi_smart_proto.proto`). Only the services this app speaks are listed.
 */
object GarminSmartService {
    const val CALENDAR = 1
    const val HTTP = 2
    const val DATA_TRANSFER = 7
    const val DEVICE_STATUS = 8
    const val CORE = 13
    const val AUTHENTICATION = 27
    const val FIND_MY_WATCH = 12
    const val SETTINGS = 42
}
