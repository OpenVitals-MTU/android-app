package tech.mmarca.openvitals.features.cycle

import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.HormonalContraception
import tech.mmarca.openvitals.domain.cycle.HormonalContraceptionKind
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.domain.model.BbtDisturbance
import tech.mmarca.openvitals.domain.model.CervicalSensation
import tech.mmarca.openvitals.domain.model.CervixFirmness
import tech.mmarca.openvitals.domain.model.CervixOpening
import tech.mmarca.openvitals.domain.model.CervixPosition
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.HcgTestResult
import tech.mmarca.openvitals.domain.model.PillPlan

/**
 * The cycle journal as JSON objects, shared by the phone-to-phone sync and
 * the backup file so both read the other's output. Enums travel by their
 * stable ids or names; a value this build does not know is dropped.
 */
object CycleJournalJson {

    fun entry(entry: CycleJournalEntry): JsonObject = buildJsonObject {
        put("date", entry.date.toString())
        put("bleedingNone", entry.bleedingNone)
        entry.painLevel?.let { put("pain", it) }
        entry.moodLevel?.let { put("mood", it) }
        entry.energyLevel?.let { put("energy", it) }
        put("symptoms", buildJsonArray { entry.symptoms.map { it.id }.sorted().forEach { add(it) } })
        put("notes", entry.notes)
        entry.hcgTest?.let { put("hcg", it.name) }
        put("disturbances", buildJsonArray { entry.bbtDisturbances.map { it.name }.sorted().forEach { add(it) } })
        entry.cervicalSensation?.let { put("sensation", it.name) }
        entry.cervixOpening?.let { put("cervixOpening", it.name) }
        entry.cervixFirmness?.let { put("cervixFirmness", it.name) }
        entry.cervixPosition?.let { put("cervixPosition", it.name) }
        entry.desireLevel?.let { put("desire", it) }
        put("updatedAt", entry.updatedAt.toEpochMilli())
    }

    /** Null for an object this build cannot read; the caller skips it. */
    fun entryOrNull(json: JsonObject): CycleJournalEntry? = runCatching {
        CycleJournalEntry(
            date = LocalDate.parse(json.getValue("date").jsonPrimitive.content),
            bleedingNone = json["bleedingNone"]?.jsonPrimitive?.boolean ?: false,
            painLevel = json.intOrNull("pain"),
            moodLevel = json.intOrNull("mood"),
            energyLevel = json.intOrNull("energy"),
            symptoms = json.names("symptoms").mapNotNullTo(mutableSetOf()) { id -> CycleSymptom.entries.firstOrNull { it.id == id } },
            notes = json["notes"]?.jsonPrimitive?.content.orEmpty(),
            hcgTest = json.nameOrNull("hcg")?.let { name -> HcgTestResult.entries.firstOrNull { it.name == name } },
            bbtDisturbances = json.names("disturbances").mapNotNullTo(mutableSetOf()) { name -> BbtDisturbance.entries.firstOrNull { it.name == name } },
            cervicalSensation = json.nameOrNull("sensation")?.let { name -> CervicalSensation.entries.firstOrNull { it.name == name } },
            cervixOpening = json.nameOrNull("cervixOpening")?.let { name -> CervixOpening.entries.firstOrNull { it.name == name } },
            cervixFirmness = json.nameOrNull("cervixFirmness")?.let { name -> CervixFirmness.entries.firstOrNull { it.name == name } },
            cervixPosition = json.nameOrNull("cervixPosition")?.let { name -> CervixPosition.entries.firstOrNull { it.name == name } },
            desireLevel = json.intOrNull("desire")?.takeIf { it in CycleJournalEntry.TRACKING_SCALE },
            updatedAt = json["updatedAt"]?.jsonPrimitive?.long?.let(Instant::ofEpochMilli) ?: Instant.EPOCH,
        )
    }.getOrNull()

    fun exclusion(start: LocalDate, reason: CycleExclusionReason?): JsonObject = buildJsonObject {
        put("start", start.toString())
        reason?.let { put("reason", it.name) }
    }

    fun exclusionOrNull(json: JsonObject): Pair<LocalDate, CycleExclusionReason?>? = runCatching {
        LocalDate.parse(json.getValue("start").jsonPrimitive.content) to CycleExclusionReason.fromName(json.nameOrNull("reason"))
    }.getOrNull()

    fun profile(profile: CycleTrackingProfile): JsonObject = buildJsonObject {
        put("contexts", buildJsonArray { profile.contexts.map { it.id }.sorted().forEach { add(it) } })
        profile.ageBand?.let { put("ageBand", it.id) }
        val hormonal = profile.hormonalContraception
        if (hormonal.isDeclared) {
            put("hormonalContraception", hormonal.kind.id)
            hormonal.stoppedOn?.let { put("hormonalContraceptionStoppedOn", it.toString()) }
        }
    }

    fun profileOrNull(json: JsonObject): CycleTrackingProfile? = runCatching {
        CycleTrackingProfile(
            contexts = json.names("contexts").mapNotNullTo(mutableSetOf()) { TrackingContext.fromId(it) },
            ageBand = AgeBand.fromId(json.nameOrNull("ageBand")),
            hormonalContraception = HormonalContraception(
                kind = HormonalContraceptionKind.fromId(json.nameOrNull("hormonalContraception"))
                    ?: HormonalContraceptionKind.NONE,
                stoppedOn = json.nameOrNull("hormonalContraceptionStoppedOn")
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            ),
        )
    }.getOrNull()

    /** The scheme only. The reminder switch and time stay on their phone, like the cycle reminders. */
    fun pillPlan(plan: PillPlan): JsonObject = buildJsonObject {
        put("enabled", plan.enabled)
        put("activeDays", plan.activeDays)
        put("pauseDays", plan.pauseDays)
        plan.packStart?.let { put("packStart", it.toString()) }
        put("updatedAt", plan.updatedAt.toEpochMilli())
    }

    /** A plan with default reminder fields; the caller keeps its own. */
    fun pillPlanOrNull(json: JsonObject): PillPlan? = runCatching {
        PillPlan(
            enabled = json["enabled"]?.jsonPrimitive?.boolean ?: false,
            activeDays = json.intOrNull("activeDays") ?: PillPlan().activeDays,
            pauseDays = json.intOrNull("pauseDays") ?: PillPlan().pauseDays,
            packStart = json.nameOrNull("packStart")?.let(LocalDate::parse),
            updatedAt = json["updatedAt"]?.jsonPrimitive?.long?.let(Instant::ofEpochMilli) ?: Instant.EPOCH,
        ).normalized()
    }.getOrNull()

    fun pillIntake(date: LocalDate): JsonObject = buildJsonObject { put("date", date.toString()) }

    fun pillIntakeOrNull(json: JsonObject): LocalDate? = runCatching {
        LocalDate.parse(json.getValue("date").jsonPrimitive.content)
    }.getOrNull()

    private fun JsonObject.intOrNull(key: String): Int? = this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.int

    private fun JsonObject.nameOrNull(key: String): String? = this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content

    private fun JsonObject.names(key: String): List<String> =
        this[key]?.takeIf { it !is JsonNull }?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
}
