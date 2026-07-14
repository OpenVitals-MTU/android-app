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
/// Health Connect has no record type for it, and the app stores no health data of its
/// own, so nothing here is persisted: HRR is DERIVED, on read, from the heart-rate
/// samples Health Connect already holds — the same standing as cardio load or stress.
///
/// It does not matter who wrote those samples. A chest strap the app recorded and a
/// watch that synced afterwards produce the same arithmetic; they differ only in how
/// densely they sampled, and that is reported ([HeartRateRecoveryIssue.coarseSampling])
/// rather than hidden. Which is the whole difficulty: a watch commonly samples every
/// second or so DURING a workout and then reverts to a reading every few minutes the
/// moment it ends — exactly when heart-rate recovery is measured. So a mark for which
/// no sample exists is reported as absent. It is never interpolated, and the drop is
/// never guessed from an average. A number that was not measured is worse than a blank.

/// The marks, in the order they are always returned.
const List<Duration> heartRateRecoveryOffsets = [
  Duration(seconds: 10),
  Duration(seconds: 30),
  Duration(minutes: 1),
  Duration(minutes: 2),
  Duration(minutes: 3),
  Duration(minutes: 4),
  Duration(minutes: 5),
];

/// The headline mark. Not the 10-second one: optical sensors smooth over several
/// seconds, so ten seconds after cessation the reported figure is still substantially
/// the effort heart rate — and the one-minute drop is the only mark with a body of
/// normative literature behind it.
const Duration heartRateRecoveryHeadlineOffset = Duration(minutes: 1);

/// How far from a mark a sample may sit and still be taken as that mark.
///
/// Tight where the curve is steep, loose where it is flat. Heart rate falls fastest
/// immediately after cessation — on the order of 0.5-1.0 bpm per second in the first
/// half minute — so being 10 seconds out at the 10-second mark could cost ~10 bpm,
/// which is most of the number being reported. By two minutes the decay is nearer
/// 0.1-0.2 bpm/s and a 20-second error costs a couple of beats, inside sensor noise.
// Not `const`: Duration overrides `==`, which Dart forbids as a const map key.
final Map<Duration, Duration> heartRateRecoveryTolerances = {
  Duration(seconds: 10): Duration(seconds: 5),
  Duration(seconds: 30): Duration(seconds: 8),
  Duration(minutes: 1): Duration(seconds: 10),
  Duration(minutes: 2): Duration(seconds: 15),
  Duration(minutes: 3): Duration(seconds: 20),
  Duration(minutes: 4): Duration(seconds: 20),
  Duration(minutes: 5): Duration(seconds: 20),
};

/// Windows tried, in order, when looking for the peak behind the stop. See
/// [HeartRateRecoveryIssue.peakWindowWidened].
const List<Duration> _peakWindows = [
  Duration(seconds: 10),
  Duration(seconds: 30),
  Duration(seconds: 60),
];

/// How far either side of the recovery start a sample may sit and still count as "the
/// heart rate when they stopped", for the cool-down check.
const Duration _recoveryStartTolerance = Duration(seconds: 15);

/// A fall of this much between the last real high point and the stop means the heart
/// rate was ALREADY coming down before the "stop" — they eased off before they pressed
/// the button. Beat-to-beat noise is 3-4 bpm, so 8 clears it without a long baseline.
const int _cooldownBeforeStopDropBpm = 8;

/// How far back the cool-down check looks for that high point.
const Duration _cooldownLookback = Duration(seconds: 60);

/// Above this median gap between samples, the fine marks cannot exist and the coarse
/// ones are approximations.
const int _coarseSamplingGapSeconds = 20;

/// Fraction of maximum heart rate at or above which an effort is near-maximal, and a
/// recovery reading is comparable with another. HRR norms come from graded tests taken
/// to volitional maximum; readings from efforts of unlike intensity say little.
const double _nearMaximalEffortFraction = 0.85;

/// Below this, there is no vigorous effort to recover from — roughly the floor of the
/// conventional vigorous band.
const double _vigorousEffortFraction = 0.70;

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

/// Where the recovery began, and what told us.
enum HeartRateRecoveryStartSource {
  /// A qualifying rest segment at the end of the session — written by the app's own
  /// guided test, which is the only thing that knows the true instant of cessation.
  trailingRestSegment,

  /// Nothing marked it, so the session's end is taken as the moment effort stopped.
  /// True when someone stops their watch the moment they stop; a lie when they walk it
  /// off first, which is what [HeartRateRecoveryIssue.cooldownBeforeStop] catches.
  sessionEnd,
}

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

  /// Nothing in the 10 seconds before the stop, so the peak came from further back.
  peakWindowWidened,

  /// Exactly one sample stood behind the peak; a single spurious reading would be it.
  peakFromSingleSample,

  /// The heart rate was already falling before the stop — they eased off first, so the
  /// "drop" measures the cool-down and flatters them.
  cooldownBeforeStop,

  /// Hard, but not near-maximal. The drop is real; it is not comparable.
  submaximalEffort,

  /// Not a vigorous effort. There is no recovery to speak of.
  effortNotVigorous,

  /// No maximum heart rate could be resolved, so effort could not be judged.
  unknownMaxHeartRate,

  /// Samples too far apart for the fine marks to exist.
  coarseSampling,
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
    @Default(HeartRateRecoveryStartSource.sessionEnd)
    HeartRateRecoveryStartSource source,
    int? peakBpm,
    DateTime? peakTime,
    @Default(0) int peakWindowSeconds,
    @Default(0) int peakWindowSampleCount,
    @Default(<HeartRateRecoveryMark>[]) List<HeartRateRecoveryMark> marks,
    int? maxHeartRateBpmUsed,
    @Default(false) bool maxHeartRateEstimated,
    double? peakFractionOfMax,
    double? medianRecoveryGapSeconds,
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

  /// Whether this reading should be charted as a comparable data point.
  bool get isComparable => quality == HeartRateRecoveryQuality.clean ||
      quality == HeartRateRecoveryQuality.approximate;
}

/// Where to measure from, and what to read, for one Health Connect session.
@freezed
abstract class HeartRateRecoveryWindow with _$HeartRateRecoveryWindow {
  const factory HeartRateRecoveryWindow({
    required DateTime recoveryStart,
    required DateTime readStart,
    required DateTime readEnd,
    required HeartRateRecoveryStartSource source,
  }) = _HeartRateRecoveryWindow;
}

/// The instant effort stopped, for [session], and the window of heart-rate samples that
/// has to be read to measure the recovery from it.
///
/// One rule serves both the app's guided test and a workout that came from a watch: the
/// recovery begins at a qualifying trailing rest segment if the session carries one, and
/// otherwise at the session's end.
///
/// "Qualifying" is doing real work. The app writes a rest segment after every set of a
/// strength session, the last one included, so a bare "ends with a rest segment" test
/// would read every set-based workout as an HRR test with a one-minute recovery. A
/// segment therefore qualifies only if it is at least [_minimumRecoverySegmentDuration]
/// long AND ends within [_trailingSegmentSlack] of the session end.
HeartRateRecoveryWindow heartRateRecoveryWindowFor(ExerciseData session) {
  final sessionEnd = session.endTime;
  var recoveryStart = sessionEnd;
  var source = HeartRateRecoveryStartSource.sessionEnd;

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
    if (source == HeartRateRecoveryStartSource.sessionEnd ||
        segment.startTime.isAfter(recoveryStart)) {
      recoveryStart = segment.startTime;
      source = HeartRateRecoveryStartSource.trailingRestSegment;
    }
  }

  return HeartRateRecoveryWindow(
    recoveryStart: recoveryStart,
    readStart: recoveryStart.subtract(_readHeadPadding),
    readEnd: recoveryStart
        .add(heartRateRecoveryOffsets.last)
        .add(_readTailPadding),
    source: source,
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
  HeartRateRecoveryStartSource source = HeartRateRecoveryStartSource.sessionEnd,
}) {
  final ordered = _ordered(samples);
  if (ordered.isEmpty) return HeartRateRecoveryReading.noData;

  final issues = <HeartRateRecoveryIssue>{};

  final peak = _peak(ordered, recoveryStart);
  if (peak == null) return HeartRateRecoveryReading.noData;
  if (peak.windowSeconds > _peakWindows.first.inSeconds) {
    issues.add(HeartRateRecoveryIssue.peakWindowWidened);
  }
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
      source: source,
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

  final medianGapSeconds = _medianGapSeconds(recoverySamples);
  if (medianGapSeconds != null &&
      medianGapSeconds > _coarseSamplingGapSeconds) {
    issues.add(HeartRateRecoveryIssue.coarseSampling);
  }

  final maxContext = _resolveMaxHeartRate(
    profileMaxHeartRateBpm: profileMaxHeartRateBpm,
    observedMaxHeartRateBpm: observedMaxHeartRateBpm,
    restingHeartRateBpm: restingHeartRateBpm,
    ageYears: ageYears,
  );
  if (maxContext == null) {
    issues.add(HeartRateRecoveryIssue.unknownMaxHeartRate);
  }

  final peakFraction =
      maxContext == null ? null : peak.bpm / maxContext.bpm;
  if (peakFraction != null) {
    if (peakFraction < _vigorousEffortFraction) {
      issues.add(HeartRateRecoveryIssue.effortNotVigorous);
    } else if (peakFraction < _nearMaximalEffortFraction) {
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

  return HeartRateRecoveryReading(
    recoveryStart: recoveryStart,
    source: source,
    peakBpm: peak.bpm,
    peakTime: peak.time,
    peakWindowSeconds: peak.windowSeconds,
    peakWindowSampleCount: peak.sampleCount,
    marks: marks,
    maxHeartRateBpmUsed: maxContext?.bpm,
    maxHeartRateEstimated: maxContext?.estimated ?? false,
    peakFractionOfMax: peakFraction,
    medianRecoveryGapSeconds: medianGapSeconds,
    recoverySampleCount: recoverySamples.length,
    quality: _quality(issues, marks),
    issues: issues,
  );
}

HeartRateRecoveryQuality _quality(
  Set<HeartRateRecoveryIssue> issues,
  List<HeartRateRecoveryMark> marks,
) {
  if (issues.contains(HeartRateRecoveryIssue.effortNotVigorous) ||
      issues.contains(HeartRateRecoveryIssue.cooldownBeforeStop)) {
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
      issues.contains(HeartRateRecoveryIssue.peakWindowWidened) ||
      issues.contains(HeartRateRecoveryIssue.peakFromSingleSample) ||
      issues.contains(HeartRateRecoveryIssue.coarseSampling) ||
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

/// The highest heart rate in the run-up to the stop.
///
/// Ten seconds is what the definition wants and what a strap can give. A watch may have
/// nothing at all in that window, and a hard 10-second rule would then return no-data for
/// almost every workout anybody has ever recorded — the feature would ship permanently
/// empty. So the window widens, and says that it widened.
_Peak? _peak(List<HeartRateSample> ordered, DateTime recoveryStart) {
  for (final window in _peakWindows) {
    final start = recoveryStart.subtract(window);
    final inWindow = ordered
        .where((sample) =>
            !sample.time.isBefore(start) && !sample.time.isAfter(recoveryStart))
        .toList();
    if (inWindow.isEmpty) continue;
    var best = inWindow.first;
    for (final sample in inWindow) {
      if (sample.beatsPerMinute > best.beatsPerMinute) best = sample;
    }
    return _Peak(
      best.beatsPerMinute,
      best.time,
      window.inSeconds,
      inWindow.length,
    );
  }
  return null;
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

double? _medianGapSeconds(List<HeartRateSample> recoverySamples) {
  if (recoverySamples.length < 2) return null;
  final gaps = <double>[];
  for (var index = 1; index < recoverySamples.length; index++) {
    gaps.add(recoverySamples[index]
            .time
            .difference(recoverySamples[index - 1].time)
            .inMilliseconds /
        1000.0);
  }
  gaps.sort();
  final middle = gaps.length ~/ 2;
  return gaps.length.isOdd
      ? gaps[middle]
      : (gaps[middle - 1] + gaps[middle]) / 2.0;
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
    return _MaxHeartRate(math.max(1, 220 - age), true);
  }

  return null;
}
