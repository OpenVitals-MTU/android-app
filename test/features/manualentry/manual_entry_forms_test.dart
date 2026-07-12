import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/body_repository.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/data/repository/contract/vitals_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/body_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/model/vitals_models.dart';
import 'package:openvitals/features/manualentry/presentation/body_measurement_entry_screen.dart';
import 'package:openvitals/features/manualentry/presentation/carbs_entry_screen.dart';
import 'package:openvitals/features/manualentry/presentation/hydration_entry_screen.dart';
import 'package:openvitals/features/manualentry/presentation/vitals_measurement_entry_screen.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

// ── Fake repositories capturing the write requests ────────────────────────────

class _FakeBodyRepository implements BodyRepository {
  BodyMeasurementWriteRequest? written;
  int writeCount = 0;

  @override
  Set<String> bodyWritePermissions(BodyMeasurementType type) =>
      {HcPermissions.writeWeight};

  @override
  Future<Result<bool>> hasBodyWritePermission(BodyMeasurementType type) async =>
      const Ok(true);

  @override
  Future<Result<String>> writeBodyMeasurementEntry(
    BodyMeasurementWriteRequest request,
  ) async {
    written = request;
    writeCount += 1;
    return const Ok('record-id');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeVitalsRepository implements VitalsRepository {
  VitalsMeasurementWriteRequest? written;
  int writeCount = 0;

  @override
  Set<String> vitalsWritePermissions(VitalsMeasurementType type) =>
      {HcPermissions.writeBloodPressure};

  @override
  Future<bool> hasVitalsWritePermission(VitalsMeasurementType type) async =>
      true;

  @override
  Future<String> writeVitalsMeasurementEntry(
    VitalsMeasurementWriteRequest request,
  ) async {
    written = request;
    writeCount += 1;
    return 'record-id';
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeHydrationRepository implements HydrationRepository {
  HydrationWriteRequest? written;
  int writeCount = 0;

  @override
  Set<String> get hydrationWritePermissions => {HcPermissions.writeHydration};

  @override
  Map<String, double> hydrationContainerVolumeMilliliters() =>
      const <String, double>{};

  @override
  double hydrationDailyGoalLiters() => 2.0;

  @override
  double? lastCustomHydrationAmountMilliliters() => null;

  @override
  void setLastCustomHydrationAmountMilliliters(double milliliters) {}

  @override
  Future<Result<List<CustomHydrationDrink>>> customHydrationDrinks() async =>
      const Ok(<CustomHydrationDrink>[]);

  @override
  Future<Result<bool>> hasHydrationWritePermission() async => const Ok(true);

  @override
  Future<Result<List<DailyHydration>>> loadDailyHydration(
    LocalDate start,
    LocalDate end,
  ) async =>
      const Ok(<DailyHydration>[]);

  @override
  Future<Result<String>> writeHydrationEntry(
    HydrationWriteRequest request,
  ) async {
    written = request;
    writeCount += 1;
    return const Ok('hydration-id');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeNutritionRepository implements NutritionRepository {
  NutritionWriteRequest? written;
  int writeCount = 0;
  int permissionChecks = 0;

  @override
  Set<String> get nutritionWritePermissions => {HcPermissions.writeNutrition};

  @override
  Future<Result<bool>> hasNutritionWritePermission() async {
    permissionChecks++;
    return const Ok(true);
  }

  @override
  Future<Result<String>> writeCarbsEntry(NutritionWriteRequest request) async {
    written = request;
    writeCount += 1;
    return const Ok('carbs-id');
  }

  @override
  Future<Result<String>> writeNutritionEntry(
    NutritionWriteRequest request,
  ) async {
    written = request;
    writeCount += 1;
    return const Ok('nutrition-id');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<SharedPreferences> _prefs() async {
  // Pin the unit system to metric so canonicalisation is deterministic (the
  // default is locale-derived and can resolve to imperial in CI).
  SharedPreferences.setMockInitialValues(
    const <String, Object>{'unit_system': 'metric'},
  );
  return SharedPreferences.getInstance();
}

void main() {
  testWidgets('Body weight form writes the expected request on save',
      (tester) async {
    final repo = _FakeBodyRepository();
    final prefs = await _prefs();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
          healthConnectAvailabilityProvider
              .overrideWith((ref) async => HealthConnectAvailability.available),
          grantedHealthPermissionsProvider
              .overrideWith((ref) async => {HcPermissions.writeWeight}),
          bodyRepositoryProvider.overrideWithValue(repo),
        ],
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: BodyMeasurementEntryScreen(bodyMeasurementType: 'WEIGHT'),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField), '72.5');
    await tester.pump();
    await tester.tap(find.widgetWithText(FilledButton, 'Add Weight'));
    await tester.pumpAndSettle();

    expect(repo.writeCount, 1);
    expect(repo.written!.type, BodyMeasurementType.weight);
    expect(repo.written!.value, 72.5);
  });

  testWidgets('Body weight form blocks the write on invalid input',
      (tester) async {
    final repo = _FakeBodyRepository();
    final prefs = await _prefs();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
          healthConnectAvailabilityProvider
              .overrideWith((ref) async => HealthConnectAvailability.available),
          grantedHealthPermissionsProvider
              .overrideWith((ref) async => {HcPermissions.writeWeight}),
          bodyRepositoryProvider.overrideWithValue(repo),
        ],
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: BodyMeasurementEntryScreen(bodyMeasurementType: 'WEIGHT'),
        ),
      ),
    );
    await tester.pumpAndSettle();

    // No value entered → tapping save records an error, not a write.
    await tester.tap(find.widgetWithText(FilledButton, 'Add Weight'));
    await tester.pumpAndSettle();

    expect(repo.writeCount, 0);
    // Localized copy, shared across all three body measurements.
    expect(find.text('Enter a valid value for this measurement.'), findsOneWidget);
  });

  testWidgets('Blood pressure form writes systolic + diastolic on save',
      (tester) async {
    final repo = _FakeVitalsRepository();
    final prefs = await _prefs();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
          healthConnectAvailabilityProvider
              .overrideWith((ref) async => HealthConnectAvailability.available),
          grantedHealthPermissionsProvider
              .overrideWith((ref) async => {HcPermissions.writeBloodPressure}),
          vitalsRepositoryProvider.overrideWithValue(repo),
        ],
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: VitalsMeasurementEntryScreen(
            vitalsMeasurementType: 'BLOOD_PRESSURE',
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField).at(0), '120');
    await tester.enterText(find.byType(TextField).at(1), '80');
    await tester.pump();
    await tester.tap(find.widgetWithText(FilledButton, 'Add Blood pressure'));
    await tester.pumpAndSettle();

    expect(repo.writeCount, 1);
    expect(repo.written!.type, VitalsMeasurementType.bloodPressure);
    expect(repo.written!.value, 120.0);
    expect(repo.written!.secondaryValue, 80.0);
  });

  testWidgets('Blood pressure form blocks the write when systolic <= diastolic',
      (tester) async {
    final repo = _FakeVitalsRepository();
    final prefs = await _prefs();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
          healthConnectAvailabilityProvider
              .overrideWith((ref) async => HealthConnectAvailability.available),
          grantedHealthPermissionsProvider
              .overrideWith((ref) async => {HcPermissions.writeBloodPressure}),
          vitalsRepositoryProvider.overrideWithValue(repo),
        ],
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: VitalsMeasurementEntryScreen(
            vitalsMeasurementType: 'BLOOD_PRESSURE',
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField).at(0), '110');
    await tester.enterText(find.byType(TextField).at(1), '120');
    await tester.pump();
    await tester.tap(find.widgetWithText(FilledButton, 'Add Blood pressure'));
    await tester.pumpAndSettle();

    expect(repo.writeCount, 0);
    expect(find.text('Enter a valid value for this vital.'), findsOneWidget);
  });

  testWidgets('Hydration form shows the tracker card, not container presets',
      (tester) async {
    // The Kotlin tracker card dropped the container chips and the custom-amount
    // field; everything is logged through the drink catalog now.
    final hydrationRepo = _FakeHydrationRepository();
    final nutritionRepo = _FakeNutritionRepository();
    final prefs = await _prefs();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
          healthConnectAvailabilityProvider
              .overrideWith((ref) async => HealthConnectAvailability.available),
          grantedHealthPermissionsProvider
              .overrideWith((ref) async => {HcPermissions.writeHydration}),
          hydrationRepositoryProvider.overrideWithValue(hydrationRepo),
          nutritionRepositoryProvider.overrideWithValue(nutritionRepo),
        ],
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: HydrationEntryScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Log beverage'), findsOneWidget);
    expect(find.text('Drink catalog'), findsOneWidget);
    expect(find.text('New drink'), findsOneWidget);
    // No container presets, no "Add 100 mL", no custom-amount field.
    expect(find.byType(ChoiceChip), findsNothing);
    expect(find.textContaining('Add 100'), findsNothing);
    expect(find.widgetWithText(TextField, 'Amount (mL)'), findsNothing);
    // The only field is the catalog search.
    expect(find.widgetWithText(TextField, 'Search drinks'), findsOneWidget);
    expect(hydrationRepo.writeCount, 0);
  });

  testWidgets('re-checks the write permission when the screen resumes',
      (tester) async {
    // Port of the Kotlin `LifecycleEventEffect(ON_RESUME) { refreshPermission() }`:
    // a user who leaves to grant the permission must come back to a live form.
    final repo = _FakeNutritionRepository();
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
          healthConnectAvailabilityProvider
              .overrideWith((ref) async => HealthConnectAvailability.available),
          grantedHealthPermissionsProvider
              .overrideWith((ref) async => {HcPermissions.writeNutrition}),
          nutritionRepositoryProvider.overrideWithValue(repo),
        ],
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: CarbsEntryScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();
    final afterBuild = repo.permissionChecks;
    expect(afterBuild, greaterThan(0));

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();

    expect(repo.permissionChecks, greaterThan(afterBuild));
  });
}
