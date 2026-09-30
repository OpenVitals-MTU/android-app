package tech.mmarca.openvitals.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.FhirSourceGrouper
import tech.mmarca.openvitals.domain.medical.MedicalImportGroupPlan
import tech.mmarca.openvitals.domain.medical.MedicalImportGroupResult
import tech.mmarca.openvitals.domain.medical.MedicalImportPlanner
import tech.mmarca.openvitals.domain.medical.MedicalImportProgress
import tech.mmarca.openvitals.domain.medical.MedicalImportResult
import tech.mmarca.openvitals.domain.medical.MedicalImportTarget
import tech.mmarca.openvitals.domain.medical.MedicalWriteRejection
import tech.mmarca.openvitals.domain.medical.PreparedResource
import tech.mmarca.openvitals.domain.medical.platformReason
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/**
 * The import step: writes each included group into its data source, in batches.
 *
 * A batch is one transaction, so one bad record fails it. The batch is then retried one
 * record at a time: the bad one is reported with Health Connect's reason and the rest are
 * written. A failure that is not about a record, such as a revoked permission, stops the
 * import, and the result says so.
 */
class ImportMedicalRecordsUseCase @Inject constructor(
    private val repository: MedicalRecordsRepository,
) {
    suspend operator fun invoke(
        plans: List<MedicalImportGroupPlan>,
        today: LocalDate,
        onProgress: (MedicalImportProgress) -> Unit = {},
    ): MedicalImportResult {
        val included = plans.filter { it.include }
        val total = included.sumOf { it.group.ready.size }
        val ownSources = repository.ownSources().toMutableList()
        val results = mutableListOf<MedicalImportGroupResult>()
        var done = 0
        onProgress(MedicalImportProgress(done, total))
        for (plan in included) {
            val counts = GroupCounts()
            try {
                if (plan.group.ready.isNotEmpty()) {
                    val source = sourceFor(plan, ownSources, today)
                    counts.sourceName = source.displayName
                    plan.group.ready.chunked(BatchSize).forEach { batch ->
                        writeBatch(source, plan.fhirVersion, batch, counts)
                        done += batch.size
                        onProgress(MedicalImportProgress(done, total))
                    }
                } else {
                    counts.sourceName = plan.targetName
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                results += counts.result(plan)
                return MedicalImportResult(results, excluded = plans.filterNot { it.include }, stoppedBy = failure.platformReason())
            }
            results += counts.result(plan)
        }
        return MedicalImportResult(results, excluded = plans.filterNot { it.include })
    }

    private suspend fun sourceFor(
        plan: MedicalImportGroupPlan,
        ownSources: MutableList<MedicalRecordSource>,
        today: LocalDate,
    ): MedicalRecordSource = when (val target = plan.target) {
        is MedicalImportTarget.Existing -> target.source
        is MedicalImportTarget.New -> {
            // Another group of this import may have just made it. A source has one FHIR version.
            ownSources.firstOrNull { FhirSourceGrouper.sameBase(it.fhirBaseUri, target.baseUri) && it.fhirVersion == plan.fhirVersion }
                ?: repository.createSource(
                    fhirBaseUri = target.baseUri,
                    displayName = MedicalImportPlanner.uniqueName(target.name, ownSources, today),
                    fhirVersion = plan.fhirVersion,
                ).also { ownSources += it }
        }
    }

    private suspend fun writeBatch(
        source: MedicalRecordSource,
        fhirVersion: String,
        batch: List<PreparedResource>,
        counts: GroupCounts,
    ) {
        val refs = batch.map { MedicalRecordRef(source.id, it.type, it.id) }
        val existing = runCatching { repository.readRecords(refs) }
            .getOrElse { if (it is CancellationException) throw it else emptyList() }
            .mapTo(mutableSetOf()) { it.ref }
        val whole = runCatching { repository.upsert(source.id, fhirVersion, batch.map { it.json.toString() }) }
        if (whole.isSuccess) {
            counts.count(refs, existing)
            return
        }
        whole.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        batch.zip(refs).forEach { (resource, ref) ->
            val json = resource.json.toString()
            try {
                repository.upsert(source.id, fhirVersion, listOf(json))
                counts.count(listOf(ref), existing)
            } catch (refused: IllegalArgumentException) {
                counts.rejected += MedicalWriteRejection(resource.type, resource.id, refused.platformReason(), json)
            }
        }
    }

    private class GroupCounts {
        var sourceName: String = ""
        var written = 0
        var updated = 0
        val rejected = mutableListOf<MedicalWriteRejection>()
        val refs = mutableListOf<MedicalRecordRef>()

        fun count(refs: List<MedicalRecordRef>, existing: Set<MedicalRecordRef>) {
            val before = refs.count { it in existing }
            updated += before
            written += refs.size - before
            this.refs += refs
        }

        fun result(plan: MedicalImportGroupPlan) =
            MedicalImportGroupResult(plan, sourceName, written, updated, rejected.toList(), refs.toList())
    }

    private companion object {
        /** Measured on the test phone in the spike: about 0.25 s, and a failed batch costs 100 single writes. */
        const val BatchSize = 100
    }
}
