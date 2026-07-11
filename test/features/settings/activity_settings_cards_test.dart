import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/settings/cards/activity_recording_preferences_card.dart';
import 'package:openvitals/features/settings/cards/favorite_activity_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';

Future<(Widget, SharedPreferences)> _bootstrap(Widget card) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return (
    ProviderScope(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: SingleChildScrollView(child: card),
        ),
      ),
    ),
    prefs,
  );
}

void main() {
  group('ActivityRecordingPreferencesCard', () {
    testWidgets('renders the intro and all sub-controls', (tester) async {
      final (widget, _) =
          await _bootstrap(const ActivityRecordingPreferencesCard());
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      // Intro.
      expect(find.text('Activity recording'), findsOneWidget);
      // Switches.
      expect(find.text('Screen always on'), findsOneWidget);
      expect(find.text('Auto-idle'), findsOneWidget);
      expect(find.text('Barometer climb'), findsOneWidget);
      expect(find.text('Rest timer bell'), findsOneWidget);
      expect(find.text('Voice announcements'), findsOneWidget);
      expect(find.text('Idle announcements'), findsOneWidget);
      expect(find.text('Lap announcements'), findsOneWidget);
      // Segmented choices.
      expect(find.text('Idle timeout'), findsOneWidget);
      expect(find.text('Required GPS accuracy'), findsOneWidget);
      expect(find.text('New route segment after gap'), findsOneWidget);
      expect(find.text('Recording time interval'), findsOneWidget);
      expect(find.text('Recording distance interval'), findsOneWidget);
      expect(find.text('Announce by time'), findsOneWidget);
      expect(find.text('Announce by distance'), findsOneWidget);
      // Distinctive option labels.
      expect(find.text('60 s'), findsOneWidget);
      expect(find.text('0.5 s'), findsOneWidget);
      expect(find.text('Auto'), findsOneWidget);
    });

    testWidgets('toggling a switch persists to the repository',
        (tester) async {
      final (widget, prefs) =
          await _bootstrap(const ActivityRecordingPreferencesCard());
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      final repo = PreferencesRepository(prefs);
      expect(repo.activityRecordingPreferences().keepScreenOnDuringRecording,
          isFalse);

      // The first switch in tree order is "Screen always on".
      await tester.tap(find.byType(Switch).first);
      await tester.pumpAndSettle();

      expect(repo.activityRecordingPreferences().keepScreenOnDuringRecording,
          isTrue);
    });

    testWidgets('selecting a segment persists to the repository',
        (tester) async {
      final (widget, prefs) =
          await _bootstrap(const ActivityRecordingPreferencesCard());
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      final repo = PreferencesRepository(prefs);
      expect(repo.activityRecordingPreferences().autoIdleTimeoutSeconds, 10);

      final option = find.text('60 s');
      await tester.ensureVisible(option);
      await tester.tap(option);
      await tester.pumpAndSettle();

      expect(repo.activityRecordingPreferences().autoIdleTimeoutSeconds, 60);
    });

    testWidgets('idle-timeout choice is disabled when auto-idle is off',
        (tester) async {
      final (widget, prefs) =
          await _bootstrap(const ActivityRecordingPreferencesCard());
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      final repo = PreferencesRepository(prefs);
      expect(repo.activityRecordingPreferences().autoIdleEnabled, isTrue);

      // Switch index 1 in tree order is "Auto-idle"; turn it off.
      await tester.tap(find.byType(Switch).at(1));
      await tester.pumpAndSettle();
      expect(repo.activityRecordingPreferences().autoIdleEnabled, isFalse);

      // The idle-timeout chips are now non-interactive: tapping "60 s" is a
      // no-op, so the stored timeout stays at its default.
      final disabledChip = tester.widget<ChoiceChip>(
        find.ancestor(
          of: find.text('60 s'),
          matching: find.byType(ChoiceChip),
        ),
      );
      expect(disabledChip.onSelected, isNull);

      final option = find.text('60 s');
      await tester.ensureVisible(option);
      await tester.tap(option, warnIfMissed: false);
      await tester.pumpAndSettle();
      expect(repo.activityRecordingPreferences().autoIdleTimeoutSeconds, 10);
    });
  });

  group('FavoriteActivityCard', () {
    testWidgets('selecting a type persists and "latest" clears it',
        (tester) async {
      final (widget, prefs) = await _bootstrap(const FavoriteActivityCard());
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      final repo = PreferencesRepository(prefs);
      expect(repo.favoriteActivityExerciseType, isNull);
      expect(find.text('Use latest'), findsOneWidget);

      // Open the dropdown and pick Running (exercise type 56).
      await tester.tap(find.text('Use latest'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Running').last);
      await tester.pumpAndSettle();

      expect(repo.favoriteActivityExerciseType, 56);

      // Re-open and choose "Use latest" to clear it.
      await tester.tap(find.text('Running'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Use latest').last);
      await tester.pumpAndSettle();

      expect(repo.favoriteActivityExerciseType, isNull);
    });
  });
}
