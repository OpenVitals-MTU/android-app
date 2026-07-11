// The dashboard summary must read the USER's daily goals, not the defaults.
//
// It used to hardcode them: a 6,000-step goal still read "steps of 8,000" on the
// summary and filled the ring against 8,000, while the metric detail screen --
// which does read the goal store -- showed 6,000. Two screens, two answers, and
// no error anywhere. These tests pin the whole set, because the same class held
// fixed constants for all fourteen metrics, not just steps.

import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/domain/insights/daily_goals.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/dashboard/dashboard_summary_presentation.dart';
import 'package:openvitals/l10n/app_localizations.dart';

Future<PreferencesRepository> prefsWith(Map<String, Object> values) async {
  SharedPreferences.setMockInitialValues(values);
  return PreferencesRepository(await SharedPreferences.getInstance());
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late AppLocalizations l10n;
  setUpAll(() async {
    l10n = await AppLocalizations.delegate.load(const Locale('en'));
  });

  DashboardSummary summaryWith(DashboardGoals goals, {int steps = 3000}) =>
      buildDashboardSummary(
        DashboardData(
          date: LocalDate(2026, 1, 2),
          steps: steps,
          supportedMetrics: DashboardMetric.values.toSet(),
        ),
        UnitFormatter(unitSystemProvider: () => UnitSystem.metric),
        l10n,
        goals: goals,
      );

  test('the steps ring uses the user\'s goal, not the 8,000 default', () async {
    // The reported bug, exactly: goal set to 6,000, summary said 8,000.
    final prefs = await prefsWith(<String, Object>{'flutter.goal_steps': 6000.0});
    expect(
      prefs.dailyGoalFor(MetricDailyGoalKey.steps),
      6000.0,
      reason: 'premise: the goal store really does hold 6,000',
    );

    final summary = summaryWith(DashboardGoals.fromPreferences(prefs));

    expect(summary.steps.subtitle, 'steps of 6,000');
    // And the ring FILLS against 6,000, not 8,000 -- the subtitle being right
    // while the progress is wrong would be a worse bug, not a better one.
    expect(summary.steps.progress, closeTo(3000 / 6000, 1e-9));
  });

  test('every goal the summary shows comes from preferences', () async {
    // The bug was never only about steps: one class held fixed constants for all
    // fourteen metrics. If any of these still reads a default, that metric's ring
    // is lying to the same user in the same way.
    final prefs = await prefsWith(<String, Object>{
      'flutter.goal_steps': 6000.0,
      'flutter.goal_distance_meters': 3000.0,
      'flutter.goal_calories_out_kcal': 2500.0,
      'flutter.goal_active_calories_kcal': 600.0,
      'flutter.goal_floors': 20.0,
      'flutter.goal_elevation_meters': 250.0,
      'flutter.goal_wheelchair_pushes': 2000.0,
      'flutter.goal_sleep_hours': 7.0,
      'flutter.goal_calories_in_kcal': 2200.0,
      'flutter.goal_protein_grams': 120.0,
      'flutter.goal_carbs_grams': 300.0,
      'flutter.goal_fat_grams': 80.0,
      'flutter.goal_mindfulness_minutes': 20.0,
      'flutter.hydration_daily_goal_liters': 3.0,
    });

    final goals = DashboardGoals.fromPreferences(prefs);

    expect(goals.steps, 6000.0);
    expect(goals.distanceMeters, 3000.0);
    expect(goals.caloriesOutKcal, 2500.0);
    expect(goals.activeCaloriesKcal, 600.0);
    expect(goals.floors, 20.0);
    expect(goals.elevationMeters, 250.0);
    expect(goals.wheelchairPushes, 2000.0);
    expect(goals.sleepHours, 7.0);
    expect(goals.caloriesInKcal, 2200.0);
    expect(goals.proteinGrams, 120.0);
    expect(goals.carbsGrams, 300.0);
    expect(goals.fatGrams, 80.0);
    expect(goals.mindfulnessMinutes, 20.0);
    // Hydration is the odd one out: its own preference key, NOT a
    // MetricDailyGoalKey. Reading it from the wrong place is a silent default.
    expect(goals.hydrationLiters, 3.0);

    // None of them may equal the out-of-the-box value, or the assertion above
    // could pass on a default that happens to match.
    expect(goals.steps, isNot(kDefaultDashboardGoals.steps));
    expect(goals.hydrationLiters, isNot(kDefaultDashboardGoals.hydrationLiters));
  });

  test('an untouched install still gets the documented defaults', () async {
    final prefs = await prefsWith(<String, Object>{});
    final goals = DashboardGoals.fromPreferences(prefs);

    expect(goals.steps, kDefaultDashboardGoals.steps);
    expect(goals.sleepHours, kDefaultDashboardGoals.sleepHours);
    expect(goals.hydrationLiters, kDefaultDashboardGoals.hydrationLiters);
  });
}
