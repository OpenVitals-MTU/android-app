import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/manualentry/manual_entry_screen.dart';
import 'package:openvitals/health/health_data_source.dart';
import 'package:openvitals/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

/// A device-free source whose permission taxonomy is driven by the flags and
/// unsupported set the test injects — exactly how the real one resolves it.
class _FakeSource extends HealthDataSource {
  _FakeSource({
    Set<String> unsupported = const <String>{},
    bool mindfulness = true,
  }) {
    cachedAvailability = HealthConnectAvailability.available;
    unsupportedPermissions = unsupported;
    featureFlags = HealthConnectFeatureFlags(mindfulnessAvailable: mindfulness);
  }
}

Future<Widget> _bootstrap({
  Set<String> unsupported = const <String>{},
  bool mindfulness = true,
  List<String>? storedOrder,
}) async {
  SharedPreferences.setMockInitialValues(
    storedOrder == null
        ? const <String, Object>{}
        : <String, Object>{'manual_entry_widget_order': storedOrder.join(',')},
  );
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      healthDataSourceProvider.overrideWithValue(
        _FakeSource(unsupported: unsupported, mindfulness: mindfulness),
      ),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
    ],
    child: const MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: ManualEntryScreen()),
    ),
  );
}

void _useTallViewport(WidgetTester tester) {
  tester.view.physicalSize = const Size(800, 1600);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

void main() {
  testWidgets('shows every entry type the device supports', (tester) async {
    _useTallViewport(tester);
    await tester.pumpWidget(await _bootstrap());
    await tester.pumpAndSettle();

    expect(find.text('Hydration'), findsOneWidget);
    expect(find.text('Blood oxygen'), findsOneWidget);
    expect(find.text('Mindfulness'), findsOneWidget);
  });

  testWidgets('hides entry types the provider cannot accept writes for',
      (tester) async {
    _useTallViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(unsupported: {HcPermissions.writeSpO2}),
    );
    await tester.pumpAndSettle();

    expect(find.text('Blood oxygen'), findsNothing);
    // Its neighbours survive.
    expect(find.text('Blood pressure'), findsOneWidget);
    expect(find.text('Respiratory rate'), findsOneWidget);

    // And it is not offered in the add tray either — it could never be granted.
    await tester.tap(find.byTooltip('Edit entries'));
    await tester.pumpAndSettle();
    expect(find.widgetWithText(OutlinedButton, 'Blood oxygen'), findsNothing);
  });

  testWidgets('hides mindfulness when the feature is unavailable',
      (tester) async {
    _useTallViewport(tester);
    await tester.pumpWidget(await _bootstrap(mindfulness: false));
    await tester.pumpAndSettle();

    expect(find.text('Mindfulness'), findsNothing);
    expect(find.text('Hydration'), findsOneWidget);
  });

  testWidgets('edit mode removes a tile to the tray and adds it back',
      (tester) async {
    _useTallViewport(tester);
    await tester.pumpWidget(await _bootstrap());
    await tester.pumpAndSettle();

    // No remove buttons until editing.
    expect(find.byTooltip('Remove widget'), findsNothing);

    await tester.tap(find.byTooltip('Edit entries'));
    await tester.pumpAndSettle();
    expect(find.byTooltip('Remove widget'), findsWidgets);
    expect(find.text('All widgets are already on the summary.'), findsOneWidget);

    // Remove the first tile (Hydration).
    await tester.tap(find.byTooltip('Remove widget').first);
    await tester.pumpAndSettle();
    expect(find.text('Hydration'), findsOneWidget); // now only the tray button
    expect(find.widgetWithText(OutlinedButton, 'Hydration'), findsOneWidget);

    // Persisted as the visible order, minus the removed one.
    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getString('manual_entry_widget_order'), isNot(contains('HYDRATION')));

    // Add it back; it returns to the grid and the tray empties.
    await tester.tap(find.widgetWithText(OutlinedButton, 'Hydration'));
    await tester.pumpAndSettle();
    expect(find.widgetWithText(OutlinedButton, 'Hydration'), findsNothing);
    expect(find.text('All widgets are already on the summary.'), findsOneWidget);
    expect(
      prefs.getString('manual_entry_widget_order'),
      contains('HYDRATION'),
    );
  });

  testWidgets('tiles are draggable only while editing', (tester) async {
    _useTallViewport(tester);
    await tester.pumpWidget(await _bootstrap());
    await tester.pumpAndSettle();

    expect(find.byType(LongPressDraggable<int>), findsNothing);

    await tester.tap(find.byTooltip('Edit entries'));
    await tester.pumpAndSettle();
    expect(find.byType(LongPressDraggable<int>), findsWidgets);
    expect(
      find.text('Hold to drag & reorder · tap ✕ to remove'),
      findsOneWidget,
    );
  });

  testWidgets('a stored order drives which tiles show and in what order',
      (tester) async {
    _useTallViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(storedOrder: ['WEIGHT', 'HYDRATION']),
    );
    await tester.pumpAndSettle();

    expect(find.text('Weight'), findsOneWidget);
    expect(find.text('Hydration'), findsOneWidget);
    // Everything else was removed by the user, so it only lives in the tray.
    expect(find.text('Blood pressure'), findsNothing);

    await tester.tap(find.byTooltip('Edit entries'));
    await tester.pumpAndSettle();
    expect(find.widgetWithText(OutlinedButton, 'Blood pressure'), findsOneWidget);
  });
}
