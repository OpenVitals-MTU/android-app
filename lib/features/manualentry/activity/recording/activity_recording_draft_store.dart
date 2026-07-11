import '../activity_entry_state.dart';

/// Port of the Kotlin `ActivityRecordingDraftStore` — an in-memory hand-off of a
/// finished-but-unsaved recording draft between activity-entry view models.
class ActivityRecordingDraftStore {
  ActivityEntryUiState? _draft;

  ActivityEntryUiState? restore() => _draft;

  void store(ActivityEntryUiState state) {
    if (state.isRecordingDraft && !state.isEditMode) {
      _draft = state;
    }
  }

  void clear() {
    _draft = null;
  }
}
