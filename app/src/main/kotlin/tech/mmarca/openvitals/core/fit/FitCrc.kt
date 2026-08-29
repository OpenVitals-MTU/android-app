package tech.mmarca.openvitals.core.fit

/**
 * The FIT CRC-16 — the checksum over a FIT file's header (bytes 0..11 into
 * bytes 12..13) and over the whole file into the trailing two bytes.
 *
 * Byte-for-byte port of Gadgetbridge's `ChecksumCalculator.computeCrc`
 * (AGPLv3, the same licence as this app) — a nibble-table CRC that Garmin
 * uses both for FIT files and for GFDI packet framing ([GarminCrc] in the
 * device layer delegates here), so it must be ported literally rather than
 * swapped for a library CRC-16.
 *
 * Bytes are read unsigned (`b and 0xFF`) before the nibble selects, which
 * gives the identical result to Gadgetbridge's signed-byte arithmetic —
 * `b and 15` and `(b shr 4) and 15` select the same two nibbles either way,
 * and that is all the algorithm reads from each byte.
 */
object FitCrc {

    private val CONSTANTS = intArrayOf(
        0x0000, 0xCC01, 0xD801, 0x1400, 0xF001, 0x3C00, 0x2800, 0xE401,
        0xA001, 0x6C00, 0x7800, 0xB401, 0x5000, 0x9C01, 0x8801, 0x4400,
    )

    /**
     * CRC over `data[offset until offset+length]`, seeded with [initialCrc]
     * (0 for a whole file or packet). Masked to 16 bits.
     */
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
