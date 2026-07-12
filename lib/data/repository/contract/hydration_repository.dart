import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/caffeine_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/hydration_period_data.dart';

/// Port of the Kotlin `HydrationRepository` contract.
///
/// Fallible operations return [Result]; the synchronous probes (the write
/// permission set, the preference-backed container/goal reads and writes)
/// read cached state and cannot fail, so they stay bare.
abstract interface class HydrationRepository {
  Set<String> get hydrationWritePermissions;

  Map<String, double> hydrationContainerVolumeMilliliters();

  void setHydrationContainerVolumeMilliliters(
    String containerId,
    double milliliters,
  );

  double? lastCustomHydrationAmountMilliliters();

  void setLastCustomHydrationAmountMilliliters(double milliliters);

  /// The drink catalog: the seeded presets (still/gasified water plus the
  /// CaffeineHealth beverage list) merged with the user's own drinks.
  ///
  /// Asynchronous because the drift-backed `BeverageStore` is. The Kotlin
  /// `beverageStore.beverages()` is a blocking Room read, which is why its
  /// contract is synchronous.
  Future<Result<List<CustomHydrationDrink>>> customHydrationDrinks();

  Future<Result<void>> saveCustomHydrationDrink(CustomHydrationDrink drink);

  Future<Result<void>> deleteCustomHydrationDrink(String drinkId);

  Future<Result<void>> reorderCustomHydrationDrinks(List<String> drinkIds);

  Future<Result<void>> moveCustomHydrationDrinkToCategory(
    String drinkId,
    CaffeineSourceCategory? category,
  );

  double hydrationDailyGoalLiters();

  Future<Result<HydrationPeriodData>> loadHydrationPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<Result<List<DailyHydration>>> loadDailyHydration(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<List<HydrationEntry>>> loadHydrationEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<bool>> hasHydrationWritePermission();

  Future<Result<String>> writeHydrationEntry(HydrationWriteRequest request);

  Future<Result<HydrationEntry?>> loadHydrationEntry(String id);

  Future<Result<void>> updateHydrationEntry(
    String id,
    HydrationWriteRequest request,
  );

  Future<Result<void>> deleteHydrationEntry(String id);
}
