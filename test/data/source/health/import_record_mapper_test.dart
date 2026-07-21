import 'package:flutter_test/flutter_test.dart';
import 'package:health_connect_native/health_connect_native.dart';
import 'package:openvitals/data/source/health/native/import_record_mapper.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';

DateTime utc(int d, [int h = 0]) => DateTime.utc(2026, 1, d, h);

/// One record per structural family, to exercise both mapper directions.
final samples = <ImportRecord>[
  StepsImportRecord(clientRecordId: 'a', startTime: utc(1, 8), startZoneOffset: const Duration(hours: 1), endTime: utc(1, 9), endZoneOffset: const Duration(hours: 1), count: 1200),
  DistanceImportRecord(clientRecordId: 'a', startTime: utc(1, 8), startZoneOffset: null, endTime: utc(1, 9), endZoneOffset: null, meters: 3200.5),
  WeightImportRecord(clientRecordId: 'a', time: utc(2), zoneOffset: null, kilograms: 72.4),
  HeartRateImportRecord(clientRecordId: 'a', startTime: utc(3, 6), startZoneOffset: null, endTime: utc(3, 7), endZoneOffset: null, samples: [HeartRateSampleValue(utc(3, 6), 60), HeartRateSampleValue(utc(3, 7), 66)]),
  SleepSessionImportRecord(clientRecordId: 'a', startTime: utc(4, 23), startZoneOffset: null, endTime: utc(5, 7), endZoneOffset: null, title: 'Night', stages: [SleepStageValue(startTime: utc(4, 23), endTime: utc(5, 1), stage: SleepStageType.light), SleepStageValue(startTime: utc(5, 1), endTime: utc(5, 3), stage: SleepStageType.deep)]),
  NutritionImportRecord(clientRecordId: 'a', startTime: utc(6, 12), startZoneOffset: null, endTime: utc(6, 12), endZoneOffset: null, name: 'Lunch', nutrientGrams: const {'protein': 30.5, 'totalCarbohydrate': 45}, energyKilocalories: 600),
  ExerciseSessionImportRecord(clientRecordId: 'a', startTime: utc(7, 18), startZoneOffset: null, endTime: utc(7, 19), endZoneOffset: null, exerciseType: ImportExerciseType.running, title: 'Run', route: ExerciseRoute([ExerciseRouteLocation(time: utc(7, 18), latitude: 41.1, longitude: 2.1, altitudeMeters: 12)])),
  CervicalMucusImportRecord(clientRecordId: 'a', time: utc(8), zoneOffset: null, appearance: CervicalMucusAppearance.eggWhite, sensation: CervicalMucusSensation.medium),
  BloodPressureImportRecord(clientRecordId: 'a', time: utc(9), zoneOffset: null, systolicMmHg: 118, diastolicMmHg: 76),
  OvulationTestImportRecord(clientRecordId: 'a', time: utc(10), zoneOffset: null, result: OvulationResultType.positive),
  HydrationImportRecord(clientRecordId: 'a', startTime: utc(11, 9), startZoneOffset: null, endTime: utc(11, 9), endZoneOffset: null, milliliters: 350),
  TotalCaloriesBurnedImportRecord(clientRecordId: 'a', startTime: utc(12), startZoneOffset: null, endTime: utc(12, 23), endZoneOffset: null, kilocalories: 2100),
  PowerImportRecord(clientRecordId: 'a', startTime: utc(13, 6), startZoneOffset: null, endTime: utc(13, 7), endZoneOffset: null, samples: [PowerSampleValue(utc(13, 6), 200), PowerSampleValue(utc(13, 7), 180)]),
  SkinTemperatureImportRecord(clientRecordId: 'a', startTime: utc(14, 2), startZoneOffset: null, endTime: utc(14, 7), endZoneOffset: null, baselineCelsius: 33.2, measurementLocation: 1, deltas: [SkinTemperatureDeltaValue(utc(14, 2), 0.3)]),
  MenstruationPeriodImportRecord(clientRecordId: 'a', startTime: utc(15), startZoneOffset: null, endTime: utc(19), endZoneOffset: null),
];

void main() {
  group('importRecordMsg <-> importRecordFromMsg round-trip', () {
    for (final original in samples) {
      test('${original.targetType} survives a round trip', () {
        final msg = importRecordMsg(original);
        final back = importRecordFromMsg(msg);
        expect(back, isNotNull);
        expect(back!.targetType, original.targetType);
        expect(back.clientRecordId, original.clientRecordId);
        // Field-level spot checks per family.
        switch (back) {
          case StepsImportRecord r:
            expect(r.count, 1200);
          case DistanceImportRecord r:
            expect(r.meters, closeTo(3200.5, 1e-9));
          case WeightImportRecord r:
            expect(r.kilograms, closeTo(72.4, 1e-9));
          case HeartRateImportRecord r:
            expect(r.samples.map((s) => s.beatsPerMinute), [60, 66]);
          case SleepSessionImportRecord r:
            expect(r.stages.map((s) => s.stage), [SleepStageType.light, SleepStageType.deep]);
            expect(r.title, 'Night');
          case NutritionImportRecord r:
            expect(r.nutrientGrams['protein'], closeTo(30.5, 1e-9));
            expect(r.energyKilocalories, closeTo(600, 1e-9));
          case ExerciseSessionImportRecord r:
            expect(r.exerciseType, ImportExerciseType.running);
            expect(r.route?.route.single.latitude, closeTo(41.1, 1e-9));
          case CervicalMucusImportRecord r:
            expect(r.appearance, CervicalMucusAppearance.eggWhite);
            expect(r.sensation, CervicalMucusSensation.medium);
          case BloodPressureImportRecord r:
            expect(r.systolicMmHg, closeTo(118, 1e-9));
          case OvulationTestImportRecord r:
            expect(r.result, OvulationResultType.positive);
          case HydrationImportRecord r:
            expect(r.milliliters, closeTo(350, 1e-6));
          case TotalCaloriesBurnedImportRecord r:
            expect(r.kilocalories, closeTo(2100, 1e-9));
          case PowerImportRecord r:
            expect(r.samples.map((s) => s.watts), [200, 180]);
          case SkinTemperatureImportRecord r:
            expect(r.baselineCelsius, closeTo(33.2, 1e-9));
            expect(r.deltas.single.deltaCelsius, closeTo(0.3, 1e-9));
          case MenstruationPeriodImportRecord r:
            expect(r.endTime, utc(19));
          default:
        }
      });
    }
  });
}
