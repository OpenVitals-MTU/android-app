import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/sleep_score.dart';
import 'package:openvitals/domain/insights/stress_tracking.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';

void main() {
  final date = LocalDate(2026, 6, 10);

  ExerciseData workout() {
    final start = date.atTimeInstant(7);
    final end = start.add(const Duration(minutes: 35));
    return ExerciseData(
      id: 'run',
      title: null,
      exerciseType: 0,
      startTime: start,
      endTime: end,
      durationMs: end.difference(start).inMilliseconds,
      source: 'test',
    );
  }

  test('lowHrvAndElevatedRestingHeartRateProduceHighStress', () {
    final estimate = calculatePhysiologicalStress(
      DashboardData(
        date: date,
        avgHeartRateBpm: 91,
        restingHeartRateBpm: 72,
        restingHeartRateBaselineBpm: 58,
        hrvRmssdMs: 32.0,
        hrvBaselineRmssdMs: 55.0,
        loadedMetrics: const {
          DashboardMetric.avgHeartRate,
          DashboardMetric.restingHeartRate,
          DashboardMetric.hrv,
        },
      ),
    );

    expect(estimate.level, PhysiologicalStressLevel.high);
    expect((estimate.score ?? 0) >= 76, isTrue);
    expect(estimate.confidence, PhysiologicalStressConfidence.high);
  });

  test('balancedHrvAndNormalRestingHeartRateProduceRestingStress', () {
    final estimate = calculatePhysiologicalStress(
      DashboardData(
        date: date,
        avgHeartRateBpm: 62,
        restingHeartRateBpm: 55,
        restingHeartRateBaselineBpm: 56,
        hrvRmssdMs: 58.0,
        hrvBaselineRmssdMs: 56.0,
        loadedMetrics: const {
          DashboardMetric.avgHeartRate,
          DashboardMetric.restingHeartRate,
          DashboardMetric.hrv,
        },
      ),
    );

    expect(estimate.level, PhysiologicalStressLevel.resting);
    expect(estimate.confidence, PhysiologicalStressConfidence.high);
  });

  test('workoutsLowerConfidenceAndAddActivityCaveat', () {
    final estimate = calculatePhysiologicalStress(
      DashboardData(
        date: date,
        avgHeartRateBpm: 82,
        restingHeartRateBpm: 58,
        restingHeartRateBaselineBpm: 58,
        hrvRmssdMs: 54.0,
        hrvBaselineRmssdMs: 56.0,
        workouts: [workout()],
        loadedMetrics: const {
          DashboardMetric.workout,
          DashboardMetric.avgHeartRate,
          DashboardMetric.restingHeartRate,
          DashboardMetric.hrv,
        },
      ),
    );

    expect(estimate.hasWorkoutInfluence, isTrue);
    expect(estimate.confidence, PhysiologicalStressConfidence.medium);
    expect(
      estimate.caveats.any((c) => c.toLowerCase().contains('workouts')),
      isTrue,
    );
  });

  test('noStressSignalsNeedMoreData', () {
    final estimate = calculatePhysiologicalStress(DashboardData(date: date));

    expect(estimate.level, PhysiologicalStressLevel.needsMoreData);
    expect(estimate.confidence, PhysiologicalStressConfidence.noData);
    expect(estimate.score, isNull);
  });

  test('oneHrvPointIsUsedButReportedAsThinCoverage', () {
    final estimate = calculatePhysiologicalStress(
      DashboardData(
        date: date,
        hrvRmssdMs: 46.0,
        hrvBaselineRmssdMs: 50.0,
        hrvSampleCount: 1,
        hrvSampleStartTime: date.atTimeInstant(9),
        hrvSampleEndTime: date.atTimeInstant(9),
        loadedMetrics: const {DashboardMetric.hrv},
      ),
    );

    expect(estimate.level, PhysiologicalStressLevel.low);
    expect(
      estimate.dataCoverage.any((c) => c.contains('1 RMSSD point')),
      isTrue,
    );
    expect(
      estimate.caveats.any((c) => c.contains('Only one HRV point')),
      isTrue,
    );
  });

  test('dayContextCanRaiseStressEstimateAroundHeartSignals', () {
    final estimate = calculatePhysiologicalStress(
      DashboardData(
        date: date,
        avgHeartRateBpm: 76,
        heartRateSampleCount: 4,
        heartRateSampleStartTime: date.atTimeInstant(6),
        heartRateSampleEndTime: date.atTimeInstant(10),
        restingHeartRateBpm: 62,
        restingHeartRateBaselineBpm: 58,
        hrvRmssdMs: 45.0,
        hrvBaselineRmssdMs: 50.0,
        hrvSampleCount: 2,
        hrvSampleStartTime: date.atTimeInstant(6, 10),
        hrvSampleEndTime: date.atTimeInstant(10, 10),
        sleepScore: const SleepScoreEstimate(
          score: 40,
          confidence: SleepScoreConfidence.medium,
          sleepDurationMinutes: 300.0,
        ),
        hydrationLiters: 0.2,
        latestSkinTemperatureDeltaCelsius: 0.7,
        loadedMetrics: const {
          DashboardMetric.avgHeartRate,
          DashboardMetric.restingHeartRate,
          DashboardMetric.hrv,
          DashboardMetric.sleep,
          DashboardMetric.hydration,
          DashboardMetric.skinTemperature,
        },
      ),
    );

    expect((estimate.score ?? 0) >= 70, isTrue);
    expect(
      estimate.contributingFactors.any((f) => f.contains('Sleep score is 40')),
      isTrue,
    );
    expect(
      estimate.contributingFactors.any((f) => f.contains('Hydration')),
      isTrue,
    );
    expect(
      estimate.contributingFactors.any((f) => f.contains('Temperature context')),
      isTrue,
    );
    expect(
      estimate.dataCoverage.any((c) => c.contains('Heart rate used 4 samples')),
      isTrue,
    );
    expect(
      estimate.dataCoverage.any((c) => c.contains('HRV used 2 RMSSD points')),
      isTrue,
    );
  });
}
