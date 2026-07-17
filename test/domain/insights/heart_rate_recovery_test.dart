import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/insights/heart_rate_recovery.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/heart_models.dart';

/// The instant effort stops. Everything is expressed as an offset from it, in seconds,
/// so a reader can see at a glance which side of the stop a sample is on.
final DateTime _stop = DateTime.utc(2026, 7, 14, 18, 30);

DateTime _at(int seconds) => _stop.add(Duration(seconds: seconds));

HeartRateSample _hr(int atSeconds, int bpm, {String source = 'strap'}) =>
    HeartRateSample(
      time: _at(atSeconds),
      beatsPerMinute: bpm,
      source: source,
    );

/// A hard effort that stops dead, sampled every second — a chest strap.
///
/// Peak 180 five seconds before the stop, 178 at the stop, then a normal decay.
int _bpmAt(int seconds) {
  if (seconds <= 0) return seconds == -5 || seconds == -4 ? 180 : 178;
  const anchors = <int, int>{
    0: 178,
    10: 170,
    30: 155,
    60: 145,
    120: 130,
    180: 120,
    240: 115,
    300: 110,
  };
  final keys = anchors.keys.toList()..sort();
  for (var i = 1; i < keys.length; i++) {
    final lo = keys[i - 1];
    final hi = keys[i];
    if (seconds <= hi) {
      final span = hi - lo;
      final t = (seconds - lo) / span;
      return (anchors[lo]! + (anchors[hi]! - anchors[lo]!) * t).round();
    }
  }
  return anchors[300]!;
}

List<HeartRateSample> _strapSamples({int everySeconds = 1}) => [
      for (var t = -60; t <= 300; t += everySeconds) _hr(t, _bpmAt(t)),
    ];

HeartRateRecoveryReading _calculate(
  List<HeartRateSample> samples, {
  int? profileMax = 190,
  int? restingHeartRate = 55,
  int? age = 40,
  int? observedMax,
}) =>
    calculateHeartRateRecovery(
      recoveryStart: _stop,
      samples: samples,
      profileMaxHeartRateBpm: profileMax,
      restingHeartRateBpm: restingHeartRate,
      ageYears: age,
      observedMaxHeartRateBpm: observedMax,
    );

int? _dropAt(HeartRateRecoveryReading reading, Duration offset) =>
    reading.markAt(offset)!.dropBpm;

int? _bpmMark(HeartRateRecoveryReading reading, Duration offset) =>
    reading.markAt(offset)!.heartRateBpm;

ExerciseData _session({
  List<ExerciseSegmentData> segments = const [],
  Duration duration = const Duration(minutes: 30),
}) {
  final start = _stop.subtract(duration);
  return ExerciseData(
    id: 'w1',
    title: 'Bike',
    exerciseType: 0,
    startTime: start,
    endTime: _stop,
    durationMs: duration.inMilliseconds,
    source: 'test',
    segments: segments,
  );
}

ExerciseSegmentData _rest(int fromSeconds, int toSeconds) => ExerciseSegmentData(
      startTime: _at(fromSeconds),
      endTime: _at(toSeconds),
      segmentType: ExerciseSegmentType.rest,
      repetitions: 0,
    );

void main() {
  group('calculateHeartRateRecovery', () {
    test('the offsets are 30s..5min, no 10s mark', () {
      expect(heartRateRecoveryOffsets, const [
        Duration(seconds: 30),
        Duration(minutes: 1),
        Duration(minutes: 2),
        Duration(minutes: 3),
        Duration(minutes: 4),
        Duration(minutes: 5),
      ]);
    });

    test('a chest strap at 1Hz measures every mark and reads clean', () {
      final reading = _calculate(_strapSamples());

      expect(reading.quality, HeartRateRecoveryQuality.clean);
      expect(reading.issues, isEmpty);
      expect(reading.peakBpm, 180);
      expect(reading.peakWindowSeconds, 10);

      // Every mark measured, and the drop is peak minus the sample there.
      for (final offset in heartRateRecoveryOffsets) {
        expect(_bpmMark(reading, offset), isNotNull,
            reason: 'no sample at $offset');
      }
      expect(_bpmMark(reading, const Duration(minutes: 1)), 145);
      expect(_dropAt(reading, const Duration(minutes: 1)), 180 - 145);
      expect(reading.headlineDropBpm, 35);
      expect(reading.isComparable, isTrue);
    });

    test(
        'a watch that samples once a minute after the workout leaves the 30s mark '
        'BLANK rather than interpolated', () {
      final samples = <HeartRateSample>[
        // Dense during the effort...
        for (var t = -60; t <= 0; t += 5) _hr(t, _bpmAt(t), source: 'watch'),
        // ...then the watch reverts to a reading a minute.
        for (var t = 60; t <= 300; t += 60) _hr(t, _bpmAt(t), source: 'watch'),
      ];

      final reading = _calculate(samples);

      // The 30s mark cannot be produced from this data — and is never invented.
      expect(_bpmMark(reading, const Duration(seconds: 30)), isNull);
      expect(_dropAt(reading, const Duration(seconds: 30)), isNull);

      // The one-minute mark it can, so the reading still charts.
      expect(_bpmMark(reading, const Duration(minutes: 1)), 145);
      expect(_dropAt(reading, const Duration(minutes: 1)), 35);
      expect(_bpmMark(reading, const Duration(minutes: 5)), 110);
      expect(reading.isComparable, isTrue);
    });

    test('a watch every 5 seconds keeps all six marks', () {
      final reading = _calculate(_strapSamples(everySeconds: 5));

      for (final offset in heartRateRecoveryOffsets) {
        expect(_bpmMark(reading, offset), isNotNull, reason: 'missing $offset');
      }
      expect(reading.marks, hasLength(6));
      expect(reading.quality, HeartRateRecoveryQuality.clean);
    });

    test('a watch that stops recording at the workout end measures nothing', () {
      final samples = [
        for (var t = -60; t <= 0; t += 5) _hr(t, _bpmAt(t)),
      ];

      final reading = _calculate(samples);

      expect(reading.quality, HeartRateRecoveryQuality.noData);
      expect(reading.issues, contains(HeartRateRecoveryIssue.noRecoverySamples));
      expect(reading.recoverySampleCount, 0);
      expect(reading.marks, hasLength(6));
      for (final mark in reading.marks) {
        expect(mark.heartRateBpm, isNull);
        expect(mark.dropBpm, isNull);
      }
      expect(reading.isComparable, isFalse);
    });

    test('no samples at all is noData, not a crash', () {
      final reading = _calculate(const []);
      expect(reading.quality, HeartRateRecoveryQuality.noData);
      expect(reading.peakBpm, isNull);
    });

    test('nothing in the hard last-10s window means no peak, and noData', () {
      // Effort ends early: nothing at all in the last 40 seconds before the stop. A wider
      // peak window would draw the peak from when the effort was still going and inflate
      // the recovery; the hard window instead refuses to measure.
      final samples = <HeartRateSample>[
        for (var t = -60; t <= -40; t += 5) _hr(t, t == -45 ? 180 : 176),
        for (var t = 60; t <= 300; t += 60) _hr(t, _bpmAt(t)),
      ];

      final reading = _calculate(samples);

      expect(reading.peakBpm, isNull);
      expect(reading.quality, HeartRateRecoveryQuality.noData);
    });

    test('easing off before pressing stop is caught, not rewarded', () {
      // Peak 180 at -45s, walked down to 160 by the stop. The fall from the last real
      // high point (180) to the reading at the stop (160) is 20 bpm — well over the gate.
      final samples = <HeartRateSample>[
        for (var t = -60; t <= 0; t++)
          _hr(t, t <= -45 ? 180 : (180 - ((t + 45) * 20 / 45)).round()),
        for (var t = 1; t <= 300; t++) _hr(t, _bpmAt(t)),
      ];

      final reading = _calculate(samples);

      expect(reading.issues, contains(HeartRateRecoveryIssue.cooldownBeforeStop));
      expect(reading.quality, HeartRateRecoveryQuality.invalid);
      expect(reading.isComparable, isFalse,
          reason: 'an invalid reading must never reach the trend');
    });

    test('a fall of just five bpm before the stop still counts as a cool-down', () {
      // The gate is now 4 bpm, just above beat-to-beat noise. High of 176 in the last
      // minute, 171 at the stop: a 5-bpm easing-off that the old 8-bpm gate would miss.
      final samples = <HeartRateSample>[
        for (var t = -60; t <= 0; t++) _hr(t, t <= -20 ? 176 : 171),
        for (var t = 1; t <= 300; t++) _hr(t, (171 - t ~/ 6).clamp(120, 171)),
      ];

      final reading = _calculate(samples);

      expect(reading.issues, contains(HeartRateRecoveryIssue.cooldownBeforeStop));
      expect(reading.quality, HeartRateRecoveryQuality.invalid);
    });

    test('a heart rate that ROSE after the stop is not a recovery', () {
      final samples = <HeartRateSample>[
        for (var t = -180; t <= 0; t += 60) _hr(t, 113),
        _hr(60, 115),
        _hr(120, 117),
        _hr(180, 115),
        _hr(240, 116),
        _hr(300, 94),
      ];

      final reading = _calculate(samples, profileMax: 130);

      expect(reading.issues, contains(HeartRateRecoveryIssue.heartRateDidNotFall));
      expect(reading.quality, HeartRateRecoveryQuality.invalid);
      expect(reading.isComparable, isFalse,
          reason: 'a negative recovery must never reach the trend');
    });

    test('a reading with no one-minute mark cannot be charted', () {
      final samples = <HeartRateSample>[
        for (var t = -120; t <= 0; t += 60) _hr(t, 120),
        _hr(30, 98),
        _hr(210, 92),
      ];

      final reading = _calculate(samples, profileMax: 130);

      expect(_bpmMark(reading, const Duration(seconds: 30)), 98);
      expect(_dropAt(reading, const Duration(seconds: 30)), 22);
      expect(_bpmMark(reading, const Duration(minutes: 1)), isNull);
      expect(reading.isComparable, isFalse,
          reason: 'no one-minute fall means no point to plot');
    });

    test('a submaximal effort is shown, flagged not-comparable, never hidden', () {
      // Peak 152 against a stated max of 190: more than 10 bpm below a KNOWN maximum, so
      // submaximal. The drop is still measured; it just cannot be compared across days.
      final samples = [
        for (var t = -60; t <= 300; t += 5)
          _hr(t, t <= 0 ? 152 : (152 - t ~/ 6).clamp(110, 152)),
      ];

      final reading = _calculate(samples);

      expect(reading.issues, contains(HeartRateRecoveryIssue.submaximalEffort));
      expect(reading.quality, HeartRateRecoveryQuality.notComparable);
      // There is no separate "not vigorous" hide-gate: even a weak effort is shown.
      expect(_dropAt(reading, const Duration(minutes: 1)), isNotNull);
      expect(reading.isComparable, isFalse);
    });

    test('near-max is an absolute band, wider for an ESTIMATED max', () {
      // A 40-year-old's estimated max is 208 - 0.7*40 = 180. A peak of 160 is 20 below it
      // — inside the 22-bpm confidence band, so NOT flagged submaximal.
      final samples = [for (var t = -60; t <= 300; t += 5) _hr(t, t <= 0 ? 160 : 150)];
      final reading = _calculate(samples, profileMax: null, age: 40);

      expect(reading.maxHeartRateBpmUsed, 180);
      expect(reading.maxHeartRateEstimated, isTrue);
      expect(reading.issues,
          isNot(contains(HeartRateRecoveryIssue.submaximalEffort)));
    });

    test('the same peak against a KNOWN max is submaximal (tighter band)', () {
      // Peak 160 against a STATED max of 180 is 20 below it — beyond the 10-bpm band that
      // applies when the maximum is known rather than estimated.
      final samples = [for (var t = -60; t <= 300; t += 5) _hr(t, t <= 0 ? 160 : 150)];
      final reading = _calculate(samples, profileMax: 180, age: 40);

      expect(reading.maxHeartRateEstimated, isFalse);
      expect(reading.issues, contains(HeartRateRecoveryIssue.submaximalEffort));
      expect(reading.quality, HeartRateRecoveryQuality.notComparable);
    });

    test('an unknown max heart rate still reports every mark', () {
      final reading = _calculate(
        _strapSamples(),
        profileMax: null,
        age: null,
        observedMax: null,
        restingHeartRate: null,
      );

      expect(reading.issues, contains(HeartRateRecoveryIssue.unknownMaxHeartRate));
      expect(reading.maxHeartRateBpmUsed, isNull);
      expect(reading.quality, HeartRateRecoveryQuality.approximate);
      expect(_dropAt(reading, const Duration(minutes: 1)), 35);
    });

    test('the age formula is Tanaka (208 - 0.7*age), flagged estimated', () {
      // 20yo: 208 - 0.7*20 = 194 (the old 220-age gave 200).
      final young = _calculate(_strapSamples(), profileMax: null, age: 20);
      expect(young.maxHeartRateBpmUsed, 194);
      expect(young.maxHeartRateEstimated, isTrue);

      // 40yo: 208 - 28 = 180.
      final middle = _calculate(_strapSamples(), profileMax: null, age: 40);
      expect(middle.maxHeartRateBpmUsed, 180);
    });

    test('an observed max below the trust bar is not used as a maximum', () {
      final reading = _calculate(
        _strapSamples(),
        profileMax: null,
        observedMax: 140,
        restingHeartRate: 55,
        age: 40,
      );

      expect(reading.maxHeartRateBpmUsed, 180,
          reason: 'the age estimate, not the untrustworthy observed 140');
      expect(reading.maxHeartRateEstimated, isTrue);
    });

    test('two sources on the same instant collapse to the higher reading', () {
      final samples = <HeartRateSample>[
        ..._strapSamples(),
        for (var t = -60; t <= 300; t += 1)
          _hr(t, _bpmAt(t) - 3, source: 'watch'),
      ];

      final reading = _calculate(samples);

      // Higher of the two kept: the strap's 145, not the watch's 142. That reports the
      // SMALLER drop, which is the conservative direction.
      expect(_bpmMark(reading, const Duration(minutes: 1)), 145);
    });

    test('samples arriving out of order are sorted, not trusted', () {
      final shuffled = _strapSamples().reversed.toList();
      final reading = _calculate(shuffled);

      expect(reading.peakBpm, 180);
      expect(_dropAt(reading, const Duration(minutes: 1)), 35);
      expect(reading.quality, HeartRateRecoveryQuality.clean);
    });

    test('a sample exactly on the tighter 1-minute tolerance boundary counts', () {
      // The 1-minute tolerance is now +-5s. The only recovery sample sits at 65s.
      final samples = <HeartRateSample>[
        for (var t = -60; t <= 0; t++) _hr(t, _bpmAt(t)),
        _hr(65, 144),
      ];

      final reading = _calculate(samples);

      expect(_bpmMark(reading, const Duration(minutes: 1)), 144);
      expect(reading.markAt(const Duration(minutes: 1))!.sampleSkew,
          const Duration(seconds: 5));
    });

    test('a sample one second beyond the tolerance does not', () {
      final samples = <HeartRateSample>[
        for (var t = -60; t <= 0; t++) _hr(t, _bpmAt(t)),
        _hr(66, 144),
      ];

      final reading = _calculate(samples);

      expect(_bpmMark(reading, const Duration(minutes: 1)), isNull);
    });

    test('a tie between two samples goes to the earlier, higher one', () {
      // Equidistant either side of the 2-minute mark (now +-5s tolerance).
      final samples = <HeartRateSample>[
        for (var t = -60; t <= 0; t++) _hr(t, _bpmAt(t)),
        _hr(60, 145),
        _hr(118, 133),
        _hr(122, 128),
      ];

      final reading = _calculate(samples);

      // 118 and 122 are both 2s from 120. The earlier wins: on a falling curve it is the
      // higher reading, so it reports the smaller drop.
      expect(_bpmMark(reading, const Duration(minutes: 2)), 133);
    });
  });

  group('heartRateRecoveryWindowFor', () {
    test('a session with no rest segment has no recovery window', () {
      // The core change: an ordinary workout gives no guarantee effort ceased, so its end
      // is NOT taken as a stop. No cessation mark, no reading.
      expect(heartRateRecoveryWindowFor(_session()), isNull);
    });

    test('a qualifying trailing rest segment is the moment effort stopped', () {
      final window = heartRateRecoveryWindowFor(
        _session(segments: [_rest(-300, 0)]),
      );

      expect(window, isNotNull);
      expect(window!.recoveryStart, _at(-300));
      expect(window.source, HeartRateRecoveryStartSource.trailingRestSegment);
      // Reads a minute back for the peak, and past the last mark for the tail.
      expect(window.readStart, _at(-360));
      expect(window.readEnd, _at(30));
    });

    test(
        'the rest segment after the last set of a strength workout is NOT a recovery',
        () {
      // The app writes a rest segment after every set. A 60s breather is too short to
      // qualify, so there is no recovery window at all.
      expect(
        heartRateRecoveryWindowFor(
          _session(segments: [_rest(-600, -540), _rest(-60, 0)]),
        ),
        isNull,
      );
    });

    test('a long rest that is not at the end is not a recovery either', () {
      // Five minutes of rest, but the session ran on for four more minutes afterwards.
      expect(
        heartRateRecoveryWindowFor(
          _session(segments: [_rest(-540, -240)]),
        ),
        isNull,
      );
    });

    test('a rest ending just shy of the session end still qualifies', () {
      final window = heartRateRecoveryWindowFor(
        _session(segments: [_rest(-300, -20)]),
      );

      expect(window, isNotNull);
      expect(window!.recoveryStart, _at(-300));
      expect(window.source, HeartRateRecoveryStartSource.trailingRestSegment);
    });
  });
}
