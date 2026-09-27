package tech.mmarca.openvitals.features.cycle

import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.model.CycleJournalEntry

/**
 * The journal as a JSON file: a backup and a way to another phone without
 * Bluetooth. Health Connect records are not in it; Health Connect has its
 * own export.
 */
object CycleJournalExport {
    const val FormatVersion = 1
    const val MimeType = "application/json"
    const val FileName = "openvitals-cycle-journal.json"

    /** Years of daily entries stay far under this. */
    const val ImportMaxBytes = 4 * 1024 * 1024
}

/** What a backup file holds once parsed. */
data class CycleJournalImport(
    val entries: List<CycleJournalEntry>,
    val exclusions: List<Pair<LocalDate, CycleExclusionReason?>>,
    val profile: CycleTrackingProfile?,
)

private val json = Json { prettyPrint = true }

fun cycleJournalExportJson(
    entries: List<CycleJournalEntry>,
    exclusions: Map<LocalDate, CycleExclusionReason?>,
    profile: CycleTrackingProfile,
    exportedAt: Instant,
): String = json.encodeToString(
    JsonObject.serializer(),
    buildJsonObject {
        put("format", CycleJournalExport.FormatVersion)
        put("exportedAt", exportedAt.toEpochMilli())
        put("entries", buildJsonArray { entries.sortedBy { it.date }.forEach { add(CycleJournalJson.entry(it)) } })
        put(
            "exclusions",
            buildJsonArray { exclusions.entries.sortedBy { it.key }.forEach { add(CycleJournalJson.exclusion(it.key, it.value)) } },
        )
        put("profile", CycleJournalJson.profile(profile))
    },
)

/** Null when the text is not a journal export this version understands. */
fun parseCycleJournalExport(text: String): CycleJournalImport? {
    val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
    val format = root["format"]?.jsonPrimitive?.intOrNull ?: return null
    if (format > CycleJournalExport.FormatVersion) return null
    val entries = root["entries"]?.let { runCatching { it.jsonArray }.getOrNull() } ?: return null
    return CycleJournalImport(
        entries = entries.mapNotNull { element ->
            runCatching { element.jsonObject }.getOrNull()?.let(CycleJournalJson::entryOrNull)
        }.filter { it.hasObservations },
        exclusions = root["exclusions"]?.let { runCatching { it.jsonArray }.getOrNull() }
            ?.mapNotNull { element -> runCatching { element.jsonObject }.getOrNull()?.let(CycleJournalJson::exclusionOrNull) }
            .orEmpty(),
        profile = root["profile"]?.let { runCatching { it.jsonObject }.getOrNull() }?.let(CycleJournalJson::profileOrNull),
    )
}
