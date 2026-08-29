package tech.mmarca.openvitals.devices.garmin

import tech.mmarca.openvitals.core.fit.FitCrc

/**
 * Garmin GFDI packet checksum — the same nibble-table CRC-16 Garmin uses for
 * FIT files, so the one implementation lives in `core/fit` ([FitCrc]) and the
 * device layer delegates to it.
 */
object GarminCrc {

    /**
     * CRC over `data[offset until offset+length]`, seeded with [initialCrc]
     * (0 for a whole packet). Masked to 16 bits, matching Gadgetbridge's
     * `(short)` truncation before it is written into the frame.
     */
    fun compute(
        data: ByteArray,
        offset: Int = 0,
        length: Int? = null,
        initialCrc: Int = 0,
    ): Int = FitCrc.compute(data, offset, length, initialCrc)
}
