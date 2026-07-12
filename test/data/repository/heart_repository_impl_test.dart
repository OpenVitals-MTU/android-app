import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/impl/heart_repository_impl.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';

/// Records what the repository asks the data source for.
///
/// The point of these tests changed when the record-boundary problem moved to
/// Kotlin. Working out WHICH Health Connect records hold the samples for a window
/// — the widened re-read, the clip, the aggregate last resort — is now settled in
/// `HealthConnectSeries.kt`, next to the SDK that has the problem. Dart used to
/// guess at it from the far side of a Pigeon channel, with a one-hour look-back
/// that a 17-hour record walked straight through.
///
/// What is left to pin here is the contract that is still Dart's: ask for the
/// window you were actually given, and trust what comes back.
class _CaptureDataSource extends HealthDataSource {
  _CaptureDataSource(this._raw, {Set<String>? granted})
      : _granted = granted ?? {HcPermissions.readHeartRate};

  final List<HeartRateSample> _raw;
  final Set<String> _granted;

  DateTime? capturedRawStart;
  DateTime? capturedRawEnd;

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
}

HeartRateSample _sample(DateTime time, int bpm) =>
    HeartRateSample(time: time, beatsPerMinute: bpm, source: 'test');

void main() {
  group('loadHeartRateSamplesInstant', () {
    final start = DateTime.utc(2026, 7, 11, 10);
    final end = DateTime.utc(2026, 7, 11, 11);

    test('asks for exactly the window it was given', () async {
      final source = _CaptureDataSource(const []);

      await HeartRepositoryImpl(source).loadHeartRateSamplesInstant(start, end);

      // No look-back, no padding. Widening the read to work around Health
      // Connect's record boundaries was the old Dart-side heuristic, and it is
      // gone: the native reader owns that, and it can look at the record instead
      // of guessing how long it might be.
      expect(source.capturedRawStart, start);
      expect(source.capturedRawEnd, end);
    });

    test('returns the samples the native reader found', () async {
      final source = _CaptureDataSource([
        _sample(start, 120),
        _sample(start.add(const Duration(minutes: 30)), 150),
      ]);

      final samples = (await HeartRepositoryImpl(source)
              .loadHeartRateSamplesInstant(start, end))
          .orThrow();

      expect(samples.map((s) => s.beatsPerMinute), [120, 150]);
    });

    test('returns empty for an inverted or empty window', () async {
      final source = _CaptureDataSource([_sample(start, 120)]);
      final repo = HeartRepositoryImpl(source);

      expect(
          (await repo.loadHeartRateSamplesInstant(end, start)).orThrow(), isEmpty);
      expect(
          (await repo.loadHeartRateSamplesInstant(start, start)).orThrow(),
          isEmpty);
      expect(source.capturedRawStart, isNull);
    });

    test('returns empty without the heart-rate permission', () async {
      final source = _CaptureDataSource(
        [_sample(start, 120)],
        granted: const {},
      );

      expect(
        (await HeartRepositoryImpl(source).loadHeartRateSamplesInstant(start, end))
            .orThrow(),
        isEmpty,
      );
      expect(source.capturedRawStart, isNull);
    });
  });
}
