package tech.mmarca.openvitals.devices.wearos

/** One entry of the adapter's bonded list, stripped to what matching needs. */
data class BondedWatch(val address: String, val name: String?)

/**
 * Finds the bonded Classic entry for a registered Wear OS watch.
 *
 * The registry holds the address from the BLE scan. Wear OS watches advertise
 * with a private address, so the bond usually sits under another one. Order:
 * exact address, then the same name, then the only bonded smartwatch.
 */
object WearOsBondMatcher {

    fun pick(
        bonded: List<BondedWatch>,
        targetAddress: String?,
        targetName: String?,
    ): BondedWatch? {
        if (!targetAddress.isNullOrBlank()) {
            bonded.firstOrNull { it.address.equals(targetAddress, ignoreCase = true) }
                ?.let { return it }
        }
        val wanted = normalize(targetName)
        if (wanted != null) {
            bonded.firstOrNull { normalize(it.name) == wanted }?.let { return it }
        }
        // Several smartwatches and no match: pinging a guess could report the wrong watch.
        return bonded.filter { WearOsDeviceNames.isSmartwatchName(it.name) }.singleOrNull()
    }

    /** Galaxy Watches advertise "<name> LE" over BLE and "<name>" as the Classic bond. */
    private fun normalize(name: String?): String? =
        name?.trim()
            ?.replace(TrailingLe, "")
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }

    private val TrailingLe = Regex("\\s+LE$", RegexOption.IGNORE_CASE)
}
