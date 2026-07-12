import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/features/activity/presentation/activity_metric_relevance.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';

/// The relevance table behind the activity detail screen's metric rows.
///
/// The screen's rule has two halves and this file only guards the second one:
/// whether the ABSENCE of a metric is worth reporting. A metric that HAS a value
/// is always shown regardless of what this table says — that half is guarded by
/// the widget test in activity_detail_metrics_test.dart.

const int _bikingStationary = 9;
const int _stairClimbing = 68;
const int _rowingMachine = 54;
const int _swimmingPool = 74;

void main() {
  group('cycling', () {
    const type = ExerciseSessionType.biking;

    test('reports the absence of the metrics a bike ride can produce', () {
      for (final metric in [
        ActivityMetric.distance,
        ActivityMetric.averageSpeed,
        ActivityMetric.cyclingCadence,
        ActivityMetric.elevationGained,
        ActivityMetric.averageHeartRate,
        ActivityMetric.caloriesBurned,
      ]) {
        expect(isMetricRelevant(metric, type), isTrue, reason: '$metric');
      }
    });

    test('stays silent about metrics a bike ride can never produce', () {
      // This is the bug the user reported: a ride advertising "Steps: Not
      // available", "Floors climbed: Not available", "Wheelchair pushes: Not
      // available" — three absences that were never going to be anything else.
      for (final metric in [
        ActivityMetric.steps,
        ActivityMetric.stepsCadence,
        ActivityMetric.floorsClimbed,
        ActivityMetric.wheelchairPushes,
        // A cyclist reads speed, not pace.
        ActivityMetric.averagePace,
      ]) {
        expect(isMetricRelevant(metric, type), isFalse, reason: '$metric');
      }
    });
  });

  test('sensor-only metrics earn a row by being recorded, never by being '
      'missing', () {
    // A power meter, a footpod, a bike computer's own speed average: hardware
    // most people do not own. "Average power: Not available" on every ride is
    // the same noise the fix exists to remove, so these are value-only for
    // EVERY type -- the screen still shows them the moment they carry a figure.
    for (final metric in [
      ActivityMetric.averagePower,
      ActivityMetric.stepsCadence,
      ActivityMetric.recordedSpeed,
    ]) {
      for (final type in [
        ExerciseSessionType.biking,
        ExerciseSessionType.running,
        ExerciseSessionType.walking,
        ExerciseSessionType.strengthTraining,
      ]) {
        expect(
          isMetricRelevant(metric, type),
          isFalse,
          reason: '$metric on type $type',
        );
      }
    }
  });

  group('running', () {
    const type = ExerciseSessionType.running;

    test('reports steps and pace', () {
      for (final metric in [
        ActivityMetric.steps,
        ActivityMetric.averagePace,
        ActivityMetric.distance,
        ActivityMetric.elevationGained,
      ]) {
        expect(isMetricRelevant(metric, type), isTrue, reason: '$metric');
      }
    });

    test('stays silent about the crank', () {
      expect(isMetricRelevant(ActivityMetric.cyclingCadence, type), isFalse);
      expect(isMetricRelevant(ActivityMetric.wheelchairPushes, type), isFalse);
      expect(isMetricRelevant(ActivityMetric.floorsClimbed, type), isFalse);
    });
  });

  test('indoor activities do not report a missing elevation gain', () {
    // The ground never rises on a treadmill, so "Elevation gained: Not
    // available" is noise; outdoors it is a real statement about the GPS.
    for (final indoor in [
      ExerciseSessionType.runningTreadmill,
      _bikingStationary,
      _rowingMachine,
      _swimmingPool,
    ]) {
      expect(
        isMetricRelevant(ActivityMetric.elevationGained, indoor),
        isFalse,
        reason: 'type $indoor',
      );
    }
    expect(
      isMetricRelevant(
        ActivityMetric.elevationGained,
        ExerciseSessionType.running,
      ),
      isTrue,
    );
  });

  test('floors climbed belongs to stair climbing and nothing else', () {
    expect(isMetricRelevant(ActivityMetric.floorsClimbed, _stairClimbing), isTrue);
    for (final other in [
      ExerciseSessionType.running,
      ExerciseSessionType.biking,
      ExerciseSessionType.wheelchair,
    ]) {
      expect(
        isMetricRelevant(ActivityMetric.floorsClimbed, other),
        isFalse,
        reason: 'type $other',
      );
    }
  });

  test('wheelchair pushes belong to wheelchair and nothing else', () {
    expect(
      isMetricRelevant(
        ActivityMetric.wheelchairPushes,
        ExerciseSessionType.wheelchair,
      ),
      isTrue,
    );
    expect(
      isMetricRelevant(ActivityMetric.steps, ExerciseSessionType.wheelchair),
      isFalse,
    );
  });

  test('a strength session reports none of the distance metrics', () {
    const type = ExerciseSessionType.strengthTraining;
    for (final metric in [
      ActivityMetric.distance,
      ActivityMetric.averagePace,
      ActivityMetric.steps,
      ActivityMetric.cyclingCadence,
      ActivityMetric.elevationGained,
    ]) {
      expect(isMetricRelevant(metric, type), isFalse, reason: '$metric');
    }
    // It still burns energy and still has a heart rate.
    expect(isMetricRelevant(ActivityMetric.averageHeartRate, type), isTrue);
    expect(isMetricRelevant(ActivityMetric.caloriesBurned, type), isTrue);
    expect(isMetricRelevant(ActivityMetric.duration, type), isTrue);
  });

  test('an unknown exercise type reports the universal absences and nothing '
      'invented', () {
    // A Health Connect constant this table has never seen — a new type, or an
    // activity imported from another app. It needs no special case: it reports
    // what every session has, shows anything it did record, and invents nothing.
    // The card can never be empty, because a session always has a duration.
    const unknown = 9999;
    for (final metric in [
      ActivityMetric.duration,
      ActivityMetric.averageHeartRate,
      ActivityMetric.caloriesBurned,
      ActivityMetric.activeCalories,
    ]) {
      expect(isMetricRelevant(metric, unknown), isTrue, reason: '$metric');
    }
    for (final metric in [
      ActivityMetric.distance,
      ActivityMetric.averagePace,
      ActivityMetric.steps,
      ActivityMetric.cyclingCadence,
      ActivityMetric.wheelchairPushes,
    ]) {
      expect(isMetricRelevant(metric, unknown), isFalse, reason: '$metric');
    }
  });
}
