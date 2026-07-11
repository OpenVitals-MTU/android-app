import 'package:collection/collection.dart';

import '../../core/time/local_date.dart';
import '../../domain/model/mindfulness_models.dart';
import '../../domain/model/nutrition_models.dart';
import '../../domain/preferences/activity_recording_dashboard_layout.dart';

/// The string codec that `PreferencesRepository` stores its structured values
/// with.
///
/// Everything in here is **pure**: it maps between domain objects and the
/// separator-delimited strings that live in `shared_preferences`, and it never
/// touches a `SharedPreferences` instance. That is the whole point of the file
/// — these are the parts of the repository that can be tested without a store.
///
/// The separators and the encoding are on-disk format: changing any of them
/// silently orphans existing user data.

/// Separates repeated values inside one stored string (widget orders, layout
/// items, drink id lists).
const String valueSeparator = ',';

/// Separates a key from its value inside one such repeated element.
const String valuePairSeparator = '=';

/// Separates nutrient entries inside an encoded custom hydration drink.
const String nutrientSeparator = ';';

/// Separates the top-level sections of a composite value (the dashboard
/// layout's template from its items; a drink's fields from each other).
const String layoutSectionSeparator = '|';

/// Percent-encodes a value so it cannot contain one of the separators above.
String encodePreferenceValue(String value) => Uri.encodeQueryComponent(value);

String decodePreferenceValue(String value) => Uri.decodeQueryComponent(value);

/// Splits [value] on [separator] into at most [limit] parts, leaving any
/// remaining separators inside the final part (Kotlin's `split(limit = n)`).
List<String> splitWithLimit(String value, String separator, int limit) {
  final parts = <String>[];
  var start = 0;
  while (parts.length < limit - 1) {
    final index = value.indexOf(separator, start);
    if (index < 0) break;
    parts.add(value.substring(start, index));
    start = index + separator.length;
  }
  parts.add(value.substring(start));
  return parts;
}

/// Resolves an enum constant by its `name`, returning null for an unknown or
/// absent value rather than throwing — a stored enum may come from an older or
/// newer build.
T? enumByName<T extends Enum>(List<T> values, String? name) =>
    name == null ? null : values.firstWhereOrNull((e) => e.name == name);

/// Parses `HH:mm` / `HH:mm:ss`, returning null on anything malformed or out of
/// range.
LocalTime? parseLocalTime(String value) {
  final parts = value.split(':');
  if (parts.length < 2) return null;
  final hour = int.tryParse(parts[0]);
  final minute = int.tryParse(parts[1]);
  final second = parts.length > 2 ? int.tryParse(parts[2]) : 0;
  if (hour == null || minute == null || second == null) return null;
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
  if (second < 0 || second > 59) return null;
  return LocalTime(hour, minute, second);
}

LocalTime toReminderTimeOrDefault(String? value, LocalTime fallback) {
  if (value == null) return fallback;
  return parseLocalTime(value) ?? fallback;
}

/// Reads a stored bell sound.
///
/// `'SOFT'` and `'DEEP'` are a **read migration**: they are the names an older
/// build wrote, and they must keep resolving to the sounds that replaced them
/// or an existing user's timer silently changes its bell.
MindfulnessBellSound? toMindfulnessBellSound(String? value) {
  if (value == null) return null;
  switch (value) {
    case 'SOFT':
      return MindfulnessBellSound.struck;
    case 'DEEP':
      return MindfulnessBellSound.temple;
    default:
      return MindfulnessBellSound.fromStorage(value);
  }
}

MindfulnessBackgroundSound? optionalBackgroundSound(String? value) =>
    value == null ? null : MindfulnessBackgroundSound.fromStorage(value);

// region Custom hydration drink serialization.

/// Trims and validates a drink, returning null if it is not storable at all
/// (no id, no name, a non-positive or non-finite volume, a multiplier outside
/// 0..1). Nutrients with a non-positive or non-finite amount are dropped, and
/// the survivors are sorted by storage name so the encoded form is stable.
CustomHydrationDrink? normalizedCustomHydrationDrink(
  CustomHydrationDrink drink,
) {
  final normalizedName = drink.name.trim();
  if (drink.id.isEmpty || normalizedName.isEmpty) return null;
  if (drink.volumeMilliliters <= 0.0 || !drink.volumeMilliliters.isFinite) {
    return null;
  }
  if (drink.hydrationMultiplier < 0.0 ||
      drink.hydrationMultiplier > 1.0 ||
      !drink.hydrationMultiplier.isFinite) {
    return null;
  }
  final filtered = drink.nutrientValues.entries
      .where((e) => e.value > 0.0 && e.value.isFinite)
      .toList()
    ..sort((a, b) => a.key.storageName.compareTo(b.key.storageName));
  final normalizedNutrients = <NutritionNutrient, double>{
    for (final entry in filtered) entry.key: entry.value,
  };
  return drink.copyWith(
    name: normalizedName,
    nutrientValues: normalizedNutrients,
  );
}

String customHydrationDrinkToPreferenceString(CustomHydrationDrink drink) {
  final nutrients = drink.nutrientValues.entries
      .map((e) => '${e.key.storageName}$valuePairSeparator${e.value}')
      .join(nutrientSeparator);
  return [
    encodePreferenceValue(drink.id),
    encodePreferenceValue(drink.name),
    drink.volumeMilliliters.toString(),
    drink.hydrationMultiplier.toString(),
    encodePreferenceValue(nutrients),
  ].join(layoutSectionSeparator);
}

CustomHydrationDrink? toCustomHydrationDrink(String value) {
  final parts = splitWithLimit(value, layoutSectionSeparator, 5);
  if (parts.length < 4) return null;
  final id = decodePreferenceValue(parts[0]);
  if (id.isEmpty) return null;
  final name = decodePreferenceValue(parts[1]);
  if (name.isEmpty) return null;
  final volume = double.tryParse(parts[2]);
  if (volume == null || volume <= 0.0 || !volume.isFinite) return null;
  final parsedMultiplier = double.tryParse(parts[3]);
  final hydrationMultiplier = (parsedMultiplier != null &&
          parsedMultiplier >= 0.0 &&
          parsedMultiplier <= 1.0 &&
          parsedMultiplier.isFinite)
      ? parsedMultiplier
      : 1.0;
  final nutrientValues = <NutritionNutrient, double>{};
  final rawNutrients = parts.length > 4 ? parts[4] : null;
  if (rawNutrients != null) {
    for (final section
        in decodePreferenceValue(rawNutrients).split(nutrientSeparator)) {
      final sections = splitWithLimit(section, valuePairSeparator, 2);
      final nutrient =
          sections.isEmpty ? null : NutritionNutrient.fromStorage(sections[0]);
      if (nutrient == null) continue;
      final amount = sections.length > 1 ? double.tryParse(sections[1]) : null;
      if (amount == null || amount <= 0.0 || !amount.isFinite) continue;
      nutrientValues[nutrient] = amount;
    }
  }
  return CustomHydrationDrink(
    id: id,
    name: name,
    volumeMilliliters: volume,
    hydrationMultiplier: hydrationMultiplier,
    nutrientValues: nutrientValues,
  );
}
// endregion

// region Activity recording dashboard layout serialization.
String layoutToPreferenceString(ActivityRecordingDashboardLayout layout) {
  final normalized = layout.normalized();
  final items = normalized.items
      .map((item) => '${item.field.storageName}$valuePairSeparator'
          '${item.size.toPreferenceString()}')
      .join(valueSeparator);
  return '${normalized.template.storageName}$layoutSectionSeparator$items';
}

/// Decodes a stored layout, returning null when the template is missing or
/// unknown. Unknown *fields* are dropped instead: a layout written by a newer
/// build must still render on this one.
ActivityRecordingDashboardLayout? layoutFromPreferenceString(String value) {
  final sections = splitWithLimit(value, layoutSectionSeparator, 2);
  final template = sections.isEmpty
      ? null
      : ActivityRecordingDashboardTemplate.fromStorage(sections.first);
  if (template == null) return null;
  final fields = <ActivityRecordingDashboardField>[];
  final sizes =
      <ActivityRecordingDashboardField, ActivityRecordingDashboardItemSize>{};
  if (sections.length > 1) {
    for (final entry in sections[1].split(valueSeparator)) {
      final itemSections = splitWithLimit(entry, valuePairSeparator, 2);
      final field = itemSections.isEmpty
          ? null
          : ActivityRecordingDashboardField.fromStorage(itemSections.first);
      if (field == null) continue;
      final size = itemSections.length > 1
          ? ActivityRecordingDashboardItemSize.fromPreferenceString(
              itemSections[1],
            )
          : null;
      fields.add(field);
      if (size != null) sizes[field] = size;
    }
  }
  return ActivityRecordingDashboardLayout(
    template: template,
    fields: fields,
    sizes: sizes,
  ).normalized();
}
// endregion
