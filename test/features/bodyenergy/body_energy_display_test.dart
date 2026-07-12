import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/features/bodyenergy/application/body_energy_display.dart';

/// The Body Energy display was always a pure mapping — it was just being run in
/// the screen's build method. It lives in application/ now, and the view-model
/// calls it once per load; these are its unit tests.
BodyEnergyTimelinePoint _point(
  LocalDate date,
  int hour, {
  int score = 60,
  double charge = 0.0,
  double intensityDrain = 0.0,
  double stressDrain = 0.0,
  double recoveryDebtDrain = 0.0,
  BodyEnergyPrimaryInfluence influence = BodyEnergyPrimaryInfluence.steady,
}) =>
    BodyEnergyTimelinePoint.build(
      time: date.atTimeInstant(hour),
      score: score,
      delta: 0,
      state: BodyEnergyBucketState.rest,
      confidence: BodyEnergyConfidence.high,
      charge: charge,
      intensityDrain: intensityDrain,
      stressDrain: stressDrain,
      recoveryDebtDrain: recoveryDebtDrain,
      primaryInfluence: influence,
    );

BodyEnergyTimeline _timeline(
  LocalDate date,
  List<BodyEnergyTimelinePoint> points,
) =>
    BodyEnergyTimeline(
      date: date,
      startScore: 50,
      currentScore: 62,
      charged: 14,
      drained: 2,
      points: points,
      confidence: BodyEnergyConfidence.high,
      confidenceReason: 'test',
    );

void main() {
  const day = LocalDate(2026, 3, 2);

  test('no timeline at all is an empty display', () {
    final display = buildBodyEnergyDisplay(null);

    expect(display.isEmpty, isTrue);
    expect(display.timeline, isNull);
    expect(display.chartPoints, isEmpty);
    expect(display.inputRows, isEmpty);
    expect(display.maxInfluenceMagnitude, 1.0);
  });

  test('a timeline with no points still explains its inputs', () {
    final display = buildBodyEnergyDisplay(_timeline(day, const []));

    expect(display.isEmpty, isTrue);
    expect(display.chartPoints, isEmpty);
    // The "what it ran on" card is exactly what an empty day needs.
    expect(display.inputRows, isNotEmpty);
    expect(display.inputRows.first.kind, BodyEnergyInputKind.heartRate);
    expect(display.inputRows.first.status, BodyEnergyInputStatus.missing);
  });

  test('points become day fractions, and the strip scales to its tallest bar',
      () {
    final display = buildBodyEnergyDisplay(_timeline(day, [
      _point(day, 0, score: 50),
      _point(day, 6, score: 60, charge: 4.0),
      _point(day, 18, score: 40, intensityDrain: 9.0),
    ]));

    expect(display.isEmpty, isFalse);
    expect(display.chartPoints.first.xFraction, 0.0);
    expect(display.chartPoints[1].xFraction, closeTo(0.25, 0.001));
    expect(display.chartPoints.last.xFraction, closeTo(0.75, 0.001));
    expect(display.chartPoints.last.score, 40.0);

    // The drain is the sum of the three drains; the scale is the tallest of
    // either side.
    expect(display.influenceBars.last.drain, 9.0);
    expect(display.maxInfluenceMagnitude, 9.0);
  });

  test('the reasons rank charge and drain together, and drop the trivial ones',
      () {
    final display = buildBodyEnergyDisplay(_timeline(day, [
      _point(
        day,
        2,
        charge: 12.0,
        influence: BodyEnergyPrimaryInfluence.sleepRecovery,
      ),
      _point(day, 9, intensityDrain: 20.0),
      _point(day, 14, stressDrain: 3.0),
      // Below the 0.5 floor: not a reason, just noise.
      _point(day, 16, recoveryDebtDrain: 0.2),
    ]));

    expect(
      display.topReasons.map((r) => r.influence).toList(),
      [
        BodyEnergyPrimaryInfluence.exertion,
        BodyEnergyPrimaryInfluence.sleepRecovery,
        BodyEnergyPrimaryInfluence.elevatedHeartRate,
      ],
    );
    expect(display.topReasons.first.direction, BodyEnergyReasonDirection.drain);
    expect(display.topReasons.first.roundedAmount, 20);
    expect(display.topReasons[1].direction, BodyEnergyReasonDirection.charge);
    // recoveryDebt's 0.2 never makes the list.
    expect(
      display.topReasons.map((r) => r.influence),
      isNot(contains(BodyEnergyPrimaryInfluence.recoveryDebt)),
    );
  });

  test('the legend lists only the influences that actually moved the score', () {
    final display = buildBodyEnergyDisplay(_timeline(day, [
      _point(day, 3, charge: 2.0, influence: BodyEnergyPrimaryInfluence.quietRest),
      // No charge, no drain, not NO_DATA: nothing to put in the legend.
      _point(day, 4, influence: BodyEnergyPrimaryInfluence.steady),
      _point(
        day,
        10,
        intensityDrain: 5.0,
        influence: BodyEnergyPrimaryInfluence.exertion,
      ),
    ]));

    expect(display.legendInfluences, [
      BodyEnergyPrimaryInfluence.quietRest,
      BodyEnergyPrimaryInfluence.exertion,
    ]);
  });
}
