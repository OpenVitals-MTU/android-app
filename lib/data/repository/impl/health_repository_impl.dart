import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/permission_grant_mode.dart';
import '../../../health/health_data_source.dart';
import '../contract/health_repository.dart';

/// Port of the Kotlin `HealthRepositoryImpl` — a thin facade over the
/// [HealthDataSource] permission taxonomy.
class HealthRepositoryImpl implements HealthRepository {
  HealthRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  @override
  HealthConnectAvailability availability() => _dataSource.cachedAvailability;

  @override
  Future<HealthConnectAvailability> refreshAvailability() async {
    final availability = await _dataSource.availability();
    if (availability == HealthConnectAvailability.available) {
      await _dataSource.resolveFeatureFlags();
      await _dataSource.resolveSupportedPermissions();
    }
    return availability;
  }

  @override
  Future<bool> requestPermissions(Set<String> permissions) =>
      _dataSource.requestPermissions(permissions);

  @override
  Future<bool> openHealthConnectSettings() =>
      _dataSource.openHealthConnectSettings();

  @override
  Set<String> get phase1Permissions =>
      _dataSource.permissionService.phase1Permissions;

  @override
  Set<String> get minimumOnboardingPermissions =>
      _dataSource.permissionService.minimumOnboardingPermissions;

  @override
  Set<String> get phase2Permissions =>
      _dataSource.permissionService.phase2Permissions;

  @override
  Set<String> get phase3Permissions =>
      _dataSource.permissionService.phase3Permissions;

  @override
  Set<String> get phase4Permissions =>
      _dataSource.permissionService.phase4Permissions;

  @override
  Set<String> get corePermissions =>
      _dataSource.permissionService.corePermissions;

  @override
  Set<String> get routePermissions =>
      _dataSource.permissionService.routePermissions;

  @override
  Set<String> get activityWritePermissions =>
      _dataSource.permissionService.activityWritePermissions;

  @override
  Set<String> get heartPermissions =>
      _dataSource.permissionService.heartPermissions;

  @override
  Set<String> get bodyPermissions =>
      _dataSource.permissionService.bodyPermissions;

  @override
  Set<String> get bodyWritePermissions =>
      _dataSource.permissionService.bodyWritePermissions;

  @override
  Set<String> get activityExtrasPermissions =>
      _dataSource.permissionService.activityExtrasPermissions;

  @override
  Set<String> get nutritionHydrationPermissions =>
      _dataSource.permissionService.nutritionHydrationPermissions;

  @override
  Set<String> get hydrationWritePermissions =>
      _dataSource.permissionService.hydrationWritePermissions;

  @override
  Set<String> get mindfulnessPermissions =>
      _dataSource.permissionService.mindfulnessPermissions;

  @override
  Set<String> get mindfulnessWritePermissions =>
      _dataSource.permissionService.mindfulnessWritePermissions;

  @override
  Set<String> get additionalDataAccessPermissions =>
      _dataSource.permissionService.additionalDataAccessPermissions;

  @override
  Set<String> get vitalsPermissions =>
      _dataSource.permissionService.vitalsPermissions;

  @override
  Set<String> get vitalsWritePermissions =>
      _dataSource.permissionService.vitalsWritePermissions;

  @override
  Set<String> get dataImportWritePermissions =>
      _dataSource.permissionService.dataImportWritePermissions;

  @override
  Set<String> get cyclePermissions =>
      _dataSource.permissionService.cyclePermissions;

  @override
  Set<String> get manualOnlyPermissions =>
      _dataSource.permissionService.manualOnlyPermissions;

  @override
  Set<String> get requestableWritePermissions =>
      _dataSource.permissionService.requestableWritePermissions;

  @override
  Set<String> get onboardingPermissions =>
      _dataSource.permissionService.onboardingPermissions;

  @override
  Set<String> get allPermissions => _dataSource.permissionService.allPermissions;

  @override
  Set<String> get managedPermissions =>
      _dataSource.permissionService.managedPermissions;

  @override
  PermissionGrantMode grantModeFor(String permission) =>
      _dataSource.permissionService.grantModeFor(permission);

  @override
  bool isMindfulnessAvailable() => _dataSource.isMindfulnessSessionAvailable();

  @override
  Future<Set<String>> grantedPermissions() => _dataSource.grantedPermissions();

  @override
  Future<Set<String>> missingPhase1() async {
    final granted = await _dataSource.grantedPermissions();
    return phase1Permissions.difference(granted);
  }
}
