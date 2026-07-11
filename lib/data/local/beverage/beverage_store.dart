import '../../../domain/model/caffeine_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../prefs/preferences_repository.dart';
import '../open_vitals_database.dart';
import 'beverage_entity.dart';

/// Facade over [BeverageDao] mirroring the Kotlin `BeverageStore`.
///
/// The Kotlin version is synchronous via `runBlocking`; the Dart port keeps the
/// same behaviour but exposes `Future`s (drift is async). Initialization seeds
/// [BeverageEntity.preloadedDefaults] and performs the one-time migration of
/// legacy SharedPreferences custom drinks into the database.
class BeverageStore {
  BeverageStore(this._dao, this._preferencesRepository);

  final BeverageDao _dao;
  final PreferencesRepository _preferencesRepository;

  Future<void>? _initialization;

  Future<List<CustomHydrationDrink>> beverages() async {
    await _ensureInitialized();
    final entities = await _dao.activeBeverages();
    return entities.map((entity) => entity.toDomain()).toList();
  }

  Future<void> save(CustomHydrationDrink drink) async {
    await _ensureInitialized();
    final existing = await _dao.beverageById(drink.id);
    final entity = BeverageEntity.fromDomain(
      drink: drink,
      sortOrder: existing?.sortOrder ?? await _dao.nextSortOrder(),
      isPreloaded: existing?.isPreloaded ?? drink.isPreloaded,
      category: drink.category ?? existing?.toDomain().category,
    );
    await _dao.upsert(entity.copyWith(isDeleted: false));
  }

  Future<void> delete(String drinkId) async {
    await _ensureInitialized();
    await _dao.softDelete(drinkId);
  }

  Future<void> moveToCategory(
    String drinkId,
    CaffeineSourceCategory? category,
  ) async {
    await _ensureInitialized();
    await _dao.updateCategory(drinkId, category?.storageName);
  }

  Future<void> reorder(List<String> drinkIds) async {
    await _ensureInitialized();
    final current = await _dao.activeBeverages();
    final currentIds = current.map((entity) => entity.id).toSet();
    final orderedIds = <String>[];
    for (final id in drinkIds) {
      if (currentIds.contains(id) && !orderedIds.contains(id)) {
        orderedIds.add(id);
      }
    }
    final orderedIdSet = orderedIds.toSet();
    final remaining = current
        .map((entity) => entity.id)
        .where((id) => !orderedIdSet.contains(id));
    await _dao.updateSortOrderForIds([...orderedIds, ...remaining]);
  }

  Future<void> _ensureInitialized() => _initialization ??= _initialize();

  Future<void> _initialize() async {
    await _dao.insertDefaults(BeverageEntity.preloadedDefaults());
    if (!_preferencesRepository.hasMigratedHydrationBeveragesToRoom()) {
      final nextSortOrder = await _dao.nextSortOrder();
      final legacyDrinks = _preferencesRepository.customHydrationDrinks();
      for (var index = 0; index < legacyDrinks.length; index++) {
        final drink = legacyDrinks[index];
        await _dao.upsert(
          BeverageEntity.fromDomain(
            drink: drink,
            sortOrder: nextSortOrder + index,
            isPreloaded: false,
            category: drink.category,
          ),
        );
      }
      _preferencesRepository.setMigratedHydrationBeveragesToRoom();
    }
  }
}
