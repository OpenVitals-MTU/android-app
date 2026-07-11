import 'package:drift/native.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/impl/hydration_repository_impl.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/local/beverage/beverage_store.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/caffeine_health_drink_catalog.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/manualentry/hydration_entry_notifier.dart';
import 'package:openvitals/health/health_data_source.dart';

/// Exercises the real drift-backed [BeverageStore], not a fake: the seeded
/// catalog only exists because the store seeds it on first read, and the bug
/// this guards was the repository reading preferences instead.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late OpenVitalsDatabase db;
  late PreferencesRepository prefs;
  late HydrationRepository repository;

  setUp(() async {
    db = OpenVitalsDatabase(NativeDatabase.memory());
    addTearDown(db.close);
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    prefs = PreferencesRepository(await SharedPreferences.getInstance());
    repository = HydrationRepositoryImpl(
      HealthDataSource(),
      preferencesRepository: prefs,
      beverageStore: BeverageStore(db.beverageDao, prefs),
    );
  });

  test('the drink catalog is seeded from the CaffeineHealth presets', () async {
    final drinks = await repository.customHydrationDrinks();

    // Two waters plus every catalog item that has a serving size and is not a
    // supplement — the Kotlin `BeverageEntity.preloadedDefaults()` rule.
    final expected = 2 + CaffeineHealthDrinkCatalog.beveragePresets().length;
    expect(drinks, hasLength(expected));
    expect(drinks.length, greaterThan(100));

    final names = [for (final drink in drinks) drink.name];
    expect(names.first, 'Still water');
    expect(names, contains('Gasified water'));
    expect(names, contains('Drip coffee'));
    expect(names, contains('Espresso'));
  });

  test('seeded drinks are marked preloaded and carry their category', () async {
    final drinks = await repository.customHydrationDrinks();
    final coffee = drinks.firstWhere((drink) => drink.name == 'Drip coffee');

    expect(coffee.isPreloaded, isTrue);
    expect(coffee.category, CaffeineSourceCategory.coffee);
    expect(coffee.volumeMilliliters, 240.0);
    // Caffeine comes from the catalog's typical content, so the drink logs a
    // nutrition record alongside its hydration.
    expect(coffee.nutrientValues[NutritionNutrient.caffeine], greaterThan(0));
  });

  test('supplements and servingless items are excluded from the seed',
      () async {
    final drinks = await repository.customHydrationDrinks();
    final ids = {for (final drink in drinks) drink.id};

    for (final item in CaffeineHealthDrinkCatalog.items) {
      final seeded = ids.contains('caffeinehealth-${item.id}');
      final eligible = item.defaultServingMilliliters != null &&
          item.category != CaffeineSourceCategory.supplement;
      expect(seeded, eligible, reason: '${item.id} seeded=$seeded');
    }
  });

  test('a user drink is saved to the store and read back with the seed',
      () async {
    await repository.saveCustomHydrationDrink(
      const CustomHydrationDrink(
        id: 'mine',
        name: 'My smoothie',
        volumeMilliliters: 400,
      ),
    );

    final drinks = await repository.customHydrationDrinks();
    final mine = drinks.firstWhere((drink) => drink.id == 'mine');
    expect(mine.name, 'My smoothie');
    expect(mine.isPreloaded, isFalse);
    // The seed is still there alongside it.
    expect(drinks.any((drink) => drink.name == 'Drip coffee'), isTrue);
  });

  test('deleting and recategorizing round-trip through the store', () async {
    await repository.saveCustomHydrationDrink(
      const CustomHydrationDrink(
        id: 'mine',
        name: 'My smoothie',
        volumeMilliliters: 400,
      ),
    );

    await repository.moveCustomHydrationDrinkToCategory(
      'mine',
      CaffeineSourceCategory.other,
    );
    var drinks = await repository.customHydrationDrinks();
    expect(
      drinks.firstWhere((drink) => drink.id == 'mine').category,
      CaffeineSourceCategory.other,
    );

    await repository.deleteCustomHydrationDrink('mine');
    drinks = await repository.customHydrationDrinks();
    expect(drinks.any((drink) => drink.id == 'mine'), isFalse);
  });

  test('the entry notifier surfaces the seeded catalog', () async {
    final container = ProviderContainer(
      overrides: [
        hydrationRepositoryProvider.overrideWithValue(repository),
        preferencesRepositoryProvider.overrideWithValue(prefs),
      ],
    );
    addTearDown(container.dispose);

    final provider =
        NotifierProvider<HydrationEntryNotifier, HydrationEntryState>(
      HydrationEntryNotifier.new,
    );
    container.read(provider);
    // Let the build's async catalog load finish.
    for (var i = 0; i < 40; i++) {
      if (container.read(provider).customDrinkOptions.isNotEmpty) break;
      await Future<void>.delayed(const Duration(milliseconds: 5));
    }

    final options = container.read(provider).customDrinkOptions;
    expect(options, isNotEmpty, reason: 'the catalog never reached the screen');
    expect(options.any((drink) => drink.name == 'Drip coffee'), isTrue);
  });
}
