import 'dart:math' as math;

import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../model/caffeine_models.dart';
import '../preferences/body_profile.dart';
import '../preferences/caffeine_preferences.dart';
import 'caffeine_health_drink_catalog.dart';

/// Faithful port of `CaffeineInsightCalculator` from the Kotlin app.
///
/// Two-compartment caffeine pharmacokinetics with per-minute integration and a
/// binary-search time-to-threshold.
class CaffeineInsightCalculator {
  CaffeineInsightCalculator._();

  static const int _curvePastHours = 24;
  static const int _curveFutureHours = 18;
  static const int _curveStepMinutes = 30;
  static const int _contributionStepMinutes = 20;
  static const int _forecastLimitHours = 168;
  static const double _milligramsEpsilon = 0.01;

  static CaffeineInsights build({
    required List<CaffeineEntry> entries,
    required DatePeriod period,
    required CaffeinePreferences preferences,
    DateTime? now,
    BodyProfile bodyProfile = const BodyProfile(),
  }) {
    final resolvedNow = now ?? DateTime.now().toUtc();
    final normalizedPreferences = preferences.normalized();
    final periodEntries = entries.where((entry) {
      final date = instantToLocalDate(entry.startTime);
      return !date.isBefore(period.start) && !date.isAfter(period.end);
    }).toList();
    final today = instantToLocalDate(resolvedNow);
    final todayEntries = periodEntries
        .where((entry) => instantToLocalDate(entry.startTime) == today)
        .toList();
    final currentMg = activeCaffeineMg(
      entries: entries,
      at: resolvedNow,
      preferences: normalizedPreferences,
      bodyProfile: bodyProfile,
    );
    final bedtime = _bedtimeInstant(today, normalizedPreferences.bedtime);
    final bedtimeMg = activeCaffeineMg(
      entries: entries,
      at: bedtime,
      preferences: normalizedPreferences,
      bodyProfile: bodyProfile,
    );
    final dailyStats =
        _dailyStats(entries, period, normalizedPreferences, bodyProfile);
    final periodTotal =
        periodEntries.fold<double>(0.0, (sum, entry) => sum + entry.caffeineMg);
    final periodDays = math.max(dailyStats.length, 1);
    final loggedDays = dailyStats.where((stat) => stat.totalMg > 0.0).length;

    final sortedEntries = List<CaffeineEntry>.of(periodEntries);
    _stableSortByDescendingStartTime(sortedEntries);
    final entryInsights = sortedEntries.map((entry) {
      final peak = peakContribution(
        entry: entry,
        preferences: normalizedPreferences,
        bodyProfile: bodyProfile,
      );
      final catalogMatch = CaffeineHealthDrinkCatalog.match(entry);
      return CaffeineEntryInsight(
        entry: entry,
        currentContributionMg: contributionMg(
          entry: entry,
          at: resolvedNow,
          preferences: normalizedPreferences,
          bodyProfile: bodyProfile,
        ),
        peakTime: peak.time,
        peakMg: peak.valueMg,
        contributionPoints: _contributionCurve(
          entry,
          normalizedPreferences,
          bodyProfile,
        ),
        inferredCategory:
            catalogMatch?.item.category ?? inferCategory(entry.name),
        catalogMatch: catalogMatch,
      );
    }).toList();

    return CaffeineInsights(
      currentMg: currentMg,
      todayTotalMg:
          todayEntries.fold<double>(0.0, (sum, entry) => sum + entry.caffeineMg),
      periodTotalMg: periodTotal,
      periodAverageMg: periodTotal / periodDays,
      loggedDays: loggedDays,
      peakDay: _peakDay(dailyStats),
      safeNights: dailyStats.where((stat) => stat.safeForSleep).length,
      totalNights: dailyStats.length,
      safeSleepStreak: _safeSleepStreak(dailyStats, today),
      bedtimeMg: bedtimeMg,
      sleepThresholdMg: normalizedPreferences.sleepThresholdMg,
      bedtime: normalizedPreferences.bedtime,
      timeToThresholdMinutes: _timeUntilBelowThreshold(
        entries: entries,
        from: resolvedNow,
        thresholdMg: normalizedPreferences.sleepThresholdMg.toDouble(),
        preferences: normalizedPreferences,
        bodyProfile: bodyProfile,
      ),
      curvePoints: _caffeineCurve(
        entries,
        resolvedNow,
        normalizedPreferences,
        bodyProfile,
      ),
      dailyStats: dailyStats,
      entryInsights: entryInsights,
      sourceTotals: _distribution(
        periodEntries,
        (entry) => entry.source.trim().isEmpty ? 'Unknown source' : entry.source,
      ),
      itemTotals: _distribution(
        periodEntries,
        (entry) => (entry.name != null && entry.name!.trim().isNotEmpty)
            ? entry.name!
            : 'Caffeine entry',
      ),
      categoryTotals: _distribution(
        periodEntries,
        (entry) => CaffeineHealthDrinkCatalog.categoryFor(entry).displayLabel,
      ),
      timeBuckets: _timeBuckets(periodEntries),
    );
  }

  static double activeCaffeineMg({
    required List<CaffeineEntry> entries,
    required DateTime at,
    required CaffeinePreferences preferences,
    BodyProfile bodyProfile = const BodyProfile(),
  }) =>
      _zeroFloor(
        entries.fold<double>(
          0.0,
          (sum, entry) =>
              sum + contributionMg(
                entry: entry,
                at: at,
                preferences: preferences,
                bodyProfile: bodyProfile,
              ),
        ),
      );

  static double contributionMg({
    required CaffeineEntry entry,
    required DateTime at,
    required CaffeinePreferences preferences,
    BodyProfile bodyProfile = const BodyProfile(),
  }) {
    if (entry.caffeineMg <= 0.0 || at.isBefore(entry.startTime)) return 0.0;
    final durationMinutes = _modelingDurationMinutes(entry);
    final dosePerMinute = entry.caffeineMg / durationMinutes;
    var total = 0.0;
    for (var minute = 0; minute < durationMinutes; minute++) {
      final doseTime = entry.startTime.add(Duration(minutes: minute));
      if (at.isBefore(doseTime)) continue;
      final elapsedMinutes =
          math.max(0, at.difference(doseTime).inMinutes);
      total += dosePerMinute *
          _absorbedRemainingFraction(
            elapsedMinutes.toDouble(),
            preferences,
            bodyProfile,
          );
    }
    return _zeroFloor(total);
  }

  static CaffeinePoint peakContribution({
    required CaffeineEntry entry,
    required CaffeinePreferences preferences,
    BodyProfile bodyProfile = const BodyProfile(),
  }) {
    var best = CaffeinePoint(time: entry.startTime, valueMg: 0.0);
    final scanUntilMinutes =
        (preferences.effectiveHalfLifeMinutes(bodyProfile) * 4)
            .clamp(12 * 60, _forecastLimitHours * 60);
    var minute = 0;
    while (minute <= scanUntilMinutes) {
      final time = entry.startTime.add(Duration(minutes: minute));
      final value = contributionMg(
        entry: entry,
        at: time,
        preferences: preferences,
        bodyProfile: bodyProfile,
      );
      if (value > best.valueMg) best = CaffeinePoint(time: time, valueMg: value);
      minute += 5;
    }
    return best;
  }

  static List<CaffeinePoint> _contributionCurve(
    CaffeineEntry entry,
    CaffeinePreferences preferences,
    BodyProfile bodyProfile,
  ) {
    final endMinutes = (preferences.effectiveHalfLifeMinutes(bodyProfile) * 5)
        .clamp(12 * 60, _forecastLimitHours * 60);
    final points = <CaffeinePoint>[];
    for (var minute = 0; minute <= endMinutes; minute += _contributionStepMinutes) {
      final time = entry.startTime.add(Duration(minutes: minute));
      points.add(
        CaffeinePoint(
          time: time,
          valueMg: contributionMg(
            entry: entry,
            at: time,
            preferences: preferences,
            bodyProfile: bodyProfile,
          ),
        ),
      );
    }
    return points;
  }

  static List<CaffeinePoint> _caffeineCurve(
    List<CaffeineEntry> entries,
    DateTime now,
    CaffeinePreferences preferences,
    BodyProfile bodyProfile,
  ) {
    final start = now.subtract(const Duration(hours: _curvePastHours));
    final end = now.add(const Duration(hours: _curveFutureHours));
    final points = <CaffeinePoint>[];
    var time = start;
    while (!time.isAfter(end)) {
      points.add(
        CaffeinePoint(
          time: time,
          valueMg: activeCaffeineMg(
            entries: entries,
            at: time,
            preferences: preferences,
            bodyProfile: bodyProfile,
          ),
        ),
      );
      time = time.add(const Duration(minutes: _curveStepMinutes));
    }
    return points;
  }

  static List<CaffeineDailyStat> _dailyStats(
    List<CaffeineEntry> entries,
    DatePeriod period,
    CaffeinePreferences preferences,
    BodyProfile bodyProfile,
  ) {
    final stats = <CaffeineDailyStat>[];
    var date = period.start;
    while (!date.isAfter(period.end)) {
      final currentDate = date;
      final total = entries.fold<double>(
        0.0,
        (sum, entry) => instantToLocalDate(entry.startTime) == currentDate
            ? sum + entry.caffeineMg
            : sum,
      );
      final bedtime = _bedtimeInstant(date, preferences.bedtime);
      final bedtimeMg = activeCaffeineMg(
        entries: entries,
        at: bedtime,
        preferences: preferences,
        bodyProfile: bodyProfile,
      );
      stats.add(
        CaffeineDailyStat(
          date: date,
          totalMg: total,
          bedtimeMg: bedtimeMg,
          safeForSleep: bedtimeMg <= preferences.sleepThresholdMg,
        ),
      );
      date = date.plusDays(1);
    }
    return stats;
  }

  static int? _timeUntilBelowThreshold({
    required List<CaffeineEntry> entries,
    required DateTime from,
    required double thresholdMg,
    required CaffeinePreferences preferences,
    required BodyProfile bodyProfile,
  }) {
    double activeAt(DateTime at) => activeCaffeineMg(
          entries: entries,
          at: at,
          preferences: preferences,
          bodyProfile: bodyProfile,
        );
    if (activeAt(from) <= thresholdMg) return 0;
    final limit = from.add(const Duration(hours: _forecastLimitHours));
    var low = from;
    var high = limit;
    if (activeAt(high) > thresholdMg) return null;
    for (var i = 0; i < 32; i++) {
      final mid = low.add(
        Duration(milliseconds: high.difference(low).inMilliseconds ~/ 2),
      );
      if (activeAt(mid) > thresholdMg) {
        low = mid;
      } else {
        high = mid;
      }
    }
    return math.max(0, high.difference(from).inMinutes);
  }

  static int _safeSleepStreak(
    List<CaffeineDailyStat> stats,
    LocalDate today,
  ) {
    var streak = 0;
    final ordered = stats.where((stat) => !stat.date.isAfter(today)).toList()
      ..sort((a, b) => b.date.compareTo(a.date));
    for (final stat in ordered) {
      if (!stat.safeForSleep) return streak;
      streak += 1;
    }
    return streak;
  }

  static List<CaffeineDistributionSlice> _distribution(
    List<CaffeineEntry> entries,
    String Function(CaffeineEntry) labelFor,
  ) {
    final byLabel = <String, List<CaffeineEntry>>{};
    for (final entry in entries) {
      (byLabel[labelFor(entry)] ??= <CaffeineEntry>[]).add(entry);
    }
    final slices = byLabel.entries
        .map(
          (group) => CaffeineDistributionSlice(
            label: group.key,
            valueMg: group.value
                .fold<double>(0.0, (sum, entry) => sum + entry.caffeineMg),
          ),
        )
        .where((slice) => slice.valueMg > 0.0)
        .toList();
    _stableSortSlicesByDescendingValue(slices);
    return slices;
  }

  static List<CaffeineTimeBucket> _timeBuckets(List<CaffeineEntry> entries) =>
      CaffeineTimeOfDayBucket.values
          .map(
            (bucket) => CaffeineTimeBucket(
              bucket: bucket,
              valueMg: entries
                  .where(
                    (entry) =>
                        _bucketFor(instantToLocalTime(entry.startTime)) ==
                        bucket,
                  )
                  .fold<double>(0.0, (sum, entry) => sum + entry.caffeineMg),
            ),
          )
          .toList();

  static double _absorbedRemainingFraction(
    double elapsedMinutes,
    CaffeinePreferences preferences,
    BodyProfile bodyProfile,
  ) {
    if (elapsedMinutes < 0.0) return 0.0;
    final ka = math.log(10.0) / math.max(preferences.absorptionMinutes, 1);
    final ke = math.log(2.0) /
        math.max(preferences.effectiveHalfLifeMinutes(bodyProfile), 1);
    final double fraction;
    if ((ka - ke).abs() < 0.000001) {
      fraction = ka * elapsedMinutes * math.exp(-ke * elapsedMinutes);
    } else {
      fraction = (ka / (ka - ke)) *
          (math.exp(-ke * elapsedMinutes) - math.exp(-ka * elapsedMinutes));
    }
    return math.max(fraction, 0.0);
  }

  static DateTime _bedtimeInstant(LocalDate date, LocalTime bedtime) =>
      date.atTimeInstant(bedtime.hour, bedtime.minute, bedtime.second);

  static int _modelingDurationMinutes(CaffeineEntry entry) {
    final minutes = entry.endTime.difference(entry.startTime).inMinutes;
    if (minutes >= 1) {
      return math.min(minutes, 24 * 60);
    }
    return CaffeinePreferences.defaultConsumptionDurationMinutes;
  }

  static CaffeineSourceCategory inferCategory(String? name) =>
      CaffeineHealthDrinkCatalog.matchName(name)?.item.category ??
      CaffeineHealthDrinkCatalog.inferGenericCategory(name);

  static CaffeineTimeOfDayBucket _bucketFor(LocalTime time) {
    final hour = time.hour;
    if (hour >= 5 && hour <= 11) return CaffeineTimeOfDayBucket.morning;
    if (hour >= 12 && hour <= 14) return CaffeineTimeOfDayBucket.afternoon;
    if (hour >= 15 && hour <= 18) return CaffeineTimeOfDayBucket.evening;
    return CaffeineTimeOfDayBucket.night;
  }

  static double _zeroFloor(double value) =>
      value < _milligramsEpsilon ? 0.0 : value;

  static CaffeineDailyStat? _peakDay(List<CaffeineDailyStat> stats) {
    CaffeineDailyStat? best;
    for (final stat in stats) {
      if (best == null || stat.totalMg > best.totalMg) best = stat;
    }
    if (best != null && best.totalMg <= 0.0) return null;
    return best;
  }

  static void _stableSortByDescendingStartTime(List<CaffeineEntry> list) {
    final indexed = <MapEntry<int, CaffeineEntry>>[
      for (var i = 0; i < list.length; i++) MapEntry(i, list[i]),
    ];
    indexed.sort((a, b) {
      final byTime = b.value.startTime.compareTo(a.value.startTime);
      if (byTime != 0) return byTime;
      return a.key.compareTo(b.key);
    });
    for (var i = 0; i < list.length; i++) {
      list[i] = indexed[i].value;
    }
  }

  static void _stableSortSlicesByDescendingValue(
    List<CaffeineDistributionSlice> list,
  ) {
    final indexed = <MapEntry<int, CaffeineDistributionSlice>>[
      for (var i = 0; i < list.length; i++) MapEntry(i, list[i]),
    ];
    indexed.sort((a, b) {
      final byValue = b.value.valueMg.compareTo(a.value.valueMg);
      if (byValue != 0) return byValue;
      return a.key.compareTo(b.key);
    });
    for (var i = 0; i < list.length; i++) {
      list[i] = indexed[i].value;
    }
  }
}
