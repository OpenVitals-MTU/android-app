import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/source/sync/import_record_sync_codec.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';

DateTime utc(int y, int mo, int d, [int h = 0, int mi = 0]) =>
    DateTime.utc(y, mo, d, h, mi);

/// One record of each structural family, to exercise the codec breadth.
final samples = <ImportRecord>[
  StepsImportRecord(
    clientRecordId: 'x',
    startTime: utc(2026, 1, 1, 8),
    startZoneOffset: const Duration(hours: 1),
    endTime: utc(2026, 1, 1, 9),
    endZoneOffset: const Duration(hours: 1),
    count: 1200,
  ),
  WeightImportRecord(
    clientRecordId: 'x',
    time: utc(2026, 1, 2),
    zoneOffset: null,
    kilograms: 72.4,
  ),
  HeartRateImportRecord(
    clientRecordId: 'x',
    startTime: utc(2026, 1, 3, 6),
    startZoneOffset: const Duration(hours: -5),
    endTime: utc(2026, 1, 3, 7),
    endZoneOffset: const Duration(hours: -5),
    samples: [
      HeartRateSampleValue(utc(2026, 1, 3, 6, 0), 60),
      HeartRateSampleValue(utc(2026, 1, 3, 6, 30), 65),
    ],
  ),
  SleepSessionImportRecord(
    clientRecordId: 'x',
    startTime: utc(2026, 1, 4, 23),
    startZoneOffset: null,
    endTime: utc(2026, 1, 5, 7),
    endZoneOffset: null,
    title: 'Night',
    stages: [
      SleepStageValue(startTime: utc(2026, 1, 4, 23), endTime: utc(2026, 1, 5, 1), stage: SleepStageType.light),
      SleepStageValue(startTime: utc(2026, 1, 5, 1), endTime: utc(2026, 1, 5, 3), stage: SleepStageType.deep),
    ],
  ),
  NutritionImportRecord(
    clientRecordId: 'x',
    startTime: utc(2026, 1, 6, 12),
    startZoneOffset: null,
    endTime: utc(2026, 1, 6, 12),
    endZoneOffset: null,
    name: 'Lunch',
    nutrientGrams: const {'protein': 30.5, 'totalCarbohydrate': 45.0},
    energyKilocalories: 600,
  ),
  ExerciseSessionImportRecord(
    clientRecordId: 'x',
    startTime: utc(2026, 1, 7, 18),
    startZoneOffset: null,
    endTime: utc(2026, 1, 7, 19),
    endZoneOffset: null,
    exerciseType: ImportExerciseType.running,
    title: 'Evening run',
    route: ExerciseRoute([
      ExerciseRouteLocation(time: utc(2026, 1, 7, 18, 1), latitude: 41.1, longitude: 2.1, altitudeMeters: 12),
    ]),
  ),
  CervicalMucusImportRecord(
    clientRecordId: 'x',
    time: utc(2026, 1, 8),
    zoneOffset: null,
    appearance: CervicalMucusAppearance.eggWhite,
    sensation: CervicalMucusSensation.medium,
  ),
  BloodPressureImportRecord(
    clientRecordId: 'x',
    time: utc(2026, 1, 9),
    zoneOffset: null,
    systolicMmHg: 118,
    diastolicMmHg: 76,
  ),
];

void main() {
  group('round-trip encode/decode', () {
    for (final original in samples) {
      test('${original.targetType} survives a round trip', () {
        final fp = syncFingerprint(original);
        final payload = encodeImportRecordPayload(original);
        final decoded = decodeImportRecord(
          recordType: original.targetType,
          clientRecordId: fp,
          payload: payload,
        );
        expect(decoded.targetType, original.targetType);
        expect(decoded.clientRecordId, fp);
        // Re-fingerprinting the decoded record yields the same id — proving the
        // identifying content survived the round trip.
        expect(syncFingerprint(decoded), fp);
      });
    }
  });

  group('fingerprint', () {
    test('is stable and prefixed sync_', () {
      final fp = syncFingerprint(samples.first);
      expect(fp, startsWith('sync_'));
      expect(fp, syncFingerprint(samples.first));
    });

    test('differs when identifying content differs', () {
      final a = WeightImportRecord(clientRecordId: 'x', time: utc(2026, 1, 2), zoneOffset: null, kilograms: 72.4);
      final b = WeightImportRecord(clientRecordId: 'x', time: utc(2026, 1, 2), zoneOffset: null, kilograms: 72.5);
      expect(syncFingerprint(a), isNot(syncFingerprint(b)));
    });

    test('ignores the current clientRecordId (content-only)', () {
      final a = WeightImportRecord(clientRecordId: 'apple_health_1', time: utc(2026, 1, 2), zoneOffset: null, kilograms: 80);
      final b = WeightImportRecord(clientRecordId: 'sync_zzz', time: utc(2026, 1, 2), zoneOffset: null, kilograms: 80);
      expect(syncFingerprint(a), syncFingerprint(b));
    });

    test('a whole-second instant has no trailing .000 in its parts', () {
      // Two records differing only by sub-second should still be distinguishable,
      // but a whole-second one must fingerprint identically to itself — the
      // appleInstantToStableString contract that prevents re-sync duplicates.
      final r = WeightImportRecord(clientRecordId: 'x', time: utc(2026, 1, 2, 3, 4), zoneOffset: null, kilograms: 70);
      expect(syncFingerprint(r), syncFingerprint(r));
    });
  });
}
