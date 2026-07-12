import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/nutrition_period_data.dart';

/// Port of the Kotlin `NutritionRepository` contract.
///
/// Fallible operations return [Result]; the synchronous probe
/// ([nutritionWritePermissions]) reads cached state and cannot fail, so it
/// stays bare.
abstract interface class NutritionRepository {
  Set<String> get nutritionWritePermissions;

  Future<Result<NutritionPeriodData>> loadNutritionPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<Result<List<DailyMacros>>> loadDailyMacros(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<List<NutritionEntry>>> loadNutritionEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<bool>> hasNutritionWritePermission();

  Future<Result<String>> writeCarbsEntry(NutritionWriteRequest request);

  Future<Result<String>> writeNutritionEntry(NutritionWriteRequest request);

  Future<Result<void>> deleteNutritionEntry(String id);
}
