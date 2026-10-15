package tech.mmarca.openvitals.domain.medical

import java.net.URI
import java.text.Normalizer

/**
 * The records of one origin, bound for one Health Connect data source.
 *
 * [baseUri] is null for records whose file names no origin: the user names that
 * source, and [importBaseUri] turns the name into its base URI.
 */
data class FhirSourceGroup(
    val baseUri: String?,
    val suggestedName: String,
    val fhirVersion: String?,
    val entries: List<FhirEntry>,
)

/**
 * Splits a file's records by origin, so two origins never share a data source.
 *
 * In order: an origin the file's index gives (Apple's export), the base of an
 * absolute `fullUrl` (every OpenVitals export), `meta.source`, and otherwise one
 * group the user names.
 */
object FhirSourceGrouper {
    const val ImportScheme = "openvitals://import"

    fun group(file: FhirFile, fileName: String): List<FhirSourceGroup> {
        val keyed = file.entries.groupBy(::keyOf)
        val groups = keyed.map { (key, entries) ->
            FhirSourceGroup(
                baseUri = key.baseUri,
                suggestedName = key.baseUri?.let { suggestedName(it, entries) } ?: fileStem(fileName),
                fhirVersion = key.version,
                entries = entries,
            )
        }
        return nameVersionSplits(groups)
    }

    /** The base URI of a source the user named: stable for the same name, so a re-import finds it. */
    fun importBaseUri(displayName: String): String = "$ImportScheme/${slug(displayName)}"

    /**
     * The base of a `fullUrl` that ends in `<type>/<id>`, as FHIR defines it, without
     * a trailing slash. Null for a `urn:` URL or one that does not end that way.
     */
    fun baseOf(fullUrl: String, resourceType: String): String? {
        if (fullUrl.startsWith("urn:", ignoreCase = true)) return null
        val match = FullUrlShape.matchEntire(fullUrl) ?: return null
        val (base, type) = match.destructured
        return base.trimEnd('/').takeIf { type == resourceType && it.contains("://") }
    }

    /** The id at the end of a `fullUrl` of the form `<base>/<type>/<id>`, when the type matches. */
    fun idOf(fullUrl: String, resourceType: String): String? {
        if (fullUrl.startsWith("urn:", ignoreCase = true)) return null
        val match = FullUrlShape.matchEntire(fullUrl) ?: return null
        return match.groupValues[3].takeIf { match.groupValues[2] == resourceType }
    }

    fun sameBase(a: String, b: String): Boolean = a.trimEnd('/').equals(b.trimEnd('/'), ignoreCase = true)

    private data class Key(val baseUri: String?, val version: String?)

    private fun keyOf(entry: FhirEntry): Key {
        entry.origin?.let { return Key(it.baseUri.trimEnd('/'), it.fhirVersion) }
        val type = entry.type
        val fromFullUrl = if (entry.fullUrl != null && type != null) baseOf(entry.fullUrl, type) else null
        if (fromFullUrl != null) return Key(fromFullUrl, entry.exportedFhirVersion)
        val metaSource = entry.resource.obj("meta")?.string("source")?.takeIf { it.isNotBlank() }
        if (metaSource != null) return Key(metaSource.trimEnd('/'), null)
        return Key(null, null)
    }

    private fun suggestedName(baseUri: String, entries: List<FhirEntry>): String =
        entries.firstNotNullOfOrNull { it.origin?.displayName ?: it.exportedSourceName }
            ?: runCatching { URI(baseUri).host }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: baseUri

    /**
     * A data source has one FHIR version, so an origin in two versions is two sources. When
     * the second has the first's name, it gets the version. An OpenVitals export already
     * names each source apart.
     */
    private fun nameVersionSplits(groups: List<FhirSourceGroup>): List<FhirSourceGroup> {
        val seen = mutableSetOf<Pair<String, String>>()
        return groups.map { group ->
            val base = group.baseUri ?: return@map group
            val named = if (group.fhirVersion != null && (base to group.suggestedName) in seen) {
                group.copy(suggestedName = "${group.suggestedName} (FHIR ${group.fhirVersion})")
            } else {
                group
            }
            seen += base to named.suggestedName
            named
        }
    }

    private fun fileStem(fileName: String): String =
        fileName.substringAfterLast('/').substringBeforeLast('.').ifBlank { fileName }.ifBlank { "Imported records" }

    private fun slug(text: String): String {
        val plain = Normalizer.normalize(text, Normalizer.Form.NFD).replace(CombiningMarks, "")
        return plain.lowercase().replace(NonSlug, "-").trim('-').take(MaxSlug).trim('-').ifEmpty { "records" }
    }

    private val FullUrlShape = Regex("^(.*)/([A-Za-z]+)/([A-Za-z0-9\\-.]{1,64})(?:/_history/[^/]+)?$")
    private val CombiningMarks = Regex("\\p{Mn}+")
    private val NonSlug = Regex("[^a-z0-9]+")
    private const val MaxSlug = 48
}
