import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/activity/activities_screen.dart';
import 'package:openvitals/features/activity/activity_metric_screen.dart';
import 'package:openvitals/features/activity/calories_screen.dart';
import 'package:openvitals/features/activity/cardio_load_detail_screen.dart';
import 'package:openvitals/features/body/body_screen.dart';
import 'package:openvitals/features/caffeine/caffeine_screen.dart';
import 'package:openvitals/features/cycle/cycle_screen.dart';
import 'package:openvitals/features/dashboard/metric_screen.dart';
import 'package:openvitals/features/heart/heart_metric_screen.dart';
import 'package:openvitals/features/hydration/hydration_screen.dart';
import 'package:openvitals/features/mindfulness/mindfulness_screen.dart';
import 'package:openvitals/features/nutrition/nutrition_metric_screen.dart';
import 'package:openvitals/features/sleep/sleep_screen.dart';
import 'package:openvitals/navigation/app_router.dart';

/// Pins the `/metric/:metricId` dispatch to the Kotlin `MetricRouteContent`
/// precedence: the calories and body AGGREGATES intercept their ids before the
/// per-metric activity/body screens can claim them, and `workout` renders the
/// activities aggregate.
void main() {
  test('calories ids land on the calories aggregate, not the activity screen',
      () {
    expect(metricScreenFor('CALORIES_OUT'), isA<CaloriesScreen>());
    expect(metricScreenFor('ACTIVE_CALORIES'), isA<CaloriesScreen>());
    expect(metricScreenFor('BMR'), isA<CaloriesScreen>());
  });

  test('body ids land on the body aggregate, not a per-metric screen', () {
    for (final id in [
      'WEIGHT',
      'HEIGHT',
      'BMI',
      'FFMI',
      'BODY_FAT',
      'LEAN_MASS',
      'BONE_MASS',
      'BODY_WATER_MASS',
    ]) {
      expect(metricScreenFor(id), isA<BodyScreen>(), reason: id);
    }
  });

  test('nutrition ids land on the per-metric nutrition screen', () {
    for (final id in ['CALORIES_IN', 'PROTEIN', 'CARBS', 'FAT']) {
      expect(metricScreenFor(id), isA<NutritionMetricScreen>(), reason: id);
    }
  });

  test('movement ids land on the activity metric screen', () {
    for (final id in [
      'STEPS',
      'DISTANCE',
      'FLOORS',
      'ELEVATION',
      'WHEELCHAIR_PUSHES',
    ]) {
      expect(metricScreenFor(id), isA<ActivityMetricScreen>(), reason: id);
    }
  });

  test('heart and vitals ids land on the heart metric screen', () {
    for (final id in [
      'AVG_HEART_RATE',
      'RESTING_HEART_RATE',
      'HRV',
      'BLOOD_PRESSURE',
      'SPO2',
      'VO2_MAX',
      'RESPIRATORY_RATE',
      'BODY_TEMPERATURE',
      'BLOOD_GLUCOSE',
      'SKIN_TEMPERATURE',
    ]) {
      expect(metricScreenFor(id), isA<HeartMetricScreen>(), reason: id);
    }
  });

  test('explicit tail: workout/sleep/hydration/caffeine/mindfulness/cycle', () {
    expect(metricScreenFor('WORKOUT'), isA<ActivitiesScreen>());
    expect(metricScreenFor('SLEEP'), isA<SleepScreen>());
    expect(metricScreenFor('HYDRATION'), isA<HydrationScreen>());
    expect(metricScreenFor('CAFFEINE'), isA<CaffeineScreen>());
    expect(metricScreenFor('MINDFULNESS'), isA<MindfulnessScreen>());
    expect(metricScreenFor('CYCLE'), isA<CycleScreen>());
    expect(metricScreenFor('WEEKLY_CARDIO_LOAD'), isA<CardioLoadDetailScreen>());
    expect(metricScreenFor('CARDIO_LOAD'), isA<CardioLoadDetailScreen>());
  });

  test('unknown ids fall back to the generic metric placeholder', () {
    expect(metricScreenFor('NOT_A_METRIC'), isA<MetricScreen>());
    expect(metricScreenFor(null), isA<MetricScreen>());
  });
}
