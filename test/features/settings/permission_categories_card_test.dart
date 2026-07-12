import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/settings/presentation/cards/permission_categories_card.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// A [HealthDataSource] whose permission grants and availability are fixed by
/// the test, and which records the sets passed to [requestPermissions].
class _FakeHealthDataSource extends HealthDataSource {
  _FakeHealthDataSource({this.granted = const <String>{}});

  final Set<String> granted;
  final List<Set<String>> requested = <Set<String>>[];
  bool openedSettings = false;

  @override
  Future<HealthConnectAvailability> availability() async =>
      HealthConnectAvailability.available;

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

Future<Widget> _bootstrap(_FakeHealthDataSource dataSource) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      healthDataSourceProvider.overrideWithValue(dataSource),
    ],
    child: const MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(
        body: SingleChildScrollView(child: PermissionCategoriesCard()),
      ),
    ),
  );
}

void main() {
  testWidgets('renders a row per category with granted/optional status',
      (tester) async {
    // Grant the full core (activity & sleep) set so it reads as Granted; leave
    // the rest ungranted so they read as Optional / Not supported.
    final dataSource = _FakeHealthDataSource(granted: {
      'android.permission.health.READ_STEPS',
      'android.permission.health.READ_DISTANCE',
      'android.permission.health.READ_EXERCISE',
      'android.permission.health.READ_SLEEP',
    });
    await tester.pumpWidget(await _bootstrap(dataSource));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // All ten Kotlin categories render (base taxonomy leaves every set
    // non-empty), so titles for the unambiguous ones are present.
    expect(find.text('Activity & sleep'), findsOneWidget);
    expect(find.text('Cycle tracking'), findsOneWidget);
    // The fully-granted category shows Granted; ungranted ones show Optional.
    expect(find.text('Granted'), findsWidgets);
    expect(find.text('Optional'), findsWidgets);
    // Mindfulness is unavailable in the base taxonomy → Not supported.
    expect(find.text('Not supported'), findsWidgets);
  });

  testWidgets('a grant button requests the category permissions',
      (tester) async {
    final dataSource = _FakeHealthDataSource();
    await tester.pumpWidget(await _bootstrap(dataSource));
    await tester.pumpAndSettle();

    // Every ungranted, available category exposes a Grant button.
    final grantButton = find.widgetWithText(FilledButton, 'Grant');
    expect(grantButton, findsWidgets);

    await tester.tap(grantButton.first);
    await tester.pumpAndSettle();

    expect(dataSource.requested, isNotEmpty);
    expect(dataSource.requested.first, isNotEmpty);
  });
}
