import '../core/period/time_range.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/presentation/unit_formatter.dart';
import '../di/providers.dart';
import '../domain/health/health_permissions.dart';
import '../domain/preferences/activity_week_mode.dart';
import '../domain/preferences/app_language.dart';
import '../domain/preferences/app_theme_mode.dart';
import '../domain/preferences/chart_aggregation_mode.dart';
import '../domain/preferences/unit_system.dart';

/// App-shell state providers.
///
/// The Kotlin UI collects `PreferencesRepository` `StateFlow`s; the Dart port
/// backs each reactive preference with a [ValueListenable]. [_watchListenable]
/// bridges one into Riverpod: it reads the current value and re-runs the
/// provider (via [Ref.invalidateSelf]) whenever the listenable fires, so any
/// widget watching the provider rebuilds when the setting changes.
T _watchListenable<T>(Ref ref, ValueListenable<T> listenable) {
  void listener() => ref.invalidateSelf();
  listenable.addListener(listener);
  ref.onDispose(() => listenable.removeListener(listener));
  return listenable.value;
}

/// The selected [AppThemeMode] (system/light/dark/amoled). Reactive.
final appThemeModeProvider = Provider<AppThemeMode>((ref) {
  final repo = ref.watch(preferencesRepositoryProvider);
  return _watchListenable(ref, repo.appThemeModeListenable);
});

/// Whether Material You dynamic colour is enabled. Reactive.
final dynamicColorEnabledProvider = Provider<bool>((ref) {
  final repo = ref.watch(preferencesRepositoryProvider);
  return _watchListenable(ref, repo.dynamicColorListenable);
});

/// The selected in-app [AppLanguage] (`system` follows the OS locale). Reactive.
final appLanguageProvider = Provider<AppLanguage>((ref) {
  final repo = ref.watch(preferencesRepositoryProvider);
  return _watchListenable(ref, repo.appLanguageListenable);
});

/// The selected [UnitSystem] (metric/imperial). Reactive.
final unitSystemProvider = Provider<UnitSystem>((ref) {
  final repo = ref.watch(preferencesRepositoryProvider);
  return _watchListenable(ref, repo.unitSystemListenable);
});

/// The split distance for the activity detail screen's splits card, in METERS.
/// Reactive: changing it in settings re-cuts the splits on the next detail load.
final activitySplitDistanceMetersProvider = Provider<double>((ref) {
  final repo = ref.watch(preferencesRepositoryProvider);
  return _watchListenable(ref, repo.activitySplitDistanceMetersListenable);
});

/// How the intraday vitals charts summarise their data (raw or bucketed).
/// Reactive: changing it in settings rebuilds the charts on the next read.
final chartAggregationModeProvider = Provider<ChartAggregationMode>((ref) {
  final repo = ref.watch(preferencesRepositoryProvider);
  return _watchListenable(ref, repo.chartAggregationModeListenable);
});

/// A [UnitFormatter] bound to the current [unitSystemProvider]. Rebuilds (and so
/// re-formats every consuming widget) when the unit-system preference changes.
/// The Kotlin app injects a single `UnitFormatter`; here it is a derived
/// provider so feature screens can `ref.watch` it instead of threading it down.
final unitFormatterProvider = Provider<UnitFormatter>((ref) {
  final unitSystem = ref.watch(unitSystemProvider);
  return UnitFormatter(unitSystemProvider: () => unitSystem);
});

/// Whether onboarding has been completed *for the current permission set*. Read
/// once to pick the start destination; not backed by a listenable in the
/// repository, so this is a plain snapshot read (the onboarding flow persists
/// the flags and then routes on to the dashboard imperatively).
///
/// The version check is what makes widening the required permission set mean
/// anything to existing users. `onboardingDone` alone is a one-way door: someone
/// who finished onboarding when it asked for less would never be asked again,
/// and would sit behind permission gates on every screen instead. Bumping
/// [HealthPermissionService.PERMISSION_SET_VERSION] sends them through once more.
final onboardingCompleteProvider = Provider<bool>((ref) {
  final prefs = ref.watch(preferencesRepositoryProvider);
  return prefs.onboardingDone &&
      prefs.lastPromptedPermissionSetVersion >=
          HealthPermissionService.PERMISSION_SET_VERSION;
});

/// The period mode driving *every* period title — the navigator's and the chart
/// summaries' alike.
///
/// DELIBERATE DEVIATION from the Kotlin app. Kotlin makes only the navigator
/// title mode-aware, so on a rolling Month its navigator reads "Last 30 days"
/// while a chart summary underneath still reads "This month" — two names for the
/// same window, on the same screen. We thread the mode everywhere instead, so the
/// titles agree. A parity audit will flag this; it is intended.
final weekPeriodModeProvider = Provider<WeekPeriodMode>((ref) {
  final repo = ref.watch(preferencesRepositoryProvider);
  // Through the listenable bridge like every other reactive pref: reading the
  // plain getter froze this provider at its first value, so toggling the
  // setting retitled nothing until an app restart — and screens that had
  // already loaded showed a period computed under one mode with data loaded
  // under the other.
  return _watchListenable(ref, repo.activityWeekModeListenable)
      .toWeekPeriodMode();
});
