package tech.mmarca.openvitals.devices.garmin

/**
 * Garmin GFDI transport UUIDs — the GATT service/characteristic identifiers
 * the FIT-file sync connects over. Split out of the shared `BleUuids` so the
 * generic BLE stack carries no Garmin protocol knowledge: these are
 * discoverable only AFTER connecting (never advertised), so nothing outside
 * the Garmin transport has any use for them.
 *
 * The one Garmin UUID that stays in `BleUuids` is `GARMIN_MEMBER_SERVICE`
 * (`0xFE1F`): it is what a watch puts in its ADVERTISEMENT, so the shared
 * scanner needs it to spot a watch in the first place.
 */
object GarminUuids {

    /**
     * Garmin's GFDI V1 service — the transport older watches pull FIT files
     * over.
     *
     * **Not advertised.** A GATT service discoverable only after connecting,
     * so it must never go in a scan filter. From Gadgetbridge's
     * `CommunicatorV1.UUID_SERVICE_GARMIN_GFDI_V1`.
     */
    const val GFDI_SERVICE_V1 = "6a4e2401-667b-11e3-949a-0800200c9a66"

    const val GFDI_SEND_V1 = "6a4e4c80-667b-11e3-949a-0800200c9a66"

    const val GFDI_RECEIVE_V1 = "6a4ecd28-667b-11e3-949a-0800200c9a66"

    /**
     * Garmin's V2 multi-link service — what a vívoactive 5 exposes (confirmed
     * by the on-device GATT probe). Also GATT-only, never advertised.
     */
    const val ML_SERVICE_V2 = "6a4e2800-667b-11e3-949a-0800200c9a66"

    /**
     * V2 receive (notify) characteristic handles. Each is paired with a send
     * characteristic at `handle + [ML_SEND_HANDLE_OFFSET]`, and the first pair
     * that exists on the device is the one to use
     * (`CommunicatorV2.initializeDevice`).
     */
    const val ML_FIRST_RECEIVE_HANDLE = 0x2810
    const val ML_LAST_RECEIVE_HANDLE = 0x2814
    const val ML_SEND_HANDLE_OFFSET = 0x10

    /**
     * Builds a Garmin 128-bit UUID from its 16-bit handle, splicing it into
     * Gadgetbridge's `BASE_UUID` (`6A4E%04X-667B-11E3-949A-0800200C9A66`).
     * Lowercase, to match `UUID.toString()`.
     */
    fun uuidForHandle(handle: Int): String =
        "6a4e${handle.toString(16).padStart(4, '0')}-667b-11e3-949a-0800200c9a66"
}
