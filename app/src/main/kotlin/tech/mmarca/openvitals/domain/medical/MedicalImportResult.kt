package tech.mmarca.openvitals.domain.medical

import java.time.Instant
import tech.mmarca.openvitals.domain.model.MedicalRecordRef

data class MedicalImportProgress(val done: Int, val total: Int)

/** A record Health Connect refused. [reason] is the platform's own, which names the rule and the field. */
data class MedicalWriteRejection(val type: String, val id: String, val reason: String, val json: String)

/** [updated] records existed before the write; [written] are new. Upsert says neither, so each batch is read first. */
data class MedicalImportGroupResult(
    val plan: MedicalImportGroupPlan,
    val sourceName: String,
    val written: Int,
    val updated: Int,
    val rejected: List<MedicalWriteRejection>,
    /** The records written or updated, so a kept document can link to them. */
    val refs: List<MedicalRecordRef> = emptyList(),
)

/** [stoppedBy] names what ended the import early, such as a revoked permission. Groups after it did not run. */
data class MedicalImportResult(
    val groups: List<MedicalImportGroupResult>,
    val excluded: List<MedicalImportGroupPlan>,
    val stoppedBy: String? = null,
    /** Records the source left out before analysis, such as FHIR DSTU2 in an Apple export. */
    val sourceSkips: List<MedicalSourceSkip> = emptyList(),
) {
    val written: Int get() = groups.sumOf { it.written }
    val updated: Int get() = groups.sumOf { it.updated }
    val refs: List<MedicalRecordRef> get() = groups.flatMap { it.refs }

    /** Types Health Connect does not store, and groups left out because another app holds them. */
    val skipped: Int get() = (groups.map { it.plan } + excluded).sumOf { it.group.skippedTypes.values.sum() } +
        excluded.sumOf { it.group.ready.size } + sourceSkips.size

    /** Held back by pre-flight, or refused by Health Connect. */
    val rejected: Int get() = groups.sumOf { it.plan.group.rejected.size + it.rejected.size }

    /** Every reason a record was not written, with how many. Pre-flight reasons first. */
    val rejectionReasons: List<Pair<MedicalRejectionReason, Int>> get() {
        val preflight = groups.flatMap { g -> g.plan.group.rejected.map { MedicalRejectionReason.Preflight(it.problem.reason) } }
        val platform = groups.flatMap { g -> g.rejected.map { MedicalRejectionReason.Platform(it.reason) } }
        return (preflight + platform).groupingBy { it }.eachCount().toList()
    }
}

/** Why a record was not written: a pre-flight check, which the screen words, or Health Connect's own reason. */
sealed interface MedicalRejectionReason {
    data class Preflight(val reason: FhirRejection) : MedicalRejectionReason
    data class Platform(val text: String) : MedicalRejectionReason
}

/**
 * The import report: every group, every repair, every rejected record with its content,
 * so a failed import can be understood. It holds medical data; the result step says to
 * review it before sharing. English, like the other import reports: it is for support.
 */
object MedicalImportReport {
    fun text(fileName: String?, result: MedicalImportResult, finishedAt: Instant): String = buildString {
        appendLine("OpenVitals medical records import")
        appendLine("File: ${fileName ?: "unknown"}")
        appendLine("Finished: $finishedAt")
        appendLine("Written ${result.written}, updated ${result.updated}, skipped ${result.skipped}, rejected ${result.rejected}")
        result.stoppedBy?.let { appendLine("Stopped early: $it") }
        result.sourceSkips.forEach { appendLine("Left out before import: ${it.reason}: ${it.detail}") }
        result.groups.forEach { group ->
            appendLine()
            appendLine("Source: ${group.sourceName} (${group.plan.target.baseUri()}), FHIR ${group.plan.fhirVersion}")
            appendLine("  Written ${group.written}, updated ${group.updated}, refused ${group.rejected.size}")
            appendGroupDetail(group.plan)
            group.rejected.forEach { appendLine("  Refused by Health Connect: ${it.type}/${it.id}: ${it.reason}"); appendLine("    ${it.json}") }
        }
        result.excluded.forEach { plan ->
            appendLine()
            appendLine("Left out: ${plan.targetName} (${plan.group.baseUri}), ${plan.group.ready.size} records")
            plan.otherApp?.let { appendLine("  Already in Health Connect from ${it.packageName} as \"${it.displayName}\"") }
        }
    }

    private fun StringBuilder.appendGroupDetail(plan: MedicalImportGroupPlan) {
        plan.group.skippedTypes.forEach { (type, count) -> appendLine("  Skipped, not stored by Health Connect: $type x$count") }
        plan.group.rejected.forEach {
            appendLine("  Held back: ${it.resource.type}/${it.resource.id}: ${it.problem.reason}${it.problem.field?.let { f -> " ($f)" }.orEmpty()}")
            appendLine("    ${it.resource.json}")
        }
        plan.group.notes.forEach { note ->
            appendLine(
                "  " + when (note) {
                    is FhirIdNote.IdAssigned -> "Id for ${note.type}: ${note.previousId ?: "none"} -> ${note.id}"
                    is FhirIdNote.ContainedLifted -> "Contained ${note.type} lifted from ${note.parentType}/${note.parentId} as ${note.id}"
                    is FhirIdNote.ContainedDropped -> "Contained ${note.type} dropped from ${note.parentType}/${note.parentId}"
                    is FhirIdNote.Duplicate -> "Duplicate ${note.type}/${note.id}: the later one was kept"
                },
            )
        }
    }

    private fun MedicalImportTarget.baseUri(): String = when (this) {
        is MedicalImportTarget.Existing -> source.fhirBaseUri
        is MedicalImportTarget.New -> baseUri
    }
}

/** The platform's reason without the exception class names it arrives wrapped in. */
internal fun Throwable.platformReason(): String =
    (message ?: this::class.simpleName.orEmpty()).substringAfterLast("Exception: ").trim().ifEmpty { this::class.simpleName.orEmpty() }
