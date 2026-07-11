import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/sleep_models.dart';

final DateTime _epoch = DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);

ExerciseData _exercise({required int durationMs}) => ExerciseData(
      id: '1',
      title: null,
      exerciseType: 0,
      startTime: _epoch,
      endTime: _epoch,
      durationMs: durationMs,
      source: 'test',
    );

SleepData _sleep({required int durationMs}) => SleepData(
      id: '1',
      startTime: _epoch,
      endTime: _epoch,
      durationMs: durationMs,
      source: 'test',
    );

void main() {
  // ExerciseData.durationMinutes ------------------------------------------

  test('durationMinutes truncates sub-minute remainder', () {
    expect(_exercise(durationMs: 90000).durationMinutes, 1);
  });

  test('durationMinutes is zero for sub-minute duration', () {
    expect(_exercise(durationMs: 59999).durationMinutes, 0);
  });

  test('durationMinutes is exact for whole-minute duration', () {
    expect(_exercise(durationMs: 3600000).durationMinutes, 60);
  });

  // SleepData.durationHours -----------------------------------------------

  test('durationHours returns fractional hours', () {
    expect(_sleep(durationMs: 27000000).durationHours, closeTo(7.5, 0.001));
  });

  test('durationHours is zero for zero duration', () {
    expect(_sleep(durationMs: 0).durationHours, closeTo(0.0, 0.0));
  });

  // SleepStage.durationMs -------------------------------------------------

  test('SleepStage durationMs equals end minus start epoch millis', () {
    final stage = SleepStage(
      startTime: DateTime.fromMillisecondsSinceEpoch(1000000, isUtc: true),
      endTime: DateTime.fromMillisecondsSinceEpoch(2500000, isUtc: true),
      stageType: SleepStage.stageRem,
    );
    expect(stage.durationMs, 1500000);
  });

  // DailySteps optional fields --------------------------------------------

  test('DailySteps defaults all optional fields to null', () {
    final day = DailySteps(
      date: LocalDate(2026, 1, 1),
      steps: 1000,
      distanceMeters: 800.0,
    );
    expect(day.floorsClimbed, isNull);
    expect(day.activeCaloriesKcal, isNull);
    expect(day.elevationGainedMeters, isNull);
  });

  test('DailySteps stores all optional fields when provided', () {
    final day = DailySteps(
      date: LocalDate(2026, 1, 1),
      steps: 10000,
      distanceMeters: 7500.0,
      floorsClimbed: 15,
      activeCaloriesKcal: 420.5,
      elevationGainedMeters: 65.0,
    );
    expect(day.floorsClimbed, 15);
    expect(day.activeCaloriesKcal!, closeTo(420.5, 0.01));
    expect(day.elevationGainedMeters!, closeTo(65.0, 0.01));
  });

  // ActivityProgressPoint optional fields ---------------------------------

  test('ActivityProgressPoint defaults detailed optional fields to null', () {
    final point = ActivityProgressPoint(
      time: _epoch,
      totalSteps: 1000,
      totalDistanceMeters: null,
      totalCaloriesBurnedKcal: null,
    );

    expect(point.totalActiveCaloriesKcal, isNull);
    expect(point.totalFloorsClimbed, isNull);
    expect(point.totalElevationGainedMeters, isNull);
  });

  test('ActivityProgressPoint stores detailed optional fields', () {
    final point = ActivityProgressPoint(
      time: _epoch,
      totalSteps: 1000,
      totalDistanceMeters: 800.0,
      totalCaloriesBurnedKcal: 120.0,
      totalActiveCaloriesKcal: 80.0,
      totalFloorsClimbed: 4,
      totalElevationGainedMeters: 20.0,
    );

    expect(point.totalActiveCaloriesKcal!, closeTo(80.0, 0.01));
    expect(point.totalFloorsClimbed, 4);
    expect(point.totalElevationGainedMeters!, closeTo(20.0, 0.01));
  });

  // DashboardData ---------------------------------------------------------

  test('DashboardData defaults weight to null', () {
    final data = DashboardData(date: LocalDate(2026, 1, 1));
    expect(data.weightKg, isNull);
    expect(data.weightTime, isNull);
    expect(data.heightTime, isNull);
  });

  test('DashboardData stores latest weight with time when provided', () {
    final time = DateTime.parse('2026-01-01T08:00:00Z');
    final data = DashboardData(
      date: LocalDate(2026, 1, 1),
      weightKg: 74.2,
      weightTime: time,
    );
    expect(data.weightKg!, closeTo(74.2, 0.01));
    expect(data.weightTime, time);
  });

  test('DashboardData stores latest height with time when provided', () {
    final time = DateTime.parse('2026-01-02T08:00:00Z');
    final data = DashboardData(
      date: LocalDate(2026, 1, 1),
      heightCm: 178.0,
      heightTime: time,
    );
    expect(data.heightCm!, closeTo(178.0, 0.01));
    expect(data.heightTime, time);
  });

  test('DashboardData defaults floorsClimbed to null', () {
    final data = DashboardData(date: LocalDate(2026, 1, 1));
    expect(data.floorsClimbed, isNull);
  });

  test('DashboardData stores floorsClimbed when provided', () {
    final data = DashboardData(date: LocalDate(2026, 1, 1), floorsClimbed: 8);
    expect(data.floorsClimbed, 8);
  });

  test('DashboardData defaults elevationGainedMeters to null', () {
    final data = DashboardData(date: LocalDate(2026, 1, 1));
    expect(data.elevationGainedMeters, isNull);
  });

  test('DashboardData stores elevationGainedMeters when provided', () {
    final data = DashboardData(
      date: LocalDate(2026, 1, 1),
      elevationGainedMeters: 120.0,
    );
    expect(data.elevationGainedMeters!, closeTo(120.0, 0.01));
  });

  test('DailySteps floorsClimbed zero is non-null — permission granted no data',
      () {
    final day = DailySteps(
      date: LocalDate(2026, 1, 1),
      steps: 0,
      distanceMeters: 0.0,
      floorsClimbed: 0,
    );
    expect(day.floorsClimbed, 0);
  });

  test(
      'DailySteps elevationGainedMeters zero is non-null — permission granted no data',
      () {
    final day = DailySteps(
      date: LocalDate(2026, 1, 1),
      steps: 0,
      distanceMeters: 0.0,
      elevationGainedMeters: 0.0,
    );
    expect(day.elevationGainedMeters!, closeTo(0.0, 0.0));
  });
}
