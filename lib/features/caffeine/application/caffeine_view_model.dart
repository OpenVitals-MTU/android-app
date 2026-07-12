import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../core/period/time_range.dart';
import '../../../di/providers.dart';
import '../../../domain/insights/caffeine_insight_calculator.dart';
import '../../../domain/model/caffeine_models.dart';
import '../../../domain/model/refresh_mode.dart';

part 'caffeine_view_model.freezed.dart';

/// The analytics window the caffeine screen aggregates over. Port of the Kotlin
/// `CaffeineAnalyticsRange`.
enum CaffeineAnalyticsRange {
  today('Today'),
  yesterday('Yesterday'),
  last30Days('Last 30 days'),
  last90Days('Last 90 days');

  const CaffeineAnalyticsRange(this.label);

  final String label;

  /// The [DatePeriod] ending on [today] this range covers (Kotlin
  /// `CaffeineAnalyticsRange.periodEnding`).
  DatePeriod periodEnding(LocalDate today) {
    switch (this) {
      case CaffeineAnalyticsRange.today:
        return DatePeriod(today, today);
      case CaffeineAnalyticsRange.yesterday:
        final yesterday = today.minusDays(1);
        return DatePeriod(yesterday, yesterday);
      case CaffeineAnalyticsRange.last30Days:
        return DatePeriod(today.minusDays(29), today);
      case CaffeineAnalyticsRange.last90Days:
        return DatePeriod(today.minusDays(89), today);
    }
  }
}

/// The Riverpod port of the Kotlin `CaffeineUiState`, trimmed to the read-only
/// analytics UI: the loading flag, the selected analytics range, the home
/// (today) insights and the analytics-range insights, plus an error slot. The
/// Kotlin setup/entry-selection fields are Phase 6 concerns and are omitted.
@freezed
abstract class CaffeineState with _$CaffeineState {
  const factory CaffeineState({
    @Default(true) bool isLoading,
    @Default(CaffeineAnalyticsRange.last30Days)
    CaffeineAnalyticsRange analyticsRange,
    @Default(CaffeineInsights()) CaffeineInsights homeDisplay,
    @Default(CaffeineInsights()) CaffeineInsights analyticsDisplay,
    ScreenError? error,
  }) = _CaffeineState;
}

/// The Riverpod port of the Kotlin `CaffeineViewModel`.
///
/// A manual [Notifier] (no codegen) following the dashboard template: [build]
/// self-triggers the first load; [selectAnalyticsRange] and [refresh] re-run it.
/// Each pass loads the union of today + the analytics period from the
/// [CaffeineRepository], then runs the ported [CaffeineInsightCalculator] for
/// both the home (today) and analytics windows, reading the caffeine
/// preferences + body profile from the [PreferencesRepository] for the PK model.
/// A monotonic [_generation] guard drops stale results.
class CaffeineViewModel extends Notifier<CaffeineState> {
  int _generation = 0;

  @override
  CaffeineState build() {
    Future.microtask(() {
      if (ref.mounted) load();
    });
    return const CaffeineState();
  }

  void selectAnalyticsRange(CaffeineAnalyticsRange range) {
    if (state.analyticsRange == range) return;
    state = state.copyWith(analyticsRange: range);
    load();
  }

  Future<void> refresh() => load(refreshMode: RefreshMode.force);

  Future<void> load({RefreshMode refreshMode = RefreshMode.normal}) async {
    final generation = ++_generation;
    final loadCaffeine = ref.read(loadCaffeineUseCaseProvider);
    final prefs = ref.read(preferencesRepositoryProvider);
    final preferences = prefs.caffeinePreferences();
    final bodyProfile = prefs.bodyProfile();

    final today = LocalDate.now();
    final homePeriod = DatePeriod(today, today);
    final analyticsPeriod = state.analyticsRange.periodEnding(today);

    state = state.copyWith(isLoading: true, error: null);

    try {
      // One read over the union of the two windows — see [LoadCaffeineUseCase].
      final result = await loadCaffeine(
        homePeriod,
        analyticsPeriod,
        refreshMode: refreshMode,
      );
      if (!ref.mounted || generation != _generation) return;
      final home = CaffeineInsightCalculator.build(
        entries: result.entries,
        period: homePeriod,
        preferences: preferences,
        bodyProfile: bodyProfile,
      );
      final analytics = CaffeineInsightCalculator.build(
        entries: result.entries,
        period: analyticsPeriod,
        preferences: preferences,
        bodyProfile: bodyProfile,
      );
      state = state.copyWith(
        isLoading: false,
        homeDisplay: home,
        analyticsDisplay: analytics,
        error: null,
      );
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: throwableToScreenError(error, fallback: 'Unable to load data.'),
      );
    }
  }
}

/// The caffeine screen's state provider. A manually-declared [NotifierProvider]
/// (no codegen), matching the dashboard template.
final caffeineProvider =
    NotifierProvider<CaffeineViewModel, CaffeineState>(CaffeineViewModel.new);
