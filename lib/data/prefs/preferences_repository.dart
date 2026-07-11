import 'dart:async';
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
import '../../domain/preferences/sleep_range_mode.dart';
import '../../domain/preferences/unit_system.dart';
import 'device_locale.dart';
import 'prefs_codec.dart';
import 'prefs_migrations.dart';

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
      : _unitSystem = ValueNotifier(_readUnitSystem(_prefs, localeName)),
        _appLanguage = ValueNotifier(_readAppLanguage(_prefs)),
        _appThemeMode = ValueNotifier(_readAppThemeMode(_prefs)),
        _dynamicColor = ValueNotifier(_readDynamicColor(_prefs)),
        _sleepRangeMode = ValueNotifier(_readSleepRangeMode(_prefs)),
        _activityWeekMode = ValueNotifier(_readActivityWeekMode(_prefs)),
        _activitySplitDistanceMeters =
            ValueNotifier(_readActivitySplitDistanceMeters(_prefs)),
        _showOpenVitalsCalculatedCalories =
            ValueNotifier(_readShowOpenVitalsCalculatedCalories(_prefs)),
        _healthConnectSyncEnabled =
            ValueNotifier(_readHealthConnectSyncEnabled(_prefs)) {
    // Runs once per construction, before the body profile is read — it used to
    // hide inside that read, which made a write look like a read. It is a no-op
    // unless a legacy install has values to fold in.
    migrateLegacyBodyProfileValues(_prefs);
    _bodyEnergyCalibration = ValueNotifier(_readBodyEnergyCalibration());
    _caffeinePreferences = ValueNotifier(_readCaffeinePreferences());
    _bodyProfile = ValueNotifier(_readBodyProfile());
  }

  final SharedPreferences _prefs;

  final ValueNotifier<UnitSystem> _unitSystem;
  final ValueNotifier<AppLanguage> _appLanguage;
  final ValueNotifier<AppThemeMode> _appThemeMode;
  final ValueNotifier<bool> _dynamicColor;
  final ValueNotifier<SleepRangeMode> _sleepRangeMode;
  final ValueNotifier<ActivityWeekMode> _activityWeekMode;
  final ValueNotifier<double> _activitySplitDistanceMeters;
  final ValueNotifier<bool> _showOpenVitalsCalculatedCalories;
  final ValueNotifier<bool> _healthConnectSyncEnabled;
  late final ValueNotifier<BodyEnergyCalibration> _bodyEnergyCalibration;
  late final ValueNotifier<CaffeinePreferences> _caffeinePreferences;
  late final ValueNotifier<BodyProfile> _bodyProfile;

  // region Reactive listenables (Kotlin StateFlows).
  ValueListenable<UnitSystem> get unitSystemListenable => _unitSystem;
  ValueListenable<AppLanguage> get appLanguageListenable => _appLanguage;
  ValueListenable<AppThemeMode> get appThemeModeListenable => _appThemeMode;
  ValueListenable<bool> get dynamicColorListenable => _dynamicColor;
  ValueListenable<SleepRangeMode> get sleepRangeModeListenable =>
      _sleepRangeMode;
  ValueListenable<ActivityWeekMode> get activityWeekModeListenable =>
      _activityWeekMode;
  ValueListenable<double> get activitySplitDistanceMetersListenable =>
      _activitySplitDistanceMeters;
  ValueListenable<bool> get showOpenVitalsCalculatedCaloriesListenable =>
      _showOpenVitalsCalculatedCalories;
  ValueListenable<bool> get healthConnectSyncEnabledListenable =>
      _healthConnectSyncEnabled;
  ValueListenable<BodyEnergyCalibration> get bodyEnergyCalibrationListenable =>
      _bodyEnergyCalibration;
  ValueListenable<CaffeinePreferences> get caffeinePreferencesListenable =>
      _caffeinePreferences;
  ValueListenable<BodyProfile> get bodyProfileListenable => _bodyProfile;
  // endregion

  bool get onboardingDone => _prefs.getBool(_keyOnboardingDone) ?? false;
  set onboardingDone(bool value) => _putBool(_keyOnboardingDone, value);

  UnitSystem get unitSystem => _unitSystem.value;
  set unitSystem(UnitSystem value) {
    _putString(_keyUnitSystem, value.name);
    _unitSystem.value = value;
  }

  AppLanguage get appLanguage => _appLanguage.value;
  set appLanguage(AppLanguage value) {
    _putString(_keyAppLanguage, value.name);
    _appLanguage.value = value;
  }

  AppThemeMode get appThemeMode => _appThemeMode.value;
  set appThemeMode(AppThemeMode value) {
    _putString(_keyAppThemeMode, value.name);
    _appThemeMode.value = value;
  }

  bool get dynamicColor => _dynamicColor.value;
  set dynamicColor(bool value) {
    _putBool(_keyDynamicColor, value);
    _dynamicColor.value = value;
  }

  SleepRangeMode get sleepRangeMode => _sleepRangeMode.value;
  set sleepRangeMode(SleepRangeMode value) {
    _putString(_keySleepRangeMode, value.name);
    _sleepRangeMode.value = value;
  }

  ActivityWeekMode get activityWeekMode => _activityWeekMode.value;
  set activityWeekMode(ActivityWeekMode value) {
    _putString(_keyActivityWeekMode, value.name);
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
    _putDouble(_keyActivitySplitDistanceMeters, normalized);
    _activitySplitDistanceMeters.value = normalized;
  }

  WeekPeriodMode get weekPeriodMode => activityWeekMode.toWeekPeriodMode();

  bool get showOpenVitalsCalculatedCalories =>
      _showOpenVitalsCalculatedCalories.value;
  set showOpenVitalsCalculatedCalories(bool value) {
    _putBool(_keyShowOpenVitalsCalculatedCalories, value);
    _showOpenVitalsCalculatedCalories.value = value;
  }

  bool get healthConnectSyncEnabled => _healthConnectSyncEnabled.value;
  set healthConnectSyncEnabled(bool value) {
    _putBool(_keyHealthConnectSyncEnabled, value);
    _healthConnectSyncEnabled.value = value;
  }

  int get healthConnectPermissionCancelCount =>
      _prefs.getInt(_keyHealthConnectPermissionCancelCount) ?? 0;
  set healthConnectPermissionCancelCount(int value) => _putInt(
        _keyHealthConnectPermissionCancelCount,
        value < 0 ? 0 : value,
      );

  String? get acceptedPrivacyPolicyVersion =>
      _prefs.getString(_keyAcceptedPrivacyPolicyVersion);
  set acceptedPrivacyPolicyVersion(String? value) {
    if (value == null) {
      _remove(_keyAcceptedPrivacyPolicyVersion);
    } else {
      _putString(_keyAcceptedPrivacyPolicyVersion, value);
    }
  }

  int get privacyPolicyAcceptedAtMillis =>
      _prefs.getInt(_keyPrivacyPolicyAcceptedAt) ?? 0;
  set privacyPolicyAcceptedAtMillis(int value) =>
      _putInt(_keyPrivacyPolicyAcceptedAt, value);

  bool get appLockEnabled => _prefs.getBool(_keyAppLockEnabled) ?? false;
  set appLockEnabled(bool value) => _putBool(_keyAppLockEnabled, value);

  int? get lastActivityExerciseType {
    final value = _prefs.getInt(_keyLastActivityExerciseType) ??
        _missingExerciseType;
    return value != _missingExerciseType ? value : null;
  }

  set lastActivityExerciseType(int? value) {
    if (value == null) {
      _remove(_keyLastActivityExerciseType);
    } else {
      _putInt(_keyLastActivityExerciseType, value);
    }
  }

  int? get favoriteActivityExerciseType {
    final value = _prefs.getInt(_keyFavoriteActivityExerciseType) ??
        _missingExerciseType;
    return value != _missingExerciseType ? value : null;
  }

  set favoriteActivityExerciseType(int? value) {
    if (value == null) {
      _remove(_keyFavoriteActivityExerciseType);
    } else {
      _putInt(_keyFavoriteActivityExerciseType, value);
    }
  }

  double get hydrationDailyGoalLiters =>
      _prefs.getDouble(_keyHydrationDailyGoalLiters) ??
      _defaultHydrationDailyGoalLiters;
  set hydrationDailyGoalLiters(double value) => _putDouble(
        _keyHydrationDailyGoalLiters,
        value
            .clamp(_minHydrationDailyGoalLiters, _maxHydrationDailyGoalLiters)
            .toDouble(),
      );

  int get highHeartRateThresholdBpm =>
      (_prefs.getInt(_keyHighHeartRateThresholdBpm) ??
              defaultHighHeartRateThresholdBpm)
          .clamp(minHighHeartRateThresholdBpm, maxHighHeartRateThresholdBpm);
  set highHeartRateThresholdBpm(int value) => _putInt(
        _keyHighHeartRateThresholdBpm,
        value.clamp(minHighHeartRateThresholdBpm, maxHighHeartRateThresholdBpm),
      );

  int get lowHeartRateThresholdBpm =>
      (_prefs.getInt(_keyLowHeartRateThresholdBpm) ??
              defaultLowHeartRateThresholdBpm)
          .clamp(minLowHeartRateThresholdBpm, maxLowHeartRateThresholdBpm);
  set lowHeartRateThresholdBpm(int value) => _putInt(
        _keyLowHeartRateThresholdBpm,
        value.clamp(minLowHeartRateThresholdBpm, maxLowHeartRateThresholdBpm),
      );

  int get lastPromptedPermissionSetVersion =>
      _prefs.getInt(_keyLastPromptedPermissionSetVersion) ?? 0;
  set lastPromptedPermissionSetVersion(int value) =>
      _putInt(_keyLastPromptedPermissionSetVersion, value);

  BodyEnergyCalibration bodyEnergyCalibration() => _bodyEnergyCalibration.value;

  void setBodyEnergyCalibration(BodyEnergyCalibration calibration) {
    final normalized = calibration.normalized();
    _putBool(_keyBodyEnergyUseManualZones, normalized.useManualZones);
    _putBool(_keyBodyEnergySetupCompleted, normalized.setupCompleted);
    final zones = normalized.manualZoneThresholdsBpm;
    if (zones != null) {
      _putString(_keyBodyEnergyZoneThresholdsBpm, zones.toPreferenceString());
    } else {
      _remove(_keyBodyEnergyZoneThresholdsBpm);
    }
    _bodyEnergyCalibration.value = normalized;
  }

  BodyProfile bodyProfile() => _bodyProfile.value;

  void setBodyProfile(BodyProfile profile) {
    final normalized = profile.normalized();
    _putOrRemoveInt(keyBodyProfileBirthYear, normalized.birthYear);
    _putOrRemoveDouble(keyBodyProfileWeightKg, normalized.weightKg);
    _putOrRemoveInt(keyBodyProfileRestingHrBpm, normalized.restingHeartRateBpm);
    _putOrRemoveInt(keyBodyProfileMaxHrBpm, normalized.maxHeartRateBpm);
    _bodyProfile.value = normalized;
  }

  CaffeinePreferences caffeinePreferences() => _caffeinePreferences.value;

  void setCaffeinePreferences(CaffeinePreferences preferences) {
    final normalized = preferences.normalized();
    _putBool(_keyCaffeineProfileCompleted, normalized.profileCompleted);
    _putInt(_keyCaffeineHalfLifeMinutes, normalized.halfLifeMinutes);
    _putInt(_keyCaffeineAbsorptionMinutes, normalized.absorptionMinutes);
    _putInt(_keyCaffeineSleepThresholdMg, normalized.sleepThresholdMg);
    _putString(_keyCaffeineBedtime, normalized.bedtime.toString());
    _putString(_keyCaffeineSleepSensitivity, normalized.sleepSensitivity.name);
    _putBool(_keyCaffeineSmoker, normalized.smoker);
    _putString(_keyCaffeineAlcoholUse, normalized.alcoholUse.name);
    _putString(_keyCaffeineHabituation, normalized.caffeineHabituation.name);
    _putBool(_keyCaffeineLiverImpairment, normalized.liverImpairment);
    _putBool(_keyCaffeineMedicationInteraction, normalized.medicationInteraction);
    _putString(_keyCaffeineCyp1a2Genotype, normalized.cyp1a2Genotype.name);
    _putString(_keyCaffeineAhrGenotype, normalized.ahrGenotype.name);
    _putString(_keyCaffeineHormonalStatus, normalized.hormonalStatus.name);
    _caffeinePreferences.value = normalized;
  }

  TimeRange timeRangeFor(PeriodRangePreferenceKey key) =>
      enumByName(TimeRange.values, _prefs.getString(key.storageKey)) ??
      key.defaultRange;

  void setTimeRangeFor(PeriodRangePreferenceKey key, TimeRange range) =>
      _putString(key.storageKey, range.name);

  double dailyGoalFor(MetricDailyGoalKey key) => key.normalize(
        _prefs.getDouble(key.storageKey) ?? key.defaultValue,
      );

  void setDailyGoalFor(MetricDailyGoalKey key, double value) =>
      _putDouble(key.storageKey, key.normalize(value));

  HydrationReminderConfig hydrationReminderConfig() => HydrationReminderConfig(
        enabled: _prefs.getBool(_keyHydrationRemindersEnabled) ?? false,
        intervalMinutes: _prefs.getInt(_keyHydrationReminderIntervalMinutes) ??
            HydrationReminderConfig.defaultIntervalMinutes,
        activeStartTime: toReminderTimeOrDefault(
          _prefs.getString(_keyHydrationReminderActiveStartTime),
          HydrationReminderConfig.defaultActiveStartTime,
        ),
        activeEndTime: toReminderTimeOrDefault(
          _prefs.getString(_keyHydrationReminderActiveEndTime),
          HydrationReminderConfig.defaultActiveEndTime,
        ),
      ).normalized();

  void setHydrationReminderConfig(HydrationReminderConfig config) {
    final normalized = config.normalized();
    _putBool(_keyHydrationRemindersEnabled, normalized.enabled);
    _putInt(_keyHydrationReminderIntervalMinutes, normalized.intervalMinutes);
    _putString(
      _keyHydrationReminderActiveStartTime,
      normalized.activeStartTime.toString(),
    );
    _putString(
      _keyHydrationReminderActiveEndTime,
      normalized.activeEndTime.toString(),
    );
  }

  Map<String, double> hydrationContainerVolumeMilliliters() {
    final result = <String, double>{};
    for (final entry
        in _prefs.getStringList(_keyHydrationContainerVolumeMilliliters) ??
            const <String>[]) {
      final separatorIndex = entry.indexOf(valuePairSeparator);
      if (separatorIndex <= 0 || separatorIndex == entry.length - 1) continue;
      final key = entry.substring(0, separatorIndex);
      final value = double.tryParse(entry.substring(separatorIndex + 1));
      if (value != null && value > 0.0 && value.isFinite) {
        result[key] = value;
      }
    }
    return result;
  }

  void setHydrationContainerVolumeMilliliters(
    String containerId,
    double milliliters,
  ) {
    if (containerId.isEmpty || milliliters <= 0.0 || !milliliters.isFinite) {
      return;
    }
    final values = hydrationContainerVolumeMilliliters();
    values[containerId] = milliliters;
    _putStringList(
      _keyHydrationContainerVolumeMilliliters,
      values.entries
          .map((e) => '${e.key}$valuePairSeparator${e.value}')
          .toSet()
          .toList(),
    );
  }

  double? lastCustomHydrationAmountMilliliters() {
    final milliliters =
        _prefs.getDouble(_keyLastCustomHydrationAmountMilliliters) ??
            _missingHydrationAmountMilliliters;
    if (milliliters != _missingHydrationAmountMilliliters &&
        milliliters > 0.0 &&
        milliliters.isFinite) {
      return milliliters;
    }
    return null;
  }

  void setLastCustomHydrationAmountMilliliters(double milliliters) {
    if (milliliters <= 0.0 || !milliliters.isFinite) return;
    _putDouble(_keyLastCustomHydrationAmountMilliliters, milliliters);
  }

  List<CustomHydrationDrink> customHydrationDrinks() {
    final drinks =
        (_prefs.getStringList(_keyCustomHydrationDrinks) ?? const <String>[])
            .map(toCustomHydrationDrink)
            .whereType<CustomHydrationDrink>()
            .toList();
    if (drinks.isEmpty) return const <CustomHydrationDrink>[];

    final drinksById = <String, CustomHydrationDrink>{
      for (final drink in drinks) drink.id: drink,
    };
    final orderedIds = <String>[];
    for (final id in _customHydrationDrinkOrder()) {
      if (drinksById.containsKey(id) && !orderedIds.contains(id)) {
        orderedIds.add(id);
      }
    }
    final orderedDrinks =
        orderedIds.map((id) => drinksById[id]).whereType<CustomHydrationDrink>();
    final orderedIdSet = orderedIds.toSet();
    final missingOrderDrinks = drinks
        .where((drink) => !orderedIdSet.contains(drink.id))
        .toList()
      ..sort((a, b) => a.name.toLowerCase().compareTo(b.name.toLowerCase()));
    return [...orderedDrinks, ...missingOrderDrinks];
  }

  void saveCustomHydrationDrink(CustomHydrationDrink drink) {
    final normalized = normalizedCustomHydrationDrink(drink);
    if (normalized == null) return;
    final current = customHydrationDrinks();
    bool matches(CustomHydrationDrink d) =>
        d.id == normalized.id ||
        d.name.toLowerCase() == normalized.name.toLowerCase();
    final existingIndex = current.indexWhere(matches);
    final values = current.where((d) => !matches(d)).toList();
    if (existingIndex >= 0) {
      values.insert(existingIndex.clamp(0, values.length), normalized);
    } else {
      values.add(normalized);
    }
    final trimmed = values.length > _maxCustomHydrationDrinks
        ? values.sublist(values.length - _maxCustomHydrationDrinks)
        : values;
    _persistCustomHydrationDrinks(trimmed);
  }

  void deleteCustomHydrationDrink(String drinkId) {
    if (drinkId.isEmpty) return;
    _persistCustomHydrationDrinks(
      customHydrationDrinks().where((d) => d.id != drinkId).toList(),
    );
  }

  void reorderCustomHydrationDrinks(List<String> drinkIds) {
    final current = customHydrationDrinks();
    final drinksById = <String, CustomHydrationDrink>{
      for (final drink in current) drink.id: drink,
    };
    final orderedIds = <String>[];
    for (final id in drinkIds) {
      if (drinksById.containsKey(id) && !orderedIds.contains(id)) {
        orderedIds.add(id);
      }
    }
    final orderedDrinks =
        orderedIds.map((id) => drinksById[id]).whereType<CustomHydrationDrink>();
    final orderedIdSet = orderedIds.toSet();
    final remaining = current.where((d) => !orderedIdSet.contains(d.id));
    _persistCustomHydrationDrinks([...orderedDrinks, ...remaining]);
  }

  bool hasMigratedHydrationBeveragesToRoom() =>
      _prefs.getBool(_keyHydrationBeveragesRoomMigrated) ?? false;

  void setMigratedHydrationBeveragesToRoom() =>
      _putBool(_keyHydrationBeveragesRoomMigrated, true);

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
    _putBool(_keyMindfulnessRemindersEnabled, normalized.enabled);
    _putString(
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
    _putInt(_keyMindfulnessTimerDurationMinutes, duration);
    _putInt(_keyMindfulnessTimerIntervalMinutes, interval ?? 0);
    _putString(_keyMindfulnessTimerBellSound, config.bellSound.storageName);
    _putString(
      _keyMindfulnessTimerBackgroundSound,
      config.backgroundSound.storageName,
    );
  }

  List<String>? dashboardWidgetOrder() => _readOrderList(_keyDashboardWidgetOrder);

  void setDashboardWidgetOrder(List<String> widgetIds) =>
      _putString(_keyDashboardWidgetOrder, widgetIds.join(valueSeparator));

  List<String>? dashboardRingOrder() => _readOrderList(_keyDashboardRingOrder);

  void setDashboardRingOrder(List<String> ringIds) =>
      _putString(_keyDashboardRingOrder, ringIds.join(valueSeparator));

  /// The dashboard metric tiles the user has hidden (keyed by tile title).
  Set<String> dashboardHiddenWidgets() =>
      (_prefs.getStringList(_keyDashboardHiddenWidgets) ?? const <String>[])
          .toSet();

  void setDashboardHiddenWidgets(Set<String> widgetIds) =>
      _putStringList(_keyDashboardHiddenWidgets, widgetIds.toList());

  List<String>? manualEntryWidgetOrder() =>
      _readOrderList(_keyManualEntryWidgetOrder);

  void setManualEntryWidgetOrder(List<String> widgetIds) =>
      _putString(_keyManualEntryWidgetOrder, widgetIds.join(valueSeparator));

  List<String>? metricDetailSectionOrder() =>
      _readOrderList(_keyMetricDetailSectionOrder);

  void setMetricDetailSectionOrder(List<String> sectionIds) => _putString(
        _keyMetricDetailSectionOrder,
        sectionIds.join(valueSeparator),
      );

  Set<String> acknowledgedPermissions() =>
      (_prefs.getStringList(_keyAcknowledgedPermissions) ?? const <String>[])
          .toSet();

  void acknowledgePermissions(Set<String> permissions) => _putStringList(
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
    _putStringList(
      _acknowledgedFeatureKey(featureName),
      {...acknowledgedPermissionsFor(featureName), ...permissions}.toList(),
    );
  }

  // region Activity recording preferences.
  ActivityRecordingPreferences activityRecordingPreferences() {
    int? nullableIfSentinel(String key, int defaultValue, int sentinel) {
      final value = _prefs.getInt(key) ?? defaultValue;
      return value == sentinel ? null : value;
    }

    return ActivityRecordingPreferences(
      autoIdleEnabled: _prefs.getBool(_keyActivityRecordingAutoIdleEnabled) ??
          ActivityRecordingPreferences.defaultAutoIdleEnabled,
      autoIdleTimeoutSeconds:
          _prefs.getInt(_keyActivityRecordingAutoIdleTimeoutSeconds) ??
              ActivityRecordingPreferences.defaultAutoIdleTimeoutSeconds,
      keepScreenOnDuringRecording:
          _prefs.getBool(_keyActivityRecordingKeepScreenOn) ??
              ActivityRecordingPreferences.defaultKeepScreenOnDuringRecording,
      requiredGpsAccuracyMeters:
          _prefs.getInt(_keyActivityRecordingRequiredGpsAccuracyMeters) ??
              ActivityRecordingPreferences.defaultRequiredGpsAccuracyMeters,
      routeGapMeters: nullableIfSentinel(
        _keyActivityRecordingRouteGapMeters,
        ActivityRecordingPreferences.defaultRouteGapMeters,
        _routeGapOff,
      ),
      barometerClimbEnabled:
          _prefs.getBool(_keyActivityRecordingBarometerClimbEnabled) ??
              ActivityRecordingPreferences.defaultBarometerClimbEnabled,
      recordingDistanceIntervalMeters: nullableIfSentinel(
        _keyActivityRecordingDistanceIntervalMeters,
        ActivityRecordingPreferences.defaultRecordingDistanceIntervalMeters ??
            _recordingIntervalOff,
        _recordingIntervalOff,
      ),
      recordingTimeIntervalMillis:
          _prefs.getInt(_keyActivityRecordingTimeIntervalMillis) ??
              ActivityRecordingPreferences.defaultRecordingTimeIntervalMillis,
      voiceAnnouncementsEnabled:
          _prefs.getBool(_keyActivityRecordingVoiceEnabled) ??
              ActivityRecordingPreferences.defaultVoiceAnnouncementsEnabled,
      voiceAnnouncementTimeIntervalMinutes: nullableIfSentinel(
        _keyActivityRecordingVoiceTimeIntervalMinutes,
        ActivityRecordingPreferences.defaultVoiceAnnouncementTimeIntervalMinutes,
        _recordingIntervalOff,
      ),
      voiceAnnouncementDistanceIntervalMeters: nullableIfSentinel(
        _keyActivityRecordingVoiceDistanceIntervalMeters,
        ActivityRecordingPreferences
            .defaultVoiceAnnouncementDistanceIntervalMeters,
        _recordingIntervalOff,
      ),
      voiceIdleAnnouncementsEnabled:
          _prefs.getBool(_keyActivityRecordingVoiceIdleEnabled) ??
              ActivityRecordingPreferences.defaultVoiceIdleAnnouncementsEnabled,
      voiceLapAnnouncementsEnabled:
          _prefs.getBool(_keyActivityRecordingVoiceLapEnabled) ??
              ActivityRecordingPreferences.defaultVoiceLapAnnouncementsEnabled,
      restTimerBellEnabled:
          _prefs.getBool(_keyActivityRecordingRestTimerBellEnabled) ??
              ActivityRecordingPreferences.defaultRestTimerBellEnabled,
    ).normalized();
  }

  void setActivityRecordingPreferences(ActivityRecordingPreferences preferences) {
    final normalized = preferences.normalized();
    _putBool(_keyActivityRecordingAutoIdleEnabled, normalized.autoIdleEnabled);
    _putInt(
      _keyActivityRecordingAutoIdleTimeoutSeconds,
      normalized.autoIdleTimeoutSeconds,
    );
    _putBool(
      _keyActivityRecordingKeepScreenOn,
      normalized.keepScreenOnDuringRecording,
    );
    _putInt(
      _keyActivityRecordingRequiredGpsAccuracyMeters,
      normalized.requiredGpsAccuracyMeters,
    );
    _putInt(
      _keyActivityRecordingRouteGapMeters,
      normalized.routeGapMeters ?? _routeGapOff,
    );
    _putBool(
      _keyActivityRecordingBarometerClimbEnabled,
      normalized.barometerClimbEnabled,
    );
    _putInt(
      _keyActivityRecordingDistanceIntervalMeters,
      normalized.recordingDistanceIntervalMeters ?? _recordingIntervalOff,
    );
    _putInt(
      _keyActivityRecordingTimeIntervalMillis,
      normalized.recordingTimeIntervalMillis,
    );
    _putBool(
      _keyActivityRecordingVoiceEnabled,
      normalized.voiceAnnouncementsEnabled,
    );
    _putInt(
      _keyActivityRecordingVoiceTimeIntervalMinutes,
      normalized.voiceAnnouncementTimeIntervalMinutes ?? _recordingIntervalOff,
    );
    _putInt(
      _keyActivityRecordingVoiceDistanceIntervalMeters,
      normalized.voiceAnnouncementDistanceIntervalMeters ?? _recordingIntervalOff,
    );
    _putBool(
      _keyActivityRecordingVoiceIdleEnabled,
      normalized.voiceIdleAnnouncementsEnabled,
    );
    _putBool(
      _keyActivityRecordingVoiceLapEnabled,
      normalized.voiceLapAnnouncementsEnabled,
    );
    _putBool(
      _keyActivityRecordingRestTimerBellEnabled,
      normalized.restTimerBellEnabled,
    );
  }

  ActivityRecordingDashboardLayout activityRecordingDashboardLayout(
    String activityTypeId,
  ) {
    final raw =
        _prefs.getString(_activityRecordingDashboardLayoutKey(activityTypeId));
    if (raw == null) return ActivityRecordingDashboardLayout();
    return layoutFromPreferenceString(raw) ??
        ActivityRecordingDashboardLayout();
  }

  void setActivityRecordingDashboardLayout(
    String activityTypeId,
    ActivityRecordingDashboardLayout layout,
  ) {
    if (activityTypeId.trim().isEmpty) return;
    _putString(
      _activityRecordingDashboardLayoutKey(activityTypeId),
      layoutToPreferenceString(layout),
    );
  }
  // endregion

  /// Disposes the reactive [ValueNotifier]s.
  void dispose() {
    _unitSystem.dispose();
    _appLanguage.dispose();
    _appThemeMode.dispose();
    _dynamicColor.dispose();
    _sleepRangeMode.dispose();
    _activityWeekMode.dispose();
    _activitySplitDistanceMeters.dispose();
    _showOpenVitalsCalculatedCalories.dispose();
    _healthConnectSyncEnabled.dispose();
    _bodyEnergyCalibration.dispose();
    _caffeinePreferences.dispose();
    _bodyProfile.dispose();
  }

  // region Reads used to seed the reactive notifiers.
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

  static SleepRangeMode _readSleepRangeMode(SharedPreferences prefs) =>
      enumByName(SleepRangeMode.values, prefs.getString(_keySleepRangeMode)) ??
      SleepRangeMode.evening18h;

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

  BodyEnergyCalibration _readBodyEnergyCalibration() => BodyEnergyCalibration(
        manualZoneThresholdsBpm: HeartZoneThresholds.fromPreferenceString(
          _prefs.getString(_keyBodyEnergyZoneThresholdsBpm),
        ),
        useManualZones: _prefs.getBool(_keyBodyEnergyUseManualZones) ?? false,
        setupCompleted: _prefs.getBool(_keyBodyEnergySetupCompleted) ?? false,
      ).normalized();

  BodyProfile _readBodyProfile() => BodyProfile(
        birthYear: _intOrNull(keyBodyProfileBirthYear),
        weightKg: _doubleOrNull(keyBodyProfileWeightKg),
        restingHeartRateBpm: _intOrNull(keyBodyProfileRestingHrBpm),
        maxHeartRateBpm: _intOrNull(keyBodyProfileMaxHrBpm),
      ).normalized();

  CaffeinePreferences _readCaffeinePreferences() => CaffeinePreferences(
        profileCompleted: _prefs.getBool(_keyCaffeineProfileCompleted) ?? false,
        halfLifeMinutes: _prefs.getInt(_keyCaffeineHalfLifeMinutes) ??
            CaffeinePreferences.defaultHalfLifeMinutes,
        absorptionMinutes: _prefs.getInt(_keyCaffeineAbsorptionMinutes) ??
            CaffeinePreferences.defaultAbsorptionMinutes,
        sleepThresholdMg: _prefs.getInt(_keyCaffeineSleepThresholdMg) ??
            CaffeinePreferences.defaultSleepThresholdMg,
        bedtime: toReminderTimeOrDefault(
          _prefs.getString(_keyCaffeineBedtime),
          CaffeinePreferences.defaultBedtime,
        ),
        sleepSensitivity: enumByName(
              CaffeineSleepSensitivity.values,
              _prefs.getString(_keyCaffeineSleepSensitivity),
            ) ??
            CaffeineSleepSensitivity.normal,
        smoker: _prefs.getBool(_keyCaffeineSmoker) ?? false,
        alcoholUse: enumByName(
              CaffeineAlcoholUse.values,
              _prefs.getString(_keyCaffeineAlcoholUse),
            ) ??
            CaffeineAlcoholUse.none,
        caffeineHabituation: enumByName(
              CaffeineHabituation.values,
              _prefs.getString(_keyCaffeineHabituation),
            ) ??
            CaffeineHabituation.moderate,
        liverImpairment: _prefs.getBool(_keyCaffeineLiverImpairment) ?? false,
        medicationInteraction:
            _prefs.getBool(_keyCaffeineMedicationInteraction) ?? false,
        cyp1a2Genotype: enumByName(
              CaffeineGenotype.values,
              _prefs.getString(_keyCaffeineCyp1a2Genotype),
            ) ??
            CaffeineGenotype.unknown,
        ahrGenotype: enumByName(
              CaffeineGenotype.values,
              _prefs.getString(_keyCaffeineAhrGenotype),
            ) ??
            CaffeineGenotype.unknown,
        hormonalStatus: enumByName(
              CaffeineHormonalStatus.values,
              _prefs.getString(_keyCaffeineHormonalStatus),
            ) ??
            CaffeineHormonalStatus.none,
      ).normalized();
  // endregion

  // region Custom hydration drink storage (the codec itself is in
  // prefs_codec.dart).
  List<String> _customHydrationDrinkOrder() =>
      (_prefs.getString(_keyCustomHydrationDrinkOrder)?.split(valueSeparator) ??
              const <String>[])
          .map(decodePreferenceValue)
          .where((it) => it.isNotEmpty)
          .toList();

  void _persistCustomHydrationDrinks(List<CustomHydrationDrink> drinks) {
    _putStringList(
      _keyCustomHydrationDrinks,
      drinks.map(customHydrationDrinkToPreferenceString).toSet().toList(),
    );
    _putString(
      _keyCustomHydrationDrinkOrder,
      drinks
          .map((drink) => encodePreferenceValue(drink.id))
          .join(valueSeparator),
    );
  }
  // endregion

  // region Small helpers.
  List<String>? _readOrderList(String key) => _prefs
      .getString(key)
      ?.split(valueSeparator)
      .where((it) => it.isNotEmpty)
      .toList();

  String _acknowledgedFeatureKey(String featureName) =>
      '$_keyAcknowledgedFeaturePrefix$featureName';

  String _activityRecordingDashboardLayoutKey(String activityTypeId) =>
      '$_keyActivityRecordingDashboardLayoutPrefix$activityTypeId';

  int? _intOrNull(String key) => _prefs.containsKey(key) ? _prefs.getInt(key) : null;

  double? _doubleOrNull(String key) =>
      _prefs.containsKey(key) ? _prefs.getDouble(key) : null;

  void _putOrRemoveInt(String key, int? value) {
    if (value != null) {
      _putInt(key, value);
    } else {
      _remove(key);
    }
  }

  void _putOrRemoveDouble(String key, double? value) {
    if (value != null) {
      _putDouble(key, value);
    } else {
      _remove(key);
    }
  }

  void _putString(String key, String value) =>
      unawaited(_prefs.setString(key, value));

  void _putBool(String key, bool value) =>
      unawaited(_prefs.setBool(key, value));

  void _putInt(String key, int value) => unawaited(_prefs.setInt(key, value));

  void _putDouble(String key, double value) =>
      unawaited(_prefs.setDouble(key, value));

  void _putStringList(String key, List<String> value) =>
      unawaited(_prefs.setStringList(key, value));

  void _remove(String key) => unawaited(_prefs.remove(key));
  // endregion

  // region Keys and constants (verbatim from the Kotlin companion object).
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
  static const String _keySleepRangeMode = 'sleep_range_mode';
  static const String _keyActivityWeekMode = 'activity_week_mode';
  static const String _keyActivitySplitDistanceMeters =
      'activity_split_distance_meters';
  static const String _keyActivityRecordingAutoIdleEnabled =
      'activity_recording_auto_idle_enabled';
  static const String _keyActivityRecordingAutoIdleTimeoutSeconds =
      'activity_recording_auto_idle_timeout_seconds';
  static const String _keyActivityRecordingKeepScreenOn =
      'activity_recording_keep_screen_on';
  static const String _keyActivityRecordingRequiredGpsAccuracyMeters =
      'activity_recording_required_gps_accuracy_meters';
  static const String _keyActivityRecordingRouteGapMeters =
      'activity_recording_route_gap_meters';
  static const String _keyActivityRecordingBarometerClimbEnabled =
      'activity_recording_barometer_climb_enabled';
  static const String _keyActivityRecordingDistanceIntervalMeters =
      'activity_recording_distance_interval_meters';
  static const String _keyActivityRecordingTimeIntervalMillis =
      'activity_recording_time_interval_millis';
  static const String _keyActivityRecordingVoiceEnabled =
      'activity_recording_voice_enabled';
  static const String _keyActivityRecordingVoiceTimeIntervalMinutes =
      'activity_recording_voice_time_interval_minutes';
  static const String _keyActivityRecordingVoiceDistanceIntervalMeters =
      'activity_recording_voice_distance_interval_meters';
  static const String _keyActivityRecordingVoiceIdleEnabled =
      'activity_recording_voice_idle_enabled';
  static const String _keyActivityRecordingVoiceLapEnabled =
      'activity_recording_voice_lap_enabled';
  static const String _keyActivityRecordingRestTimerBellEnabled =
      'activity_recording_rest_timer_bell_enabled';
  static const String _keyActivityRecordingDashboardLayoutPrefix =
      'activity_recording_dashboard_layout_';
  static const int _routeGapOff = 0;
  static const int _recordingIntervalOff = 0;
  static const String _keyShowOpenVitalsCalculatedCalories =
      'show_openvitals_calculated_calories';
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
  static const String _keyHydrationDailyGoalLiters =
      'hydration_daily_goal_liters';
  static const String _keyHydrationContainerVolumeMilliliters =
      'hydration_container_volume_milliliters';
  static const String _keyLastCustomHydrationAmountMilliliters =
      'last_custom_hydration_amount_milliliters';
  static const String _keyCustomHydrationDrinks = 'custom_hydration_drinks';
  static const String _keyCustomHydrationDrinkOrder =
      'custom_hydration_drink_order';
  static const String _keyHydrationBeveragesRoomMigrated =
      'hydration_beverages_room_migrated';
  static const String _keyHydrationRemindersEnabled =
      'hydration_reminders_enabled';
  static const String _keyHydrationReminderIntervalMinutes =
      'hydration_reminder_interval_minutes';
  static const String _keyHydrationReminderActiveStartTime =
      'hydration_reminder_active_start_time';
  static const String _keyHydrationReminderActiveEndTime =
      'hydration_reminder_active_end_time';
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
  static const String _keyCaffeineProfileCompleted =
      'caffeine_profile_completed';
  static const String _keyCaffeineHalfLifeMinutes = 'caffeine_half_life_minutes';
  static const String _keyCaffeineAbsorptionMinutes =
      'caffeine_absorption_minutes';
  static const String _keyCaffeineSleepThresholdMg =
      'caffeine_sleep_threshold_mg';
  static const String _keyCaffeineBedtime = 'caffeine_bedtime';
  static const String _keyCaffeineSleepSensitivity =
      'caffeine_sleep_sensitivity';
  static const String _keyCaffeineSmoker = 'caffeine_smoker';
  static const String _keyCaffeineAlcoholUse = 'caffeine_alcohol_use';
  static const String _keyCaffeineHabituation = 'caffeine_habituation';
  static const String _keyCaffeineLiverImpairment = 'caffeine_liver_impairment';
  static const String _keyCaffeineMedicationInteraction =
      'caffeine_medication_interaction';
  static const String _keyCaffeineCyp1a2Genotype = 'caffeine_cyp1a2_genotype';
  static const String _keyCaffeineAhrGenotype = 'caffeine_ahr_genotype';
  static const String _keyCaffeineHormonalStatus = 'caffeine_hormonal_status';
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

  static const double _defaultHydrationDailyGoalLiters = 2.0;
  static const double _minHydrationDailyGoalLiters = 0.25;
  static const double _maxHydrationDailyGoalLiters = 10.0;
  static const int _defaultMindfulnessTimerDurationMinutes = 10;
  static const int _minMindfulnessTimerMinutes = 1;
  static const int _maxMindfulnessTimerMinutes = 24 * 60;
  static const int _missingExerciseType = -2147483648; // Int.MIN_VALUE
  static const double _missingHydrationAmountMilliliters = -1.0;
  static const int _maxCustomHydrationDrinks = 25;
  // endregion
}
