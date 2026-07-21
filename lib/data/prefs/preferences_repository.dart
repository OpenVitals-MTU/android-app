import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../core/period/period_range_preference_key.dart';
import '../../core/period/time_range.dart';
import '../../domain/insights/daily_goals.dart';
import '../../domain/model/hydration_reminder_config.dart';
import '../../domain/model/mindfulness_models.dart';
import '../../domain/model/mindfulness_reminder_config.dart';
import '../../domain/model/nutrition_models.dart';
import '../../domain/preferences/activity_recording_dashboard_layout.dart';
import '../../domain/preferences/activity_recording_preferences.dart';
import '../../domain/preferences/activity_split_distance.dart';
import '../../domain/preferences/activity_week_mode.dart';
import '../../domain/preferences/app_language.dart';
import '../../domain/preferences/app_theme_mode.dart';
import '../../domain/preferences/body_energy_calibration.dart';
import '../../domain/preferences/body_profile.dart';
import '../../domain/preferences/caffeine_preferences.dart';
import '../../domain/preferences/chart_aggregation_mode.dart';
import '../../domain/preferences/sleep_window.dart';
import '../../domain/preferences/unit_system.dart';
import 'device_locale.dart';
import 'prefs_codec.dart';
import 'prefs_migrations.dart';
import 'prefs_store.dart';
import 'stores/activity_recording_prefs_store.dart';
import 'stores/caffeine_store.dart';
import 'stores/hydration_store.dart';

/// Port of the Kotlin `PreferencesRepository` over `shared_preferences`.
///
/// Reactivity: the Kotlin class exposes `StateFlow`s. Here each reactive value
/// is backed by a [ValueNotifier]; the synchronous getter reads `.value` and
/// the setter writes to prefs and updates the notifier. The `*Listenable`
/// getters expose the [ValueListenable] for UI/Riverpod wiring.
///
/// Note: SharedPreferences updates its in-memory cache synchronously, so the
/// void setters here fire the async platform write without awaiting while
/// remaining immediately readable (matching the Kotlin `apply()` semantics).
/// The write primitives themselves live in [PrefsStore], which this class
/// **holds as a private field rather than extending**: inheriting them would
/// re-export `putString`, `remove` and the raw store on the repository's own
/// public API, and letting a caller write an arbitrary key past the facade
/// would undo the point of having one.
///
/// The three biggest key groups have their own stores ([CaffeineStore],
/// [ActivityRecordingPrefsStore], [HydrationStore]), each owning its keys; the
/// repository keeps the reactive notifiers and the public API every caller
/// already uses, and delegates the storage.
///
/// The string codec lives in `prefs_codec.dart`, the one-shot read migration in
/// `prefs_migrations.dart` and the locale-derived unit-system default in
/// `device_locale.dart` — none of those need a store, so none of them belong
/// here.
class PreferencesRepository {
  /// [localeName] only seeds the unit system for a user who has never chosen
  /// one; it defaults to the device locale. It exists so that default is
  /// testable — `Platform.localeName` is not a compile-time constant, hence the
  /// factory.
  factory PreferencesRepository(
    SharedPreferences prefs, {
    String? localeName,
  }) =>
      PreferencesRepository._(prefs, localeName ?? Platform.localeName);

  PreferencesRepository._(this._prefs, String localeName)
      : _store = PrefsStore(_prefs),
        _caffeine = CaffeineStore(_prefs),
        _activityRecording = ActivityRecordingPrefsStore(_prefs),
        _hydration = HydrationStore(_prefs),
        _unitSystem = ValueNotifier(_readUnitSystem(_prefs, localeName)),
        _appLanguage = ValueNotifier(_readAppLanguage(_prefs)),
        _appThemeMode = ValueNotifier(_readAppThemeMode(_prefs)),
        _dynamicColor = ValueNotifier(_readDynamicColor(_prefs)),
        _nightStartHour = ValueNotifier(_readNightHour(_prefs, _keyNightStartHour, 18)),
        _nightEndHour = ValueNotifier(_readNightHour(_prefs, _keyNightEndHour, 10)),
        _chartAggregationMode =
            ValueNotifier(_readChartAggregationMode(_prefs)),
        _activityWeekMode = ValueNotifier(_readActivityWeekMode(_prefs)),
        _activitySplitDistanceMeters =
            ValueNotifier(_readActivitySplitDistanceMeters(_prefs)),
        _showOpenVitalsCalculatedCalories =
            ValueNotifier(_readShowOpenVitalsCalculatedCalories(_prefs)),
        _healthConnectSyncEnabled =
            ValueNotifier(_readHealthConnectSyncEnabled(_prefs)),
        _healthConnectMindfulnessEnabled =
            ValueNotifier(_readHealthConnectMindfulnessEnabled(_prefs)) {
    // Runs once per construction, before the body profile is read — it used to
    // hide inside that read, which made a write look like a read. It is a no-op
    // unless a legacy install has values to fold in.
    migrateLegacyBodyProfileValues(_prefs);
    _bodyEnergyCalibration = ValueNotifier(_readBodyEnergyCalibration());
    _caffeinePreferences = ValueNotifier(_caffeine.read());
    _bodyProfile = ValueNotifier(_readBodyProfile());
  }

  final SharedPreferences _prefs;

  /// The storage primitives for the keys this class still owns itself. Private
  /// on purpose — callers get preferences, not a key-value store.
  final PrefsStore _store;
  final CaffeineStore _caffeine;
  final ActivityRecordingPrefsStore _activityRecording;
  final HydrationStore _hydration;

  final ValueNotifier<UnitSystem> _unitSystem;
  final ValueNotifier<AppLanguage> _appLanguage;
  final ValueNotifier<AppThemeMode> _appThemeMode;
  final ValueNotifier<bool> _dynamicColor;
  final ValueNotifier<int> _nightStartHour;
  final ValueNotifier<int> _nightEndHour;
  final ValueNotifier<ChartAggregationMode> _chartAggregationMode;
  final ValueNotifier<ActivityWeekMode> _activityWeekMode;
  final ValueNotifier<double> _activitySplitDistanceMeters;
  final ValueNotifier<bool> _showOpenVitalsCalculatedCalories;
  final ValueNotifier<bool> _healthConnectSyncEnabled;
  final ValueNotifier<bool> _healthConnectMindfulnessEnabled;
  late final ValueNotifier<BodyEnergyCalibration> _bodyEnergyCalibration;
  late final ValueNotifier<CaffeinePreferences> _caffeinePreferences;
  late final ValueNotifier<BodyProfile> _bodyProfile;

  // region Reactive listenables (Kotlin StateFlows).
  ValueListenable<UnitSystem> get unitSystemListenable => _unitSystem;
  ValueListenable<AppLanguage> get appLanguageListenable => _appLanguage;
  ValueListenable<AppThemeMode> get appThemeModeListenable => _appThemeMode;
  ValueListenable<bool> get dynamicColorListenable => _dynamicColor;
  ValueListenable<int> get nightStartHourListenable => _nightStartHour;
  ValueListenable<int> get nightEndHourListenable => _nightEndHour;
  ValueListenable<ChartAggregationMode> get chartAggregationModeListenable =>
      _chartAggregationMode;
  ValueListenable<ActivityWeekMode> get activityWeekModeListenable =>
      _activityWeekMode;
  ValueListenable<double> get activitySplitDistanceMetersListenable =>
      _activitySplitDistanceMeters;
  ValueListenable<bool> get showOpenVitalsCalculatedCaloriesListenable =>
      _showOpenVitalsCalculatedCalories;
  ValueListenable<bool> get healthConnectSyncEnabledListenable =>
      _healthConnectSyncEnabled;
  ValueListenable<bool> get healthConnectMindfulnessEnabledListenable =>
      _healthConnectMindfulnessEnabled;
  ValueListenable<BodyEnergyCalibration> get bodyEnergyCalibrationListenable =>
      _bodyEnergyCalibration;
  ValueListenable<CaffeinePreferences> get caffeinePreferencesListenable =>
      _caffeinePreferences;
  ValueListenable<BodyProfile> get bodyProfileListenable => _bodyProfile;
  // endregion

  bool get onboardingDone => _prefs.getBool(_keyOnboardingDone) ?? false;
  set onboardingDone(bool value) => _store.putBool(_keyOnboardingDone, value);

  UnitSystem get unitSystem => _unitSystem.value;
  set unitSystem(UnitSystem value) {
    _store.putString(_keyUnitSystem, value.name);
    _unitSystem.value = value;
  }

  AppLanguage get appLanguage => _appLanguage.value;
  set appLanguage(AppLanguage value) {
    _store.putString(_keyAppLanguage, value.name);
    _appLanguage.value = value;
  }

  AppThemeMode get appThemeMode => _appThemeMode.value;
  set appThemeMode(AppThemeMode value) {
    _store.putString(_keyAppThemeMode, value.name);
    _appThemeMode.value = value;
  }

  bool get dynamicColor => _dynamicColor.value;
  set dynamicColor(bool value) {
    _store.putBool(_keyDynamicColor, value);
    _dynamicColor.value = value;
  }

  /// The nightly sleep window (default 18:00 → 10:00). Sleep is captured within
  /// it; sessions outside are daytime naps.
  SleepWindow get sleepWindow => SleepWindow(
        startHour: _nightStartHour.value,
        endHour: _nightEndHour.value,
      );

  int get nightStartHour => _nightStartHour.value;
  set nightStartHour(int value) {
    final hour = value.clamp(0, 23);
    _store.putInt(_keyNightStartHour, hour);
    _nightStartHour.value = hour;
  }

  int get nightEndHour => _nightEndHour.value;
  set nightEndHour(int value) {
    final hour = value.clamp(0, 23);
    _store.putInt(_keyNightEndHour, hour);
    _nightEndHour.value = hour;
  }

  ChartAggregationMode get chartAggregationMode => _chartAggregationMode.value;
  set chartAggregationMode(ChartAggregationMode value) {
    _store.putString(_keyChartAggregationMode, value.name);
    _chartAggregationMode.value = value;
  }

  ActivityWeekMode get activityWeekMode => _activityWeekMode.value;
  set activityWeekMode(ActivityWeekMode value) {
    _store.putString(_keyActivityWeekMode, value.name);
    _activityWeekMode.value = value;
  }

  /// How far apart the activity detail screen cuts derived splits, in METERS
  /// (storage is metric; the settings UI offers km or mile presets and converts
  /// on save). Clamped through [ActivitySplitDistance.normalize], so a corrupt
  /// or out-of-range stored value degrades to the 1 km default instead of
  /// producing a million-row splits card.
  double get activitySplitDistanceMeters => _activitySplitDistanceMeters.value;
  set activitySplitDistanceMeters(double value) {
    final normalized = ActivitySplitDistance.normalize(value);
    _store.putDouble(_keyActivitySplitDistanceMeters, normalized);
    _activitySplitDistanceMeters.value = normalized;
  }

  WeekPeriodMode get weekPeriodMode => activityWeekMode.toWeekPeriodMode();

  bool get showOpenVitalsCalculatedCalories =>
      _showOpenVitalsCalculatedCalories.value;
  set showOpenVitalsCalculatedCalories(bool value) {
    _store.putBool(_keyShowOpenVitalsCalculatedCalories, value);
    _showOpenVitalsCalculatedCalories.value = value;
  }

  bool get healthConnectSyncEnabled => _healthConnectSyncEnabled.value;
  set healthConnectSyncEnabled(bool value) {
    _store.putBool(_keyHealthConnectSyncEnabled, value);
    _healthConnectSyncEnabled.value = value;
  }

  /// Whether to use Health Connect for mindfulness sessions.
  ///
  /// **Off by default, and that is not timidity.** A Health Connect module can
  /// define the mindfulness permission and report the feature available while
  /// its own permission screen cannot draw a row for it — it throws, the system
  /// Health Connect app dies, and the user can then grant this app NOTHING. We
  /// have no way to ask whether the permission UI works, so we do not ask for
  /// the permission until a user tells us to.
  bool get healthConnectMindfulnessEnabled =>
      _healthConnectMindfulnessEnabled.value;
  set healthConnectMindfulnessEnabled(bool value) {
    _store.putBool(_keyHealthConnectMindfulnessEnabled, value);
    _healthConnectMindfulnessEnabled.value = value;
  }

  int get healthConnectPermissionCancelCount =>
      _prefs.getInt(_keyHealthConnectPermissionCancelCount) ?? 0;
  set healthConnectPermissionCancelCount(int value) => _store.putInt(
        _keyHealthConnectPermissionCancelCount,
        value < 0 ? 0 : value,
      );

  String? get acceptedPrivacyPolicyVersion =>
      _prefs.getString(_keyAcceptedPrivacyPolicyVersion);
  set acceptedPrivacyPolicyVersion(String? value) {
    if (value == null) {
      _store.remove(_keyAcceptedPrivacyPolicyVersion);
    } else {
      _store.putString(_keyAcceptedPrivacyPolicyVersion, value);
    }
  }

  int get privacyPolicyAcceptedAtMillis =>
      _prefs.getInt(_keyPrivacyPolicyAcceptedAt) ?? 0;
  set privacyPolicyAcceptedAtMillis(int value) =>
      _store.putInt(_keyPrivacyPolicyAcceptedAt, value);

  bool get appLockEnabled => _prefs.getBool(_keyAppLockEnabled) ?? false;
  set appLockEnabled(bool value) => _store.putBool(_keyAppLockEnabled, value);

  int? get lastActivityExerciseType {
    final value =
        _prefs.getInt(_keyLastActivityExerciseType) ?? _missingExerciseType;
    return value != _missingExerciseType ? value : null;
  }

  set lastActivityExerciseType(int? value) {
    if (value == null) {
      _store.remove(_keyLastActivityExerciseType);
    } else {
      _store.putInt(_keyLastActivityExerciseType, value);
    }
  }

  int? get favoriteActivityExerciseType {
    final value =
        _prefs.getInt(_keyFavoriteActivityExerciseType) ?? _missingExerciseType;
    return value != _missingExerciseType ? value : null;
  }

  set favoriteActivityExerciseType(int? value) {
    if (value == null) {
      _store.remove(_keyFavoriteActivityExerciseType);
    } else {
      _store.putInt(_keyFavoriteActivityExerciseType, value);
    }
  }

  /// Clamped on write and NOT on read — see [HydrationStore].
  double get hydrationDailyGoalLiters => _hydration.readDailyGoalLiters();
  set hydrationDailyGoalLiters(double value) =>
      _hydration.writeDailyGoalLiters(value);

  int get highHeartRateThresholdBpm =>
      (_prefs.getInt(_keyHighHeartRateThresholdBpm) ??
              defaultHighHeartRateThresholdBpm)
          .clamp(minHighHeartRateThresholdBpm, maxHighHeartRateThresholdBpm);
  set highHeartRateThresholdBpm(int value) => _store.putInt(
        _keyHighHeartRateThresholdBpm,
        value.clamp(minHighHeartRateThresholdBpm, maxHighHeartRateThresholdBpm),
      );

  int get lowHeartRateThresholdBpm =>
      (_prefs.getInt(_keyLowHeartRateThresholdBpm) ??
              defaultLowHeartRateThresholdBpm)
          .clamp(minLowHeartRateThresholdBpm, maxLowHeartRateThresholdBpm);
  set lowHeartRateThresholdBpm(int value) => _store.putInt(
        _keyLowHeartRateThresholdBpm,
        value.clamp(minLowHeartRateThresholdBpm, maxLowHeartRateThresholdBpm),
      );

  int get lastPromptedPermissionSetVersion =>
      _prefs.getInt(_keyLastPromptedPermissionSetVersion) ?? 0;
  set lastPromptedPermissionSetVersion(int value) =>
      _store.putInt(_keyLastPromptedPermissionSetVersion, value);

  BodyEnergyCalibration bodyEnergyCalibration() => _bodyEnergyCalibration.value;

  void setBodyEnergyCalibration(BodyEnergyCalibration calibration) {
    final normalized = calibration.normalized();
    _store.putBool(_keyBodyEnergyUseManualZones, normalized.useManualZones);
    _store.putBool(_keyBodyEnergySetupCompleted, normalized.setupCompleted);
    final zones = normalized.manualZoneThresholdsBpm;
    if (zones != null) {
      _store.putString(
        _keyBodyEnergyZoneThresholdsBpm,
        zones.toPreferenceString(),
      );
    } else {
      _store.remove(_keyBodyEnergyZoneThresholdsBpm);
    }
    _store.putDouble(_keyBodyEnergySleepChargeGain, normalized.sleepChargeGain);
    _store.putDouble(
      _keyBodyEnergyActivityDrainGain,
      normalized.activityDrainGain,
    );
    _store.putDouble(_keyBodyEnergyBasalDrainGain, normalized.basalDrainGain);
    _store.putDouble(_keyBodyEnergyStressDrainGain, normalized.stressDrainGain);
    _store.putInt(_keyBodyEnergyFeelCheckCount, normalized.feelCheckCount);
    _bodyEnergyCalibration.value = normalized;
  }

  BodyProfile bodyProfile() => _bodyProfile.value;

  void setBodyProfile(BodyProfile profile) {
    final normalized = profile.normalized();
    _store.putOrRemoveInt(keyBodyProfileBirthYear, normalized.birthYear);
    _store.putOrRemoveDouble(keyBodyProfileWeightKg, normalized.weightKg);
    _store.putOrRemoveInt(
      keyBodyProfileRestingHrBpm,
      normalized.restingHeartRateBpm,
    );
    _store.putOrRemoveInt(keyBodyProfileMaxHrBpm, normalized.maxHeartRateBpm);
    _bodyProfile.value = normalized;
  }

  CaffeinePreferences caffeinePreferences() => _caffeinePreferences.value;

  void setCaffeinePreferences(CaffeinePreferences preferences) {
    final normalized = preferences.normalized();
    _caffeine.write(normalized);
    _caffeinePreferences.value = normalized;
  }

  TimeRange timeRangeFor(PeriodRangePreferenceKey key) =>
      enumByName(TimeRange.values, _prefs.getString(key.storageKey)) ??
      key.defaultRange;

  void setTimeRangeFor(PeriodRangePreferenceKey key, TimeRange range) =>
      _store.putString(key.storageKey, range.name);

  double dailyGoalFor(MetricDailyGoalKey key) => key.normalize(
        _prefs.getDouble(key.storageKey) ?? key.defaultValue,
      );

  void setDailyGoalFor(MetricDailyGoalKey key, double value) =>
      _store.putDouble(key.storageKey, key.normalize(value));

  // region Hydration (stored by HydrationStore).
  HydrationReminderConfig hydrationReminderConfig() =>
      _hydration.readReminderConfig();

  void setHydrationReminderConfig(HydrationReminderConfig config) =>
      _hydration.writeReminderConfig(config);

  Map<String, double> hydrationContainerVolumeMilliliters() =>
      _hydration.readContainerVolumeMilliliters();

  void setHydrationContainerVolumeMilliliters(
    String containerId,
    double milliliters,
  ) =>
      _hydration.writeContainerVolumeMilliliters(containerId, milliliters);

  double? lastCustomHydrationAmountMilliliters() =>
      _hydration.readLastCustomAmountMilliliters();

  void setLastCustomHydrationAmountMilliliters(double milliliters) =>
      _hydration.writeLastCustomAmountMilliliters(milliliters);

  List<CustomHydrationDrink> customHydrationDrinks() =>
      _hydration.readCustomDrinks();

  void saveCustomHydrationDrink(CustomHydrationDrink drink) =>
      _hydration.saveCustomDrink(drink);

  void deleteCustomHydrationDrink(String drinkId) =>
      _hydration.deleteCustomDrink(drinkId);

  void reorderCustomHydrationDrinks(List<String> drinkIds) =>
      _hydration.reorderCustomDrinks(drinkIds);

  bool hasMigratedHydrationBeveragesToRoom() =>
      _hydration.hasMigratedBeveragesToRoom();

  void setMigratedHydrationBeveragesToRoom() =>
      _hydration.setMigratedBeveragesToRoom();
  // endregion

  MindfulnessReminderConfig mindfulnessReminderConfig() =>
      MindfulnessReminderConfig(
        enabled: _prefs.getBool(_keyMindfulnessRemindersEnabled) ?? false,
        reminderTime: toReminderTimeOrDefault(
          _prefs.getString(_keyMindfulnessReminderTime),
          MindfulnessReminderConfig.defaultReminderTime,
        ),
      ).normalized();

  void setMindfulnessReminderConfig(MindfulnessReminderConfig config) {
    final normalized = config.normalized();
    _store.putBool(_keyMindfulnessRemindersEnabled, normalized.enabled);
    _store.putString(
      _keyMindfulnessReminderTime,
      normalized.reminderTime.toString(),
    );
  }

  MindfulnessTimerConfig mindfulnessTimerConfig() {
    final duration = (_prefs.getInt(_keyMindfulnessTimerDurationMinutes) ??
            _defaultMindfulnessTimerDurationMinutes)
        .clamp(_minMindfulnessTimerMinutes, _maxMindfulnessTimerMinutes);
    final storedInterval =
        _prefs.getInt(_keyMindfulnessTimerIntervalMinutes) ?? 0;
    final interval = storedInterval > 0
        ? storedInterval.clamp(
            _minMindfulnessTimerMinutes,
            _maxMindfulnessTimerMinutes,
          )
        : null;
    final bellSound = toMindfulnessBellSound(
          _prefs.getString(_keyMindfulnessTimerBellSound),
        ) ??
        MindfulnessBellSound.struck;
    final backgroundSound = optionalBackgroundSound(
          _prefs.getString(_keyMindfulnessTimerBackgroundSound),
        ) ??
        MindfulnessBackgroundSound.none;
    return MindfulnessTimerConfig(
      durationMinutes: duration,
      intervalMinutes: (interval != null && interval < duration) ? interval : null,
      bellSound: bellSound,
      backgroundSound: backgroundSound,
    );
  }

  void setMindfulnessTimerConfig(MindfulnessTimerConfig config) {
    final duration = config.durationMinutes
        .clamp(_minMindfulnessTimerMinutes, _maxMindfulnessTimerMinutes);
    int? interval = config.intervalMinutes;
    if (interval != null && duration > _minMindfulnessTimerMinutes) {
      final upperBound = (duration - 1) < _minMindfulnessTimerMinutes
          ? _minMindfulnessTimerMinutes
          : (duration - 1);
      interval = interval.clamp(_minMindfulnessTimerMinutes, upperBound);
    } else {
      interval = null;
    }
    _store.putInt(_keyMindfulnessTimerDurationMinutes, duration);
    _store.putInt(_keyMindfulnessTimerIntervalMinutes, interval ?? 0);
    _store.putString(
      _keyMindfulnessTimerBellSound,
      config.bellSound.storageName,
    );
    _store.putString(
      _keyMindfulnessTimerBackgroundSound,
      config.backgroundSound.storageName,
    );
  }

  List<String>? dashboardWidgetOrder() =>
      _store.readOrderList(_keyDashboardWidgetOrder);

  void setDashboardWidgetOrder(List<String> widgetIds) => _store.putString(
        _keyDashboardWidgetOrder,
        widgetIds.join(valueSeparator),
      );

  List<String>? dashboardRingOrder() =>
      _store.readOrderList(_keyDashboardRingOrder);

  void setDashboardRingOrder(List<String> ringIds) =>
      _store.putString(_keyDashboardRingOrder, ringIds.join(valueSeparator));

  /// The dashboard metric tiles the user has hidden (keyed by tile title).
  Set<String> dashboardHiddenWidgets() =>
      (_prefs.getStringList(_keyDashboardHiddenWidgets) ?? const <String>[])
          .toSet();

  void setDashboardHiddenWidgets(Set<String> widgetIds) =>
      _store.putStringList(_keyDashboardHiddenWidgets, widgetIds.toList());

  List<String>? manualEntryWidgetOrder() =>
      _store.readOrderList(_keyManualEntryWidgetOrder);

  void setManualEntryWidgetOrder(List<String> widgetIds) => _store.putString(
        _keyManualEntryWidgetOrder,
        widgetIds.join(valueSeparator),
      );

  List<String>? metricDetailSectionOrder() =>
      _store.readOrderList(_keyMetricDetailSectionOrder);

  void setMetricDetailSectionOrder(List<String> sectionIds) => _store.putString(
        _keyMetricDetailSectionOrder,
        sectionIds.join(valueSeparator),
      );

  Set<String> acknowledgedPermissions() =>
      (_prefs.getStringList(_keyAcknowledgedPermissions) ?? const <String>[])
          .toSet();

  void acknowledgePermissions(Set<String> permissions) => _store.putStringList(
        _keyAcknowledgedPermissions,
        {...acknowledgedPermissions(), ...permissions}.toList(),
      );

  /// The Kotlin overload keys by a `HealthConnectFeature`; that enum is not part
  /// of the ported domain layer, so this keeps the same key format by taking the
  /// feature's name string directly.
  Set<String> acknowledgedPermissionsFor(String featureName) =>
      (_prefs.getStringList(_acknowledgedFeatureKey(featureName)) ??
              const <String>[])
          .toSet();

  void acknowledgePermissionsForFeature(
    String featureName,
    Set<String> permissions,
  ) {
    if (permissions.isEmpty) return;
    _store.putStringList(
      _acknowledgedFeatureKey(featureName),
      {...acknowledgedPermissionsFor(featureName), ...permissions}.toList(),
    );
  }

  // region Activity recording preferences (stored by
  // ActivityRecordingPrefsStore).
  ActivityRecordingPreferences activityRecordingPreferences() =>
      _activityRecording.read();

  void setActivityRecordingPreferences(
    ActivityRecordingPreferences preferences,
  ) =>
      _activityRecording.write(preferences);

  ActivityRecordingDashboardLayout activityRecordingDashboardLayout(
    String activityTypeId,
  ) =>
      _activityRecording.readDashboardLayout(activityTypeId);

  void setActivityRecordingDashboardLayout(
    String activityTypeId,
    ActivityRecordingDashboardLayout layout,
  ) =>
      _activityRecording.writeDashboardLayout(activityTypeId, layout);
  // endregion

  /// Disposes the reactive [ValueNotifier]s.
  void dispose() {
    _unitSystem.dispose();
    _appLanguage.dispose();
    _appThemeMode.dispose();
    _dynamicColor.dispose();
    _nightStartHour.dispose();
    _nightEndHour.dispose();
    _chartAggregationMode.dispose();
    _activityWeekMode.dispose();
    _activitySplitDistanceMeters.dispose();
    _showOpenVitalsCalculatedCalories.dispose();
    _healthConnectSyncEnabled.dispose();
    _healthConnectMindfulnessEnabled.dispose();
    _bodyEnergyCalibration.dispose();
    _caffeinePreferences.dispose();
    _bodyProfile.dispose();
  }

  // region Reads used to seed the reactive notifiers. The `prefs` these static
  // helpers take is the constructor's parameter, not a field: they run while the
  // fields are still being initialized.
  static UnitSystem _readUnitSystem(
    SharedPreferences prefs,
    String localeName,
  ) =>
      enumByName(UnitSystem.values, prefs.getString(_keyUnitSystem)) ??
      unitSystemForLocale(localeName);

  static AppLanguage _readAppLanguage(SharedPreferences prefs) =>
      enumByName(AppLanguage.values, prefs.getString(_keyAppLanguage)) ??
      AppLanguage.system;

  static AppThemeMode _readAppThemeMode(SharedPreferences prefs) =>
      enumByName(AppThemeMode.values, prefs.getString(_keyAppThemeMode)) ??
      AppThemeMode.system;

  static bool _readDynamicColor(SharedPreferences prefs) =>
      prefs.getBool(_keyDynamicColor) ?? false;

  static int _readNightHour(
    SharedPreferences prefs,
    String key,
    int fallback,
  ) =>
      (prefs.getInt(key) ?? fallback).clamp(0, 23);

  static ChartAggregationMode _readChartAggregationMode(
    SharedPreferences prefs,
  ) =>
      enumByName(
        ChartAggregationMode.values,
        prefs.getString(_keyChartAggregationMode),
      ) ??
      ChartAggregationMode.off;

  static ActivityWeekMode _readActivityWeekMode(SharedPreferences prefs) =>
      enumByName(
        ActivityWeekMode.values,
        prefs.getString(_keyActivityWeekMode),
      ) ??
      ActivityWeekMode.mondayToSunday;

  static double _readActivitySplitDistanceMeters(SharedPreferences prefs) =>
      ActivitySplitDistance.normalize(
        prefs.getDouble(_keyActivitySplitDistanceMeters) ??
            ActivitySplitDistance.defaultMeters,
      );

  static bool _readShowOpenVitalsCalculatedCalories(SharedPreferences prefs) =>
      prefs.getBool(_keyShowOpenVitalsCalculatedCalories) ?? false;

  static bool _readHealthConnectSyncEnabled(SharedPreferences prefs) =>
      prefs.getBool(_keyHealthConnectSyncEnabled) ?? true;

  static bool _readHealthConnectMindfulnessEnabled(SharedPreferences prefs) =>
      prefs.getBool(_keyHealthConnectMindfulnessEnabled) ?? false;

  BodyEnergyCalibration _readBodyEnergyCalibration() => BodyEnergyCalibration(
        manualZoneThresholdsBpm: HeartZoneThresholds.fromPreferenceString(
          _prefs.getString(_keyBodyEnergyZoneThresholdsBpm),
        ),
        useManualZones: _prefs.getBool(_keyBodyEnergyUseManualZones) ?? false,
        setupCompleted: _prefs.getBool(_keyBodyEnergySetupCompleted) ?? false,
        sleepChargeGain:
            _prefs.getDouble(_keyBodyEnergySleepChargeGain) ?? 1.0,
        activityDrainGain:
            _prefs.getDouble(_keyBodyEnergyActivityDrainGain) ?? 1.0,
        basalDrainGain: _prefs.getDouble(_keyBodyEnergyBasalDrainGain) ?? 1.0,
        stressDrainGain: _prefs.getDouble(_keyBodyEnergyStressDrainGain) ?? 1.0,
        feelCheckCount: _prefs.getInt(_keyBodyEnergyFeelCheckCount) ?? 0,
      ).normalized();

  BodyProfile _readBodyProfile() => BodyProfile(
        birthYear: _store.intOrNull(keyBodyProfileBirthYear),
        weightKg: _store.doubleOrNull(keyBodyProfileWeightKg),
        restingHeartRateBpm: _store.intOrNull(keyBodyProfileRestingHrBpm),
        maxHeartRateBpm: _store.intOrNull(keyBodyProfileMaxHrBpm),
      ).normalized();
  // endregion

  String _acknowledgedFeatureKey(String featureName) =>
      '$_keyAcknowledgedFeaturePrefix$featureName';

  // region Keys and constants (verbatim from the Kotlin companion object). The
  // caffeine, activity recording and hydration keys moved to their stores.
  static const String prefsFile = 'openvitals_prefs';
  static const String currentPrivacyPolicyVersion = '1.0';
  static const int defaultHighHeartRateThresholdBpm = 120;
  static const int defaultLowHeartRateThresholdBpm = 50;
  static const int minHighHeartRateThresholdBpm = 80;
  static const int maxHighHeartRateThresholdBpm = 220;
  static const int minLowHeartRateThresholdBpm = 30;
  static const int maxLowHeartRateThresholdBpm = 100;

  static const String _keyOnboardingDone = 'onboarding_done';
  static const String _keyAcknowledgedPermissions = 'acknowledged_permissions';
  static const String _keyAcknowledgedFeaturePrefix =
      'acknowledged_feature_permissions_';
  static const String _keyLastPromptedPermissionSetVersion =
      'last_prompted_permission_set_version';
  static const String _keyUnitSystem = 'unit_system';
  static const String _keyAppLanguage = 'app_language';
  static const String _keyAppThemeMode = 'app_theme_mode';
  static const String _keyDynamicColor = 'dynamic_color';
  static const String _keyNightStartHour = 'sleep_night_start_hour';
  static const String _keyNightEndHour = 'sleep_night_end_hour';
  static const String _keyChartAggregationMode = 'chart_aggregation_mode';
  static const String _keyActivityWeekMode = 'activity_week_mode';
  static const String _keyActivitySplitDistanceMeters =
      'activity_split_distance_meters';
  static const String _keyShowOpenVitalsCalculatedCalories =
      'show_openvitals_calculated_calories';
  static const String _keyHealthConnectMindfulnessEnabled =
      'health_connect_mindfulness_enabled';
  static const String _keyHealthConnectSyncEnabled =
      'health_connect_sync_enabled';
  static const String _keyHealthConnectPermissionCancelCount =
      'health_connect_permission_cancel_count';
  static const String _keyAcceptedPrivacyPolicyVersion =
      'accepted_privacy_policy_version';
  static const String _keyPrivacyPolicyAcceptedAt = 'privacy_policy_accepted_at';
  static const String _keyAppLockEnabled = 'app_lock_enabled';
  static const String _keyLastActivityExerciseType =
      'last_activity_exercise_type';
  static const String _keyFavoriteActivityExerciseType =
      'favorite_activity_exercise_type';
  static const String _keyDashboardWidgetOrder = 'dashboard_widget_order';
  static const String _keyDashboardHiddenWidgets = 'dashboard_hidden_widgets';
  static const String _keyDashboardRingOrder = 'dashboard_ring_order';
  static const String _keyManualEntryWidgetOrder = 'manual_entry_widget_order';
  static const String _keyMetricDetailSectionOrder =
      'metric_detail_section_order';
  static const String _keyHighHeartRateThresholdBpm =
      'high_heart_rate_threshold_bpm';
  static const String _keyLowHeartRateThresholdBpm =
      'low_heart_rate_threshold_bpm';
  // The body profile keys (and the legacy body-energy/caffeine keys they were
  // migrated from) live in prefs_migrations.dart — see keyBodyProfileBirthYear
  // and friends, imported above.
  static const String _keyBodyEnergyZoneThresholdsBpm =
      'body_energy_zone_thresholds_bpm';
  static const String _keyBodyEnergyUseManualZones =
      'body_energy_use_manual_zones';
  static const String _keyBodyEnergySetupCompleted =
      'body_energy_setup_completed';
  static const String _keyBodyEnergySleepChargeGain =
      'body_energy_sleep_charge_gain';
  static const String _keyBodyEnergyActivityDrainGain =
      'body_energy_activity_drain_gain';
  static const String _keyBodyEnergyBasalDrainGain =
      'body_energy_basal_drain_gain';
  static const String _keyBodyEnergyStressDrainGain =
      'body_energy_stress_drain_gain';
  static const String _keyBodyEnergyFeelCheckCount =
      'body_energy_feel_check_count';
  static const String _keyMindfulnessTimerDurationMinutes =
      'mindfulness_timer_duration_minutes';
  static const String _keyMindfulnessTimerIntervalMinutes =
      'mindfulness_timer_interval_minutes';
  static const String _keyMindfulnessTimerBellSound =
      'mindfulness_timer_bell_sound';
  static const String _keyMindfulnessTimerBackgroundSound =
      'mindfulness_timer_background_sound';
  static const String _keyMindfulnessRemindersEnabled =
      'mindfulness_reminders_enabled';
  static const String _keyMindfulnessReminderTime = 'mindfulness_reminder_time';

  // The separators and the percent-encoding of stored values live in
  // prefs_codec.dart, which is where they are actually applied.

  static const int _defaultMindfulnessTimerDurationMinutes = 10;
  static const int _minMindfulnessTimerMinutes = 1;
  static const int _maxMindfulnessTimerMinutes = 24 * 60;
  static const int _missingExerciseType = -2147483648; // Int.MIN_VALUE
  // endregion
}
