import '../../../domain/insights/body_energy_timeline.dart';

/// The presentation model for the Body Energy timeline detail, a Dart port of the
/// Kotlin `BodyEnergyDisplayState` + `toBodyEnergyDisplayState()` mapping: the
/// per-bucket chart points, the charge/drain influence bars, the ranked "why"
/// reasons, and the input-availability rows.
///
/// Built once per load by [buildBodyEnergyDisplay] and stored on the state — the
/// view-model precomputes, the screen only renders. It used to be built in the
/// screen's build method, which is the seam this refactor reverses; the function
/// itself was already pure, and only had to move layer.
const double _minutesPerDay = 24.0 * 60.0;
const double _minimumReasonAmount = 0.5;
const int _maxTopReasons = 3;

class BodyEnergyChartPoint {
  const BodyEnergyChartPoint(this.xFraction, this.score);

  final double xFraction;
  final double score;
}

class BodyEnergyInfluenceBar {
  const BodyEnergyInfluenceBar({
    required this.xFraction,
    required this.widthFraction,
    required this.charge,
    required this.drain,
    required this.influence,
  });

  final double xFraction;
  final double widthFraction;
  final double charge;
  final double drain;
  final BodyEnergyPrimaryInfluence influence;
}

enum BodyEnergyReasonDirection { charge, drain }

class BodyEnergyReason {
  const BodyEnergyReason({
    required this.influence,
    required this.direction,
    required this.amount,
  });

  final BodyEnergyPrimaryInfluence influence;
  final BodyEnergyReasonDirection direction;
  final double amount;

  int get roundedAmount => amount.round();
}

enum BodyEnergyInputKind {
  heartRate,
  sleep,
  workouts,
  restingHeartRate,
  heartRateBaseline,
  hrv,
  respiratoryRate,
  previousScore,
  calibration,
}

enum BodyEnergyInputStatus { available, missing, optional }

class BodyEnergyInputRow {
  const BodyEnergyInputRow({
    required this.kind,
    required this.status,
    this.count,
    this.value,
  });

  final BodyEnergyInputKind kind;
  final BodyEnergyInputStatus status;
  final int? count;
  final String? value;
}

class BodyEnergyDisplay {
  const BodyEnergyDisplay({
    this.timeline,
    this.inputSummary,
    this.chartPoints = const [],
    this.influenceBars = const [],
    this.legendInfluences = const [],
    this.topReasons = const [],
    this.inputRows = const [],
    this.maxInfluenceMagnitude = 1.0,
  });

  final BodyEnergyTimeline? timeline;
  final BodyEnergyInputSummary? inputSummary;
  final List<BodyEnergyChartPoint> chartPoints;
  final List<BodyEnergyInfluenceBar> influenceBars;
  final List<BodyEnergyPrimaryInfluence> legendInfluences;
  final List<BodyEnergyReason> topReasons;
  final List<BodyEnergyInputRow> inputRows;

  /// The tallest bar the influence strip has to fit, floored at 1.0 so an
  /// all-zero day divides by something. The painter used to scan for it on
  /// every repaint.
  final double maxInfluenceMagnitude;

  bool get isEmpty => timeline == null || chartPoints.isEmpty;
}

BodyEnergyDisplay buildBodyEnergyDisplay(BodyEnergyTimeline? timeline) {
  if (timeline == null) return const BodyEnergyDisplay();
  if (timeline.points.isEmpty) {
    return BodyEnergyDisplay(
      timeline: timeline,
      inputSummary: timeline.inputSummary,
      inputRows: _inputRows(timeline.inputSummary),
    );
  }

  final start = timeline.date.atTimeInstant(0);
  const totalSeconds = 86400.0;
  final widthFraction = timeline.inputSummary.bucketMinutes / _minutesPerDay;

  final chartPoints = [
    for (final point in timeline.points)
      BodyEnergyChartPoint(
        (point.time.difference(start).inSeconds / totalSeconds).clamp(0.0, 1.0),
        point.score.toDouble(),
      ),
  ];
  final influenceBars = [
    for (final point in timeline.points)
      BodyEnergyInfluenceBar(
        xFraction:
            (point.time.difference(start).inSeconds / totalSeconds)
                .clamp(0.0, 1.0),
        widthFraction: widthFraction,
        charge: point.charge,
        drain: point.basalDrain +
            point.appliedActivityDrain +
            point.stressDrain +
            point.recoveryDebtDrain,
        influence: point.primaryInfluence,
      ),
  ];
  final legend = <BodyEnergyPrimaryInfluence>[];
  for (final bar in influenceBars) {
    final active = bar.charge > 0.0 ||
        bar.drain > 0.0 ||
        bar.influence == BodyEnergyPrimaryInfluence.noData;
    if (active && !legend.contains(bar.influence)) legend.add(bar.influence);
  }
  if (legend.isEmpty) legend.add(BodyEnergyPrimaryInfluence.steady);

  return BodyEnergyDisplay(
    timeline: timeline,
    inputSummary: timeline.inputSummary,
    chartPoints: chartPoints,
    influenceBars: influenceBars,
    legendInfluences: legend,
    topReasons: _topReasons(timeline),
    inputRows: _inputRows(timeline.inputSummary),
    maxInfluenceMagnitude: _maxInfluenceMagnitude(influenceBars),
  );
}

/// The influence strip scales every bar against the tallest one; a day with no
/// charge and no drain divides by 1.0 rather than by zero.
double _maxInfluenceMagnitude(List<BodyEnergyInfluenceBar> bars) {
  var maxMagnitude = 0.0;
  for (final bar in bars) {
    final magnitude = bar.charge > bar.drain ? bar.charge : bar.drain;
    if (magnitude > maxMagnitude) maxMagnitude = magnitude;
  }
  if (maxMagnitude <= 0.0) maxMagnitude = 1.0;
  return maxMagnitude;
}

List<BodyEnergyReason> _topReasons(BodyEnergyTimeline timeline) {
  final chargeByInfluence = <BodyEnergyPrimaryInfluence, double>{};
  for (final point in timeline.points) {
    if (point.charge <= 0.0) continue;
    final influence =
        point.primaryInfluence == BodyEnergyPrimaryInfluence.sleepRecovery
            ? BodyEnergyPrimaryInfluence.sleepRecovery
            : BodyEnergyPrimaryInfluence.quietRest;
    chargeByInfluence[influence] =
        (chargeByInfluence[influence] ?? 0.0) + point.charge;
  }

  final drainByInfluence = <BodyEnergyPrimaryInfluence, double>{};
  for (final point in timeline.points) {
    void add(BodyEnergyPrimaryInfluence influence, double amount) {
      if (amount <= 0.0) return;
      drainByInfluence[influence] =
          (drainByInfluence[influence] ?? 0.0) + amount;
    }

    // The applied activity drain is everyday movement when the active-calorie
    // estimate carried it, exertion when heart rate did.
    final activityInfluence =
        point.activityEnergyDrain > point.intensityDrain
            ? BodyEnergyPrimaryInfluence.everydayActivity
            : BodyEnergyPrimaryInfluence.exertion;
    add(activityInfluence, point.appliedActivityDrain);
    add(BodyEnergyPrimaryInfluence.elevatedHeartRate, point.stressDrain);
    add(BodyEnergyPrimaryInfluence.recoveryDebt, point.recoveryDebtDrain);
    add(BodyEnergyPrimaryInfluence.steady, point.basalDrain);
  }

  final reasons = <BodyEnergyReason>[
    for (final entry in chargeByInfluence.entries)
      BodyEnergyReason(
        influence: entry.key,
        direction: BodyEnergyReasonDirection.charge,
        amount: entry.value,
      ),
    for (final entry in drainByInfluence.entries)
      BodyEnergyReason(
        influence: entry.key,
        direction: BodyEnergyReasonDirection.drain,
        amount: entry.value,
      ),
  ]
      .where((reason) => reason.amount >= _minimumReasonAmount)
      .toList()
    ..sort((a, b) => b.amount.compareTo(a.amount));
  return reasons.take(_maxTopReasons).toList();
}

List<BodyEnergyInputRow> _inputRows(BodyEnergyInputSummary summary) {
  BodyEnergyInputStatus availableOrMissing(bool present) => present
      ? BodyEnergyInputStatus.available
      : BodyEnergyInputStatus.missing;
  BodyEnergyInputStatus availableOrOptional(bool present) => present
      ? BodyEnergyInputStatus.available
      : BodyEnergyInputStatus.optional;

  return [
    BodyEnergyInputRow(
      kind: BodyEnergyInputKind.heartRate,
      status: availableOrMissing(summary.heartRateSampleCount > 0),
      count: summary.heartRateSampleCount,
    ),
    BodyEnergyInputRow(
      kind: BodyEnergyInputKind.sleep,
      status: availableOrOptional(summary.sleepSessionCount > 0),
      count: summary.sleepSessionCount,
    ),
    BodyEnergyInputRow(
      kind: BodyEnergyInputKind.workouts,
      status: availableOrOptional(summary.workoutCount > 0),
      count: summary.workoutCount,
    ),
    BodyEnergyInputRow(
      kind: BodyEnergyInputKind.restingHeartRate,
      status: availableOrMissing(summary.hasRestingHeartRate),
    ),
    BodyEnergyInputRow(
      kind: BodyEnergyInputKind.heartRateBaseline,
      status: availableOrMissing(
        summary.hasBaselineRestingHeartRate || summary.hasObservedMaxHeartRate,
      ),
    ),
    BodyEnergyInputRow(
      kind: BodyEnergyInputKind.hrv,
      status: availableOrOptional(
        summary.hrvSampleCount > 0 || summary.hasHrvBaseline,
      ),
      count: summary.hrvSampleCount,
    ),
    BodyEnergyInputRow(
      kind: BodyEnergyInputKind.respiratoryRate,
      status: availableOrOptional(
        summary.respiratorySampleCount > 0 || summary.hasRespiratoryBaseline,
      ),
      count: summary.respiratorySampleCount,
    ),
    BodyEnergyInputRow(
      kind: BodyEnergyInputKind.previousScore,
      status: availableOrOptional(summary.previousEndScore != null),
      value: summary.previousEndScore?.toString(),
    ),
    BodyEnergyInputRow(
      kind: BodyEnergyInputKind.calibration,
      status: BodyEnergyInputStatus.available,
      value: summary.calibrationMode.name,
    ),
  ];
}
