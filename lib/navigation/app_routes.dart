import 'package:flutter/material.dart';

import '../core/time/local_date.dart';

/// Route paths + typed argument helpers, ported from the Kotlin
/// `navigation/Screen.kt` (plus the three internal detail routes declared in
/// `AppNavigation.kt`).
///
/// go_router uses `:name` path parameters and leading slashes, whereas the
/// Kotlin Navigation-Compose graph used `{name}` and slash-less routes. The
/// path strings here are the go_router form; [AppRoute] enumerates every
/// destination and the `location(...)` helpers build concrete, URI-encoded
/// targets — the analogue of Kotlin's `Screen.*.createRoute(...)`.
class AppRoutes {
  const AppRoutes._();

  // ── Path-parameter names (verbatim from Screen.kt) ──────────────────────────
  static const String activityDetailIdArg = 'activityId';
  static const String activityEntryIdArg = 'activityEntryId';
  static const String activityEntryModeArg = 'mode';
  static const String activityEntryPlanIdArg = 'planId';
  static const String activityEntryTypeArg = 'activityTypeId';
  static const String sleepDetailIdArg = 'sleepId';
  static const String metricIdArg = 'metricId';
  static const String bodyMeasurementTypeArg = 'bodyMeasurementType';
  static const String bodyEntryIdArg = 'bodyEntryId';
  static const String hydrationEntryIdArg = 'hydrationEntryId';
  static const String hydrationDrinkIdArg = 'hydrationDrinkId';
  static const String mindfulnessEntryIdArg = 'mindfulnessEntryId';
  static const String vitalsMeasurementTypeArg = 'vitalsMeasurementType';
  static const String vitalsEntryIdArg = 'vitalsEntryId';
  static const String watchDeviceIdArg = 'watchDeviceId';
  static const String watchScreenIdArg = 'watchScreenId';
  static const String stressDateArg = 'stressDate';
  static const String bodyEnergyDateArg = 'bodyEnergyDate';
  static const String trainingReadinessDateArg = 'trainingReadinessDate';

  // ── Top-level (shell branch) paths ──────────────────────────────────────────
  static const String onboarding = '/onboarding';
  static const String dashboard = '/dashboard';
  static const String activity = '/activity';
  static const String manualEntry = '/manual_entry';
  static const String settings = '/settings';

  // ── Readiness ───────────────────────────────────────────────────────────────
  static const String dailyReadiness = '/daily_readiness';
  static const String stressDetails = '/daily_readiness/stress/:$stressDateArg';
  static const String bodyEnergyDetails =
      '/daily_readiness/body_energy/:$bodyEnergyDateArg';
  static const String trainingReadinessDetails =
      '/daily_readiness/training_readiness/:$trainingReadinessDateArg';

  // ── Manual-entry children ──────────────────────────────────────────────────
  static const String hydrationEntry = '/manual_entry/hydration';
  static const String hydrationEntryEdit =
      '/manual_entry/hydration/edit/:$hydrationEntryIdArg';
  static const String hydrationEntryLogDrink =
      '/manual_entry/hydration/log/:$hydrationDrinkIdArg';
  static const String carbsEntry = '/manual_entry/carbs';
  static const String activityEntry = '/manual_entry/activity';
  static const String activityEntryEdit =
      '/manual_entry/activity/edit/:$activityEntryIdArg';
  static const String mindfulnessEntry = '/manual_entry/mindfulness';
  static const String mindfulnessEntryEdit =
      '/manual_entry/mindfulness/edit/:$mindfulnessEntryIdArg';
  static const String bodyMeasurementEntry =
      '/manual_entry/body/:$bodyMeasurementTypeArg';
  static const String bodyMeasurementEntryEdit =
      '/manual_entry/body/:$bodyMeasurementTypeArg/edit/:$bodyEntryIdArg';
  static const String vitalsMeasurementEntry =
      '/manual_entry/vitals/:$vitalsMeasurementTypeArg';
  static const String vitalsMeasurementEntryEdit =
      '/manual_entry/vitals/:$vitalsMeasurementTypeArg/edit/:$vitalsEntryIdArg';

  // ── Metric sections & details ──────────────────────────────────────────────
  static const String calories = '/calories';
  static const String nutrition = '/nutrition';
  static const String body = '/body';
  static const String heartVitals = '/heart_vitals';
  static const String activityDetail = '/activity_detail/:$activityDetailIdArg';
  static const String sleep = '/sleep';
  static const String sleepDetail = '/sleep_detail/:$sleepDetailIdArg';
  static const String metric = '/metric/:$metricIdArg';
  static const String achievements = '/achievements';

  // Internal detail routes (from AppNavigation.kt).
  static const String cardioLoadDetail = '/activity/cardio_load';
  static const String heartRateRecoveryDetail = '/heart/recovery';
  static const String caffeineDrinkIdArg = 'entryId';
  static const String caffeineDrink = '/caffeine/drink/:entryId';
  static String caffeineDrinkLocation(String entryId) => '/caffeine/drink/$entryId';
  static const String sleepEfficiencyDetail = '/recovery/sleep_efficiency';
  static const String sleepScoreDetail = '/recovery/sleep_score';

  // ── Settings children ──────────────────────────────────────────────────────
  static const String settingsDisplay = '/settings/display';
  static const String settingsActivities = '/settings/activities';
  static const String settingsSensors = '/settings/sensors';
  static const String settingsWatches = '/settings/watches';
  static const String settingsNutrition = '/settings/nutrition';
  static const String settingsCalories = '/settings/calories';
  static const String settingsCaffeine = '/settings/caffeine';
  static const String settingsRecovery = '/settings/recovery';
  static const String settingsSleep = '/settings/sleep';
  static const String settingsBodyEnergy = '/settings/body_energy';
  static const String settingsDataImport = '/settings/data_import';
  static const String settingsDeviceSync = '/settings/device_sync';
  static const String settingsHealthConnect = '/settings/health_connect';
  static const String settingsPermissions = '/settings/permissions';
  static const String settingsDebugDiagnostics = '/settings/debug_diagnostics';

  // ── Concrete location builders (Kotlin `createRoute(...)`) ──────────────────
  static String stressDetailsLocation(String date) =>
      '/daily_readiness/stress/${Uri.encodeComponent(date)}';
  static String bodyEnergyDetailsLocation(String date) =>
      '/daily_readiness/body_energy/${Uri.encodeComponent(date)}';
  static String trainingReadinessDetailsLocation(String date) =>
      '/daily_readiness/training_readiness/${Uri.encodeComponent(date)}';
  static String hydrationEntryEditLocation(String entryId) =>
      '/manual_entry/hydration/edit/${Uri.encodeComponent(entryId)}';
  static String hydrationEntryLogDrinkLocation(String drinkId) =>
      '/manual_entry/hydration/log/${Uri.encodeComponent(drinkId)}';
  static String activityEntryEditLocation(String entryId) =>
      '/manual_entry/activity/edit/${Uri.encodeComponent(entryId)}';
  static String mindfulnessEntryEditLocation(String entryId) =>
      '/manual_entry/mindfulness/edit/${Uri.encodeComponent(entryId)}';
  static String bodyMeasurementEntryLocation(String type) =>
      '/manual_entry/body/${Uri.encodeComponent(type)}';
  static String bodyMeasurementEntryEditLocation(String type, String entryId) =>
      '/manual_entry/body/${Uri.encodeComponent(type)}'
      '/edit/${Uri.encodeComponent(entryId)}';
  static String vitalsMeasurementEntryLocation(String type) =>
      '/manual_entry/vitals/${Uri.encodeComponent(type)}';
  static String vitalsMeasurementEntryEditLocation(String type, String entryId) =>
      '/manual_entry/vitals/${Uri.encodeComponent(type)}'
      '/edit/${Uri.encodeComponent(entryId)}';
  static String activityDetailLocation(String activityId) =>
      '/activity_detail/${Uri.encodeComponent(activityId)}';
  static String sleepDetailLocation(String sleepId) =>
      '/sleep_detail/${Uri.encodeComponent(sleepId)}';
  static String metricLocation(String metricId) =>
      '/metric/${Uri.encodeComponent(metricId)}';

  /// The one device view. Reached from the Watches list AND from the summary
  /// tile — deliberately the same destination, so a watch has one home.
  static String watchDeviceLocation(String deviceId) =>
      '/watch/${Uri.encodeComponent(deviceId)}';
  static String watchDataLocation(String deviceId) =>
      '/watch/${Uri.encodeComponent(deviceId)}/data';

  /// One screen of the watch's OWN settings tree. The id is the watch's, so a
  /// deep link only means anything while that watch is the one paired.
  static String watchSettingsLocation(String deviceId, int screenId) =>
      '/watch/${Uri.encodeComponent(deviceId)}/settings/$screenId';

  /// Query parameter carrying the day a metric detail screen should OPEN on.
  static const String selectedDayArg = 'day';

  /// Pins [location] to the day the user was looking at when they tapped.
  ///
  /// The dashboard is a day view. Step it back to yesterday, tap the hydration
  /// card, and the detail screen used to open on TODAY — every detail screen builds
  /// its selection from `LocalDate.now()`, so the day you were looking at was simply
  /// dropped on the way. It rides along as `?day=YYYY-MM-DD` instead.
  ///
  /// Today adds nothing (it is what the screens already do) and is left off, so the
  /// ordinary case keeps producing the ordinary location.
  static String withSelectedDay(String location, LocalDate day) {
    if (day == LocalDate.now()) return location;
    final separator = location.contains('?') ? '&' : '?';
    return '$location$separator$selectedDayArg='
        '${Uri.encodeQueryComponent(day.toString())}';
  }

  /// Kotlin `Screen.ActivityEntry.createRoute(mode, planId, activityTypeId)`.
  /// Optional intents ride as query parameters; the bare path still matches.
  static String activityEntryLocation({
    String? mode,
    String? planId,
    String? activityTypeId,
  }) {
    final params = <String, String>{};
    if (mode != null) params[activityEntryModeArg] = mode;
    if (planId != null) params[activityEntryPlanIdArg] = planId;
    if (activityTypeId != null) params[activityEntryTypeArg] = activityTypeId;
    if (params.isEmpty) return activityEntry;
    final query = params.entries
        .map((e) => '${e.key}=${Uri.encodeQueryComponent(e.value)}')
        .join('&');
    return '$activityEntry?$query';
  }
}

/// Intent values understood by the activity-entry `mode` argument
/// (Kotlin `Screen.ActivityEntryMode`).
enum ActivityEntryMode {
  record('record'),
  manual('manual'),
  plan('plan');

  const ActivityEntryMode(this.value);

  final String value;

  static ActivityEntryMode? fromValue(String? value) {
    if (value == null) return null;
    for (final mode in values) {
      if (mode.value == value) return mode;
    }
    return null;
  }
}

/// The top-level nav-suite destinations rendered inside the adaptive scaffold.
///
/// The Kotlin `AppNavigation` keeps a single runtime nav-suite item (Dashboard)
/// and reaches Activities / Manual entry / Settings through top-bar actions; the
/// Flutter shell promotes those conceptual sections to first-class adaptive
/// destinations (dashboard, activities, add-entry, settings), which is the
/// arrangement the app-shell brief calls for.
enum TopLevelDestination {
  dashboard(
    branchIndex: 0,
    location: AppRoutes.dashboard,
    icon: Icons.dashboard_outlined,
    selectedIcon: Icons.dashboard,
    label: 'Dashboard',
  ),
  activities(
    branchIndex: 1,
    location: AppRoutes.activity,
    icon: Icons.directions_run_outlined,
    selectedIcon: Icons.directions_run,
    label: 'Activities',
  ),
  addEntry(
    branchIndex: 2,
    location: AppRoutes.manualEntry,
    icon: Icons.add_circle_outline,
    selectedIcon: Icons.add_circle,
    label: 'Add entry',
  ),
  settings(
    branchIndex: 3,
    location: AppRoutes.settings,
    icon: Icons.settings_outlined,
    selectedIcon: Icons.settings,
    label: 'Settings',
  );

  const TopLevelDestination({
    required this.branchIndex,
    required this.location,
    required this.icon,
    required this.selectedIcon,
    required this.label,
  });

  final int branchIndex;
  final String location;
  final IconData icon;
  final IconData selectedIcon;
  final String label;
}

/// Typed mirror of the Kotlin `features.dashboard.DashboardWidgetId` enum. The
/// dashboard UI layer that owns it is a Phase 5 concern, so it lives here purely
/// so the `/metric/:metricId` route can classify its string argument and route
/// to the matching feature placeholder (the analogue of `MetricRouteContent`).
enum DashboardMetricId {
  steps,
  distance,
  caloriesOut('CALORIES_OUT'),
  activeCalories('ACTIVE_CALORIES'),
  floors,
  elevation,
  wheelchairPushes('WHEELCHAIR_PUSHES'),
  workout,
  sleep,
  bodyEnergy('BODY_ENERGY'),
  hydration,
  caloriesIn('CALORIES_IN'),
  protein,
  carbs,
  fat,
  caffeine,
  weight,
  height,
  bmi,
  ffmi,
  bodyFat('BODY_FAT'),
  leanMass('LEAN_MASS'),
  bmr,
  boneMass('BONE_MASS'),
  bodyWaterMass('BODY_WATER_MASS'),
  avgHeartRate('AVG_HEART_RATE'),
  restingHeartRate('RESTING_HEART_RATE'),
  hrv,
  bloodPressure('BLOOD_PRESSURE'),
  spo2('SPO2'),
  vo2Max('VO2_MAX'),
  respiratoryRate('RESPIRATORY_RATE'),
  bodyTemperature('BODY_TEMPERATURE'),
  bloodGlucose('BLOOD_GLUCOSE'),
  skinTemperature('SKIN_TEMPERATURE'),
  weeklyCardioLoad('WEEKLY_CARDIO_LOAD'),
  cardioLoad('CARDIO_LOAD'),
  mindfulness,
  cycle;

  const DashboardMetricId([String? storageName])
      : _storageName = storageName;

  final String? _storageName;

  /// The Kotlin enum-constant name (SCREAMING_SNAKE_CASE), used as the route arg.
  String get storageName => _storageName ?? name.toUpperCase();

  static DashboardMetricId? fromStorage(String? value) {
    if (value == null) return null;
    for (final id in values) {
      if (id.storageName == value) return id;
    }
    return null;
  }
}
