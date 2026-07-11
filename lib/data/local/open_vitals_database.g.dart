// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'open_vitals_database.dart';

// ignore_for_file: type=lint
mixin _$BeverageDaoMixin on DatabaseAccessor<OpenVitalsDatabase> {
  $BeveragesTable get beverages => attachedDatabase.beverages;
  BeverageDaoManager get managers => BeverageDaoManager(this);
}

class BeverageDaoManager {
  final _$BeverageDaoMixin _db;
  BeverageDaoManager(this._db);
  $$BeveragesTableTableManager get beverages =>
      $$BeveragesTableTableManager(_db.attachedDatabase, _db.beverages);
}

class $BeveragesTable extends Beverages
    with TableInfo<$BeveragesTable, BeverageEntity> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $BeveragesTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
    'id',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _nameMeta = const VerificationMeta('name');
  @override
  late final GeneratedColumn<String> name = GeneratedColumn<String>(
    'name',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _categoryMeta = const VerificationMeta(
    'category',
  );
  @override
  late final GeneratedColumn<String> category = GeneratedColumn<String>(
    'category',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _volumeMillilitersMeta = const VerificationMeta(
    'volumeMilliliters',
  );
  @override
  late final GeneratedColumn<double> volumeMilliliters =
      GeneratedColumn<double>(
        'volume_milliliters',
        aliasedName,
        false,
        type: DriftSqlType.double,
        requiredDuringInsert: true,
      );
  static const VerificationMeta _hydrationMultiplierMeta =
      const VerificationMeta('hydrationMultiplier');
  @override
  late final GeneratedColumn<double> hydrationMultiplier =
      GeneratedColumn<double>(
        'hydration_multiplier',
        aliasedName,
        false,
        type: DriftSqlType.double,
        requiredDuringInsert: true,
      );
  static const VerificationMeta _isPreloadedMeta = const VerificationMeta(
    'isPreloaded',
  );
  @override
  late final GeneratedColumn<bool> isPreloaded = GeneratedColumn<bool>(
    'is_preloaded',
    aliasedName,
    false,
    type: DriftSqlType.bool,
    requiredDuringInsert: true,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'CHECK ("is_preloaded" IN (0, 1))',
    ),
  );
  static const VerificationMeta _isDeletedMeta = const VerificationMeta(
    'isDeleted',
  );
  @override
  late final GeneratedColumn<bool> isDeleted = GeneratedColumn<bool>(
    'is_deleted',
    aliasedName,
    false,
    type: DriftSqlType.bool,
    requiredDuringInsert: true,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'CHECK ("is_deleted" IN (0, 1))',
    ),
  );
  static const VerificationMeta _sortOrderMeta = const VerificationMeta(
    'sortOrder',
  );
  @override
  late final GeneratedColumn<int> sortOrder = GeneratedColumn<int>(
    'sort_order',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _energyKcalMeta = const VerificationMeta(
    'energyKcal',
  );
  @override
  late final GeneratedColumn<double> energyKcal = GeneratedColumn<double>(
    'energy_kcal',
    aliasedName,
    true,
    type: DriftSqlType.double,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _proteinGramsMeta = const VerificationMeta(
    'proteinGrams',
  );
  @override
  late final GeneratedColumn<double> proteinGrams = GeneratedColumn<double>(
    'protein_grams',
    aliasedName,
    true,
    type: DriftSqlType.double,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _totalCarbohydrateGramsMeta =
      const VerificationMeta('totalCarbohydrateGrams');
  @override
  late final GeneratedColumn<double> totalCarbohydrateGrams =
      GeneratedColumn<double>(
        'total_carbohydrate_grams',
        aliasedName,
        true,
        type: DriftSqlType.double,
        requiredDuringInsert: false,
      );
  static const VerificationMeta _totalFatGramsMeta = const VerificationMeta(
    'totalFatGrams',
  );
  @override
  late final GeneratedColumn<double> totalFatGrams = GeneratedColumn<double>(
    'total_fat_grams',
    aliasedName,
    true,
    type: DriftSqlType.double,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _dietaryFiberGramsMeta = const VerificationMeta(
    'dietaryFiberGrams',
  );
  @override
  late final GeneratedColumn<double> dietaryFiberGrams =
      GeneratedColumn<double>(
        'dietary_fiber_grams',
        aliasedName,
        true,
        type: DriftSqlType.double,
        requiredDuringInsert: false,
      );
  static const VerificationMeta _sugarGramsMeta = const VerificationMeta(
    'sugarGrams',
  );
  @override
  late final GeneratedColumn<double> sugarGrams = GeneratedColumn<double>(
    'sugar_grams',
    aliasedName,
    true,
    type: DriftSqlType.double,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _saturatedFatGramsMeta = const VerificationMeta(
    'saturatedFatGrams',
  );
  @override
  late final GeneratedColumn<double> saturatedFatGrams =
      GeneratedColumn<double>(
        'saturated_fat_grams',
        aliasedName,
        true,
        type: DriftSqlType.double,
        requiredDuringInsert: false,
      );
  static const VerificationMeta _sodiumGramsMeta = const VerificationMeta(
    'sodiumGrams',
  );
  @override
  late final GeneratedColumn<double> sodiumGrams = GeneratedColumn<double>(
    'sodium_grams',
    aliasedName,
    true,
    type: DriftSqlType.double,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _potassiumGramsMeta = const VerificationMeta(
    'potassiumGrams',
  );
  @override
  late final GeneratedColumn<double> potassiumGrams = GeneratedColumn<double>(
    'potassium_grams',
    aliasedName,
    true,
    type: DriftSqlType.double,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _calciumGramsMeta = const VerificationMeta(
    'calciumGrams',
  );
  @override
  late final GeneratedColumn<double> calciumGrams = GeneratedColumn<double>(
    'calcium_grams',
    aliasedName,
    true,
    type: DriftSqlType.double,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _caffeineGramsMeta = const VerificationMeta(
    'caffeineGrams',
  );
  @override
  late final GeneratedColumn<double> caffeineGrams = GeneratedColumn<double>(
    'caffeine_grams',
    aliasedName,
    true,
    type: DriftSqlType.double,
    requiredDuringInsert: false,
  );
  @override
  List<GeneratedColumn> get $columns => [
    id,
    name,
    category,
    volumeMilliliters,
    hydrationMultiplier,
    isPreloaded,
    isDeleted,
    sortOrder,
    energyKcal,
    proteinGrams,
    totalCarbohydrateGrams,
    totalFatGrams,
    dietaryFiberGrams,
    sugarGrams,
    saturatedFatGrams,
    sodiumGrams,
    potassiumGrams,
    calciumGrams,
    caffeineGrams,
  ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'beverages';
  @override
  VerificationContext validateIntegrity(
    Insertable<BeverageEntity> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('name')) {
      context.handle(
        _nameMeta,
        name.isAcceptableOrUnknown(data['name']!, _nameMeta),
      );
    } else if (isInserting) {
      context.missing(_nameMeta);
    }
    if (data.containsKey('category')) {
      context.handle(
        _categoryMeta,
        category.isAcceptableOrUnknown(data['category']!, _categoryMeta),
      );
    }
    if (data.containsKey('volume_milliliters')) {
      context.handle(
        _volumeMillilitersMeta,
        volumeMilliliters.isAcceptableOrUnknown(
          data['volume_milliliters']!,
          _volumeMillilitersMeta,
        ),
      );
    } else if (isInserting) {
      context.missing(_volumeMillilitersMeta);
    }
    if (data.containsKey('hydration_multiplier')) {
      context.handle(
        _hydrationMultiplierMeta,
        hydrationMultiplier.isAcceptableOrUnknown(
          data['hydration_multiplier']!,
          _hydrationMultiplierMeta,
        ),
      );
    } else if (isInserting) {
      context.missing(_hydrationMultiplierMeta);
    }
    if (data.containsKey('is_preloaded')) {
      context.handle(
        _isPreloadedMeta,
        isPreloaded.isAcceptableOrUnknown(
          data['is_preloaded']!,
          _isPreloadedMeta,
        ),
      );
    } else if (isInserting) {
      context.missing(_isPreloadedMeta);
    }
    if (data.containsKey('is_deleted')) {
      context.handle(
        _isDeletedMeta,
        isDeleted.isAcceptableOrUnknown(data['is_deleted']!, _isDeletedMeta),
      );
    } else if (isInserting) {
      context.missing(_isDeletedMeta);
    }
    if (data.containsKey('sort_order')) {
      context.handle(
        _sortOrderMeta,
        sortOrder.isAcceptableOrUnknown(data['sort_order']!, _sortOrderMeta),
      );
    } else if (isInserting) {
      context.missing(_sortOrderMeta);
    }
    if (data.containsKey('energy_kcal')) {
      context.handle(
        _energyKcalMeta,
        energyKcal.isAcceptableOrUnknown(data['energy_kcal']!, _energyKcalMeta),
      );
    }
    if (data.containsKey('protein_grams')) {
      context.handle(
        _proteinGramsMeta,
        proteinGrams.isAcceptableOrUnknown(
          data['protein_grams']!,
          _proteinGramsMeta,
        ),
      );
    }
    if (data.containsKey('total_carbohydrate_grams')) {
      context.handle(
        _totalCarbohydrateGramsMeta,
        totalCarbohydrateGrams.isAcceptableOrUnknown(
          data['total_carbohydrate_grams']!,
          _totalCarbohydrateGramsMeta,
        ),
      );
    }
    if (data.containsKey('total_fat_grams')) {
      context.handle(
        _totalFatGramsMeta,
        totalFatGrams.isAcceptableOrUnknown(
          data['total_fat_grams']!,
          _totalFatGramsMeta,
        ),
      );
    }
    if (data.containsKey('dietary_fiber_grams')) {
      context.handle(
        _dietaryFiberGramsMeta,
        dietaryFiberGrams.isAcceptableOrUnknown(
          data['dietary_fiber_grams']!,
          _dietaryFiberGramsMeta,
        ),
      );
    }
    if (data.containsKey('sugar_grams')) {
      context.handle(
        _sugarGramsMeta,
        sugarGrams.isAcceptableOrUnknown(data['sugar_grams']!, _sugarGramsMeta),
      );
    }
    if (data.containsKey('saturated_fat_grams')) {
      context.handle(
        _saturatedFatGramsMeta,
        saturatedFatGrams.isAcceptableOrUnknown(
          data['saturated_fat_grams']!,
          _saturatedFatGramsMeta,
        ),
      );
    }
    if (data.containsKey('sodium_grams')) {
      context.handle(
        _sodiumGramsMeta,
        sodiumGrams.isAcceptableOrUnknown(
          data['sodium_grams']!,
          _sodiumGramsMeta,
        ),
      );
    }
    if (data.containsKey('potassium_grams')) {
      context.handle(
        _potassiumGramsMeta,
        potassiumGrams.isAcceptableOrUnknown(
          data['potassium_grams']!,
          _potassiumGramsMeta,
        ),
      );
    }
    if (data.containsKey('calcium_grams')) {
      context.handle(
        _calciumGramsMeta,
        calciumGrams.isAcceptableOrUnknown(
          data['calcium_grams']!,
          _calciumGramsMeta,
        ),
      );
    }
    if (data.containsKey('caffeine_grams')) {
      context.handle(
        _caffeineGramsMeta,
        caffeineGrams.isAcceptableOrUnknown(
          data['caffeine_grams']!,
          _caffeineGramsMeta,
        ),
      );
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  BeverageEntity map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return BeverageEntity(
      id: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}id'],
      )!,
      name: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}name'],
      )!,
      category: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}category'],
      ),
      volumeMilliliters: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}volume_milliliters'],
      )!,
      hydrationMultiplier: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}hydration_multiplier'],
      )!,
      isPreloaded: attachedDatabase.typeMapping.read(
        DriftSqlType.bool,
        data['${effectivePrefix}is_preloaded'],
      )!,
      isDeleted: attachedDatabase.typeMapping.read(
        DriftSqlType.bool,
        data['${effectivePrefix}is_deleted'],
      )!,
      sortOrder: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}sort_order'],
      )!,
      energyKcal: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}energy_kcal'],
      ),
      proteinGrams: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}protein_grams'],
      ),
      totalCarbohydrateGrams: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}total_carbohydrate_grams'],
      ),
      totalFatGrams: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}total_fat_grams'],
      ),
      dietaryFiberGrams: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}dietary_fiber_grams'],
      ),
      sugarGrams: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}sugar_grams'],
      ),
      saturatedFatGrams: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}saturated_fat_grams'],
      ),
      sodiumGrams: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}sodium_grams'],
      ),
      potassiumGrams: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}potassium_grams'],
      ),
      calciumGrams: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}calcium_grams'],
      ),
      caffeineGrams: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}caffeine_grams'],
      ),
    );
  }

  @override
  $BeveragesTable createAlias(String alias) {
    return $BeveragesTable(attachedDatabase, alias);
  }
}

class BeveragesCompanion extends UpdateCompanion<BeverageEntity> {
  final Value<String> id;
  final Value<String> name;
  final Value<String?> category;
  final Value<double> volumeMilliliters;
  final Value<double> hydrationMultiplier;
  final Value<bool> isPreloaded;
  final Value<bool> isDeleted;
  final Value<int> sortOrder;
  final Value<double?> energyKcal;
  final Value<double?> proteinGrams;
  final Value<double?> totalCarbohydrateGrams;
  final Value<double?> totalFatGrams;
  final Value<double?> dietaryFiberGrams;
  final Value<double?> sugarGrams;
  final Value<double?> saturatedFatGrams;
  final Value<double?> sodiumGrams;
  final Value<double?> potassiumGrams;
  final Value<double?> calciumGrams;
  final Value<double?> caffeineGrams;
  final Value<int> rowid;
  const BeveragesCompanion({
    this.id = const Value.absent(),
    this.name = const Value.absent(),
    this.category = const Value.absent(),
    this.volumeMilliliters = const Value.absent(),
    this.hydrationMultiplier = const Value.absent(),
    this.isPreloaded = const Value.absent(),
    this.isDeleted = const Value.absent(),
    this.sortOrder = const Value.absent(),
    this.energyKcal = const Value.absent(),
    this.proteinGrams = const Value.absent(),
    this.totalCarbohydrateGrams = const Value.absent(),
    this.totalFatGrams = const Value.absent(),
    this.dietaryFiberGrams = const Value.absent(),
    this.sugarGrams = const Value.absent(),
    this.saturatedFatGrams = const Value.absent(),
    this.sodiumGrams = const Value.absent(),
    this.potassiumGrams = const Value.absent(),
    this.calciumGrams = const Value.absent(),
    this.caffeineGrams = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  BeveragesCompanion.insert({
    required String id,
    required String name,
    this.category = const Value.absent(),
    required double volumeMilliliters,
    required double hydrationMultiplier,
    required bool isPreloaded,
    required bool isDeleted,
    required int sortOrder,
    this.energyKcal = const Value.absent(),
    this.proteinGrams = const Value.absent(),
    this.totalCarbohydrateGrams = const Value.absent(),
    this.totalFatGrams = const Value.absent(),
    this.dietaryFiberGrams = const Value.absent(),
    this.sugarGrams = const Value.absent(),
    this.saturatedFatGrams = const Value.absent(),
    this.sodiumGrams = const Value.absent(),
    this.potassiumGrams = const Value.absent(),
    this.calciumGrams = const Value.absent(),
    this.caffeineGrams = const Value.absent(),
    this.rowid = const Value.absent(),
  }) : id = Value(id),
       name = Value(name),
       volumeMilliliters = Value(volumeMilliliters),
       hydrationMultiplier = Value(hydrationMultiplier),
       isPreloaded = Value(isPreloaded),
       isDeleted = Value(isDeleted),
       sortOrder = Value(sortOrder);
  static Insertable<BeverageEntity> custom({
    Expression<String>? id,
    Expression<String>? name,
    Expression<String>? category,
    Expression<double>? volumeMilliliters,
    Expression<double>? hydrationMultiplier,
    Expression<bool>? isPreloaded,
    Expression<bool>? isDeleted,
    Expression<int>? sortOrder,
    Expression<double>? energyKcal,
    Expression<double>? proteinGrams,
    Expression<double>? totalCarbohydrateGrams,
    Expression<double>? totalFatGrams,
    Expression<double>? dietaryFiberGrams,
    Expression<double>? sugarGrams,
    Expression<double>? saturatedFatGrams,
    Expression<double>? sodiumGrams,
    Expression<double>? potassiumGrams,
    Expression<double>? calciumGrams,
    Expression<double>? caffeineGrams,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (name != null) 'name': name,
      if (category != null) 'category': category,
      if (volumeMilliliters != null) 'volume_milliliters': volumeMilliliters,
      if (hydrationMultiplier != null)
        'hydration_multiplier': hydrationMultiplier,
      if (isPreloaded != null) 'is_preloaded': isPreloaded,
      if (isDeleted != null) 'is_deleted': isDeleted,
      if (sortOrder != null) 'sort_order': sortOrder,
      if (energyKcal != null) 'energy_kcal': energyKcal,
      if (proteinGrams != null) 'protein_grams': proteinGrams,
      if (totalCarbohydrateGrams != null)
        'total_carbohydrate_grams': totalCarbohydrateGrams,
      if (totalFatGrams != null) 'total_fat_grams': totalFatGrams,
      if (dietaryFiberGrams != null) 'dietary_fiber_grams': dietaryFiberGrams,
      if (sugarGrams != null) 'sugar_grams': sugarGrams,
      if (saturatedFatGrams != null) 'saturated_fat_grams': saturatedFatGrams,
      if (sodiumGrams != null) 'sodium_grams': sodiumGrams,
      if (potassiumGrams != null) 'potassium_grams': potassiumGrams,
      if (calciumGrams != null) 'calcium_grams': calciumGrams,
      if (caffeineGrams != null) 'caffeine_grams': caffeineGrams,
      if (rowid != null) 'rowid': rowid,
    });
  }

  BeveragesCompanion copyWith({
    Value<String>? id,
    Value<String>? name,
    Value<String?>? category,
    Value<double>? volumeMilliliters,
    Value<double>? hydrationMultiplier,
    Value<bool>? isPreloaded,
    Value<bool>? isDeleted,
    Value<int>? sortOrder,
    Value<double?>? energyKcal,
    Value<double?>? proteinGrams,
    Value<double?>? totalCarbohydrateGrams,
    Value<double?>? totalFatGrams,
    Value<double?>? dietaryFiberGrams,
    Value<double?>? sugarGrams,
    Value<double?>? saturatedFatGrams,
    Value<double?>? sodiumGrams,
    Value<double?>? potassiumGrams,
    Value<double?>? calciumGrams,
    Value<double?>? caffeineGrams,
    Value<int>? rowid,
  }) {
    return BeveragesCompanion(
      id: id ?? this.id,
      name: name ?? this.name,
      category: category ?? this.category,
      volumeMilliliters: volumeMilliliters ?? this.volumeMilliliters,
      hydrationMultiplier: hydrationMultiplier ?? this.hydrationMultiplier,
      isPreloaded: isPreloaded ?? this.isPreloaded,
      isDeleted: isDeleted ?? this.isDeleted,
      sortOrder: sortOrder ?? this.sortOrder,
      energyKcal: energyKcal ?? this.energyKcal,
      proteinGrams: proteinGrams ?? this.proteinGrams,
      totalCarbohydrateGrams:
          totalCarbohydrateGrams ?? this.totalCarbohydrateGrams,
      totalFatGrams: totalFatGrams ?? this.totalFatGrams,
      dietaryFiberGrams: dietaryFiberGrams ?? this.dietaryFiberGrams,
      sugarGrams: sugarGrams ?? this.sugarGrams,
      saturatedFatGrams: saturatedFatGrams ?? this.saturatedFatGrams,
      sodiumGrams: sodiumGrams ?? this.sodiumGrams,
      potassiumGrams: potassiumGrams ?? this.potassiumGrams,
      calciumGrams: calciumGrams ?? this.calciumGrams,
      caffeineGrams: caffeineGrams ?? this.caffeineGrams,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (name.present) {
      map['name'] = Variable<String>(name.value);
    }
    if (category.present) {
      map['category'] = Variable<String>(category.value);
    }
    if (volumeMilliliters.present) {
      map['volume_milliliters'] = Variable<double>(volumeMilliliters.value);
    }
    if (hydrationMultiplier.present) {
      map['hydration_multiplier'] = Variable<double>(hydrationMultiplier.value);
    }
    if (isPreloaded.present) {
      map['is_preloaded'] = Variable<bool>(isPreloaded.value);
    }
    if (isDeleted.present) {
      map['is_deleted'] = Variable<bool>(isDeleted.value);
    }
    if (sortOrder.present) {
      map['sort_order'] = Variable<int>(sortOrder.value);
    }
    if (energyKcal.present) {
      map['energy_kcal'] = Variable<double>(energyKcal.value);
    }
    if (proteinGrams.present) {
      map['protein_grams'] = Variable<double>(proteinGrams.value);
    }
    if (totalCarbohydrateGrams.present) {
      map['total_carbohydrate_grams'] = Variable<double>(
        totalCarbohydrateGrams.value,
      );
    }
    if (totalFatGrams.present) {
      map['total_fat_grams'] = Variable<double>(totalFatGrams.value);
    }
    if (dietaryFiberGrams.present) {
      map['dietary_fiber_grams'] = Variable<double>(dietaryFiberGrams.value);
    }
    if (sugarGrams.present) {
      map['sugar_grams'] = Variable<double>(sugarGrams.value);
    }
    if (saturatedFatGrams.present) {
      map['saturated_fat_grams'] = Variable<double>(saturatedFatGrams.value);
    }
    if (sodiumGrams.present) {
      map['sodium_grams'] = Variable<double>(sodiumGrams.value);
    }
    if (potassiumGrams.present) {
      map['potassium_grams'] = Variable<double>(potassiumGrams.value);
    }
    if (calciumGrams.present) {
      map['calcium_grams'] = Variable<double>(calciumGrams.value);
    }
    if (caffeineGrams.present) {
      map['caffeine_grams'] = Variable<double>(caffeineGrams.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('BeveragesCompanion(')
          ..write('id: $id, ')
          ..write('name: $name, ')
          ..write('category: $category, ')
          ..write('volumeMilliliters: $volumeMilliliters, ')
          ..write('hydrationMultiplier: $hydrationMultiplier, ')
          ..write('isPreloaded: $isPreloaded, ')
          ..write('isDeleted: $isDeleted, ')
          ..write('sortOrder: $sortOrder, ')
          ..write('energyKcal: $energyKcal, ')
          ..write('proteinGrams: $proteinGrams, ')
          ..write('totalCarbohydrateGrams: $totalCarbohydrateGrams, ')
          ..write('totalFatGrams: $totalFatGrams, ')
          ..write('dietaryFiberGrams: $dietaryFiberGrams, ')
          ..write('sugarGrams: $sugarGrams, ')
          ..write('saturatedFatGrams: $saturatedFatGrams, ')
          ..write('sodiumGrams: $sodiumGrams, ')
          ..write('potassiumGrams: $potassiumGrams, ')
          ..write('calciumGrams: $calciumGrams, ')
          ..write('caffeineGrams: $caffeineGrams, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

abstract class _$OpenVitalsDatabase extends GeneratedDatabase {
  _$OpenVitalsDatabase(QueryExecutor e) : super(e);
  $OpenVitalsDatabaseManager get managers => $OpenVitalsDatabaseManager(this);
  late final $BeveragesTable beverages = $BeveragesTable(this);
  late final BeverageDao beverageDao = BeverageDao(this as OpenVitalsDatabase);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [beverages];
}

typedef $$BeveragesTableCreateCompanionBuilder =
    BeveragesCompanion Function({
      required String id,
      required String name,
      Value<String?> category,
      required double volumeMilliliters,
      required double hydrationMultiplier,
      required bool isPreloaded,
      required bool isDeleted,
      required int sortOrder,
      Value<double?> energyKcal,
      Value<double?> proteinGrams,
      Value<double?> totalCarbohydrateGrams,
      Value<double?> totalFatGrams,
      Value<double?> dietaryFiberGrams,
      Value<double?> sugarGrams,
      Value<double?> saturatedFatGrams,
      Value<double?> sodiumGrams,
      Value<double?> potassiumGrams,
      Value<double?> calciumGrams,
      Value<double?> caffeineGrams,
      Value<int> rowid,
    });
typedef $$BeveragesTableUpdateCompanionBuilder =
    BeveragesCompanion Function({
      Value<String> id,
      Value<String> name,
      Value<String?> category,
      Value<double> volumeMilliliters,
      Value<double> hydrationMultiplier,
      Value<bool> isPreloaded,
      Value<bool> isDeleted,
      Value<int> sortOrder,
      Value<double?> energyKcal,
      Value<double?> proteinGrams,
      Value<double?> totalCarbohydrateGrams,
      Value<double?> totalFatGrams,
      Value<double?> dietaryFiberGrams,
      Value<double?> sugarGrams,
      Value<double?> saturatedFatGrams,
      Value<double?> sodiumGrams,
      Value<double?> potassiumGrams,
      Value<double?> calciumGrams,
      Value<double?> caffeineGrams,
      Value<int> rowid,
    });

class $$BeveragesTableFilterComposer
    extends Composer<_$OpenVitalsDatabase, $BeveragesTable> {
  $$BeveragesTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get name => $composableBuilder(
    column: $table.name,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get category => $composableBuilder(
    column: $table.category,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get volumeMilliliters => $composableBuilder(
    column: $table.volumeMilliliters,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get hydrationMultiplier => $composableBuilder(
    column: $table.hydrationMultiplier,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<bool> get isPreloaded => $composableBuilder(
    column: $table.isPreloaded,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<bool> get isDeleted => $composableBuilder(
    column: $table.isDeleted,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get sortOrder => $composableBuilder(
    column: $table.sortOrder,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get energyKcal => $composableBuilder(
    column: $table.energyKcal,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get proteinGrams => $composableBuilder(
    column: $table.proteinGrams,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get totalCarbohydrateGrams => $composableBuilder(
    column: $table.totalCarbohydrateGrams,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get totalFatGrams => $composableBuilder(
    column: $table.totalFatGrams,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get dietaryFiberGrams => $composableBuilder(
    column: $table.dietaryFiberGrams,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get sugarGrams => $composableBuilder(
    column: $table.sugarGrams,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get saturatedFatGrams => $composableBuilder(
    column: $table.saturatedFatGrams,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get sodiumGrams => $composableBuilder(
    column: $table.sodiumGrams,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get potassiumGrams => $composableBuilder(
    column: $table.potassiumGrams,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get calciumGrams => $composableBuilder(
    column: $table.calciumGrams,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get caffeineGrams => $composableBuilder(
    column: $table.caffeineGrams,
    builder: (column) => ColumnFilters(column),
  );
}

class $$BeveragesTableOrderingComposer
    extends Composer<_$OpenVitalsDatabase, $BeveragesTable> {
  $$BeveragesTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get name => $composableBuilder(
    column: $table.name,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get category => $composableBuilder(
    column: $table.category,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get volumeMilliliters => $composableBuilder(
    column: $table.volumeMilliliters,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get hydrationMultiplier => $composableBuilder(
    column: $table.hydrationMultiplier,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<bool> get isPreloaded => $composableBuilder(
    column: $table.isPreloaded,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<bool> get isDeleted => $composableBuilder(
    column: $table.isDeleted,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get sortOrder => $composableBuilder(
    column: $table.sortOrder,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get energyKcal => $composableBuilder(
    column: $table.energyKcal,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get proteinGrams => $composableBuilder(
    column: $table.proteinGrams,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get totalCarbohydrateGrams => $composableBuilder(
    column: $table.totalCarbohydrateGrams,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get totalFatGrams => $composableBuilder(
    column: $table.totalFatGrams,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get dietaryFiberGrams => $composableBuilder(
    column: $table.dietaryFiberGrams,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get sugarGrams => $composableBuilder(
    column: $table.sugarGrams,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get saturatedFatGrams => $composableBuilder(
    column: $table.saturatedFatGrams,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get sodiumGrams => $composableBuilder(
    column: $table.sodiumGrams,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get potassiumGrams => $composableBuilder(
    column: $table.potassiumGrams,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get calciumGrams => $composableBuilder(
    column: $table.calciumGrams,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get caffeineGrams => $composableBuilder(
    column: $table.caffeineGrams,
    builder: (column) => ColumnOrderings(column),
  );
}

class $$BeveragesTableAnnotationComposer
    extends Composer<_$OpenVitalsDatabase, $BeveragesTable> {
  $$BeveragesTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get name =>
      $composableBuilder(column: $table.name, builder: (column) => column);

  GeneratedColumn<String> get category =>
      $composableBuilder(column: $table.category, builder: (column) => column);

  GeneratedColumn<double> get volumeMilliliters => $composableBuilder(
    column: $table.volumeMilliliters,
    builder: (column) => column,
  );

  GeneratedColumn<double> get hydrationMultiplier => $composableBuilder(
    column: $table.hydrationMultiplier,
    builder: (column) => column,
  );

  GeneratedColumn<bool> get isPreloaded => $composableBuilder(
    column: $table.isPreloaded,
    builder: (column) => column,
  );

  GeneratedColumn<bool> get isDeleted =>
      $composableBuilder(column: $table.isDeleted, builder: (column) => column);

  GeneratedColumn<int> get sortOrder =>
      $composableBuilder(column: $table.sortOrder, builder: (column) => column);

  GeneratedColumn<double> get energyKcal => $composableBuilder(
    column: $table.energyKcal,
    builder: (column) => column,
  );

  GeneratedColumn<double> get proteinGrams => $composableBuilder(
    column: $table.proteinGrams,
    builder: (column) => column,
  );

  GeneratedColumn<double> get totalCarbohydrateGrams => $composableBuilder(
    column: $table.totalCarbohydrateGrams,
    builder: (column) => column,
  );

  GeneratedColumn<double> get totalFatGrams => $composableBuilder(
    column: $table.totalFatGrams,
    builder: (column) => column,
  );

  GeneratedColumn<double> get dietaryFiberGrams => $composableBuilder(
    column: $table.dietaryFiberGrams,
    builder: (column) => column,
  );

  GeneratedColumn<double> get sugarGrams => $composableBuilder(
    column: $table.sugarGrams,
    builder: (column) => column,
  );

  GeneratedColumn<double> get saturatedFatGrams => $composableBuilder(
    column: $table.saturatedFatGrams,
    builder: (column) => column,
  );

  GeneratedColumn<double> get sodiumGrams => $composableBuilder(
    column: $table.sodiumGrams,
    builder: (column) => column,
  );

  GeneratedColumn<double> get potassiumGrams => $composableBuilder(
    column: $table.potassiumGrams,
    builder: (column) => column,
  );

  GeneratedColumn<double> get calciumGrams => $composableBuilder(
    column: $table.calciumGrams,
    builder: (column) => column,
  );

  GeneratedColumn<double> get caffeineGrams => $composableBuilder(
    column: $table.caffeineGrams,
    builder: (column) => column,
  );
}

class $$BeveragesTableTableManager
    extends
        RootTableManager<
          _$OpenVitalsDatabase,
          $BeveragesTable,
          BeverageEntity,
          $$BeveragesTableFilterComposer,
          $$BeveragesTableOrderingComposer,
          $$BeveragesTableAnnotationComposer,
          $$BeveragesTableCreateCompanionBuilder,
          $$BeveragesTableUpdateCompanionBuilder,
          (
            BeverageEntity,
            BaseReferences<
              _$OpenVitalsDatabase,
              $BeveragesTable,
              BeverageEntity
            >,
          ),
          BeverageEntity,
          PrefetchHooks Function()
        > {
  $$BeveragesTableTableManager(_$OpenVitalsDatabase db, $BeveragesTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$BeveragesTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$BeveragesTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$BeveragesTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<String> id = const Value.absent(),
                Value<String> name = const Value.absent(),
                Value<String?> category = const Value.absent(),
                Value<double> volumeMilliliters = const Value.absent(),
                Value<double> hydrationMultiplier = const Value.absent(),
                Value<bool> isPreloaded = const Value.absent(),
                Value<bool> isDeleted = const Value.absent(),
                Value<int> sortOrder = const Value.absent(),
                Value<double?> energyKcal = const Value.absent(),
                Value<double?> proteinGrams = const Value.absent(),
                Value<double?> totalCarbohydrateGrams = const Value.absent(),
                Value<double?> totalFatGrams = const Value.absent(),
                Value<double?> dietaryFiberGrams = const Value.absent(),
                Value<double?> sugarGrams = const Value.absent(),
                Value<double?> saturatedFatGrams = const Value.absent(),
                Value<double?> sodiumGrams = const Value.absent(),
                Value<double?> potassiumGrams = const Value.absent(),
                Value<double?> calciumGrams = const Value.absent(),
                Value<double?> caffeineGrams = const Value.absent(),
                Value<int> rowid = const Value.absent(),
              }) => BeveragesCompanion(
                id: id,
                name: name,
                category: category,
                volumeMilliliters: volumeMilliliters,
                hydrationMultiplier: hydrationMultiplier,
                isPreloaded: isPreloaded,
                isDeleted: isDeleted,
                sortOrder: sortOrder,
                energyKcal: energyKcal,
                proteinGrams: proteinGrams,
                totalCarbohydrateGrams: totalCarbohydrateGrams,
                totalFatGrams: totalFatGrams,
                dietaryFiberGrams: dietaryFiberGrams,
                sugarGrams: sugarGrams,
                saturatedFatGrams: saturatedFatGrams,
                sodiumGrams: sodiumGrams,
                potassiumGrams: potassiumGrams,
                calciumGrams: calciumGrams,
                caffeineGrams: caffeineGrams,
                rowid: rowid,
              ),
          createCompanionCallback:
              ({
                required String id,
                required String name,
                Value<String?> category = const Value.absent(),
                required double volumeMilliliters,
                required double hydrationMultiplier,
                required bool isPreloaded,
                required bool isDeleted,
                required int sortOrder,
                Value<double?> energyKcal = const Value.absent(),
                Value<double?> proteinGrams = const Value.absent(),
                Value<double?> totalCarbohydrateGrams = const Value.absent(),
                Value<double?> totalFatGrams = const Value.absent(),
                Value<double?> dietaryFiberGrams = const Value.absent(),
                Value<double?> sugarGrams = const Value.absent(),
                Value<double?> saturatedFatGrams = const Value.absent(),
                Value<double?> sodiumGrams = const Value.absent(),
                Value<double?> potassiumGrams = const Value.absent(),
                Value<double?> calciumGrams = const Value.absent(),
                Value<double?> caffeineGrams = const Value.absent(),
                Value<int> rowid = const Value.absent(),
              }) => BeveragesCompanion.insert(
                id: id,
                name: name,
                category: category,
                volumeMilliliters: volumeMilliliters,
                hydrationMultiplier: hydrationMultiplier,
                isPreloaded: isPreloaded,
                isDeleted: isDeleted,
                sortOrder: sortOrder,
                energyKcal: energyKcal,
                proteinGrams: proteinGrams,
                totalCarbohydrateGrams: totalCarbohydrateGrams,
                totalFatGrams: totalFatGrams,
                dietaryFiberGrams: dietaryFiberGrams,
                sugarGrams: sugarGrams,
                saturatedFatGrams: saturatedFatGrams,
                sodiumGrams: sodiumGrams,
                potassiumGrams: potassiumGrams,
                calciumGrams: calciumGrams,
                caffeineGrams: caffeineGrams,
                rowid: rowid,
              ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$BeveragesTableProcessedTableManager =
    ProcessedTableManager<
      _$OpenVitalsDatabase,
      $BeveragesTable,
      BeverageEntity,
      $$BeveragesTableFilterComposer,
      $$BeveragesTableOrderingComposer,
      $$BeveragesTableAnnotationComposer,
      $$BeveragesTableCreateCompanionBuilder,
      $$BeveragesTableUpdateCompanionBuilder,
      (
        BeverageEntity,
        BaseReferences<_$OpenVitalsDatabase, $BeveragesTable, BeverageEntity>,
      ),
      BeverageEntity,
      PrefetchHooks Function()
    >;

class $OpenVitalsDatabaseManager {
  final _$OpenVitalsDatabase _db;
  $OpenVitalsDatabaseManager(this._db);
  $$BeveragesTableTableManager get beverages =>
      $$BeveragesTableTableManager(_db, _db.beverages);
}
