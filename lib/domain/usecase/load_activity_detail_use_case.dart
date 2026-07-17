import 'dart:math' as math;

import '../../core/result/result.dart';
import '../../core/time/local_date.dart';
import '../../data/repository/contract/activity_repository.dart';
import '../../data/repository/contract/heart_repository.dart';
import '../insights/heart_rate_recovery.dart';
import '../model/activity_backfill.dart';
import '../model/activity_models.dart';
import '../model/exercise_session_metrics.dart';
import '../model/heart_models.dart';
import '../preferences/body_profile.dart';

/// How far back to look for the highest heart rate we have actually seen, when the
/// user has not told us their maximum. Daily summaries, not raw samples: one
/// aggregate read, not three months of beats.
const int heartRateRecoveryObservedMaxLookbackDays = 90;

/// One workout, fully reassembled: the session, its samples, and the totals the
/// session record does not itself carry.
class ActivityDetailLoadResult {
  const ActivityDetailLoadResult({
    required this.workout,
    required this.heartRateSamples,
    required this.speedSamples,
    required this.cadenceSamples,
    required this.heartRateRecovery,
  });

  /// The session with its missing metrics filled in — see
  /// [LoadActivityDetailUseCase] for what "missing" means here.
  final ExerciseData workout;
  final List<HeartRateSample> heartRateSamples;
  final List<SpeedSample> speedSamples;
  final List<ActivityCadenceSample> cadenceSamples;

  /// How the heart rate fell after the effort stopped.
  /// [HeartRateRecoveryQuality.noData] when it cannot be known — which is the
  /// common case for a watch that stops recording when the workout ends.
  final HeartRateRecoveryReading heartRateRecovery;
}

/// Loads one activity and puts it back together again.
///
/// An `ExerciseSessionRecord` carries almost nothing. A watch writes the walk as
/// a session, and its steps, distance, calories and elevation as SEPARATE records
/// over the same span — so the session read alone has a duration and little else.
/// That is how a recorded walk came to report "Steps: Not available" directly
/// above a chart of its own step cadence.
///
/// Reassembling it needs two repositories and three fallible reads, in a
/// particular order:
///
/// 1. The session itself. Absent → `Ok(null)`, and the screen says "not found".
/// 2. Health Connect's own sibling-record totals for the session's window. It is
///    the authority, so these are applied FIRST.
/// 3. The samples (heart rate, speed, cadence), which fill whatever is still
///    missing — the averages, and a distance integrated from speed for a device
///    that recorded speed but wrote no DistanceRecord at all.
///
/// Speed, cadence and the session metrics each degrade to empty on failure: a
/// missing permission costs one card, never the screen. The workout itself and
/// the heart-rate read do not degrade — without them there is nothing to show.
///
/// This orchestration used to live in `ActivityDetailViewModel`, which made a
/// view-model responsible for knowing that Health Connect outranks the samples.
class LoadActivityDetailUseCase {
  const LoadActivityDetailUseCase(this._activityRepository, this._heartRepository);

  final ActivityRepository _activityRepository;
  final HeartRepository _heartRepository;

  /// `Ok(null)` when no workout has this id.
  ///
  /// [profile] carries the user's own maximum and resting heart rate, which decide
  /// whether the effort was hard enough for its recovery to mean anything.
  Future<Result<ActivityDetailLoadResult?>> call(
    String activityId, {
    BodyProfile profile = const BodyProfile(),
    LocalDate? today,
  }) async {
    final loadedWorkout = await _activityRepository.loadWorkout(activityId);
    return loadedWorkout.flatMap((workout) async {
      if (workout == null) return const Ok(null);

      final loadedHeartRate = await _heartRepository.loadHeartRateSamplesInstant(
        workout.startTime,
        workout.endTime,
      );
      return loadedHeartRate.flatMap((heartRateSamples) async {
        // A failed speed / cadence / session-metrics read is worth one card,
        // not the screen: each degrades to empty.
        final speedSamples = (await _activityRepository.loadSpeedSamples(
              workout.startTime,
              workout.endTime,
            ))
                .getOrNull() ??
            const <SpeedSample>[];
        final cadenceSamples =
            (await _activityRepository.loadActivityCadenceSamples(
              workout.startTime,
              workout.endTime,
            ))
                    .getOrNull() ??
                const <ActivityCadenceSample>[];
        final sessionMetrics = (await _activityRepository.loadWorkoutMetrics(
              workout.startTime,
              workout.endTime,
            ))
                .getOrNull() ??
            ExerciseSessionMetrics.none;

        final heartRateRecovery = await _heartRateRecovery(
          workout,
          profile,
          today,
        );

        return Ok(ActivityDetailLoadResult(
          // Health Connect first — it is the authority — then the samples, which
          // fill whatever it left null.
          workout: workout
              .withSessionMetricsBackfilled(sessionMetrics)
              .withSampleBackfilledMetrics(
                heartRateSamples: heartRateSamples,
                speedSamples: speedSamples,
                cadenceSamples: cadenceSamples,
              ),
          heartRateSamples: heartRateSamples,
          speedSamples: speedSamples,
          cadenceSamples: cadenceSamples,
          heartRateRecovery: heartRateRecovery,
        ));
      });
    });
  }

  /// How the heart rate fell once the effort stopped.
  ///
  /// Only the guided recovery test is measured: without its trailing rest segment
  /// there is no cessation mark, so [heartRateRecoveryWindowFor] returns null and this
  /// answers [HeartRateRecoveryReading.noData] without spending a read. When there is a
  /// mark, the recovery gets its OWN heart-rate read over the bounded window around it,
  /// separate from the read that feeds the session's heart-rate chart.
  ///
  /// Every read here degrades: a recovery that cannot be worked out costs one card,
  /// never the screen.
  Future<HeartRateRecoveryReading> _heartRateRecovery(
    ExerciseData workout,
    BodyProfile profile,
    LocalDate? today,
  ) async {
    // No cessation mark, no recovery: an ordinary workout gives no guarantee effort
    // stopped, so heart-rate recovery is only measured for the guided test.
    final window = heartRateRecoveryWindowFor(workout);
    if (window == null) return HeartRateRecoveryReading.noData;
    final samples = (await _heartRepository.loadHeartRateSamplesInstant(
      window.readStart,
      window.readEnd,
    ))
        .getOrNull();
    if (samples == null || samples.isEmpty) {
      return HeartRateRecoveryReading.noData;
    }

    return calculateHeartRateRecovery(
      recoveryStart: window.recoveryStart,
      samples: samples,
      profileMaxHeartRateBpm: profile.maxHeartRateBpm,
      restingHeartRateBpm: profile.restingHeartRateBpm,
      ageYears: profile.ageYears(today: today),
      observedMaxHeartRateBpm: await _observedMaxHeartRate(workout, profile),
    );
  }

  /// The highest heart rate on record in the three months before this workout — only
  /// worth asking for when the user has not told us their maximum, since a stated
  /// maximum outranks an observed one.
  Future<int?> _observedMaxHeartRate(
    ExerciseData workout,
    BodyProfile profile,
  ) async {
    if (profile.maxHeartRateBpm != null) return null;

    final end = LocalDate.fromDateTime(workout.endTime);
    final summaries = (await _heartRepository.loadDailyHeartRateSummaries(
      end.minusDays(heartRateRecoveryObservedMaxLookbackDays),
      end,
    ))
        .getOrNull();
    if (summaries == null || summaries.isEmpty) return null;

    return summaries.map((summary) => summary.maxBpm).reduce(math.max);
  }
}
