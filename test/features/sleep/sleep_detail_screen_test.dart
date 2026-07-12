import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/repository/contract/sleep_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/features/sleep/presentation/sleep_detail_screen.dart';
import 'package:openvitals/features/sleep/presentation/sleep_stage_chart.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// A fake [SleepRepository] serving one canned session by id.
class _FakeSleepRepository implements SleepRepository {
  _FakeSleepRepository({this.session});

  final SleepData? session;

  @override
  Future<SleepData?> loadSleepSession(String id) async =>
      session?.id == id ? session : null;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

SleepData _session() {
  final end = DateTime.utc(2026, 7, 8, 6, 0);
  final start = end.subtract(const Duration(hours: 8));
  return SleepData(
    id: 's1',
    startTime: start,
    endTime: end,
    durationMs: const Duration(hours: 8).inMilliseconds,
    source: 'com.test.tracker',
    title: 'Night sleep',
    notes: 'Slept well',
    startZoneOffset: const Duration(hours: 2),
    endZoneOffset: const Duration(hours: 2),
    recordingMethod: 2,
    device: const SleepDeviceData(
      type: 1,
      manufacturer: 'Acme',
      model: 'Watch 5',
    ),
    stages: [
      SleepStage(
        startTime: start,
        endTime: start.add(const Duration(hours: 4)),
        stageType: SleepStage.stageLight,
      ),
      SleepStage(
        startTime: start.add(const Duration(hours: 4)),
        endTime: start.add(const Duration(hours: 6)),
        stageType: SleepStage.stageDeep,
      ),
      SleepStage(
        startTime: start.add(const Duration(hours: 6)),
        endTime: start.add(const Duration(hours: 7, minutes: 30)),
        stageType: SleepStage.stageRem,
      ),
      SleepStage(
        startTime: start.add(const Duration(hours: 7, minutes: 30)),
        endTime: end,
        stageType: SleepStage.stageAwake,
      ),
    ],
  );
}

Future<Widget> _bootstrap({
  required _FakeSleepRepository sleepRepository,
  String sleepId = 's1',
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      sleepRepositoryProvider.overrideWithValue(sleepRepository),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: SleepDetailScreen(sleepId: sleepId),
    ),
  );
}

void main() {
  testWidgets('renders summary, breakdown, details and stage events',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 6000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      await _bootstrap(
        sleepRepository: _FakeSleepRepository(session: _session()),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);

    // Summary card: session title, duration headline, source chip.
    expect(find.byType(SleepSummaryCard), findsOneWidget);
    expect(find.text('Night sleep'), findsWidgets);
    expect(find.text('8h 00m'), findsWidgets);

    // Stage breakdown: the lane chart plus one totals row per stage type.
    expect(find.byType(SleepStageBreakdownCard), findsOneWidget);
    expect(find.byType(SleepStagesLaneChart), findsOneWidget);
    expect(find.text('Stages'), findsOneWidget);
    expect(find.textContaining('4h 00m · 50%'), findsOneWidget);

    // Session details rows straight from the record metadata.
    expect(find.text('Session details'), findsOneWidget);
    expect(find.text('Automatically recorded'), findsOneWidget);
    expect(find.text('com.test.tracker'), findsWidgets);
    expect(find.text('Watch'), findsOneWidget);
    expect(find.text('Acme'), findsOneWidget);
    expect(find.text('Watch 5'), findsOneWidget);
    expect(find.text('+02:00'), findsNWidgets(2));
    expect(find.text('Slept well'), findsOneWidget);

    // Stage events: the count summary and one row per stage.
    expect(find.text('Stage events'), findsOneWidget);
    expect(find.text('4 recorded stages'), findsOneWidget);
    expect(find.byType(SleepStageEventRow), findsNWidgets(4));
    expect(find.text('Deep'), findsWidgets);
    expect(find.text('REM'), findsWidgets);
  });

  testWidgets(
      'the stage lane chart paints the cross-lane connector without throwing',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 6000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    // _session()'s four stages are contiguous across the Light/Deep/REM/Awake
    // lanes, so the painter walks the `previous.end == stage.start` connector
    // branch (a diagonal lineTo) for every hop.
    await tester.pumpWidget(
      await _bootstrap(
        sleepRepository: _FakeSleepRepository(session: _session()),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    final chart = find.byType(SleepStagesLaneChart);
    expect(chart, findsOneWidget);
    // The single multi-lane painter is mounted under the chart.
    expect(
      find.descendant(of: chart, matching: find.byType(CustomPaint)),
      findsWidgets,
    );
  });

  testWidgets('shows the no-stages message when a session has no stages',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 4000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final end = DateTime.utc(2026, 7, 8, 6, 0);
    final session = SleepData(
      id: 's1',
      startTime: end.subtract(const Duration(hours: 7)),
      endTime: end,
      durationMs: const Duration(hours: 7).inMilliseconds,
      source: 'com.test.tracker',
    );
    await tester.pumpWidget(
      await _bootstrap(
        sleepRepository: _FakeSleepRepository(session: session),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('No stages recorded.'), findsOneWidget);
    expect(find.text('Stage events'), findsNothing);
    expect(find.byType(SleepStageEventRow), findsNothing);
    // A blank title falls back to the localized "Sleep session".
    expect(find.text('Sleep session'), findsOneWidget);
  });

  testWidgets('shows an error when the session is missing', (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        sleepRepository: _FakeSleepRepository(session: null),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Unknown error'), findsOneWidget);
    expect(find.byType(SleepSummaryCard), findsNothing);
  });
}
