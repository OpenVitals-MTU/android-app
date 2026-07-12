import '../../core/result/result.dart';
import '../../data/repository/contract/health_repository.dart';

/// Opens this app's page in Health Connect.
///
/// The escape hatch for the permissions the runtime dialog will not grant —
/// exercise routes, background reads, history access. Health Connect reports them
/// as non-requestable rather than denied, so the dialog simply comes back having
/// done nothing, and the only way through is the settings page.
class OpenHealthConnectSettingsUseCase {
  const OpenHealthConnectSettingsUseCase(this._healthRepository);

  final HealthRepository _healthRepository;

  /// Whether a page was actually launched.
  Future<Result<bool>> call() => _healthRepository.openHealthConnectSettings();
}
