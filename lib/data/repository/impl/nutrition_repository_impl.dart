import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/nutrition_period_data.dart';
import '../../source/health/health_data_source.dart';
import '../../source/health/health_permissions.dart';
import '../contract/nutrition_repository.dart';
import '../contract/repository_exceptions.dart';
import 'repository_time.dart';
import 'health_connect_gating.dart';
import 'run_catching.dart';

/// Port of the Kotlin `NutritionRepositoryImpl`.
///
/// Public methods convert exceptions to failures via [runCatching] at the
/// boundary; the private `_raw` bodies keep the original throwing flow so
/// internal composition stays plain awaits.
class NutritionRepositoryImpl implements NutritionRepository {
  NutritionRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  @override
  Set<String> get nutritionWritePermissions => {HcPermissions.writeNutrition};

  @override
  Future<Result<NutritionPeriodData>> loadNutritionPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
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
                ? await _dataSource.readDailyMacros(
                    w.current.start, w.current.end)
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
      });

  @override
  Future<Result<List<DailyMacros>>> loadDailyMacros(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readNutrition)) return const [];
        return _dataSource.readDailyMacros(start, end);
      });

  @override
  Future<Result<List<NutritionEntry>>> loadNutritionEntries(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readNutrition)) return const [];
        return _dataSource.readNutritionEntries(
            localDayStart(start), localDayEnd(end));
      });

  @override
  Future<Result<bool>> hasNutritionWritePermission() =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        return granted.containsAll(nutritionWritePermissions);
      });

  @override
  Future<Result<String>> writeCarbsEntry(NutritionWriteRequest request) =>
      runCatching(() => _writeNutritionEntryRaw(request));

  @override
  Future<Result<String>> writeNutritionEntry(NutritionWriteRequest request) =>
      runCatching(() => _writeNutritionEntryRaw(request));

  Future<String> _writeNutritionEntryRaw(NutritionWriteRequest request) async {
    await _requireWrite();
    return _dataSource.writeNutritionEntry(request);
  }

  @override
  Future<Result<void>> deleteNutritionEntry(String id) =>
      runCatching(() async {
        await _requireWrite();
        await _dataSource.deleteNutritionEntry(id);
      });

  Future<void> _requireWrite() async {
    final granted = await _dataSource.grantedIfAvailable();
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
