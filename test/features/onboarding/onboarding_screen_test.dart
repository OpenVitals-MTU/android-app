import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/repository/impl/health_repository_impl.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/usecase/read_onboarding_permission_catalog_use_case.dart';
import 'package:openvitals/domain/preferences/app_language.dart';
import 'package:openvitals/features/onboarding/application/onboarding_view_model.dart';
import 'package:openvitals/features/onboarding/presentation/onboarding_screen.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/app_language_dropdown.dart';
import 'package:openvitals/ui/components/ov_card.dart';

class _FakeHealthDataSource extends HealthDataSource {
  _FakeHealthDataSource({
    required HealthConnectAvailability availability,
    this.granted = const <String>{},
    this.mindfulnessSupportedByDevice = false,
    this.mindfulnessIntegrationEnabled,
  }) {
    cachedAvailability = availability;
  }

  Set<String> granted;

  /// What the provider says about mindfulness, before the user's opt-in.
  final bool mindfulnessSupportedByDevice;

  /// The opt-in itself, read live so flipping the preference changes the answer
  /// the way the real data source's `mindfulnessIntegrationEnabled` callback
  /// does.
  final bool Function()? mindfulnessIntegrationEnabled;

  final List<Set<String>> requested = <Set<String>>[];
  int openedSettingsCount = 0;
  bool get openedSettings => openedSettingsCount > 0;

  @override
  Future<HealthConnectAvailability> availability() async => cachedAvailability;

  @override
  Future<HealthConnectFeatureFlags> resolveFeatureFlags() async {
    final flags = HealthConnectFeatureFlags(
      mindfulnessSupportedByDevice: mindfulnessSupportedByDevice,
      mindfulnessAvailable: mindfulnessSupportedByDevice &&
          (mindfulnessIntegrationEnabled?.call() ?? false),
    );
    featureFlags = flags;
    return flags;
  }

  @override
  Future<Set<String>> grantedPermissions() async => granted;

  @override
  Future<bool> requestPermissions(Set<String> permissions) async {
    requested.add(permissions);
    return true;
  }

  @override
  Future<bool> openHealthConnectSettings() async {
    openedSettingsCount++;
    return true;
  }
}

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

/// Exactly what step 1 gates on: Activity + Sleep, minus exercise routes.
///
/// Routes ride along with the Activity request (Health Connect shows them as a
/// slider there) but must never gate, so they stay ungranted throughout these
/// tests — which doubles as proof that an ungranted slider does not trap anyone.
Set<String> get _requiredPermissions =>
    HealthRepositoryImpl(HealthDataSource()).requiredOnboardingPermissions;

/// Walks off step 1 by tapping Next. Assumes the required set is granted.
Future<void> _tapNext(WidgetTester tester) async {
  final next = find.widgetWithText(FilledButton, 'Next');
  await tester.ensureVisible(next);
  await tester.pumpAndSettle();
  await tester.tap(next);
  await tester.pumpAndSettle();
}

Future<void> _tapLabelled(WidgetTester tester, String label) async {
  final f = find.widgetWithText(FilledButton, label);
  await tester.ensureVisible(f);
  await tester.pumpAndSettle();
  await tester.tap(f);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('step 1 lists the five Health Connect categories',
      (tester) async {
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.available,
    );
    await tester.pumpWidget(widget);

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('OpenVitals'), findsOneWidget);
    // Health Connect's own names, because these are the headings the system
    // dialog is about to draw.
    for (final name in const [
      'Activity',
      'Body measurements',
      'Nutrition',
      'Sleep',
      'Vitals',
    ]) {
      expect(find.text(name), findsOneWidget, reason: '$name row missing');
    }
    // The later steps' categories are not on this screen.
    expect(find.text('Cycle tracking'), findsNothing);
  });

  testWidgets('Next is refused until Activity and Sleep are granted',
      (tester) async {
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.available,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    expect(
      tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Next'))
          .onPressed,
      isNull,
    );
  });

  testWidgets('granting only Activity and Sleep is enough to move on',
      (tester) async {
    // Body, Nutrition and Vitals are deliberately still missing.
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.available,
      granted: _requiredPermissions,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    expect(
      tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Next'))
          .onPressed,
      isNotNull,
    );
    // Still outstanding, and that is fine — they are not required.
    expect(find.text('Optional'), findsWidgets);
  });

  testWidgets('a category row requests exactly its own permissions',
      (tester) async {
    final dataSource = _FakeHealthDataSource(
      availability: HealthConnectAvailability.available,
    );
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.available,
      dataSource: dataSource,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    final sleepGrant = find.descendant(
      of: find
          .ancestor(of: find.text('Sleep'), matching: find.byType(OpenVitalsCard))
          .first,
      matching: find.widgetWithText(FilledButton, 'Grant'),
    );
    await tester.ensureVisible(sleepGrant);
    await tester.pumpAndSettle();
    await tester.tap(sleepGrant);
    await tester.pumpAndSettle();

    // One dialog, carrying Sleep and nothing else — the point of grouping by
    // Health Connect's categories.
    expect(dataSource.requested, hasLength(1));
    expect(
      dataSource.requested.single,
      HealthRepositoryImpl(HealthDataSource()).sleepCategoryPermissions,
    );
  });

  testWidgets('the mindfulness step is skipped when the device lacks it',
      (tester) async {
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.available,
      granted: _requiredPermissions,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    await _tapNext(tester);

    // Straight past mindfulness to cycle tracking — a step with nothing to
    // offer is not shown as a dead end.
    expect(find.text('Cycle tracking'), findsWidgets);
    expect(find.byType(SwitchListTile), findsNothing);
  });

  testWidgets('the mindfulness step appears where the device has it',
      (tester) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    final dataSource = _FakeHealthDataSource(
      availability: HealthConnectAvailability.available,
      granted: _requiredPermissions,
      mindfulnessSupportedByDevice: true,
      mindfulnessIntegrationEnabled: () =>
          prefs.getBool('health_connect_mindfulness_enabled') ?? false,
    );
    await tester.pumpWidget(ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        healthDataSourceProvider.overrideWithValue(dataSource),
      ],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: const OnboardingScreen(),
      ),
    ));
    await tester.pumpAndSettle();
    await _tapNext(tester);

    // Offered, off, and with nothing to grant until it is turned on.
    final toggle = find.byType(SwitchListTile);
    expect(toggle, findsOneWidget);
    expect(tester.widget<SwitchListTile>(toggle).value, isFalse);
    expect(find.widgetWithText(FilledButton, 'Grant'), findsNothing);

    await tester.tap(toggle);
    await tester.pumpAndSettle();

    expect(prefs.getBool('health_connect_mindfulness_enabled'), isTrue);

    // Granting requests mindfulness ALONE. Merging it into another category is
    // what would let a provider that crashes on it cost the user everything.
    dataSource.requested.clear();
    await _tapLabelled(tester, 'Grant');
    expect(dataSource.requested, hasLength(1));
    expect(
      dataSource.requested.single.every((p) => p.contains('MINDFULNESS')),
      isTrue,
      reason: 'the mindfulness step must request nothing but mindfulness',
    );
  });

  testWidgets('the forward button stops saying "Not now" once a step is done',
      (tester) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    final dataSource = _FakeHealthDataSource(
      availability: HealthConnectAvailability.available,
      granted: _requiredPermissions,
      mindfulnessSupportedByDevice: true,
      mindfulnessIntegrationEnabled: () =>
          prefs.getBool('health_connect_mindfulness_enabled') ?? false,
    );
    await tester.pumpWidget(ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        healthDataSourceProvider.overrideWithValue(dataSource),
      ],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: const OnboardingScreen(),
      ),
    ));
    await tester.pumpAndSettle();
    await _tapNext(tester);

    // Opt-in off: there is nothing to grant BECAUSE the user declined, so
    // moving on really is skipping.
    expect(find.widgetWithText(FilledButton, 'Not now'), findsOneWidget);

    await tester.tap(find.byType(SwitchListTile));
    await tester.pumpAndSettle();
    // On but ungranted — still leaving something behind.
    expect(find.widgetWithText(FilledButton, 'Not now'), findsOneWidget);

    // Grant it, and the way on is no longer a skip.
    dataSource.granted = {
      ..._requiredPermissions,
      'android.permission.health.READ_MINDFULNESS',
      'android.permission.health.WRITE_MINDFULNESS',
    };
    await _tapLabelled(tester, 'Grant');

    expect(find.widgetWithText(FilledButton, 'Not now'), findsNothing);
    expect(find.widgetWithText(FilledButton, 'Next'), findsOneWidget);
  });

  testWidgets('back walks the steps, and only exits from the first',
      (tester) async {
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.available,
      granted: _requiredPermissions,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    // No Back on step 1 — there is nowhere behind it, and an inert button would
    // say otherwise.
    expect(find.widgetWithText(OutlinedButton, 'Back'), findsNothing);

    await _tapNext(tester);
    expect(find.text('Cycle tracking'), findsWidgets);

    final back = find.widgetWithText(OutlinedButton, 'Back');
    expect(back, findsOneWidget);
    await tester.ensureVisible(back);
    await tester.pumpAndSettle();
    await tester.tap(back);
    await tester.pumpAndSettle();

    // Back on step 1, with its five rows.
    expect(find.text('Body measurements'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, 'Back'), findsNothing);
  });

  testWidgets('the last step walks the user to exercise routes by hand',
      (tester) async {
    final dataSource = _FakeHealthDataSource(
      availability: HealthConnectAvailability.available,
      granted: _requiredPermissions,
    );
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.available,
      dataSource: dataSource,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    await _tapNext(tester); // → cycle tracking
    await _tapLabelled(tester, 'Not now'); // → additional access

    // Health Connect exposes no deep link to "Additional access", so the last
    // stretch is a walkthrough. Confirm all three steps render.
    expect(find.text('Exercise routes'), findsOneWidget);
    expect(find.textContaining('Additional access'), findsWidgets);
    for (final n in const ['1', '2', '3']) {
      expect(find.text(n), findsOneWidget, reason: 'step $n missing');
    }

    // Nothing has been opened on our own initiative — that is the user's tap.
    expect(dataSource.openedSettingsCount, 0);
    await _tapLabelled(tester, 'Open Health Connect permissions');
    expect(dataSource.openedSettingsCount, 1);
  });

  testWidgets('finishing persists the prefs and the permission-set version',
      (tester) async {
    final (widget, prefs) = await _bootstrap(
      availability: HealthConnectAvailability.available,
      granted: _requiredPermissions,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    await _tapNext(tester);
    await _tapLabelled(tester, 'Not now');
    expect(prefs.getBool('onboarding_done'), isNot(true));

    await _tapLabelled(tester, 'Finish');

    expect(prefs.getBool('onboarding_done'), isTrue);
    // Without the stamp, widening the required set later would never reach this
    // user — `onboarding_done` alone is a one-way door.
    expect(
      prefs.getInt('last_prompted_permission_set_version'),
      HealthPermissionService.PERMISSION_SET_VERSION,
    );
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
    // No wizard at all on a device that cannot store health data.
    expect(find.widgetWithText(FilledButton, 'Next'), findsNothing);
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

  testWidgets('the header renders the wide logo and the language dropdown',
      (tester) async {
    final (widget, _) = await _bootstrap(
      availability: HealthConnectAvailability.available,
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(AppLanguageDropdown), findsOneWidget);
    expect(find.text('System default'), findsOneWidget);
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
    await tester.tap(find.text('Deutsch').last);
    await tester.pumpAndSettle();

    expect(prefs.getString('app_language'), AppLanguage.german.name);
  });

  test('the catalog is Health Connect\'s categories, in wizard order', () async {
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

    final notifier = container.read(onboardingProvider.notifier);
    // `mindfulness` is absent because this harness reports the feature
    // unavailable, which empties its permission set — and an empty category is
    // dropped rather than rendered as a row that grants nothing.
    expect(
      notifier.permissionCategories.map((c) => c.id).toList(),
      const <String>[
        'activity',
        'body',
        'nutrition',
        'sleep',
        'vitals',
        'cycle_tracking',
        // `additional_data_access` is absent: this harness reports neither
        // history nor background reads, and exercise routes now ride with
        // Activity rather than propping this category up.
      ],
    );
    // Only the two the dashboard cannot render without.
    expect(
      notifier.permissionCategories
          .where((c) => c.isRequired)
          .map((c) => c.id)
          .toList(),
      const <String>['activity', 'sleep'],
    );
  });

  test('the additional-access row counts only what its button can grant', () {
    // Exercise routes are handled by the step's own walkthrough, not by this
    // row. Counting them here made it read "2 of 3" forever: the third could
    // never be granted from anywhere the row's button leads.
    final repo = HealthRepositoryImpl(
      HealthDataSource()
        ..featureFlags = const HealthConnectFeatureFlags(
          healthDataHistoryAvailable: true,
          backgroundReadAvailable: true,
        ),
    );
    final catalog =
        ReadOnboardingPermissionCatalogUseCase(repo)(mindfulnessAvailable: false);

    final row = catalog.categories
        .firstWhere((c) => c.id == 'additional_data_access');
    expect(row.permissions, hasLength(2));
    expect(row.permissions, repo.additionalDataAccessPermissions);
    expect(row.permissions.intersection(repo.routePermissions), isEmpty);
    expect(row.manualPermissions, isEmpty);
  });

  test('the required set is Activity and Sleep, and nothing that cannot be granted',
      () {
    final repo = HealthRepositoryImpl(HealthDataSource());
    final required = repo.requiredOnboardingPermissions;

    // Set EQUALITY, so adding a category later cannot silently make it required.
    expect(required, <String>{
      ...repo.activityCategoryPermissions,
      ...repo.sleepCategoryPermissions,
    }.difference(repo.routePermissions));

    // Route WRITE and route READ are not a pair, whatever the names suggest.
    // WRITE_EXERCISE_ROUTE is an ordinary toggle in the Activity group, so it
    // ships with Activity; READ_EXERCISE_ROUTES lives under Additional access
    // and cannot be requested at all, so it must stay out of every category.
    expect(
      repo.activityCategoryPermissions,
      contains('android.permission.health.WRITE_EXERCISE_ROUTE'),
    );
    expect(
      repo.activityCategoryPermissions.intersection(repo.routePermissions),
      isEmpty,
    );
    expect(repo.routePermissions,
        {'android.permission.health.READ_EXERCISE_ROUTES'});

    // Blocking on anything the dialog may refuse is an onboarding nobody can
    // leave. Asserted by intersection so a member added to any of these groups
    // later is still caught.
    expect(required.intersection(repo.routePermissions), isEmpty);
    expect(required.intersection(repo.additionalDataAccessPermissions), isEmpty);
    expect(required.intersection(repo.cycleCategoryPermissions), isEmpty);
    expect(required.intersection(repo.mindfulnessCategoryPermissions), isEmpty);
    expect(required.where((p) => p.contains('MINDFULNESS')), isEmpty);

    // And it is not empty by accident.
    expect(required, contains('android.permission.health.READ_STEPS'));
    expect(required, contains('android.permission.health.READ_SLEEP'));
    expect(required, contains('android.permission.health.WRITE_SLEEP'));
  });
}
