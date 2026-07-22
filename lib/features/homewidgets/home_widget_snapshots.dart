import '../../core/presentation/display_value.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../domain/insights/body_energy_timeline.dart';
import '../../domain/insights/daily_readiness.dart';
import '../../domain/model/dashboard_data.dart';
import '../../domain/model/dashboard_query.dart';
import '../../l10n/app_localizations.dart';
import 'home_widget_service.dart';

/// Pure snapshot builders: a loaded [DashboardData] → the flat
/// [HomeWidgetSnapshot] the native Glance composables render.
///
/// Ports the Kotlin `loadDailyReadinessSnapshot` / `loadBodyEnergySnapshot` /
/// `loadTodayVitalsSnapshot` (HomeReadinessWidgets.kt) and
/// `DashboardData.toSnapshot` (HomeMetricWidget.kt). Kotlin's widgets load their
/// own data from Hilt on `onUpdate`; here the data is loaded once by
/// `HomeWidgetRefresher` and passed in, so these stay pure functions — no I/O,
/// no `BuildContext`.
///
/// [AppLocalizations] is a parameter rather than a `context` lookup because the
/// same builders run in the alarm isolate (see `home_widget_alarm.dart`), where
/// there is no widget tree; the caller resolves it with `lookupAppLocalizations`.
///
/// Every number is formatted to a `String` here: the `home_widget` plugin stores
/// a Dart `double` as raw long bits, which the native read mangles.

/// The value shown when a metric has no reading (Kotlin's literal `"--"`).
const String homeWidgetNoValue = '--';

/// The metrics offered by the metric widget's configuration screen, matching the
/// Kotlin `homeMetricWidgetCatalog()`.
///
/// Kotlin drops `CARDIO_LOAD` (a duplicate of `WEEKLY_CARDIO_LOAD`) and
/// `CAFFEINE`. The Flutter [DashboardMetric] has no `cardioLoad` at all, and adds
/// an `intensityMinutes` that Kotlin's `DashboardWidgetId` does not have — so
/// that one is dropped too, leaving exactly the Kotlin catalog.
List<DashboardMetric> homeMetricWidgetCatalog() => <DashboardMetric>[
      for (final metric in DashboardMetric.values)
        if (metric != DashboardMetric.caffeine &&
            metric != DashboardMetric.intensityMinutes)
          metric,
    ];

/// Daily-readiness widget snapshot (Kotlin `loadDailyReadinessSnapshot`).
///
/// Falls back to "--" / "Open for details" when readiness cannot be computed —
/// which is what an `UNKNOWN` state means: no signal was available at all.
HomeWidgetSnapshot buildDailyReadinessSnapshot(
  DashboardData data,
  AppLocalizations l10n, {
  DailyReadinessGoalInputs goals = const DailyReadinessGoalInputs(),
}) {
  final title = l10n.screenDailyReadiness;
  const route = 'daily_readiness';
  final insight = calculateDailyReadiness(data, goals: goals);
  if (insight.state == ReadinessState.unknown) {
    return _fallbackStatusSnapshot(l10n, title: title, route: route);
  }
  return HomeWidgetSnapshot(
    title: title,
    value: '${insight.score}',
    subtitle: insight.statusTitle,
    route: route,
    rows: [
      HomeWidgetRow(
        label: l10n.dashboardReadinessRecommended,
        value: insight.recommendation,
      ),
    ],
  );
}

/// Body-energy widget snapshot (Kotlin `loadBodyEnergySnapshot`).
///
/// Reads [DashboardData.bodyEnergyTimeline], which the dashboard loader already
/// populates (Kotlin re-queries the `BodyEnergyRepository` from the widget).
HomeWidgetSnapshot buildBodyEnergySnapshot(
  DashboardData data,
  AppLocalizations l10n,
) {
  final title = l10n.screenBodyEnergy;
  final route = 'daily_readiness/body_energy/${data.date}';
  final timeline = data.bodyEnergyTimeline;
  if (timeline == null) {
    return _fallbackStatusSnapshot(l10n, title: title, route: route);
  }
  return HomeWidgetSnapshot(
    title: title,
    value: '${timeline.currentScore}',
    subtitle: homeWidgetBodyEnergyStatus(timeline.currentScore, l10n),
    route: route,
    series: homeWidgetSeries(
      [for (final point in timeline.points) point.score],
    ),
    rows: [
      HomeWidgetRow(
        label: l10n.bodyEnergyTimelineStart,
        value: '${timeline.startScore}',
      ),
      HomeWidgetRow(
        label: l10n.bodyEnergyTimelineCharged,
        value: '+${timeline.charged}',
      ),
      HomeWidgetRow(
        label: l10n.bodyEnergyTimelineDrained,
        value: '-${timeline.drained}',
      ),
    ],
  );
}

/// Thins [values] down to at most [maxHomeWidgetSeriesPoints], evenly.
///
/// The LAST value always survives. It is the one the widget also prints as the
/// current score, and a plot whose line ended somewhere other than the number
/// beside it would be visibly disagreeing with itself.
List<int> homeWidgetSeries(List<int> values) {
  if (values.length <= maxHomeWidgetSeriesPoints) return List.of(values);
  final out = <int>[];
  // Spread the sample points across the whole range rather than taking every
  // Nth from the start, which would stop short of the end by up to N.
  final step = (values.length - 1) / (maxHomeWidgetSeriesPoints - 1);
  for (var i = 0; i < maxHomeWidgetSeriesPoints; i++) {
    out.add(values[(i * step).round()]);
  }
  return out;
}

/// The one-word body-energy status (Kotlin `bodyEnergyStatus`).
String homeWidgetBodyEnergyStatus(int score, AppLocalizations l10n) {
  if (score >= 80) return l10n.homeWidgetBodyEnergyCharged;
  if (score >= 60) return l10n.homeWidgetBodyEnergySteady;
  if (score >= 40) return l10n.homeWidgetBodyEnergyLimited;
  return l10n.homeWidgetBodyEnergyLow;
}

/// Today-vitals widget snapshot (Kotlin `loadTodayVitalsSnapshot`).
///
/// The row order is the Kotlin one and is load-bearing — the composable renders
/// them in sequence, splitting into two columns at the halfway point.
HomeWidgetSnapshot buildTodayVitalsSnapshot(
  DashboardData data,
  UnitFormatter f,
  AppLocalizations l10n, {
  DailyReadinessGoalInputs goals = const DailyReadinessGoalInputs(),
}) {
  final insight = calculateDailyReadiness(data, goals: goals);
  final timeline = data.bodyEnergyTimeline;
  return HomeWidgetSnapshot(
    title: l10n.homeWidgetTodayTitle,
    route: 'dashboard',
    rows: [
      // Readiness is dropped entirely (rather than shown as "--") when it has no
      // signals to work with, exactly as Kotlin does.
      if (insight.state != ReadinessState.unknown)
        HomeWidgetRow(
          label: l10n.screenDailyReadiness,
          value: '${insight.score}',
          subtitle: insight.statusTitle,
        ),
      _bodyEnergyRow(timeline, l10n),
      for (final metric in const [
        DashboardMetric.sleep,
        DashboardMetric.steps,
        DashboardMetric.distance,
        DashboardMetric.restingHeartRate,
        DashboardMetric.hrv,
        DashboardMetric.weeklyCardioLoad,
        DashboardMetric.hydration,
      ])
        _metricRow(
          metric,
          data,
          f,
          l10n,
          // Kotlin overrides the HRV label: the full metric title
          // ("Heart rate variability (HRV)") does not fit the row.
          label: metric == DashboardMetric.hrv ? l10n.homeWidgetHrvShort : null,
        ),
    ],
  );
}

/// Configurable metric-widget snapshot (Kotlin `loadSnapshot` +
/// `DashboardData.toSnapshot`).
///
/// Three states, in Kotlin's order of precedence: a missing permission wins over
/// a missing reading, and a present reading carries "Today" as its subtitle.
HomeWidgetSnapshot buildMetricSnapshot(
  DashboardMetric metric,
  DashboardData data,
  UnitFormatter f,
  AppLocalizations l10n,
) {
  if (data.missingPermissions.isNotEmpty) {
    return HomeWidgetSnapshot(
      title: homeWidgetMetricTitle(metric, l10n),
      value: homeWidgetNoValue,
      subtitle: l10n.homeMetricWidgetPermissionNeeded,
      route: homeWidgetMetricRoute(metric, data.date.toString()),
    );
  }
  return _metricDataSnapshot(metric, data, f, l10n);
}

/// The reading-only snapshot, Kotlin's `DashboardData.toSnapshot`.
///
/// Split out from [buildMetricSnapshot] because the today-vitals rows go through
/// this directly: Kotlin's permission fallback lives in `loadSnapshot`, so a
/// missing permission blanks the *metric widget* but leaves the today rows to
/// report "--" / "No data" per metric.
HomeWidgetSnapshot _metricDataSnapshot(
  DashboardMetric metric,
  DashboardData data,
  UnitFormatter f,
  AppLocalizations l10n,
) {
  final display = _metricDisplayValue(metric, data, f, l10n);
  return HomeWidgetSnapshot(
    title: homeWidgetMetricTitle(metric, l10n),
    value: display?.value ?? homeWidgetNoValue,
    unit: display?.unit ?? '',
    subtitle: display == null
        ? l10n.noData
        : _metricSubtitle(metric, data, l10n) ?? l10n.periodToday,
    route: homeWidgetMetricRoute(metric, data.date.toString()),
  );
}

/// The route a metric widget opens, in the Kotlin `Screen.*.createRoute` wire
/// form (no leading slash) — `home_widget_launch.dart` maps it back to a
/// go_router location.
String homeWidgetMetricRoute(DashboardMetric metric, String date) =>
    switch (metric) {
      DashboardMetric.bodyEnergy => 'daily_readiness/body_energy/$date',
      // Kotlin sends the caffeine widget to the dashboard: it has no metric
      // detail screen of its own in the widget's allow-list.
      DashboardMetric.caffeine => 'dashboard',
      _ => 'metric/${metric.storageName}',
    };

/// The metric widget's title (Kotlin `DashboardWidgetId.homeMetricTitleRes()`).
String homeWidgetMetricTitle(DashboardMetric metric, AppLocalizations l10n) =>
    switch (metric) {
      DashboardMetric.steps => l10n.metricSteps,
      DashboardMetric.distance => l10n.metricDistance,
      DashboardMetric.caloriesOut => l10n.metricCaloriesOut,
      DashboardMetric.activeCalories => l10n.metricActiveCalories,
      DashboardMetric.floors => l10n.metricFloorsClimbed,
      DashboardMetric.elevation => l10n.metricElevationGained,
      DashboardMetric.wheelchairPushes => l10n.metricWheelchairPushes,
      DashboardMetric.workout => l10n.metricWorkout,
      DashboardMetric.sleep => l10n.metricSleep,
      DashboardMetric.bodyEnergy => l10n.metricBodyEnergy,
      DashboardMetric.hydration => l10n.metricHydration,
      DashboardMetric.caloriesIn => l10n.metricCaloriesIn,
      DashboardMetric.protein => l10n.metricProtein,
      DashboardMetric.carbs => l10n.metricCarbs,
      DashboardMetric.fat => l10n.metricFat,
      DashboardMetric.caffeine => l10n.metricCaffeine,
      DashboardMetric.weight => l10n.metricWeight,
      DashboardMetric.height => l10n.metricHeight,
      DashboardMetric.bmi => l10n.metricBmi,
      DashboardMetric.ffmi => l10n.metricFfmi,
      DashboardMetric.bodyFat => l10n.metricBodyFat,
      DashboardMetric.leanMass => l10n.metricLeanMass,
      DashboardMetric.bmr => l10n.metricBmr,
      DashboardMetric.boneMass => l10n.metricBoneMass,
      DashboardMetric.bodyWaterMass => l10n.metricBodyWaterMass,
      DashboardMetric.avgHeartRate => l10n.metricAvgHeartRate,
      DashboardMetric.restingHeartRate => l10n.metricRestingHeartRate,
      DashboardMetric.hrv => l10n.metricHrv,
      DashboardMetric.bloodPressure => l10n.metricBloodPressure,
      DashboardMetric.spo2 => l10n.metricSpo2,
      DashboardMetric.vo2Max => l10n.metricVo2Max,
      DashboardMetric.respiratoryRate => l10n.metricRespiratoryRate,
      DashboardMetric.bodyTemperature => l10n.metricBodyTemp,
      DashboardMetric.bloodGlucose => l10n.metricBloodGlucose,
      DashboardMetric.skinTemperature => l10n.metricSkinTemperature,
      DashboardMetric.weeklyCardioLoad => l10n.metricWeeklyCardioLoad,
      DashboardMetric.mindfulness => l10n.metricMindfulness,
      DashboardMetric.cycle => l10n.metricCycle,
      // Not in the Kotlin widget catalog (no `DashboardWidgetId` for it), so it
      // has no string of its own.
      // TODO(l10n): add an "Intensity minutes" key when the ARB is regenerated.
      DashboardMetric.intensityMinutes => 'Intensity minutes',
    };

/// The formatted reading for [metric], or null when there is none.
///
/// A faithful port of the Kotlin `DashboardData.toSnapshot` table, down to which
/// metrics treat a zero as "no reading" (`takeIf { it > 0.0 }`) and which report
/// it verbatim. It cannot delegate to `buildDashboardSummary`: that mapper is
/// keyed by tile *title*, filters by device support, and folds Steps / Weekly
/// cardio into hero rings rather than metrics — but it does share the same
/// formatting primitives, [UnitFormatter] + [DisplayValue], so no number
/// formatting is hand-rolled here.
DisplayValue? _metricDisplayValue(
  DashboardMetric metric,
  DashboardData data,
  UnitFormatter f,
  AppLocalizations l10n,
) {
  DisplayValue? countOnly(int? value) =>
      value == null ? null : DisplayValue(f.count(value), '');
  double? positive(double? value) =>
      (value != null && value > 0.0) ? value : null;
  int? positiveInt(int? value) => (value != null && value > 0) ? value : null;
  DisplayValue? grams(double? value) =>
      value == null ? null : DisplayValue(f.decimal(value, 0), l10n.unitGrams);

  switch (metric) {
    case DashboardMetric.steps:
      return countOnly(data.steps);
    case DashboardMetric.distance:
      final meters = positive(data.distanceMeters);
      return meters == null ? null : f.distance(meters);
    case DashboardMetric.caloriesOut:
      final kcal = positive(data.caloriesKcal);
      return kcal == null ? null : f.energy(kcal);
    case DashboardMetric.activeCalories:
      final kcal = data.activeCaloriesKcal;
      return kcal == null ? null : f.energy(kcal);
    case DashboardMetric.floors:
      return countOnly(data.floorsClimbed);
    case DashboardMetric.elevation:
      final meters = data.elevationGainedMeters;
      return meters == null ? null : f.elevation(meters);
    case DashboardMetric.wheelchairPushes:
      return countOnly(data.wheelchairPushes);
    case DashboardMetric.workout:
      return countOnly(positiveInt(data.workouts.length));
    case DashboardMetric.sleep:
      final sleep = data.sleep;
      return sleep == null
          ? null
          : DisplayValue(f.duration(sleep.durationMs), '');
    case DashboardMetric.bodyEnergy:
      final timeline = data.bodyEnergyTimeline;
      return timeline == null
          ? null
          : DisplayValue(f.count(timeline.currentScore), '');
    case DashboardMetric.hydration:
      final liters = positive(data.hydrationLiters);
      return liters == null ? null : f.hydration(liters);
    case DashboardMetric.caloriesIn:
      final kcal = data.caloriesInKcal;
      return kcal == null ? null : f.energy(kcal);
    case DashboardMetric.protein:
      return grams(data.proteinGrams);
    case DashboardMetric.carbs:
      return grams(data.carbsGrams);
    case DashboardMetric.fat:
      return grams(data.fatGrams);
    case DashboardMetric.caffeine:
      final caffeine = positive(data.caffeineGrams);
      // The dashboard's caffeine tile likewise renders grams as milligrams.
      return caffeine == null
          ? null
          : DisplayValue(f.decimal(caffeine * 1000, 0), 'mg');
    case DashboardMetric.weight:
      final kg = data.weightKg;
      return kg == null ? null : f.weight(kg);
    case DashboardMetric.height:
      final cm = data.heightCm;
      return cm == null ? null : f.height(cm);
    case DashboardMetric.bmi:
      final bmi = data.bmi;
      return bmi == null ? null : DisplayValue(f.decimal(bmi, 1), '');
    case DashboardMetric.ffmi:
      final ffmi = data.ffmi;
      return ffmi == null ? null : DisplayValue(f.decimal(ffmi, 1), '');
    case DashboardMetric.bodyFat:
      final percent = positive(data.bodyFatPercent);
      return percent == null ? null : f.percent(percent);
    case DashboardMetric.leanMass:
      final kg = data.leanMassKg;
      return kg == null ? null : f.bodyMass(kg);
    case DashboardMetric.bmr:
      final kcal = data.bmrKcal;
      return kcal == null ? null : f.energy(kcal);
    case DashboardMetric.boneMass:
      final kg = data.boneMassKg;
      return kg == null ? null : f.bodyMass(kg);
    case DashboardMetric.bodyWaterMass:
      final kg = data.bodyWaterMassKg;
      return kg == null ? null : f.bodyMass(kg);
    case DashboardMetric.avgHeartRate:
      final bpm = positiveInt(data.avgHeartRateBpm);
      return bpm == null ? null : f.heartRate(bpm);
    case DashboardMetric.restingHeartRate:
      final bpm = positiveInt(data.restingHeartRateBpm);
      return bpm == null ? null : f.heartRate(bpm);
    case DashboardMetric.hrv:
      final hrv = data.hrvRmssdMs;
      return hrv == null ? null : f.hrv(hrv);
    case DashboardMetric.bloodPressure:
      final systolic = data.latestSystolicMmHg;
      final diastolic = data.latestDiastolicMmHg;
      return (systolic == null || diastolic == null)
          ? null
          : f.bloodPressure(systolic, diastolic);
    case DashboardMetric.spo2:
      final spo2 = data.latestSpO2Percent;
      return spo2 == null ? null : f.percent(spo2, decimals: 0);
    case DashboardMetric.vo2Max:
      final vo2 = data.latestVo2Max;
      return vo2 == null ? null : f.vo2Max(vo2);
    case DashboardMetric.respiratoryRate:
      final rate = data.avgRespiratoryRate;
      return rate == null ? null : f.respiratoryRate(rate);
    case DashboardMetric.bodyTemperature:
      final celsius = data.latestBodyTemperatureCelsius;
      return celsius == null ? null : f.temperature(celsius);
    case DashboardMetric.bloodGlucose:
      final glucose = data.latestBloodGlucoseMillimolesPerLiter;
      return glucose == null ? null : f.bloodGlucose(glucose);
    case DashboardMetric.skinTemperature:
      final delta = data.latestSkinTemperatureDeltaCelsius;
      return delta == null ? null : f.temperatureDelta(delta);
    case DashboardMetric.weeklyCardioLoad:
      final load = data.weeklyCardioLoad;
      return load == null
          ? null
          : DisplayValue(
              l10n.dashboardWeeklyCardioLoadProgress(
                load.currentScore,
                load.targetScore,
              ),
              '',
            );
    case DashboardMetric.intensityMinutes:
      final intensity = data.weeklyIntensityMinutes;
      return intensity == null
          ? null
          : DisplayValue(f.count(intensity.moderateEquivalentMinutes), 'min');
    case DashboardMetric.mindfulness:
      final minutes = data.mindfulnessMinutes;
      return minutes == null ? null : f.minutes(minutes);
    case DashboardMetric.cycle:
      final periodDays = data.menstruationPeriodDays;
      if (periodDays != null) {
        return DisplayValue(f.count(periodDays), l10n.unitDays);
      }
      final tests = data.ovulationTestCount;
      if (tests != null) {
        return DisplayValue(f.count(tests), l10n.unitTests);
      }
      final basal = data.latestBasalBodyTemperatureCelsius;
      return basal == null ? null : f.temperature(basal);
  }
}

/// The two metrics whose subtitle carries data rather than the day ("Today").
String? _metricSubtitle(
  DashboardMetric metric,
  DashboardData data,
  AppLocalizations l10n,
) =>
    switch (metric) {
      DashboardMetric.bodyEnergy => switch (data.bodyEnergyTimeline) {
          final t? => '+${t.charged} / -${t.drained}',
          null => null,
        },
      DashboardMetric.weeklyCardioLoad => switch (data.weeklyCardioLoad) {
          final load? => l10n.dashboardCardioLoadPercent(load.progressPercent),
          null => null,
        },
      _ => null,
    };

/// A today-vitals row for [metric] (Kotlin `HomeDashboardWidgetResult.row`).
///
/// Value and unit are joined ("8,432" + "steps"), and the "Today" subtitle is
/// dropped — in a list of today's rows it is noise.
HomeWidgetRow _metricRow(
  DashboardMetric metric,
  DashboardData data,
  UnitFormatter f,
  AppLocalizations l10n, {
  String? label,
}) {
  final snapshot = _metricDataSnapshot(metric, data, f, l10n);
  return HomeWidgetRow(
    label: label ?? snapshot.title,
    value: DisplayValue(snapshot.value, snapshot.unit).text,
    subtitle: snapshot.subtitle == l10n.periodToday ? '' : snapshot.subtitle,
  );
}

/// The body-energy row of the today-vitals widget (Kotlin `bodyEnergyRow`).
HomeWidgetRow _bodyEnergyRow(
  BodyEnergyTimeline? timeline,
  AppLocalizations l10n,
) =>
    timeline == null
        ? HomeWidgetRow(
            label: l10n.screenBodyEnergy,
            value: homeWidgetNoValue,
            subtitle: l10n.noData,
          )
        : HomeWidgetRow(
            label: l10n.screenBodyEnergy,
            value: '${timeline.currentScore}',
            subtitle: '+${timeline.charged} / -${timeline.drained}',
          );

/// Kotlin `fallbackStatusSnapshot`: the widget still routes into the app, where
/// the user can see *why* there is nothing to show.
HomeWidgetSnapshot _fallbackStatusSnapshot(
  AppLocalizations l10n, {
  required String title,
  required String route,
}) =>
    HomeWidgetSnapshot(
      title: title,
      value: homeWidgetNoValue,
      subtitle: l10n.homeMetricWidgetOpenForDetails,
      route: route,
    );
