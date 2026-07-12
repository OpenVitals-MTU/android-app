import '../../core/result/result.dart';
import '../../data/repository/contract/health_repository.dart';
import '../model/health_connect_availability.dart';

/// What came of putting the permission dialog in front of the user.
class HealthPermissionGrant {
  const HealthPermissionGrant({
    required this.grantedPermissions,
    required this.needsManualGrant,
  });

  /// The full granted set afterwards — not just the ones asked for.
  final Set<String> grantedPermissions;

  /// The dialog achieved nothing and the permissions are still missing, so the
  /// only way through is the Health Connect settings page. The caller opens it;
  /// it is not opened here, so that the caller can publish the new granted set
  /// *before* it disappears into another app's UI.
  final bool needsManualGrant;
}

/// Requests a group of permissions and works out whether the dialog was any use.
///
/// The dialog is not honest about failure. Health Connect reports some
/// permissions as non-requestable — exercise routes, background reads, history
/// access — and for those the dialog simply returns, having done nothing, exactly
/// as it does when the user says no. The two are told apart the only way they can
/// be: by comparing the granted set before and after.
///
/// Nothing gained *and* still not everything asked for is the case that needs the
/// settings page. Nothing gained but everything already held is a user re-tapping
/// a category they granted earlier, and must not fling them into Health Connect
/// for no reason.
class GrantOnboardingPermissionsUseCase {
  const GrantOnboardingPermissionsUseCase(this._healthRepository);

  final HealthRepository _healthRepository;

  Future<Result<HealthPermissionGrant>> call(Set<String> permissions) async {
    // STRICT: the verdict is a before/after diff, so it is wrong with half the
    // evidence — any failed step sinks the grant, as the throwing flow did.
    final beforeRead =
        _healthRepository.availability() == HealthConnectAvailability.available
            ? await _healthRepository.grantedPermissions()
            : const Ok(<String>{});
    return beforeRead.flatMap((before) async {
      final requested = await _healthRepository.requestPermissions(permissions);
      return requested.flatMap((_) async {
        final grantedRead = await _healthRepository.grantedPermissions();
        return grantedRead.map((granted) {
          final gainedAny = permissions
              .any((permission) =>
                  granted.contains(permission) && !before.contains(permission));
          return HealthPermissionGrant(
            grantedPermissions: granted,
            needsManualGrant: !gainedAny && !permissions.every(granted.contains),
          );
        });
      });
    });
  }
}
