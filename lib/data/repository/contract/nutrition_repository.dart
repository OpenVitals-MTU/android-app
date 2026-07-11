import '../../../core/period/period_load_query.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/nutrition_period_data.dart';

/// Port of the Kotlin `NutritionRepository` contract.
abstract interface class NutritionRepository {
  Set<String> get nutritionWritePermissions;

  Future<NutritionPeriodData> loadNutritionPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<List<DailyMacros>> loadDailyMacros(LocalDate start, LocalDate end);

  Future<List<NutritionEntry>> loadNutritionEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<bool> hasNutritionWritePermission();

  Future<String> writeCarbsEntry(NutritionWriteRequest request);

  Future<String> writeNutritionEntry(NutritionWriteRequest request);

  Future<void> deleteNutritionEntry(String id);
}
