package tech.mmarca.openvitals.devices.garmin

import tech.mmarca.openvitals.core.fit.FitRecordDecoder

/**
 * Capabilities from the FIT records some watches push instead of a
 * CONFIGURATION message: `connectivity_supported` on the FIT `capabilities`
 * message, bit for bit the CONFIGURATION bitmap. Which field carries it is
 * a best guess, so the caveat stays.
 */
class GarminFitCapabilities {

    private val records = FitRecordDecoder()

    /** Applies a FIT_DEFINITION payload. Malformed bytes are logged and dropped. */
    fun define(payload: ByteArray) {
        try {
            records.read(payload)
        } catch (error: IllegalArgumentException) {
            GarminLog.log("[GARMIN-SYNC] FIT definition not understood: ${error.message}")
        }
    }

    /**
     * Decodes a FIT_DATA payload: the capability set when a `capabilities`
     * record carries one, else null. Malformed bytes, or a record whose
     * definition never arrived, are logged and read as null.
     */
    fun capabilitiesIn(payload: ByteArray): Set<GarminCapability>? {
        val decoded = try {
            records.read(payload)
        } catch (error: IllegalArgumentException) {
            GarminLog.log("[GARMIN-SYNC] FIT data not understood: ${error.message}")
            return null
        }
        val mask = decoded
            .firstOrNull { it.globalMessageNumber == FIT_MESSAGE_CAPABILITIES }
            ?.values
            ?.get(FIT_FIELD_CONNECTIVITY_SUPPORTED)
            ?: return null
        // Bit i of the mask is capability i, as in the CONFIGURATION bitmap.
        val bits = ByteArray(Long.SIZE_BYTES) { index -> ((mask ushr (index * 8)) and 0xFF).toByte() }
        return GarminCapability.decode(bits)
    }

    private companion object {
        /** The FIT profile's `capabilities` message. */
        const val FIT_MESSAGE_CAPABILITIES = 1

        /** Its `connectivity_supported` field, a uint32z bitmask. */
        const val FIT_FIELD_CONNECTIVITY_SUPPORTED = 23
    }
}
