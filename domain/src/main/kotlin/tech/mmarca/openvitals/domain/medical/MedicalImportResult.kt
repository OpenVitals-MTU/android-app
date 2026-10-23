package tech.mmarca.openvitals.domain.medical

import java.time.Instant
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor
import tech.mmarca.openvitals.domain.model.MedicalRecordRef

data class MedicalImportProgress(val done: Int, val total: Int)

/** A record Health Connect refused. [reason] is the platform's own, masked: it names the rule and the field, and can quote a value. */
data class MedicalWriteRejection(val type: String, val id: String, val reason: String)

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
 * The import report: for every source, what was written, held back or refused, and why, so a
 * failed import can be understood and the report shared. Structure only: counts, record types
 * and reasons, never a record's content or id, a file's, source's or app's name, or an address.
 * English, like the other import reports: it is for support.
 */
object MedicalImportReport {
    fun text(result: MedicalImportResult, finishedAt: Instant): String = buildString {
        appendLine("OpenVitals medical records import")
        appendLine("Finished: $finishedAt")
        appendLine("Written ${result.written}, updated ${result.updated}, skipped ${result.skipped}, rejected ${result.rejected}")
        result.stoppedBy?.let { appendLine("Stopped early: $it") }
        appendSourceSkips(result.sourceSkips)
        result.groups.forEachIndexed { index, group ->
            appendLine()
            appendLine("Source ${index + 1}: ${group.plan.target.kind()}, FHIR ${group.plan.fhirVersion}")
            appendLine("  Written ${group.written}, updated ${group.updated}, refused ${group.rejected.size}")
            appendGroupDetail(group.plan)
            group.rejected.groupingBy { it.type to it.reason }.eachCount().forEach { (key, count) ->
                appendLine("  Refused by Health Connect: ${key.first} x$count: ${key.second}")
            }
        }
        result.excluded.forEachIndexed { index, plan ->
            appendLine()
            appendLine("Source ${result.groups.size + index + 1}: left out, ${plan.group.ready.size} records already held by another app")
        }
    }

    /**
     * The report of an import that never started: what stopped it, what the file holds, why
     * the source left records out, and what each group held back. [contents] comes from
     * [MedicalFileInspector] and holds none of the records' content. [cause] gives its class
     * only: its message can quote the file, and a release build's stack names no class.
     */
    fun failureText(
        problem: String,
        readAt: Instant,
        contents: List<String> = emptyList(),
        cause: Throwable? = null,
        plans: List<MedicalImportGroupPlan> = emptyList(),
        sourceSkips: List<MedicalSourceSkip> = emptyList(),
    ): String = buildString {
        appendLine("OpenVitals medical records import")
        appendLine("Read: $readAt")
        appendLine("Not imported: $problem")
        cause?.let { appendLine("Error: ${it::class.qualifiedName}") }
        appendSourceSkips(sourceSkips)
        if (contents.isNotEmpty()) {
            appendLine()
            appendLine("What the file holds (sizes and shapes, none of the records' content):")
            contents.forEach(::appendLine)
        }
        plans.forEachIndexed { index, plan ->
            appendLine()
            appendLine("Source ${index + 1}: ${plan.target.kind()}, FHIR ${plan.fhirVersion}")
            appendLine("  Ready ${plan.group.ready.size}, held back ${plan.group.rejected.size}")
            appendGroupDetail(plan)
        }
    }

    // By reason: a skip's detail names a file or a document's title.
    private fun StringBuilder.appendSourceSkips(skips: List<MedicalSourceSkip>) {
        skips.groupingBy { it.reason }.eachCount().forEach { (reason, count) -> appendLine("Left out before import: $reason x$count") }
    }

    private fun StringBuilder.appendGroupDetail(plan: MedicalImportGroupPlan) {
        plan.group.skippedTypes.forEach { (type, count) -> appendLine("  Skipped, not stored by Health Connect: $type x$count") }
        plan.group.rejected
            .groupingBy { Triple(it.resource.type, it.problem.reason, it.problem.field) }
            .eachCount()
            .forEach { (key, count) ->
                appendLine("  Held back: ${key.first} x$count: ${key.second}${key.third?.let { f -> " ($f)" }.orEmpty()}")
            }
        plan.group.notes
            .groupingBy { note ->
                when (note) {
                    is FhirIdNote.IdAssigned -> "Id assigned" to note.type
                    is FhirIdNote.ContainedLifted -> "Contained record lifted" to note.type
                    is FhirIdNote.ContainedDropped -> "Contained record dropped" to note.type
                    is FhirIdNote.Duplicate -> "Duplicate, the later one kept" to note.type
                }
            }
            .eachCount()
            .forEach { (key, count) -> appendLine("  ${key.first}: ${key.second} x$count") }
    }

    private fun MedicalImportTarget.kind(): String = when (this) {
        is MedicalImportTarget.Existing -> "an existing source"
        is MedicalImportTarget.New -> "a new source"
    }
}

/** The platform's reason without the exception class names it arrives wrapped in, masked like any exception message. */
fun Throwable.platformReason(): String {
    val reason = (message ?: this::class.simpleName.orEmpty()).substringAfterLast("Exception: ").trim()
    return PrivacyRedactor.maskMessage(reason) ?: this::class.simpleName.orEmpty()
}
