package tech.mmarca.openvitals.core.fit

/**
 * The FIT CRC-16, over a file's header and over the whole file. A literal
 * port of Gadgetbridge's nibble-table CRC (AGPLv3), also used for GFDI
 * framing. Bytes are read unsigned, which gives the identical result.
 */
object FitCrc {

    private val CONSTANTS = intArrayOf(
        0x0000, 0xCC01, 0xD801, 0x1400, 0xF001, 0x3C00, 0x2800, 0xE401,
        0xA001, 0x6C00, 0x7800, 0xB401, 0x5000, 0x9C01, 0x8801, 0x4400,
    )

    /** CRC over `data[offset until offset+length]`, seeded with [initialCrc]. */
    fun compute(
        data: ByteArray,
        offset: Int = 0,
        length: Int? = null,
        initialCrc: Int = 0,
    ): Int {
        val end = offset + (length ?: (data.size - offset))
        var crc = initialCrc
        for (i in offset until end) {
            val b = data[i].toInt() and 0xFF
            crc = (((crc shr 4) and 4095) xor CONSTANTS[crc and 15]) xor
                CONSTANTS[b and 15]
            crc = (((crc shr 4) and 4095) xor CONSTANTS[crc and 15]) xor
                CONSTANTS[(b shr 4) and 15]
        }
        return crc and 0xFFFF
    }
}
