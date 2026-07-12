import '../../../domain/preferences/activity_recording_dashboard_layout.dart';
import '../../../domain/preferences/activity_recording_preferences.dart';
import '../prefs_codec.dart';
import '../prefs_store.dart';

/// Storage for the activity recording settings and the per-activity-type
/// recording dashboard layouts.
///
/// Named `...PrefsStore` and not `ActivityRecordingStore` on purpose: that name
/// belongs to the recording controller's own in-progress-session store in
/// `features/manualentry/activity/recording/`, which is a different thing
/// entirely.
///
/// `null` is stored as a sentinel `0` for the route gap and for the three
/// recording/announcement intervals — "off" and "unset" are the same state to
/// the user, and the Kotlin build wrote it this way. [read] maps the sentinel
/// back to null; do not change either direction independently.
class ActivityRecordingPrefsStore extends PrefsStore {
  const ActivityRecordingPrefsStore(super.prefs);

  ActivityRecordingPreferences read() {
    int? nullableIfSentinel(String key, int defaultValue, int sentinel) {
      final value = prefs.getInt(key) ?? defaultValue;
      return value == sentinel ? null : value;
    }

    return ActivityRecordingPreferences(
      autoIdleEnabled: prefs.getBool(_keyActivityRecordingAutoIdleEnabled) ??
          ActivityRecordingPreferences.defaultAutoIdleEnabled,
      autoIdleTimeoutSeconds:
          prefs.getInt(_keyActivityRecordingAutoIdleTimeoutSeconds) ??
              ActivityRecordingPreferences.defaultAutoIdleTimeoutSeconds,
      keepScreenOnDuringRecording:
          prefs.getBool(_keyActivityRecordingKeepScreenOn) ??
              ActivityRecordingPreferences.defaultKeepScreenOnDuringRecording,
      requiredGpsAccuracyMeters:
          prefs.getInt(_keyActivityRecordingRequiredGpsAccuracyMeters) ??
              ActivityRecordingPreferences.defaultRequiredGpsAccuracyMeters,
      routeGapMeters: nullableIfSentinel(
        _keyActivityRecordingRouteGapMeters,
        ActivityRecordingPreferences.defaultRouteGapMeters,
        _routeGapOff,
      ),
      barometerClimbEnabled:
          prefs.getBool(_keyActivityRecordingBarometerClimbEnabled) ??
              ActivityRecordingPreferences.defaultBarometerClimbEnabled,
      recordingDistanceIntervalMeters: nullableIfSentinel(
        _keyActivityRecordingDistanceIntervalMeters,
        ActivityRecordingPreferences.defaultRecordingDistanceIntervalMeters ??
            _recordingIntervalOff,
        _recordingIntervalOff,
      ),
      recordingTimeIntervalMillis:
          prefs.getInt(_keyActivityRecordingTimeIntervalMillis) ??
              ActivityRecordingPreferences.defaultRecordingTimeIntervalMillis,
      voiceAnnouncementsEnabled:
          prefs.getBool(_keyActivityRecordingVoiceEnabled) ??
              ActivityRecordingPreferences.defaultVoiceAnnouncementsEnabled,
      voiceAnnouncementTimeIntervalMinutes: nullableIfSentinel(
        _keyActivityRecordingVoiceTimeIntervalMinutes,
        ActivityRecordingPreferences.defaultVoiceAnnouncementTimeIntervalMinutes,
        _recordingIntervalOff,
      ),
      voiceAnnouncementDistanceIntervalMeters: nullableIfSentinel(
        _keyActivityRecordingVoiceDistanceIntervalMeters,
        ActivityRecordingPreferences
            .defaultVoiceAnnouncementDistanceIntervalMeters,
        _recordingIntervalOff,
      ),
      voiceIdleAnnouncementsEnabled:
          prefs.getBool(_keyActivityRecordingVoiceIdleEnabled) ??
              ActivityRecordingPreferences.defaultVoiceIdleAnnouncementsEnabled,
      voiceLapAnnouncementsEnabled:
          prefs.getBool(_keyActivityRecordingVoiceLapEnabled) ??
              ActivityRecordingPreferences.defaultVoiceLapAnnouncementsEnabled,
      restTimerBellEnabled:
          prefs.getBool(_keyActivityRecordingRestTimerBellEnabled) ??
              ActivityRecordingPreferences.defaultRestTimerBellEnabled,
    ).normalized();
  }

  void write(ActivityRecordingPreferences preferences) {
    final normalized = preferences.normalized();
    putBool(_keyActivityRecordingAutoIdleEnabled, normalized.autoIdleEnabled);
    putInt(
      _keyActivityRecordingAutoIdleTimeoutSeconds,
      normalized.autoIdleTimeoutSeconds,
    );
    putBool(
      _keyActivityRecordingKeepScreenOn,
      normalized.keepScreenOnDuringRecording,
    );
    putInt(
      _keyActivityRecordingRequiredGpsAccuracyMeters,
      normalized.requiredGpsAccuracyMeters,
    );
    putInt(
      _keyActivityRecordingRouteGapMeters,
      normalized.routeGapMeters ?? _routeGapOff,
    );
    putBool(
      _keyActivityRecordingBarometerClimbEnabled,
      normalized.barometerClimbEnabled,
    );
    putInt(
      _keyActivityRecordingDistanceIntervalMeters,
      normalized.recordingDistanceIntervalMeters ?? _recordingIntervalOff,
    );
    putInt(
      _keyActivityRecordingTimeIntervalMillis,
      normalized.recordingTimeIntervalMillis,
    );
    putBool(
      _keyActivityRecordingVoiceEnabled,
      normalized.voiceAnnouncementsEnabled,
    );
    putInt(
      _keyActivityRecordingVoiceTimeIntervalMinutes,
      normalized.voiceAnnouncementTimeIntervalMinutes ?? _recordingIntervalOff,
    );
    putInt(
      _keyActivityRecordingVoiceDistanceIntervalMeters,
      normalized.voiceAnnouncementDistanceIntervalMeters ?? _recordingIntervalOff,
    );
    putBool(
      _keyActivityRecordingVoiceIdleEnabled,
      normalized.voiceIdleAnnouncementsEnabled,
    );
    putBool(
      _keyActivityRecordingVoiceLapEnabled,
      normalized.voiceLapAnnouncementsEnabled,
    );
    putBool(
      _keyActivityRecordingRestTimerBellEnabled,
      normalized.restTimerBellEnabled,
    );
  }

  ActivityRecordingDashboardLayout readDashboardLayout(String activityTypeId) {
    final raw = prefs.getString(_dashboardLayoutKey(activityTypeId));
    if (raw == null) return ActivityRecordingDashboardLayout();
    return layoutFromPreferenceString(raw) ??
        ActivityRecordingDashboardLayout();
  }

  void writeDashboardLayout(
    String activityTypeId,
    ActivityRecordingDashboardLayout layout,
  ) {
    if (activityTypeId.trim().isEmpty) return;
    putString(
      _dashboardLayoutKey(activityTypeId),
      layoutToPreferenceString(layout),
    );
  }

  String _dashboardLayoutKey(String activityTypeId) =>
      '$_keyActivityRecordingDashboardLayoutPrefix$activityTypeId';

  // region Keys and sentinels (on-disk format — never rename or renumber one).
  static const String _keyActivityRecordingAutoIdleEnabled =
      'activity_recording_auto_idle_enabled';
  static const String _keyActivityRecordingAutoIdleTimeoutSeconds =
      'activity_recording_auto_idle_timeout_seconds';
  static const String _keyActivityRecordingKeepScreenOn =
      'activity_recording_keep_screen_on';
  static const String _keyActivityRecordingRequiredGpsAccuracyMeters =
      'activity_recording_required_gps_accuracy_meters';
  static const String _keyActivityRecordingRouteGapMeters =
      'activity_recording_route_gap_meters';
  static const String _keyActivityRecordingBarometerClimbEnabled =
      'activity_recording_barometer_climb_enabled';
  static const String _keyActivityRecordingDistanceIntervalMeters =
      'activity_recording_distance_interval_meters';
  static const String _keyActivityRecordingTimeIntervalMillis =
      'activity_recording_time_interval_millis';
  static const String _keyActivityRecordingVoiceEnabled =
      'activity_recording_voice_enabled';
  static const String _keyActivityRecordingVoiceTimeIntervalMinutes =
      'activity_recording_voice_time_interval_minutes';
  static const String _keyActivityRecordingVoiceDistanceIntervalMeters =
      'activity_recording_voice_distance_interval_meters';
  static const String _keyActivityRecordingVoiceIdleEnabled =
      'activity_recording_voice_idle_enabled';
  static const String _keyActivityRecordingVoiceLapEnabled =
      'activity_recording_voice_lap_enabled';
  static const String _keyActivityRecordingRestTimerBellEnabled =
      'activity_recording_rest_timer_bell_enabled';
  static const String _keyActivityRecordingDashboardLayoutPrefix =
      'activity_recording_dashboard_layout_';
  static const int _routeGapOff = 0;
  static const int _recordingIntervalOff = 0;
  // endregion
}
