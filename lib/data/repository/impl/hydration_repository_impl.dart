import 'dart:async';

import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../local/beverage/beverage_store.dart';
import '../../prefs/preferences_repository.dart';
import '../../../domain/model/caffeine_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/hydration_period_data.dart';
import '../../source/health/health_data_source.dart';
import '../../../domain/health/health_permissions.dart';
import '../contract/hydration_repository.dart';
import '../contract/repository_exceptions.dart';
import 'repository_time.dart';
import 'health_connect_gating.dart';
import 'run_catching.dart';

/// Port of the Kotlin `HydrationRepositoryImpl`.
///
/// Custom-drink state uses the (synchronous) [PreferencesRepository]; category
/// moves delegate to the (async) [BeverageStore] when present, fired without
/// awaiting to preserve the contract's synchronous `void` shape (matching the
/// Kotlin `apply()` semantics).
///
/// Public async methods convert exceptions to failures via [runCatching] at
/// the boundary.
class HydrationRepositoryImpl implements HydrationRepository {
  HydrationRepositoryImpl(
    this._dataSource, {
    PreferencesRepository? preferencesRepository,
    BeverageStore? beverageStore,
  })  : _preferences = preferencesRepository,
        _beverages = beverageStore;

  final HealthDataSource _dataSource;
  final PreferencesRepository? _preferences;
  final BeverageStore? _beverages;

  @override
  Set<String> get hydrationWritePermissions => {HcPermissions.writeHydration};

  @override
  Map<String, double> hydrationContainerVolumeMilliliters() =>
      _preferences?.hydrationContainerVolumeMilliliters() ?? <String, double>{};

  @override
  void setHydrationContainerVolumeMilliliters(
    String containerId,
    double milliliters,
  ) =>
      _preferences?.setHydrationContainerVolumeMilliliters(
          containerId, milliliters);

  @override
  double? lastCustomHydrationAmountMilliliters() =>
      _preferences?.lastCustomHydrationAmountMilliliters();

  @override
  void setLastCustomHydrationAmountMilliliters(double milliliters) =>
      _preferences?.setLastCustomHydrationAmountMilliliters(milliliters);

  /// The beverage store is the source of truth when it is wired: it seeds the
  /// preset catalog on first read and migrates any drinks the preferences
  /// repository holds from before it existed. Preferences are the fallback for
  /// contexts with no database (the reminder's background isolate).
  ///
  /// Port of the Kotlin `beverageStore?.beverages() ?: preferencesRepository…`.
  @override
  Future<Result<List<CustomHydrationDrink>>> customHydrationDrinks() =>
      runCatching(() async {
        final beverages = _beverages;
        if (beverages != null) return beverages.beverages();
        return _preferences?.customHydrationDrinks() ??
            const <CustomHydrationDrink>[];
      });

  @override
  Future<Result<void>> saveCustomHydrationDrink(CustomHydrationDrink drink) =>
      runCatching(() async {
        final beverages = _beverages;
        if (beverages != null) return beverages.save(drink);
        _preferences?.saveCustomHydrationDrink(drink);
      });

  @override
  Future<Result<void>> deleteCustomHydrationDrink(String drinkId) =>
      runCatching(() async {
        final beverages = _beverages;
        if (beverages != null) return beverages.delete(drinkId);
        _preferences?.deleteCustomHydrationDrink(drinkId);
      });

  @override
  Future<Result<void>> reorderCustomHydrationDrinks(List<String> drinkIds) =>
      runCatching(() async {
        final beverages = _beverages;
        if (beverages != null) return beverages.reorder(drinkIds);
        _preferences?.reorderCustomHydrationDrinks(drinkIds);
      });

  @override
  Future<Result<void>> moveCustomHydrationDrinkToCategory(
    String drinkId,
    CaffeineSourceCategory? category,
  ) =>
      runCatching(() async {
        final beverages = _beverages;
        if (beverages != null) {
          return beverages.moveToCategory(drinkId, category);
        }
        // Preferences have no category column; nothing to persist.
      });

  @override
  double hydrationDailyGoalLiters() =>
      _preferences?.hydrationDailyGoalLiters ?? 2.0;

  @override
  Future<Result<HydrationPeriodData>> loadHydrationPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        final hasPerm = granted.contains(HcPermissions.readHydration);
        final w = query.windows;
        final isDay = query.range == TimeRange.day;

        final entries = hasPerm
            ? await _dataSource.readHydrationEntries(
                localDayStart(w.current.start), localDayEnd(w.current.end))
            : const <HydrationEntry>[];

        final dailyHydration = isDay
            ? _hydrationForDay(entries, query.selectedDate)
            : (hasPerm
                ? await _dataSource.readDailyHydration(
                    w.current.start, w.current.end)
                : const <DailyHydration>[]);
        final previous = hasPerm
            ? await _dataSource.readDailyHydration(
                w.previous.start, w.previous.end)
            : const <DailyHydration>[];
        final baseline = hasPerm
            ? await _dataSource.readDailyHydration(
                w.baseline.start, w.baseline.end)
            : const <DailyHydration>[];

        return HydrationPeriodData(
          dailyHydration: dailyHydration,
          previousDailyHydration: previous,
          baselineDailyHydration: baseline,
          hydrationEntries: entries,
        );
      });

  @override
  Future<Result<List<DailyHydration>>> loadDailyHydration(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readHydration)) return const [];
        return _dataSource.readDailyHydration(start, end);
      });

  @override
  Future<Result<List<HydrationEntry>>> loadHydrationEntries(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readHydration)) return const [];
        return _dataSource.readHydrationEntries(
            localDayStart(start), localDayEnd(end));
      });

  @override
  Future<Result<bool>> hasHydrationWritePermission() =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        return granted.containsAll(hydrationWritePermissions);
      });

  @override
  Future<Result<String>> writeHydrationEntry(HydrationWriteRequest request) =>
      runCatching(() async {
        await _requireWrite();
        return _dataSource.writeHydrationEntry(request);
      });

  @override
  Future<Result<HydrationEntry?>> loadHydrationEntry(String id) =>
      runCatching(() => _dataSource.readHydrationEntry(id));

  @override
  Future<Result<void>> updateHydrationEntry(
    String id,
    HydrationWriteRequest request,
  ) =>
      runCatching(() async {
        await _requireWrite();
        await _dataSource.updateHydrationEntry(id, request);
      });

  @override
  Future<Result<void>> deleteHydrationEntry(String id) =>
      runCatching(() async {
        await _requireWrite();
        final clientRecordId = await _dataSource.deleteHydrationEntry(id);
        if (clientRecordId != null) {
          final granted = await _dataSource.grantedIfAvailable();
          if (granted.contains(HcPermissions.writeNutrition)) {
            await _dataSource.deleteHydrationNutritionEntry(clientRecordId);
          }
        }
      });

  @override
  Future<Result<void>> deleteHydrationEntryByClientRecordId(
    String clientRecordId,
  ) =>
      runCatching(() async {
        await _requireWrite();
        await _dataSource.deleteHydrationEntryByClientRecordId(clientRecordId);
      });

  Future<void> _requireWrite() async {
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.containsAll(hydrationWritePermissions)) {
      throw const MissingHealthPermissionException(
        'Missing Health Connect hydration write permission.',
      );
    }
  }

  List<DailyHydration> _hydrationForDay(
    List<HydrationEntry> entries,
    LocalDate selectedDate,
  ) {
    var liters = 0.0;
    for (final entry in entries) {
      final date = LocalDate.fromDateTime(entry.startTime.toLocal());
      if (date == selectedDate) liters += entry.liters;
    }
    if (liters <= 0) return const [];
    return [DailyHydration(date: selectedDate, liters: liters)];
  }
}
