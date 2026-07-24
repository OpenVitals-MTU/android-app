import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';

part 'caffeine_models.freezed.dart';

@freezed
abstract class CaffeineEntry with _$CaffeineEntry {
  const factory CaffeineEntry({
    required String id,
    required DateTime startTime,
    required DateTime endTime,
    required double caffeineMg,
    required String? name,
    required String source,
    required int mealType,
    String? clientRecordId,
    @Default(false) bool isOpenVitalsEntry,
  }) = _CaffeineEntry;
}

@freezed
abstract class CaffeinePeriodData with _$CaffeinePeriodData {
  const factory CaffeinePeriodData({
    required List<CaffeineEntry> entries,
  }) = _CaffeinePeriodData;
}

@freezed
abstract class CaffeinePoint with _$CaffeinePoint {
  const factory CaffeinePoint({
    required DateTime time,
    required double valueMg,
  }) = _CaffeinePoint;
}

@freezed
abstract class CaffeineEntryInsight with _$CaffeineEntryInsight {
  const factory CaffeineEntryInsight({
    required CaffeineEntry entry,
    required double currentContributionMg,
    required DateTime peakTime,
    required double peakMg,
    required List<CaffeinePoint> contributionPoints,
    required CaffeineSourceCategory inferredCategory,
    CaffeineCatalogMatch? catalogMatch,
  }) = _CaffeineEntryInsight;
}

@freezed
abstract class CaffeineDailyStat with _$CaffeineDailyStat {
  const factory CaffeineDailyStat({
    required LocalDate date,
    required double totalMg,
    required double bedtimeMg,
    required bool safeForSleep,

    /// Whether this day's bedtime has already passed. For today (and, right
    /// after midnight with an after-midnight bedtime, even yesterday) the
    /// [bedtimeMg]/[safeForSleep] figures are a PROJECTION of a night that
    /// has not happened — such rows must not count as lived nights in
    /// safe-night totals or streaks.
    @Default(true) bool nightCompleted,
  }) = _CaffeineDailyStat;
}

@freezed
abstract class CaffeineDistributionSlice with _$CaffeineDistributionSlice {
  const factory CaffeineDistributionSlice({
    required String label,
    required double valueMg,
  }) = _CaffeineDistributionSlice;
}

@freezed
abstract class CaffeineTimeBucket with _$CaffeineTimeBucket {
  const factory CaffeineTimeBucket({
    required CaffeineTimeOfDayBucket bucket,
    required double valueMg,
  }) = _CaffeineTimeBucket;
}

@freezed
abstract class CaffeineInsights with _$CaffeineInsights {
  const factory CaffeineInsights({
    @Default(0.0) double currentMg,
    @Default(0.0) double todayTotalMg,
    @Default(0.0) double periodTotalMg,
    @Default(0.0) double periodAverageMg,
    @Default(0) int loggedDays,
    CaffeineDailyStat? peakDay,
    @Default(0) int safeNights,
    @Default(0) int totalNights,
    @Default(0) int safeSleepStreak,
    @Default(0.0) double bedtimeMg,
    @Default(0) int sleepThresholdMg,
    @Default(LocalTime(0, 0)) LocalTime bedtime,
    int? timeToThresholdMinutes,
    @Default(<CaffeinePoint>[]) List<CaffeinePoint> curvePoints,
    @Default(<CaffeineDailyStat>[]) List<CaffeineDailyStat> dailyStats,
    @Default(<CaffeineEntryInsight>[]) List<CaffeineEntryInsight> entryInsights,
    @Default(<CaffeineDistributionSlice>[])
    List<CaffeineDistributionSlice> sourceTotals,
    @Default(<CaffeineDistributionSlice>[])
    List<CaffeineDistributionSlice> itemTotals,
    @Default(<CaffeineDistributionSlice>[])
    List<CaffeineDistributionSlice> categoryTotals,
    @Default(<CaffeineTimeBucket>[]) List<CaffeineTimeBucket> timeBuckets,
  }) = _CaffeineInsights;
}

enum CaffeineSourceCategory {
  water('WATER'),
  coffee('COFFEE'),
  tea('TEA'),
  energyDrink('ENERGY_DRINK'),
  soda('SODA'),
  chocolate('CHOCOLATE'),
  supplement('SUPPLEMENT'),
  other('OTHER');

  const CaffeineSourceCategory(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static CaffeineSourceCategory? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

enum CaffeineTimeOfDayBucket {
  morning('MORNING'),
  afternoon('AFTERNOON'),
  evening('EVENING'),
  night('NIGHT');

  const CaffeineTimeOfDayBucket(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static CaffeineTimeOfDayBucket? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

@freezed
abstract class CaffeineCatalogItem with _$CaffeineCatalogItem {
  const factory CaffeineCatalogItem({
    required String id,
    required String name,
    required CaffeineSourceCategory category,
    required double typicalCaffeineMg,
    double? defaultServingMilliliters,
    @Default(<String>[]) List<String> aliases,
  }) = _CaffeineCatalogItem;
}

@freezed
abstract class CaffeineCatalogMatch with _$CaffeineCatalogMatch {
  const factory CaffeineCatalogMatch({
    required CaffeineCatalogItem item,
    required CaffeineCatalogMatchConfidence confidence,
    required String matchedText,
  }) = _CaffeineCatalogMatch;
}

enum CaffeineCatalogMatchConfidence {
  exact('EXACT'),
  alias('ALIAS'),
  contains('CONTAINS');

  const CaffeineCatalogMatchConfidence(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static CaffeineCatalogMatchConfidence? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

extension CaffeineSourceCategoryDisplay on CaffeineSourceCategory {
  String get displayLabel => switch (this) {
        CaffeineSourceCategory.water => 'Water',
        CaffeineSourceCategory.coffee => 'Coffee',
        CaffeineSourceCategory.tea => 'Tea',
        CaffeineSourceCategory.energyDrink => 'Energy drink',
        CaffeineSourceCategory.soda => 'Soda',
        CaffeineSourceCategory.chocolate => 'Chocolate',
        CaffeineSourceCategory.supplement => 'Supplement',
        CaffeineSourceCategory.other => 'Other',
      };
}
