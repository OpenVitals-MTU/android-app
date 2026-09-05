package tech.mmarca.openvitals.devices.garmin

import android.content.Context

/**
 * [GarminTransportProbe] over [GarminGattClient]: connect, enumerate,
 * classify, hang up. The full service map is logged, since it is the only
 * evidence for an UNKNOWN verdict.
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
        // Line by line, so logcat drops nothing.
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
        /** V2 first, as `GarminSupport.initializeDevice`: a watch offering both expects multi-link. */
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
