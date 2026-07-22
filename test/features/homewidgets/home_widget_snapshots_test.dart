import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/insights/cardio_load.dart';
import 'package:openvitals/domain/insights/daily_readiness.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/homewidgets/home_widget_service.dart';
import 'package:openvitals/features/homewidgets/home_widget_snapshots.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// Readiness reports UNKNOWN until at least one signal is loaded; a resting
/// heart rate with a baseline is the cheapest way to get a real score.
DashboardData _dataWithReadiness({
  int restingHeartRateBpm = 52,
  int baselineBpm = 54,
}) =>
    DashboardData(
      date: LocalDate(2026, 7, 10),
      restingHeartRateBpm: restingHeartRateBpm,
      restingHeartRateBaselineBpm: baselineBpm,
      loadedMetrics: const {DashboardMetric.restingHeartRate},
    );

BodyEnergyTimeline _timeline({
  required int currentScore,
  int startScore = 70,
  int charged = 30,
  int drained = 12,
  List<int> scores = const [],
}) =>
    BodyEnergyTimeline(
      date: LocalDate(2026, 7, 10),
      startScore: startScore,
      currentScore: currentScore,
      charged: charged,
      drained: drained,
      points: [
        for (var i = 0; i < scores.length; i++)
          BodyEnergyTimelinePoint(
            time: DateTime(2026, 7, 10).add(Duration(minutes: 5 * i)),
            score: scores[i],
            delta: 0,
            state: BodyEnergyBucketState.rest,
            confidence: BodyEnergyConfidence.high,
            charge: 0,
            intensityDrain: 0,
            activityEnergyDrain: 0,
            basalDrain: 0,
            stressDrain: 0,
            recoveryDebtDrain: 0,
            primaryInfluence: BodyEnergyPrimaryInfluence.steady,
          ),
      ],
      confidence: BodyEnergyConfidence.high,
      confidenceReason: 'test',
    );

const DashboardWeeklyCardioLoad _cardioLoad = DashboardWeeklyCardioLoad(
  currentScore: 180,
  targetScore: 300,
  todayScore: 20,
  confidence: CardioLoadConfidence.high,
  targetSource: DashboardWeeklyCardioLoadTargetSource.recentHistory,
);

void main() {
  late AppLocalizations l10n;
  late UnitFormatter formatter;

  setUpAll(() async {
    l10n = await AppLocalizations.delegate.load(const Locale('en'));
    formatter =
        UnitFormatter(unitSystemProvider: () => UnitSystem.metric);
  });

  group('buildDailyReadinessSnapshot', () {
    test('reports the score, status and recommendation', () {
      final data = _dataWithReadiness();

      final snapshot = buildDailyReadinessSnapshot(data, l10n);
      final insight = calculateDailyReadiness(data);

      expect(snapshot.title, l10n.screenDailyReadiness);
      expect(snapshot.value, '${insight.score}');
      expect(snapshot.unit, '');
      expect(snapshot.subtitle, insight.statusTitle);
      expect(snapshot.route, 'daily_readiness');
      expect(snapshot.rows.single.label, l10n.dashboardReadinessRecommended);
      expect(snapshot.rows.single.value, insight.recommendation);
    });

    test('falls back to "--" when no signal is loaded (UNKNOWN)', () {
      // Nothing loaded → availableSignals == 0 → ReadinessState.unknown.
      final snapshot = buildDailyReadinessSnapshot(
        DashboardData(date: LocalDate(2026, 7, 10)),
        l10n,
      );

      expect(snapshot.value, '--');
      expect(snapshot.subtitle, l10n.homeMetricWidgetOpenForDetails);
      // Still routes into the app, where the user can see why it is empty.
      expect(snapshot.route, 'daily_readiness');
      expect(snapshot.rows, isEmpty);
    });

    test('honours the caller\'s goals', () {
      final data = DashboardData(
        date: LocalDate(2026, 7, 10),
        hydrationLiters: 0.2,
        loadedMetrics: const {DashboardMetric.hydration},
      );

      // A 4 L goal leaves 0.2 L far behind; a 0.25 L goal does not.
      final behind = buildDailyReadinessSnapshot(
        data,
        l10n,
        goals: const DailyReadinessGoalInputs(hydrationLitersGoal: 4),
      );
      final met = buildDailyReadinessSnapshot(
        data,
        l10n,
        goals: const DailyReadinessGoalInputs(hydrationLitersGoal: 0.25),
      );

      expect(int.parse(behind.value), lessThan(int.parse(met.value)));
    });
  });

  group('buildBodyEnergySnapshot', () {
    test('reports the score with start/charged/drained rows', () {
      final snapshot = buildBodyEnergySnapshot(
        DashboardData(
          date: LocalDate(2026, 7, 10),
          bodyEnergyTimeline: _timeline(currentScore: 88),
        ),
        l10n,
      );

      expect(snapshot.title, l10n.screenBodyEnergy);
      expect(snapshot.value, '88');
      expect(snapshot.subtitle, l10n.homeWidgetBodyEnergyCharged);
      expect(snapshot.route, 'daily_readiness/body_energy/2026-07-10');
      expect(
        snapshot.rows.map((r) => (r.label, r.value)),
        [
          (l10n.bodyEnergyTimelineStart, '70'),
          (l10n.bodyEnergyTimelineCharged, '+30'),
          (l10n.bodyEnergyTimelineDrained, '-12'),
        ],
      );
    });

    test('maps every status threshold', () {
      String statusFor(int score) => buildBodyEnergySnapshot(
            DashboardData(
              date: LocalDate(2026, 7, 10),
              bodyEnergyTimeline: _timeline(currentScore: score),
            ),
            l10n,
          ).subtitle;

      expect(statusFor(80), l10n.homeWidgetBodyEnergyCharged);
      expect(statusFor(79), l10n.homeWidgetBodyEnergySteady);
      expect(statusFor(60), l10n.homeWidgetBodyEnergySteady);
      expect(statusFor(59), l10n.homeWidgetBodyEnergyLimited);
      expect(statusFor(40), l10n.homeWidgetBodyEnergyLimited);
      expect(statusFor(39), l10n.homeWidgetBodyEnergyLow);
      expect(statusFor(0), l10n.homeWidgetBodyEnergyLow);
    });

    test('carries the day as a series the widget can plot', () {
      final snapshot = buildBodyEnergySnapshot(
        DashboardData(
          date: LocalDate(2026, 7, 10),
          bodyEnergyTimeline:
              _timeline(currentScore: 55, scores: const [70, 62, 58, 55]),
        ),
        l10n,
      );

      expect(snapshot.series, [70, 62, 58, 55]);
    });

    test('a full day is thinned, and still ends where the number says', () {
      // Body Energy is computed in five-minute buckets — 288 of them — which is
      // more than the widget can draw and more than belongs in the shared
      // preferences file on every refresh.
      final day = [for (var i = 0; i < 288; i++) 100 - (i ~/ 6)];
      final snapshot = buildBodyEnergySnapshot(
        DashboardData(
          date: LocalDate(2026, 7, 10),
          bodyEnergyTimeline: _timeline(currentScore: day.last, scores: day),
        ),
        l10n,
      );

      expect(snapshot.series, hasLength(maxHomeWidgetSeriesPoints));
      expect(snapshot.series.first, day.first);
      // The last point IS the current score. A line ending anywhere else would
      // visibly disagree with the number printed beside it.
      expect(snapshot.series.last, day.last);
      expect(snapshot.value, '${day.last}');
    });

    test('falls back to "--" with no rows when the timeline is absent', () {
      final snapshot = buildBodyEnergySnapshot(
        DashboardData(date: LocalDate(2026, 7, 10)),
        l10n,
      );

      expect(snapshot.value, '--');
      expect(snapshot.subtitle, l10n.homeMetricWidgetOpenForDetails);
      expect(snapshot.route, 'daily_readiness/body_energy/2026-07-10');
      expect(snapshot.rows, isEmpty);
    });
  });

  group('buildTodayVitalsSnapshot', () {
    test('lists the rows in the Kotlin order, values joined with their unit',
        () {
      final data = _dataWithReadiness().copyWith(
        steps: 8432,
        distanceMeters: 6200,
        hydrationLiters: 1.5,
        hrvRmssdMs: 42.5,
        sleep: SleepData(
          id: 'sleep-1',
          startTime: DateTime(2026, 7, 9, 23),
          endTime: DateTime(2026, 7, 10, 6, 30),
          durationMs: 27000000,
          source: 'test',
        ),
        bodyEnergyTimeline: _timeline(currentScore: 64),
        weeklyCardioLoad: _cardioLoad,
      );

      final snapshot = buildTodayVitalsSnapshot(data, formatter, l10n);

      expect(snapshot.title, l10n.homeWidgetTodayTitle);
      expect(snapshot.value, '');
      expect(snapshot.route, 'dashboard');
      expect(
        snapshot.rows.map((r) => r.label),
        [
          l10n.screenDailyReadiness,
          l10n.screenBodyEnergy,
          l10n.metricSleep,
          l10n.metricSteps,
          l10n.metricDistance,
          l10n.metricRestingHeartRate,
          l10n.homeWidgetHrvShort,
          l10n.metricWeeklyCardioLoad,
          l10n.metricHydration,
        ],
      );

      final rows = {for (final row in snapshot.rows) row.label: row};
      expect(rows[l10n.screenBodyEnergy]!.value, '64');
      expect(rows[l10n.screenBodyEnergy]!.subtitle, '+30 / -12');
      expect(rows[l10n.metricSleep]!.value, '7h 30m');
      expect(rows[l10n.metricSteps]!.value, '8,432');
      expect(rows[l10n.metricDistance]!.value, '6.2 km');
      expect(rows[l10n.metricRestingHeartRate]!.value, '52 bpm');
      expect(rows[l10n.homeWidgetHrvShort]!.value, '42.5 ms');
      expect(rows[l10n.metricHydration]!.value, '1.50 L');
      // Weekly cardio keeps its own subtitle; the rest drop the "Today" one.
      expect(rows[l10n.metricWeeklyCardioLoad]!.value, '180 of 300');
      expect(
        rows[l10n.metricWeeklyCardioLoad]!.subtitle,
        l10n.dashboardCardioLoadPercent(60),
      );
      expect(rows[l10n.metricSteps]!.subtitle, '');
    });

    test('drops the readiness row and shows "No data" rows when empty', () {
      final snapshot = buildTodayVitalsSnapshot(
        DashboardData(date: LocalDate(2026, 7, 10)),
        formatter,
        l10n,
      );

      // Readiness is UNKNOWN with no signals: Kotlin omits the row entirely.
      expect(
        snapshot.rows.map((r) => r.label),
        isNot(contains(l10n.screenDailyReadiness)),
      );
      final bodyEnergy = snapshot.rows.first;
      expect(bodyEnergy.label, l10n.screenBodyEnergy);
      expect(bodyEnergy.value, '--');
      expect(bodyEnergy.subtitle, l10n.noData);

      final sleep = snapshot.rows[1];
      expect(sleep.value, '--');
      expect(sleep.subtitle, l10n.noData);
      // Steps and hydration read a real zero, so they are never "no data".
      final rows = {for (final row in snapshot.rows) row.label: row};
      expect(rows[l10n.metricSteps]!.value, '0');
      expect(rows[l10n.metricHydration]!.value, '--');
    });
  });

  group('buildMetricSnapshot', () {
    test('formats a metric with its unit and routes to the metric screen', () {
      final data = DashboardData(
        date: LocalDate(2026, 7, 10),
        distanceMeters: 6200,
      );

      final snapshot =
          buildMetricSnapshot(DashboardMetric.distance, data, formatter, l10n);

      expect(snapshot.title, l10n.metricDistance);
      expect(snapshot.value, '6.2');
      expect(snapshot.unit, 'km');
      expect(snapshot.subtitle, l10n.periodToday);
      expect(snapshot.route, 'metric/DISTANCE');
    });

    test('body energy routes to its dated detail screen, not /metric', () {
      final snapshot = buildMetricSnapshot(
        DashboardMetric.bodyEnergy,
        DashboardData(
          date: LocalDate(2026, 7, 10),
          bodyEnergyTimeline: _timeline(currentScore: 64),
        ),
        formatter,
        l10n,
      );

      expect(snapshot.value, '64');
      expect(snapshot.subtitle, '+30 / -12');
      expect(snapshot.route, 'daily_readiness/body_energy/2026-07-10');
    });

    test('reports "--" / "No data" for an absent reading', () {
      final snapshot = buildMetricSnapshot(
        DashboardMetric.hrv,
        DashboardData(date: LocalDate(2026, 7, 10)),
        formatter,
        l10n,
      );

      expect(snapshot.value, '--');
      expect(snapshot.unit, '');
      expect(snapshot.subtitle, l10n.noData);
      expect(snapshot.route, 'metric/HRV');
    });

    test('a missing permission wins over the reading', () {
      final snapshot = buildMetricSnapshot(
        DashboardMetric.steps,
        DashboardData(
          date: LocalDate(2026, 7, 10),
          steps: 8432,
          missingPermissions: const {'android.permission.health.READ_STEPS'},
        ),
        formatter,
        l10n,
      );

      expect(snapshot.value, '--');
      expect(snapshot.subtitle, l10n.homeMetricWidgetPermissionNeeded);
      expect(snapshot.route, 'metric/STEPS');
    });

    test('weekly cardio load reports progress and percent', () {
      final snapshot = buildMetricSnapshot(
        DashboardMetric.weeklyCardioLoad,
        DashboardData(
          date: LocalDate(2026, 7, 10),
          weeklyCardioLoad: _cardioLoad,
        ),
        formatter,
        l10n,
      );

      expect(snapshot.value, l10n.dashboardWeeklyCardioLoadProgress(180, 300));
      expect(snapshot.subtitle, l10n.dashboardCardioLoadPercent(60));
    });

    test('every catalog metric has a title, a route and a no-data snapshot', () {
      final empty = DashboardData(date: LocalDate(2026, 7, 10));
      for (final metric in homeMetricWidgetCatalog()) {
        final snapshot = buildMetricSnapshot(metric, empty, formatter, l10n);
        expect(snapshot.title, isNotEmpty, reason: metric.name);
        expect(snapshot.route, isNotEmpty, reason: metric.name);
        expect(snapshot.value, isNotEmpty, reason: metric.name);
      }
    });
  });

  group('homeMetricWidgetCatalog', () {
    test('drops caffeine and intensity minutes (the Kotlin catalog)', () {
      final catalog = homeMetricWidgetCatalog();

      expect(catalog, isNot(contains(DashboardMetric.caffeine)));
      expect(catalog, isNot(contains(DashboardMetric.intensityMinutes)));
      expect(catalog, contains(DashboardMetric.bodyEnergy));
      expect(catalog, contains(DashboardMetric.weeklyCardioLoad));
      expect(catalog, hasLength(DashboardMetric.values.length - 2));
    });
  });
}
