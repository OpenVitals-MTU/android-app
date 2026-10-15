package tech.mmarca.openvitals.domain.medical

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Where a record came from, when the file says so outside the FHIR itself, as Apple's export index does. */
data class FhirOrigin(
    val baseUri: String,
    val displayName: String,
    val fhirVersion: String? = null,
)

/**
 * One resource from a file, before any repair. [fullUrl] is the Bundle entry's, when
 * there was one. [exportedSourceName], [exportedSourcePackage] and [exportedFhirVersion]
 * come from an OpenVitals export's entry extension. [snippet] is the text a phase 2 parser read the
 * resource from, shown beside it for review. FHIR files have none.
 */
data class FhirEntry(
    val resource: JsonObject,
    val fullUrl: String? = null,
    val exportedSourceName: String? = null,
    val exportedSourcePackage: String? = null,
    val exportedFhirVersion: String? = null,
    val origin: FhirOrigin? = null,
    val snippet: String? = null,
) {
    val type: String? get() = resource.resourceType
}

/** What a FHIR file holds. [bundleProfiles] are the Bundle's own `meta.profile` URLs. */
data class FhirFile(
    val entries: List<FhirEntry>,
    val isOpenVitalsExport: Boolean = false,
    val bundleProfiles: List<String> = emptyList(),
)

sealed interface FhirParseResult {
    data class Parsed(val file: FhirFile) : FhirParseResult

    /** Not FHIR JSON: not JSON at all, or JSON with no `resourceType`. */
    data object NotFhir : FhirParseResult
}

/** The markers an OpenVitals export writes, so a re-import can restore its sources. */
object OpenVitalsFhir {
    const val SourceExtensionUrl = "https://openvitals.health/fhir/StructureDefinition/medical-data-source"
    const val SourceNameUrl = "displayName"
    const val SourcePackageUrl = "packageName"
    const val SourceFhirVersionUrl = "fhirVersion"
    const val TagSystem = "https://openvitals.health/fhir/CodeSystem/tags"
    const val ExportTag = "medical-records-export"
}

/**
 * Reads a FHIR file: one resource, a Bundle of any type, or NDJSON with one resource
 * per line. A Bundle's entries are unwrapped. Nothing is repaired or checked here.
 */
object FhirFileParser {
    /**
     * The largest file the wizard reads. The parsed tree takes about 14 bytes of heap per
     * byte of file (measured on 2026-09-29), so 8 MB costs about 115 MB. That is about
     * 11,000 lab results, far more than a person's records.
     */
    const val MaxFileBytes = 8 * 1024 * 1024

    private val json = Json { isLenient = false }

    fun parse(text: String): FhirParseResult {
        val whole = runCatching { json.parseToJsonElement(text) }.getOrNull()
        if (whole != null) {
            val root = whole as? JsonObject ?: return FhirParseResult.NotFhir
            return fromRoot(root)?.let(FhirParseResult::Parsed) ?: FhirParseResult.NotFhir
        }
        return parseNdjson(text.lineSequence())
    }

    /** NDJSON, read line by line. Every non-blank line must be one resource. */
    fun parseNdjson(lines: Sequence<String>): FhirParseResult {
        val entries = mutableListOf<FhirEntry>()
        for (line in lines) {
            if (line.isBlank()) continue
            val resource = runCatching { json.parseToJsonElement(line) as? JsonObject }.getOrNull()
                ?: return FhirParseResult.NotFhir
            if (resource.resourceType == null) return FhirParseResult.NotFhir
            entries += FhirEntry(resource)
        }
        return if (entries.isEmpty()) FhirParseResult.NotFhir else FhirParseResult.Parsed(FhirFile(entries))
    }

    private fun fromRoot(root: JsonObject): FhirFile? {
        val type = root.resourceType ?: return null
        if (type != "Bundle") return FhirFile(listOf(FhirEntry(root)))
        val entries = root.objects("entry").mapNotNull { entry ->
            val resource = entry.obj("resource")?.takeIf { it.resourceType != null } ?: return@mapNotNull null
            val source = entry.objects("extension").firstOrNull { it.string("url") == OpenVitalsFhir.SourceExtensionUrl }
            FhirEntry(
                resource = resource,
                fullUrl = entry.string("fullUrl"),
                exportedSourceName = source?.subExtension(OpenVitalsFhir.SourceNameUrl),
                exportedSourcePackage = source?.subExtension(OpenVitalsFhir.SourcePackageUrl),
                // Only a version Health Connect takes: any other would fail when the source is made.
                exportedFhirVersion = source?.subExtension(OpenVitalsFhir.SourceFhirVersionUrl)
                    ?.takeIf { it == FhirVersionDetector.R4 || it == FhirVersionDetector.R4B },
            )
        }
        val meta = root.obj("meta")
        val isExport = meta?.objects("tag").orEmpty().any {
            it.string("system") == OpenVitalsFhir.TagSystem && it.string("code") == OpenVitalsFhir.ExportTag
        }
        return FhirFile(entries, isOpenVitalsExport = isExport, bundleProfiles = meta?.strings("profile").orEmpty())
    }

    private fun JsonObject.subExtension(url: String): String? =
        objects("extension").firstOrNull { it.string("url") == url }?.string("valueString")
}
