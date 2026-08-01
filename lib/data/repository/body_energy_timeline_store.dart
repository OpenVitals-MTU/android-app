import 'package:drift/drift.dart' show Value;

import '../../core/time/local_date.dart';
import '../../domain/insights/body_energy_timeline.dart';
import '../local/open_vitals_database.dart';

/// How long a past day stays eligible for recomputation.
///
/// Watches sync late and Health Connect back-fills, so a recent day can still
/// gain data and is worth recomputing. A day beyond this window cannot: nothing
/// new will arrive for it, recomputing costs ~8 Health Connect reads to
/// reproduce what is already stored, and — for a user who declined the history
/// grant, which Android cannot offer from the in-app dialog — Health Connect
/// serves only ~30 days, so the recompute can come back empty and take the
/// stored day with it.
///
/// Seven days rather than two or three because a Garmin watch is often synced
/// only once a week, and one sync back-fills every day it covers.
const int bodyEnergyChainSettlingDays = 7;

/// Drift-backed storage for the Body Energy chain: the day summaries whose end
/// scores seed each following day, and the 5-minute buckets behind them.
///
/// Replaces the SharedPreferences timeline cache. That store encoded a whole
/// day as one delimited string, so answering "what did yesterday end on" — the
/// question the chain asks constantly — meant decoding 288 points, and a range
/// query for a multi-day view was impossible.
///
/// The [BodyEnergyTimelineDao] speaks generated rows; the repository speaks
/// [BodyEnergyTimeline]. That mapping lives here so the repository stays about
/// the chain rather than about columns.
class BodyEnergyTimelineStore {
  BodyEnergyTimelineStore(this._dao);

  final BodyEnergyTimelineDao _dao;

  /// The stored timeline for [date], or null when nothing is stored or the
  /// stored row was computed under a different [signature].
  Future<BodyEnergyTimeline?> load(LocalDate date, String signature) async {
    final day = await _dao.day(date.epochDay);
    if (day == null || day.signature != signature) return null;
    final buckets = await _dao.bucketsForDay(date.epochDay);
    return _toTimeline(day, buckets);
  }

  /// Persists [timeline], replacing whatever was stored for its date.
  Future<void> save(BodyEnergyTimeline timeline) async {
    // An unsigned timeline cannot be validated on read, so storing it would
    // only produce a row every future read discards (the prefs store had the
    // same guard).
    if (timeline.signature.trim().isEmpty) return;
    final epochDay = timeline.date.epochDay;
    await _dao.upsertDay(
      _toSummaryCompanion(timeline, epochDay),
      timeline.points
          .map((point) => _toBucketCompanion(point, epochDay))
          .toList(),
    );
  }

  /// The chain-relevant facts for `[start, end]`, oldest first — one query, no
  /// bucket decoding. The walk-back reads its whole lookback window with this.
  Future<List<BodyEnergyStoredDay>> storedDaysBetween(
    LocalDate start,
    LocalDate end,
  ) async {
    final rows = await _dao.daysBetween(start.epochDay, end.epochDay);
    return rows
        .map((row) => BodyEnergyStoredDay(
              date: LocalDate.fromEpochDay(row.epochDay),
              signature: row.signature,
              startScore: row.startScore,
              endScore: row.endScore,
              generatedAt: DateTime.fromMillisecondsSinceEpoch(
                row.generatedAtMillis,
                isUtc: true,
              ),
            ))
        .toList();
  }

  /// Whether a stored day still has buckets behind it.
  ///
  /// Signature-independent on purpose: the caller is protecting the *shape* of
  /// a day it may no longer be able to re-read, and a calibration edit is no
  /// reason to discard that.
  Future<bool> hasStoredPoints(LocalDate date) async =>
      await _dao.countBucketsForDay(date.epochDay) > 0;

  /// Forward ripple: every day in `[from, to]` was computed from a seed that no
  /// longer holds, so drop them and let them be recomputed.
  Future<void> invalidateForward(LocalDate from, LocalDate to) =>
      _dao.deleteDays(from.epochDay, to.epochDay);

  /// Drops the whole chain and its cursor — the calibration/algorithm reset.
  Future<void> purgeAll() => _dao.purgeAll();

  /// Drops buckets older than [bodyEnergyBucketRetentionDays], keeping the day
  /// summaries so the chain stays walkable.
  Future<void> applyRetention(LocalDate today) => _dao.purgeBucketsBefore(
        today.minusDays(bodyEnergyBucketRetentionDays).epochDay,
      );

  /// The global signature the stored chain was built under, or null when the
  /// chain has never been synced.
  Future<String?> storedGlobalSignature() async =>
      (await _dao.chainCursor())?.changesToken;

  Future<void> writeGlobalSignature(String signature) =>
      _dao.writeChainCursor(globalSignature: signature);

  Future<DateTime?> lastPassAt() async {
    final millis = (await _dao.chainCursor())?.lastFullSyncMillis;
    return millis == null
        ? null
        : DateTime.fromMillisecondsSinceEpoch(millis, isUtc: true);
  }

  Future<void> writeLastPassAt(DateTime at) =>
      _dao.writeChainCursor(lastPassMillis: at.toUtc().millisecondsSinceEpoch);
}

/// A stored day's chain-relevant facts.
///
/// Deliberately not a [BodyEnergyTimeline]: the walk-back asks for a fortnight
/// of these on a cold screen open and must not pay a bucket read per day to
/// learn an end score.
class BodyEnergyStoredDay {
  const BodyEnergyStoredDay({
    required this.date,
    required this.signature,
    required this.startScore,
    required this.endScore,
    required this.generatedAt,
  });

  final LocalDate date;
  final String signature;
  final int startScore;
  final int endScore;
  final DateTime generatedAt;
}

BodyEnergyTimeline _toTimeline(
  BodyEnergyDay day,
  List<BodyEnergyBucket> buckets,
) =>
    BodyEnergyTimeline(
      date: LocalDate.fromEpochDay(day.epochDay),
      startScore: day.startScore,
      currentScore: day.endScore,
      charged: day.charged,
      drained: day.drained,
      points: buckets.map(_toPoint).toList(),
      confidence: BodyEnergyConfidence.fromStorage(day.confidence) ??
          BodyEnergyConfidence.noData,
      confidenceReason: day.confidenceReason,
      confidenceReasonCode: bodyEnergyReasonCodeForText(day.confidenceReason),
      inputSummary: BodyEnergyInputSummary(
        algorithmVersion: day.algorithmVersion,
        bucketMinutes: day.bucketMinutes,
        heartRateSampleCount: day.heartRateSampleCount,
        hrvSampleCount: day.hrvSampleCount,
        sleepSessionCount: day.sleepSessionCount,
        workoutCount: day.workoutCount,
        respiratorySampleCount: day.respiratorySampleCount,
        hasRestingHeartRate: day.hasRestingHeartRate,
        hasBaselineRestingHeartRate: day.hasBaselineRestingHeartRate,
        hasObservedMaxHeartRate: day.hasObservedMaxHeartRate,
        hasHrvBaseline: day.hasHrvBaseline,
        hasRespiratoryBaseline: day.hasRespiratoryBaseline,
        previousEndScore: day.previousEndScore,
        carryOverFloorApplied: day.carryOverFloorApplied,
        seedSource: BodyEnergySeedSource.fromStorage(day.seedSource) ??
            BodyEnergySeedSource.neutral,
        calibrationMode:
            BodyEnergyCalibrationMode.fromStorage(day.calibrationMode) ??
                BodyEnergyCalibrationMode.automatic,
      ),
      generatedAt: DateTime.fromMillisecondsSinceEpoch(
        day.generatedAtMillis,
        isUtc: true,
      ),
      signature: day.signature,
    );

BodyEnergyTimelinePoint _toPoint(BodyEnergyBucket bucket) =>
    BodyEnergyTimelinePoint(
      time: DateTime.fromMillisecondsSinceEpoch(bucket.timeMillis, isUtc: true),
      score: bucket.score,
      delta: bucket.delta,
      state: BodyEnergyBucketState.fromStorage(bucket.state) ??
          BodyEnergyBucketState.unmeasurable,
      confidence: BodyEnergyConfidence.fromStorage(bucket.confidence) ??
          BodyEnergyConfidence.noData,
      charge: bucket.charge,
      intensityDrain: bucket.intensityDrain,
      activityEnergyDrain: bucket.activityEnergyDrain,
      basalDrain: bucket.basalDrain,
      stressDrain: bucket.stressDrain,
      recoveryDebtDrain: bucket.recoveryDebtDrain,
      primaryInfluence:
          BodyEnergyPrimaryInfluence.fromStorage(bucket.primaryInfluence) ??
              BodyEnergyPrimaryInfluence.steady,
    );

BodyEnergyDaysCompanion _toSummaryCompanion(
  BodyEnergyTimeline timeline,
  int epochDay,
) {
  final summary = timeline.inputSummary;
  return BodyEnergyDaysCompanion.insert(
    epochDay: Value(epochDay),
    signature: timeline.signature,
    startScore: timeline.startScore,
    endScore: timeline.currentScore,
    charged: timeline.charged,
    drained: timeline.drained,
    confidence: timeline.confidence.storageName,
    confidenceReason: timeline.confidenceReason,
    generatedAtMillis:
        (timeline.generatedAt ?? DateTime.now()).toUtc().millisecondsSinceEpoch,
    algorithmVersion: summary.algorithmVersion,
    bucketMinutes: summary.bucketMinutes,
    heartRateSampleCount: summary.heartRateSampleCount,
    hrvSampleCount: summary.hrvSampleCount,
    sleepSessionCount: summary.sleepSessionCount,
    workoutCount: summary.workoutCount,
    respiratorySampleCount: summary.respiratorySampleCount,
    hasRestingHeartRate: summary.hasRestingHeartRate,
    hasBaselineRestingHeartRate: summary.hasBaselineRestingHeartRate,
    hasObservedMaxHeartRate: summary.hasObservedMaxHeartRate,
    hasHrvBaseline: summary.hasHrvBaseline,
    hasRespiratoryBaseline: summary.hasRespiratoryBaseline,
    previousEndScore: Value(summary.previousEndScore),
    carryOverFloorApplied: summary.carryOverFloorApplied,
    seedSource: summary.seedSource.storageName,
    calibrationMode: summary.calibrationMode.storageName,
  );
}

BodyEnergyBucketsCompanion _toBucketCompanion(
  BodyEnergyTimelinePoint point,
  int epochDay,
) =>
    BodyEnergyBucketsCompanion.insert(
      epochDay: epochDay,
      timeMillis: point.time.toUtc().millisecondsSinceEpoch,
      score: point.score,
      delta: point.delta,
      state: point.state.storageName,
      confidence: point.confidence.storageName,
      charge: point.charge,
      intensityDrain: point.intensityDrain,
      activityEnergyDrain: point.activityEnergyDrain,
      basalDrain: point.basalDrain,
      stressDrain: point.stressDrain,
      recoveryDebtDrain: point.recoveryDebtDrain,
      primaryInfluence: point.primaryInfluence.storageName,
    );
