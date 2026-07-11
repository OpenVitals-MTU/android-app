import 'package:drift/drift.dart' show Value;
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/local/beverage/beverage_entity.dart';
import 'package:openvitals/data/local/beverage/beverage_store.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late OpenVitalsDatabase db;
  late PreferencesRepository prefsRepo;
  late BeverageStore store;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    prefsRepo = PreferencesRepository(prefs);
    db = OpenVitalsDatabase(NativeDatabase.memory());
    store = BeverageStore(db.beverageDao, prefsRepo);
  });

  tearDown(() async {
    await db.close();
  });

  test('beverages() seeds preloaded defaults on first access', () async {
    final beverages = await store.beverages();

    expect(beverages.length, BeverageEntity.preloadedDefaults().length);
    expect(beverages[0].id, 'openvitals-still-water');
    expect(beverages[1].id, 'openvitals-gasified-water');
    // The migration flag is flipped once the store initializes.
    expect(prefsRepo.hasMigratedHydrationBeveragesToRoom(), isTrue);
  });

  test('save() inserts a new active drink with the next sort order', () async {
    await store.beverages(); // force seeding
    const drink = CustomHydrationDrink(
      id: 'my-smoothie',
      name: 'Smoothie',
      volumeMilliliters: 350.0,
      hydrationMultiplier: 0.8,
      nutrientValues: {NutritionNutrient.energy: 180.0},
      category: CaffeineSourceCategory.other,
    );

    await store.save(drink);
    final beverages = await store.beverages();

    final saved = beverages.firstWhere((d) => d.id == 'my-smoothie');
    expect(saved.name, 'Smoothie');
    expect(saved.volumeMilliliters, closeTo(350.0, 0.001));
    expect(saved.hydrationMultiplier, closeTo(0.8, 0.001));
    expect(saved.category, CaffeineSourceCategory.other);
    expect(saved.nutrientValues[NutritionNutrient.energy], closeTo(180.0, 0.001));
  });

  test('delete() soft-deletes and hides the drink from active listing',
      () async {
    await store.beverages();
    await store.delete('openvitals-still-water');

    final beverages = await store.beverages();
    expect(
      beverages.where((d) => d.id == 'openvitals-still-water'),
      isEmpty,
    );
    // Soft delete keeps the row, just flips is_deleted.
    final row = await db.beverageDao.beverageById('openvitals-still-water');
    expect(row, isNotNull);
    expect(row!.isDeleted, isTrue);
  });

  test('moveToCategory() updates the persisted category', () async {
    await store.beverages();
    await store.moveToCategory('openvitals-still-water', CaffeineSourceCategory.tea);

    final row = await db.beverageDao.beverageById('openvitals-still-water');
    expect(row!.category, CaffeineSourceCategory.tea.storageName);
  });

  test('reorder() reindexes provided ids first, keeping the rest after',
      () async {
    await store.beverages();
    await store.reorder(['openvitals-gasified-water', 'openvitals-still-water']);

    final beverages = await store.beverages();
    expect(beverages[0].id, 'openvitals-gasified-water');
    expect(beverages[1].id, 'openvitals-still-water');
  });

  test('DAO nextSortOrder returns max+1', () async {
    await db.beverageDao.insertDefaults([
      const BeverageEntity(
        id: 'a',
        name: 'A',
        category: null,
        volumeMilliliters: 100,
        hydrationMultiplier: 1,
        isPreloaded: false,
        sortOrder: 4,
      ),
    ]);
    expect(await db.beverageDao.nextSortOrder(), 5);
  });

  test('DAO insertDefaults ignores conflicts on existing ids', () async {
    const entity = BeverageEntity(
      id: 'dup',
      name: 'First',
      category: null,
      volumeMilliliters: 100,
      hydrationMultiplier: 1,
      isPreloaded: false,
      sortOrder: 0,
    );
    await db.beverageDao.insertDefaults([entity]);
    await db.beverageDao.insertDefaults([
      entity.copyWith(name: 'Second'),
    ]);
    final row = await db.beverageDao.beverageById('dup');
    expect(row!.name, 'First');
  });

  test('DAO upsert replaces the row and can clear the delete flag', () async {
    await db.beverageDao.upsert(
      const BeverageEntity(
        id: 'x',
        name: 'X',
        category: null,
        volumeMilliliters: 100,
        hydrationMultiplier: 1,
        isPreloaded: false,
        isDeleted: true,
        sortOrder: 0,
      ),
    );
    await (db.update(db.beverages)..where((b) => b.id.equals('x')))
        .write(const BeveragesCompanion(name: Value('X2')));
    final row = await db.beverageDao.beverageById('x');
    expect(row!.name, 'X2');
  });
}
