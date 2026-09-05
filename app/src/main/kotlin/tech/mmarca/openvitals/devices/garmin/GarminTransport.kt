package tech.mmarca.openvitals.devices.garmin

/**
 * Which GFDI transport a device speaks, found by enumerating its GATT
 * services after bonding. Gadgetbridge decides this at runtime too.
 */
enum class GarminTransportVariant {
    /** Single send/receive characteristic pair under service `6A4E2401-…`. */
    V1,

    /** Multi-link service `6A4E2800-…`. Newer watches; several channels over one connection. */
    V2,

    /** Enumerated, but neither variant found: not a Garmin, or an unseen transport. */
    UNKNOWN,

    /** Could not connect or enumerate at all — says nothing about the device. */
    UNREACHABLE,
    ;

    /** The lowercase rendering the log format (and the Flutter build) uses. */
    val wireName: String get() = name.lowercase()
}

/** One GATT service and the characteristics under it, as read off the device. */
data class GarminGattService(
    val uuid: String,
    /** Characteristic UUIDs with their property flags rendered for the log. */
    val characteristics: Map<String, List<String>>,
)

/** What a probe found, kept whole: the raw map explains an UNKNOWN verdict. */
data class GarminGattReport(
    val address: String,
    val variant: GarminTransportVariant,
    val services: List<GarminGattService>,
) {

    /** True when this app has a transport that can talk to the device. */
    val isSupported: Boolean
        get() = variant == GarminTransportVariant.V1 ||
            variant == GarminTransportVariant.V2

    /** A multi-line dump for the log — the whole point of the probe. */
    fun describe(): String {
        val buffer = StringBuilder()
        buffer.appendLine(
            "[GARMIN-GATT] $address variant=${variant.wireName} " +
                "services=${services.size}",
        )
        for (service in services) {
            buffer.appendLine("[GARMIN-GATT]   service ${service.uuid}")
            for ((uuid, properties) in service.characteristics) {
                buffer.appendLine(
                    "[GARMIN-GATT]     char $uuid [${properties.joinToString(",")}]",
                )
            }
        }
        return buffer.toString().trimEnd()
    }
}
