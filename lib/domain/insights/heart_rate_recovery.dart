import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/activity_models.dart';
import '../model/activity_entry_types.dart';
import '../model/heart_models.dart';
import 'max_heart_rate.dart';

part 'heart_rate_recovery.freezed.dart';

/// Heart-rate recovery: how far the heart rate falls in the minutes after hard
/// effort stops. A fitter heart falls faster.
///
/// It is only meaningful when the heart rate was driven near its maximum and effort
/// then stopped ABRUPTLY. Ease off gradually — slow down but keep moving — and the
/// number is a lie. So HRR is measured only for the app's guided recovery test, which
/// marks the instant of cessation with a trailing rest segment; an ordinary workout,
/// which carries no such mark, is not measured at all (see [heartRateRecoveryWindowFor]).
///
/// Health Connect has no record type for it, and the app stores no health data of its
/// own, so nothing here is persisted: HRR is DERIVED, on read, from the heart-rate
/// samples Health Connect already holds — the same standing as cardio load or stress.
/// A mark for which no sample exists is reported as absent: it is never interpolated,
/// and the drop is never guessed from an average. A number that was not measured is
/// worse than a blank.

/// The marks, in the order they are always returned.
///
/// No 10-second mark: optical sensors smooth over several seconds and even an
/// arm/chest strap is borderline that early, so a ten-second figure is unreliable
/// across the monitors people actually wear. The one-minute drop leads (it is the
/// mark with a body of normative literature behind it) and is fine for every
/// monitor type.
const List<Duration> heartRateRecoveryOffsets = [
  Duration(seconds: 30),
  Duration(minutes: 1),
  Duration(minutes: 2),
  Duration(minutes: 3),
  Duration(minutes: 4),
  Duration(minutes: 5),
];

/// The headline mark — the one-minute drop, the only mark with a body of normative
/// literature behind it.
const Duration heartRateRecoveryHeadlineOffset = Duration(minutes: 1);

/// How far from a mark a sample may sit and still be taken as that mark.
///
/// Kept tight: heart rate falls fast right after cessation (roughly 0.5-1.0 bpm/s
/// in the first half minute), so a loose window at 30s could cost several bpm — a
/// large fraction of the number reported. Monitors sample often enough while and
/// just after hard effort that a small window still finds a sample.
// Not `const`: Duration overrides `==`, which Dart forbids as a const map key.
final Map<Duration, Duration> heartRateRecoveryTolerances = {
  Duration(seconds: 30): Duration(seconds: 3),
  Duration(minutes: 1): Duration(seconds: 5),
  Duration(minutes: 2): Duration(seconds: 5),
  Duration(minutes: 3): Duration(seconds: 5),
  Duration(minutes: 4): Duration(seconds: 5),
  Duration(minutes: 5): Duration(seconds: 5),
};

/// The peak heart rate must come from a HARD window of the last ten seconds before
/// the stop. A wider window would let an effort that eased off earlier read a peak
/// from when it was still going, inflating the recovery. Monitors sample fast during
/// hard effort, so a sample is there.
const Duration _peakWindow = Duration(seconds: 10);

/// How far either side of the recovery start a sample may sit and still count as "the
/// heart rate when they stopped", for the cool-down check.
const Duration _recoveryStartTolerance = Duration(seconds: 15);

/// A fall of more than this between the last real high point and the stop means the
/// heart rate was ALREADY coming down before the "stop" — they eased off before they
/// pressed the button, which invalidates the recovery. Beat-to-beat noise is 3-4 bpm,
/// so 4 sits just above it: a genuine pre-stop cool-down of even a few beats matters.
const int _cooldownBeforeStopDropBpm = 4;

/// How far back the cool-down check looks for that high point.
const Duration _cooldownLookback = Duration(seconds: 60);

/// How far below the maximum the peak may sit and the effort still count as
/// near-maximal (so the recovery is comparable with another). A fixed BAND, not a
/// fraction: HR-max estimates carry a roughly constant absolute uncertainty, so a
/// percentage floor is too low for the young and too high for the old.
///
/// [_estimatedMaxNearBandBpm] is the ~95% confidence interval of the age formula
/// (208 - 0.7*age): a peak within ~22 bpm of the estimate is consistent with a
/// near-maximal effort. When the maximum is KNOWN (the user stated it, or we have a
/// trustworthy observed max) there is no such uncertainty, so the band is tighter.
const int _estimatedMaxNearBandBpm = 22;
const int _knownMaxNearBandBpm = 10;

/// A trailing rest segment shorter than this is an inter-set breather, not a recovery.
///
/// This matters concretely: the app already writes a rest segment after EVERY set of a
/// strength session, including the last. Without a floor, every set-based workout ever
/// recorded would be read as an HRR test whose "recovery" was a one-minute rest.
const Duration _minimumRecoverySegmentDuration = Duration(seconds: 90);

/// How near the session end a rest segment must end to be the trailing one.
const Duration _trailingSegmentSlack = Duration(seconds: 30);

/// How long after the recovery start we keep reading, past the last mark.
const Duration _readTailPadding = Duration(seconds: 30);

/// How far before the recovery start we keep reading, to find the peak.
const Duration _readHeadPadding = Duration(seconds: 60);

/// One verdict on a reading, for the UI to lead with.
enum HeartRateRecoveryQuality {
  /// Near-maximal effort, a peak taken close to the stop, and at least the one-minute
  /// mark present.
  clean,

  /// Usable, but something was estimated or coarse. See the issues.
  approximate,

  /// A real drop, from an effort too easy to compare against other readings.
  notComparable,

  /// The number would mislead. Do not chart it.
  invalid,

  /// There was nothing to measure.
  noData,
}

enum HeartRateRecoveryIssue {
  /// No samples at all in the five minutes after the stop. Typically a watch that
  /// stopped recording heart rate when the workout ended.
  noRecoverySamples,

  /// Exactly one sample stood behind the peak; a single spurious reading would be it.
  peakFromSingleSample,

  /// The heart rate was already falling before the stop — they eased off first, so the
  /// "drop" measures the cool-down and flatters them.
  cooldownBeforeStop,

  /// Hard, but not near-maximal. The drop is real; it is not comparable.
  submaximalEffort,

  /// No maximum heart rate could be resolved, so effort could not be judged.
  unknownMaxHeartRate,

  /// The heart rate did not fall after the "stop" — it was the same or higher at one of
  /// the marks. Whatever ended, the effort did not: the recording stopped before the
  /// person did. There is no recovery here, only a session boundary.
  heartRateDidNotFall,
}

/// One mark. [heartRateBpm] is null when no sample fell within tolerance — the mark
/// did not happen, and is never invented.
@freezed
abstract class HeartRateRecoveryMark with _$HeartRateRecoveryMark {
  const factory HeartRateRecoveryMark({
    required Duration offset,
    required int? heartRateBpm,
    required int? dropBpm,
    required DateTime? sampleTime,

    /// How far the sample actually sat from the mark. Lets the UI be honest: "+58s".
    required Duration? sampleSkew,
  }) = _HeartRateRecoveryMark;
}

@freezed
abstract class HeartRateRecoveryReading with _$HeartRateRecoveryReading {
  const HeartRateRecoveryReading._();

  const factory HeartRateRecoveryReading({
    required DateTime? recoveryStart,
    int? peakBpm,
    DateTime? peakTime,
    @Default(0) int peakWindowSeconds,
    @Default(0) int peakWindowSampleCount,
    @Default(<HeartRateRecoveryMark>[]) List<HeartRateRecoveryMark> marks,
    int? maxHeartRateBpmUsed,
    @Default(false) bool maxHeartRateEstimated,
    double? peakFractionOfMax,
    @Default(0) int recoverySampleCount,
    @Default(HeartRateRecoveryQuality.noData) HeartRateRecoveryQuality quality,
    @Default(<HeartRateRecoveryIssue>{}) Set<HeartRateRecoveryIssue> issues,
  }) = _HeartRateRecoveryReading;

  static const HeartRateRecoveryReading noData =
      HeartRateRecoveryReading(recoveryStart: null);

  /// The drop one minute after the stop — the figure to lead with, null when that mark
  /// was not measured.
  int? get headlineDropBpm => markAt(heartRateRecoveryHeadlineOffset)?.dropBpm;

  HeartRateRecoveryMark? markAt(Duration offset) {
    for (final mark in marks) {
      if (mark.offset == offset) return mark;
    }
    return null;
  }

  /// Whether this reading may be charted as a point in a trend.
  ///
  /// Being merely "not invalid" is not enough. The trend is of the one-minute fall, so a
  /// reading that never measured it has nothing to contribute — and on watch data, which
  /// commonly samples once a minute, that is most of them. Charting them would be
  /// charting the gaps.
  bool get isComparable =>
      (quality == HeartRateRecoveryQuality.clean ||
          quality == HeartRateRecoveryQuality.approximate) &&
      headlineDropBpm != null;
}

/// Where to measure from, and what to read, for one Health Connect session.
@freezed
abstract class HeartRateRecoveryWindow with _$HeartRateRecoveryWindow {
  const factory HeartRateRecoveryWindow({
    required DateTime recoveryStart,
    required DateTime readStart,
    required DateTime readEnd,
  }) = _HeartRateRecoveryWindow;
}

/// The instant effort stopped, for [session], and the window of heart-rate samples that
/// has to be read to measure the recovery from it — or null when the session carries no
/// mark of a deliberate stop.
///
/// Heart-rate recovery is only meaningful when the person drove their heart rate near
/// its maximum and then ABRUPTLY stopped and rested. An ordinary recorded session gives
/// no such guarantee — you slow down but keep moving — so the session's end cannot be
/// taken as the moment effort stopped. The recovery therefore begins only at a
/// qualifying trailing rest segment, which the app's guided test writes at the true
/// instant of cessation (a watch that genuinely recorded a trailing rest qualifies too).
/// No segment, no reading.
///
/// "Qualifying" is doing real work. The app writes a rest segment after every set of a
/// strength session, the last one included, so a bare "ends with a rest segment" test
/// would read every set-based workout as an HRR test with a one-minute recovery. A
/// segment therefore qualifies only if it is at least [_minimumRecoverySegmentDuration]
/// long AND ends within [_trailingSegmentSlack] of the session end.
HeartRateRecoveryWindow? heartRateRecoveryWindowFor(ExerciseData session) {
  final sessionEnd = session.endTime;
  DateTime? recoveryStart;

  for (final segment in session.segments) {
    if (segment.segmentType != ExerciseSegmentType.rest) continue;
    if (!segment.startTime.isAfter(session.startTime)) continue;
    if (segment.endTime.difference(segment.startTime) <
        _minimumRecoverySegmentDuration) {
      continue;
    }
    if (sessionEnd.difference(segment.endTime).abs() > _trailingSegmentSlack) {
      continue;
    }
    // The last qualifying one wins, so a session that somehow carries two takes the
    // one nearest the end.
    if (recoveryStart == null || segment.startTime.isAfter(recoveryStart)) {
      recoveryStart = segment.startTime;
    }
  }

  if (recoveryStart == null) return null;

  return HeartRateRecoveryWindow(
    recoveryStart: recoveryStart,
    readStart: recoveryStart.subtract(_readHeadPadding),
    readEnd: recoveryStart
        .add(heartRateRecoveryOffsets.last)
        .add(_readTailPadding),
  );
}

/// Measures the recovery from [recoveryStart] out of [samples].
///
/// [samples] should span the window [heartRateRecoveryWindowFor] asked for; anything
/// outside it is ignored. Nothing is invented: a mark with no sample within tolerance
/// comes back null.
HeartRateRecoveryReading calculateHeartRateRecovery({
  required DateTime recoveryStart,
  required List<HeartRateSample> samples,
  required int? profileMaxHeartRateBpm,
  required int? restingHeartRateBpm,
  required int? ageYears,
  required int? observedMaxHeartRateBpm,
}) {
  final ordered = _ordered(samples);
  if (ordered.isEmpty) return HeartRateRecoveryReading.noData;

  final issues = <HeartRateRecoveryIssue>{};

  final peak = _peak(ordered, recoveryStart);
  if (peak == null) return HeartRateRecoveryReading.noData;
  if (peak.sampleCount == 1) {
    issues.add(HeartRateRecoveryIssue.peakFromSingleSample);
  }

  // Strictly AFTER the stop. A sample landing exactly on it is the reading at cessation
  // — the thing we measure the fall FROM — not part of the fall. Counting it would let a
  // watch that quits the moment the workout ends look as though it had recorded a
  // recovery, when it recorded nothing at all.
  final recoverySamples = ordered
      .where((sample) =>
          sample.time.isAfter(recoveryStart) &&
          !sample.time.isAfter(recoveryStart.add(heartRateRecoveryOffsets.last)))
      .toList();
  if (recoverySamples.isEmpty) {
    return HeartRateRecoveryReading(
      recoveryStart: recoveryStart,
      peakBpm: peak.bpm,
      peakTime: peak.time,
      peakWindowSeconds: peak.windowSeconds,
      peakWindowSampleCount: peak.sampleCount,
      marks: [
        for (final offset in heartRateRecoveryOffsets)
          HeartRateRecoveryMark(
            offset: offset,
            heartRateBpm: null,
            dropBpm: null,
            sampleTime: null,
            sampleSkew: null,
          ),
      ],
      recoverySampleCount: 0,
      quality: HeartRateRecoveryQuality.noData,
      issues: {...issues, HeartRateRecoveryIssue.noRecoverySamples},
    );
  }

  final marks = [
    for (final offset in heartRateRecoveryOffsets)
      _markAt(ordered, recoveryStart, offset, peak.bpm),
  ];

  final maxContext = _resolveMaxHeartRate(
    profileMaxHeartRateBpm: profileMaxHeartRateBpm,
    observedMaxHeartRateBpm: observedMaxHeartRateBpm,
    restingHeartRateBpm: restingHeartRateBpm,
    ageYears: ageYears,
  );
  if (maxContext == null) {
    issues.add(HeartRateRecoveryIssue.unknownMaxHeartRate);
  }

  // Near-maximal effort, judged as an absolute distance below the maximum, not a
  // fraction of it — a fixed band, wider when the maximum was estimated from age
  // (which carries that much uncertainty) than when it is known. A peak more than the
  // band below the maximum is a real recovery from a submaximal effort: shown, but not
  // comparable across days.
  final peakFraction =
      maxContext == null ? null : peak.bpm / maxContext.bpm;
  if (maxContext != null) {
    final band = maxContext.estimated
        ? _estimatedMaxNearBandBpm
        : _knownMaxNearBandBpm;
    if (peak.bpm < maxContext.bpm - band) {
      issues.add(HeartRateRecoveryIssue.submaximalEffort);
    }
  }

  // Was the heart rate already coming down before they "stopped"?
  //
  // Compare against the highest reading of the last MINUTE, not against [peak]. When the
  // peak window is the default ten seconds, peak is drawn from those ten seconds alone —
  // and someone who eased off forty seconds before pressing stop has nothing but decayed
  // values in there, so peak would sit just above the reading at the stop and the check
  // could never fire. It is the fall from the last real high point that gives them away.
  final atStop = _nearest(ordered, recoveryStart, _recoveryStartTolerance);
  final recentHigh = _maxBpmWithin(ordered, recoveryStart, _cooldownLookback);
  if (atStop != null &&
      recentHigh != null &&
      recentHigh - atStop.beatsPerMinute > _cooldownBeforeStopDropBpm) {
    issues.add(HeartRateRecoveryIssue.cooldownBeforeStop);
  }

  // Did the heart rate fall at all? If it was as high or higher at any mark than it was
  // at the peak, then whatever the session end was, it was not the end of the effort —
  // the recording stopped while the rider kept riding. A "recovery" of MINUS four beats
  // is not a small recovery, it is not a recovery, and reporting it as one would be the
  // worst thing this code could do.
  if (marks.any(
      (mark) => mark.dropBpm != null && mark.dropBpm! <= 0)) {
    issues.add(HeartRateRecoveryIssue.heartRateDidNotFall);
  }

  return HeartRateRecoveryReading(
    recoveryStart: recoveryStart,
    peakBpm: peak.bpm,
    peakTime: peak.time,
    peakWindowSeconds: peak.windowSeconds,
    peakWindowSampleCount: peak.sampleCount,
    marks: marks,
    maxHeartRateBpmUsed: maxContext?.bpm,
    maxHeartRateEstimated: maxContext?.estimated ?? false,
    peakFractionOfMax: peakFraction,
    recoverySampleCount: recoverySamples.length,
    quality: _quality(issues, marks),
    issues: issues,
  );
}

HeartRateRecoveryQuality _quality(
  Set<HeartRateRecoveryIssue> issues,
  List<HeartRateRecoveryMark> marks,
) {
  if (issues.contains(HeartRateRecoveryIssue.cooldownBeforeStop) ||
      issues.contains(HeartRateRecoveryIssue.heartRateDidNotFall)) {
    return HeartRateRecoveryQuality.invalid;
  }
  // Samples after the stop, but none of them near enough to any mark to be one. Nothing
  // was measured, so the verdict is nothing measured — not "approximate".
  if (marks.every((mark) => mark.heartRateBpm == null)) {
    return HeartRateRecoveryQuality.noData;
  }
  if (issues.contains(HeartRateRecoveryIssue.submaximalEffort)) {
    return HeartRateRecoveryQuality.notComparable;
  }
  final headline = marks
      .where((mark) => mark.offset == heartRateRecoveryHeadlineOffset)
      .firstOrNull;
  // Without the one-minute mark there is no anchor for a trend, so the reading must not
  // be dressed up as authoritative however good the rest of it looks.
  final headlineMissing = headline?.heartRateBpm == null;
  if (headlineMissing ||
      issues.contains(HeartRateRecoveryIssue.peakFromSingleSample) ||
      issues.contains(HeartRateRecoveryIssue.unknownMaxHeartRate)) {
    return HeartRateRecoveryQuality.approximate;
  }
  return HeartRateRecoveryQuality.clean;
}

/// Samples in time order, at most one per instant.
///
/// Two sources (a strap and a watch, both recording) can land a sample on the same
/// instant. Keeping the higher of the two is the conservative choice in both directions:
/// a higher peak is harder to clear the vigour gate with, and a higher recovery reading
/// means a SMALLER reported drop. It also keeps the median gap honest — left in, the
/// duplicates read as zero-second gaps and would mask coarse sampling.
List<HeartRateSample> _ordered(List<HeartRateSample> samples) {
  final byInstant = <int, HeartRateSample>{};
  for (final sample in samples) {
    final key = sample.time.millisecondsSinceEpoch;
    final existing = byInstant[key];
    if (existing == null || sample.beatsPerMinute > existing.beatsPerMinute) {
      byInstant[key] = sample;
    }
  }
  final ordered = byInstant.values.toList()
    ..sort((a, b) => a.time.compareTo(b.time));
  return ordered;
}

class _Peak {
  const _Peak(this.bpm, this.time, this.windowSeconds, this.sampleCount);

  final int bpm;
  final DateTime time;
  final int windowSeconds;
  final int sampleCount;
}

/// The highest heart rate in the hard [_peakWindow] before the stop, or null if nothing
/// sits there.
///
/// A hard ten-second window on purpose: a wider one would let an effort that eased off
/// earlier draw its "peak" from when it was still going, inflating the recovery. The
/// guided test is the only thing that reaches this code now, and monitors sample fast
/// during hard effort, so a sample is there.
_Peak? _peak(List<HeartRateSample> ordered, DateTime recoveryStart) {
  final start = recoveryStart.subtract(_peakWindow);
  final inWindow = ordered
      .where((sample) =>
          !sample.time.isBefore(start) && !sample.time.isAfter(recoveryStart))
      .toList();
  if (inWindow.isEmpty) return null;
  var best = inWindow.first;
  for (final sample in inWindow) {
    if (sample.beatsPerMinute > best.beatsPerMinute) best = sample;
  }
  return _Peak(
    best.beatsPerMinute,
    best.time,
    _peakWindow.inSeconds,
    inWindow.length,
  );
}

/// The highest reading in the [lookback] before [recoveryStart], or null if there is
/// nothing there.
int? _maxBpmWithin(
  List<HeartRateSample> ordered,
  DateTime recoveryStart,
  Duration lookback,
) {
  final start = recoveryStart.subtract(lookback);
  int? best;
  for (final sample in ordered) {
    if (sample.time.isBefore(start) || sample.time.isAfter(recoveryStart)) {
      continue;
    }
    if (best == null || sample.beatsPerMinute > best) {
      best = sample.beatsPerMinute;
    }
  }
  return best;
}

HeartRateRecoveryMark _markAt(
  List<HeartRateSample> ordered,
  DateTime recoveryStart,
  Duration offset,
  int peakBpm,
) {
  final target = recoveryStart.add(offset);
  final tolerance = heartRateRecoveryTolerances[offset]!;
  final sample = _nearest(ordered, target, tolerance);
  if (sample == null) {
    return HeartRateRecoveryMark(
      offset: offset,
      heartRateBpm: null,
      dropBpm: null,
      sampleTime: null,
      sampleSkew: null,
    );
  }
  return HeartRateRecoveryMark(
    offset: offset,
    heartRateBpm: sample.beatsPerMinute,
    dropBpm: peakBpm - sample.beatsPerMinute,
    sampleTime: sample.time,
    sampleSkew: sample.time.difference(target).abs(),
  );
}

/// The sample nearest [target], or null if the nearest is further than [tolerance].
///
/// A tie goes to the EARLIER sample: deterministic, and the conservative call, since the
/// earlier sample of a falling curve is the higher one and so reports the smaller drop.
HeartRateSample? _nearest(
  List<HeartRateSample> ordered,
  DateTime target,
  Duration tolerance,
) {
  HeartRateSample? best;
  Duration? bestSkew;
  for (final sample in ordered) {
    final skew = sample.time.difference(target).abs();
    if (skew > tolerance) continue;
    if (bestSkew == null || skew < bestSkew) {
      best = sample;
      bestSkew = skew;
    }
  }
  return best;
}

class _MaxHeartRate {
  const _MaxHeartRate(this.bpm, this.estimated);

  final int bpm;
  final bool estimated;
}

/// What to measure the effort against.
///
/// In order: what the user told us; then the highest we have actually seen, but only if
/// it clears the bar for being a real maximum rather than the ceiling of an easy week
/// ([isObservedMaxHeartRateTrustworthy]); then the age formula; then nothing.
///
/// Nothing is a legitimate outcome, and it must not blank the screen: a user who never
/// filled in a birth year still gets every mark, and only loses the judgement of whether
/// the effort was hard enough to compare.
_MaxHeartRate? _resolveMaxHeartRate({
  required int? profileMaxHeartRateBpm,
  required int? observedMaxHeartRateBpm,
  required int? restingHeartRateBpm,
  required int? ageYears,
}) {
  final profileMax = profileMaxHeartRateBpm;
  if (profileMax != null && profileMax > 0) {
    return _MaxHeartRate(profileMax, false);
  }

  final observed = observedMaxHeartRateBpm;
  if (observed != null) {
    final trustworthy = restingHeartRateBpm != null
        ? isObservedMaxHeartRateTrustworthy(observed, restingHeartRateBpm)
        : observed >= observedMaxHeartRateMinimumBpm;
    if (trustworthy) return _MaxHeartRate(observed, false);
  }

  final age = ageYears;
  if (age != null) {
    // Tanaka (208 - 0.7*age): more accurate across ages than the old 220 - age.
    return _MaxHeartRate(math.max(1, (208 - 0.7 * age).round()), true);
  }

  return null;
}
