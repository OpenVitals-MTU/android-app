package tech.mmarca.openvitals.devices.garmin

import android.content.Context

/**
 * [GarminTransportProbe] over [GarminGattClient].
 *
 * The first piece of the GFDI transport: before anything can be sent to a
 * watch, this establishes which characteristics to send it on. It connects,
 * enumerates, classifies and hangs up — no GFDI traffic, no writes. The
 * radio work lives in [GarminGattClient.enumerateServices]; this file only
 * classifies what came back, which keeps `android.bluetooth` in exactly one
 * file.
 *
 * The full service map is logged, not just the verdict. A watch that comes
 * back [GarminTransportVariant.UNKNOWN] is the case that needs diagnosing,
 * and the map is the only evidence that explains it.
 */
class GarminGattProbe(private val context: Context) : GarminTransportProbe {

    override suspend fun probe(address: String): GarminGattReport {
        val client = GarminGattClient(context, address)
        val services = try {
            client.enumerateServices()
        } catch (error: Exception) {
            GarminLog.log("[GARMIN-GATT] $address probe failed: ${error.message}")
            return unreachable(address)
        } finally {
            client.close()
        }
        val report = GarminGattReport(
            address = address,
            variant = classify(services),
            services = services,
        )
        // Log line by line so nothing is dropped or reordered in logcat.
        for (line in report.describe().split('\n')) {
            GarminLog.log(line)
        }
        return report
    }

    private fun unreachable(address: String) = GarminGattReport(
        address = address,
        variant = GarminTransportVariant.UNREACHABLE,
        services = emptyList(),
    )

    companion object {
        /**
         * V2 is checked FIRST, matching `GarminSupport.initializeDevice`: a
         * watch that offers both must be driven over the multi-link
         * transport, because that is what its firmware expects to carry the
         * sync.
         */
        fun classify(services: List<GarminGattService>): GarminTransportVariant {
            val characteristics = buildSet {
                for (service in services) addAll(service.characteristics.keys)
            }

            for (
                handle in
                GarminUuids.ML_FIRST_RECEIVE_HANDLE..GarminUuids.ML_LAST_RECEIVE_HANDLE
            ) {
                val receive = GarminUuids.uuidForHandle(handle)
                val send = GarminUuids.uuidForHandle(handle + GarminUuids.ML_SEND_HANDLE_OFFSET)
                if (receive in characteristics && send in characteristics) {
                    return GarminTransportVariant.V2
                }
            }

            if (GarminUuids.GFDI_SEND_V1 in characteristics &&
                GarminUuids.GFDI_RECEIVE_V1 in characteristics
            ) {
                return GarminTransportVariant.V1
            }

            return GarminTransportVariant.UNKNOWN
        }
    }
}
