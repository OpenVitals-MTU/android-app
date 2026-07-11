import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/daily_readiness.dart';
import 'package:openvitals/domain/insights/intensity_minutes.dart';
import 'package:openvitals/domain/insights/stress_tracking.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/readiness/training_readiness_details_screen.dart';
import 'package:openvitals/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/data_source_education_item.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

final LocalDate today = LocalDate.now();

DailyReadinessInsight _insight({
  int trainingReadinessScore = 84,
  ReadinessState state = ReadinessState.ready,
  ReadinessConfidence confidence = ReadinessConfidence.high,
  String confidenceReason = 'complete_data',
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
      strainTarget: "Today's strain target: 10-14",
      currentStrain: 'Current strain: 6.0',
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
      physiologicalStress: const PhysiologicalStressEstimate(
        level: PhysiologicalStressLevel.low,
        label: 'Low',
        score: 30,
        summary: 'Physiological stress looks low.',
        detail: 'Detail.',
        confidence: PhysiologicalStressConfidence.high,
        confidenceReason: 'complete_data',
        hrvPercentFromBaseline: 0,
        restingHeartRateDeltaBpm: 0,
        averageHeartRateDeltaFromRestingBpm: 0,
        hasWorkoutInfluence: false,
        contributingFactors: [],
        dataCoverage: [],
        caveats: [],
      ),
      factors: factors,
      recoveryModeSuggested: false,
    );

const _trainingLoadFactor = DailyReadinessFactor(
  kind: ReadinessFactorKind.trainingLoadNormal,
  label: 'Training load is stable',
  detail: 'This week is 100% of your current load target.',
  impact: ReadinessFactorImpact.positive,
);

Future<Widget> _bootstrap({
  required DailyReadinessInsight insight,
  String date = '',
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith(
        (ref) async => {HcPermissions.readHeartRate, HcPermissions.readSleep},
      ),
      trainingReadinessInsightProvider
          .overrideWith((ref, arg) async => insight),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: TrainingReadinessDetailsScreen(
        date: date.isEmpty ? '$today' : date,
      ),
    ),
  );
}

void main() {
  testWidgets('renders score, verdict, and training factor group',
      (tester) async {
    tester.view.physicalSize = const Size(1000, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      await _bootstrap(insight: _insight(factors: const [_trainingLoadFactor])),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Training Readiness'), findsWidgets);
    expect(find.text('84/100'), findsOneWidget);
    expect(find.text('Strong'), findsOneWidget);
    expect(find.textContaining('High confidence'), findsOneWidget);
    // Training-side factor appears in the "Signals used" card.
    expect(
      find.textContaining('Training load is stable'),
      findsOneWidget,
    );
    // Guidance card.
    expect(find.textContaining('Recommended:'), findsOneWidget);
    expect(find.textContaining('Avoid:'), findsOneWidget);
    expect(find.textContaining('Strain target:'), findsOneWidget);
    // The data-source education link closes the detail content.
    expect(find.byType(DataSourceEducationItem), findsOneWidget);
  });

  testWidgets('falls back to the no-signals message when factors are empty',
      (tester) async {
    await tester.pumpWidget(await _bootstrap(insight: _insight()));
    await tester.pumpAndSettle();

    expect(
      find.text('No usable training-side signals were available.'),
      findsOneWidget,
    );
  });

  testWidgets('shows the needs-more-data verdict for an unknown state',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        insight: _insight(
          trainingReadinessScore: 0,
          state: ReadinessState.unknown,
          confidence: ReadinessConfidence.low,
          confidenceReason: 'missing_sleep_data',
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Needs more data'), findsOneWidget);
    expect(find.textContaining('Low confidence'), findsOneWidget);
    expect(find.textContaining('sleep data missing'), findsOneWidget);
  });

  testWidgets('an invalid date argument falls back to today and still renders',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(insight: _insight(), date: 'not-a-date'),
    );
    await tester.pumpAndSettle();

    // Malformed arg must not throw; the screen renders for the fallback day.
    expect(tester.takeException(), isNull);
    expect(find.text('84/100'), findsOneWidget);
  });
}
