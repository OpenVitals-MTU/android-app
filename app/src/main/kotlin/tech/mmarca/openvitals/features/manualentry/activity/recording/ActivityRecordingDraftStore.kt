package tech.mmarca.openvitals.features.manualentry.activity.recording

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



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
