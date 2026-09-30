package tech.mmarca.openvitals.domain.usecase

import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.FhirBundleWriter
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/** What to export: everything, one category, or one record. */
sealed interface MedicalExportScope {
    data object All : MedicalExportScope
    data class Category(val category: MedicalCategory) : MedicalExportScope
    data class Record(val ref: MedicalRecordRef) : MedicalExportScope
}

/**
 * A FHIR Bundle ready to save or share. [ownOnly] lists categories whose read access is
 * declined, so they hold only this app's records. [leftOut] lists categories with no
 * access at all.
 */
data class MedicalExport(
    val bundleJson: String,
    val recordCount: Int,
    val ownOnly: Set<MedicalCategory>,
    val leftOut: Set<MedicalCategory>,
)

/** Reads every record the scope covers and this app may read, and writes them as one Bundle. */
class ExportMedicalRecordsUseCase @Inject constructor(
    private val repository: MedicalRecordsRepository,
) {
    suspend operator fun invoke(scope: MedicalExportScope, appVersion: String, exportedAt: Instant): MedicalExport {
        val readable = repository.readableCategories()
        val ownOnly = mutableSetOf<MedicalCategory>()
        val leftOut = mutableSetOf<MedicalCategory>()
        val records = when (scope) {
            is MedicalExportScope.Record -> repository.readRecords(listOf(scope.ref))
            is MedicalExportScope.Category -> readCategories(listOf(scope.category), readable, ownOnly, leftOut)
            MedicalExportScope.All -> readCategories(MedicalCategory.entries, readable, ownOnly, leftOut)
        }
        val json = FhirBundleWriter.write(records, sources(), appVersion, exportedAt)
        return MedicalExport(json, records.size, ownOnly, leftOut)
    }

    private suspend fun readCategories(
        categories: List<MedicalCategory>,
        readable: Set<MedicalCategory>,
        ownOnly: MutableSet<MedicalCategory>,
        leftOut: MutableSet<MedicalCategory>,
    ): List<MedicalRecord> = categories.flatMap { category ->
        try {
            repository.readCategory(category).also { if (category !in readable) ownOnly += category }
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            if (!failure.isPermissionFailure()) throw failure
            leftOut += category
            emptyList()
        }
    }

    /** Sources name each entry's origin. Without them the file still holds every record. */
    private suspend fun sources(): List<MedicalRecordSource> {
        val all = runCatching { repository.allSources() }.getOrElse { if (it is CancellationException) throw it else emptyList() }
        val own = runCatching { repository.ownSources() }.getOrElse { if (it is CancellationException) throw it else emptyList() }
        return (all + own).distinctBy { it.id }
    }
}
