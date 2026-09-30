package tech.mmarca.openvitals.domain.usecase

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.FhirSourceGrouper
import tech.mmarca.openvitals.domain.medical.FhirVersionDetector
import tech.mmarca.openvitals.domain.medical.ManualFhirWriter
import tech.mmarca.openvitals.domain.medical.ManualRecordDraft
import tech.mmarca.openvitals.domain.medical.ManualRecordKind
import tech.mmarca.openvitals.domain.medical.MedicalImportPlanner
import tech.mmarca.openvitals.domain.medical.PatientIdentity
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

sealed interface ManualSaveResult {
    data class Saved(val ref: MedicalRecordRef) : ManualSaveResult

    /** The owner's name is not known yet: the form asks once, then saves again. */
    data object NeedsIdentity : ManualSaveResult
}

/**
 * Saves a manual entry into the one manual source, `openvitals://manual`. The first entry also
 * writes the owner's Patient record, taken from a Patient this app can read or else from what
 * the user typed. Health Connect is the only copy of it.
 */
class SaveManualMedicalRecordUseCase @Inject constructor(
    private val repository: MedicalRecordsRepository,
) {
    /** True when the next save needs the owner's name first. */
    suspend fun needsIdentity(): Boolean {
        val source = manualSource()
        return (source == null || !hasPatient(source)) && readablePatient() == null
    }

    /** The stored record for an edit, or null when it is gone. */
    suspend fun stored(kind: ManualRecordKind, id: String): JsonObject? {
        val source = manualSource() ?: return null
        return repository.readRecords(listOf(MedicalRecordRef(source.id, kind.resourceType, id)))
            .firstOrNull()?.let { Json.parseToJsonElement(it.json).jsonObject }
    }

    /**
     * [editId] updates that record and keeps its other fields. [identity] is what the user typed
     * when [needsIdentity] said so. [sourceName] names the manual source the first time it is made.
     */
    suspend operator fun invoke(
        draft: ManualRecordDraft,
        today: LocalDate,
        sourceName: String,
        identity: PatientIdentity? = null,
        editId: String? = null,
        now: Instant = Instant.now(),
    ): ManualSaveResult {
        val existing = manualSource()
        val needsPatient = existing == null || !hasPatient(existing)
        val owner = if (needsPatient) identity ?: readablePatient() ?: return ManualSaveResult.NeedsIdentity else null
        val source = existing ?: repository.createSource(
            fhirBaseUri = ManualFhirWriter.SourceBaseUri,
            displayName = MedicalImportPlanner.uniqueName(sourceName, repository.ownSources(), today),
            fhirVersion = FhirVersionDetector.R4,
        )
        val id = editId ?: UUID.randomUUID().toString()
        val base = editId?.let { stored(draft.kind, it) }
        val record = ManualFhirWriter.resource(draft, id, today, base, savedAt = now)
        // One transaction: the owner's record and the entry are written together, or neither is.
        val jsons = listOfNotNull(owner?.let(ManualFhirWriter::patient), record).map { it.toString() }
        repository.upsert(source.id, source.fhirVersion, jsons)
        return ManualSaveResult.Saved(MedicalRecordRef(source.id, draft.kind.resourceType, id))
    }

    private suspend fun manualSource(): MedicalRecordSource? =
        repository.ownSources().firstOrNull { FhirSourceGrouper.sameBase(it.fhirBaseUri, ManualFhirWriter.SourceBaseUri) }

    private suspend fun hasPatient(source: MedicalRecordSource): Boolean =
        repository.readRecords(listOf(MedicalRecordRef(source.id, "Patient", ManualFhirWriter.PatientId))).isNotEmpty()

    /** The first named Patient this app can read. Without access to personal details there is none. */
    private suspend fun readablePatient(): PatientIdentity? {
        val patients = runCatching { repository.readCategory(MedicalCategory.PERSONAL_DETAILS) }
            .getOrElse { if (it is CancellationException) throw it else emptyList() }
        return patients.asSequence()
            .filter { it.ref.resourceType == "Patient" }
            .map { PatientIdentity.of(Json.parseToJsonElement(it.json).jsonObject) }
            .firstOrNull { it.givenNames.isNotEmpty() || it.familyNames.isNotEmpty() }
    }
}
