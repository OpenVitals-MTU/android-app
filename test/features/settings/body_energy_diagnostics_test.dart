import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/insights/body_energy_watch_observations.dart';
import 'package:openvitals/domain/model/health_source_totals.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';
import 'package:openvitals/features/settings/application/body_energy_diagnostics.dart';

void main() {
  const date = LocalDate(2026, 7, 25);
  final dayStart = DateTime(2026, 7, 25);

  BodyEnergyTimelinePoint point({
    required int minute,
    required int score,
    double charge = 0.0,
    double intensityDrain = 0.0,
    double activityEnergyDrain = 0.0,
    double basalDrain = 0.0,
    double stressDrain = 0.0,
    double recoveryDebtDrain = 0.0,
    BodyEnergyPrimaryInfluence influence = BodyEnergyPrimaryInfluence.steady,
  }) =>
      BodyEnergyTimelinePoint(
        time: dayStart.add(Duration(minutes: minute)),
        score: score,
        delta: charge -
            (basalDrain +
                (intensityDrain > activityEnergyDrain
                    ? intensityDrain
                    : activityEnergyDrain) +
                stressDrain +
                recoveryDebtDrain),
        state: BodyEnergyBucketState.rest,
        confidence: BodyEnergyConfidence.high,
        charge: charge,
        intensityDrain: intensityDrain,
        activityEnergyDrain: activityEnergyDrain,
        basalDrain: basalDrain,
        stressDrain: stressDrain,
        recoveryDebtDrain: recoveryDebtDrain,
        primaryInfluence: influence,
      );

  BodyEnergyTimeline timeline({
    required List<BodyEnergyTimelinePoint> points,
    int startScore = 50,
    int? currentScore,
    int charged = 0,
    int drained = 0,
  }) =>
      BodyEnergyTimeline(
        date: date,
        startScore: startScore,
        currentScore: currentScore ?? (points.isEmpty ? startScore : points.last.score),
        charged: charged,
        drained: drained,
        points: points,
        confidence: BodyEnergyConfidence.high,
        confidenceReason: 'test',
      );

  SourceDayTotal source(
    String package,
    double total, {
    HealthRecordSourceMetric metric = HealthRecordSourceMetric.activeCalories,
    int recordCount = 100,
    double coveredMinutes = 1400,
  }) =>
      SourceDayTotal(
        metric: metric,
        package: package,
        date: date,
        total: total,
        recordCount: recordCount,
        manualEntryCount: 0,
        coveredMinutes: coveredMinutes,
        firstStart: dayStart,
        lastEnd: dayStart.add(const Duration(hours: 23)),
      );

  BodyEnergyDiagnosticsReport report({
    required List<BodyEnergyTimeline> days,
    Map<int, List<WatchBodyEnergySample>> watch = const {},
    Map<int, ActivityProgressTotals> input = const {},
    List<SourceDayTotal> sources = const [],
    BodyEnergyCalibration calibration = const BodyEnergyCalibration(),
  }) =>
      buildBodyEnergyDiagnostics(
        days: days,
        watchSamplesByEpochDay: watch,
        modelInputByEpochDay: input,
        sourceTotals: sources,
        calibration: calibration,
      );

  group('the component decomposition', () {
    test('activity uses the per-point max, never the sum', () {
      // Summing intensity and energy would double-count every bucket. This is
      // the arithmetic the whole constants-vs-input question rests on.
      final result = report(days: [
        timeline(points: [
          point(minute: 0, score: 50, intensityDrain: 2.0, activityEnergyDrain: 5.0),
          point(minute: 5, score: 45, intensityDrain: 7.0, activityEnergyDrain: 1.0),
        ]),
      ]);

      final day = result.days.single;
      expect(day.activityApplied, closeTo(12.0, 1e-9));
      expect(day.activityFromEnergy, closeTo(5.0, 1e-9));
      expect(day.activityFromIntensity, closeTo(7.0, 1e-9));
      expect(day.energyWonBuckets, 1);
      expect(day.intensityWonBuckets, 1);
    });

    test('a tie counts as the calorie estimate winning', () {
      // `>=`, matching the model's own `energyDriven` test, so the two agree on
      // which side a tie is attributed to.
      final result = report(days: [
        timeline(points: [
          point(minute: 0, score: 50, intensityDrain: 3.0, activityEnergyDrain: 3.0),
        ]),
      ]);

      expect(result.days.single.energyWonBuckets, 1);
      expect(result.days.single.intensityWonBuckets, 0);
    });

    test('buckets with no activity drain count for neither side', () {
      final result = report(days: [
        timeline(points: [point(minute: 0, score: 50, basalDrain: 0.11)]),
      ]);

      final day = result.days.single;
      expect(day.energyWonBuckets, 0);
      expect(day.intensityWonBuckets, 0);
      expect(day.basalDrain, closeTo(0.11, 1e-9));
    });
  });

  group('clipping', () {
    test('a floored day reports its buckets and when it first pinned', () {
      // The headline `drained` cannot show over-draining since the totals became
      // applied: a day wanting 250 and one wanting 60 read the same once pinned.
      final result = report(days: [
        timeline(
          startScore: 30,
          charged: 0,
          drained: 30,
          points: [
            point(minute: 0, score: 20),
            point(minute: 5, score: 0),
            point(minute: 10, score: 0),
          ],
        ),
      ]);

      final day = result.days.single;
      expect(day.floorBuckets, 2);
      expect(day.firstFloorTime, dayStart.add(const Duration(minutes: 5)));
      expect(day.ceilingBuckets, 0);
      expect(day.ledgerOk, isTrue);
    });

    test('a ledger that does not balance is flagged', () {
      final result = report(days: [
        timeline(
          startScore: 50,
          charged: 0,
          drained: 10,
          currentScore: 25,
          points: [point(minute: 0, score: 25)],
        ),
      ]);

      expect(result.days.single.ledgerOk, isFalse);
    });
  });

  group('the watch totals', () {
    test('are delta sums, not start minus end', () {
      // A saw-tooth day nets to zero but both charged and drained are large.
      // Endpoint arithmetic would report nothing happened.
      final samples = [
        for (final (minute, score) in [(0, 50), (10, 70), (20, 40), (30, 50)])
          WatchBodyEnergySample(
            time: dayStart.add(Duration(minutes: minute)),
            score: score,
          ),
      ];
      final result = report(
        days: [timeline(points: [point(minute: 0, score: 50)])],
        watch: {date.epochDay: samples},
      );

      final day = result.days.single;
      expect(day.watchStart, 50);
      expect(day.watchEnd, 50);
      expect(day.watchCharged, closeTo(30.0, 1e-9)); // +20 then +10
      expect(day.watchDrained, closeTo(30.0, 1e-9)); // -30
      expect(day.watchMin, 40);
      expect(day.watchMax, 70);
    });

    test('are absent when the watch never synced that day', () {
      final result = report(days: [
        timeline(points: [point(minute: 0, score: 50)]),
      ]);

      final day = result.days.single;
      expect(day.watchDrained, isNull);
      expect(day.drainError, isNull);
      expect(day.watchSampleCount, 0);
    });

    test('drainError is signed so an over-draining model is visible', () {
      final result = report(
        days: [
          timeline(
            startScore: 50,
            drained: 40,
            points: [point(minute: 0, score: 50), point(minute: 5, score: 10)],
          ),
        ],
        watch: {
          date.epochDay: [
            WatchBodyEnergySample(time: dayStart, score: 50),
            WatchBodyEnergySample(
              time: dayStart.add(const Duration(minutes: 5)),
              score: 35,
            ),
          ],
        },
      );

      // Model drained 40, watch drained 15 — the model is 25 points hotter.
      expect(result.days.single.drainError, closeTo(25.0, 1e-9));
    });
  });

  group('per-influence errors', () {
    test('are signed, count-weighted, and omit unobserved influences', () {
      final points = [
        point(
          minute: 0,
          score: 60,
          influence: BodyEnergyPrimaryInfluence.everydayActivity,
        ),
        point(
          minute: 60,
          score: 40,
          influence: BodyEnergyPrimaryInfluence.everydayActivity,
        ),
      ];
      final result = report(
        days: [timeline(points: points)],
        watch: {
          date.epochDay: [
            WatchBodyEnergySample(time: dayStart, score: 70),
            WatchBodyEnergySample(
              time: dayStart.add(const Duration(minutes: 60)),
              score: 60,
            ),
          ],
        },
      );

      expect(result.influences, hasLength(1));
      final everyday = result.influences.single;
      expect(everyday.influence, BodyEnergyPrimaryInfluence.everydayActivity);
      expect(everyday.observationCount, 2);
      // (70-60) and (60-40) → mean +15: the watch sat above the model, i.e. the
      // model drained harder.
      expect(everyday.meanSignedError, closeTo(15.0, 1e-9));
      expect(everyday.meanAbsoluteError, closeTo(15.0, 1e-9));
      expect(
        result.influences.map((i) => i.influence),
        isNot(contains(BodyEnergyPrimaryInfluence.steady)),
      );
    });
  });

  group('source attribution', () {
    test('flags a day two apps both wrote active calories for', () {
      final result = report(
        days: [timeline(points: [point(minute: 0, score: 50)])],
        sources: [
          source('tech.mmarca.openvitals', 1100.0),
          source('com.garmin.android.apps.connectmobile', 1130.0),
        ],
      );

      expect(result.hasMultipleCalorieSources, isTrue);
      // Everything but the largest source — the size of the double count.
      expect(result.secondarySourceActiveKcal, closeTo(1100.0, 1e-9));
      expect(result.sources.first.total, 1130.0, reason: 'biggest first');
    });

    test('stays quiet when one app wrote them', () {
      final result = report(
        days: [timeline(points: [point(minute: 0, score: 50)])],
        sources: [source('tech.mmarca.openvitals', 1100.0)],
      );

      expect(result.hasMultipleCalorieSources, isFalse);
      expect(result.secondarySourceActiveKcal, 0.0);
    });

    test('does not confuse two metrics for two calorie sources', () {
      final result = report(
        days: [timeline(points: [point(minute: 0, score: 50)])],
        sources: [
          source('tech.mmarca.openvitals', 1100.0),
          source('tech.mmarca.openvitals', 9000.0,
              metric: HealthRecordSourceMetric.steps),
        ],
      );

      expect(result.hasMultipleCalorieSources, isFalse);
    });
  });

  group('toReportText', () {
    test('is stable, explicit, and carries the decisive figures', () {
      final result = report(
        days: [
          timeline(
            startScore: 30,
            charged: 0,
            drained: 30,
            points: [
              point(
                minute: 0,
                score: 10,
                activityEnergyDrain: 8.0,
                intensityDrain: 1.0,
                basalDrain: 0.11,
              ),
              point(minute: 5, score: 0, basalDrain: 0.11),
            ],
          ),
        ],
        input: {
          date.epochDay: const ActivityProgressTotals(
            activeKcal: 2233.0,
            totalKcal: 3800.0,
            steps: 9120,
          ),
        },
        sources: [
          source('tech.mmarca.openvitals', 1100.0),
          source('com.garmin.android.apps.connectmobile', 1133.0),
        ],
      );

      final text = result.toReportText();

      expect(text, contains('start 30 +0 -30 end 0'));
      expect(text, contains('floor 1b from 00:05'));
      expect(text, contains('kcal 8.0/1b'));
      expect(text, contains('activeKcal 2233.0'));
      expect(text, contains('steps 9120'));
      expect(text, contains('MULTIPLE apps wrote active calories'));
      // No bare DateTime.toString() anywhere — that would be timezone-dependent.
      expect(text, isNot(contains('.000')));
    });

    test('names missing permissions rather than showing an empty report', () {
      // "The app wrote nothing" and "you never granted it" must not look alike.
      final result = buildBodyEnergyDiagnostics(
        days: const [],
        watchSamplesByEpochDay: const {},
        modelInputByEpochDay: const {},
        sourceTotals: const [],
        calibration: const BodyEnergyCalibration(),
        missingPermissions: const {'READ_ACTIVE_CALORIES_BURNED'},
      );

      expect(result.toReportText(),
          contains('MISSING PERMISSIONS: READ_ACTIVE_CALORIES_BURNED'));
    });

    test('says so when the per-source read was truncated', () {
      final result = buildBodyEnergyDiagnostics(
        days: const [],
        watchSamplesByEpochDay: const {},
        modelInputByEpochDay: const {},
        sourceTotals: const [],
        calibration: const BodyEnergyCalibration(),
        truncated: true,
      );

      expect(result.toReportText(), contains('truncated, not proportional'));
    });
  });
}
