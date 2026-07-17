import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_clock.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_state.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_write_request_builder.dart';

/// The session written to Health Connect has to CONTAIN the samples it carries.
///
/// A recording reaches the entry form as TEXT, at minute granularity: the start time
/// loses its seconds, and the duration is rounded up to a whole minute. The write path
/// then rebuilds the session range from that text — so the rebuilt end can land before
/// the last sample that was actually recorded.
///
/// Health Connect does not drop a sample that falls outside its session. It CLAMPS it to
/// the session bounds. So the samples past the end are not lost, they are stacked onto
/// the closing instant, all sharing one timestamp — which is worse than losing them, and
/// is invisible unless you go looking. For a bike ride it is a strange-looking tail. For
/// a heart-rate recovery, computed from precisely those last samples, it is everything.

BleRecordingSampleBuffer _heartRate(List<(DateTime, int)> beats) =>
    BleRecordingSampleBuffer(
      heartRateSamples: [
        for (final (time, bpm) in beats)
          BleHeartRateSample(time: time, beatsPerMinute: bpm),
      ],
    );

ActivityEntryUiState _recorded({
  required String startTimeText,
  required String durationMinutesText,
  required BleRecordingSampleBuffer samples,
}) {
  final clock = ActivityEntryClock.system();
  return initialActivityEntryState(clock, const {'write-exercise'}).copyWith(
    startDateText: '2026-07-14',
    startTimeText: startTimeText,
    durationMinutesText: durationMinutesText,
    isRecordingDraft: true,
    recordedBleSamples: samples,
  );
}

void main() {
  test('the session is stretched to cover the last recorded sample', () {
    // The worked example: recording started at 10:00:59 and ran 120 seconds. The form
    // truncates the start to 10:00:00 and rounds the duration to 2 minutes, so the
    // rebuilt session ends at 10:02:00 — while the last heart-rate sample was taken at
    // 10:02:59, a full 59 seconds later.
    final request = buildWriteRequest(
      _recorded(
        startTimeText: '10:00',
        durationMinutesText: '2',
        samples: _heartRate([
          (DateTime(2026, 7, 14, 10, 0, 59), 150),
          (DateTime(2026, 7, 14, 10, 1, 59), 165),
          (DateTime(2026, 7, 14, 10, 2, 59), 172),
        ]),
      ),
      UnitSystem.metric,
    );

    expect(request, isNotNull);
    final lastSample = DateTime(2026, 7, 14, 10, 2, 59);
    expect(
      request!.endTime.isAfter(lastSample),
      isTrue,
      reason: 'the session must end after its own last sample, or Health Connect '
          'collapses that final minute of readings onto a single instant',
    );
    // And the samples themselves are still all there.
    expect(request.bleSamples.heartRateSamples, hasLength(3));
  });

  test('an untruncated recording is left exactly as it is', () {
    final request = buildWriteRequest(
      _recorded(
        startTimeText: '10:00',
        durationMinutesText: '30',
        samples: _heartRate([
          (DateTime(2026, 7, 14, 10, 5), 150),
          (DateTime(2026, 7, 14, 10, 25), 160),
        ]),
      ),
      UnitSystem.metric,
    );

    expect(request!.endTime, DateTime(2026, 7, 14, 10, 30),
        reason: 'nothing to stretch: the samples already fit');
    expect(request.bleSamples.heartRateSamples, hasLength(2));
  });

  test('samples before a start the user moved forward are dropped, not clamped', () {
    // The user recorded from 10:00, then edited the start to 10:10 before saving. The
    // early samples cannot be kept: Health Connect would clamp them ONTO 10:10, inventing
    // a burst of readings that were never taken at that time.
    final request = buildWriteRequest(
      _recorded(
        startTimeText: '10:10',
        durationMinutesText: '20',
        samples: _heartRate([
          (DateTime(2026, 7, 14, 10, 0), 120),
          (DateTime(2026, 7, 14, 10, 15), 150),
        ]),
      ),
      UnitSystem.metric,
    );

    expect(request, isNotNull, reason: 'the workout still saves');
    expect(
      request!.bleSamples.isEmpty(),
      isTrue,
      reason: 'better to write no heart rate than to write a reading at a time it '
          'was never taken',
    );
  });
}
