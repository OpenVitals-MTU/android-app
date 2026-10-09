package tech.mmarca.openvitals.devices.wearos

/** One entry of the adapter's bonded list, stripped to what matching needs. */
data class BondedWatch(val address: String, val name: String?)

/**
 * Finds the bonded Classic entry for a registered Wear OS watch.
 *
 * The registry holds the address from the BLE scan. Wear OS watches advertise
 * with a private address, so the bond usually sits under another one. Order:
 * the Classic address a hello was once answered from, then the exact
 * registered address, then the same name. No guess beyond that: pinging
 * another bonded watch would report the wrong one as paired.
 */
object WearOsBondMatcher {

    fun pick(
        bonded: List<BondedWatch>,
        targetAddress: String?,
        targetName: String?,
        /** The Classic address a hello was once answered from; the surest match. */
        linkAddress: String? = null,
    ): BondedWatch? {
        if (!linkAddress.isNullOrBlank()) {
            bonded.firstOrNull { it.address.equals(linkAddress, ignoreCase = true) }
                ?.let { return it }
        }
        if (!targetAddress.isNullOrBlank()) {
            bonded.firstOrNull { it.address.equals(targetAddress, ignoreCase = true) }
                ?.let { return it }
        }
        val wanted = normalize(targetName)
        if (wanted == null) return null
        return bonded.firstOrNull { normalize(it.name) == wanted }
    }

    /** Galaxy Watches advertise "<name> LE" over BLE and "<name>" as the Classic bond. */
    private fun normalize(name: String?): String? =
        name?.trim()
            ?.replace(TrailingLe, "")
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }

    private val TrailingLe = Regex("\\s+LE$", RegexOption.IGNORE_CASE)
}
