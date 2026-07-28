// Port of the Kotlin `HealthConnectPermissionService` permission taxonomy.
//
// In the Kotlin source a permission is an AndroidX Health Connect permission
// string (e.g. `android.permission.health.READ_STEPS`). We keep those exact
// string identifiers so the phased sets, `PERMISSION_SET_VERSION`, and any
// persisted permission state remain byte-for-byte faithful to the original.
//
// The native Health Connect plugin ([HealthConnectHostApi]) requests and
// queries these permission strings directly, so no per-record data-type mapping
// is needed here — this is a pure, platform-independent taxonomy, which is why
// it lives in the domain layer and not beside the data source that consumes it.
import '../model/permission_grant_mode.dart';

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
  static final String readPower = _read('POWER');
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

  /// Sleep write access. Already inside
  /// [HealthPermissionService.dataImportWritePermissions], but it had no name to
  /// be checked by — so nothing could ask whether a sleep write was allowed
  /// before attempting one.
  static final String writeSleep = _write('SLEEP');
  static final String writeWeight = _write('WEIGHT');
  static final String writeHeight = _write('HEIGHT');
  static final String writeBodyFat = _write('BODY_FAT');

  // The rest of body composition. Already declared in the manifest and already
  // inside [HealthPermissionService.dataImportWritePermissions], but they had no
  // name to be requested individually by — the typed manual-entry API only ever
  // covered weight/height/body fat, so nothing needed one until the CSV importer
  // wanted to ask for exactly the metrics a file actually maps.
  static final String writeLeanBodyMass = _write('LEAN_BODY_MASS');
  static final String writeBoneMass = _write('BONE_MASS');
  static final String writeBodyWaterMass = _write('BODY_WATER_MASS');
  static final String writeBasalMetabolicRate = _write('BASAL_METABOLIC_RATE');

  // The vitals a CSV can carry that had no constant yet. All five are already
  // declared in the manifest; only the manual-entry screens' narrower needs
  // decided which ones got a name.
  static final String writeRestingHeartRate = _write('RESTING_HEART_RATE');
  static final String writeHrv = _write('HEART_RATE_VARIABILITY');
  static final String writeBasalBodyTemperature =
      _write('BASAL_BODY_TEMPERATURE');
  static final String writeBloodGlucose = _write('BLOOD_GLUCOSE');
  static final String writeVo2Max = _write('VO2_MAX');
  static final String writeBloodPressure = _write('BLOOD_PRESSURE');
  static final String writeSpO2 = _write('OXYGEN_SATURATION');
  static final String writeRespiratoryRate = _write('RESPIRATORY_RATE');
  static final String writeBodyTemperature = _write('BODY_TEMPERATURE');
  static final String writeMindfulness = _write('MINDFULNESS');

  // The sensor series a recorded activity carries alongside its session. These are
  // already in the set the app REQUESTS ([HealthPermissionService.activityWritePermissions]);
  // what they were missing was a name to be checked by, which is why a recording could
  // be saved without ever asking whether its heart rate was allowed through.
  static final String writeHeartRate = _write('HEART_RATE');
  static final String writePower = _write('POWER');
  static final String writeSpeed = _write('SPEED');
  static final String writeCyclingCadence = _write('CYCLING_PEDALING_CADENCE');
  static final String writeStepsCadence = _write('STEPS_CADENCE');
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
    bool? mindfulnessSupportedByDevice,
    this.skinTemperatureAvailable = false,
    this.plannedExerciseAvailable = false,
    this.healthDataHistoryAvailable = false,
    this.backgroundReadAvailable = false,
  }) : mindfulnessSupportedByDevice =
            mindfulnessSupportedByDevice ?? mindfulnessAvailable;

  /// Whether the app may ask for mindfulness: the device reports the feature AND
  /// the user has opted in. Everything that derives a permission set reads this
  /// one, never [mindfulnessSupportedByDevice].
  final bool mindfulnessAvailable;

  /// The device's own answer alone, before the user's opt-in is folded in.
  ///
  /// The *only* legitimate use is deciding whether to offer the opt-in at all: a
  /// phone whose Health Connect has no mindfulness feature should not be shown a
  /// toggle it cannot honour. Deriving permissions from this would defeat the
  /// opt-in — see the crash it exists to avoid, documented on
  /// `HealthConnectNativeDataSource.resolveFeatureFlags`.
  final bool mindfulnessSupportedByDevice;

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

  /// [permissions] minus the ones this device's provider does not define.
  ///
  /// Public because asking for a permission is not the only thing that has to
  /// respect the device: code that REQUIRES a permission before writing must
  /// filter through the same rule, or it demands one that can never be granted
  /// and refuses the write forever — see
  /// `ActivityRepositoryImpl.activityWritePermissionsForRequest`.
  Set<String> supportedOnly(Set<String> permissions) => _supported(permissions);

  Set<String> _supported(Set<String> permissions) =>
      unsupportedPermissions.isEmpty
          ? permissions
          : permissions.difference(unsupportedPermissions);

  /// Bump when requestable/managed permissions change so existing users see the
  /// new-permissions prompt. Mirrors the Kotlin constant.
  ///
  /// Read against `PreferencesRepository.lastPromptedPermissionSetVersion` by the
  /// router: a user whose stamp is older is routed back through onboarding once.
  ///
  /// 3 — onboarding stopped splitting required from optional and now asks for
  /// everything the device can grant in one request.
  // ignore: constant_identifier_names
  static const int PERMISSION_SET_VERSION = 3;

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

  /// Every single-instant measurement the app can import: body composition and
  /// the vitals that are one number at one moment.
  ///
  /// A superset of [bodyWritePermissions], which stops at the three metrics the
  /// manual-entry screens can write. Narrower than [dataImportWritePermissions],
  /// which spans all 38 writes: prompting for exercise routes and menstruation
  /// to import a weight column would be absurd.
  ///
  /// Callers intersect this with the metrics a mapping actually uses. Because
  /// [_supported] has already dropped whatever the installed provider does not
  /// define, that intersection can never request an unsupported permission —
  /// which would throw rather than be refused (invariant #5).
  ///
  /// Blood pressure is deliberately absent: systolic and diastolic must be
  /// written as one record, which the CSV mapping model cannot yet express.
  Set<String> get instantMeasurementWritePermissions => _supported({
        _write('WEIGHT'),
        _write('HEIGHT'),
        _write('BODY_FAT'),
        _write('LEAN_BODY_MASS'),
        _write('BONE_MASS'),
        _write('BODY_WATER_MASS'),
        _write('BASAL_METABOLIC_RATE'),
        _write('HEART_RATE'),
        _write('RESTING_HEART_RATE'),
        _write('HEART_RATE_VARIABILITY'),
        _write('OXYGEN_SATURATION'),
        _write('RESPIRATORY_RATE'),
        _write('BODY_TEMPERATURE'),
        _write('BASAL_BODY_TEMPERATURE'),
        _write('BLOOD_GLUCOSE'),
        _write('VO2_MAX'),
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
        ...cycleWritePermissions,
      });

  /// Write access to the cycle records, split out of
  /// [dataImportWritePermissions] (which still spreads it, so the CSV importer's
  /// set is unchanged) so onboarding can subtract cycle tracking wholesale from
  /// what it requires — see [requiredOnboardingPermissions].
  ///
  /// `WRITE_BASAL_BODY_TEMPERATURE` deliberately appears here *and* in
  /// [instantMeasurementWritePermissions]: it is a cycle record and a single
  /// instant measurement a CSV can carry, and those are different consumers.
  Set<String> get cycleWritePermissions => _supported({
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

  // ── Health Connect data categories ────────────────────────────────────────
  //
  // The seven groups Health Connect itself files records under, each carrying
  // READ **and** WRITE for its records. Onboarding asks by these because they
  // are the exact headers the system permission dialog draws: a screen that
  // says "Activity" produces a dialog that says "Activity".
  //
  // The sets ABOVE are grouped by app feature instead — `heartPermissions`,
  // `activityExtrasPermissions`, `dataImportWritePermissions` — and match
  // nothing the user is about to see. They stay because repositories, the CSV
  // importer and the dashboard all read them; these are additive, not a
  // replacement.
  //
  // Do not build these by spreading the feature sets. Two of them mix
  // directions — [plannedExercisePermissions] carries read AND write, which is
  // how `WRITE_PLANNED_EXERCISE` ends up inside the read-only-sounding
  // [activityExtrasPermissions] — so composing from them produces silently
  // wrong groupings. Members are listed explicitly here for that reason.

  /// Health Connect's **Activity** category.
  ///
  /// VO2 max lives here, not in [vitalsCategoryPermissions]: Health Connect
  /// files it under Activity, and matching the dialog is the whole point of
  /// this grouping — even though the app's own [vitalsPermissions] treats it as
  /// a vital.
  ///
  /// **Route WRITE is here; route READ is not**, and the difference is Health
  /// Connect's, not ours. `WRITE_EXERCISE_ROUTE` is an ordinary toggle in the
  /// Activity group of the write list ("Allowed to write → Activity → Exercise
  /// route"), so it belongs with the rest of Activity. `READ_EXERCISE_ROUTES`
  /// is the awkward one: it lives under *Additional access* and cannot be
  /// granted from the request dialog at all, which is why it stays in
  /// [manualOnlyPermissions] and gets its own walkthrough.
  ///
  /// The two are not even named symmetrically — READ is plural, WRITE singular —
  /// which is a fair warning that they are not a pair.
  Set<String> get activityCategoryPermissions => _supported({
        writeExerciseRoutePermission,
        _read('STEPS'), _write('STEPS'),
        _read('DISTANCE'), _write('DISTANCE'),
        _read('EXERCISE'), _write('EXERCISE'),
        _read('FLOORS_CLIMBED'), _write('FLOORS_CLIMBED'),
        _read('ELEVATION_GAINED'), _write('ELEVATION_GAINED'),
        _read('WHEELCHAIR_PUSHES'), _write('WHEELCHAIR_PUSHES'),
        _read('ACTIVE_CALORIES_BURNED'), _write('ACTIVE_CALORIES_BURNED'),
        _read('TOTAL_CALORIES_BURNED'), _write('TOTAL_CALORIES_BURNED'),
        _read('SPEED'), _write('SPEED'),
        _read('POWER'), _write('POWER'),
        _read('STEPS_CADENCE'), _write('STEPS_CADENCE'),
        _read('CYCLING_PEDALING_CADENCE'), _write('CYCLING_PEDALING_CADENCE'),
        _read('VO2_MAX'), _write('VO2_MAX'),
        // Both directions, and empty as a pair when the provider lacks it.
        ...plannedExercisePermissions,
      });

  /// Health Connect's **Body measurements** category.
  Set<String> get bodyCategoryPermissions => _supported({
        _read('WEIGHT'), _write('WEIGHT'),
        _read('HEIGHT'), _write('HEIGHT'),
        _read('BODY_FAT'), _write('BODY_FAT'),
        _read('LEAN_BODY_MASS'), _write('LEAN_BODY_MASS'),
        _read('BONE_MASS'), _write('BONE_MASS'),
        _read('BODY_WATER_MASS'), _write('BODY_WATER_MASS'),
        _read('BASAL_METABOLIC_RATE'), _write('BASAL_METABOLIC_RATE'),
      });

  /// Health Connect's **Nutrition** category.
  Set<String> get nutritionCategoryPermissions => _supported({
        _read('HYDRATION'), _write('HYDRATION'),
        _read('NUTRITION'), _write('NUTRITION'),
      });

  /// Health Connect's **Sleep** category.
  Set<String> get sleepCategoryPermissions => _supported({
        _read('SLEEP'), _write('SLEEP'),
      });

  /// Health Connect's **Vitals** category.
  ///
  /// Heart rate and its derivatives are vitals here, not a category of their
  /// own — Health Connect has no "Heart" category, whatever [heartPermissions]
  /// suggests. Basal body temperature is a vital too, and so appears in BOTH
  /// this set and [cycleCategoryPermissions]; the overlap is deliberate and
  /// harmless, it just means granting Vitals leaves the cycle row partly
  /// satisfied.
  ///
  /// **Not symmetric.** `SKIN_TEMPERATURE` is read-only: no
  /// `WRITE_SKIN_TEMPERATURE` exists in the manifest or anywhere in this
  /// taxonomy, so it cannot be paired. It is also feature-gated, so on most
  /// devices the question does not arise.
  Set<String> get vitalsCategoryPermissions => _supported({
        _read('HEART_RATE'), _write('HEART_RATE'),
        _read('RESTING_HEART_RATE'), _write('RESTING_HEART_RATE'),
        _read('HEART_RATE_VARIABILITY'), _write('HEART_RATE_VARIABILITY'),
        _read('BLOOD_PRESSURE'), _write('BLOOD_PRESSURE'),
        _read('OXYGEN_SATURATION'), _write('OXYGEN_SATURATION'),
        _read('RESPIRATORY_RATE'), _write('RESPIRATORY_RATE'),
        _read('BODY_TEMPERATURE'), _write('BODY_TEMPERATURE'),
        _read('BASAL_BODY_TEMPERATURE'), _write('BASAL_BODY_TEMPERATURE'),
        _read('BLOOD_GLUCOSE'), _write('BLOOD_GLUCOSE'),
        if (flags.skinTemperatureAvailable) _read('SKIN_TEMPERATURE'),
      });

  /// Health Connect's **Cycle tracking** category.
  ///
  /// Shares basal body temperature with [vitalsCategoryPermissions] — see there.
  Set<String> get cycleCategoryPermissions => {
        ...cyclePermissions,
        ...cycleWritePermissions,
      };

  /// Health Connect's **Mindfulness** category.
  ///
  /// Empty unless the provider has the feature AND the user opted in, and it is
  /// requested on its own, never merged into another category's request — see
  /// [mindfulnessPermissions] for the crash that buys.
  Set<String> get mindfulnessCategoryPermissions => {
        ...mindfulnessPermissions,
        ...mindfulnessWritePermissions,
      };

  // ── Derived / phased sets ─────────────────────────────────────────────────

  /// Everything onboarding will not finish without: **Activity and Sleep**.
  ///
  /// Not everything the app can use — the other categories are asked for and
  /// can be skipped. These two are the floor because the dashboard renders
  /// nothing without them, and because a first run that blocks on all seven
  /// categories is one a user can be trapped in by a single stray refusal.
  ///
  /// Both constituents have already been through [_supported], so this never
  /// contains a permission the installed provider does not define.
  ///
  /// [manualOnlyPermissions] is subtracted as a structural guard. Route READ is
  /// not in either category today, so the subtraction is a no-op — but Activity
  /// is REQUIRED, and anything the request dialog cannot grant would make this
  /// gate unsatisfiable and trap the user in onboarding forever. Keeping the
  /// subtraction means that stays true even if a manual-only permission is
  /// folded into a category later.
  Set<String> get requiredOnboardingPermissions => <String>{
        ...activityCategoryPermissions,
        ...sleepCategoryPermissions,
      }.difference(manualOnlyPermissions);

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

  /// Whether the device could do mindfulness if the user opted in — the input to
  /// "should we offer the toggle", never to "what may we request".
  bool isMindfulnessSupportedByDevice() => flags.mindfulnessSupportedByDevice;
}
