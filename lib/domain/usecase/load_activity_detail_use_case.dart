import '../../core/result/result.dart';
import '../../data/repository/contract/activity_repository.dart';
import '../../data/repository/contract/heart_repository.dart';
import '../model/activity_backfill.dart';
import '../model/activity_models.dart';
import '../model/exercise_session_metrics.dart';
import '../model/heart_models.dart';

/// One workout, fully reassembled: the session, its samples, and the totals the
/// session record does not itself carry.
class ActivityDetailLoadResult {
  const ActivityDetailLoadResult({
    required this.workout,
    required this.heartRateSamples,
    required this.speedSamples,
    required this.cadenceSamples,
  });

  /// The session with its missing metrics filled in — see
  /// [LoadActivityDetailUseCase] for what "missing" means here.
  final ExerciseData workout;
  final List<HeartRateSample> heartRateSamples;
  final List<SpeedSample> speedSamples;
  final List<ActivityCadenceSample> cadenceSamples;
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
  Future<Result<ActivityDetailLoadResult?>> call(String activityId) async {
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
        ));
      });
    });
  }
}
