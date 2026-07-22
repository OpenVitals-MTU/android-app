import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/settings/presentation/watch_data_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// Seeds the app's own watch-metric table, which is the only source this screen
/// reads — nothing here touches Health Connect by design.
Future<ProviderContainer> _containerWith(
  List<(GarminWellnessMetric, int)> samples, {
  DateTime? at,
}) async {
  final db = OpenVitalsDatabase(NativeDatabase.memory());
  addTearDown(db.close);
  final when = at ?? DateTime.now();
  await db.garminWellnessDao.upsertSamples([
    for (final (metric, value) in samples)
      GarminWellnessSamplesCompanion.insert(
        metric: metric.storageName,
        timeMillis: when.toUtc().millisecondsSinceEpoch,
        value: value,
      ),
  ]);
  final container = ProviderContainer(
    overrides: [openVitalsDatabaseProvider.overrideWithValue(db)],
  );
  addTearDown(container.dispose);
  return container;
}

Widget _harness(ProviderContainer container) => UncontrolledProviderScope(
      container: container,
      child: const MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: WatchDataScreen(deviceId: 'watch-1'),
      ),
    );

void main() {
  testWidgets('shows only the metrics the watch has actually sent',
      (tester) async {
    final container = await _containerWith([
      (GarminWellnessMetric.sleepScore, 71),
      (GarminWellnessMetric.sleepAwakeSeconds, 1020),
    ]);
    await tester.pumpWidget(_harness(container));
    await tester.pumpAndSettle();

    expect(find.text('71'), findsOneWidget);
    // 1020 SECONDS, not the minutes Garmin's own profile claims.
    expect(find.text('17 min'), findsOneWidget);
    // Nothing was sent for these, so they must not appear as blank rows —
    // permanent em-dashes teach people to stop reading the screen.
    expect(find.text('Stress'), findsNothing);
    expect(find.text('Recovery time'), findsNothing);
  });

  testWidgets('names what is missing once, at the foot', (tester) async {
    final container = await _containerWith([
      (GarminWellnessMetric.sleepScore, 71),
    ]);
    await tester.pumpWidget(_harness(container));
    await tester.pumpAndSettle();

    expect(
      find.textContaining('Not sent by this watch'),
      findsOneWidget,
    );
  });

  testWidgets('Sleep Coach reads as a comparison, not a bare number',
      (tester) async {
    final container = await _containerWith([
      (GarminWellnessMetric.sleepNeedMinutes, 520),
      (GarminWellnessMetric.sleepNeedNormalMinutes, 470),
    ]);
    await tester.pumpWidget(_harness(container));
    await tester.pumpAndSettle();

    expect(find.text('8h 40m'), findsOneWidget);
    // "8h 40m needed" alone says nothing; against the usual 7h 50m it says
    // what the day's strain cost.
    expect(find.textContaining('50 min above your usual 7h 50m'), findsOneWidget);
  });

  testWidgets('an empty table says so instead of rendering empty sections',
      (tester) async {
    final container = await _containerWith(const []);
    await tester.pumpWidget(_harness(container));
    await tester.pumpAndSettle();

    expect(find.textContaining('Nothing yet'), findsOneWidget);
  });

  testWidgets('vigorous intensity minutes count double, as Garmin counts them',
      (tester) async {
    final container = await _containerWith([
      (GarminWellnessMetric.moderateMinutes, 30),
      (GarminWellnessMetric.vigorousMinutes, 10),
    ]);
    await tester.pumpWidget(_harness(container));
    await tester.pumpAndSettle();

    expect(find.text('50'), findsOneWidget); // 30 + 2*10
  });
}
