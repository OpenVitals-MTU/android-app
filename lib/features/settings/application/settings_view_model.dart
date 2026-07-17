import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../ui/components/health_connect_gate.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/preferences/activity_week_mode.dart';
import '../../../domain/preferences/app_language.dart';
import '../../../domain/preferences/app_theme_mode.dart';
import '../../../domain/preferences/chart_aggregation_mode.dart';
import '../../../domain/preferences/sleep_range_mode.dart';
import '../../../domain/preferences/unit_system.dart';

part 'settings_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `SettingsUiState`, trimmed to the settings
/// this batch wires to already-ported preferences. Apple Health import / offline
/// maps / activity-recording preferences are Phase-6 subsystems and are not part
/// of this state.
@freezed
abstract class SettingsState with _$SettingsState {
  const factory SettingsState({
    @Default(UnitSystem.metric) UnitSystem unitSystem,
    @Default(AppLanguage.system) AppLanguage appLanguage,
    @Default(AppThemeMode.system) AppThemeMode appThemeMode,
    @Default(false) bool dynamicColor,
    @Default(SleepRangeMode.evening18h) SleepRangeMode sleepRangeMode,
    @Default(ChartAggregationMode.off) ChartAggregationMode chartAggregationMode,
    @Default(ActivityWeekMode.mondayToSunday) ActivityWeekMode activityWeekMode,
    @Default(false) bool showOpenVitalsCalculatedCalories,
    @Default(true) bool healthConnectSyncEnabled,
    @Default(false) bool healthConnectMindfulnessEnabled,
    @Default(false) bool appLockEnabled,
    @Default(2.0) double hydrationDailyGoalLiters,
    @Default(120) int highHeartRateThresholdBpm,
    @Default(50) int lowHeartRateThresholdBpm,
  }) = _SettingsState;
}

/// The Riverpod port of the Kotlin `SettingsViewModel` preference wiring. Reads
/// the current values from [PreferencesRepository] on build and writes each
/// change back through it. The app-level reactive providers (`appThemeMode`,
/// `unitSystem`, `appLanguage`, `dynamicColor`, HC sync) are backed by the same
/// preference listenables, so a write here updates the whole app live.
class SettingsViewModel extends Notifier<SettingsState> {
  PreferencesRepository get _prefs => ref.read(preferencesRepositoryProvider);

  @override
  SettingsState build() {
    final prefs = ref.read(preferencesRepositoryProvider);
    return SettingsState(
      unitSystem: prefs.unitSystem,
      appLanguage: prefs.appLanguage,
      appThemeMode: prefs.appThemeMode,
      dynamicColor: prefs.dynamicColor,
      sleepRangeMode: prefs.sleepRangeMode,
      chartAggregationMode: prefs.chartAggregationMode,
      activityWeekMode: prefs.activityWeekMode,
      showOpenVitalsCalculatedCalories: prefs.showOpenVitalsCalculatedCalories,
      healthConnectSyncEnabled: prefs.healthConnectSyncEnabled,
      healthConnectMindfulnessEnabled: prefs.healthConnectMindfulnessEnabled,
      appLockEnabled: prefs.appLockEnabled,
      hydrationDailyGoalLiters: prefs.hydrationDailyGoalLiters,
      highHeartRateThresholdBpm: prefs.highHeartRateThresholdBpm,
      lowHeartRateThresholdBpm: prefs.lowHeartRateThresholdBpm,
    );
  }

  void selectUnitSystem(UnitSystem value) {
    _prefs.unitSystem = value;
    state = state.copyWith(unitSystem: value);
  }

  void selectAppLanguage(AppLanguage value) {
    _prefs.appLanguage = value;
    state = state.copyWith(appLanguage: value);
  }

  void selectAppThemeMode(AppThemeMode value) {
    _prefs.appThemeMode = value;
    state = state.copyWith(appThemeMode: value);
  }

  void setDynamicColor(bool enabled) {
    _prefs.dynamicColor = enabled;
    state = state.copyWith(dynamicColor: enabled);
  }

  void selectSleepRangeMode(SleepRangeMode value) {
    _prefs.sleepRangeMode = value;
    state = state.copyWith(sleepRangeMode: value);
  }

  void selectChartAggregationMode(ChartAggregationMode value) {
    _prefs.chartAggregationMode = value;
    state = state.copyWith(chartAggregationMode: value);
  }

  void selectActivityWeekMode(ActivityWeekMode value) {
    _prefs.activityWeekMode = value;
    state = state.copyWith(activityWeekMode: value);
  }

  void setShowOpenVitalsCalculatedCalories(bool enabled) {
    _prefs.showOpenVitalsCalculatedCalories = enabled;
    state = state.copyWith(showOpenVitalsCalculatedCalories: enabled);
  }

  void setHealthConnectSyncEnabled(bool enabled) {
    _prefs.healthConnectSyncEnabled = enabled;
    state = state.copyWith(healthConnectSyncEnabled: enabled);
  }

  /// Turning this on or off changes which permissions the app declares an
  /// interest in, so the resolved feature flags and the granted-permission set
  /// are both stale the moment it flips. Invalidating the two providers the gate
  /// reads makes the app re-resolve them; without it the mindfulness screens
  /// would keep believing whatever they believed a second ago.
  void setHealthConnectMindfulnessEnabled(bool enabled) {
    _prefs.healthConnectMindfulnessEnabled = enabled;
    state = state.copyWith(healthConnectMindfulnessEnabled: enabled);
    ref.invalidate(healthConnectAvailabilityProvider);
    ref.invalidate(grantedHealthPermissionsProvider);
  }

  void setAppLockEnabled(bool enabled) {
    _prefs.appLockEnabled = enabled;
    state = state.copyWith(appLockEnabled: enabled);
  }

  void setHydrationDailyGoalLiters(double liters) {
    _prefs.hydrationDailyGoalLiters = liters;
    state = state.copyWith(
      hydrationDailyGoalLiters: _prefs.hydrationDailyGoalLiters,
    );
  }

  void setHighHeartRateThresholdBpm(int bpm) {
    _prefs.highHeartRateThresholdBpm = bpm;
    state = state.copyWith(
      highHeartRateThresholdBpm: _prefs.highHeartRateThresholdBpm,
    );
  }

  void setLowHeartRateThresholdBpm(int bpm) {
    _prefs.lowHeartRateThresholdBpm = bpm;
    state = state.copyWith(
      lowHeartRateThresholdBpm: _prefs.lowHeartRateThresholdBpm,
    );
  }
}

/// The state provider for the settings screens.
final settingsProvider = NotifierProvider<SettingsViewModel, SettingsState>(
  SettingsViewModel.new,
);
