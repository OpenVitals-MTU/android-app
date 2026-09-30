package tech.mmarca.openvitals.domain.medical

import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/**
 * Writes records as a FHIR `collection` Bundle that keeps where each came from, so a
 * re-import restores the same sources and ids.
 *
 * - Each entry's `fullUrl` is its source's base URI followed by `<type>/<id>`, as FHIR
 *   defines it. Records from two sources can share an id without clashing.
 * - Each entry carries an OpenVitals extension with the source's name, the app that wrote
 *   it, and its FHIR version. An origin in R4 and R4B then comes back as two sources.
 * - The Bundle's `meta.tag` marks it as an OpenVitals export, with the app version.
 * - Resources are written exactly as Health Connect holds them.
 */
object FhirBundleWriter {

    /** A record whose source is missing from [sources] is written without the extension. */
    fun write(
        records: List<MedicalRecord>,
        sources: List<MedicalRecordSource>,
        appVersion: String,
        exportedAt: Instant,
    ): String {
        val byId = sources.associateBy { it.id }
        val bundle = buildJsonObject {
            put("resourceType", "Bundle")
            put(
                "meta",
                buildJsonObject {
                    put(
                        "tag",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("system", OpenVitalsFhir.TagSystem)
                                    put("code", OpenVitalsFhir.ExportTag)
                                    put("display", "OpenVitals $appVersion")
                                },
                            )
                        },
                    )
                },
            )
            put("type", "collection")
            put("timestamp", exportedAt.toString())
            put("entry", JsonArray(records.map { entry(it, byId[it.ref.dataSourceId]) }))
        }
        return bundle.toString()
    }

    /** [part] names a subset, such as a category, so two exports of one day keep apart. */
    fun fileName(date: LocalDate, part: String? = null): String =
        listOfNotNull("openvitals-medical-records", part?.lowercase()?.replace(NonFileName, "-"), date.toString()).joinToString("-") + ".json"

    private val NonFileName = Regex("[^a-z0-9.-]+")

    /** One Bundle entry. Phone-to-phone sync sends each record this way. */
    internal fun entry(record: MedicalRecord, source: MedicalRecordSource?): JsonObject = buildJsonObject {
        if (source != null) {
            put("fullUrl", "${source.fhirBaseUri.trimEnd('/')}/${record.ref.resourceType}/${record.ref.resourceId}")
            put(
                "extension",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("url", OpenVitalsFhir.SourceExtensionUrl)
                            put(
                                "extension",
                                buildJsonArray {
                                    add(subExtension(OpenVitalsFhir.SourceNameUrl, source.displayName))
                                    add(subExtension(OpenVitalsFhir.SourcePackageUrl, source.packageName))
                                    add(subExtension(OpenVitalsFhir.SourceFhirVersionUrl, source.fhirVersion))
                                },
                            )
                        },
                    )
                },
            )
        }
        put("resource", Json.parseToJsonElement(record.json))
    }

    private fun subExtension(url: String, value: String) = buildJsonObject {
        put("url", url)
        put("valueString", value)
    }
}
