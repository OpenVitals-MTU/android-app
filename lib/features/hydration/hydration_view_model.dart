import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/period/period_load_query.dart';
import '../../core/period/period_selection.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/model/nutrition_models.dart';
import '../../domain/model/refresh_mode.dart';
import '../../domain/hydration/hydration_entry_merge.dart';

part 'hydration_view_model.freezed.dart';

/// The period hydration summary, a trimmed port of the Kotlin
/// `HydrationPeriodSummary` computed by `HydrationPresentationMapper`.
@freezed
abstract class HydrationSummary with _$HydrationSummary {
  const factory HydrationSummary({
    @Default(0.0) double totalLiters,
    @Default(0) int trackedDays,
    @Default(0) int loggedDays,
    @Default(0.0) double averageLiters,
    @Default(0.0) double bestDayLiters,
    @Default(0) int goalMetDays,
    @Default(0) int goalSuccessRatePercent,
    @Default(0) int currentGoalStreakDays,
    @Default(0) int longestGoalStreakDays,
  }) = _HydrationSummary;
}

/// A single drink-type breakdown slice (drink name + summed litres over the
/// period).
///
/// [label] is null when the drink has no name at all — a bare `HydrationRecord`
/// from another app, which Health Connect gives us as a volume and a package
/// name and nothing else. The screen names those slices; a package name
/// ("tech.mmarca.openvitals") is never a drink name.
@freezed
abstract class HydrationDrinkSlice with _$HydrationDrinkSlice {
  const factory HydrationDrinkSlice({
    required String? label,
    required double liters,
  }) = _HydrationDrinkSlice;
}

/// The Riverpod port of the Kotlin `HydrationUiState`, trimmed to the read-only
/// period detail: the scaffold-driven selection, the loaded daily totals +
/// entries, the resolved daily goal, the precomputed [HydrationSummary] and
/// drink breakdown, and loading/error flags. The reminder-config and
/// quick-add/edit fields are Phase 6 concerns and are omitted.
@freezed
abstract class HydrationState with _$HydrationState {
  const factory HydrationState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    @Default(2.0) double dailyGoalLiters,
    @Default(<DailyHydration>[]) List<DailyHydration> dailyHydration,
    @Default(<HydrationEntry>[]) List<HydrationEntry> entries,
    @Default(HydrationSummary()) HydrationSummary summary,
    @Default(<HydrationDrinkSlice>[]) List<HydrationDrinkSlice> drinkBreakdown,
  }) = _HydrationState;

  const HydrationState._();

  bool get hasData => dailyHydration.any((day) => day.liters > 0.0);
}

/// The Riverpod port of the Kotlin `HydrationViewModel`.
///
/// A manual [Notifier] (no codegen) matching the activity template: the owning
/// [MetricDetailScaffold] drives every load through [load] and pull-to-refresh
/// through [refresh]. Each pass loads the period totals + entries through
/// [LoadHydrationPeriodUseCase], reads the daily goal, and precomputes the period
/// summary + drink breakdown (Kotlin `HydrationPresentationMapper`). A monotonic
/// [_generation] guard drops stale results.
class HydrationViewModel extends Notifier<HydrationState> {
  int _generation = 0;

  @override
  HydrationState build() => HydrationState(selectedDate: LocalDate.now());

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final loadHydrationPeriod = ref.read(loadHydrationPeriodUseCaseProvider);
    // The daily goal is persisted configuration, not a health read, and it is
    // applied to the state *before* the load starts — so a goal just changed in
    // settings shows on the goal card at once, not a round-trip later. That is
    // why the read is synchronous (see [ReadHydrationDailyGoalUseCase]).
    final goal = ref.read(readHydrationDailyGoalUseCaseProvider)();

    state = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      isLoading: true,
      error: null,
      dailyGoalLiters: goal,
    );

    final query = PeriodLoadQuery(
      range: selection.selectedRange,
      anchorDate: selection.selectedDate,
      weekPeriodMode: prefs.weekPeriodMode,
    );

    try {
      // The hydration/nutrition join that puts the drink names back onto the
      // entries is domain work, and lives in the use case.
      final result = await loadHydrationPeriod(query, refreshMode: refreshMode);
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: null,
        dailyHydration: result.dailyHydration,
        entries: result.entries,
        summary: _summarize(result.dailyHydration, goal),
        drinkBreakdown: _drinkBreakdown(result.entries),
      );
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: throwableToScreenError(error, fallback: 'Unable to load data.'),
      );
    }
  }

  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );
}

/// Port of the Kotlin `List<DailyHydration>.summaryForGoal`.
HydrationSummary _summarize(List<DailyHydration> days, double goalLiters) {
  final sorted = [...days]..sort((a, b) => a.date.compareTo(b.date));
  final totalLiters = sorted.fold<double>(0.0, (sum, day) => sum + day.liters);
  final trackedDays = sorted.where((day) => day.liters > 0.0).length;
  bool meetsGoal(DailyHydration day) => goalLiters > 0.0 && day.liters >= goalLiters;
  final goalMetDays = sorted.where(meetsGoal).length;

  var currentGoalStreak = 0;
  var longestGoalStreak = 0;
  for (final day in sorted) {
    if (meetsGoal(day)) {
      currentGoalStreak += 1;
      if (currentGoalStreak > longestGoalStreak) {
        longestGoalStreak = currentGoalStreak;
      }
    } else {
      currentGoalStreak = 0;
    }
  }

  final reversed = sorted.reversed.toList();
  var trailingGoalStreak = 0;
  for (final day in reversed) {
    if (!meetsGoal(day)) break;
    trailingGoalStreak += 1;
  }

  final bestDay = sorted.isEmpty
      ? 0.0
      : sorted.map((day) => day.liters).reduce((a, b) => a > b ? a : b);

  return HydrationSummary(
    totalLiters: totalLiters,
    trackedDays: trackedDays,
    loggedDays: sorted.length,
    averageLiters: trackedDays > 0 ? totalLiters / trackedDays : 0.0,
    bestDayLiters: bestDay,
    goalMetDays: goalMetDays,
    goalSuccessRatePercent:
        trackedDays > 0 ? (goalMetDays * 100 ~/ trackedDays) : 0,
    currentGoalStreakDays: trailingGoalStreak,
    longestGoalStreakDays: longestGoalStreak,
  );
}

/// Groups hydration entries by drink name, summing litres. Only
/// hydration-bearing entries are counted (nutrition-only entries carry no
/// volume).
///
/// The name comes from the paired nutrition record (see
/// [mergeHydrationAndNutrition]); drinks with no name group together under a
/// single null-labelled slice, which the screen titles. It must never fall back
/// to [HydrationEntry.source] — that is the originating *package*, so an entry
/// this very app wrote rendered as "tech.mmarca.openvitals".
List<HydrationDrinkSlice> _drinkBreakdown(List<HydrationEntry> entries) {
  final byLabel = <String?, double>{};
  for (final entry in entries) {
    if (entry.liters <= 0.0) continue;
    final displayName = entry.displayName?.trim();
    final label =
        (displayName != null && displayName.isNotEmpty) ? displayName : null;
    byLabel[label] = (byLabel[label] ?? 0.0) + entry.liters;
  }
  final slices = byLabel.entries
      .map((e) => HydrationDrinkSlice(label: e.key, liters: e.value))
      .toList()
    ..sort((a, b) => b.liters.compareTo(a.liters));
  return slices;
}

/// The hydration screen's state provider. A manually-declared [NotifierProvider]
/// (no codegen), matching the activity template.
final hydrationProvider =
    NotifierProvider<HydrationViewModel, HydrationState>(HydrationViewModel.new);
