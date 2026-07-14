import 'dart:math' as math;

import '../../core/period/period_load_query.dart';
import '../../core/result/result.dart';
import '../../core/time/local_date.dart';
import '../../data/repository/contract/activity_repository.dart';
import '../../data/repository/contract/heart_repository.dart';
import '../insights/heart_rate_recovery.dart';
import '../model/activity_models.dart';
import '../preferences/body_profile.dart';

/// Sessions shorter than this cannot have had a recovery worth measuring — and reading
/// heart rate for every 90-second entry in a busy month is a lot of Health Connect calls
/// for nothing.
const Duration minimumHeartRateRecoverySessionDuration = Duration(minutes: 5);

/// How many sessions the heart-rate reads are allowed to fan out over at once.
///
/// Each session needs its own ~6-minute window of samples — there is no way to ask for
/// them in bulk, and asking for a year of heart rate in one call would be millions of
/// samples. Eight at a time keeps a month brisk without hammering Health Connect.
const int heartRateRecoveryReadConcurrency = 8;

/// The ceiling on how many sessions a period will look at.
///
/// A year of training is a few hundred sessions and a few hundred reads. Rather than let
/// the screen crawl, the newest are taken and the fact is REPORTED
/// ([HeartRateRecoveryPeriodData.truncated]) — a silently short chart is a chart that
/// lies about what it looked at.
const int maxHeartRateRecoverySessions = 400;

/// One session's recovery, with enough of the session to label it on a chart.
class HeartRateRecoverySessionReading {
  const HeartRateRecoverySessionReading({
    required this.sessionId,
    required this.title,
    required this.exerciseType,
    required this.startTime,
    required this.reading,
  });

  final String sessionId;
  final String? title;
  final int exerciseType;
  final DateTime startTime;
  final HeartRateRecoveryReading reading;
}

class HeartRateRecoveryPeriodData {
  const HeartRateRecoveryPeriodData({
    this.readings = const [],
    this.truncated = false,
  });

  final List<HeartRateRecoverySessionReading> readings;

  /// The period held more sessions than [maxHeartRateRecoverySessions] and only the most
  /// recent were read.
  final bool truncated;

  /// The ones that may be plotted: a real, comparable fall with a one-minute mark in it.
  /// On watch data this is commonly none of them, and the screen has to say so rather
  /// than draw an empty chart.
  List<HeartRateRecoverySessionReading> get comparable =>
      readings.where((entry) => entry.reading.isComparable).toList();
}

/// Every workout in the period, and how the heart rate fell after each one.
///
/// Nothing is stored: each reading is computed on the spot from the heart-rate samples
/// Health Connect already holds, exactly as the single-workout card does — the same pure
/// function, on the same rule for where the recovery began.
class LoadHeartRateRecoveryPeriodUseCase {
  const LoadHeartRateRecoveryPeriodUseCase(
    this._activityRepository,
    this._heartRepository,
  );

  final ActivityRepository _activityRepository;
  final HeartRepository _heartRepository;

  Future<Result<HeartRateRecoveryPeriodData>> call(
    PeriodLoadQuery query, {
    BodyProfile profile = const BodyProfile(),
  }) async {
    final window = query.windows.current;
    final loadedWorkouts =
        await _activityRepository.loadWorkouts(window.start, window.end);

    return loadedWorkouts.flatMap((workouts) async {
      final candidates = workouts
          .where((workout) =>
              workout.endTime.difference(workout.startTime) >=
              minimumHeartRateRecoverySessionDuration)
          .toList()
        ..sort((a, b) => b.startTime.compareTo(a.startTime));

      final truncated = candidates.length > maxHeartRateRecoverySessions;
      final considered = truncated
          ? candidates.take(maxHeartRateRecoverySessions).toList()
          : candidates;
      if (considered.isEmpty) {
        return const Ok(HeartRateRecoveryPeriodData());
      }

      // Only worth asking once for the whole period, and only when the user has not told
      // us their maximum.
      final observedMax = await _observedMaxHeartRate(profile, window.end);

      final readings = <HeartRateRecoverySessionReading>[];
      for (var index = 0;
          index < considered.length;
          index += heartRateRecoveryReadConcurrency) {
        final chunk = considered.skip(index).take(heartRateRecoveryReadConcurrency);
        readings.addAll(
          await Future.wait(
            chunk.map((workout) => _readingFor(workout, profile, observedMax)),
          ),
        );
      }

      readings.sort((a, b) => a.startTime.compareTo(b.startTime));
      return Ok(HeartRateRecoveryPeriodData(
        readings: readings,
        truncated: truncated,
      ));
    });
  }

  Future<HeartRateRecoverySessionReading> _readingFor(
    ExerciseData workout,
    BodyProfile profile,
    int? observedMaxHeartRateBpm,
  ) async {
    final window = heartRateRecoveryWindowFor(workout);
    final samples = (await _heartRepository.loadHeartRateSamplesInstant(
      window.readStart,
      window.readEnd,
    ))
        .getOrNull();

    // A failed or empty read is not an error here: it is the ordinary answer for a watch
    // that stopped recording when the workout ended.
    final reading = (samples == null || samples.isEmpty)
        ? HeartRateRecoveryReading.noData
        : calculateHeartRateRecovery(
            recoveryStart: window.recoveryStart,
            samples: samples,
            profileMaxHeartRateBpm: profile.maxHeartRateBpm,
            restingHeartRateBpm: profile.restingHeartRateBpm,
            ageYears: profile.ageYears(),
            observedMaxHeartRateBpm: observedMaxHeartRateBpm,
            source: window.source,
          );

    return HeartRateRecoverySessionReading(
      sessionId: workout.id,
      title: workout.title,
      exerciseType: workout.exerciseType,
      startTime: workout.startTime,
      reading: reading,
    );
  }

  Future<int?> _observedMaxHeartRate(BodyProfile profile, LocalDate end) async {
    if (profile.maxHeartRateBpm != null) return null;
    final summaries = (await _heartRepository.loadDailyHeartRateSummaries(
      end.minusDays(90),
      end,
    ))
        .getOrNull();
    if (summaries == null || summaries.isEmpty) return null;
    return summaries.map((summary) => summary.maxBpm).reduce(math.max);
  }
}
