import 'package:freezed_annotation/freezed_annotation.dart';

part 'activity_recording_preferences.freezed.dart';

/// Port of the Kotlin `ActivityRecordingPreferences` — GPS/activity recording
/// tuning used by the (deferred) activity-recording feature. Persisted through
/// `PreferencesRepository`.
@freezed
abstract class ActivityRecordingPreferences with _$ActivityRecordingPreferences {
  const ActivityRecordingPreferences._();

  const factory ActivityRecordingPreferences({
    @Default(ActivityRecordingPreferences.defaultAutoIdleEnabled)
    bool autoIdleEnabled,
    @Default(ActivityRecordingPreferences.defaultAutoIdleTimeoutSeconds)
    int autoIdleTimeoutSeconds,
    @Default(ActivityRecordingPreferences.defaultKeepScreenOnDuringRecording)
    bool keepScreenOnDuringRecording,
    @Default(ActivityRecordingPreferences.defaultRequiredGpsAccuracyMeters)
    int requiredGpsAccuracyMeters,
    @Default(ActivityRecordingPreferences.defaultRouteGapMeters)
    int? routeGapMeters,
    @Default(ActivityRecordingPreferences.defaultBarometerClimbEnabled)
    bool barometerClimbEnabled,
    int? recordingDistanceIntervalMeters,
    @Default(ActivityRecordingPreferences.defaultRecordingTimeIntervalMillis)
    int recordingTimeIntervalMillis,
    @Default(ActivityRecordingPreferences.defaultVoiceAnnouncementsEnabled)
    bool voiceAnnouncementsEnabled,
    @Default(
      ActivityRecordingPreferences.defaultVoiceAnnouncementTimeIntervalMinutes,
    )
    int? voiceAnnouncementTimeIntervalMinutes,
    @Default(
      ActivityRecordingPreferences
          .defaultVoiceAnnouncementDistanceIntervalMeters,
    )
    int? voiceAnnouncementDistanceIntervalMeters,
    @Default(ActivityRecordingPreferences.defaultVoiceIdleAnnouncementsEnabled)
    bool voiceIdleAnnouncementsEnabled,
    @Default(ActivityRecordingPreferences.defaultVoiceLapAnnouncementsEnabled)
    bool voiceLapAnnouncementsEnabled,
    @Default(ActivityRecordingPreferences.defaultRestTimerBellEnabled)
    bool restTimerBellEnabled,
  }) = _ActivityRecordingPreferences;

  ActivityRecordingPreferences normalized() => copyWith(
        autoIdleTimeoutSeconds: autoIdleTimeoutSeconds
            .clamp(minAutoIdleTimeoutSeconds, maxAutoIdleTimeoutSeconds)
            .toInt(),
        requiredGpsAccuracyMeters: _closestAllowed(
          requiredGpsAccuracyMeters,
          allowedGpsAccuracyMeters,
        ),
        routeGapMeters: routeGapMeters == null
            ? null
            : _closestAllowed(routeGapMeters!, allowedRouteGapMeters),
        recordingDistanceIntervalMeters: recordingDistanceIntervalMeters == null
            ? null
            : _closestAllowed(
                recordingDistanceIntervalMeters!,
                allowedRecordingDistanceIntervalMeters,
              ),
        recordingTimeIntervalMillis: _closestAllowed(
          recordingTimeIntervalMillis,
          allowedRecordingTimeIntervalMillis,
        ),
        voiceAnnouncementTimeIntervalMinutes:
            voiceAnnouncementTimeIntervalMinutes == null
                ? null
                : _closestAllowed(
                    voiceAnnouncementTimeIntervalMinutes!,
                    allowedVoiceAnnouncementTimeIntervalMinutes,
                  ),
        voiceAnnouncementDistanceIntervalMeters:
            voiceAnnouncementDistanceIntervalMeters == null
                ? null
                : _closestAllowed(
                    voiceAnnouncementDistanceIntervalMeters!,
                    allowedVoiceAnnouncementDistanceIntervalMeters,
                  ),
      );

  static int _closestAllowed(int value, List<int> allowedValues) {
    var best = allowedValues.first;
    var bestDistance = (best - value).abs();
    for (final candidate in allowedValues.skip(1)) {
      final distance = (candidate - value).abs();
      if (distance < bestDistance) {
        best = candidate;
        bestDistance = distance;
      }
    }
    return best;
  }

  static const bool defaultAutoIdleEnabled = true;
  static const int defaultAutoIdleTimeoutSeconds = 10;
  static const int minAutoIdleTimeoutSeconds = 5;
  static const int maxAutoIdleTimeoutSeconds = 60;
  static const bool defaultKeepScreenOnDuringRecording = false;
  static const int defaultRequiredGpsAccuracyMeters = 30;
  static const bool defaultBarometerClimbEnabled = true;
  static const int? defaultRecordingDistanceIntervalMeters = null;
  static const int defaultRecordingTimeIntervalMillis = 500;
  static const bool defaultVoiceAnnouncementsEnabled = false;
  static const int defaultVoiceAnnouncementTimeIntervalMinutes = 5;
  static const int defaultVoiceAnnouncementDistanceIntervalMeters = 1000;
  static const bool defaultVoiceIdleAnnouncementsEnabled = true;
  static const bool defaultVoiceLapAnnouncementsEnabled = true;
  static const bool defaultRestTimerBellEnabled = true;
  static const int defaultRouteGapMeters = 200;
  static const List<int> allowedGpsAccuracyMeters = [10, 30, 50, 100];
  static const List<int> allowedRouteGapMeters = [100, 200, 500];
  static const List<int> allowedRecordingDistanceIntervalMeters = [5, 10, 25, 50];
  static const List<int> allowedRecordingTimeIntervalMillis = [
    500,
    1000,
    5000,
    10000,
  ];
  static const List<int> allowedVoiceAnnouncementTimeIntervalMinutes = [1, 5, 10];
  static const List<int> allowedVoiceAnnouncementDistanceIntervalMeters = [
    500,
    1000,
    5000,
  ];
}
