// The readiness screens' derivations, extracted from their `build` methods: the
// panel's confidence/stress/strain lines and capped factor list, and the
// training detail's verdict band, signal list and guidance bullets.

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/insights/daily_readiness.dart';
import 'package:openvitals/domain/insights/intensity_minutes.dart';
import 'package:openvitals/domain/insights/stress_tracking.dart';
import 'package:openvitals/features/readiness/application/daily_readiness_display.dart';
import 'package:openvitals/features/readiness/application/training_readiness_display.dart';

DailyReadinessInsight _insight({
  ReadinessState state = ReadinessState.ready,
  int trainingReadinessScore = 84,
  ReadinessConfidence confidence = ReadinessConfidence.high,
  String confidenceReason = 'complete_data',
  String? currentStrain = 'Current strain: 6.0',
  String strainTarget = "Today's strain target: 10-14",
  int? stressScore = 30,
  List<DailyReadinessFactor> factors = const [],
}) =>
    DailyReadinessInsight(
      state: state,
      score: trainingReadinessScore,
      bodyEnergyScore: 70,
      trainingReadinessScore: trainingReadinessScore,
      recommendationType: ReadinessRecommendationType.hardTraining,
      statusTitle: 'Ready to train',
      recommendation: 'Good day for hard training if you feel normal.',
      explanation: 'Signals look strong.',
      alternative: 'Reduce to easy cardio if sore.',
      suggestedWorkout: 'Strength training or intervals.',
      avoid: 'Overreaching if you feel sore.',
      strainTarget: strainTarget,
      currentStrain: currentStrain,
      adaptiveGoal: 'Adaptive goal: 8800 steps + workout',
      confidence: confidence,
      confidenceReason: confidenceReason,
      hrvStatus: const HrvStatusInsight(
        status: HrvStatus.balanced,
        label: 'Balanced',
        detail: 'HRV is near your usual baseline.',
        currentRmssdMs: 60,
        baselineRmssdMs: 60,
        percentFromBaseline: 0,
      ),
      intensityMinutes: const IntensityMinutesReadinessInsight(
        status: IntensityMinutesStatus.onTrack,
        label: 'On track',
        detail: '90/150 moderate-equivalent min this week.',
        moderateEquivalentMinutes: 90,
        targetMinutes: 150,
        todayModerateEquivalentMinutes: 20,
        progressPercent: 60,
        confidence: IntensityMinutesConfidence.high,
      ),
      physiologicalStress: PhysiologicalStressEstimate(
        level: PhysiologicalStressLevel.low,
        label: 'Low',
        score: stressScore,
        summary: 'Physiological stress looks low.',
        detail: 'Detail.',
        confidence: PhysiologicalStressConfidence.high,
        confidenceReason: 'complete_data',
        hrvPercentFromBaseline: 0,
        restingHeartRateDeltaBpm: 0,
        averageHeartRateDeltaFromRestingBpm: 0,
        hasWorkoutInfluence: false,
        contributingFactors: const [],
        dataCoverage: const [],
        caveats: const [],
      ),
      factors: factors,
      recoveryModeSuggested: false,
    );

DailyReadinessFactor _factor(
  ReadinessFactorKind kind, {
  String label = 'Label',
  String detail = 'Detail',
}) =>
    DailyReadinessFactor(
      kind: kind,
      label: label,
      detail: detail,
      impact: ReadinessFactorImpact.positive,
    );

void main() {
  group('daily readiness panel', () {
    test('composes every line the panel used to build inline', () {
      final display = buildDailyReadinessDisplay(_insight());

      expect(display.confidenceText, 'High confidence · complete local data');
      expect(
        display.hrvStatusValue,
        'Balanced · HRV is near your usual baseline.',
      );
      expect(
        display.intensityMinutesValue,
        'On track · 90/150 moderate-equivalent min this week.',
      );
      expect(
        display.stressValue,
        'Low · 30/100 · Physiological stress looks low.',
      );
      expect(
        display.strainValue,
        "Today's strain target: 10-14 · Current strain: 6.0",
      );
    });

    test('an empty insight still composes: no strain, no score, no factors', () {
      final display = buildDailyReadinessDisplay(
        _insight(
          state: ReadinessState.unknown,
          confidence: ReadinessConfidence.low,
          confidenceReason: 'missing_sleep_data',
          currentStrain: null,
          strainTarget: '',
          stressScore: null,
        ),
      );

      expect(display.confidenceText, 'Low confidence · sleep data missing');
      // No score means no "n/100" segment at all.
      expect(display.stressValue, 'Low · Physiological stress looks low.');
      expect(display.strainValue, isEmpty);
      expect(display.topFactors, isEmpty);
    });

    test('an unrecognised confidence reason falls back to partial data', () {
      final display = buildDailyReadinessDisplay(
        _insight(confidenceReason: 'something_else'),
      );
      expect(display.confidenceText, 'High confidence · partial local data');
    });

    test('the factor list is capped at five', () {
      final display = buildDailyReadinessDisplay(
        _insight(
          factors: [
            for (var i = 0; i < 8; i++)
              _factor(ReadinessFactorKind.hrvNormal, label: 'Factor $i'),
          ],
        ),
      );

      expect(display.topFactors, hasLength(5));
      expect(display.topFactors.first.label, 'Factor 0');
      expect(display.topFactors.last.label, 'Factor 4');
    });
  });

  group('training readiness detail', () {
    test('score, verdict, confidence, signals and guidance', () {
      final display = buildTrainingReadinessDisplay(
        _insight(
          factors: [
            _factor(
              ReadinessFactorKind.trainingLoadNormal,
              label: 'Training load is stable',
              detail: 'This week is 100% of your current load target.',
            ),
          ],
        ),
      );

      expect(display.score, 84);
      expect(display.verdict, 'Strong');
      expect(display.confidence, 'High confidence · complete local data');
      expect(display.signals, [
        'Training load is stable: This week is 100% of your current load '
            'target.',
      ]);
      expect(display.guidance, [
        'Recommended: Strength training or intervals.',
        'Avoid: Overreaching if you feel sore.',
        "Strain target: Today's strain target: 10-14 · Current strain: 6.0",
      ]);
    });

    test('no training-side factors falls back to the no-signals message', () {
      // A recovery-side factor is not a training-side one, so it is filtered out
      // and the card must not pretend it was used.
      final display = buildTrainingReadinessDisplay(
        _insight(factors: [_factor(ReadinessFactorKind.hydrationLow)]),
      );

      expect(display.signals, [
        'No usable training-side signals were available.',
      ]);
    });

    test('an unknown state reads as needs-more-data, whatever the score', () {
      final display = buildTrainingReadinessDisplay(
        _insight(state: ReadinessState.unknown, trainingReadinessScore: 0),
      );

      expect(display.verdict, 'Needs more data');
      expect(display.score, 0);
    });

    test('the verdict bands', () {
      String verdictFor(int score) => buildTrainingReadinessDisplay(
            _insight(trainingReadinessScore: score),
          ).verdict;

      expect(verdictFor(80), 'Strong');
      expect(verdictFor(79), 'Steady');
      expect(verdictFor(60), 'Steady');
      expect(verdictFor(59), 'Limited');
      expect(verdictFor(40), 'Limited');
      expect(verdictFor(39), 'Low');
    });

    test('an empty strain drops the strain bullet entirely', () {
      final display = buildTrainingReadinessDisplay(
        _insight(strainTarget: '', currentStrain: null),
      );

      expect(display.guidance, hasLength(2));
      expect(display.guidance.last, startsWith('Avoid:'));
    });
  });
}
