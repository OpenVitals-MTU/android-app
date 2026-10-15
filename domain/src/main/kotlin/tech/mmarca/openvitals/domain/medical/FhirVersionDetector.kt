package tech.mmarca.openvitals.domain.medical

/**
 * The FHIR version of a group: what its origin says, else R4B when a profile URL
 * hints at it, else R4. A matched data source's own version wins over this; the
 * import wizard applies that.
 */
object FhirVersionDetector {
    const val R4 = "4.0.1"
    const val R4B = "4.3.0"

    fun detect(group: FhirSourceGroup, bundleProfiles: List<String> = emptyList()): String {
        group.fhirVersion?.let { return it }
        val profiles = bundleProfiles + group.entries.flatMap { it.resource.obj("meta")?.strings("profile").orEmpty() }
        return if (profiles.any(::hintsR4B)) R4B else R4
    }

    private fun hintsR4B(url: String): Boolean =
        R4BHints.any { url.contains(it, ignoreCase = true) }

    private val R4BHints = listOf("/R4B/", "/4.3.0/", "/4.3/", "fhir.r4b")
}
