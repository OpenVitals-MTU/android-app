import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/manualentry/presentation/hydration_catalog.dart';

CustomHydrationDrink _drink(
  String id, {
  String? name,
  CaffeineSourceCategory? category,
}) =>
    CustomHydrationDrink(
      id: id,
      name: name ?? id,
      volumeMilliliters: 250,
      category: category,
    );

List<String> _rowIds(List<HydrationCatalogRowItem> rows) =>
    [for (final row in rows) row.drink.id];

List<HydrationCatalogRowItem> _section(
  HydrationCatalogGroupedDrinks grouped,
  HydrationCatalogSectionKey key,
) =>
    grouped.sections.firstWhere((section) => section.key == key).rows;

void main() {
  test('uncategorized saved drinks land in the unassigned group', () {
    final grouped = hydrationCatalogGroupedDrinks(
      savedDrinks: [_drink('a'), _drink('b')],
      frequentDrinks: const [],
    );

    expect(_rowIds(grouped.unassignedSavedRows), ['a', 'b']);
    expect(grouped.isEmpty, isFalse);
  });

  test('a drink is filed under its category section', () {
    final grouped = hydrationCatalogGroupedDrinks(
      savedDrinks: [
        _drink('c', category: CaffeineSourceCategory.coffee),
        _drink('s', category: CaffeineSourceCategory.soda),
      ],
      frequentDrinks: const [],
    );

    expect(
      _rowIds(_section(grouped, HydrationCatalogSectionKey.coffee)),
      ['c'],
    );
    expect(
      _rowIds(_section(grouped, HydrationCatalogSectionKey.carbonatedSoftDrink)),
      ['s'],
    );
    expect(grouped.unassignedSavedRows, isEmpty);
  });

  test('supplement collapses into the other section, as in Kotlin', () {
    final grouped = hydrationCatalogGroupedDrinks(
      savedDrinks: [_drink('x', category: CaffeineSourceCategory.supplement)],
      frequentDrinks: const [],
    );

    expect(_rowIds(_section(grouped, HydrationCatalogSectionKey.other)), ['x']);
    // …but dropping a drink into "other" writes back the `other` category.
    expect(
      sectionCategory(HydrationCatalogSectionKey.other),
      CaffeineSourceCategory.other,
    );
  });

  test('a session category override beats the drink’s persisted category', () {
    final grouped = hydrationCatalogGroupedDrinks(
      savedDrinks: [_drink('c', category: CaffeineSourceCategory.coffee)],
      frequentDrinks: const [],
      savedDrinkCategories: {'c': HydrationCatalogSectionKey.tea},
    );

    expect(_rowIds(_section(grouped, HydrationCatalogSectionKey.coffee)), isEmpty);
    expect(_rowIds(_section(grouped, HydrationCatalogSectionKey.tea)), ['c']);
  });

  test('a frequent drink is not repeated in its section', () {
    final coffee = _drink('c', category: CaffeineSourceCategory.coffee);
    final grouped = hydrationCatalogGroupedDrinks(
      savedDrinks: [coffee, _drink('t', category: CaffeineSourceCategory.tea)],
      frequentDrinks: [coffee],
    );

    expect(_rowIds(grouped.frequentRows), ['c']);
    expect(_rowIds(_section(grouped, HydrationCatalogSectionKey.coffee)), isEmpty);
    expect(_rowIds(_section(grouped, HydrationCatalogSectionKey.tea)), ['t']);
  });

  test('a frequent drink that is no longer saved is dropped', () {
    final grouped = hydrationCatalogGroupedDrinks(
      savedDrinks: [_drink('a')],
      frequentDrinks: [_drink('gone')],
    );
    expect(grouped.frequentRows, isEmpty);
  });

  test('the search query filters by name, case-insensitively', () {
    final grouped = hydrationCatalogGroupedDrinks(
      savedDrinks: [_drink('a', name: 'Cold Brew'), _drink('b', name: 'Tea')],
      frequentDrinks: const [],
      normalizedQuery: 'brew',
    );

    expect(_rowIds(grouped.unassignedSavedRows), ['a']);
  });

  test('a query matching nothing leaves the grouping empty', () {
    final grouped = hydrationCatalogGroupedDrinks(
      savedDrinks: [_drink('a', name: 'Cola')],
      frequentDrinks: const [],
      normalizedQuery: 'zzz',
    );
    expect(grouped.isEmpty, isTrue);
  });

  test('a session row order reorders a section, unknown keys keep their place',
      () {
    final grouped = hydrationCatalogGroupedDrinks(
      savedDrinks: [_drink('a'), _drink('b'), _drink('c')],
      frequentDrinks: const [],
      unassignedSavedOrder: [savedCatalogRowKey('c'), savedCatalogRowKey('a')],
    );

    // Ordered rows first, then whatever the order did not mention.
    expect(_rowIds(grouped.unassignedSavedRows), ['c', 'a', 'b']);
  });

  group('row keys', () {
    test('round-trip a saved drink id', () {
      final key = savedCatalogRowKey('abc');
      expect(savedDrinkIdFromRowKey(key), 'abc');
      expect(catalogDrinkIdFromRowKey(key), 'abc');
    });

    test('a preset key is not a saved key but still yields its id', () {
      final key = presetCatalogRowKey('abc');
      expect(savedDrinkIdFromRowKey(key), isNull);
      expect(catalogDrinkIdFromRowKey(key), 'abc');
    });

    test('an unprefixed key yields nothing', () {
      expect(catalogDrinkIdFromRowKey('abc'), isNull);
    });
  });

  test('every category maps to a section and back', () {
    for (final category in CaffeineSourceCategory.values) {
      final key = catalogSectionKeyFor(category);
      // supplement is the one lossy case, collapsing into `other`.
      if (category == CaffeineSourceCategory.supplement) {
        expect(sectionCategory(key), CaffeineSourceCategory.other);
      } else {
        expect(sectionCategory(key), category);
      }
    }
  });
}
