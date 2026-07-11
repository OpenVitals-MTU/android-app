import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/preferences/app_theme_mode.dart';
import 'package:openvitals/features/settings/settings_screen.dart';
import 'package:openvitals/features/settings/settings_section.dart';
import 'package:openvitals/features/settings/settings_section_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

Future<(Widget, SharedPreferences)> _bootstrap(Widget child) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  PackageInfo.setMockInitialValues(
    appName: 'OpenVitals',
    packageName: 'tech.mmarca.openvitals',
    version: '1.2.3',
    buildNumber: '45',
    buildSignature: '',
  );
  final prefs = await SharedPreferences.getInstance();
  return (
    ProviderScope(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: child,
      ),
    ),
    prefs,
  );
}

void main() {
  testWidgets('settings root renders the section cards', (tester) async {
    final (widget, _) = await _bootstrap(const SettingsScreen());
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Display'), findsOneWidget);
    expect(find.text('Health Connect'), findsOneWidget);
    expect(find.text('Nutrition'), findsOneWidget);
  });

  testWidgets('settings root shows the support card and version footer',
      (tester) async {
    final (widget, _) = await _bootstrap(const SettingsScreen());
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);

    // The hub is a lazy ListView; scroll the support/version footer into view.
    await tester.scrollUntilVisible(find.text('Report an issue'), 400);
    await tester.pumpAndSettle();

    // Support section card title + the three link buttons.
    expect(find.text('Support OpenVitals'), findsOneWidget);
    expect(find.text('Report an issue'), findsOneWidget);
    expect(find.text('Join Zulip discussions'), findsOneWidget);
    expect(find.text('Open Liberapay'), findsOneWidget);

    // Version footer, populated from the mocked package info.
    await tester.scrollUntilVisible(find.text('Version 1.2.3 (45)'), 400);
    expect(find.text('Version 1.2.3 (45)'), findsOneWidget);
  });

  testWidgets('selecting a theme mode persists through the repository',
      (tester) async {
    final (widget, prefs) = await _bootstrap(
      const SettingsSectionScreen(section: SettingsSection.display),
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    // Sanity: it starts at the default (system).
    expect(PreferencesRepository(prefs).appThemeMode, AppThemeMode.system);

    final darkChip = find.text('Dark');
    await tester.ensureVisible(darkChip);
    await tester.tap(darkChip);
    await tester.pumpAndSettle();

    expect(PreferencesRepository(prefs).appThemeMode, AppThemeMode.dark);
  });
}
