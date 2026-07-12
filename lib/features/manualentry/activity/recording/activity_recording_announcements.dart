import '../../../../core/presentation/unit_formatter.dart';
import '../../../../domain/preferences/activity_recording_preferences.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../domain/model/activity_entry_types.dart';
import 'activity_recording.dart';
import 'activity_recording_dashboard.dart' show formatRecordingElapsed;

/// Pure announcement/notification texts for a recording in progress. Port of
/// `ActivityRecordingAnnouncements.kt` (voice) and the notification content of
/// `ActivityRecordingService.kt`. The controller owns TTS/notification I/O;
/// everything here is deterministic and unit-testable.

/// Kotlin `ActivityRecordingAnnouncementTracker`.
class ActivityRecordingAnnouncementTracker {
  int _lastTimeBucket = 0;
  int _lastDistanceBucket = 0;
  int _lastLapCount = 0;
  bool _wasIdle = false;

  String? announcementFor(
    ActivityRecordingState state,
    ActivityRecordingPreferences preferences, {
    required DateTime now,
    required AppLocalizations l10n,
    required UnitFormatter unitFormatter,
  }) {
    if (state.status != ActivityRecordingStatus.recording) return null;

    if (preferences.voiceLapAnnouncementsEnabled &&
        state.manualLaps.length > _lastLapCount) {
      _lastLapCount = state.manualLaps.length;
      return l10n.activityRecordingVoiceLap(
        _lastLapCount,
        _summaryAnnouncement(state, now, l10n, unitFormatter),
      );
    }

    final idle = state.isAutoIdle(now);
    if (preferences.voiceIdleAnnouncementsEnabled && idle && !_wasIdle) {
      _wasIdle = true;
      return l10n.activityRecordingVoiceIdle;
    }
    if (preferences.voiceIdleAnnouncementsEnabled && _wasIdle && !idle) {
      _wasIdle = false;
      return l10n.activityRecordingVoiceResumed;
    }

    final minutes = preferences.voiceAnnouncementTimeIntervalMinutes;
    if (minutes != null) {
      final intervalMillis = minutes * 60000;
      if (intervalMillis > 0) {
        final bucket =
            state.elapsedDuration(now).inMilliseconds ~/ intervalMillis;
        if (bucket > _lastTimeBucket) {
          _lastTimeBucket = bucket;
          return _summaryAnnouncement(state, now, l10n, unitFormatter);
        }
      }
    }

    final meters = preferences.voiceAnnouncementDistanceIntervalMeters;
    if (meters != null && meters > 0) {
      final bucket = state.distanceMeters ~/ meters;
      if (bucket > _lastDistanceBucket) {
        _lastDistanceBucket = bucket;
        return _summaryAnnouncement(state, now, l10n, unitFormatter);
      }
    }

    return null;
  }

  void reset() {
    _lastTimeBucket = 0;
    _lastDistanceBucket = 0;
    _lastLapCount = 0;
    _wasIdle = false;
  }
}

String _summaryAnnouncement(
  ActivityRecordingState state,
  DateTime now,
  AppLocalizations l10n,
  UnitFormatter unitFormatter,
) {
  final elapsed = formatRecordingElapsed(state.elapsedDuration(now));
  final distance = unitFormatter.distance(state.distanceMeters).text;
  final averageSpeed = unitFormatter
      .averageSpeed(state.distanceMeters, state.movingDuration(now).inMilliseconds)
      .text;
  final lap = state.manualLaps.length + 1;
  return l10n.activityRecordingVoiceSummary(
      elapsed, distance, averageSpeed, lap);
}

/// Kotlin `ActivityRecordingService.notificationText`.
String activityRecordingNotificationText(
  ActivityRecordingState state, {
  required DateTime now,
  required AppLocalizations l10n,
  required UnitFormatter unitFormatter,
}) {
  final totalTime = formatRecordingElapsed(state.elapsedDuration(now));
  if (state.recordingKind == ActivityRecordingKind.repetition) {
    final activityType = activityEntryTypeById(state.activityTypeId);
    final unit = activityType?.repetitionUnit == ActivityRepetitionUnit.steps
        ? l10n.unitSteps
        : l10n.unitReps;
    return switch (state.status) {
      ActivityRecordingStatus.recording =>
        l10n.activityRecordingNotificationRepetitionRecording(
            totalTime, unitFormatter.count(state.repetitionCount), unit),
      ActivityRecordingStatus.paused =>
        l10n.activityRecordingNotificationRepetitionPaused(
            totalTime, unitFormatter.count(state.repetitionCount), unit),
      ActivityRecordingStatus.resting =>
        l10n.activityRecordingNotificationRepetitionResting(
            totalTime, formatRecordingElapsed(state.restRemainingDuration(now))),
      ActivityRecordingStatus.idle => l10n.activityRecordingNotificationTitle,
    };
  }
  final heartRateBpm = state.currentHeartRateBpm;
  final heartRateSuffix = heartRateBpm == null
      ? ''
      : ' · ${l10n.activityRecordingNotificationHeartRate(unitFormatter.count(heartRateBpm))}';
  if (state.recordingKind == ActivityRecordingKind.timed) {
    return switch (state.status) {
      ActivityRecordingStatus.recording =>
        l10n.activityRecordingNotificationTimedRecording(totalTime) +
            heartRateSuffix,
      ActivityRecordingStatus.paused =>
        l10n.activityRecordingNotificationTimedPaused(totalTime) +
            heartRateSuffix,
      ActivityRecordingStatus.resting ||
      ActivityRecordingStatus.idle =>
        l10n.activityRecordingNotificationTitle,
    };
  }
  final movingTime = formatRecordingElapsed(state.movingDuration(now));
  final distance = unitFormatter.distance(state.distanceMeters).text;
  final gpsStatus = _gpsStatusLabel(state, now, l10n);
  return switch (state.status) {
    ActivityRecordingStatus.recording =>
      l10n.activityRecordingNotificationRecording(
              totalTime, movingTime, distance, gpsStatus) +
          heartRateSuffix,
    ActivityRecordingStatus.paused => l10n.activityRecordingNotificationPaused(
        totalTime, movingTime, distance, l10n.activityEntryRecordingPaused),
    ActivityRecordingStatus.resting ||
    ActivityRecordingStatus.idle =>
      l10n.activityRecordingNotificationTitle,
  };
}

/// Kotlin `ActivityRecordingState.gpsStatusLabelRes`.
String _gpsStatusLabel(
    ActivityRecordingState state, DateTime now, AppLocalizations l10n) {
  if (state.status == ActivityRecordingStatus.paused) {
    return l10n.activityEntryRecordingPaused;
  }
  if (state.isAutoIdle(now)) return l10n.activityEntryRecordingIdle;
  return switch (state.gpsStatus) {
    ActivityGpsStatus.fix => l10n.activityEntryRecordingGpsFix,
    ActivityGpsStatus.poorAccuracy => l10n.activityEntryRecordingGpsPoor,
    ActivityGpsStatus.lost => l10n.activityEntryRecordingGpsLost,
    ActivityGpsStatus.disabled => l10n.activityEntryRecordingGpsOff,
    ActivityGpsStatus.waitingForFix => l10n.activityEntryRecordingWaitingForGps,
  };
}
