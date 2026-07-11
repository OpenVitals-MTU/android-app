// Port of the Kotlin `HealthConnectPermissionService` permission taxonomy.
//
// In the Kotlin source a permission is an AndroidX Health Connect permission
// string (e.g. `android.permission.health.READ_STEPS`). We keep those exact
// string identifiers so the phased sets, `PERMISSION_SET_VERSION`, and any
// persisted permission state remain byte-for-byte faithful to the original.
//
// The native Health Connect plugin ([HealthConnectHostApi]) requests and
// queries these permission strings directly, so no per-record data-type mapping
// is needed here — this file is now a pure, platform-independent taxonomy.
import '../domain/model/permission_grant_mode.dart';

/// AndroidX permission-string prefix, mirrored verbatim from Health Connect.
const String _hcPrefix = 'android.permission.health.';

String _read(String record) => '${_hcPrefix}READ_$record';

String _write(String record) => '${_hcPrefix}WRITE_$record';

/// Individual Health Connect permission strings, mirroring the per-record
/// `readXPermission` / `writeXPermission` fields the Kotlin repositories and
/// `DashboardDataLoader` keep. Values are the exact AndroidX permission strings.
abstract final class HcPermissions {
  static final String readSteps = _read('STEPS');
  static final String readDistance = _read('DISTANCE');
  static final String readSpeed = _read('SPEED');
  static final String readExercise = _read('EXERCISE');
  static final String readSleep = _read('SLEEP');
  static final String readHeartRate = _read('HEART_RATE');
  static final String readRestingHeartRate = _read('RESTING_HEART_RATE');
  static final String readHrv = _read('HEART_RATE_VARIABILITY');
  static final String readWeight = _read('WEIGHT');
  static final String readHeight = _read('HEIGHT');
  static final String readBodyFat = _read('BODY_FAT');
  static final String readLeanMass = _read('LEAN_BODY_MASS');
  static final String readBmr = _read('BASAL_METABOLIC_RATE');
  static final String readBoneMass = _read('BONE_MASS');
  static final String readBodyWaterMass = _read('BODY_WATER_MASS');
  static final String readTotalCalories = _read('TOTAL_CALORIES_BURNED');
  static final String readActiveCalories = _read('ACTIVE_CALORIES_BURNED');
  static final String readHydration = _read('HYDRATION');
  static final String readNutrition = _read('NUTRITION');
  static final String readBloodPressure = _read('BLOOD_PRESSURE');
  static final String readSpO2 = _read('OXYGEN_SATURATION');
  static final String readVo2Max = _read('VO2_MAX');
  static final String readRespiratoryRate = _read('RESPIRATORY_RATE');
  static final String readBodyTemperature = _read('BODY_TEMPERATURE');
  static final String readBloodGlucose = _read('BLOOD_GLUCOSE');
  static final String readSkinTemperature = _read('SKIN_TEMPERATURE');
  static final String readFloors = _read('FLOORS_CLIMBED');
  static final String readElevation = _read('ELEVATION_GAINED');
  static final String readWheelchairPushes = _read('WHEELCHAIR_PUSHES');
  static final String readMindfulness = _read('MINDFULNESS');
  static final String readMenstruationFlow = _read('MENSTRUATION');
  // Health Connect exposes a single READ_MENSTRUATION permission covering both
  // MenstruationFlowRecord and MenstruationPeriodRecord — there is no separate
  // READ_MENSTRUATION_PERIOD permission. Alias to the real (grantable) string so
  // period reads gated on this constant work once menstruation access is granted.
  static final String readMenstruationPeriod = _read('MENSTRUATION');
  static final String readOvulationTest = _read('OVULATION_TEST');
  static final String readCervicalMucus = _read('CERVICAL_MUCUS');
  static final String readBasalBodyTemperature = _read('BASAL_BODY_TEMPERATURE');
  static final String readIntermenstrualBleeding =
      _read('INTERMENSTRUAL_BLEEDING');
  static final String readSexualActivity = _read('SEXUAL_ACTIVITY');

  static final String writeExercise = _write('EXERCISE');
  static final String writeExerciseRoute = _write('EXERCISE_ROUTE');
  static final String writeSteps = _write('STEPS');
  static final String writeDistance = _write('DISTANCE');
  static final String writeElevation = _write('ELEVATION_GAINED');
  static final String writeActiveCalories = _write('ACTIVE_CALORIES_BURNED');
  static final String writeTotalCalories = _write('TOTAL_CALORIES_BURNED');
  static final String writeHydration = _write('HYDRATION');
  static final String writeNutrition = _write('NUTRITION');
  static final String writeWeight = _write('WEIGHT');
  static final String writeHeight = _write('HEIGHT');
  static final String writeBodyFat = _write('BODY_FAT');
  static final String writeBloodPressure = _write('BLOOD_PRESSURE');
  static final String writeSpO2 = _write('OXYGEN_SATURATION');
  static final String writeRespiratoryRate = _write('RESPIRATORY_RATE');
  static final String writeBodyTemperature = _write('BODY_TEMPERATURE');
  static final String writeMindfulness = _write('MINDFULNESS');
}

/// Feature-availability inputs that gate parts of the taxonomy, mirroring the
/// Kotlin `isMindfulnessSessionAvailable()` / `isSkinTemperatureAvailable()` /
/// `isPlannedExerciseAvailable()` / history / background feature checks.
///
/// Resolved once from [HealthDataSource] and passed in so the taxonomy stays
/// pure and unit-testable.
class HealthConnectFeatureFlags {
  const HealthConnectFeatureFlags({
    this.mindfulnessAvailable = false,
    this.skinTemperatureAvailable = false,
    this.plannedExerciseAvailable = false,
    this.healthDataHistoryAvailable = false,
    this.backgroundReadAvailable = false,
  });

  /// Resolved from the native plugin's `getFeatureStatus("MINDFULNESS_SESSION")`.
  final bool mindfulnessAvailable;

  /// Resolved from `getFeatureStatus("SKIN_TEMPERATURE")`.
  final bool skinTemperatureAvailable;

  /// Resolved from `getFeatureStatus("PLANNED_EXERCISE")`.
  final bool plannedExerciseAvailable;
  final bool healthDataHistoryAvailable;
  final bool backgroundReadAvailable;
}

/// Faithful Dart port of `HealthConnectPermissionService`.
///
/// Pure and synchronous: all sets are derived from the injected
/// [HealthConnectFeatureFlags]. Callers resolve the flags from the platform via
/// [HealthDataSource] and construct this once.
class HealthPermissionService {
  const HealthPermissionService([
    this.flags = const HealthConnectFeatureFlags(),
    this.unsupportedPermissions = const <String>{},
  ]);

  final HealthConnectFeatureFlags flags;

  /// Permissions the installed Health Connect provider does not define — resolved
  /// natively via `filterSupportedPermissions` — and therefore can never be
  /// granted (the app's connect-client is newer than the on-device provider, so
  /// it knows record types like STEPS_CADENCE the provider doesn't). Subtracted
  /// from every permission set below via [_supported] so onboarding and gating
  /// only ever deal with device-supported permissions.
  final Set<String> unsupportedPermissions;

  Set<String> _supported(Set<String> permissions) =>
      unsupportedPermissions.isEmpty
          ? permissions
          : permissions.difference(unsupportedPermissions);

  /// Bump when requestable/managed permissions change so existing users see the
  /// new-permissions prompt. Mirrors the Kotlin constant.
  // ignore: constant_identifier_names
  static const int PERMISSION_SET_VERSION = 2;

  // ── Special (non record-backed) permission strings ────────────────────────
  static final String readExerciseRoutesPermission = _read('EXERCISE_ROUTES');
  static final String writeExerciseRoutePermission = _write('EXERCISE_ROUTE');
  static final String readHealthDataHistoryPermission =
      '${_hcPrefix}READ_HEALTH_DATA_HISTORY';
  static final String readHealthDataInBackgroundPermission =
      '${_hcPrefix}READ_HEALTH_DATA_IN_BACKGROUND';

  // ── Base category sets (mirror the Kotlin `val`s) ─────────────────────────

  Set<String> get corePermissions => _supported({
        _read('STEPS'),
        _read('DISTANCE'),
        _read('EXERCISE'),
        _read('SLEEP'),
      });

  Set<String> get routePermissions =>
      _supported({readExerciseRoutesPermission});

  Set<String> get activityWritePermissions => _supported({
        _write('EXERCISE'),
        _write('DISTANCE'),
        _write('ELEVATION_GAINED'),
        _write('ACTIVE_CALORIES_BURNED'),
        _write('TOTAL_CALORIES_BURNED'),
        writeExerciseRoutePermission,
        _write('HEART_RATE'),
        _write('POWER'),
        _write('SPEED'),
        _write('CYCLING_PEDALING_CADENCE'),
        _write('STEPS_CADENCE'),
      });

  Set<String> get plannedExercisePermissions =>
      _supported(flags.plannedExerciseAvailable
          ? {_read('PLANNED_EXERCISE'), _write('PLANNED_EXERCISE')}
          : <String>{});

  Set<String> get heartPermissions => _supported({
        _read('HEART_RATE'),
        _read('RESTING_HEART_RATE'),
        _read('HEART_RATE_VARIABILITY'),
      });

  Set<String> get bodyPermissions => _supported({
        _read('WEIGHT'),
        _read('HEIGHT'),
        _read('BODY_FAT'),
        _read('LEAN_BODY_MASS'),
        _read('BASAL_METABOLIC_RATE'),
        _read('BONE_MASS'),
        _read('BODY_WATER_MASS'),
      });

  Set<String> get activityExtrasPermissions => _supported({
        _read('FLOORS_CLIMBED'),
        _read('ACTIVE_CALORIES_BURNED'),
        _read('ELEVATION_GAINED'),
        _read('WHEELCHAIR_PUSHES'),
        _read('TOTAL_CALORIES_BURNED'),
        _read('SPEED'),
        _read('POWER'),
        _read('STEPS_CADENCE'),
        _read('CYCLING_PEDALING_CADENCE'),
        ...plannedExercisePermissions,
      });

  Set<String> get nutritionHydrationPermissions => _supported({
        _read('HYDRATION'),
        _read('NUTRITION'),
      });

  Set<String> get hydrationWritePermissions =>
      _supported({_write('HYDRATION')});

  Set<String> get nutritionWritePermissions =>
      _supported({_write('NUTRITION')});

  Set<String> get bodyWritePermissions => _supported({
        _write('WEIGHT'),
        _write('HEIGHT'),
        _write('BODY_FAT'),
      });

  /// Empty when the installed provider does not expose mindfulness sessions.
  ///
  /// Kotlin 1.9.0 (1f2b435) moved the availability check *into* these getters.
  /// Before, the guard was repeated at some call sites and simply forgotten at
  /// others — so [allPermissions] and [managedPermissions] still spread the
  /// mindfulness permissions on devices whose provider does not define them, and
  /// the app would ask for a permission that cannot be granted. Gating here makes
  /// every consumer correct by construction.
  Set<String> get mindfulnessPermissions => flags.mindfulnessAvailable
      ? _supported({_read('MINDFULNESS')})
      : const <String>{};

  Set<String> get mindfulnessWritePermissions => flags.mindfulnessAvailable
      ? _supported({_write('MINDFULNESS')})
      : const <String>{};

  Set<String> get additionalDataAccessPermissions => _supported({
        if (flags.healthDataHistoryAvailable) readHealthDataHistoryPermission,
        if (flags.backgroundReadAvailable)
          readHealthDataInBackgroundPermission,
      });

  Set<String> get vitalsPermissions => _supported({
        _read('BLOOD_PRESSURE'),
        _read('OXYGEN_SATURATION'),
        _read('RESPIRATORY_RATE'),
        _read('BODY_TEMPERATURE'),
        _read('VO2_MAX'),
        _read('BLOOD_GLUCOSE'),
        if (flags.skinTemperatureAvailable) _read('SKIN_TEMPERATURE'),
      });

  Set<String> get vitalsWritePermissions => _supported({
        _write('BLOOD_PRESSURE'),
        _write('OXYGEN_SATURATION'),
        _write('RESPIRATORY_RATE'),
        _write('BODY_TEMPERATURE'),
      });

  Set<String> get dataImportWritePermissions => _supported({
        _write('STEPS'),
        _write('DISTANCE'),
        _write('EXERCISE'),
        writeExerciseRoutePermission,
        _write('ACTIVE_CALORIES_BURNED'),
        _write('TOTAL_CALORIES_BURNED'),
        _write('FLOORS_CLIMBED'),
        _write('ELEVATION_GAINED'),
        _write('WHEELCHAIR_PUSHES'),
        _write('SPEED'),
        _write('HEART_RATE'),
        _write('RESTING_HEART_RATE'),
        _write('HEART_RATE_VARIABILITY'),
        _write('WEIGHT'),
        _write('HEIGHT'),
        _write('BODY_FAT'),
        _write('LEAN_BODY_MASS'),
        _write('BASAL_METABOLIC_RATE'),
        _write('BONE_MASS'),
        _write('BODY_WATER_MASS'),
        _write('HYDRATION'),
        _write('NUTRITION'),
        _write('SLEEP'),
        _write('BLOOD_PRESSURE'),
        _write('OXYGEN_SATURATION'),
        _write('RESPIRATORY_RATE'),
        _write('BODY_TEMPERATURE'),
        _write('BLOOD_GLUCOSE'),
        _write('VO2_MAX'),
        // Gated by the getter itself (Kotlin 1f2b435), not by a guard here.
        ...mindfulnessWritePermissions,
        _write('MENSTRUATION'),
        _write('OVULATION_TEST'),
        _write('CERVICAL_MUCUS'),
        _write('BASAL_BODY_TEMPERATURE'),
        _write('INTERMENSTRUAL_BLEEDING'),
        _write('SEXUAL_ACTIVITY'),
      });

  Set<String> get cyclePermissions => _supported({
        // READ_MENSTRUATION covers both flow and period records (no separate
        // READ_MENSTRUATION_PERIOD permission exists in Health Connect).
        _read('MENSTRUATION'),
        _read('OVULATION_TEST'),
        _read('CERVICAL_MUCUS'),
        _read('BASAL_BODY_TEMPERATURE'),
        _read('INTERMENSTRUAL_BLEEDING'),
        _read('SEXUAL_ACTIVITY'),
      });

  // ── Derived / phased sets ─────────────────────────────────────────────────

  Set<String> get minimumOnboardingPermissions => {
        ...corePermissions,
        ...heartPermissions,
        ...vitalsPermissions,
      };

  Set<String> get phase1Permissions => corePermissions;

  Set<String> get phase2Permissions => {
        ...heartPermissions,
        ...bodyPermissions,
        ...activityExtrasPermissions,
        ...nutritionHydrationPermissions,
        ...mindfulnessPermissions,
      };

  Set<String> get phase3Permissions => vitalsPermissions;

  Set<String> get phase4Permissions => cyclePermissions;

  Set<String> get manualOnlyPermissions => routePermissions;

  Set<String> get requestableAllPermissions => {
        ...phase1Permissions,
        ...phase2Permissions,
      };

  Set<String> get requestableWritePermissions => {
        ...activityWritePermissions,
        ...plannedExercisePermissions,
        ...hydrationWritePermissions,
        ...nutritionWritePermissions,
        ...bodyWritePermissions,
        ...vitalsWritePermissions,
        ...mindfulnessWritePermissions,
      };

  Set<String> get onboardingPermissions => {
        ...requestableAllPermissions,
        ...phase3Permissions,
        ...phase4Permissions,
        ...additionalDataAccessPermissions,
        ...requestableWritePermissions,
        ...dataImportWritePermissions,
      };

  Set<String> get requestableManagedPermissions => {
        ...onboardingPermissions,
        ...phase4Permissions,
      };

  Set<String> get allPermissions => {
        ...requestableAllPermissions,
        ...phase3Permissions,
        ...phase4Permissions,
        ...additionalDataAccessPermissions,
        ...manualOnlyPermissions,
        ...activityWritePermissions,
        ...plannedExercisePermissions,
        ...hydrationWritePermissions,
        ...nutritionWritePermissions,
        ...bodyWritePermissions,
        ...vitalsWritePermissions,
        ...mindfulnessWritePermissions,
        ...dataImportWritePermissions,
      };

  Set<String> get managedPermissions => {
        ...requestableManagedPermissions,
        ...manualOnlyPermissions,
        ...activityWritePermissions,
        ...plannedExercisePermissions,
        ...hydrationWritePermissions,
        ...nutritionWritePermissions,
        ...bodyWritePermissions,
        ...vitalsWritePermissions,
        ...mindfulnessWritePermissions,
        ...dataImportWritePermissions,
      };

  PermissionGrantMode grantModeFor(String permission) =>
      manualOnlyPermissions.contains(permission)
          ? PermissionGrantMode.manual
          : PermissionGrantMode.requestable;

  bool isMindfulnessAvailable() => flags.mindfulnessAvailable;
}
