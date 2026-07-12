import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/prefs/preferences_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/preferences/activity_recording_preferences.dart';

/// Holds the live [ActivityRecordingPreferences] and writes each change back
/// through [PreferencesRepository]. Backs the activity-recording card only, so
/// it stays out of the shared `SettingsState`.
///
/// The state is the preferences object itself — already an immutable value with
/// `copyWith`, so a freezed wrapper would add a field and nothing else.
class ActivityRecordingPreferencesViewModel
    extends Notifier<ActivityRecordingPreferences> {
  PreferencesRepository get _prefs => ref.read(preferencesRepositoryProvider);

  @override
  ActivityRecordingPreferences build() => _prefs.activityRecordingPreferences();

  void update(ActivityRecordingPreferences preferences) {
    _prefs.setActivityRecordingPreferences(preferences);
    // Read back so the state matches the repository's normalize() result.
    state = _prefs.activityRecordingPreferences();
  }
}

/// The state provider for the activity-recording settings card.
final activityRecordingPreferencesProvider = NotifierProvider<
    ActivityRecordingPreferencesViewModel, ActivityRecordingPreferences>(
  ActivityRecordingPreferencesViewModel.new,
);
