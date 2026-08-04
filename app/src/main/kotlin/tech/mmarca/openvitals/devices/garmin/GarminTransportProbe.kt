package tech.mmarca.openvitals.devices.garmin

/**
 * Asks a bonded Garmin device which GFDI transport it speaks.
 *
 * A **port** in the hexagonal sense: the domain owns the question, the
 * radio-touching layer owns the answer ([GarminGattProbe]).
 *
 * Must run AFTER bonding. Garmin's GFDI characteristics sit behind an
 * encrypted link, so enumerating an unbonded device either omits them or
 * fails outright — which would look identical to
 * [GarminTransportVariant.UNKNOWN] and send the reader hunting for a protocol
 * bug that isn't there.
 */
interface GarminTransportProbe {

    /**
     * Connects to [address], enumerates its GATT services, classifies the
     * transport and disconnects.
     *
     * Never throws: an unreachable device comes back as
     * [GarminTransportVariant.UNREACHABLE] with no services.
     */
    suspend fun probe(address: String): GarminGattReport
}
