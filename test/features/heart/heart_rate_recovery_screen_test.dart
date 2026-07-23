import 'package:clock/clock.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/features/heart/presentation/heart_rate_recovery_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

import '../../support/boot_container.dart';

/// The heart-rate-recovery HISTORY screen, over the real graph and the real
/// fixture corpus. The reading pipeline (session scanning, the one-minute
/// fall, unmeasured counting) is unit-tested elsewhere; this pins that the
/// screen actually drives it and renders SOMETHING honest for a normal week —
/// its own doc says the empty case is its hardest job, because for most
/// people it is the usual one.
final _now = DateTime(2025, 6, 25, 14, 30);

void main() {
  testWidgets('renders the period view without an error for a normal week',
      (tester) async {
    await withClock(Clock.fixed(_now), () async {
      final h = await bootContainer();
      await tester.pumpWidget(UncontrolledProviderScope(
        container: h.container,
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: HeartRateRecoveryScreen(),
        ),
      ));
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      expect(find.text('Heart rate recovery'), findsOneWidget);
      // The fixture week's ordinary workouts cannot be measured (heart rate
      // stops with the session) — and that must surface as the explanatory
      // empty card, never as a blank chart that reads as a broken app.
      expect(find.text('No recovery test in this period.'), findsOneWidget);
    });
  });
}
