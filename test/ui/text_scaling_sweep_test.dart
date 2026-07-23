import 'package:clock/clock.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/features/activity/presentation/activities_screen.dart';
import 'package:openvitals/features/body/presentation/body_screen.dart';
import 'package:openvitals/features/dashboard/presentation/dashboard_screen.dart';
import 'package:openvitals/features/hydration/presentation/hydration_screen.dart';
import 'package:openvitals/features/mindfulness/presentation/mindfulness_screen.dart';
import 'package:openvitals/features/settings/presentation/settings_screen.dart';
import 'package:openvitals/features/sleep/presentation/sleep_screen.dart';
import 'package:openvitals/features/vitals/presentation/heart_vitals_overview_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

import '../support/boot_container.dart';

/// Every top-level screen at the largest Android font scale, on a phone-sized
/// surface, over the real graph and the real fixture corpus.
///
/// A health app skews toward users who run large fonts, and a RenderFlex
/// overflow at 2.0 is invisible at 1.0 — nothing else in the suite would ever
/// catch it. A screen failing here is a layout bug, not a test problem: fix it
/// with wrapping/ellipsis/scrolling, never by shrinking the user's text.
///
/// The clock is pinned inside the fixture week so the screens render DATA at
/// 2.0, not just their empty states — a populated tile is where text collides.
final _now = DateTime(2025, 6, 25, 14, 30);

final Map<String, Widget Function()> _screens = {
  'dashboard': () => const DashboardScreen(),
  'sleep': () => const SleepScreen(),
  'hydration': () => const HydrationScreen(),
  'activities': () => const ActivitiesScreen(),
  'body': () => const BodyScreen(),
  'mindfulness': () => const MindfulnessScreen(),
  'heart vitals overview': () => const HeartVitalsOverviewScreen(),
  'settings': () => const SettingsScreen(),
};

void main() {
  for (final entry in _screens.entries) {
    testWidgets('${entry.key} screen survives 2.0 text scale', (tester) async {
      await withClock(Clock.fixed(_now), () async {
        tester.view.physicalSize = const Size(420, 900);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.reset);

        // allowUnimplemented: the sweep asserts LAYOUT under scaling; a screen
        // whose reads the fake does not answer renders its empty state, which
        // still must hold at 2.0.
        final h = await bootContainer(allowUnimplemented: true);
        await tester.pumpWidget(UncontrolledProviderScope(
          container: h.container,
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            builder: (context, child) => MediaQuery(
              data: MediaQuery.of(context)
                  .copyWith(textScaler: const TextScaler.linear(2.0)),
              child: child!,
            ),
            // Some screens rely on the shell's Material ancestor rather than
            // carrying their own Scaffold.
            home: Material(child: entry.value()),
          ),
        ));
        await tester.pumpAndSettle();

        expect(tester.takeException(), isNull);
      });
    });
  }
}
