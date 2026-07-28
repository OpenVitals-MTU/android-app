import '../../../core/result/result.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/permission_grant_mode.dart';

/// Port of the Kotlin `HealthRepository` contract.
///
/// The stable permission API the feature/state layers depend on. Permissions
/// are AndroidX Health Connect permission strings (see
/// `lib/health/health_permissions.dart`).
///
/// Fallible operations (the ones that cross the async platform boundary)
/// return [Result]; the synchronous probes — [availability], the permission-set
/// getters, [grantModeFor], [isMindfulnessAvailable] — read cached state and
/// cannot fail, so they stay bare.
///
/// Note: the Kotlin `permissionContract()` returns an Android
/// `ActivityResultContract`; the Dart `health` package instead requests
/// authorization imperatively, so that method is replaced by
/// [requestPermissions].
abstract interface class HealthRepository {
  /// The last-resolved availability (synchronous, cached). Call
  /// [refreshAvailability] once at startup / before onboarding reads this.
  HealthConnectAvailability availability();

  /// Asynchronously resolves Health Connect availability from the platform
  /// (populating the cache read by [availability]) and, when available, the
  /// optional-feature flags (mindfulness / skin temperature / planned exercise).
  /// The Kotlin `HealthConnectManager.availability()` was synchronous; the
  /// platform SDK-status check crosses an async plugin boundary here, so callers
  /// that need a fresh value must await this.
  Future<Result<HealthConnectAvailability>> refreshAvailability();

  /// Requests OS authorization for [permissions]; returns whether the request
  /// completed successfully. Replaces the Kotlin `permissionContract()`.
  Future<Result<bool>> requestPermissions(Set<String> permissions);

  /// Opens the Health Connect page for this app so the user can manually grant
  /// permissions the runtime dialog reports as non-requestable (planned
  /// exercise, exercise routes, background/history access). Returns whether a
  /// page was launched.
  Future<Result<bool>> openHealthConnectSettings();

  Set<String> get phase1Permissions;

  /// Everything onboarding refuses to finish without — every permission the
  /// runtime dialog can grant on this device, minus the groups that are opt-in
  /// or cannot be granted by the dialog at all. See
  /// `HealthPermissionService.requiredOnboardingPermissions`.
  Set<String> get requiredOnboardingPermissions;
  Set<String> get phase2Permissions;
  Set<String> get phase3Permissions;
  Set<String> get phase4Permissions;
  Set<String> get corePermissions;
  Set<String> get routePermissions;
  Set<String> get activityWritePermissions;
  Set<String> get heartPermissions;
  Set<String> get bodyPermissions;
  Set<String> get bodyWritePermissions;

  /// Every single-instant measurement the app can import — body composition and
  /// the one-number-at-one-moment vitals. Wider than [bodyWritePermissions]
  /// (which stops where manual entry does), far narrower than
  /// [dataImportWritePermissions].
  Set<String> get instantMeasurementWritePermissions;
  Set<String> get activityExtrasPermissions;
  Set<String> get nutritionHydrationPermissions;
  Set<String> get hydrationWritePermissions;
  Set<String> get mindfulnessPermissions;
  Set<String> get mindfulnessWritePermissions;
  Set<String> get additionalDataAccessPermissions;
  Set<String> get vitalsPermissions;
  Set<String> get vitalsWritePermissions;
  Set<String> get dataImportWritePermissions;
  Set<String> get cyclePermissions;

  /// Write access to the cycle records. Split out of
  /// [dataImportWritePermissions] (which still contains it) so onboarding can
  /// keep cycle tracking out of what it requires.
  Set<String> get cycleWritePermissions;

  // ── Health Connect data categories ──────────────────────────────────────
  //
  // The seven groups Health Connect files records under, each carrying read AND
  // write. Onboarding asks by these because they are the headers the system
  // dialog draws; the feature-shaped sets above match nothing the user sees.
  // See `HealthPermissionService` for the two documented irregularities
  // (skin temperature is read-only; basal body temperature is in both vitals
  // and cycle).

  Set<String> get activityCategoryPermissions;
  Set<String> get bodyCategoryPermissions;
  Set<String> get nutritionCategoryPermissions;
  Set<String> get sleepCategoryPermissions;
  Set<String> get vitalsCategoryPermissions;
  Set<String> get cycleCategoryPermissions;
  Set<String> get mindfulnessCategoryPermissions;
  Set<String> get manualOnlyPermissions;
  Set<String> get requestableWritePermissions;
  Set<String> get onboardingPermissions;
  Set<String> get allPermissions;
  Set<String> get managedPermissions;

  PermissionGrantMode grantModeFor(String permission);

  bool isMindfulnessAvailable();

  /// Whether the installed Health Connect has the mindfulness feature at all,
  /// before the user's opt-in is taken into account. The only thing that may
  /// read this is the UI deciding whether to offer that opt-in — permission sets
  /// derive from [isMindfulnessAvailable].
  bool isMindfulnessSupportedByDevice();

  Future<Result<Set<String>>> grantedPermissions();

  Future<Result<Set<String>>> missingPhase1();
}
