package tech.mmarca.openvitals.devices.garmin

/**
 * Garmin GFDI transport UUIDs, kept out of `BleUuids`: they are GATT-only,
 * never advertised. The advertised `GARMIN_MEMBER_SERVICE` stays shared.
 */
object GarminUuids {

    /** Garmin's GFDI V1 service. Not advertised; never put it in a scan filter. */
    const val GFDI_SERVICE_V1 = "6a4e2401-667b-11e3-949a-0800200c9a66"

    const val GFDI_SEND_V1 = "6a4e4c80-667b-11e3-949a-0800200c9a66"

    const val GFDI_RECEIVE_V1 = "6a4ecd28-667b-11e3-949a-0800200c9a66"

    /** Garmin's V2 multi-link service, what a vívoactive 5 exposes. GATT-only. */
    const val ML_SERVICE_V2 = "6a4e2800-667b-11e3-949a-0800200c9a66"

    /** V2 receive handles. Each pairs with a send handle at `+ [ML_SEND_HANDLE_OFFSET]`; the first pair present wins. */
    const val ML_FIRST_RECEIVE_HANDLE = 0x2810
    const val ML_LAST_RECEIVE_HANDLE = 0x2814
    const val ML_SEND_HANDLE_OFFSET = 0x10

    /** A Garmin 128-bit UUID from its 16-bit handle, lowercase to match `UUID.toString()`. */
    fun uuidForHandle(handle: Int): String =
        "6a4e${handle.toString(16).padStart(4, '0')}-667b-11e3-949a-0800200c9a66"
}
