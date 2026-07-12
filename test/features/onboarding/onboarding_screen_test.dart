import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/repository/impl/health_repository_impl.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/preferences/app_language.dart';
import 'package:openvitals/features/onboarding/onboarding_notifier.dart';
import 'package:openvitals/features/onboarding/onboarding_screen.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/app_language_dropdown.dart';

class _FakeHealthDataSource extends HealthDataSource {
  _FakeHealthDataSource({
    required HealthConnectAvailability availability,
    this.granted = const <String>{},
  }) {
    cachedAvailability = availability;
  }

  Set<String> granted;
  final List<Set<String>> requested = <Set<String>>[];
  bool openedSettings = false;

  @override
  Future<HealthConnectAvailability> availability() async => cachedAvailability;

  @override
  Future<Set<String>> grantedPermissions() async => granted;

  @override
  Future<bool> requestPermissions(Set<String> permissions) async {
    requested.add(permissions);
    return true;
  }

  @override
  Future<bool> openHealthConnectSettings() async {
    openedSettings = true;
    return true;
  }
}

/// The dashboard-minimum permission set the base permission taxonomy produces.
Set<String> get _minimumPermissions =>
    HealthRepositoryImpl(HealthDataSource()).minimumOnboardingPermissions;

Future<(Widget, SharedPreferences)> _bootstrap({
  required HealthConnectAvailability availability,
  Set<String> granted = const <String>{},
  VoidCallback? onComplete,
  _FakeHealthDataSource? dataSource,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final widget = ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      healthDataSourceProvider.overrideWithValue(
        dataSource ??
            _FakeHealthDataSource(availability: availability, granted: granted),
      ),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: OnboardingScreen(onOnboardingComplete: onComplete),
    ),
  );
  return (widget, prefs);
}

void main() {
  testWidgets('shows a loader then the grant-all flow when nothing granted',
      (tester) async {
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.available,
    );
    await tester.pumpWidget(widget);

    expect(find.byType(CircularProgressIndicator), findsOneWidget);

    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('OpenVitals'), findsOneWidget);
    expect(find.text('Grant required Health Connect permissions'), findsOneWidget);
  });

  testWidgets('completing onboarding sets the onboarding-done pref',
      (tester) async {
    final (widget, prefs) = await _bootstrap(
      availability: HealthConnectAvailability.available,
      granted: _minimumPermissions,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    // With the minimum granted the primary action is "Continue". The header
    // (language picker + logo) pushes it below the 600px test viewport, so
    // scroll it into view before tapping.
    expect(find.text('Continue'), findsOneWidget);
    expect(prefs.getBool('onboarding_done'), isNot(true));

    await tester.ensureVisible(find.text('Continue'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Continue'));
    await tester.pumpAndSettle();

    expect(prefs.getBool('onboarding_done'), isTrue);
  });

  testWidgets('shows the unavailable message when Health Connect is missing',
      (tester) async {
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.notSupported,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    expect(
      find.text('Health Connect is not supported on this device.'),
      findsOneWidget,
    );
    expect(find.text('Grant required Health Connect permissions'), findsNothing);
  });

  testWidgets('the header renders the wide logo and the language dropdown',
      (tester) async {
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.available,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // The shared AppLanguageDropdown, defaulting to "follow the system" (a
    // closed DropdownButton only builds its selected item).
    expect(find.byType(AppLanguageDropdown), findsOneWidget);
    expect(find.text('System default'), findsOneWidget);
    // The wide wordmark (decorative: excluded from semantics).
    final logo = tester.widget<Image>(
      find.byWidgetPredicate(
        (w) =>
            w is Image &&
            w.image is AssetImage &&
            (w.image as AssetImage).assetName ==
                'assets/icon/openvitals_logo_wide.png',
      ),
    );
    expect(logo.width, 152);
    expect(logo.height, 104);
    expect(logo.excludeFromSemantics, isTrue);
  });

  testWidgets('picking a language persists the app-language preference',
      (tester) async {
    final (widget, prefs) = await _bootstrap(
      availability: HealthConnectAvailability.available,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    await tester.tap(find.byType(AppLanguageDropdown));
    await tester.pumpAndSettle();
    // The menu overlay adds a second "Deutsch" — tap the one in the menu.
    await tester.tap(find.text('Deutsch').last);
    await tester.pumpAndSettle();

    expect(prefs.getString('app_language'), AppLanguage.german.name);
  });

  testWidgets(
      'a manual-only category shows the manual status and an Open button',
      (tester) async {
    // With the base feature flags, history/background reads are unavailable, so
    // "additional data access" reduces to the manual-only exercise-routes
    // permission: no requestable permission is missing → isManualGrant.
    final dataSource = _FakeHealthDataSource(
      availability: HealthConnectAvailability.available,
    );
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.available,
      dataSource: dataSource,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    expect(find.text('Open settings'), findsOneWidget);

    final openButton = find.widgetWithText(FilledButton, 'Open');
    expect(openButton, findsOneWidget);

    await tester.ensureVisible(openButton);
    await tester.pumpAndSettle();
    await tester.tap(openButton);
    await tester.pumpAndSettle();

    // A manual-only category opens Health Connect settings rather than firing
    // the (useless) runtime permission dialog.
    expect(dataSource.openedSettings, isTrue);
    expect(dataSource.requested, isEmpty);
  });

  testWidgets('needsProviderUpdate offers an install action', (tester) async {
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.needsProviderUpdate,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    expect(
      find.text('Health Connect needs to be installed or updated to use this app.'),
      findsOneWidget,
    );
    expect(
      find.widgetWithText(FilledButton, 'Install Health Connect'),
      findsOneWidget,
    );
  });

  testWidgets('the other unavailable states offer no install action',
      (tester) async {
    for (final availability in const [
      HealthConnectAvailability.notSupported,
      HealthConnectAvailability.needsPlayStore,
    ]) {
      final (widget, _) = await _bootstrap(availability: availability);
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      expect(
        find.text('Install Health Connect'),
        findsNothing,
        reason: '$availability must not offer an install action',
      );
    }
  });

  test('permissionCategories match the Kotlin source groups and order', () async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    final container = ProviderContainer(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        healthDataSourceProvider.overrideWithValue(
          _FakeHealthDataSource(
            availability: HealthConnectAvailability.available,
          ),
        ),
      ],
    );
    addTearDown(container.dispose);

    final notifier = container.read(onboardingNotifierProvider.notifier);
    final categories = notifier.permissionCategories;

    // One-to-one with the Kotlin OnboardingViewModel.permissionCategories order.
    //
    // `mindfulness` is absent because this harness's data source reports the
    // feature unavailable, which since Kotlin 1.9.0 (1f2b435) makes its
    // permission set empty — and onboarding drops empty categories
    // (`.filter { it.permissions.isNotEmpty() }`, OnboardingViewModel.kt:148).
    // That is the point of the fix: never ask for a permission the provider does
    // not define. Settings still lists it, as "Not supported".
    expect(
      categories.map((c) => c.id).toList(),
      const <String>[
        'activity_sleep',
        'heart_recovery',
        'vitals',
        'body',
        'activity_extras',
        'nutrition_hydration',
        'manual_entry_write',
        'data_import_write',
        'additional_data_access',
        'cycle_tracking',
      ],
    );

    final repo = HealthRepositoryImpl(HealthDataSource());
    OnboardingPermissionCategory byId(String id) =>
        categories.firstWhere((c) => c.id == id);
    expect(byId('manual_entry_write').permissions,
        repo.requestableWritePermissions);
    expect(
        byId('data_import_write').permissions, repo.dataImportWritePermissions);
    // Exercise routes ride along additional-data-access but are manual-only.
    expect(byId('additional_data_access').manualPermissions,
        repo.routePermissions);
  });
}
