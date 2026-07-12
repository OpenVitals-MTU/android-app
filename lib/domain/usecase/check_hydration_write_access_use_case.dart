import '../../data/repository/contract/hydration_repository.dart';
import '../../data/repository/contract/nutrition_repository.dart';

/// What the hydration entry screen may write.
///
/// Two permission sets, two verdicts, because a logged drink is two records: the
/// volume goes to Health Connect as hydration, its caffeine and macros as
/// nutrition. A user can perfectly well have granted one and refused the other,
/// and the screen has to keep them apart — see `SaveHydrationEntryUseCase`, which
/// downgrades a drink to its water half rather than refusing it outright.
class HydrationEntryWriteAccess {
  const HydrationEntryWriteAccess({
    required this.hydrationPermissions,
    required this.nutritionPermissions,
    required this.canWriteHydration,
    required this.canWriteNutrition,
    this.error,
  });

  final Set<String> hydrationPermissions;
  final Set<String> nutritionPermissions;
  final bool canWriteHydration;
  final bool canWriteNutrition;

  /// Non-null when the probe failed rather than returned a verdict.
  final Object? error;
}

/// Establishes both halves of a drink's write access in one pass.
///
/// One failure sinks both verdicts. The two probes are separate calls, so the
/// second can fail after the first succeeded — but a half-answered question is
/// not an answer: if we cannot establish that the nutrients may be written, the
/// screen must not go on believing it knows what a save will do. It reports the
/// failure and offers both permission sets, which is what the user has to act on
/// either way.
class CheckHydrationWriteAccessUseCase {
  const CheckHydrationWriteAccessUseCase(
    this._hydrationRepository,
    this._nutritionRepository,
  );

  final HydrationRepository _hydrationRepository;
  final NutritionRepository _nutritionRepository;

  Future<HydrationEntryWriteAccess> call() async {
    final hydrationPermissions = _hydrationRepository.hydrationWritePermissions;
    final nutritionPermissions = _nutritionRepository.nutritionWritePermissions;
    try {
      final canWriteHydration =
          await _hydrationRepository.hasHydrationWritePermission();
      final canWriteNutrition =
          await _nutritionRepository.hasNutritionWritePermission();
      return HydrationEntryWriteAccess(
        hydrationPermissions: hydrationPermissions,
        nutritionPermissions: nutritionPermissions,
        canWriteHydration: canWriteHydration,
        canWriteNutrition: canWriteNutrition,
      );
    } catch (error) {
      return HydrationEntryWriteAccess(
        hydrationPermissions: hydrationPermissions,
        nutritionPermissions: nutritionPermissions,
        canWriteHydration: false,
        canWriteNutrition: false,
        error: error,
      );
    }
  }
}
