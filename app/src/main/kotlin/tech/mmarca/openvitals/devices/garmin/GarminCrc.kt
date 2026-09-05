package tech.mmarca.openvitals.devices.garmin

import tech.mmarca.openvitals.core.fit.FitCrc

/** The GFDI packet checksum: the same CRC-16 as FIT files, delegated to [FitCrc]. */
object GarminCrc {

    /** CRC over `data[offset until offset+length]`, seeded with [initialCrc], masked to 16 bits. */
    fun compute(
        data: ByteArray,
        offset: Int = 0,
        length: Int? = null,
        initialCrc: Int = 0,
    ): Int = FitCrc.compute(data, offset, length, initialCrc)
}
