import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/dashboard/dashboard_summary_presentation.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// Builds the summary for [supported], with no readings for any metric beyond
/// the handful the fixture sets.
DashboardSummary _summaryFor(
  Set<DashboardMetric> supported,
  AppLocalizations l10n, {
  double? spo2,
  BodyEnergyTimeline? bodyEnergyTimeline,
  bool includeUnsupported = false,
}) =>
    buildDashboardSummary(
      DashboardData(
        date: LocalDate(2026, 1, 2),
        latestSpO2Percent: spo2,
        bodyEnergyTimeline: bodyEnergyTimeline,
        supportedMetrics: supported,
      ),
      UnitFormatter(unitSystemProvider: () => UnitSystem.metric),
      l10n,
      includeUnsupported: includeUnsupported,
    );

BodyEnergyTimeline _bodyEnergyTimeline() => BodyEnergyTimeline(
      date: LocalDate(2026, 1, 2),
      startScore: 60,
      currentScore: 74,
      charged: 30,
      drained: 16,
      points: const [],
      confidence: BodyEnergyConfidence.medium,
      confidenceReason: '',
    );

List<String> _titles(DashboardSummary s) => [for (final t in s.tiles) t.title];

StatTileData _tile(DashboardSummary s, String title) =>
    s.tiles.firstWhere((t) => t.title == title);

void main() {
  late AppLocalizations l10n;

  setUpAll(() async {
    l10n = await AppLocalizations.delegate.load(const Locale('en'));
  });

  test('a supported metric with no reading still gets an empty tile', () {
    final summary = _summaryFor({DashboardMetric.spo2}, l10n);

    expect(_titles(summary), contains('Blood oxygen'));
    final tile = _tile(summary, 'Blood oxygen');
    // Empty: the no-data message replaces the value, and no unit/progress trails.
    expect(tile.message, l10n.messageNoOxygen);
    expect(tile.value, isEmpty);
    expect(tile.unit, isNull);
    expect(tile.progress, isNull);
  });

  test('a supported metric with a reading renders its value, not a message', () {
    final summary = _summaryFor({DashboardMetric.spo2}, l10n, spo2: 97);

    final tile = _tile(summary, 'Blood oxygen');
    expect(tile.message, isNull);
    expect(tile.value, isNotEmpty);
  });

  test('an unsupported metric gets no tile at all', () {
    // Everything except blood oxygen.
    final supported = DashboardMetric.values.toSet()
      ..remove(DashboardMetric.spo2);
    final summary = _summaryFor(supported, l10n);

    expect(_titles(summary), isNot(contains('Blood oxygen')));
    // …but its supported neighbours are still there, empty.
    expect(_titles(summary), contains('VO₂ max'));
    expect(_tile(summary, 'VO₂ max').message, l10n.messageNoVo2Max);
  });

  test('required metrics show a zero reading rather than a no-data message', () {
    final summary = _summaryFor({
      DashboardMetric.distance,
      DashboardMetric.hydration,
      DashboardMetric.bodyFat,
      DashboardMetric.avgHeartRate,
      DashboardMetric.restingHeartRate,
      DashboardMetric.mindfulness,
    }, l10n);

    expect(summary.tiles, hasLength(6));
    for (final tile in summary.tiles) {
      expect(tile.message, isNull, reason: '${tile.title} should show its zero');
      expect(tile.value, isNotEmpty, reason: '${tile.title} needs a value');
    }
  });

  test('metrics the mapper previously dropped now appear when supported', () {
    // These had no tile in the mapper at all before device-support gating.
    final summary = _summaryFor(DashboardMetric.values.toSet(), l10n);
    expect(
      _titles(summary),
      containsAll(<String>[
        'Blood glucose',
        'Skin temperature',
        'BMR',
        'Bone mass',
        'Body water',
        'Cycle',
      ]),
    );
  });

  test('no tiles at all when the device supports nothing', () {
    expect(_summaryFor(const <DashboardMetric>{}, l10n).tiles, isEmpty);
  });

  // Edit mode (Kotlin expands the spec list to every widget id): without this a
  // metric the provider cannot serve has no tile, so it can never be added back.
  group('includeUnsupported', () {
    test('materialises metrics absent from supportedMetrics', () {
      final supported = DashboardMetric.values.toSet()
        ..remove(DashboardMetric.spo2);
      final summary =
          _summaryFor(supported, l10n, includeUnsupported: true);

      expect(_titles(summary), contains('Blood oxygen'));
      expect(summary.unsupportedTitles, contains('Blood oxygen'));
      // An unsupported tile is empty, like any other metric with no reading.
      expect(_tile(summary, 'Blood oxygen').value, isEmpty);
      // Supported metrics are not mistaken for unsupported ones.
      expect(summary.unsupportedTitles, hasLength(1));
    });

    test('materialises every metric when the device supports nothing', () {
      final summary = _summaryFor(
        const <DashboardMetric>{},
        l10n,
        includeUnsupported: true,
      );

      expect(summary.tiles, isNotEmpty);
      expect(summary.unsupportedTitles, hasLength(summary.tiles.length));
    });

    test('defaults to false: unsupported metrics stay dropped', () {
      final supported = DashboardMetric.values.toSet()
        ..remove(DashboardMetric.spo2);
      final summary = _summaryFor(supported, l10n);

      expect(_titles(summary), isNot(contains('Blood oxygen')));
      expect(summary.unsupportedTitles, isEmpty);
    });
  });

  group('Body Energy tile', () {
    test('renders currentScore and the Start/+/- subtitle when set up', () {
      final summary = _summaryFor(
        {DashboardMetric.bodyEnergy},
        l10n,
        bodyEnergyTimeline: _bodyEnergyTimeline(),
      );

      final tile = _tile(summary, 'Body Energy');
      expect(tile.value, '74');
      expect(tile.subtitle, 'Start 60  +30 / -16');
      expect(tile.message, isNull);
      // Taps through to the Body Energy detail screen for the selected day.
      expect(tile.location, '/daily_readiness/body_energy/2026-01-02');
    });

    test('shows "Not set up" when the timeline is absent', () {
      final summary = _summaryFor({DashboardMetric.bodyEnergy}, l10n);

      final tile = _tile(summary, 'Body Energy');
      expect(tile.value, isEmpty);
      expect(tile.message, l10n.bodyEnergyNotSetUp);
      expect(tile.subtitle, isNull);
    });
  });

  // Kotlin routes: heart/vitals tiles open the Heart & Vitals OVERVIEW (not the
  // metric directly); hydration/mindfulness/caffeine open their DETAIL screens
  // (not entry forms / the nutrition overview).
  group('tile destinations match Kotlin', () {
    test('heart and vitals tiles all open the heart_vitals overview', () {
      final summary = _summaryFor({
        DashboardMetric.avgHeartRate,
        DashboardMetric.restingHeartRate,
        DashboardMetric.hrv,
        DashboardMetric.bloodPressure,
        DashboardMetric.spo2,
        DashboardMetric.vo2Max,
        DashboardMetric.respiratoryRate,
        DashboardMetric.bodyTemperature,
        DashboardMetric.bloodGlucose,
        DashboardMetric.skinTemperature,
      }, l10n);
      for (final tile in summary.tiles) {
        expect(tile.location, '/heart_vitals', reason: tile.title);
      }
    });

    test('hydration, mindfulness and caffeine tiles open their detail views',
        () {
      final summary = _summaryFor({
        DashboardMetric.hydration,
        DashboardMetric.mindfulness,
        DashboardMetric.caffeine,
      }, l10n);
      expect(_tile(summary, 'Beverages').location, '/metric/HYDRATION');
      expect(_tile(summary, 'Mindfulness').location, '/metric/MINDFULNESS');
      expect(_tile(summary, 'Caffeine').location, '/metric/CAFFEINE');
    });
  });
}
