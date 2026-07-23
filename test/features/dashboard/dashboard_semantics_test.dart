import 'package:clock/clock.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/features/dashboard/presentation/dashboard_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

import '../../support/boot_container.dart';

/// The dashboard as a screen reader hears it. Not a full a11y audit — it pins
/// the two properties that silently rot: every tile announces its metric by
/// NAME (a tile that renders its title as bare pixels — an ExcludeSemantics,
/// a custom-painted label — reads as an unlabeled group), and the primary
/// actions are reachable as labeled tappables.
final _now = DateTime(2025, 6, 25, 14, 30);

void main() {
  testWidgets('tiles and actions announce themselves to the screen reader',
      (tester) async {
    await withClock(Clock.fixed(_now), () async {
      final handle = tester.ensureSemantics();
      final h = await bootContainer(allowUnimplemented: true);
      await tester.pumpWidget(UncontrolledProviderScope(
        container: h.container,
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: Material(child: DashboardScreen()),
        ),
      ));
      await tester.pumpAndSettle();

      // Metric tiles are named, not anonymous groups.
      expect(find.bySemanticsLabel(RegExp('Steps')), findsWidgets);
      // The date the whole screen is about is announced.
      expect(find.bySemanticsLabel(RegExp('Today')), findsWidgets);
      // The two primary actions are labeled tappables.
      for (final action in ['Log', 'Start workout']) {
        expect(
          tester.getSemantics(find.bySemanticsLabel(RegExp(action)).first),
          isSemantics(hasTapAction: true),
          reason: '"$action" renders but is not tappable through '
              'the semantics tree',
        );
      }

      handle.dispose();
    });
  });
}
