import '../../core/result/result.dart';
import '../../data/repository/contract/health_repository.dart';

/// Puts the Health Connect permission dialog in front of the user.
///
/// Nothing more: the *result* is deliberately not read here. A screen that asked
/// for permissions has to re-resolve its whole world afterwards anyway — the
/// granted-permission providers, the availability, its own data — and a granted
/// set returned from here would be a fourth source of truth that is stale the
/// moment it is handed over. Callers that need the outcome as a value use
/// `GrantOnboardingPermissionsUseCase`, which is a different question.
class RequestHealthPermissionsUseCase {
  const RequestHealthPermissionsUseCase(this._healthRepository);

  final HealthRepository _healthRepository;

  /// Whether the request completed (not whether anything was granted).
  Future<Result<bool>> call(Set<String> permissions) =>
      _healthRepository.requestPermissions(permissions);
}
