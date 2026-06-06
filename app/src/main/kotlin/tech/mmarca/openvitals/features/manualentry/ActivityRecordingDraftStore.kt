package tech.mmarca.openvitals.features.manualentry

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRecordingDraftStore @Inject constructor() {
    private var draft: ActivityEntryUiState? = null

    fun restore(): ActivityEntryUiState? = draft

    fun store(state: ActivityEntryUiState) {
        if (state.isRecordingDraft && !state.isEditMode) {
            draft = state
        }
    }

    fun clear() {
        draft = null
    }
}
