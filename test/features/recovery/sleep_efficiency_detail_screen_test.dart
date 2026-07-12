import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/sleep_score.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/features/recovery/recovery_detail_view_model.dart';
import 'package:openvitals/features/recovery/sleep_efficiency_detail_screen.dart';
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
  sleepDurationMinutes: 450.0,
  timeInBedMinutes: 480.0,
  sleepEfficiencyPercent: 93.75,
  wakeAfterSleepOnsetMinutes: 20.0,
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
      home: const SleepEfficiencyDetailScreen(),
    ),
  );
}

void main() {
  testWidgets('renders the four sleep-efficiency cards from a fixed estimate',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 6000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(await _bootstrap(_state()));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);

    // 1. Summary: today's efficiency headline, confidence and the note.
    expect(find.text('Today'), findsOneWidget);
    expect(find.text('94'), findsNWidgets(2)); // headline + numbers tile
    expect(find.text('High confidence'), findsOneWidget);
    expect(find.textContaining('sleep-continuity signal'), findsOneWidget);

    // 2. Calculation explanation with the show/hide toggle.
    expect(find.text('How it is calculated'), findsOneWidget);
    expect(find.text('Show calculation'), findsOneWidget);

    // 3. The day's numbers: efficiency, durations, WASO, schedule, stage
    //    records and the data-quality note.
    expect(find.text('Today values'), findsOneWidget);
    expect(find.text('Efficiency'), findsOneWidget);
    expect(find.text('7h 30m'), findsOneWidget); // total sleep
    expect(find.text('8h 00m'), findsOneWidget); // time in bed
    expect(find.text('Wake after sleep'), findsOneWidget);
    expect(find.text('20'), findsOneWidget);
    expect(find.text('Sleep schedule'), findsOneWidget);
    expect(find.text('Stage records'), findsOneWidget);
    expect(find.text('12'), findsOneWidget);
    expect(
      find.text('Uses Health Connect sleep stages for total sleep time.'),
      findsOneWidget,
    );

    // 4. References: three tappable outlined buttons, each with the
    //    open-in-new icon and its title as the label.
    expect(find.text('Backed links'), findsOneWidget);
    expect(find.text('Sleep efficiency definition'), findsOneWidget);
    expect(
      find.text('Sleep efficiency denominator research'),
      findsOneWidget,
    );
    expect(find.text('Sleep assessment methods review'), findsOneWidget);
    expect(
      find.widgetWithIcon(OutlinedButton, Icons.open_in_new),
      findsNWidgets(3),
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
    await tester.tap(
      find.widgetWithText(OutlinedButton, 'Sleep efficiency definition'),
    );
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

    expect(
      find.textContaining('Sleep efficiency = total sleep time'),
      findsNothing,
    );

    await tester.tap(find.text('Show calculation'));
    await tester.pumpAndSettle();
    expect(find.text('Hide calculation'), findsOneWidget);
    expect(
      find.textContaining('Sleep efficiency = total sleep time'),
      findsOneWidget,
    );
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

    expect(find.text('No data'), findsWidgets);
    expect(
      find.text('Insufficient sleep data for efficiency.'),
      findsOneWidget,
    );
  });
}
