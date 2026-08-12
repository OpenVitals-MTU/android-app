package tech.mmarca.openvitals.devices.garmin

/**
 * Which GFDI transport a Garmin device speaks, discovered by enumerating its
 * GATT services after bonding.
 *
 * Gadgetbridge picks between these at RUNTIME, not from a model table:
 * `GarminSupport.initializeDevice` tries V2's characteristics first and falls
 * back to V1 if they are absent. So this cannot be inferred from the device
 * name — it has to be asked.
 */
enum class GarminTransportVariant {
    /** Single send/receive characteristic pair under service `6A4E2401-…`. */
    V1,

    /**
     * Multi-link service `6A4E2800-…` with `0x2810`/`0x2820`-style pairs.
     * Newer watches; carries several logical channels over one connection.
     */
    V2,

    /**
     * Connected and enumerated, but neither variant's characteristics were
     * found. Either not a Garmin device, or a transport this app has never
     * seen.
     */
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
    /**
     * Characteristic UUIDs, each with its property flags rendered for the log
     * (`read`, `write`, `writeNoRsp`, `notify`, `indicate`).
     */
    val characteristics: Map<String, List<String>>,
)

/**
 * What a probe found. Kept whole rather than reduced to the verdict: when the
 * verdict is [GarminTransportVariant.UNKNOWN] the raw map is the only thing
 * that explains why, and that is exactly the case worth reporting.
 */
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
