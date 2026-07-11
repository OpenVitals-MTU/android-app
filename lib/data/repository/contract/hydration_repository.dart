import '../../../core/period/period_load_query.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/caffeine_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/hydration_period_data.dart';

/// Port of the Kotlin `HydrationRepository` contract.
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
  Future<List<CustomHydrationDrink>> customHydrationDrinks();

  Future<void> saveCustomHydrationDrink(CustomHydrationDrink drink);

  Future<void> deleteCustomHydrationDrink(String drinkId);

  Future<void> reorderCustomHydrationDrinks(List<String> drinkIds);

  Future<void> moveCustomHydrationDrinkToCategory(
    String drinkId,
    CaffeineSourceCategory? category,
  );

  double hydrationDailyGoalLiters();

  Future<HydrationPeriodData> loadHydrationPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<List<DailyHydration>> loadDailyHydration(LocalDate start, LocalDate end);

  Future<List<HydrationEntry>> loadHydrationEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<bool> hasHydrationWritePermission();

  Future<String> writeHydrationEntry(HydrationWriteRequest request);

  Future<HydrationEntry?> loadHydrationEntry(String id);

  Future<void> updateHydrationEntry(String id, HydrationWriteRequest request);

  Future<void> deleteHydrationEntry(String id);
}
