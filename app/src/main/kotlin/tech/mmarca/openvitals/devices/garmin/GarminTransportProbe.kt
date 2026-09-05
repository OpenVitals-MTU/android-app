package tech.mmarca.openvitals.devices.garmin

/**
 * Asks a bonded Garmin device which GFDI transport it speaks. A port. Must
 * run after bonding: the characteristics sit behind an encrypted link.
 */
interface GarminTransportProbe {

    /** Connects, enumerates, classifies, disconnects. Never throws: unreachable is UNREACHABLE. */
    suspend fun probe(address: String): GarminGattReport
}
