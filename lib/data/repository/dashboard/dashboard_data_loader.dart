import 'dart:math' as math;

import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../prefs/preferences_repository.dart';
import '../../../domain/dashboard/dashboard_aggregator.dart';
import '../../../domain/insights/cardio_load.dart';
import '../../../domain/insights/intensity_minutes.dart';
import '../../../domain/insights/sleep_score.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/dashboard_data.dart';
import '../../../domain/model/dashboard_query.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/sleep_daily_summary.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../domain/preferences/activity_week_mode.dart';
import '../../../domain/preferences/sleep_range_mode.dart';
import '../../source/health/health_data_source.dart';
import '../../source/health/health_permissions.dart';
import '../contract/body_energy_repository.dart';
import '../impl/health_connect_gating.dart';
import '../impl/repository_time.dart';

/// Port of the Kotlin `DashboardDataLoader`.
///
/// Read-orchestrator that assembles a [DashboardData] for the visible metrics of
/// a [DashboardQuery]. Each metric read is permission-gated and individually
/// error-guarded (the Kotlin `dashboardMetric { ... }` wrapper). The Kotlin
/// coalescer / performance-trace wrappers are dropped — only the read/assemble
/// semantics are ported. Derived metrics reuse the pure `DashboardAggregator`
/// and insight calculators.
class DashboardDataLoader {
  DashboardDataLoader(
    this._hc, {
    PreferencesRepository? preferencesRepository,
    BodyEnergyRepository? bodyEnergyRepository,
  })  : _preferences = preferencesRepository,
        _bodyEnergy = bodyEnergyRepository;

  final HealthDataSource _hc;
  final PreferencesRepository? _preferences;
  final BodyEnergyRepository? _bodyEnergy;

  static const int _cardioLoadHistoryPeriods = 4;
  static const int _weeklyCardioHeartRateSampleWeeks = 2;

  Future<DashboardData> loadDashboard(DashboardQuery query) async {
    final granted = await _hc.grantedIfAvailable();
    final showEstimatedCalories =
        _preferences?.showOpenVitalsCalculatedCaloriesListenable.value ?? false;
    return _loadDashboardUncached(
      query: query,
      metrics: query.visibleMetrics,
      granted: granted,
      showOpenVitalsCalculatedCalories: showEstimatedCalories,
    );
  }

  Future<T?> _metric<T>(
    bool enabled,
    String permission,
    Set<String> granted,
    Future<T> Function() block,
  ) async {
    if (!enabled || !granted.contains(permission)) return null;
    try {
      return await block();
    } catch (_) {
      return null;
    }
  }

  Future<DashboardData> _loadDashboardUncached({
    required DashboardQuery query,
    required Set<DashboardMetric> metrics,
    required Set<String> granted,
    required bool showOpenVitalsCalculatedCalories,
  }) async {
    final date = query.date;
    final sleepRangeMode = query.sleepRangeMode;
    final activityWeekMode = query.activityWeekMode;

    bool wants(DashboardMetric metric) => metrics.contains(metric);
    bool wantsAny(List<DashboardMetric> targets) =>
        targets.any(metrics.contains);

    final dayStart = localDayStart(date);
    final dayEnd = localDayEnd(date);
    final now = DateTime.now();
    final effectiveDayEnd =
        date == LocalDate.now() && dayEnd.isAfter(now) ? now : dayEnd;

    final steps = await _metric(
        wants(DashboardMetric.steps), HcPermissions.readSteps, granted,
        () => _hc.readSteps(date));
    final distance = await _metric(
        wants(DashboardMetric.distance), HcPermissions.readDistance, granted,
        () => _hc.readDistanceMeters(date));
    final workouts = await _metric(
            wants(DashboardMetric.workout), HcPermissions.readExercise, granted,
            () => _hc.readExerciseSessions(dayStart, dayEnd)) ??
        const <ExerciseData>[];
    final dashboardSleep = await _metric(
        wants(DashboardMetric.sleep), HcPermissions.readSleep, granted,
        () => _readDashboardSleep(date, sleepRangeMode));
    final calories = await _metric(wants(DashboardMetric.caloriesOut),
        HcPermissions.readTotalCalories, granted,
        () => _hc.readCaloriesBurned(
              date,
              includeEstimatedCalories:
                  _canEstimateTotalCalories(granted, showOpenVitalsCalculatedCalories),
            ));
    final activeCalories = (wants(DashboardMetric.activeCalories) &&
            granted.contains(HcPermissions.readActiveCalories) &&
            granted.contains(HcPermissions.readSteps) &&
            granted.contains(HcPermissions.readDistance))
        ? await _guard(() async =>
            (await _hc.readDailySteps(date, date, includeActiveCalories: true))
                .firstOrNull
                ?.activeCaloriesKcal)
        : null;
    final caloriesIn = await _metric(wants(DashboardMetric.caloriesIn),
        HcPermissions.readNutrition, granted, () => _hc.readCaloriesInKcal(date));
    final macros = await _metric(
        wantsAny([
          DashboardMetric.protein,
          DashboardMetric.carbs,
          DashboardMetric.fat,
          DashboardMetric.caffeine,
        ]),
        HcPermissions.readNutrition,
        granted,
        () async => (await _hc.readDailyMacros(date, date)).firstOrNull);
    final hydration = await _metric(wants(DashboardMetric.hydration),
        HcPermissions.readHydration, granted, () => _hc.readHydrationLiters(date));
    final weight = await _metric(
        wantsAny([DashboardMetric.weight, DashboardMetric.bmi, DashboardMetric.ffmi]),
        HcPermissions.readWeight,
        granted,
        () => _hc.readLatestWeight());
    final height = await _metric(
        wantsAny([DashboardMetric.height, DashboardMetric.bmi, DashboardMetric.ffmi]),
        HcPermissions.readHeight,
        granted,
        () => _hc.readLatestHeightEntry());
    final bodyFat = await _metric(
        wantsAny([DashboardMetric.bodyFat, DashboardMetric.ffmi]),
        HcPermissions.readBodyFat,
        granted,
        () => _hc.readLatestBodyFat());
    final leanMass = await _metric(wants(DashboardMetric.leanMass),
        HcPermissions.readLeanMass, granted, () => _hc.readLatestLeanBodyMass());
    final bmr = await _metric(wants(DashboardMetric.bmr), HcPermissions.readBmr,
        granted, () => _hc.readLatestBMR());
    final boneMass = await _metric(wants(DashboardMetric.boneMass),
        HcPermissions.readBoneMass, granted, () => _hc.readLatestBoneMass());
    final bodyWaterMass = await _metric(wants(DashboardMetric.bodyWaterMass),
        HcPermissions.readBodyWaterMass, granted, () => _hc.readLatestBodyWaterMass());
    final heartRate = await _metric(wants(DashboardMetric.avgHeartRate),
        HcPermissions.readHeartRate, granted, () => _hc.readAvgHeartRate(date));
    final restingHR = await _metric(wants(DashboardMetric.restingHeartRate),
        HcPermissions.readRestingHeartRate, granted, () => _hc.readRestingHeartRate(date));
    final restingHRBaseline = await _metric(
        query.includeHistoricalBaselines && wants(DashboardMetric.restingHeartRate),
        HcPermissions.readRestingHeartRate,
        granted,
        () async => DashboardAggregator.medianLongOrNull(
              (await _hc.readDailyRestingHR(date.minusDays(28), date.minusDays(1)))
                  .map((e) => e.bpm)
                  .where((v) => v > 0)
                  .toList(),
            ));
    final hrvSamples = await _metric(wants(DashboardMetric.hrv),
            HcPermissions.readHrv, granted,
            () => _hc.readHrvSamples(dayStart, effectiveDayEnd)) ??
        const <HrvSample>[];
    final hrvBaseline = await _metric(
        query.includeHistoricalBaselines && wants(DashboardMetric.hrv),
        HcPermissions.readHrv,
        granted,
        () async => DashboardAggregator.medianDoubleValuesOrNull(
              (await _hc.readDailyHRV(date.minusDays(28), date.minusDays(1)))
                  .map((e) => e.rmssdMs)
                  .where((v) => v > 0)
                  .toList(),
            ));
    final bloodPressure = await _metric(wants(DashboardMetric.bloodPressure),
        HcPermissions.readBloodPressure, granted, () => _hc.readLatestBloodPressure(date));
    final spO2 = await _metric(wants(DashboardMetric.spo2),
        HcPermissions.readSpO2, granted, () => _hc.readLatestSpO2(date));
    final vo2Max = await _metric(wants(DashboardMetric.vo2Max),
        HcPermissions.readVo2Max, granted, () => _hc.readLatestVo2Max(date));
    final respiratoryRate = await _metric(wants(DashboardMetric.respiratoryRate),
        HcPermissions.readRespiratoryRate, granted, () async {
      final entries = await _hc.readRespiratoryRateEntries(dayStart, dayEnd);
      if (entries.isEmpty) return null;
      return entries.map((e) => e.breathsPerMinute).reduce((a, b) => a + b) /
          entries.length;
    });
    final bodyTemperature = await _metric(wants(DashboardMetric.bodyTemperature),
        HcPermissions.readBodyTemperature, granted, () async {
      final entries = await _hc.readBodyTemperatureEntries(dayStart, dayEnd);
      return _latestByTime(entries, (e) => e.time)?.temperatureCelsius;
    });
    final bloodGlucose = await _metric(wants(DashboardMetric.bloodGlucose),
        HcPermissions.readBloodGlucose, granted, () async {
      final entries = await _hc.readBloodGlucoseEntries(dayStart, dayEnd);
      return _latestByTime(entries, (e) => e.time)?.millimolesPerLiter;
    });
    final skinTemperature = await _metric(
        wants(DashboardMetric.skinTemperature) && _hc.isSkinTemperatureAvailable(),
        HcPermissions.readSkinTemperature, granted, () async {
      final entries = await _hc.readSkinTemperatureEntries(dayStart, dayEnd);
      return _latestByTime(entries, (e) => e.time)?.averageDeltaCelsius;
    });
    final trainingSignals = (query.includeWeeklyTrainingSignals &&
            wantsAny([
              DashboardMetric.weeklyCardioLoad,
              DashboardMetric.intensityMinutes,
            ]))
        ? await _guard(() => _readWeeklyTrainingSignals(date, activityWeekMode, granted))
        : null;
    final floors = await _metric(wants(DashboardMetric.floors),
        HcPermissions.readFloors, granted, () => _hc.readFloorsClimbed(date));
    final elevation = await _metric(wants(DashboardMetric.elevation),
        HcPermissions.readElevation, granted, () => _hc.readElevationGained(date));
    final wheelchairPushes = await _metric(wants(DashboardMetric.wheelchairPushes),
        HcPermissions.readWheelchairPushes, granted, () => _hc.readWheelchairPushes(date));
    final mindfulnessMinutes = await _metric(wants(DashboardMetric.mindfulness),
        HcPermissions.readMindfulness, granted, () => _hc.readMindfulnessMinutes(date));
    final menstruationPeriods = await _metric(wants(DashboardMetric.cycle),
        HcPermissions.readMenstruationPeriod, granted,
        () => _hc.readMenstruationPeriods(dayStart, dayEnd));
    final ovulationTests = await _metric(wants(DashboardMetric.cycle),
        HcPermissions.readOvulationTest, granted,
        () => _hc.readOvulationTests(dayStart, dayEnd));
    final basalBodyTemperature = await _metric(wants(DashboardMetric.cycle),
        HcPermissions.readBasalBodyTemperature, granted, () async {
      final entries = await _hc.readBasalBodyTemperatureEntries(dayStart, dayEnd);
      return _latestByTime(entries, (e) => e.time)?.temperatureCelsius;
    });
    // Body Energy is a lazy, optional tile (Kotlin `launchBodyEnergyLoad`): it
    // only loads when the body-energy repository is wired, the tile is wanted,
    // calibration is complete, and heart-rate read is granted (the detail
    // screen's permission). The repo runs `calculateBodyEnergyTimeline`; we keep
    // today's `latestDay` for the tile's `currentScore` / start / charged /
    // drained.
    final bodyEnergyTimeline = await _metric(
        wants(DashboardMetric.bodyEnergy) &&
            _bodyEnergy != null &&
            (_preferences?.bodyEnergyCalibration().setupCompleted ?? false),
        HcPermissions.readHeartRate,
        granted,
        () async => (await _bodyEnergy!.loadTimeline(BodyEnergyTimelineQuery(
              period: DatePeriod(date, date),
              range: TimeRange.day,
              refreshMode: query.refreshMode,
            )))
            .orThrow()
            .latestDay);

    final missingPerms = _dashboardPermissionsFor(metrics, showOpenVitalsCalculatedCalories)
        .where((p) => !granted.contains(p))
        .toSet();

    final latestWeight = weight;
    final latestHeight = height;
    final dailyMacros = macros;
    final caloriesBurned = calories;
    final dayHrvSamples = hrvSamples;
    final dayHrvRmssd = dayHrvSamples.isEmpty
        ? null
        : dayHrvSamples.map((s) => s.rmssdMs).reduce((a, b) => a + b) /
            dayHrvSamples.length;
    final latestBodyFatPercent = bodyFat;

    final metricSourcePackages = <DashboardMetric, String>{};
    void putSource(DashboardMetric metric, String? source) {
      if (source != null && source.trim().isNotEmpty) {
        metricSourcePackages[metric] = source;
      }
    }

    if (wants(DashboardMetric.sleep)) {
      putSource(DashboardMetric.sleep, dashboardSleep?.sleep?.source);
    }
    if (wantsAny([DashboardMetric.weight, DashboardMetric.bmi, DashboardMetric.ffmi])) {
      putSource(DashboardMetric.weight, latestWeight?.source);
    }
    if (wantsAny([DashboardMetric.height, DashboardMetric.bmi, DashboardMetric.ffmi])) {
      putSource(DashboardMetric.height, latestHeight?.source);
    }
    if (wants(DashboardMetric.bloodPressure)) {
      putSource(DashboardMetric.bloodPressure, bloodPressure?.source);
    }
    if (wants(DashboardMetric.spo2)) putSource(DashboardMetric.spo2, spO2?.source);
    if (wants(DashboardMetric.vo2Max)) {
      putSource(DashboardMetric.vo2Max, vo2Max?.source);
    }
    if (wants(DashboardMetric.workout)) {
      putSource(DashboardMetric.workout, workouts.firstOrNull?.source);
    }

    return DashboardData(
      date: date,
      steps: steps ?? 0,
      distanceMeters: distance ?? 0.0,
      caloriesKcal: caloriesBurned?.kcal ?? 0.0,
      caloriesKcalSource: caloriesBurned?.source ?? CaloriesBurnedSource.noData,
      activeCaloriesKcal: activeCalories,
      caloriesInKcal: (caloriesIn ?? 0) > 0 ? caloriesIn : null,
      proteinGrams: (dailyMacros?.proteinGrams ?? 0) > 0 ? dailyMacros?.proteinGrams : null,
      carbsGrams: (dailyMacros?.carbsGrams ?? 0) > 0 ? dailyMacros?.carbsGrams : null,
      fatGrams: (dailyMacros?.fatGrams ?? 0) > 0 ? dailyMacros?.fatGrams : null,
      caffeineGrams: _positiveOrNull(
          dailyMacros?.nutrientValues[NutritionNutrient.caffeine]),
      hydrationLiters: hydration ?? 0.0,
      workout: workouts.firstOrNull,
      workouts: workouts,
      sleep: dashboardSleep?.sleep,
      sleepScore: dashboardSleep?.sleepScore ?? SleepScoreEstimate.noData,
      weightKg: latestWeight?.weightKg,
      weightTime: latestWeight?.time,
      heightCm: latestHeight?.heightCm,
      heightTime: latestHeight?.time,
      bmi: wants(DashboardMetric.bmi)
          ? _bmi(latestWeight?.weightKg, latestHeight?.heightCm)
          : null,
      ffmi: wants(DashboardMetric.ffmi)
          ? _adjustedFfmi(
              weightKg: latestWeight?.weightKg,
              heightCm: latestHeight?.heightCm,
              bodyFatPercent: latestBodyFatPercent,
            )
          : null,
      bodyFatPercent: latestBodyFatPercent ?? 0.0,
      leanMassKg: leanMass,
      bmrKcal: bmr,
      boneMassKg: boneMass,
      bodyWaterMassKg: bodyWaterMass,
      avgHeartRateBpm: heartRate ?? 0,
      restingHeartRateBpm: restingHR ?? 0,
      restingHeartRateBaselineBpm: restingHRBaseline,
      hrvRmssdMs: dayHrvRmssd,
      hrvBaselineRmssdMs: hrvBaseline,
      hrvSampleCount: dayHrvSamples.length,
      hrvSampleStartTime: dayHrvSamples.firstOrNull?.time,
      hrvSampleEndTime: dayHrvSamples.lastOrNull?.time,
      latestSystolicMmHg: bloodPressure?.systolicMmHg,
      latestDiastolicMmHg: bloodPressure?.diastolicMmHg,
      latestSpO2Percent: spO2?.percent,
      latestVo2Max: vo2Max?.vo2MaxMlPerKgPerMin,
      avgRespiratoryRate: respiratoryRate,
      latestBodyTemperatureCelsius: bodyTemperature,
      latestBloodGlucoseMillimolesPerLiter: bloodGlucose,
      latestSkinTemperatureDeltaCelsius: skinTemperature,
      weeklyCardioLoad:
          wants(DashboardMetric.weeklyCardioLoad) ? trainingSignals?.cardioLoad : null,
      weeklyIntensityMinutes: wants(DashboardMetric.intensityMinutes)
          ? trainingSignals?.intensityMinutes
          : null,
      floorsClimbed: floors,
      elevationGainedMeters: elevation,
      wheelchairPushes: wheelchairPushes,
      mindfulnessMinutes: mindfulnessMinutes,
      menstruationPeriodDays: _menstruationDays(menstruationPeriods),
      ovulationTestCount: ovulationTests?.length,
      latestBasalBodyTemperatureCelsius: basalBodyTemperature,
      bodyEnergyTimeline: bodyEnergyTimeline,
      missingPermissions: missingPerms,
      loadedMetrics: metrics,
      supportedMetrics: _supportedMetrics(),
      metricSourcePackages: metricSourcePackages,
    );
  }

  Future<T?> _guard<T>(Future<T> Function() block) async {
    try {
      return await block();
    } catch (_) {
      return null;
    }
  }

  Future<_DashboardSleepData> _readDashboardSleep(
    LocalDate date,
    SleepRangeMode sleepRangeMode,
  ) async {
    final data = await _hc.readSleepData(
      date.minusDays(sleepScoreLookbackDays - 1),
      date,
      sleepRangeMode,
    );
    final sessions = data.sessions;
    final aggregate = data.dailyAggregateDurations
        .where((d) => d.date == date)
        .map((d) => d.durationMs)
        .firstOrNull ??
        0;
    var sleep =
        dailySleepSummary(sessions, date, sleepRangeMode: sleepRangeMode);
    if (sleep != null && aggregate > 0) {
      sleep = sleep.copyWith(durationMs: aggregate);
    }
    return _DashboardSleepData(
      sleep: sleep,
      sleepScore: calculateSleepScoreForDate(date, sessions, sleepRangeMode),
    );
  }

  Future<_DashboardWeeklyTrainingSignals> _readWeeklyTrainingSignals(
    LocalDate date,
    ActivityWeekMode activityWeekMode,
    Set<String> granted,
  ) async {
    final currentPeriod =
        DashboardAggregator.cardioLoadPeriod(date, activityWeekMode);
    final rangeStart =
        currentPeriod.start.minusDays(_cardioLoadHistoryPeriods * 7);
    final rangeEnd = currentPeriod.end;
    final heartRateSampleStart =
        currentPeriod.start.minusDays((_weeklyCardioHeartRateSampleWeeks - 1) * 7);

    final dailySteps = await _readCardioLoadSteps(rangeStart, rangeEnd, granted);
    final heartRateSamples = granted.contains(HcPermissions.readHeartRate)
        ? await _hc.readHeartRateSamples(
            localDayStart(heartRateSampleStart), localDayEnd(currentPeriod.end))
        : const <HeartRateSample>[];
    final restingHeartRates = granted.contains(HcPermissions.readRestingHeartRate)
        ? await _hc.readDailyRestingHR(rangeStart, rangeEnd)
        : const <DailyRestingHR>[];
    final workouts = granted.contains(HcPermissions.readExercise)
        ? await _hc.readExerciseSessions(
            localDayStart(rangeStart), localDayEnd(rangeEnd))
        : const <ExerciseData>[];

    final stepsByDate = {for (final s in dailySteps) s.date: s};
    final restingByDate = {for (final r in restingHeartRates) r.date: r};
    final baselineResting = DashboardAggregator.medianLongOrNull(
        restingHeartRates.map((r) => r.bpm).toList());
    final observedMax = heartRateSamples.isEmpty
        ? null
        : heartRateSamples.map((s) => s.beatsPerMinute).reduce(math.max);
    final samplesByDate = <LocalDate, List<HeartRateSample>>{};
    for (final sample in heartRateSamples) {
      final d = LocalDate.fromDateTime(sample.time.toLocal());
      (samplesByDate[d] ??= <HeartRateSample>[]).add(sample);
    }

    final cardioByDate = <LocalDate, CardioLoadEstimate>{};
    final intensityByDate = <LocalDate, IntensityMinutesEstimate>{};
    for (final day in DashboardAggregator.datesInRange(rangeStart, rangeEnd)) {
      final windows = DashboardAggregator.cardioLoadWindows(workouts, day);
      final cardio = calculateCardioLoad(
        stepsByDate[day],
        samplesByDate[day] ?? const [],
        restingByDate[day]?.bpm,
        baselineResting,
        observedMax,
        windows,
      );
      final intensity = calculateIntensityMinutes(
        samplesByDate[day] ?? const [],
        restingByDate[day]?.bpm,
        baselineResting,
        observedMax,
        windows,
        DashboardAggregator.intensityWorkoutInputs(workouts, day),
        stepsByDate[day]?.activeCaloriesKcal,
        cardio.score,
      );
      cardioByDate[day] = cardio;
      intensityByDate[day] = intensity;
    }

    final currentDays =
        DashboardAggregator.datesInRange(currentPeriod.start, currentPeriod.end)
            .toList();
    final currentEstimates = [
      for (final day in currentDays)
        cardioByDate[day] ?? CardioLoadEstimate.noData,
    ];
    final currentScore =
        currentEstimates.fold<int>(0, (a, e) => a + e.score);
    final todayScore = cardioByDate[date]?.score ?? 0;
    final previousPeriodScores = [
      for (var periodsAgo = 1; periodsAgo <= _cardioLoadHistoryPeriods; periodsAgo++)
        DashboardAggregator.datesInRange(
          currentPeriod.start.minusDays(periodsAgo * 7),
          currentPeriod.start.minusDays(periodsAgo * 7).plusDays(6),
        ).fold<int>(0, (a, day) => a + (cardioByDate[day]?.score ?? 0)),
    ];
    final cardioTargetDays =
        _daysBetween(currentPeriod.start, currentPeriod.end) + 1;
    final target = DashboardAggregator.weeklyCardioTarget(
      currentScore: currentScore,
      daysElapsed: cardioTargetDays,
      previousWeekScores: previousPeriodScores,
    );
    final cardioLoad = target == null
        ? null
        : DashboardWeeklyCardioLoad(
            currentScore: currentScore,
            targetScore: target.score,
            todayScore: todayScore,
            confidence:
                DashboardAggregator.weeklyCardioConfidence(currentEstimates),
            targetSource: target.source,
          );

    final currentIntensity = [
      for (final day in currentDays)
        intensityByDate[day] ?? IntensityMinutesEstimate.noData,
    ];
    final intensityMinutes = DashboardWeeklyIntensityMinutes(
      moderateMinutes: currentIntensity.fold<int>(0, (a, e) => a + e.moderateMinutes),
      vigorousMinutes: currentIntensity.fold<int>(0, (a, e) => a + e.vigorousMinutes),
      moderateEquivalentMinutes:
          currentIntensity.fold<int>(0, (a, e) => a + e.moderateEquivalentMinutes),
      todayModerateEquivalentMinutes:
          intensityByDate[date]?.moderateEquivalentMinutes ?? 0,
      daysElapsed:
          (_daysBetween(currentPeriod.start, date) + 1).clamp(1, 7).toInt(),
      confidence:
          DashboardAggregator.weeklyIntensityConfidence(currentIntensity),
    );

    return _DashboardWeeklyTrainingSignals(
      cardioLoad: cardioLoad,
      intensityMinutes: intensityMinutes,
    );
  }

  Future<List<DailySteps>> _readCardioLoadSteps(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (granted.contains(HcPermissions.readSteps) &&
        granted.contains(HcPermissions.readDistance)) {
      return _hc.readDailySteps(
        start,
        end,
        includeActiveCalories: granted.contains(HcPermissions.readActiveCalories),
      );
    }
    if (granted.contains(HcPermissions.readSteps)) {
      final result = <DailySteps>[];
      for (final date in DashboardAggregator.datesInRange(start, end)) {
        result.add(DailySteps(
          date: date,
          steps: await _hc.readSteps(date),
          distanceMeters: 0.0,
        ));
      }
      return result;
    }
    return const [];
  }

  bool _canEstimateTotalCalories(
    Set<String> granted,
    bool showOpenVitalsCalculatedCalories,
  ) =>
      showOpenVitalsCalculatedCalories &&
      granted.contains(HcPermissions.readActiveCalories) &&
      granted.contains(HcPermissions.readBmr);

  /// The metrics the installed Health Connect provider can serve at all — i.e.
  /// every permission the metric reads is one the provider defines. Metrics
  /// outside this set can never be granted, so the dashboard hides their tiles
  /// entirely rather than showing a tile that can never fill.
  ///
  /// Deliberately evaluated with `showOpenVitalsCalculatedCalories: false`:
  /// whether a metric is *supported by the device* must not depend on a user
  /// preference.
  Set<DashboardMetric> _supportedMetrics() {
    final managed = _hc.permissionService.managedPermissions;
    return {
      for (final metric in DashboardMetric.values)
        if (_permissionsForMetric(metric, false)
            case final perms when perms.isNotEmpty && perms.every(managed.contains))
          metric,
    };
  }

  Set<String> _dashboardPermissionsFor(
    Set<DashboardMetric> metrics,
    bool showOpenVitalsCalculatedCalories,
  ) {
    final result = <String>{};
    for (final metric in metrics) {
      result.addAll(
        _permissionsForMetric(metric, showOpenVitalsCalculatedCalories),
      );
    }
    // `grantedPermissions()` only ever reports permissions inside
    // `managedPermissions`, which is feature-gated and has the provider's
    // unsupported permissions subtracted. Anything outside it can never be
    // reported as granted nor meaningfully requested, so keeping it here would
    // strand the permission callout on a set the user cannot grant.
    return result.intersection(_hc.permissionService.managedPermissions);
  }

  /// The raw (unfiltered) Health Connect permissions [metric] reads.
  Set<String> _permissionsForMetric(
    DashboardMetric metric,
    bool showOpenVitalsCalculatedCalories,
  ) =>
      switch (metric) {
        DashboardMetric.steps => {HcPermissions.readSteps},
        DashboardMetric.distance => {HcPermissions.readDistance},
        DashboardMetric.caloriesOut => showOpenVitalsCalculatedCalories
            ? {
                HcPermissions.readTotalCalories,
                HcPermissions.readActiveCalories,
                HcPermissions.readBmr,
              }
            : {HcPermissions.readTotalCalories},
        DashboardMetric.activeCalories => {
            HcPermissions.readActiveCalories,
            HcPermissions.readSteps,
            HcPermissions.readDistance,
          },
        DashboardMetric.floors => {HcPermissions.readFloors},
        DashboardMetric.elevation => {HcPermissions.readElevation},
        DashboardMetric.wheelchairPushes => {HcPermissions.readWheelchairPushes},
        DashboardMetric.workout => {HcPermissions.readExercise},
        DashboardMetric.sleep => {HcPermissions.readSleep},
        DashboardMetric.hydration => {HcPermissions.readHydration},
        DashboardMetric.caloriesIn ||
        DashboardMetric.protein ||
        DashboardMetric.carbs ||
        DashboardMetric.fat ||
        DashboardMetric.caffeine =>
          {HcPermissions.readNutrition},
        DashboardMetric.weight => {HcPermissions.readWeight},
        DashboardMetric.height => {HcPermissions.readHeight},
        DashboardMetric.bmi => {HcPermissions.readWeight, HcPermissions.readHeight},
        DashboardMetric.ffmi => {
            HcPermissions.readWeight,
            HcPermissions.readHeight,
            HcPermissions.readBodyFat,
          },
        DashboardMetric.bodyFat => {HcPermissions.readBodyFat},
        DashboardMetric.leanMass => {HcPermissions.readLeanMass},
        DashboardMetric.bmr => {HcPermissions.readBmr},
        DashboardMetric.boneMass => {HcPermissions.readBoneMass},
        DashboardMetric.bodyWaterMass => {HcPermissions.readBodyWaterMass},
        DashboardMetric.avgHeartRate => {HcPermissions.readHeartRate},
        // Body Energy is derived; its dashboard tile is permission-gated on
        // heart-rate read, matching the detail screen's requirement.
        DashboardMetric.bodyEnergy => {HcPermissions.readHeartRate},
        DashboardMetric.restingHeartRate => {HcPermissions.readRestingHeartRate},
        DashboardMetric.hrv => {HcPermissions.readHrv},
        DashboardMetric.bloodPressure => {HcPermissions.readBloodPressure},
        DashboardMetric.spo2 => {HcPermissions.readSpO2},
        DashboardMetric.vo2Max => {HcPermissions.readVo2Max},
        DashboardMetric.respiratoryRate => {HcPermissions.readRespiratoryRate},
        DashboardMetric.bodyTemperature => {HcPermissions.readBodyTemperature},
        DashboardMetric.bloodGlucose => {HcPermissions.readBloodGlucose},
        // No explicit feature-flag check: READ_SKIN_TEMPERATURE drops out of
        // `managedPermissions` when the feature is unavailable, so both callers
        // already treat it as unsupported.
        DashboardMetric.skinTemperature => {HcPermissions.readSkinTemperature},
        DashboardMetric.weeklyCardioLoad => {HcPermissions.readSteps},
        DashboardMetric.intensityMinutes => {
            HcPermissions.readHeartRate,
            HcPermissions.readRestingHeartRate,
            HcPermissions.readExercise,
            HcPermissions.readActiveCalories,
            HcPermissions.readSteps,
            HcPermissions.readDistance,
          },
        DashboardMetric.mindfulness => {HcPermissions.readMindfulness},
        DashboardMetric.cycle => {
            HcPermissions.readMenstruationPeriod,
            HcPermissions.readOvulationTest,
            HcPermissions.readBasalBodyTemperature,
          },
      };

  double? _bmi(double? weightKg, double? heightCm) {
    if (weightKg == null || heightCm == null || heightCm <= 0) return null;
    final meters = heightCm / 100.0;
    return weightKg / (meters * meters);
  }

  double? _adjustedFfmi({
    required double? weightKg,
    required double? heightCm,
    required double? bodyFatPercent,
  }) {
    final weight = (weightKg != null && weightKg > 0) ? weightKg : null;
    if (weight == null) return null;
    final heightMeters =
        (heightCm != null && heightCm > 0) ? heightCm / 100.0 : null;
    if (heightMeters == null) return null;
    final bodyFatRatio = (bodyFatPercent != null &&
            bodyFatPercent >= 0 &&
            bodyFatPercent <= 100)
        ? bodyFatPercent / 100.0
        : null;
    if (bodyFatRatio == null) return null;
    final fatFreeMassKg = weight * (1.0 - bodyFatRatio);
    final ffmi = fatFreeMassKg / (heightMeters * heightMeters);
    return ffmi + (6.3 * (1.8 - heightMeters));
  }

  int? _menstruationDays(List<dynamic>? periods) {
    if (periods == null) return null;
    var total = 0;
    for (final period in periods) {
      final startDate = LocalDate.fromDateTime(period.startTime.toLocal());
      final endDate = LocalDate.fromDateTime(
          period.endTime.subtract(const Duration(milliseconds: 1)).toLocal());
      final days = _daysBetween(startDate, endDate) + 1;
      total += days < 1 ? 1 : days;
    }
    return total;
  }

  double? _positiveOrNull(double? value) =>
      (value != null && value > 0) ? value : null;

  int _daysBetween(LocalDate start, LocalDate end) => end.epochDay - start.epochDay;

  T? _latestByTime<T>(List<T> items, DateTime Function(T) time) {
    if (items.isEmpty) return null;
    return items.reduce((a, b) => time(a).isAfter(time(b)) ? a : b);
  }
}

class _DashboardSleepData {
  const _DashboardSleepData({required this.sleep, required this.sleepScore});

  final SleepData? sleep;
  final SleepScoreEstimate sleepScore;
}

class _DashboardWeeklyTrainingSignals {
  const _DashboardWeeklyTrainingSignals({
    required this.cardioLoad,
    required this.intensityMinutes,
  });

  final DashboardWeeklyCardioLoad? cardioLoad;
  final DashboardWeeklyIntensityMinutes intensityMinutes;
}
