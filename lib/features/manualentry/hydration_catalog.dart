import '../../domain/model/caffeine_models.dart';
import '../../domain/model/nutrition_models.dart';

/// Port of the Kotlin hydration catalog grouping (`HydrationEntryFormContent`):
/// the saved-drink list rendered as a searchable, sectioned catalog with a
/// "frequently consumed" row at the top.
///
/// The Kotlin call site passes `catalogDrinks = emptyList()` — the preset
/// catalog is not populated on either platform — but the preset row keys are
/// kept so the grouping stays 1:1 and a preset source can be dropped in later.

const int kHydrationCatalogSearchLimit = 48;
const String _savedRowPrefix = 'saved:';
const String _presetRowPrefix = 'preset:';

/// The fixed section order (Kotlin `HydrationCatalogSections`).
enum HydrationCatalogSectionKey {
  water,
  coffee,
  energyDrink,
  tea,
  chocolate,
  carbonatedSoftDrink,
  other,
}

/// The category a section writes back when a drink is dropped into it. `other`
/// maps to [CaffeineSourceCategory.other]; `supplement` has no section of its
/// own and collapses into it, exactly as in Kotlin.
CaffeineSourceCategory sectionCategory(HydrationCatalogSectionKey key) =>
    switch (key) {
      HydrationCatalogSectionKey.water => CaffeineSourceCategory.water,
      HydrationCatalogSectionKey.coffee => CaffeineSourceCategory.coffee,
      HydrationCatalogSectionKey.energyDrink => CaffeineSourceCategory.energyDrink,
      HydrationCatalogSectionKey.tea => CaffeineSourceCategory.tea,
      HydrationCatalogSectionKey.chocolate => CaffeineSourceCategory.chocolate,
      HydrationCatalogSectionKey.carbonatedSoftDrink => CaffeineSourceCategory.soda,
      HydrationCatalogSectionKey.other => CaffeineSourceCategory.other,
    };

/// Port of the Kotlin `CaffeineSourceCategory.toHydrationCatalogSectionKey()`.
HydrationCatalogSectionKey catalogSectionKeyFor(CaffeineSourceCategory category) =>
    switch (category) {
      CaffeineSourceCategory.water => HydrationCatalogSectionKey.water,
      CaffeineSourceCategory.coffee => HydrationCatalogSectionKey.coffee,
      CaffeineSourceCategory.energyDrink => HydrationCatalogSectionKey.energyDrink,
      CaffeineSourceCategory.tea => HydrationCatalogSectionKey.tea,
      CaffeineSourceCategory.chocolate => HydrationCatalogSectionKey.chocolate,
      CaffeineSourceCategory.soda => HydrationCatalogSectionKey.carbonatedSoftDrink,
      CaffeineSourceCategory.supplement ||
      CaffeineSourceCategory.other =>
        HydrationCatalogSectionKey.other,
    };

String savedCatalogRowKey(String drinkId) => '$_savedRowPrefix$drinkId';
String presetCatalogRowKey(String drinkId) => '$_presetRowPrefix$drinkId';

String? savedDrinkIdFromRowKey(String rowKey) => rowKey.startsWith(_savedRowPrefix)
    ? rowKey.substring(_savedRowPrefix.length)
    : null;

/// The drink id behind a row key, saved or preset.
String? catalogDrinkIdFromRowKey(String rowKey) =>
    savedDrinkIdFromRowKey(rowKey) ??
    (rowKey.startsWith(_presetRowPrefix)
        ? rowKey.substring(_presetRowPrefix.length)
        : null);

class HydrationCatalogRowItem {
  const HydrationCatalogRowItem({
    required this.rowKey,
    required this.drink,
    required this.isSavedDrink,
  });

  final String rowKey;
  final CustomHydrationDrink drink;
  final bool isSavedDrink;
}

class HydrationCatalogSection {
  const HydrationCatalogSection({required this.key, required this.rows});

  final HydrationCatalogSectionKey key;
  final List<HydrationCatalogRowItem> rows;
}

class HydrationCatalogGroupedDrinks {
  const HydrationCatalogGroupedDrinks({
    required this.frequentRows,
    required this.unassignedSavedRows,
    required this.sections,
  });

  final List<HydrationCatalogRowItem> frequentRows;
  final List<HydrationCatalogRowItem> unassignedSavedRows;
  final List<HydrationCatalogSection> sections;

  bool get isEmpty =>
      frequentRows.isEmpty &&
      unassignedSavedRows.isEmpty &&
      sections.every((section) => section.rows.isEmpty);
}

List<CustomHydrationDrink> _filterByQuery(
  List<CustomHydrationDrink> drinks,
  String normalizedQuery,
) =>
    normalizedQuery.trim().isEmpty
        ? drinks
        : [
            for (final drink in drinks)
              if (drink.name.toLowerCase().contains(normalizedQuery)) drink,
          ];

/// Applies a user-defined [order] of row keys, leaving unknown rows in place at
/// the end. Port of `orderedByCatalogSectionOrder`.
List<HydrationCatalogRowItem> _orderedBy(
  List<HydrationCatalogRowItem> rows,
  List<String> order,
) {
  if (order.isEmpty) return rows;
  final byKey = {for (final row in rows) row.rowKey: row};
  final ordered = [
    for (final key in order)
      if (byKey[key] != null) byKey[key]!,
  ];
  final orderedKeys = {for (final row in ordered) row.rowKey};
  return [
    ...ordered,
    for (final row in rows)
      if (!orderedKeys.contains(row.rowKey)) row,
  ];
}

/// Groups the drinks into frequent rows, uncategorized saved rows and the fixed
/// category sections. Port of `hydrationCatalogGroupedDrinks`.
///
/// [savedDrinkCategories] is the session-local override of a drink's section
/// (a drag into another section takes effect before the repository round-trips),
/// and [unassignedSavedOrder] / [sectionOrders] are the session-local row orders.
HydrationCatalogGroupedDrinks hydrationCatalogGroupedDrinks({
  required List<CustomHydrationDrink> savedDrinks,
  required List<CustomHydrationDrink> frequentDrinks,
  List<CustomHydrationDrink> catalogDrinks = const <CustomHydrationDrink>[],
  Map<String, HydrationCatalogSectionKey> savedDrinkCategories =
      const <String, HydrationCatalogSectionKey>{},
  List<String> unassignedSavedOrder = const <String>[],
  Map<HydrationCatalogSectionKey, List<String>> sectionOrders =
      const <HydrationCatalogSectionKey, List<String>>{},
  String normalizedQuery = '',
}) {
  final rowsBySection = <HydrationCatalogSectionKey, List<HydrationCatalogRowItem>>{
    for (final key in HydrationCatalogSectionKey.values) key: [],
  };
  final unassignedSavedRows = <HydrationCatalogRowItem>[];
  final savedDrinkIds = {for (final drink in savedDrinks) drink.id};
  final catalogDrinkIds = {for (final drink in catalogDrinks) drink.id};

  final frequentRows = <HydrationCatalogRowItem>[];
  for (final drink in _filterByQuery(frequentDrinks, normalizedQuery)) {
    if (savedDrinkIds.contains(drink.id)) {
      frequentRows.add(HydrationCatalogRowItem(
        rowKey: savedCatalogRowKey(drink.id),
        drink: drink,
        isSavedDrink: true,
      ));
    } else if (catalogDrinkIds.contains(drink.id)) {
      frequentRows.add(HydrationCatalogRowItem(
        rowKey: presetCatalogRowKey(drink.id),
        drink: drink,
        isSavedDrink: false,
      ));
    }
  }
  final frequentRowKeys = {for (final row in frequentRows) row.rowKey};

  for (final drink in _filterByQuery(savedDrinks, normalizedQuery)) {
    final rowKey = savedCatalogRowKey(drink.id);
    // A frequent drink is not repeated in its section.
    if (frequentRowKeys.contains(rowKey)) continue;
    final row = HydrationCatalogRowItem(
      rowKey: rowKey,
      drink: drink,
      isSavedDrink: true,
    );
    final sectionKey = savedDrinkCategories[drink.id] ??
        (drink.category == null ? null : catalogSectionKeyFor(drink.category!));
    if (sectionKey == null) {
      unassignedSavedRows.add(row);
    } else {
      rowsBySection[sectionKey]!.add(row);
    }
  }

  // Preset drinks are capped while searching, matching Kotlin's search limit.
  final filteredCatalog = _filterByQuery(catalogDrinks, normalizedQuery);
  final cappedCatalog = normalizedQuery.trim().isEmpty
      ? filteredCatalog
      : filteredCatalog.take(kHydrationCatalogSearchLimit).toList();
  for (final drink in cappedCatalog) {
    final rowKey = presetCatalogRowKey(drink.id);
    if (frequentRowKeys.contains(rowKey)) continue;
    final sectionKey = drink.category == null
        ? HydrationCatalogSectionKey.other
        : catalogSectionKeyFor(drink.category!);
    rowsBySection[sectionKey]!.add(HydrationCatalogRowItem(
      rowKey: rowKey,
      drink: drink,
      isSavedDrink: false,
    ));
  }

  return HydrationCatalogGroupedDrinks(
    frequentRows: frequentRows,
    unassignedSavedRows: _orderedBy(unassignedSavedRows, unassignedSavedOrder),
    sections: [
      for (final key in HydrationCatalogSectionKey.values)
        HydrationCatalogSection(
          key: key,
          rows: _orderedBy(rowsBySection[key]!, sectionOrders[key] ?? const []),
        ),
    ],
  );
}
