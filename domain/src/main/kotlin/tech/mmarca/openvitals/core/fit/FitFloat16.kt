package tech.mmarca.openvitals.core.fit

/**
 * Decodes one IEEE 754 half-precision value from its 16 bits. Garmin packs
 * sleep features this way. Written out because `Float.float16ToFloat` needs
 * Java 20 and the app targets 17.
 */
fun fitFloat16(bits: Int): Float {
    val sign = if (bits and 0x8000 != 0) -1f else 1f
    val exponent = (bits ushr 10) and 0x1F
    val mantissa = bits and 0x3FF
    return when (exponent) {
        0 -> sign * mantissa * SubnormalScale
        0x1F -> if (mantissa == 0) sign * Float.POSITIVE_INFINITY else Float.NaN
        else -> {
            val signBit = if (sign < 0) 1 shl 31 else 0
            Float.fromBits(signBit or ((exponent + ExponentRebias) shl 23) or (mantissa shl 13))
        }
    }
}

/** Every little-endian half-float in this array, in order. A trailing odd byte is ignored. */
fun ByteArray.fitFloat16Array(): FloatArray {
    val out = FloatArray(size / 2)
    for (index in out.indices) {
        val low = this[2 * index].toInt() and 0xFF
        val high = this[2 * index + 1].toInt() and 0xFF
        out[index] = fitFloat16(low or (high shl 8))
    }
    return out
}

/** 2^-24: the value of one subnormal mantissa step. */
private const val SubnormalScale = 5.9604645E-8f

/** float32 bias (127) minus float16 bias (15). */
private const val ExponentRebias = 112
