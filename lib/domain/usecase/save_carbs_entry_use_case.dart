import '../../core/result/result.dart';
import '../../data/repository/contract/nutrition_repository.dart';
import '../model/nutrition_models.dart';

/// Writes a manual carbohydrate entry.
///
/// Carbs get their own write path rather than going through the general
/// nutrition write because they are logged for a different reason — a diabetic's
/// intake log, not a meal — and they are written with no drink and no other
/// nutrients attached.
class SaveCarbsEntryUseCase {
  const SaveCarbsEntryUseCase(this._nutritionRepository);

  final NutritionRepository _nutritionRepository;

  Future<Result<void>> call(NutritionWriteRequest request) async {
    final written = await _nutritionRepository.writeCarbsEntry(request);
    return written.map((_) {});
  }
}
