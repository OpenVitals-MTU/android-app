import '../../data/repository/contract/health_repository.dart';

/// The permissions Health Connect currently reports as granted.
///
/// Worth re-asking on every resume, not just at startup: the user can leave for
/// the Health Connect settings page and come back having granted (or revoked)
/// anything, and the app is never told. Polling on return is the only signal
/// there is.
class LoadGrantedHealthPermissionsUseCase {
  const LoadGrantedHealthPermissionsUseCase(this._healthRepository);

  final HealthRepository _healthRepository;

  Future<Set<String>> call() => _healthRepository.grantedPermissions();
}
