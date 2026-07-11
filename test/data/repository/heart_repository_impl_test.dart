import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/repository/impl/heart_repository_impl.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/health/health_data_source.dart';
import 'package:openvitals/health/health_permissions.dart';

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
