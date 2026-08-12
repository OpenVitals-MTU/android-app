package tech.mmarca.openvitals.devices.garmin

/**
 * Recognises which Garmin device a discovered advertisement is, from its
 * advertised Bluetooth NAME, so the classifier can decide: a watch family
 * (vívoactive, fēnix, …) → a GFDI file-sync watch; an Edge → a bike computer;
 * anything else → a plain live BLE sensor.
 *
 * Ported in spirit — not line for line — from Gadgetbridge's per-model
 * coordinators under `devices/garmin/watches/`, `.../bike/` (AGPLv3, same
 * licence as this app). Gadgetbridge needs ~100 EXACT-match patterns because
 * each one selects a coordinator class carrying that model's quirks. This app
 * needs only the product FAMILY — durable against a `vívoactive 7` that does
 * not exist yet, which is recognised here and would need a new Gadgetbridge
 * class.
 *
 * The NAME is authoritative for the kind. Garmin's member service UUID
 * `0xFE1F` (`BleUuids.GARMIN_MEMBER_SERVICE`) is used only to SURFACE a device
 * in the scan (`GarminScanClassifier` /
 * `BleDiscoveredDevice.advertisesSyncService`), never to decide
 * watch-vs-sensor: a device advertising `0xFE1F` but not matching a known
 * Garmin family is treated as a plain sensor, not swept up as a watch.
 * Deliberately absent from every family list: `HRM*` chest straps — they
 * expose the standard Heart Rate GATT service and belong to the live-sensor
 * path.
 */
object GarminDeviceNames {

    /**
     * Watch product families. The accented forms are what the devices actually
     * advertise ("vívoactive", "fēnix"); the unaccented spellings appear on
     * some firmware and in some locales, so both are matched.
     */
    private val garminWatchFamilies = listOf(
        Regex("^v[íi]voactive\\b", RegexOption.IGNORE_CASE),
        Regex("^v[íi]vomove\\b", RegexOption.IGNORE_CASE),
        Regex("^v[íi]vosmart\\b", RegexOption.IGNORE_CASE),
        Regex("^v[íi]vosport\\b", RegexOption.IGNORE_CASE),
        Regex("^f[ēe]nix\\b", RegexOption.IGNORE_CASE),
        Regex("^forerunner\\b", RegexOption.IGNORE_CASE),
        Regex("^instinct\\b", RegexOption.IGNORE_CASE),
        Regex("^venu\\b", RegexOption.IGNORE_CASE),
        Regex("^epix\\b", RegexOption.IGNORE_CASE),
        Regex("^enduro\\b", RegexOption.IGNORE_CASE),
        Regex("^descent\\b", RegexOption.IGNORE_CASE),
        Regex("^tactix\\b", RegexOption.IGNORE_CASE),
        Regex("^quatix\\b", RegexOption.IGNORE_CASE),
        Regex("^lily\\b", RegexOption.IGNORE_CASE),
        Regex("^swim \\d", RegexOption.IGNORE_CASE),
    )

    /**
     * Bike-computer product families. Not watches: they speak the same GFDI
     * protocol and carry the same activity FIT files (so they onboard and sync
     * identically), but they classify as `BleDeviceKind.BIKE_COMPUTER` so the
     * UI can present them as cycling devices and offer them the live-sensor
     * role. The single `^edge\b` pattern already covers "Edge Explore",
     * "Edge MTB", "Edge 1040" — the `\b` sits right after "edge".
     */
    private val garminBikeComputerFamilies = listOf(
        Regex("^edge\\b", RegexOption.IGNORE_CASE),
    )

    /**
     * Some models advertise with a `Garmin ` prefix (Gadgetbridge carries
     * `^(Garmin )?Forerunner 265[sS]$` for exactly this), so it is stripped
     * before matching rather than doubling every pattern above.
     */
    private val garminPrefix = Regex("^garmin\\s+", RegexOption.IGNORE_CASE)

    /**
     * True when [name] is a Garmin smartwatch — onboard as a
     * `BleDeviceKind.WATCH`. Disjoint from the bike-computer families, so a
     * device is never both.
     *
     * A null or blank name is not a match: an unnamed advertisement carries no
     * evidence, so it is left to fall through to a plain sensor.
     */
    fun isGarminWatchName(name: String?): Boolean {
        val trimmed = strippedGarminName(name) ?: return false
        return garminWatchFamilies.any { it.containsMatchIn(trimmed) }
    }

    /**
     * True when [name] is a Garmin Edge bike computer — onboard as a
     * `BleDeviceKind.BIKE_COMPUTER`. Disjoint from the watch families.
     */
    fun isGarminBikeComputerName(name: String?): Boolean {
        val trimmed = strippedGarminName(name) ?: return false
        return garminBikeComputerFamilies.any { it.containsMatchIn(trimmed) }
    }

    /**
     * True when [name] is any Garmin GFDI file-sync device — a watch OR a bike
     * computer. The union of the two family checks.
     */
    fun isGarminSyncDeviceName(name: String?): Boolean =
        isGarminWatchName(name) || isGarminBikeComputerName(name)

    /**
     * The device name with a leading `Garmin ` prefix stripped, or null when
     * it is null/blank (an unnamed advertisement carries no evidence either
     * way).
     */
    private fun strippedGarminName(name: String?): String? {
        if (name == null) return null
        val trimmed = name.trim().replaceFirst(garminPrefix, "")
        return trimmed.ifEmpty { null }
    }
}
