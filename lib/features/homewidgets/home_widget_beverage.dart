import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../../core/presentation/unit_formatter.dart';
import '../../domain/model/caffeine_models.dart';
import '../../domain/model/nutrition_models.dart';
import '../../domain/preferences/unit_system.dart';
import '../../l10n/app_localizations.dart';
import '../manualentry/application/hydration_entry_notifier.dart';
import 'home_widget_service.dart';

/// The quick-beverage home widgets: drink ordering, labels, and the cached drink
/// payload the background log callback works from.
///
/// Ported from the Kotlin `HomeQuickBeverageWidget.kt` +
/// `HomeQuickBeverageWidgetDrinkOrdering.kt`, with one structural deviation that
/// drives the whole design:
///
/// **Kotlin's widget re-reads the drink from Room on every tap**; ours cannot.
/// The drink catalog lives in a drift `BeverageStore`, and the tap is handled in
/// a *background isolate* (`home_widget`'s interactivity callback), where opening
/// a second connection to the database is a real risk — exactly the reason the
/// reminder alarm and the widget-refresh alarm hand-build their object graphs
/// without drift. So the chosen drink is snapshotted into widget storage at
/// configure/refresh time, in the foreground, where drift is live: see
/// [encodeQuickBeverageDrink] / [homeWidgetDrinkPayloadKey]. The callback logs
/// from that cached payload through Health Connect, which the reminder alarms
/// already prove works from a background isolate.

/// Key (under the instance prefix) holding the configured drink, JSON-encoded.
///
/// Not part of the rendered snapshot — the composables never read it. It exists
/// solely so the background log callback can reconstruct the drink without drift.
const String homeWidgetDrinkPayloadKey = 'drink_payload';

/// The subtitle a configured beverage widget rests at (Kotlin
/// `home_quick_beverage_widget_tap_to_log`). The 1x1 widget renders a third line
/// only when the subtitle is something *else* — i.e. a transient confirmation or
/// an error.
String quickBeverageTapToLogSubtitle(AppLocalizations l10n) =>
    l10n.homeQuickBeverageWidgetTapToLog;

/// The route a configured beverage widget's "Edit" button opens: the hydration
/// entry screen pre-loaded with this drink (Kotlin `Screen.HydrationEntryLogDrink`).
String quickBeverageDrinkRoute(String drinkId) =>
    'manual_entry/hydration/log/$drinkId';

/// The route an *unconfigured* beverage widget opens (Kotlin `Screen.HydrationEntry`).
const String quickBeverageEntryRoute = 'manual_entry/hydration';

/// The drinks the widget's configuration picker offers, in order. Port of the
/// Kotlin `quickBeverageWidgetDrinkOptions`.
///
/// Frequent drinks first (in their own ranking), then the user's own drinks,
/// then the preloaded catalog — the latter two by category, name, id.
List<CustomHydrationDrink> quickBeverageWidgetDrinkOptions({
  required List<CustomHydrationDrink> drinks,
  required List<CustomHydrationDrink> frequentDrinks,
}) {
  if (drinks.isEmpty) return const <CustomHydrationDrink>[];
  final drinkById = {for (final drink in drinks) drink.id: drink};
  final frequentOptions = <CustomHydrationDrink>[
    for (final drink in frequentDrinks)
      if (drinkById[drink.id] != null) drinkById[drink.id]!,
  ];
  final frequentIds = {for (final drink in frequentOptions) drink.id};
  final customOptions = drinks
      .where((drink) => !drink.isPreloaded && !frequentIds.contains(drink.id))
      .toList()
    ..sort(_compareQuickBeverageDrinks);
  final customIds = {for (final drink in customOptions) drink.id};
  final catalogOptions = drinks
      .where((drink) =>
          !frequentIds.contains(drink.id) && !customIds.contains(drink.id))
      .toList()
    ..sort(_compareQuickBeverageDrinks);

  return [...frequentOptions, ...customOptions, ...catalogOptions];
}

int _compareQuickBeverageDrinks(CustomHydrationDrink a, CustomHydrationDrink b) {
  final byCategory = _quickBeverageCategoryOrder(a.category)
      .compareTo(_quickBeverageCategoryOrder(b.category));
  if (byCategory != 0) return byCategory;
  final byName = a.name.toLowerCase().compareTo(b.name.toLowerCase());
  if (byName != 0) return byName;
  return a.id.compareTo(b.id);
}

/// Kotlin `CaffeineSourceCategory?.quickBeverageCategoryOrder()`.
int _quickBeverageCategoryOrder(CaffeineSourceCategory? category) =>
    switch (category) {
      CaffeineSourceCategory.water => 0,
      CaffeineSourceCategory.coffee => 1,
      CaffeineSourceCategory.energyDrink => 2,
      CaffeineSourceCategory.tea => 3,
      CaffeineSourceCategory.chocolate => 4,
      CaffeineSourceCategory.soda => 5,
      CaffeineSourceCategory.supplement ||
      CaffeineSourceCategory.other ||
      null =>
        6,
    };

/// The amount a beverage widget shows. Port of the Kotlin
/// `quickBeverageAmountLabel`: a metric sub-litre volume reads "250ml" — no
/// space, unlike the entry screen's `hydrationAmountLabel` — and anything else
/// falls back to the formatter's hydration text ("1.5 L", "8 fl oz").
String quickBeverageAmountLabel(
  CustomHydrationDrink drink,
  UnitFormatter formatter,
) {
  if (formatter.unitSystem() == UnitSystem.metric && drink.volumeLiters < 1.0) {
    return '${formatter.count(drink.volumeMilliliters.round())}ml';
  }
  return formatter.hydration(drink.volumeLiters).text;
}

/// The snapshot a configured beverage widget renders (Kotlin
/// `loadQuickBeverageSnapshot`).
///
/// The amount rides on [HomeWidgetSnapshot.value] and the drink id on the
/// instance's `selection_id`; Kotlin has a separate `HomeQuickBeverageSnapshot`,
/// but one schema serves every widget here.
///
/// [subtitle] overrides the resting "Tap to log" — the log callback uses it for
/// the transient "Saved now" and for the error states.
///
/// The resting text also rides along in [HomeWidgetSnapshot.unit], which the
/// beverage widgets otherwise do not use. That is how the 1x1 knows whether its
/// subtitle is worth a third line: it shows one only when the subtitle is
/// *something other than* the resting text. Kotlin could compare against its own
/// `R.string.home_quick_beverage_widget_tap_to_log`, but here the subtitle is
/// localized in Dart while the native strings are English-only — so a French
/// tile would never match, and would show its resting subtitle forever.
HomeWidgetSnapshot buildQuickBeverageSnapshot(
  CustomHydrationDrink drink,
  UnitFormatter formatter,
  AppLocalizations l10n, {
  String? subtitle,
}) =>
    HomeWidgetSnapshot(
      title: drink.name,
      value: quickBeverageAmountLabel(drink, formatter),
      unit: quickBeverageTapToLogSubtitle(l10n),
      subtitle: subtitle ?? quickBeverageTapToLogSubtitle(l10n),
      route: quickBeverageDrinkRoute(drink.id),
    );

/// What an instance shows when its drink is gone (deleted from the catalog) —
/// the same fallback the native composables render before the first push
/// (Kotlin `loadQuickBeverageSnapshot`'s null-drink branch).
HomeWidgetSnapshot unconfiguredQuickBeverageSnapshot(AppLocalizations l10n) =>
    HomeWidgetSnapshot(
      title: l10n.homeQuickBeverageWidgetConfigTitle,
      value: '--',
      subtitle: l10n.homeQuickBeverageWidgetNotConfigured,
      route: quickBeverageEntryRoute,
    );

/// The drink [appWidgetId] is configured with, rebuilt from the cached payload —
/// or null while the instance is unconfigured (or its payload is unusable).
///
/// The `selection_id` remains the authoritative record of *what* the user picked
/// (the same handshake the metric widget uses); the payload only carries the
/// data. A payload naming a different drink is stale — Android recycles
/// appWidgetIds — so it is refused rather than logged against the wrong drink.
Future<CustomHydrationDrink?> readQuickBeverageDrink(
  HomeWidgetService service, {
  required HomeWidgetId widget,
  required int appWidgetId,
}) async {
  final selectionId =
      await service.selectionIdOf(widget, appWidgetId: appWidgetId);
  if (selectionId == null || selectionId.isEmpty) return null;
  final drink = decodeQuickBeverageDrink(
    await service.readInstanceKey(
      widget,
      appWidgetId: appWidgetId,
      key: homeWidgetDrinkPayloadKey,
    ),
  );
  if (drink == null || drink.id != selectionId) return null;
  return drink;
}

/// The configured drink, flattened to JSON for widget storage.
///
/// Only what logging and rendering need: id, name, volume, hydration multiplier
/// and the nutrients (keyed by their storage names, so the map round-trips the
/// same way it does in the drift store). Dropping the nutrients here would
/// silently drop a drink's caffeine on every widget tap.
String encodeQuickBeverageDrink(CustomHydrationDrink drink) => jsonEncode({
      'id': drink.id,
      'name': drink.name,
      'volumeMilliliters': drink.volumeMilliliters,
      'hydrationMultiplier': drink.hydrationMultiplier,
      'nutrients': <String, double>{
        for (final entry in drink.nutrientValues.entries)
          entry.key.storageName: entry.value,
      },
    });

/// The drink cached by [encodeQuickBeverageDrink], or null when the payload is
/// absent, malformed or would not be a loggable drink.
///
/// Runs in the background isolate on every tap, so it is defensive: a corrupt
/// payload must degrade to the "select a beverage" state, never throw (an
/// exception escaping the isolate is fatal, and Android will not retry).
CustomHydrationDrink? decodeQuickBeverageDrink(String? payload) {
  if (payload == null || payload.isEmpty) return null;
  try {
    final decoded = jsonDecode(payload);
    if (decoded is! Map<String, dynamic>) return null;
    final id = decoded['id'];
    final name = decoded['name'];
    final volume = decoded['volumeMilliliters'];
    if (id is! String || name is! String || volume is! num) return null;
    final multiplier = decoded['hydrationMultiplier'];
    final nutrients = decoded['nutrients'];
    final nutrientValues = <NutritionNutrient, double>{};
    if (nutrients is Map) {
      for (final entry in nutrients.entries) {
        final key = entry.key;
        final value = entry.value;
        if (key is! String || value is! num) continue;
        final nutrient = NutritionNutrient.fromStorage(key);
        if (nutrient == null) continue;
        nutrientValues[nutrient] = value.toDouble();
      }
    }
    final drink = CustomHydrationDrink(
      id: id,
      name: name,
      volumeMilliliters: volume.toDouble(),
      hydrationMultiplier:
          multiplier is num ? multiplier.toDouble() : kFullHydrationImpactMultiplier,
      nutrientValues: nutrientValues,
    );
    return isValidCustomHydrationDrink(drink) ? drink : null;
  } catch (error) {
    debugPrint('Quick beverage payload is not decodable: $error');
    return null;
  }
}
