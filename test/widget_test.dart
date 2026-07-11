// App-shell smoke test: the root [OpenVitalsApp] pumps and renders the start
// screen without throwing. Platform-backed providers are overridden so the test
// runs headless — SharedPreferences via mock initial values, drift via an
// in-memory database, and the health data source via the safe-default base
// class (no Health Connect / HealthKit access).

import 'package:drift/native.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/app.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/dashboard/dashboard_screen.dart';
import 'package:openvitals/features/onboarding/onboarding_screen.dart';
import 'package:openvitals/health/health_data_source.dart';

/// Builds the app wrapped in a `ProviderScope` with platform providers
/// overridden. Returns the widget (rather than the override list) because
/// Riverpod 3's `Override` type is not exported from the public barrel and so
/// cannot be named in a signature.
Future<Widget> _bootstrapApp({required bool onboardingComplete}) async {
  SharedPreferences.setMockInitialValues(
    onboardingComplete ? {'onboarding_done': true} : {},
  );
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      openVitalsDatabaseProvider.overrideWith((ref) {
        final db = OpenVitalsDatabase(NativeDatabase.memory());
        ref.onDispose(db.close);
        return db;
      }),
      // Base class returns safe, side-effect-free defaults — no platform access.
      healthDataSourceProvider.overrideWithValue(HealthDataSource()),
    ],
    child: const OpenVitalsApp(),
  );
}

void main() {
  testWidgets('renders onboarding start screen when onboarding incomplete',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      await _bootstrapApp(onboardingComplete: false),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(OnboardingScreen), findsOneWidget);
    // The onboarding flow leads with the app name header.
    expect(find.text('OpenVitals'), findsWidgets);
  });

  testWidgets('renders dashboard start screen when onboarding complete',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      await _bootstrapApp(onboardingComplete: true),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(DashboardScreen), findsOneWidget);
    // The dashboard renders inside the adaptive scaffold's nav suite.
    expect(find.text('OpenVitals'), findsWidgets);
  });
}
