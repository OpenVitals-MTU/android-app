import 'dart:async';
import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/model/dashboard_data.dart';
import '../../../domain/model/dashboard_query.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/preferences/activity_week_mode.dart';
import '../../../domain/preferences/app_language.dart';
import '../../../domain/preferences/sleep_range_mode.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/health_connect_gate.dart';
import 'dashboard_display.dart';

// The rings, the tiles and the layout applied to them are the screen's data
// model; it renders them straight out of here.
export 'dashboard_display.dart';

part 'dashboard_view_model.freezed.dart';

/// The Kotlin `HealthConnectFeature.DASHBOARD.name`, used to key the
/// per-feature acknowledged-permission set in preferences.
const String _dashboardFeatureName = 'DASHBOARD';

/// The metrics loaded in the first (fast) pass. The remaining metrics load in a
/// background pass and are folded in with [DashboardDataMergeLoaded.mergeLoaded],
/// mirroring the Kotlin `DashboardViewModel` quick/background split.
const Set<DashboardMetric> dashboardQuickMetrics = <DashboardMetric>{
  DashboardMetric.steps,
  DashboardMetric.distance,
  DashboardMetric.caloriesOut,
  DashboardMetric.hydration,
  DashboardMetric.sleep,
  DashboardMetric.avgHeartRate,
  DashboardMetric.restingHeartRate,
};

/// The Riverpod port of the Kotlin `DashboardUiState`.
///
/// A subset faithful to the fields the summary UI actually consumes: the
/// selected day (capped at today), the single aggregated [DashboardData]
/// payload, the [DashboardDisplay] derived from it, loading/refresh/error flags,
/// the resolved preference snapshot (goals included), the Health Connect
/// availability + minimum-permission state, the set of metrics still loading in
/// the background, and the missing-permission set the inline callout has not yet
/// had acknowledged.
@freezed
abstract class DashboardState with _$DashboardState {
  const DashboardState._();

  const factory DashboardState({
    required LocalDate selectedDate,
    DashboardData? data,
    DashboardDisplay? display,
    @Default(kDefaultDashboardGoals) DashboardGoals goals,
    @Default(true) bool isLoading,
    @Default(false) bool isRefreshing,
    ScreenError? error,
    @Default(SleepRangeMode.evening18h) SleepRangeMode sleepRangeMode,
    @Default(ActivityWeekMode.mondayToSunday) ActivityWeekMode activityWeekMode,
    @Default(false) bool showOpenVitalsCalculatedCalories,
    @Default(HealthConnectAvailability.available)
    HealthConnectAvailability healthConnectAvailability,
    @Default(true) bool minimumPermissionsGranted,
    @Default(<DashboardMetric>{}) Set<DashboardMetric> loadingMetrics,
    @Default(<String>{}) Set<String> unacknowledgedPermissions,
    // Dashboard metric-grid edit mode + persisted layout (tiles keyed by title).
    @Default(false) bool editing,
    @Default(<String>[]) List<String> tileOrder,
    @Default(<String>[]) List<String> ringOrder,
    @Default(<String>{}) Set<String> hiddenTiles,
  }) = _DashboardState;

  /// Forward day navigation is disabled once the selected day reaches today
  /// (Kotlin `selectedDate.isBefore(LocalDate.now())`).
  bool get canGoForward => selectedDate.isBefore(LocalDate.now());

  /// Whether [metric] is still loading in the background pass.
  bool isMetricLoading(DashboardMetric metric) => loadingMetrics.contains(metric);
}

/// The Riverpod port of the Kotlin `DashboardViewModel`.
///
/// Manual [Notifier] (no codegen). [build] seeds the initial state from the
/// preference snapshot and kicks off the first load; [previousDay]/[nextDay]/
/// [selectDate]/[refresh] all funnel through [_load], which loads a fast quick
/// pass then a background pass, publishing each via [DashboardDataMergeLoaded.
/// mergeLoaded]. A monotonically increasing generation guards against a stale
/// pass clobbering a newer selection.
class DashboardViewModel extends Notifier<DashboardState> {
  int _generation = 0;

  /// True once the user has deliberately navigated to a day before today, so a
  /// resume must not yank them back to today (Kotlin `userPinnedPastDay`).
  bool _userPinnedPastDay = false;

  @override
  DashboardState build() {
    final prefs = ref.read(preferencesRepositoryProvider);
    final initial = DashboardState(
      selectedDate: LocalDate.now(),
      sleepRangeMode: prefs.sleepRangeMode,
      activityWeekMode: prefs.activityWeekMode,
      showOpenVitalsCalculatedCalories: prefs.showOpenVitalsCalculatedCalories,
      // The user's goals, not the defaults. The summary used to read these from
      // the widget's own `ref`, so a 6,000-step goal still read "of 8,000" here
      // while the detail screen showed 6,000.
      goals: DashboardGoals.fromPreferences(prefs),
      tileOrder: prefs.dashboardWidgetOrder() ?? const <String>[],
      ringOrder: prefs.dashboardRingOrder() ?? const <String>[],
      hiddenTiles: prefs.dashboardHiddenWidgets(),
    );
    // Defer to a microtask so the first `state =` runs after `build` returns.
    Future.microtask(() {
      if (ref.mounted) _load(initial.selectedDate);
    });
    return initial;
  }

  /// Force-reloads the current day (pull-to-refresh). Keeps the visible data on
  /// screen while refreshing, matching the Kotlin `RefreshMode.FORCE` path.
  Future<void> refresh() =>
      _load(state.selectedDate, refreshMode: RefreshMode.force);

  void clearError() {
    if (state.error != null) state = state.copyWith(error: null);
  }

  /// Toggles the metric-grid edit mode (drag-to-reorder + hide/show).
  ///
  /// Edit mode changes what the display *contains* (it materialises a tile for
  /// every metric the device cannot serve), so the display is rebuilt — the
  /// screen no longer re-derives it on the next frame.
  void toggleEditing() {
    state = state.copyWith(editing: !state.editing);
    _rebuildDisplay();
  }

  /// Persists a new tile order. [visibleIds] is the reordered sequence of the
  /// tiles currently on screen; previously-saved ids for tiles not present today
  /// are preserved at the end so their position survives.
  void setTileOrder(List<String> visibleIds) {
    final merged = <String>[
      ...visibleIds,
      for (final id in state.tileOrder)
        if (!visibleIds.contains(id)) id,
    ];
    ref.read(preferencesRepositoryProvider).setDashboardWidgetOrder(merged);
    state = state.copyWith(tileOrder: merged);
    _rebuildDisplay();
  }

  /// Persists a new hero-ring order (Steps / Weekly cardio).
  void setRingOrder(List<String> visibleIds) {
    final merged = <String>[
      ...visibleIds,
      for (final id in state.ringOrder)
        if (!visibleIds.contains(id)) id,
    ];
    ref.read(preferencesRepositoryProvider).setDashboardRingOrder(merged);
    state = state.copyWith(ringOrder: merged);
    _rebuildDisplay();
  }

  /// Hides or shows a tile (by its title-key).
  void setTileHidden(String id, bool hidden) {
    final next = {...state.hiddenTiles};
    if (hidden) {
      next.add(id);
    } else {
      next.remove(id);
    }
    ref.read(preferencesRepositoryProvider).setDashboardHiddenWidgets(next);
    state = state.copyWith(hiddenTiles: next);
    _rebuildDisplay();
  }

  /// Restores a widget from the edit-mode add-tray.
  ///
  /// [recordPlacement] is for a widget the device does not support: those have
  /// no tile outside edit mode, so the layout treats them as *absent* rather
  /// than merely hidden, and un-hiding alone would leave them in the tray.
  /// Recording the title in [DashboardState.tileOrder] is what marks it as
  /// deliberately placed (the Kotlin allow-list append).
  void addWidget(String title, {bool recordPlacement = false}) {
    setTileHidden(title, false);
    if (!recordPlacement || state.tileOrder.contains(title)) return;
    final merged = <String>[...state.tileOrder, title];
    ref.read(preferencesRepositoryProvider).setDashboardWidgetOrder(merged);
    state = state.copyWith(tileOrder: merged);
    _rebuildDisplay();
  }

  void previousDay() {
    _userPinnedPastDay = true;
    _load(state.selectedDate.minusDays(1));
  }

  void nextDay() {
    final today = LocalDate.now();
    final next = state.selectedDate.plusDays(1);
    if (next.isAfter(today)) return;
    _userPinnedPastDay = next.isBefore(today);
    _load(next);
  }

  void selectDate(LocalDate date) {
    final clamped = date.coerceAtMost(LocalDate.now());
    _userPinnedPastDay = clamped.isBefore(LocalDate.now());
    _load(clamped);
  }

  /// Reloads on resume — when the app returns to the foreground, or when the
  /// user pops back from a pushed detail screen (Kotlin `resumeCurrentDay`).
  ///
  /// The dashboard notifier outlives those screens, so a resume is the only
  /// signal that Health Connect data may have changed underneath it. Snaps back
  /// to today (covering a midnight rollover too) unless the user deliberately
  /// pinned a past day. [_load] re-reads the preference snapshot, so no separate
  /// refresh step is needed.
  void resumeCurrentDay() =>
      _load(_userPinnedPastDay ? state.selectedDate : LocalDate.now());

  /// Persists the acknowledgement of the currently-surfaced missing permissions
  /// and hides the inline callout (Kotlin `acknowledgeWidgetMissingPermissions`).
  void acknowledgePermissions() {
    final missing = state.unacknowledgedPermissions;
    if (missing.isEmpty) return;
    ref
        .read(preferencesRepositoryProvider)
        .acknowledgePermissionsForFeature(_dashboardFeatureName, missing);
    state = state.copyWith(unacknowledgedPermissions: const <String>{});
  }

  /// Requests the outstanding permissions, then refreshes the granted-permission
  /// providers the [HealthConnectGate] reads and reloads the day.
  ///
  /// A request that fails surfaces as the screen's error (a SnackBar, since data
  /// is on screen) and stops there — the invalidations and the reload were
  /// skipped by the thrown failure before, and still are.
  Future<void> grantPermissions() async {
    final missing = state.unacknowledgedPermissions;
    if (missing.isNotEmpty) {
      final result =
          await ref.read(requestHealthPermissionsUseCaseProvider)(missing);
      if (!ref.mounted) return;
      if (result case Err(:final failure)) {
        state = state.copyWith(
          error: failure.toScreenError(
            fallback: 'Unable to request permissions.',
          ),
        );
        return;
      }
    }
    ref.invalidate(grantedHealthPermissionsProvider);
    ref.invalidate(healthConnectAvailabilityProvider);
    await refresh();
  }

  Future<void> _load(
    LocalDate date, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final clamped = date.coerceAtMost(LocalDate.now());
    final generation = ++_generation;

    final prefs = ref.read(preferencesRepositoryProvider);
    final useCase = ref.read(loadDashboardDayUseCaseProvider);

    // Awaits the resolved availability rather than the cached value, which is
    // still `notSupported` on the first load of a cold start. Reading it too
    // early yields an empty granted set, which would surface the whole dashboard
    // permission set as missing — which is also why the availability is handed
    // to the permission check rather than re-resolved inside it.
    final availability = await ref.read(healthConnectAvailabilityProvider.future);
    if (!ref.mounted || generation != _generation) return;
    final permissionCheck =
        await ref.read(checkMinimumHealthPermissionsUseCaseProvider)(
      availability,
    );
    if (!ref.mounted || generation != _generation) return;
    final bool minimumPermissionsGranted;
    switch (permissionCheck) {
      case Ok(:final value):
        minimumPermissionsGranted = value;
      case Err(:final failure):
        // This used to throw out of the load with nothing catching it: the
        // dashboard was left on its loader, forever.
        state = state.copyWith(
          isLoading: false,
          isRefreshing: false,
          error: failure.toScreenError(fallback: 'Unknown error'),
        );
        return;
    }

    final keepData = refreshMode == RefreshMode.force && state.data != null;
    state = state.copyWith(
      selectedDate: clamped,
      isLoading: !keepData,
      isRefreshing: true,
      error: null,
      sleepRangeMode: prefs.sleepRangeMode,
      activityWeekMode: prefs.activityWeekMode,
      showOpenVitalsCalculatedCalories: prefs.showOpenVitalsCalculatedCalories,
      goals: DashboardGoals.fromPreferences(prefs),
      healthConnectAvailability: availability,
      minimumPermissionsGranted: minimumPermissionsGranted,
      loadingMetrics: const <DashboardMetric>{},
    );

    final quickResult = await useCase(
      DashboardQuery(
        date: clamped,
        sleepRangeMode: prefs.sleepRangeMode,
        activityWeekMode: prefs.activityWeekMode,
        visibleMetrics: dashboardQuickMetrics,
        refreshMode: refreshMode,
        includeHistoricalBaselines: false,
        includeWeeklyTrainingSignals: false,
      ),
    );
    if (!ref.mounted || generation != _generation) return;
    final DashboardData quickData;
    switch (quickResult) {
      case Ok(:final value):
        quickData = value;
      case Err(:final failure):
        state = state.copyWith(
          isLoading: false,
          isRefreshing: false,
          error: failure.toScreenError(fallback: 'Unknown error'),
        );
        return;
    }

    final existing = state.data;
    final merged = existing != null && existing.date == clamped
        ? existing.mergeLoaded(quickData)
        : quickData;
    final backgroundMetrics =
        DashboardMetric.values.toSet().difference(dashboardQuickMetrics);
    _publish(merged, loadingMetrics: backgroundMetrics, prefs: prefs);

    await _loadBackground(
      date: clamped,
      refreshMode: refreshMode,
      backgroundMetrics: backgroundMetrics,
      generation: generation,
    );
  }

  Future<void> _loadBackground({
    required LocalDate date,
    required RefreshMode refreshMode,
    required Set<DashboardMetric> backgroundMetrics,
    required int generation,
  }) async {
    if (backgroundMetrics.isEmpty) return;
    final prefs = ref.read(preferencesRepositoryProvider);
    final useCase = ref.read(loadDashboardDayUseCaseProvider);

    // A failing background pass leaves the quick pass on screen rather than
    // blanking it — the same tolerance the try/catch expressed.
    final data = (await useCase(
      DashboardQuery(
        date: date,
        sleepRangeMode: prefs.sleepRangeMode,
        activityWeekMode: prefs.activityWeekMode,
        visibleMetrics: backgroundMetrics,
        refreshMode: refreshMode,
        includeHistoricalBaselines: true,
        includeWeeklyTrainingSignals:
            backgroundMetrics.contains(DashboardMetric.weeklyCardioLoad),
      ),
    ))
        .getOrNull();
    if (!ref.mounted || generation != _generation) return;

    final current = state.data;
    if (current == null || current.date != date) return;
    final merged = data == null ? current : current.mergeLoaded(data);
    _publish(merged, loadingMetrics: const <DashboardMetric>{}, prefs: prefs);
  }

  void _publish(
    DashboardData data, {
    required Set<DashboardMetric> loadingMetrics,
    required PreferencesRepository prefs,
  }) {
    final acknowledged =
        prefs.acknowledgedPermissionsFor(_dashboardFeatureName);
    state = state.copyWith(
      data: data,
      // Both passes publish through here, so both rebuild the display: the fast
      // one shows the quick metrics, the background one folds the rest in.
      display: _buildDisplay(data),
      isLoading: false,
      isRefreshing: false,
      loadingMetrics: loadingMetrics,
      unacknowledgedPermissions:
          data.missingPermissions.difference(acknowledged),
    );
    _refreshHomeWidgets(data, loadingMetrics: loadingMetrics);
  }

  /// Re-derives the display from the data already on the state — for the layout
  /// mutations (edit mode, reorder, hide, add) that change what is shown without
  /// reloading anything.
  void _rebuildDisplay() {
    final data = state.data;
    if (data == null) return;
    state = state.copyWith(display: _buildDisplay(data));
  }

  DashboardDisplay _buildDisplay(DashboardData data) => buildDashboardDisplay(
        data,
        ref.read(unitFormatterProvider),
        _localizations(),
        goals: state.goals,
        editing: state.editing,
        tileOrder: state.tileOrder,
        ringOrder: state.ringOrder,
        hiddenTiles: state.hiddenTiles,
      );

  /// The localizations the tile mapper needs, resolved without a
  /// `BuildContext`: the display is derived here, off the widget tree. Mirrors
  /// `app.dart`'s locale choice — the selected [AppLanguage], or the platform
  /// locale when it is `system` — so the tiles read in the same language the
  /// rest of the app does.
  AppLocalizations _localizations() {
    final tag = ref.read(appLanguageProvider).languageTag;
    final locale =
        tag != null ? Locale(tag) : PlatformDispatcher.instance.locale;
    try {
      return lookupAppLocalizations(locale);
    } on FlutterError {
      return lookupAppLocalizations(const Locale('en'));
    }
  }

  /// Pushes the freshly-committed data to the home-screen widgets.
  ///
  /// This is the single funnel every load ends in — including a resume and a
  /// back-nav from a detail screen — so it is where the widgets learn that
  /// today's data moved, standing in for Kotlin's per-widget `onUpdate` reload.
  ///
  /// Only the *complete* commit for *today* is pushed: the quick pass carries a
  /// subset of the metrics (readiness and body energy would read as "--"), and a
  /// past day the user has navigated to is not what the widgets show. Never
  /// awaited, and the refresher swallows its own failures — a widget must not be
  /// able to stall or crash the dashboard.
  void _refreshHomeWidgets(
    DashboardData data, {
    required Set<DashboardMetric> loadingMetrics,
  }) {
    if (loadingMetrics.isNotEmpty) return;
    if (data.date != LocalDate.now()) return;
    unawaited(ref.read(homeWidgetRefresherProvider).push(data));
  }
}

/// The dashboard state provider. A manually-declared [NotifierProvider] (no
/// codegen), the template later feature batches copy.
final dashboardProvider =
    NotifierProvider<DashboardViewModel, DashboardState>(DashboardViewModel.new);
