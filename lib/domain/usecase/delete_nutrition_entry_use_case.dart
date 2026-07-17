import '../../core/result/result.dart';
import '../../data/repository/contract/nutrition_repository.dart';

/// Deletes one OpenVitals-authored nutrition entry — a drink or meal logged with
/// nutrients but no water (the hydration screen's "nutrition-only" rows), or any
/// standalone nutrition entry.
///
/// Only records this app wrote can be deleted, so the caller is expected to have
/// checked `isOpenVitalsEntry` first. Failure propagates: the screen rolls its
/// optimistic removal back.
class DeleteNutritionEntryUseCase {
  const DeleteNutritionEntryUseCase(this._nutritionRepository);

  final NutritionRepository _nutritionRepository;

  Future<Result<void>> call(String entryId) =>
      _nutritionRepository.deleteNutritionEntry(entryId);
}
