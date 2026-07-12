import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/sleep_score.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/features/recovery/application/recovery_detail_view_model.dart';
import 'package:openvitals/features/recovery/presentation/sleep_score_detail_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// Serves a fixed [RecoveryDetailState] so the cards render deterministic
/// numbers (the estimate is constructed directly, not recomputed).
class _FixedRecoveryDetailNotifier extends RecoveryDetailViewModel {
  _FixedRecoveryDetailNotifier(this._fixed);

  final RecoveryDetailState _fixed;

  @override
  RecoveryDetailState build() => _fixed;
}

const _estimate = SleepScoreEstimate(
  score: 82,
  confidence: SleepScoreConfidence.high,
  durationPoints: 30.0,
  efficiencyPoints: 25.5,
  continuityPoints: 15.0,
  regularityPoints: 11.5,
  sleepDurationMinutes: 450.0,
  timeInBedMinutes: 480.0,
  sleepEfficiencyPercent: 93.75,
  wakeAfterSleepOnsetMinutes: 20.0,
  regularityDifferenceMinutes: 15.0,
  regularityBaselineNights: 5,
  sleepStageCount: 12,
  usesSleepStages: true,
  usesExplicitAwakeStages: true,
);

RecoveryDetailState _state({SleepScoreEstimate estimate = _estimate}) {
  final today = LocalDate.now();
  final end = DateTime(today.year, today.month, today.day, 6, 30).toUtc();
  final session = SleepData(
    id: 's1',
    startTime: end.subtract(const Duration(hours: 8)),
    endTime: end,
    durationMs: const Duration(hours: 8).inMilliseconds,
    source: 'com.test.tracker',
  );
  return RecoveryDetailState(
    isLoading: false,
    selectedDate: today,
    days: [
      RecoveryDay(date: today, sessions: [session], sleepScore: estimate),
    ],
  );
}

Future<Widget> _bootstrap(RecoveryDetailState state) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      recoveryDetailProvider
          .overrideWith(() => _FixedRecoveryDetailNotifier(state)),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: const SleepScoreDetailScreen(),
    ),
  );
}

void main() {
  testWidgets('renders the four sleep-score cards from a fixed estimate',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 6000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(await _bootstrap(_state()));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);

    // 1. Summary: today's score, confidence and the not-diagnostic note.
    expect(find.text('Today'), findsOneWidget);
    expect(find.text('82'), findsOneWidget);
    expect(find.text('High confidence'), findsOneWidget);
    expect(find.textContaining('not a diagnosis'), findsOneWidget);

    // 2. Calculation explanation with the show/hide toggle.
    expect(find.text('How it is calculated'), findsOneWidget);
    expect(find.text('Show calculation'), findsOneWidget);

    // 3. The day's numbers: component points, durations, efficiency, WASO,
    //    regularity, baseline nights, stage records and the data-quality note.
    expect(find.text('Today values'), findsOneWidget);
    expect(find.text('Duration'), findsOneWidget);
    expect(find.text('30.0'), findsOneWidget);
    expect(find.text('25.5'), findsOneWidget);
    expect(find.text('Continuity'), findsOneWidget);
    expect(find.text('Regularity'), findsOneWidget);
    expect(find.text('7h 30m'), findsOneWidget); // total sleep
    expect(find.text('8h 00m'), findsOneWidget); // time in bed
    expect(find.text('94'), findsOneWidget); // efficiency percent
    expect(find.text('20'), findsOneWidget); // WASO minutes
    expect(find.text('15'), findsOneWidget); // timing difference
    expect(find.text('Baseline nights'), findsOneWidget);
    expect(find.text('Stage records'), findsOneWidget);
    expect(find.text('12'), findsOneWidget);
    expect(find.text('Sleep schedule'), findsOneWidget);
    expect(
      find.text('Uses sleep stages and awake stages from Health Connect.'),
      findsOneWidget,
    );

    // 4. References: four tappable outlined buttons, each with the open-in-new
    //    icon and its title as the label.
    expect(find.text('Backed links'), findsOneWidget);
    expect(find.text('AASM adult sleep duration'), findsOneWidget);
    expect(find.text('Multidimensional sleep health'), findsOneWidget);
    expect(find.text('Sleep efficiency definition'), findsOneWidget);
    expect(find.text('Sleep regularity research'), findsOneWidget);
    expect(
      find.widgetWithIcon(OutlinedButton, Icons.open_in_new),
      findsNWidgets(4),
    );
  });

  testWidgets('tapping a reference button opens the link without throwing',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 6000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(await _bootstrap(_state()));
    await tester.pumpAndSettle();

    // No url_launcher plugin in the test host: openExternalUrl swallows the
    // failure and shows the fallback SnackBar rather than throwing.
    await tester.tap(find.widgetWithText(OutlinedButton, 'AASM adult sleep duration'));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });

  testWidgets('the calculation card expands and collapses', (tester) async {
    tester.view.physicalSize = const Size(1200, 6000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(await _bootstrap(_state()));
    await tester.pumpAndSettle();

    expect(find.textContaining('Sleep score = duration 35'), findsNothing);

    await tester.tap(find.text('Show calculation'));
    await tester.pumpAndSettle();
    expect(find.text('Hide calculation'), findsOneWidget);
    expect(find.textContaining('Sleep score = duration 35'), findsOneWidget);

    await tester.tap(find.text('Hide calculation'));
    await tester.pumpAndSettle();
    expect(find.text('Show calculation'), findsOneWidget);
    expect(find.textContaining('Sleep score = duration 35'), findsNothing);
  });

  testWidgets('a no-data day renders the no-data placeholders', (tester) async {
    tester.view.physicalSize = const Size(1200, 6000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final today = LocalDate.now();
    await tester.pumpWidget(
      await _bootstrap(
        RecoveryDetailState(
          isLoading: false,
          selectedDate: today,
          days: [RecoveryDay(date: today)],
        ),
      ),
    );
    await tester.pumpAndSettle();

    // Score headline, timing difference and sleep schedule all fall back.
    expect(find.text('No data'), findsWidgets);
    expect(
      find.text('Insufficient sleep data for a score.'),
      findsOneWidget,
    );
  });
}
