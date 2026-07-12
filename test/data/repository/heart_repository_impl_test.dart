import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/repository/impl/heart_repository_impl.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';

/// Records what the repository asks the data source for, so the tests can pin the
/// series lookback window and prove the aggregating read is not used.
class _CaptureDataSource extends HealthDataSource {
  _CaptureDataSource(this._raw, {Set<String>? granted})
      : _granted = granted ?? {HcPermissions.readHeartRate};

  final List<HeartRateSample> _raw;
  final Set<String> _granted;

  DateTime? capturedRawStart;
  DateTime? capturedRawEnd;
  bool aggregatedReadCalled = false;

  /// What the TIME-SLICED fallback returns, when the record-bounded raw read
  /// finds nothing.
  List<HeartRateSample> aggregated = const <HeartRateSample>[];
  Duration? capturedBucket;

  @override
  HealthConnectAvailability get cachedAvailability =>
      HealthConnectAvailability.available;

  @override
  Future<Set<String>> grantedPermissions() async => _granted;

  @override
  Future<List<HeartRateSample>> readRawHeartRateSamples(
    DateTime start,
    DateTime end,
  ) async {
    capturedRawStart = start;
    capturedRawEnd = end;
    return _raw;
  }

  @override
  Future<List<HeartRateSample>> readHeartRateSamples(
    DateTime start,
    DateTime end,
  ) async {
    aggregatedReadCalled = true;
    return const [];
  }

  @override
  Future<List<HeartRateSample>> readAggregatedHeartRateSamples(
    DateTime start,
    DateTime end,
    Duration bucket,
  ) async {
    capturedBucket = bucket;
    return aggregated;
  }
}

HeartRateSample _sample(DateTime time, int bpm) =>
    HeartRateSample(time: time, beatsPerMinute: bpm, source: 'test');

void main() {
  group('loadHeartRateSamplesInstant', () {
    final start = DateTime.utc(2026, 7, 11, 10);
    final end = DateTime.utc(2026, 7, 11, 11);

    test(
      'includes samples from a series record that started before the workout',
      () async {
        // The regression: Health Connect filters HeartRateRecord by the RECORD's
        // boundary, so a Gadgetbridge record opened at 09:20 -- carrying samples that
        // run into the workout -- was dropped whole, and the first minutes of the
        // activity had no heart rate at all.
        final source = _CaptureDataSource([
          // Deliberately out of order, to prove the sort.
          _sample(end.subtract(const Duration(minutes: 1)), 150),
          _sample(start.subtract(const Duration(seconds: 1)), 60), // before window
          _sample(start, 120), // the sample the bug used to drop
          _sample(end, 99), // end is exclusive
        ]);
        final repo = HeartRepositoryImpl(source);

        final samples = await repo.loadHeartRateSamplesInstant(start, end);

        expect(
          samples.map((s) => s.beatsPerMinute).toList(),
          [120, 150],
          reason: 'window is [start, end); results are sorted by time',
        );
      },
    );

    test('reads back one hour to catch the enclosing series record', () async {
      final source = _CaptureDataSource(const []);
      final repo = HeartRepositoryImpl(source);

      await repo.loadHeartRateSamplesInstant(start, end);

      expect(source.capturedRawStart, start.subtract(const Duration(hours: 1)));
      expect(source.capturedRawEnd, end);
      expect(
        source.aggregatedReadCalled,
        isFalse,
        reason: 'the aggregating read re-applies the record-boundary filter',
      );
    });


    test(
      'falls back to the time-sliced read when the raw read is record-blind',
      () async {
        // The bug, caught on a real 36-minute strength session. The raw read
        // SUCCEEDED and Health Connect returned records -- but every sample in
        // them sat outside the session, because Health Connect filters series
        // records by the RECORD's boundary and the enclosing record began before
        // the one-hour look-back could reach it. The filter correctly dropped them
        // all, and an activity that DID have a heart rate reported "Not
        // available", with nothing anywhere looking broken.
        //
        // Aggregation slices by TIME, not by record, so it cannot be fooled the
        // same way.
        final source = _CaptureDataSource(
          // The raw read returns samples, but all of them precede the workout.
          [
            _sample(start.subtract(const Duration(minutes: 50)), 61),
            _sample(start.subtract(const Duration(minutes: 20)), 63),
          ],
        )..aggregated = [
            _sample(start.add(const Duration(minutes: 10)), 120),
            _sample(start.add(const Duration(minutes: 30)), 131),
          ];

        final samples =
            await HeartRepositoryImpl(source).loadHeartRateSamplesInstant(
          start,
          end,
        );

        expect(samples, hasLength(2));
        expect(samples.map((s) => s.beatsPerMinute), [120, 131]);
        // Fine enough to be a trace: the 15-minute chart bucket would render a
        // one-hour workout as four points.
        expect(source.capturedBucket, lessThanOrEqualTo(const Duration(minutes: 1)));
      },
    );

    test('the fallback is NOT used when the raw read already found samples',
        () async {
      final source = _CaptureDataSource([
        _sample(start.add(const Duration(minutes: 5)), 140),
      ])..aggregated = [_sample(start, 99)];

      final samples =
          await HeartRepositoryImpl(source).loadHeartRateSamplesInstant(start, end);

      expect(samples.map((s) => s.beatsPerMinute), [140]);
      expect(source.capturedBucket, isNull,
          reason: 'the extra query must only happen when raw finds nothing');
    });

    test('returns empty for an inverted or empty window', () async {
      final source = _CaptureDataSource([_sample(start, 120)]);
      final repo = HeartRepositoryImpl(source);

      expect(await repo.loadHeartRateSamplesInstant(end, start), isEmpty);
      expect(await repo.loadHeartRateSamplesInstant(start, start), isEmpty);
      expect(source.capturedRawStart, isNull);
    });

    test('returns empty without the heart-rate permission', () async {
      final source = _CaptureDataSource(
        [_sample(start, 120)],
        granted: const {},
      );
      final repo = HeartRepositoryImpl(source);

      expect(await repo.loadHeartRateSamplesInstant(start, end), isEmpty);
      expect(source.capturedRawStart, isNull);
    });
  });
}
