import 'package:flutter/material.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/unit_formatter.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../domain/insights/daily_goals.dart';
import '../../../domain/insights/sleep_score.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/dashboard_data.dart';
import '../../../domain/model/dashboard_query.dart';
import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';
import '../../../ui/theme/app_colors.dart';

part 'dashboard_display.freezed.dart';

/// The screen-ready derivation of one loaded [DashboardData]: the summary
/// mapped to ring/tile models, the user's saved layout already applied (order,
/// hidden set, edit-mode expansion), the add-tray contents, and today's
/// activities.
///
/// Built once per publish by [buildDashboardDisplay] — on each of the two load
/// passes, and again on every layout mutation — and stored on the state. The
/// screen renders it and derives nothing.
@freezed
abstract class DashboardDisplay with _$DashboardDisplay {
  const factory DashboardDisplay({
    /// Every tile in the user's saved order, hidden ones included — the set the
    /// edit grid and the add-tray are computed from.
    required List<StatTileData> orderedTiles,

    /// The tiles the carousel actually shows.
    required List<StatTileData> visibleTiles,

    /// Both hero rings in the user's saved order, hidden ones included.
    required List<RingCardData> orderedRings,

    /// The hero rings the top row actually shows.
    required List<RingCardData> visibleRings,

    /// The effective hidden set: the saved one, plus (while editing) every
    /// unsupported tile the user has never deliberately placed.
    required Set<String> hiddenTitles,

    /// The titles materialised only because the device does not support the
    /// metric (see [DashboardSummary.unsupportedTitles]). Empty outside edit
    /// mode.
    required Set<String> unsupportedTitles,

    /// The add-tray: the removed rings and tiles, in layout order.
    required List<String> trayTitles,

    /// Today's activities, with the single-workout fallback already folded in.
    required List<ExerciseData> activities,
  }) = _DashboardDisplay;
}

/// Pure derivation from the loaded day + the saved layout to the display model.
/// No clock, no ref, no I/O — unit-testable with a fixture [DashboardData].
DashboardDisplay buildDashboardDisplay(
  DashboardData data,
  UnitFormatter f,
  AppLocalizations l10n, {
  required DashboardGoals goals,
  bool editing = false,
  List<String> tileOrder = const <String>[],
  List<String> ringOrder = const <String>[],
  Set<String> hiddenTiles = const <String>{},
}) {
  // Edit mode materialises a tile for every metric, device-supported or not, so
  // one the user removed can always be added back (Kotlin expands the spec list
  // to `DashboardWidgetId.entries` while editing).
  final summary = buildDashboardSummary(
    data,
    f,
    l10n,
    goals: goals,
    includeUnsupported: editing,
  );
  // Flutter's layout is a deny-list (hiddenTiles) where Kotlin's is an
  // allow-list, so a freshly-materialised unsupported tile would default to
  // *visible* in the carousel. Treat one the user has never placed as hidden:
  // it belongs in the add-tray until they choose it.
  final hidden = <String>{
    ...hiddenTiles,
    if (editing)
      for (final title in summary.unsupportedTitles)
        if (!tileOrder.contains(title)) title,
  };
  // All data-present tiles in the user's saved order (hidden included, for the
  // edit grid); the carousel shows only the non-hidden subset.
  final orderedTiles = applyDashboardTileLayout(
    summary.tiles,
    order: tileOrder,
    includeHidden: true,
  );
  // Hero rings share the same edit mode + hidden set; only their order is
  // stored separately (they render in their own top row).
  final orderedRings = applyDashboardLayout(
    <RingCardData>[summary.steps, summary.weeklyCardio],
    (r) => r.title,
    order: ringOrder,
    includeHidden: true,
  );
  return DashboardDisplay(
    orderedTiles: orderedTiles,
    visibleTiles: [
      for (final t in orderedTiles)
        if (!hidden.contains(t.title)) t,
    ],
    orderedRings: orderedRings,
    visibleRings: [
      for (final r in orderedRings)
        if (!hidden.contains(r.title)) r,
    ],
    hiddenTitles: hidden,
    unsupportedTitles: summary.unsupportedTitles,
    trayTitles: [
      for (final r in orderedRings)
        if (hidden.contains(r.title)) r.title,
      for (final t in orderedTiles)
        if (hidden.contains(t.title)) t.title,
    ],
    activities: data.workouts.isNotEmpty
        ? data.workouts
        : (data.workout != null
            ? <ExerciseData>[data.workout!]
            : const <ExerciseData>[]),
  );
}

/// Presentation data for one of the two hero ring cards (Steps, Weekly cardio).
/// Port of the Kotlin `DashboardSummaryCard` inputs.
class RingCardData {
  const RingCardData({
    required this.title,
    required this.value,
    required this.accent,
    required this.progress,
    required this.location,
    this.subtitle,
  });

  final String title;
  final String value;
  final String? subtitle;
  final Color accent;
  final double progress;
  final String location;
}

/// Presentation data for one small metric stat tile in the carousel. Port of the
/// Kotlin `MetricStatCard` / `DashboardPresentationMapper` widget models.
class StatTileData {
  const StatTileData({
    required this.title,
    required this.value,
    required this.icon,
    required this.accent,
    required this.location,
    this.unit,
    this.subtitle,
    this.message,
    this.showTitle = true,
    this.progress,
  });

  final String title;
  final String value;
  final String? unit;
  final IconData icon;
  final Color accent;
  final String? subtitle;
  final String? message;
  final bool showTitle;
  final double? progress;
  final String location;
}

/// The fully-mapped dashboard summary: the two fixed ring cards plus the ordered
/// list of carousel stat tiles.
class DashboardSummary {
  const DashboardSummary({
    required this.steps,
    required this.weeklyCardio,
    required this.tiles,
    this.unsupportedTitles = const <String>{},
  });

  final RingCardData steps;
  final RingCardData weeklyCardio;
  final List<StatTileData> tiles;

  /// The titles of tiles that were only materialised because
  /// `buildDashboardSummary(includeUnsupported: true)` was asked for — i.e. the
  /// device does not support the metric. Always empty otherwise.
  final Set<String> unsupportedTitles;
}

/// The user's daily goals, driving every ring/tile progress fraction and the
/// "x of y" subtitles on the summary.
///
/// These MUST come from [PreferencesRepository]. This class used to hold fixed
/// constants, with a comment claiming "the Flutter port has no per-user goal
/// store yet" — which stopped being true once the goal store landed. The result
/// was that the dashboard silently ignored every customised goal (a 6,000-step
/// goal still read "of 8,000" and filled the ring against 8,000) while the
/// detail screens, which do read the store, showed the right number. Two
/// screens, two answers, no error.
class DashboardGoals {
  const DashboardGoals({
    required this.steps,
    required this.distanceMeters,
    required this.caloriesOutKcal,
    required this.activeCaloriesKcal,
    required this.floors,
    required this.elevationMeters,
    required this.wheelchairPushes,
    required this.sleepHours,
    required this.hydrationLiters,
    required this.caloriesInKcal,
    required this.proteinGrams,
    required this.carbsGrams,
    required this.fatGrams,
    required this.mindfulnessMinutes,
  });

  /// Reads every goal the summary needs. Hydration has its OWN preference key
  /// (`hydration_daily_goal_liters`), separate from [MetricDailyGoalKey].
  factory DashboardGoals.fromPreferences(PreferencesRepository prefs) =>
      DashboardGoals(
        steps: prefs.dailyGoalFor(MetricDailyGoalKey.steps),
        distanceMeters: prefs.dailyGoalFor(MetricDailyGoalKey.distanceMeters),
        caloriesOutKcal: prefs.dailyGoalFor(MetricDailyGoalKey.caloriesOutKcal),
        activeCaloriesKcal:
            prefs.dailyGoalFor(MetricDailyGoalKey.activeCaloriesKcal),
        floors: prefs.dailyGoalFor(MetricDailyGoalKey.floors),
        elevationMeters: prefs.dailyGoalFor(MetricDailyGoalKey.elevationMeters),
        wheelchairPushes:
            prefs.dailyGoalFor(MetricDailyGoalKey.wheelchairPushes),
        sleepHours: prefs.dailyGoalFor(MetricDailyGoalKey.sleepHours),
        hydrationLiters: prefs.hydrationDailyGoalLiters,
        caloriesInKcal: prefs.dailyGoalFor(MetricDailyGoalKey.caloriesInKcal),
        proteinGrams: prefs.dailyGoalFor(MetricDailyGoalKey.proteinGrams),
        carbsGrams: prefs.dailyGoalFor(MetricDailyGoalKey.carbsGrams),
        fatGrams: prefs.dailyGoalFor(MetricDailyGoalKey.fatGrams),
        mindfulnessMinutes:
            prefs.dailyGoalFor(MetricDailyGoalKey.mindfulnessMinutes),
      );

  final double steps;
  final double distanceMeters;
  final double caloriesOutKcal;
  final double activeCaloriesKcal;
  final double floors;
  final double elevationMeters;
  final double wheelchairPushes;
  final double sleepHours;
  final double hydrationLiters;
  final double caloriesInKcal;
  final double proteinGrams;
  final double carbsGrams;
  final double fatGrams;
  final double mindfulnessMinutes;
}

/// The out-of-the-box goals -- the same defaults [MetricDailyGoalKey] carries.
///
/// For TESTS, and for a caller that genuinely has no preferences (there is none
/// in the app today). Production code must use [DashboardGoals.fromPreferences]:
/// reaching for this from a screen is exactly the bug that made the dashboard
/// tell a user with a 6,000-step goal that it was 8,000.
const DashboardGoals kDefaultDashboardGoals = DashboardGoals(
  steps: 8000,
  distanceMeters: 5000,
  caloriesOutKcal: 2000,
  activeCaloriesKcal: 400,
  floors: 10,
  elevationMeters: 100,
  wheelchairPushes: 1000,
  sleepHours: 8,
  hydrationLiters: 2,
  caloriesInKcal: 2000,
  proteinGrams: 50,
  carbsGrams: 275,
  fatGrams: 70,
  mindfulnessMinutes: 10,
);

double _fraction(double current, double target) =>
    target > 0 ? (current / target).clamp(0.0, 1.0) : 0.0;

/// Returns [value] only when it is a real, positive reading, else null.
double? _positive(double? value) => (value != null && value > 0) ? value : null;
int? _positiveInt(int? value) => (value != null && value > 0) ? value : null;

/// Maps a fully-loaded [DashboardData] onto the summary UI model, mirroring the
/// Kotlin `DashboardPresentationMapper` (metric selection, icons, accent, value
/// formatting and goal-progress).
///
/// Every metric the device supports produces a tile — an empty one carrying a
/// no-data message when the reading is absent or its permission is not granted
/// (the Kotlin `addOptionalMetric`). Only metrics the installed Health Connect
/// provider cannot serve are dropped, via [DashboardData.supportedMetrics]. The
/// required set (distance, hydration, body fat, heart rate, resting HR,
/// mindfulness) always shows its value, because zero is a real reading there.
///
/// [includeUnsupported] materialises a tile for *every* metric, device support
/// or not — the Kotlin `DashboardContent` edit-mode expansion to
/// `DashboardWidgetId.entries`. Without it a metric the provider cannot serve
/// gets no tile at all, so a user who removes one can never add it back. The
/// titles so materialised are reported in [DashboardSummary.unsupportedTitles];
/// the caller is expected to keep them out of the live carousel.
DashboardSummary buildDashboardSummary(
  DashboardData data,
  UnitFormatter f,
  AppLocalizations l10n, {
  required DashboardGoals goals,
  bool includeUnsupported = false,
}) {
  final unsupportedTitles = <String>{};
  final steps = RingCardData(
    title: 'Steps',
    value: f.count(data.steps),
    subtitle: 'steps of ${f.count(goals.steps.round())}',
    accent: AppColors.steps,
    progress: _fraction(data.steps.toDouble(), goals.steps),
    location: AppRoutes.metricLocation('STEPS'),
  );

  final wcl = data.weeklyCardioLoad;
  final weeklyCardio = wcl != null
      ? RingCardData(
          title: 'Weekly cardio',
          value: '${wcl.progressPercent}%',
          subtitle: '${wcl.currentScore} of ${wcl.targetScore}',
          accent: AppColors.workout,
          progress: wcl.progressFraction,
          location: AppRoutes.cardioLoadDetail,
        )
      : const RingCardData(
          title: 'Weekly cardio',
          value: '—',
          subtitle: 'No data',
          accent: AppColors.workout,
          progress: 0,
          location: AppRoutes.cardioLoadDetail,
        );

  final tiles = <StatTileData>[];

  /// Emits a tile for [metric] whenever the device supports it. A null [value]
  /// renders the tile empty, with [noDataMessage] (or the generic "No data") in
  /// place of the value — ports the Kotlin `addOptionalMetric`.
  void add(
    DashboardMetric metric, {
    required String title,
    required IconData icon,
    required Color accent,
    required String location,
    String? value,
    String? unit,
    String? subtitle,
    String? noDataMessage,
    bool showTitle = true,
    double? progress,
  }) {
    final supported = data.supportedMetrics.contains(metric);
    if (!includeUnsupported && !supported) return;
    if (!supported) unsupportedTitles.add(title);
    final empty = value == null;
    tiles.add(StatTileData(
      title: title,
      value: value ?? '',
      unit: empty ? null : unit,
      icon: icon,
      accent: accent,
      subtitle: empty ? null : subtitle,
      message: empty ? (noDataMessage ?? l10n.noData) : null,
      showTitle: showTitle,
      progress: empty ? null : progress,
      location: location,
    ));
  }

  /// A required metric (Kotlin `dashboardRequiredMetricWidgets`): a zero reading
  /// is a real value, so these never fall back to a no-data message.
  void addRequired(
    DashboardMetric metric, {
    required String title,
    required IconData icon,
    required Color accent,
    required String location,
    required String value,
    String? unit,
    double? progress,
  }) =>
      add(
        metric,
        title: title,
        icon: icon,
        accent: accent,
        location: location,
        value: value,
        unit: unit,
        progress: progress,
      );

  // ── Activity ──────────────────────────────────────────────────────────────
  final distance = f.distance(data.distanceMeters);
  addRequired(
    DashboardMetric.distance,
    title: 'Distance',
    value: distance.value,
    unit: distance.unit,
    icon: Icons.straighten,
    accent: AppColors.distance,
    progress: _fraction(data.distanceMeters, goals.distanceMeters),
    location: AppRoutes.metricLocation('DISTANCE'),
  );

  final caloriesOut = _positive(data.caloriesKcal);
  final caloriesOutValue = caloriesOut == null ? null : f.energy(caloriesOut);
  add(
    DashboardMetric.caloriesOut,
    title: 'Total calories',
    value: caloriesOutValue?.value,
    unit: caloriesOutValue?.unit,
    icon: Icons.local_fire_department,
    accent: AppColors.calories,
    progress: _fraction(data.caloriesKcal, goals.caloriesOutKcal),
    location: AppRoutes.calories,
  );

  final activeCalories = _positive(data.activeCaloriesKcal);
  final activeValue = activeCalories == null ? null : f.energy(activeCalories);
  add(
    DashboardMetric.activeCalories,
    title: 'Active calories',
    value: activeValue?.value,
    unit: activeValue?.unit,
    icon: Icons.local_fire_department,
    accent: AppColors.activeCalories,
    progress: _fraction(activeCalories ?? 0, goals.activeCaloriesKcal),
    location: AppRoutes.calories,
  );

  final floors = _positiveInt(data.floorsClimbed);
  add(
    DashboardMetric.floors,
    title: 'Floors',
    value: floors == null ? null : f.count(floors),
    icon: Icons.stairs,
    accent: AppColors.floors,
    progress: _fraction((floors ?? 0).toDouble(), goals.floors),
    location: AppRoutes.metricLocation('FLOORS'),
  );

  final elevation = _positive(data.elevationGainedMeters);
  final elevationValue = elevation == null ? null : f.elevation(elevation);
  add(
    DashboardMetric.elevation,
    title: 'Elevation',
    value: elevationValue?.value,
    unit: elevationValue?.unit,
    icon: Icons.terrain,
    accent: AppColors.elevation,
    progress: _fraction(elevation ?? 0, goals.elevationMeters),
    location: AppRoutes.metricLocation('ELEVATION'),
  );

  final pushes = _positiveInt(data.wheelchairPushes);
  add(
    DashboardMetric.wheelchairPushes,
    title: 'Wheelchair pushes',
    value: pushes == null ? null : f.count(pushes),
    icon: Icons.accessible_forward,
    accent: AppColors.wheelchairPushes,
    progress: _fraction((pushes ?? 0).toDouble(), goals.wheelchairPushes),
    location: AppRoutes.metricLocation('WHEELCHAIR_PUSHES'),
  );

  // ── Sleep ─────────────────────────────────────────────────────────────────
  final sleep = data.sleep;
  final sleepScore = data.sleepScore;
  add(
    DashboardMetric.sleep,
    title: 'Sleep',
    value: sleep == null ? null : f.duration(sleep.durationMs),
    icon: Icons.bed,
    accent: AppColors.sleep,
    subtitle: sleepScore.confidence != SleepScoreConfidence.noData
        ? '${sleepScore.score} · ${_sleepRating(sleepScore.score)}'
        : null,
    noDataMessage: l10n.messageNoSleepDay,
    showTitle: false,
    progress: _fraction(
      (sleep?.durationMs ?? 0).toDouble(),
      goals.sleepHours * 3600 * 1000,
    ),
    location: AppRoutes.sleep,
  );

  // ── Body Energy ───────────────────────────────────────────────────────────
  // Optional derived tile (Kotlin `optionalMetricWidget` for BODY_ENERGY). The
  // loader only populates the timeline once calibration is complete and the
  // heart-rate read is granted, so a present timeline means "set up": the tile
  // shows the current battery score with a "Start X  +charged / -drained"
  // subtitle. When it is absent the tile carries the "Not set up" message
  // (Kotlin `isNotSetUp`), tapping through to the Body Energy detail screen.
  final bodyEnergy = data.bodyEnergyTimeline;
  add(
    DashboardMetric.bodyEnergy,
    title: 'Body Energy',
    value: bodyEnergy == null ? null : f.count(bodyEnergy.currentScore),
    subtitle: bodyEnergy == null
        ? null
        : 'Start ${bodyEnergy.startScore}  '
            '+${bodyEnergy.charged} / -${bodyEnergy.drained}',
    icon: Icons.battery_charging_full,
    accent: AppColors.workout,
    noDataMessage: l10n.bodyEnergyNotSetUp,
    location: AppRoutes.bodyEnergyDetailsLocation(data.date.toString()),
  );

  // ── Hydration ─────────────────────────────────────────────────────────────
  final hydration = f.hydration(data.hydrationLiters);
  addRequired(
    DashboardMetric.hydration,
    title: 'Beverages',
    value: hydration.value,
    unit: hydration.unit,
    icon: Icons.local_drink,
    accent: AppColors.hydration,
    progress: _fraction(data.hydrationLiters, goals.hydrationLiters),
    location: AppRoutes.metricLocation('HYDRATION'),
  );

  // ── Nutrition ─────────────────────────────────────────────────────────────
  final caloriesIn = _positive(data.caloriesInKcal);
  final caloriesInValue = caloriesIn == null ? null : f.energy(caloriesIn);
  add(
    DashboardMetric.caloriesIn,
    title: 'Calories in',
    value: caloriesInValue?.value,
    unit: caloriesInValue?.unit,
    icon: Icons.restaurant,
    accent: AppColors.nutrition,
    progress: _fraction(caloriesIn ?? 0, goals.caloriesInKcal),
    location: AppRoutes.nutrition,
  );

  void addGrams(DashboardMetric metric, String title, double? grams, double goal) {
    final value = _positive(grams);
    add(
      metric,
      title: title,
      value: value == null ? null : f.count(value.round()),
      unit: 'g',
      icon: Icons.restaurant,
      accent: AppColors.nutrition,
      progress: _fraction(value ?? 0, goal),
      location: AppRoutes.nutrition,
    );
  }

  addGrams(DashboardMetric.protein, 'Protein', data.proteinGrams, goals.proteinGrams);
  addGrams(DashboardMetric.carbs, 'Carbs', data.carbsGrams, goals.carbsGrams);
  addGrams(DashboardMetric.fat, 'Fat', data.fatGrams, goals.fatGrams);

  final caffeine = _positive(data.caffeineGrams);
  add(
    DashboardMetric.caffeine,
    title: 'Caffeine',
    value: caffeine == null ? null : f.decimal(caffeine * 1000, 0),
    unit: 'mg',
    icon: Icons.restaurant,
    accent: AppColors.nutrition,
    // Kotlin routes the caffeine tile to its own analytics screen, not the
    // nutrition overview.
    location: AppRoutes.metricLocation('CAFFEINE'),
  );

  // ── Body ──────────────────────────────────────────────────────────────────
  final weight = _positive(data.weightKg);
  final weightValue = weight == null ? null : f.weight(weight);
  add(
    DashboardMetric.weight,
    title: 'Weight',
    value: weightValue?.value,
    unit: weightValue?.unit,
    icon: Icons.monitor_weight,
    accent: AppColors.weight,
    location: AppRoutes.body,
  );

  final height = _positive(data.heightCm);
  final heightValue = height == null ? null : f.height(height);
  add(
    DashboardMetric.height,
    title: 'Height',
    value: heightValue?.value,
    unit: heightValue?.unit,
    icon: Icons.monitor_weight,
    accent: AppColors.weight,
    location: AppRoutes.body,
  );

  final bmi = _positive(data.bmi);
  add(
    DashboardMetric.bmi,
    title: 'BMI',
    value: bmi == null ? null : f.decimal(bmi, 1),
    icon: Icons.monitor_weight,
    accent: AppColors.weight,
    location: AppRoutes.body,
  );

  final ffmi = _positive(data.ffmi);
  add(
    DashboardMetric.ffmi,
    title: 'FFMI',
    value: ffmi == null ? null : f.decimal(ffmi, 1),
    icon: Icons.fitness_center,
    accent: AppColors.bodyFat,
    location: AppRoutes.body,
  );

  final bodyFat = f.percent(data.bodyFatPercent);
  addRequired(
    DashboardMetric.bodyFat,
    title: 'Body fat',
    value: bodyFat.value,
    unit: bodyFat.unit,
    icon: Icons.monitor_weight,
    accent: AppColors.bodyFat,
    location: AppRoutes.body,
  );

  final leanMass = _positive(data.leanMassKg);
  final leanValue = leanMass == null ? null : f.bodyMass(leanMass);
  add(
    DashboardMetric.leanMass,
    title: 'Lean mass',
    value: leanValue?.value,
    unit: leanValue?.unit,
    icon: Icons.monitor_weight,
    accent: AppColors.weight,
    location: AppRoutes.body,
  );

  final bmr = _positive(data.bmrKcal);
  final bmrValue = bmr == null ? null : f.energy(bmr);
  add(
    DashboardMetric.bmr,
    title: 'BMR',
    value: bmrValue?.value,
    unit: bmrValue?.unit,
    icon: Icons.local_fire_department,
    accent: AppColors.calories,
    location: AppRoutes.body,
  );

  final boneMass = _positive(data.boneMassKg);
  final boneValue = boneMass == null ? null : f.bodyMass(boneMass);
  add(
    DashboardMetric.boneMass,
    title: 'Bone mass',
    value: boneValue?.value,
    unit: boneValue?.unit,
    icon: Icons.monitor_weight,
    accent: AppColors.weight,
    location: AppRoutes.body,
  );

  final bodyWater = _positive(data.bodyWaterMassKg);
  final bodyWaterValue = bodyWater == null ? null : f.bodyMass(bodyWater);
  add(
    DashboardMetric.bodyWaterMass,
    title: 'Body water',
    value: bodyWaterValue?.value,
    unit: bodyWaterValue?.unit,
    icon: Icons.water_drop,
    accent: AppColors.hydration,
    location: AppRoutes.body,
  );

  // ── Heart & vitals ────────────────────────────────────────────────────────
  final avgHr = f.heartRate(data.avgHeartRateBpm);
  addRequired(
    DashboardMetric.avgHeartRate,
    title: 'Heart rate',
    value: avgHr.value,
    unit: avgHr.unit,
    icon: Icons.favorite,
    accent: AppColors.vitals,
    location: AppRoutes.heartVitals,
  );

  final restingHr = f.heartRate(data.restingHeartRateBpm);
  addRequired(
    DashboardMetric.restingHeartRate,
    title: 'Resting HR',
    value: restingHr.value,
    unit: restingHr.unit,
    icon: Icons.favorite,
    accent: AppColors.vitals,
    location: AppRoutes.heartVitals,
  );

  final hrv = _positive(data.hrvRmssdMs);
  final hrvValue = hrv == null ? null : f.hrv(hrv);
  add(
    DashboardMetric.hrv,
    title: 'HRV',
    value: hrvValue?.value,
    unit: hrvValue?.unit,
    icon: Icons.favorite_border,
    accent: AppColors.vitals,
    location: AppRoutes.heartVitals,
  );

  final systolic = data.latestSystolicMmHg;
  final diastolic = data.latestDiastolicMmHg;
  final bp = systolic != null && diastolic != null
      ? f.bloodPressure(systolic, diastolic)
      : null;
  add(
    DashboardMetric.bloodPressure,
    title: 'Blood pressure',
    value: bp?.value,
    unit: bp?.unit,
    icon: Icons.favorite,
    accent: AppColors.vitals,
    noDataMessage: l10n.messageNoBloodPressure,
    location: AppRoutes.heartVitals,
  );

  final spo2 = data.latestSpO2Percent;
  final spo2Value = spo2 == null ? null : f.percent(spo2);
  add(
    DashboardMetric.spo2,
    title: 'Blood oxygen',
    value: spo2Value?.value,
    unit: spo2Value?.unit,
    icon: Icons.favorite_border,
    accent: AppColors.vitals,
    noDataMessage: l10n.messageNoOxygen,
    location: AppRoutes.heartVitals,
  );

  final vo2 = data.latestVo2Max;
  final vo2Value = vo2 == null ? null : f.vo2Max(vo2);
  add(
    DashboardMetric.vo2Max,
    title: 'VO₂ max',
    value: vo2Value?.value,
    unit: vo2Value?.unit,
    icon: Icons.directions_run,
    accent: AppColors.vitals,
    noDataMessage: l10n.messageNoVo2Max,
    location: AppRoutes.heartVitals,
  );

  final respiratory = _positive(data.avgRespiratoryRate);
  final respiratoryValue =
      respiratory == null ? null : f.respiratoryRate(respiratory);
  add(
    DashboardMetric.respiratoryRate,
    title: 'Respiratory rate',
    value: respiratoryValue?.value,
    unit: respiratoryValue?.unit,
    icon: Icons.favorite,
    accent: AppColors.vitals,
    location: AppRoutes.heartVitals,
  );

  final bodyTemp = data.latestBodyTemperatureCelsius;
  final bodyTempValue = bodyTemp == null ? null : f.temperature(bodyTemp);
  add(
    DashboardMetric.bodyTemperature,
    title: 'Body temperature',
    value: bodyTempValue?.value,
    unit: bodyTempValue?.unit,
    icon: Icons.device_thermostat,
    accent: AppColors.vitals,
    location: AppRoutes.heartVitals,
  );

  final glucose = data.latestBloodGlucoseMillimolesPerLiter;
  final glucoseValue = glucose == null ? null : f.bloodGlucose(glucose);
  add(
    DashboardMetric.bloodGlucose,
    title: 'Blood glucose',
    value: glucoseValue?.value,
    unit: glucoseValue?.unit,
    icon: Icons.water_drop,
    accent: AppColors.vitals,
    noDataMessage: l10n.messageNoBloodGlucose,
    location: AppRoutes.heartVitals,
  );

  final skinTemp = data.latestSkinTemperatureDeltaCelsius;
  final skinTempValue = skinTemp == null ? null : f.temperatureDelta(skinTemp);
  add(
    DashboardMetric.skinTemperature,
    title: 'Skin temperature',
    value: skinTempValue?.value,
    unit: skinTempValue?.unit,
    icon: Icons.device_thermostat,
    accent: AppColors.vitals,
    noDataMessage: l10n.messageNoSkinTemperature,
    location: AppRoutes.heartVitals,
  );

  // ── Mindfulness (required tile) ───────────────────────────────────────────
  final mindfulness = f.minutes(data.mindfulnessMinutes ?? 0);
  addRequired(
    DashboardMetric.mindfulness,
    title: 'Mindfulness',
    value: mindfulness.value,
    unit: mindfulness.unit,
    icon: Icons.self_improvement,
    accent: AppColors.mindfulness,
    progress: _fraction(
      (data.mindfulnessMinutes ?? 0).toDouble(),
      goals.mindfulnessMinutes,
    ),
    location: AppRoutes.metricLocation('MINDFULNESS'),
  );

  // ── Cycle ─────────────────────────────────────────────────────────────────
  final periodDays = _positiveInt(data.menstruationPeriodDays);
  add(
    DashboardMetric.cycle,
    title: 'Cycle',
    value: periodDays == null ? null : f.count(periodDays),
    unit: 'days',
    icon: Icons.favorite_border,
    accent: AppColors.cycle,
    noDataMessage: l10n.messageCycleBrowse,
    location: AppRoutes.metricLocation(DashboardMetricId.cycle.storageName),
  );

  return DashboardSummary(
    steps: steps,
    weeklyCardio: weeklyCardio,
    tiles: tiles,
    unsupportedTitles: unsupportedTitles,
  );
}

/// Applies the user's saved dashboard layout to [items]: reorders by [order]
/// (items whose key appears in [order] come first, in that order; the rest keep
/// their default relative order, appended), and — unless [includeHidden] — drops
/// items whose key is in [hidden]. Items are keyed by [keyOf] (a unique title).
/// Unknown/new items gracefully fall to the end in default order.
List<T> applyDashboardLayout<T>(
  List<T> items,
  String Function(T) keyOf, {
  List<String> order = const <String>[],
  Set<String> hidden = const <String>{},
  bool includeHidden = false,
}) {
  final orderIndex = <String, int>{
    for (var i = 0; i < order.length; i++) order[i]: i,
  };
  final kept = includeHidden
      ? List<T>.of(items)
      : [for (final it in items) if (!hidden.contains(keyOf(it))) it];
  // Pair each with its default index so unknown items keep a stable order.
  final indexed = [for (var i = 0; i < kept.length; i++) (i, kept[i])];
  indexed.sort((a, b) {
    final ai = orderIndex[keyOf(a.$2)] ?? (order.length + a.$1);
    final bi = orderIndex[keyOf(b.$2)] ?? (order.length + b.$1);
    return ai.compareTo(bi);
  });
  return [for (final e in indexed) e.$2];
}

/// Metric-tile specialization of [applyDashboardLayout], keyed by tile title.
List<StatTileData> applyDashboardTileLayout(
  List<StatTileData> tiles, {
  List<String> order = const <String>[],
  Set<String> hidden = const <String>{},
  bool includeHidden = false,
}) =>
    applyDashboardLayout(
      tiles,
      (t) => t.title,
      order: order,
      hidden: hidden,
      includeHidden: includeHidden,
    );

/// Sleep-score rating label, from the Kotlin `sleepScoreRatingLabel(score)`.
String _sleepRating(int score) {
  if (score >= 90) return 'Excellent';
  if (score >= 80) return 'Good';
  if (score >= 60) return 'Fair';
  return 'Poor';
}
