import '../../core/result/result.dart';
import '../../data/repository/contract/health_repository.dart';
import '../model/health_connect_availability.dart';

/// Where onboarding stands: can this device store health data at all, what has it
/// already granted, and does its Health Connect do mindfulness.
class OnboardingHealthState {
  const OnboardingHealthState({
    required this.availability,
    required this.grantedPermissions,
    required this.mindfulnessAvailable,
    required this.mindfulnessSupportedByDevice,
  });

  final HealthConnectAvailability availability;
  final Set<String> grantedPermissions;

  /// Device feature AND the user's opt-in — what decides whether mindfulness may
  /// be asked for.
  final bool mindfulnessAvailable;

  /// The device's answer on its own — what decides whether the opt-in is worth
  /// offering.
  final bool mindfulnessSupportedByDevice;
}

/// Resolves everything onboarding needs before it can draw a single row.
///
/// Availability is *refreshed*, not read: the cached value is still
/// `notSupported` on a cold start, and onboarding is the first screen there is.
/// Reading it too early would tell a perfectly healthy phone that it cannot store
/// health data.
///
/// It also short-circuits. On a device without Health Connect there is nothing to
/// have been granted and no optional feature to probe, and both of those questions
/// are platform round-trips — so they are not asked. That ordering is the whole
/// reason the three answers come back together rather than as three use cases.
class CheckOnboardingStateUseCase {
  const CheckOnboardingStateUseCase(this._healthRepository);

  final HealthRepository _healthRepository;

  Future<Result<OnboardingHealthState>> call() async {
    final refreshed = await _healthRepository.refreshAvailability();
    // STRICT throughout: every answer here is content — onboarding cannot
    // draw a row without all of them — so the first failure sinks the load,
    // exactly as the pre-Result throwing flow did.
    return refreshed.flatMap((availability) async {
      if (availability != HealthConnectAvailability.available) {
        return Ok(OnboardingHealthState(
          availability: availability,
          grantedPermissions: const <String>{},
          mindfulnessAvailable: false,
          mindfulnessSupportedByDevice: false,
        ));
      }
      final mindfulnessAvailable = _healthRepository.isMindfulnessAvailable();
      final mindfulnessSupportedByDevice =
          _healthRepository.isMindfulnessSupportedByDevice();
      final granted = await _healthRepository.grantedPermissions();
      return granted.map((granted) => OnboardingHealthState(
            availability: availability,
            grantedPermissions: granted,
            mindfulnessAvailable: mindfulnessAvailable,
            mindfulnessSupportedByDevice: mindfulnessSupportedByDevice,
          ));
    });
  }
}
