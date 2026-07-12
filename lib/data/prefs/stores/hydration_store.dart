import '../../../domain/model/hydration_reminder_config.dart';
import '../../../domain/model/nutrition_models.dart';
import '../prefs_codec.dart';
import '../prefs_store.dart';

/// Storage for the whole hydration cluster: the daily goal, the reminder
/// schedule, the remembered container volumes and custom amount, the custom
/// drink list (and its separate order key), and the one-shot flag saying the
/// beverages have already been moved into the database.
///
/// Two asymmetries here are deliberate and load-bearing:
///  * the daily goal is clamped on **write only** — an out-of-range value that
///    is already on disk is returned as-is;
///  * the last custom amount and the container volumes reject non-positive and
///    non-finite values on write *and* filter them again on read, because a
///    value written by an older build may be either.
class HydrationStore extends PrefsStore {
  const HydrationStore(super.prefs);

  // region Daily goal.
  double readDailyGoalLiters() =>
      prefs.getDouble(_keyHydrationDailyGoalLiters) ??
      _defaultHydrationDailyGoalLiters;

  /// Clamps. The getter above deliberately does not — see the class doc.
  void writeDailyGoalLiters(double value) => putDouble(
        _keyHydrationDailyGoalLiters,
        value
            .clamp(_minHydrationDailyGoalLiters, _maxHydrationDailyGoalLiters)
            .toDouble(),
      );
  // endregion

  // region Reminders.
  HydrationReminderConfig readReminderConfig() => HydrationReminderConfig(
        enabled: prefs.getBool(_keyHydrationRemindersEnabled) ?? false,
        intervalMinutes: prefs.getInt(_keyHydrationReminderIntervalMinutes) ??
            HydrationReminderConfig.defaultIntervalMinutes,
        activeStartTime: toReminderTimeOrDefault(
          prefs.getString(_keyHydrationReminderActiveStartTime),
          HydrationReminderConfig.defaultActiveStartTime,
        ),
        activeEndTime: toReminderTimeOrDefault(
          prefs.getString(_keyHydrationReminderActiveEndTime),
          HydrationReminderConfig.defaultActiveEndTime,
        ),
      ).normalized();

  void writeReminderConfig(HydrationReminderConfig config) {
    final normalized = config.normalized();
    putBool(_keyHydrationRemindersEnabled, normalized.enabled);
    putInt(_keyHydrationReminderIntervalMinutes, normalized.intervalMinutes);
    putString(
      _keyHydrationReminderActiveStartTime,
      normalized.activeStartTime.toString(),
    );
    putString(
      _keyHydrationReminderActiveEndTime,
      normalized.activeEndTime.toString(),
    );
  }
  // endregion

  // region Remembered amounts.
  Map<String, double> readContainerVolumeMilliliters() {
    final result = <String, double>{};
    for (final entry
        in prefs.getStringList(_keyHydrationContainerVolumeMilliliters) ??
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

  void writeContainerVolumeMilliliters(String containerId, double milliliters) {
    if (containerId.isEmpty || milliliters <= 0.0 || !milliliters.isFinite) {
      return;
    }
    final values = readContainerVolumeMilliliters();
    values[containerId] = milliliters;
    putStringList(
      _keyHydrationContainerVolumeMilliliters,
      values.entries
          .map((e) => '${e.key}$valuePairSeparator${e.value}')
          .toSet()
          .toList(),
    );
  }

  double? readLastCustomAmountMilliliters() {
    final milliliters =
        prefs.getDouble(_keyLastCustomHydrationAmountMilliliters) ??
            _missingHydrationAmountMilliliters;
    if (milliliters != _missingHydrationAmountMilliliters &&
        milliliters > 0.0 &&
        milliliters.isFinite) {
      return milliliters;
    }
    return null;
  }

  void writeLastCustomAmountMilliliters(double milliliters) {
    if (milliliters <= 0.0 || !milliliters.isFinite) return;
    putDouble(_keyLastCustomHydrationAmountMilliliters, milliliters);
  }
  // endregion

  // region Custom drinks (the codec itself is in prefs_codec.dart).
  /// The stored drinks in the user's chosen order. Drinks with no entry in the
  /// order key (added by an older build, or by a concurrent write) sort by name
  /// after the ordered ones rather than disappearing.
  List<CustomHydrationDrink> readCustomDrinks() {
    final drinks =
        (prefs.getStringList(_keyCustomHydrationDrinks) ?? const <String>[])
            .map(toCustomHydrationDrink)
            .whereType<CustomHydrationDrink>()
            .toList();
    if (drinks.isEmpty) return const <CustomHydrationDrink>[];

    final drinksById = <String, CustomHydrationDrink>{
      for (final drink in drinks) drink.id: drink,
    };
    final orderedIds = <String>[];
    for (final id in _customDrinkOrder()) {
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

  void saveCustomDrink(CustomHydrationDrink drink) {
    final normalized = normalizedCustomHydrationDrink(drink);
    if (normalized == null) return;
    final current = readCustomDrinks();
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
    _persistCustomDrinks(trimmed);
  }

  void deleteCustomDrink(String drinkId) {
    if (drinkId.isEmpty) return;
    _persistCustomDrinks(
      readCustomDrinks().where((d) => d.id != drinkId).toList(),
    );
  }

  void reorderCustomDrinks(List<String> drinkIds) {
    final current = readCustomDrinks();
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
    _persistCustomDrinks([...orderedDrinks, ...remaining]);
  }

  List<String> _customDrinkOrder() =>
      (prefs.getString(_keyCustomHydrationDrinkOrder)?.split(valueSeparator) ??
              const <String>[])
          .map(decodePreferenceValue)
          .where((it) => it.isNotEmpty)
          .toList();

  void _persistCustomDrinks(List<CustomHydrationDrink> drinks) {
    putStringList(
      _keyCustomHydrationDrinks,
      drinks.map(customHydrationDrinkToPreferenceString).toSet().toList(),
    );
    putString(
      _keyCustomHydrationDrinkOrder,
      drinks
          .map((drink) => encodePreferenceValue(drink.id))
          .join(valueSeparator),
    );
  }
  // endregion

  // region Room migration flag.
  bool hasMigratedBeveragesToRoom() =>
      prefs.getBool(_keyHydrationBeveragesRoomMigrated) ?? false;

  void setMigratedBeveragesToRoom() =>
      putBool(_keyHydrationBeveragesRoomMigrated, true);
  // endregion

  // region Keys and constants (on-disk format — never rename one).
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

  static const double _defaultHydrationDailyGoalLiters = 2.0;
  static const double _minHydrationDailyGoalLiters = 0.25;
  static const double _maxHydrationDailyGoalLiters = 10.0;
  static const double _missingHydrationAmountMilliliters = -1.0;
  static const int _maxCustomHydrationDrinks = 25;
  // endregion
}
