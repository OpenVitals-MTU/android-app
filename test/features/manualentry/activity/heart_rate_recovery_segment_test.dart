import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/insights/heart_rate_recovery.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_clock.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_state.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_write_request_builder.dart';

/// The guided test writes the moment effort stopped; the reader finds it again.
///
/// These two halves are the feature. The recording knows the instant of cessation and
/// nothing else does — Health Connect has no field for it, so it goes in as a trailing
/// REST segment. If the segment the writer produces is not one the reader accepts, the
/// whole guided test silently degrades to an ordinary workout, measured from the session
/// end, and nobody would notice until the numbers were wrong.

final DateTime _start = DateTime(2026, 7, 14, 10, 0);
final DateTime _effortEnded = DateTime(2026, 7, 14, 10, 20);

ActivityEntryUiState _recordedTest({
  DateTime? recoveryStart,
  List<ActivityPauseInterval> pauses = const [],
}) {
  final clock = ActivityEntryClock.system();
  return initialActivityEntryState(clock, const {'write-exercise'}).copyWith(
    startDateText: '2026-07-14',
    startTimeText: '10:00',
    // Effort for 20 minutes, then 5 minutes of recovery.
    durationMinutesText: '25',
    isRecordingDraft: true,
    recordedRecoveryStartTime: recoveryStart,
    recordedPauseIntervals: pauses,
    recordedBleSamples: BleRecordingSampleBuffer(
      heartRateSamples: [
        BleHeartRateSample(time: _start, beatsPerMinute: 120),
        BleHeartRateSample(time: _effortEnded, beatsPerMinute: 178),
      ],
    ),
  );
}

/// The session as it comes back OUT of Health Connect, built from what went in.
ExerciseData _readBack(ActivityWriteRequest request) => ExerciseData(
      id: 'w1',
      title: request.title,
      exerciseType: request.exerciseType,
      startTime: request.startTime,
      endTime: request.endTime,
      durationMs: request.endTime.difference(request.startTime).inMilliseconds,
      source: 'openvitals',
      segments: [
        for (final segment in request.exerciseSegments)
          ExerciseSegmentData(
            startTime: segment.startTime,
            endTime: segment.endTime,
            segmentType: segment.segmentType,
            repetitions: segment.repetitions,
          ),
      ],
    );

void main() {
  test('the recovery is written as a trailing rest segment, and read back', () {
    final request = buildWriteRequest(
      _recordedTest(recoveryStart: _effortEnded),
      UnitSystem.metric,
    );
    expect(request, isNotNull);

    final rest = request!.exerciseSegments
        .where((s) => s.segmentType == ExerciseSegmentType.rest)
        .toList();
    expect(rest, hasLength(1));
    expect(rest.single.startTime, _effortEnded);
    // To the END of the session, not for a fixed five minutes: a rider who takes a while
    // to press save must not leave the segment stranded mid-session, where the reader
    // would ignore it and measure from the session end instead — a later moment, a lower
    // heart rate, and a recovery that flatters them.
    expect(rest.single.endTime, request.endTime);

    // No "active" segment. Health Connect validates a segment's type against the
    // session's exercise type and THROWS on a mismatch, taking the whole save with it.
    // Rest and pause are the two universal types; nothing needs an active one.
    expect(
      request.exerciseSegments.every((s) =>
          s.segmentType == ExerciseSegmentType.rest ||
          s.segmentType == ExerciseSegmentType.pause),
      isTrue,
    );

    // And now the half that matters: the reader finds it.
    final window = heartRateRecoveryWindowFor(_readBack(request));
    expect(window.source, HeartRateRecoveryStartSource.trailingRestSegment);
    expect(window.recoveryStart, _effortEnded);
  });

  test('pauses during the effort survive alongside the recovery mark', () {
    // Explicit segments suppress the ones the native writer would synthesize from the
    // pause intervals, so they have to be carried through by hand or they vanish.
    final request = buildWriteRequest(
      _recordedTest(
        recoveryStart: _effortEnded,
        pauses: [
          ActivityPauseInterval(
            startTime: DateTime(2026, 7, 14, 10, 5),
            endTime: DateTime(2026, 7, 14, 10, 7),
          ),
        ],
      ),
      UnitSystem.metric,
    );

    final pauses = request!.exerciseSegments
        .where((s) => s.segmentType == ExerciseSegmentType.pause)
        .toList();
    expect(pauses, hasLength(1));
    expect(pauses.single.startTime, DateTime(2026, 7, 14, 10, 5));
  });

  test('an ordinary recording gets no recovery mark at all', () {
    final request = buildWriteRequest(
      _recordedTest(recoveryStart: null),
      UnitSystem.metric,
    );

    expect(
      request!.exerciseSegments
          .where((s) => s.segmentType == ExerciseSegmentType.rest),
      isEmpty,
    );
    // And the reader measures from the session end, as it does for any watch workout.
    final window = heartRateRecoveryWindowFor(_readBack(request));
    expect(window.source, HeartRateRecoveryStartSource.sessionEnd);
  });
}
