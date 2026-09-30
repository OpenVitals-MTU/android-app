package tech.mmarca.openvitals.features.medical

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsPreferences
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.ManualFhirWriter
import tech.mmarca.openvitals.domain.medical.ManualRecordDraft
import tech.mmarca.openvitals.domain.medical.ManualRecordKind
import tech.mmarca.openvitals.domain.medical.PatientIdentity
import tech.mmarca.openvitals.domain.usecase.ManualSaveResult
import tech.mmarca.openvitals.domain.usecase.SaveManualMedicalRecordUseCase
import tech.mmarca.openvitals.navigation.MEDICAL_ENTRY_ID_ARG
import tech.mmarca.openvitals.navigation.MEDICAL_ENTRY_KIND_ARG

/** The owner's name and birth date, typed once on the first manual entry. */
data class MedicalOwnerFields(val givenName: String = "", val familyName: String = "", val birthDate: LocalDate? = null) {
    val hasName: Boolean get() = givenName.isNotBlank() || familyName.isNotBlank()

    fun identity(): PatientIdentity = PatientIdentity(
        givenNames = givenName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() },
        familyNames = listOfNotNull(familyName.trim().takeIf { it.isNotEmpty() }),
        birthDate = birthDate?.toString(),
    )
}

/** [draft] is null while an edit loads, or when the link names no kind. [owner] shows only when [needsIdentity]. */
data class MedicalRecordEntryUiState(
    val kind: ManualRecordKind? = null,
    val isEdit: Boolean = false,
    val isLoading: Boolean = true,
    val draft: ManualRecordDraft? = null,
    val needsIdentity: Boolean = false,
    val owner: MedicalOwnerFields = MedicalOwnerFields(),
    val canWrite: Boolean = true,
    val firstRequestDone: Boolean = true,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: ScreenError? = null,
) {
    fun canSave(today: LocalDate): Boolean =
        !isLoading && !isSaving && canWrite && draft?.isComplete(today) == true && (!needsIdentity || owner.hasName)
}

/** Adds or edits one manual record: a vaccine, an allergy, a medication or a condition. */
@HiltViewModel
class MedicalRecordEntryViewModel(
    private val repository: MedicalRecordsRepository,
    private val preferences: MedicalRecordsPreferences,
    private val saveRecord: SaveManualMedicalRecordUseCase,
    private val kind: ManualRecordKind?,
    private val editId: String?,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    @Inject
    constructor(
        repository: MedicalRecordsRepository,
        preferences: MedicalRecordsPreferences,
        saveRecord: SaveManualMedicalRecordUseCase,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        preferences = preferences,
        saveRecord = saveRecord,
        kind = savedStateHandle.get<String>(MEDICAL_ENTRY_KIND_ARG)?.let { name -> ManualRecordKind.entries.firstOrNull { it.name == name } },
        editId = savedStateHandle.get<String>(MEDICAL_ENTRY_ID_ARG)?.takeIf { it.isNotBlank() },
    )

    private val _uiState = MutableStateFlow(
        MedicalRecordEntryUiState(kind = kind, isEdit = editId != null, firstRequestDone = preferences.firstPermissionRequestDone),
    )
    val uiState: StateFlow<MedicalRecordEntryUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load()
    }

    /** Reads write access, whether the owner's name is still needed, and the stored record for an edit. */
    fun load() {
        val kind = kind ?: run {
            _uiState.update { it.copy(isLoading = false, error = ScreenError.MissingArgument) }
            return
        }
        loadCoordinator.launch(viewModelScope) load@{
            runCatching {
                val canWrite = repository.canWrite()
                val needsIdentity = canWrite && saveRecord.needsIdentity()
                val draft = _uiState.value.draft ?: editId?.let { id -> saveRecord.stored(kind, id)?.let { ManualFhirWriter.draftOf(kind, it) } }
                    ?: ManualRecordDraft(kind, date = today().takeIf { kind == ManualRecordKind.VACCINE })
                Triple(canWrite, needsIdentity, draft)
            }.onSuccess { (canWrite, needsIdentity, draft) ->
                if (!isCurrent) return@load
                _uiState.update { it.copy(isLoading = false, canWrite = canWrite, needsIdentity = needsIdentity, draft = draft, error = null) }
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                if (!isCurrent) return@load
                _uiState.update { it.copy(isLoading = false, canWrite = false, error = failure.toScreenError(logTag = TAG)) }
            }
        }
    }

    fun update(change: (ManualRecordDraft) -> ManualRecordDraft) {
        _uiState.update { state -> state.copy(draft = state.draft?.let(change)) }
    }

    fun updateOwner(change: (MedicalOwnerFields) -> MedicalOwnerFields) {
        _uiState.update { it.copy(owner = change(it.owner)) }
    }

    /** Before the area's first request, all thirteen; after it, write alone. */
    fun permissionsToRequest(): Set<String> =
        if (_uiState.value.firstRequestDone) repository.writePermissions else repository.permissions

    fun onPermissionsRequested() {
        if (_uiState.value.firstRequestDone) return
        preferences.firstPermissionRequestDone = true
        _uiState.update { it.copy(firstRequestDone = true) }
    }

    /** [sourceName] names the manual source the first time it is made, in the user's language. */
    fun save(sourceName: String) {
        val state = _uiState.value
        val draft = state.draft ?: return
        if (!state.canSave(today())) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                saveRecord(draft, today(), sourceName, identity = state.owner.identity().takeIf { state.needsIdentity }, editId = editId)
            }.onSuccess { result ->
                _uiState.update {
                    when (result) {
                        is ManualSaveResult.Saved -> it.copy(isSaving = false, saved = true)
                        ManualSaveResult.NeedsIdentity -> it.copy(isSaving = false, needsIdentity = true)
                    }
                }
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                _uiState.update { it.copy(isSaving = false, error = failure.toScreenError(logTag = TAG)) }
            }
        }
    }

    private companion object {
        const val TAG = "MedicalRecordEntry"
    }
}
