import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/cardio_load.dart';
import 'package:openvitals/domain/insights/daily_readiness.dart';
import 'package:openvitals/domain/insights/intensity_minutes.dart';
import 'package:openvitals/domain/insights/sleep_score.dart';
import 'package:openvitals/domain/insights/stress_tracking.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/sleep_models.dart';

void main() {
  final date = LocalDate(2026, 6, 10);

  SleepData sleep(Duration duration) {
    final start = date.atTimeInstant(0);
    return SleepData(
      id: 'sleep',
      startTime: start,
      endTime: start.add(duration),
      durationMs: duration.inMilliseconds,
      source: 'test',
    );
  }

  SleepScoreEstimate sleepScore({required int score, required double hours}) =>
      SleepScoreEstimate(
        score: score,
        confidence: SleepScoreConfidence.high,
        sleepDurationMinutes: hours * 60.0,
        timeInBedMinutes: hours * 60.0,
      );

  DashboardWeeklyCardioLoad weeklyLoad({
    required int current,
    required int target,
    required int today,
  }) =>
      DashboardWeeklyCardioLoad(
        currentScore: current,
        targetScore: target,
        todayScore: today,
        confidence: CardioLoadConfidence.high,
        targetSource: DashboardWeeklyCardioLoadTargetSource.recentHistory,
      );

  DashboardWeeklyIntensityMinutes weeklyIntensity({
    required int minutes,
    required int today,
    int daysElapsed = 7,
    IntensityMinutesConfidence confidence = IntensityMinutesConfidence.high,
  }) =>
      DashboardWeeklyIntensityMinutes(
        moderateMinutes: minutes,
        vigorousMinutes: 0,
        moderateEquivalentMinutes: minutes,
        todayModerateEquivalentMinutes: today,
        daysElapsed: daysElapsed,
        confidence: confidence,
      );

  test('readyWhenSleepAndRecoverySignalsAreStrong', () {
    final insight = calculateDailyReadiness(
      DashboardData(
        date: date,
        sleep: sleep(const Duration(hours: 8)),
        sleepScore: sleepScore(score: 88, hours: 8.0),
        restingHeartRateBpm: 55,
        restingHeartRateBaselineBpm: 58,
        hrvRmssdMs: 62.0,
        hrvBaselineRmssdMs: 56.0,
        avgHeartRateBpm: 68,
        weeklyCardioLoad: weeklyLoad(current: 90, target: 100, today: 12),
        weeklyIntensityMinutes: weeklyIntensity(minutes: 160, today: 34),
        mindfulnessMinutes: 10,
        loadedMetrics: const {
          DashboardMetric.sleep,
          DashboardMetric.avgHeartRate,
          DashboardMetric.restingHeartRate,
          DashboardMetric.hrv,
          DashboardMetric.weeklyCardioLoad,
          DashboardMetric.intensityMinutes,
          DashboardMetric.mindfulness,
        },
      ),
    );

    expect(insight.state, ReadinessState.ready);
    expect(insight.recommendationType, ReadinessRecommendationType.hardTraining);
    expect(insight.confidence, ReadinessConfidence.high);
    expect(insight.hrvStatus.status, HrvStatus.balanced);
    expect(insight.intensityMinutes.status, IntensityMinutesStatus.goalMet);
    expect(insight.physiologicalStress.level, PhysiologicalStressLevel.resting);
    expect(insight.score >= 80, isTrue);
    expect(
      insight.factors.any((f) => f.kind == ReadinessFactorKind.hrvNormal),
      isTrue,
    );
    expect(
      insight.factors
          .any((f) => f.kind == ReadinessFactorKind.intensityMinutesOnTarget),
      isTrue,
    );
    expect(
      insight.factors.any((f) => f.kind == ReadinessFactorKind.restingHrNormal),
      isTrue,
    );
  });

  test('recoveryDayWhenSleepHrvAndRestingHeartRateArePoor', () {
    final insight = calculateDailyReadiness(
      DashboardData(
        date: date,
        sleep: sleep(const Duration(hours: 5)),
        sleepScore: sleepScore(score: 36, hours: 5.0),
        restingHeartRateBpm: 68,
        restingHeartRateBaselineBpm: 58,
        hrvRmssdMs: 35.0,
        hrvBaselineRmssdMs: 55.0,
        avgHeartRateBpm: 94,
        weeklyCardioLoad: weeklyLoad(current: 145, target: 100, today: 20),
        loadedMetrics: const {
          DashboardMetric.sleep,
          DashboardMetric.avgHeartRate,
          DashboardMetric.restingHeartRate,
          DashboardMetric.hrv,
          DashboardMetric.weeklyCardioLoad,
        },
      ),
    );

    expect(insight.state, ReadinessState.rest);
    expect(insight.recommendationType, ReadinessRecommendationType.rest);
    expect(insight.hrvStatus.status, HrvStatus.unusuallyLow);
    expect(insight.physiologicalStress.level, PhysiologicalStressLevel.high);
    expect(insight.recoveryModeSuggested, isTrue);
    expect(insight.score < 40, isTrue);
    expect(
      insight.factors.any((f) => f.kind == ReadinessFactorKind.stressHigh),
      isTrue,
    );
    expect(insight.recommendation.contains('Avoid intense training'), isTrue);
  });

  test('hrvStatusUsesPersonalBaselineThresholds', () {
    expect(
      calculateHrvStatus(
        hrvRmssdMs: 51.0,
        baselineRmssdMs: 50.0,
        hasHrvData: true,
      ).status,
      HrvStatus.balanced,
    );
    expect(
      calculateHrvStatus(
        hrvRmssdMs: 42.0,
        baselineRmssdMs: 50.0,
        hasHrvData: true,
      ).status,
      HrvStatus.low,
    );
    expect(
      calculateHrvStatus(
        hrvRmssdMs: 34.0,
        baselineRmssdMs: 50.0,
        hasHrvData: true,
      ).status,
      HrvStatus.unusuallyLow,
    );
    expect(
      calculateHrvStatus(
        hrvRmssdMs: 58.0,
        baselineRmssdMs: 50.0,
        hasHrvData: true,
      ).status,
      HrvStatus.high,
    );
    expect(
      calculateHrvStatus(
        hrvRmssdMs: 66.0,
        baselineRmssdMs: 50.0,
        hasHrvData: true,
      ).status,
      HrvStatus.unusuallyHigh,
    );
    expect(
      calculateHrvStatus(
        hrvRmssdMs: null,
        baselineRmssdMs: 50.0,
        hasHrvData: true,
      ).status,
      HrvStatus.needsMoreHrv,
    );
    expect(
      calculateHrvStatus(
        hrvRmssdMs: 50.0,
        baselineRmssdMs: null,
        hasHrvData: true,
      ).status,
      HrvStatus.needsMoreHrv,
    );
    expect(
      calculateHrvStatus(
        hrvRmssdMs: 50.0,
        baselineRmssdMs: 50.0,
        hasHrvData: false,
      ).status,
      HrvStatus.needsMoreHrv,
    );
  });

  test('checkSymptomsWhenTemperatureSignalIsUnusual', () {
    final insight = calculateDailyReadiness(
      DashboardData(
        date: date,
        sleep: sleep(const Duration(hours: 6)),
        sleepScore: sleepScore(score: 55, hours: 6.0),
        restingHeartRateBpm: 67,
        restingHeartRateBaselineBpm: 58,
        hrvRmssdMs: 38.0,
        hrvBaselineRmssdMs: 55.0,
        latestBodyTemperatureCelsius: 38.0,
        loadedMetrics: const {
          DashboardMetric.sleep,
          DashboardMetric.restingHeartRate,
          DashboardMetric.hrv,
          DashboardMetric.bodyTemperature,
        },
      ),
    );

    expect(insight.state, ReadinessState.rest);
    expect(
      insight.recommendationType,
      ReadinessRecommendationType.checkSymptoms,
    );
    expect(
      insight.factors
          .any((f) => f.kind == ReadinessFactorKind.temperatureElevated),
      isTrue,
    );
    expect(insight.recommendation.contains('If you feel unwell'), isTrue);
  });

  test('intensityMinutesReadinessUsesWeeklyPace', () {
    expect(
      calculateIntensityMinutesReadiness(
        weeklyIntensityMinutes: weeklyIntensity(minutes: 151, today: 20),
        hasIntensityData: true,
      ).status,
      IntensityMinutesStatus.goalMet,
    );
    expect(
      calculateIntensityMinutesReadiness(
        weeklyIntensityMinutes:
            weeklyIntensity(minutes: 70, today: 10, daysElapsed: 3),
        hasIntensityData: true,
      ).status,
      IntensityMinutesStatus.onTrack,
    );
    expect(
      calculateIntensityMinutesReadiness(
        weeklyIntensityMinutes:
            weeklyIntensity(minutes: 90, today: 0, daysElapsed: 6),
        hasIntensityData: true,
      ).status,
      IntensityMinutesStatus.behind,
    );
    expect(
      calculateIntensityMinutesReadiness(
        weeklyIntensityMinutes: null,
        hasIntensityData: true,
      ).status,
      IntensityMinutesStatus.needsMoreData,
    );
  });

  test('unknownWhenNoSignalsAreAvailable', () {
    final insight = calculateDailyReadiness(DashboardData(date: date));

    expect(insight.state, ReadinessState.unknown);
    expect(insight.confidence, ReadinessConfidence.low);
    expect(insight.score, 0);
    expect(insight.explanation.contains('not enough local data'), isTrue);
  });

  test('explanationJoinsFactorDetailsWithoutDoublePunctuation', () {
    final insight = calculateDailyReadiness(
      DashboardData(
        date: date,
        sleep: sleep(const Duration(hours: 5)),
        sleepScore: sleepScore(score: 36, hours: 5.0),
        restingHeartRateBpm: 68,
        restingHeartRateBaselineBpm: 58,
        hrvRmssdMs: 35.0,
        hrvBaselineRmssdMs: 55.0,
        avgHeartRateBpm: 94,
        weeklyCardioLoad: weeklyLoad(current: 145, target: 100, today: 20),
        loadedMetrics: const {
          DashboardMetric.sleep,
          DashboardMetric.avgHeartRate,
          DashboardMetric.restingHeartRate,
          DashboardMetric.hrv,
          DashboardMetric.weeklyCardioLoad,
        },
      ),
    );

    expect(insight.explanation.contains('., and'), isFalse);
    expect(insight.explanation.endsWith('.'), isTrue);
  });

  test('nutritionFactorNotShownWhenOnlyHydrationIsLogged', () {
    final insight = calculateDailyReadiness(
      DashboardData(
        date: date,
        hydrationLiters: 1.5,
        proteinGrams: 0.0,
        carbsGrams: 0.0,
        fatGrams: 0.0,
        loadedMetrics: const {
          DashboardMetric.hydration,
          DashboardMetric.protein,
          DashboardMetric.carbs,
          DashboardMetric.fat,
        },
      ),
      goals: const DailyReadinessGoalInputs(hydrationLitersGoal: 2.0),
    );

    expect(
      insight.factors.any((f) => f.kind == ReadinessFactorKind.nutritionLogged),
      isFalse,
    );
  });

  test('nutritionFactorShownWhenMealDataIsPresent', () {
    final insight = calculateDailyReadiness(
      DashboardData(
        date: date,
        caloriesInKcal: 1800.0,
        loadedMetrics: const {DashboardMetric.caloriesIn},
      ),
    );

    expect(
      insight.factors.any((f) => f.kind == ReadinessFactorKind.nutritionLogged),
      isTrue,
    );
  });

  test('lowConfidenceWhenBaselinesAreMissing', () {
    final insight = calculateDailyReadiness(
      DashboardData(
        date: date,
        sleep: sleep(const Duration(hours: 7)),
        sleepScore: sleepScore(score: 76, hours: 7.0),
        restingHeartRateBpm: 56,
        hrvRmssdMs: 50.0,
        loadedMetrics: const {
          DashboardMetric.sleep,
          DashboardMetric.restingHeartRate,
          DashboardMetric.hrv,
        },
      ),
    );

    expect(insight.confidence, ReadinessConfidence.low);
    expect(insight.confidenceReason, 'new_user_not_enough_baseline');
    expect(insight.hrvStatus.status, HrvStatus.needsMoreHrv);
    expect(
      insight.factors
          .any((f) => f.kind == ReadinessFactorKind.newUserNotEnoughBaseline),
      isTrue,
    );
  });
}
