import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/nutrition_period_data.dart';
import '../../../health/health_data_source.dart';
import '../../../health/health_permissions.dart';
import '../contract/nutrition_repository.dart';
import 'repository_exceptions.dart';
import 'repository_time.dart';

/// Port of the Kotlin `NutritionRepositoryImpl`.
class NutritionRepositoryImpl implements NutritionRepository {
  NutritionRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  Future<Set<String>> _grantedIfAvailable() async =>
      _dataSource.cachedAvailability == HealthConnectAvailability.available
          ? _dataSource.grantedPermissions()
          : <String>{};

  @override
  Set<String> get nutritionWritePermissions => {HcPermissions.writeNutrition};

  @override
  Future<NutritionPeriodData> loadNutritionPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final granted = await _grantedIfAvailable();
    final hasPerm = granted.contains(HcPermissions.readNutrition);
    final w = query.windows;
    final isDay = query.range == TimeRange.day;

    final entries = hasPerm
        ? await _dataSource.readNutritionEntries(
            localDayStart(w.current.start), localDayEnd(w.current.end))
        : const <NutritionEntry>[];

    final dailyMacros = isDay
        ? _macrosForDay(entries, query.selectedDate)
        : (hasPerm
            ? await _dataSource.readDailyMacros(w.current.start, w.current.end)
            : const <DailyMacros>[]);
    final previous = hasPerm
        ? await _dataSource.readDailyMacros(w.previous.start, w.previous.end)
        : const <DailyMacros>[];
    final baseline = hasPerm
        ? await _dataSource.readDailyMacros(w.baseline.start, w.baseline.end)
        : const <DailyMacros>[];

    return NutritionPeriodData(
      dailyMacros: dailyMacros,
      previousDailyMacros: previous,
      baselineDailyMacros: baseline,
      entries: entries,
    );
  }

  @override
  Future<List<DailyMacros>> loadDailyMacros(LocalDate start, LocalDate end) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readNutrition)) return const [];
    return _dataSource.readDailyMacros(start, end);
  }

  @override
  Future<List<NutritionEntry>> loadNutritionEntries(
    LocalDate start,
    LocalDate end,
  ) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readNutrition)) return const [];
    return _dataSource.readNutritionEntries(localDayStart(start), localDayEnd(end));
  }

  @override
  Future<bool> hasNutritionWritePermission() async {
    final granted = await _grantedIfAvailable();
    return granted.containsAll(nutritionWritePermissions);
  }

  @override
  Future<String> writeCarbsEntry(NutritionWriteRequest request) =>
      writeNutritionEntry(request);

  @override
  Future<String> writeNutritionEntry(NutritionWriteRequest request) async {
    await _requireWrite();
    return _dataSource.writeNutritionEntry(request);
  }

  @override
  Future<void> deleteNutritionEntry(String id) async {
    await _requireWrite();
    await _dataSource.deleteNutritionEntry(id);
  }

  Future<void> _requireWrite() async {
    final granted = await _grantedIfAvailable();
    if (!granted.containsAll(nutritionWritePermissions)) {
      throw const MissingHealthPermissionException(
        'Missing Health Connect nutrition write permission.',
      );
    }
  }

  /// Sums a single day's entries into one [DailyMacros] (Kotlin
  /// `entries.toDailyMacrosForDay`).
  List<DailyMacros> _macrosForDay(
    List<NutritionEntry> entries,
    LocalDate selectedDate,
  ) {
    final totals = <NutritionNutrient, double>{};
    for (final entry in entries) {
      final entryDate = LocalDate.fromDateTime(entry.time.toLocal());
      if (entryDate != selectedDate) continue;
      entry.nutrientValues.forEach((nutrient, value) {
        totals[nutrient] = (totals[nutrient] ?? 0) + value;
      });
    }
    if (totals.isEmpty) return const [];
    return [DailyMacros(date: selectedDate, nutrientValues: totals)];
  }
}
