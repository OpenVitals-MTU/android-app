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
        'a watch that samples once a minute after the workout cannot give 10s or 30s, '
        'and they come back BLANK rather than interpolated', () {
      final samples = <HeartRateSample>[
        // Dense during the effort...
        for (var t = -60; t <= 0; t += 5) _hr(t, _bpmAt(t), source: 'watch'),
        // ...then the watch reverts to a reading a minute.
        for (var t = 60; t <= 300; t += 60) _hr(t, _bpmAt(t), source: 'watch'),
      ];

      final reading = _calculate(samples);

      // The two marks nobody can produce from this data.
      expect(_bpmMark(reading, const Duration(seconds: 10)), isNull);
      expect(_dropAt(reading, const Duration(seconds: 10)), isNull);
      expect(_bpmMark(reading, const Duration(seconds: 30)), isNull);
      expect(_dropAt(reading, const Duration(seconds: 30)), isNull);

      // The ones it can.
      expect(_bpmMark(reading, const Duration(minutes: 1)), 145);
      expect(_dropAt(reading, const Duration(minutes: 1)), 35);
      expect(_bpmMark(reading, const Duration(minutes: 5)), 110);

      expect(reading.issues, contains(HeartRateRecoveryIssue.coarseSampling));
      expect(reading.medianRecoveryGapSeconds, 60);
      expect(reading.quality, HeartRateRecoveryQuality.approximate);
      // Still worth charting: the headline mark survives.
      expect(reading.isComparable, isTrue);
    });

    test('a watch every 5 seconds keeps all seven marks', () {
      final reading = _calculate(_strapSamples(everySeconds: 5));

      for (final offset in heartRateRecoveryOffsets) {
        expect(_bpmMark(reading, offset), isNotNull, reason: 'missing $offset');
      }
      expect(reading.issues, isNot(contains(HeartRateRecoveryIssue.coarseSampling)));
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
      expect(reading.marks, hasLength(7));
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

    test('a peak 45s back widens the window and says so', () {
      final samples = <HeartRateSample>[
        // Effort ends early; nothing in the last 40 seconds before the stop.
        for (var t = -60; t <= -40; t += 5) _hr(t, t == -45 ? 180 : 176),
        for (var t = 60; t <= 300; t += 60) _hr(t, _bpmAt(t)),
      ];

      final reading = _calculate(samples);

      expect(reading.peakBpm, 180);
      expect(reading.peakWindowSeconds, 60);
      expect(reading.issues, contains(HeartRateRecoveryIssue.peakWindowWidened));
      expect(reading.quality, HeartRateRecoveryQuality.approximate);
    });

    test('easing off before pressing stop is caught, not rewarded', () {
      // Peak 180 at -45s, walked down to 160 by the stop. A naive reading would take
      // the peak from the last ten seconds (~163) and report a modest, flattering drop.
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

    test('a heart rate that ROSE after the stop is not a recovery', () {
      // Taken from a real ride: the watch sampled about once a minute, the session ended
      // while the rider was still riding, and the heart rate at 2, 3 and 4 minutes came
      // back HIGHER than the "peak" — drops of -4, -2 and -3. A recovery of minus four
      // beats is not a small recovery. It is not a recovery.
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
      // Also from a real ride: the only sample that landed near any mark was at 30
      // seconds. The trend is of the ONE-minute fall, so this reading has nothing to
      // contribute to it, however sound the 30-second figure is.
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

    test('an effort below 70% of max has no recovery worth the name', () {
      // Peak 110 against a max of 190 == 58%.
      final samples = [
        for (var t = -60; t <= 300; t += 5) _hr(t, t <= 0 ? 110 : 100),
      ];

      final reading = _calculate(samples);

      expect(reading.issues, contains(HeartRateRecoveryIssue.effortNotVigorous));
      expect(reading.quality, HeartRateRecoveryQuality.invalid);
    });

    test('a hard-but-submaximal effort is real, and not comparable', () {
      // Peak 152 against 190 == 80%: between the vigorous floor and near-maximal.
      final samples = [
        for (var t = -60; t <= 300; t += 5)
          _hr(t, t <= 0 ? 152 : (152 - t ~/ 6).clamp(110, 152)),
      ];

      final reading = _calculate(samples);

      expect(reading.issues, contains(HeartRateRecoveryIssue.submaximalEffort));
      expect(reading.quality, HeartRateRecoveryQuality.notComparable);
      // The drop is still measured — we just refuse to compare it.
      expect(_dropAt(reading, const Duration(minutes: 1)), isNotNull);
      expect(reading.isComparable, isFalse);
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
      // The point: no birth year must not mean a blank screen.
      expect(_dropAt(reading, const Duration(minutes: 1)), 35);
    });

    test('max heart rate falls back to the age formula, flagged as estimated', () {
      final reading = _calculate(_strapSamples(), profileMax: null, age: 40);

      expect(reading.maxHeartRateBpmUsed, 180);
      expect(reading.maxHeartRateEstimated, isTrue);
    });

    test('an observed max below the trust bar is not used as a maximum', () {
      // 140 is the ceiling of an easy week, not a maximum: under 150, and only 85
      // above a resting rate of 55. The age formula should win instead.
      final reading = _calculate(
        _strapSamples(),
        profileMax: null,
        observedMax: 140,
        restingHeartRate: 55,
        age: 40,
      );

      expect(reading.maxHeartRateBpmUsed, 180, reason: '220 - 40, not the observed 140');
      expect(reading.maxHeartRateEstimated, isTrue);
    });

    test('two sources on the same instant collapse to the higher reading', () {
      final samples = <HeartRateSample>[
        ..._strapSamples(),
        // A watch, recording the same session, one beat adrift.
        for (var t = -60; t <= 300; t += 1)
          _hr(t, _bpmAt(t) - 3, source: 'watch'),
      ];

      final reading = _calculate(samples);

      // The duplicates must not read as zero-second gaps and mask coarse sampling.
      expect(reading.medianRecoveryGapSeconds, 1);
      expect(reading.issues, isNot(contains(HeartRateRecoveryIssue.coarseSampling)));
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

    test('a sample exactly on the tolerance boundary counts', () {
      // The 1-minute tolerance is +-10s. Put the only recovery sample at 70s.
      final samples = <HeartRateSample>[
        for (var t = -60; t <= 0; t++) _hr(t, _bpmAt(t)),
        _hr(70, 144),
      ];

      final reading = _calculate(samples);

      expect(_bpmMark(reading, const Duration(minutes: 1)), 144);
      expect(reading.markAt(const Duration(minutes: 1))!.sampleSkew,
          const Duration(seconds: 10));
    });

    test('a sample one second beyond the tolerance does not', () {
      final samples = <HeartRateSample>[
        for (var t = -60; t <= 0; t++) _hr(t, _bpmAt(t)),
        _hr(71, 144),
      ];

      final reading = _calculate(samples);

      expect(_bpmMark(reading, const Duration(minutes: 1)), isNull);
    });

    test('a tie between two samples goes to the earlier, higher one', () {
      // Equidistant either side of the 2-minute mark (+-15s tolerance).
      final samples = <HeartRateSample>[
        for (var t = -60; t <= 0; t++) _hr(t, _bpmAt(t)),
        _hr(60, 145),
        _hr(110, 133),
        _hr(130, 128),
      ];

      final reading = _calculate(samples);

      // 110 and 130 are both 10s from 120. The earlier wins: on a falling curve it is
      // the higher reading, so it reports the smaller drop.
      expect(_bpmMark(reading, const Duration(minutes: 2)), 133);
    });
  });

  group('heartRateRecoveryWindowFor', () {
    test('a session with no segments measures from its end', () {
      final window = heartRateRecoveryWindowFor(_session());

      expect(window.recoveryStart, _stop);
      expect(window.source, HeartRateRecoveryStartSource.sessionEnd);
      // Reads a minute back for the peak, and past the last mark for the tail.
      expect(window.readStart, _at(-60));
      expect(window.readEnd, _at(330));
    });

    test('a qualifying trailing rest segment is the moment effort stopped', () {
      final window = heartRateRecoveryWindowFor(
        _session(segments: [_rest(-300, 0)]),
      );

      expect(window.recoveryStart, _at(-300));
      expect(window.source, HeartRateRecoveryStartSource.trailingRestSegment);
    });

    test(
        'the rest segment after the last set of a strength workout is NOT a recovery',
        () {
      // The app writes a rest segment after every set, the last one included. A bare
      // "ends with a rest segment" rule would read this 60s breather as an HRR test.
      final window = heartRateRecoveryWindowFor(
        _session(segments: [_rest(-600, -540), _rest(-60, 0)]),
      );

      expect(window.recoveryStart, _stop,
          reason: '60s is an inter-set rest, not a recovery');
      expect(window.source, HeartRateRecoveryStartSource.sessionEnd);
    });

    test('a long rest that is not at the end is not a recovery either', () {
      // Five minutes of rest, but the session ran on for four more minutes afterwards.
      final window = heartRateRecoveryWindowFor(
        _session(segments: [_rest(-540, -240)]),
      );

      expect(window.recoveryStart, _stop);
      expect(window.source, HeartRateRecoveryStartSource.sessionEnd);
    });

    test('a rest ending just shy of the session end still qualifies', () {
      final window = heartRateRecoveryWindowFor(
        _session(segments: [_rest(-300, -20)]),
      );

      expect(window.recoveryStart, _at(-300));
      expect(window.source, HeartRateRecoveryStartSource.trailingRestSegment);
    });
  });
}
