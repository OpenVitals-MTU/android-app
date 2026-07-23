import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/activity_splits.dart';
import 'package:openvitals/domain/model/exercise_type_traits.dart';

import '../support/boot_container.dart';

/// The activity pipeline, end to end in Dart: the real data source, the real
/// repository, the real domain (dedup, backfills, splits) and the real use case —
/// against 3,593 records derived from an actual Health Connect export.
///
/// Every bug pinned here was found by the user opening a screen. That is the only
/// detector this project had.
void main() {
  test('a workout gets the heart rate that lived inside a 17-hour record',
      () async {
    // The bug that started everything. The samples exist; Health Connect just would
    // not hand them over to a windowed read of the RECORDS. Kotlin recovers them
    // (Tier K proves that); here we prove Dart does not then lose them again on the
    // way up through the repository, the backfill and the use case.
    final h = await bootContainer();
    final workout = h.fixture.swallowedWorkout;

    final samples = (await h.container
            .read(heartRepositoryProvider)
            .loadHeartRateSamplesInstant(
              DateTime.fromMillisecondsSinceEpoch(workout['start']! as int, isUtc: true),
              DateTime.fromMillisecondsSinceEpoch(workout['end']! as int, isUtc: true),
            ))
        .orThrow();

    expect(samples, isNotEmpty,
        reason: 'The workout inside the swallowing record has no heart rate. Either '
            'the fake stopped honouring the host contract, or the repository is '
            'dropping what the host gave it.');
  });

  test('a GPS session gets REAL splits, not evenly-estimated ones', () async {
    // Regression #3. The splits fell back to `estimated` on exactly the activities
    // whose heart rate had vanished — same record-boundary bug, different record
    // type (SpeedRecord). The user reported it as "1 km interval data using
    // average", and it was: the app was dividing the total evenly because it could
    // not see the speed samples it actually had.
    final h = await bootContainer();
    final route = h.fixture.routeWorkout;
    final start =
        DateTime.fromMillisecondsSinceEpoch(route['start']! as int, isUtc: true);
    final end =
        DateTime.fromMillisecondsSinceEpoch(route['end']! as int, isUtc: true);

    final workouts = (await h.container
            .read(activityRepositoryProvider)
            // `.toLocal()` per LocalDate.fromDateTime's contract: the repository
            // expands these to LOCAL-day windows, and the UTC calendar date of
            // an instant is a different day east of UTC (at UTC+14 the workout
            // fell on the next local day and this lookup found nothing).
            .loadWorkouts(LocalDate.fromDateTime(start.toLocal()),
                LocalDate.fromDateTime(end.toLocal())))
        .orThrow();
    final workout = workouts.firstWhere((w) => w.id == route['id']);

    final speed = (await h.container
            .read(activityRepositoryProvider)
            .loadSpeedSamples(start, end))
        .orThrow();
    expect(speed, isNotEmpty,
        reason: 'No speed samples for the GPS session, so any split it produces is '
            'necessarily an estimate.');

    final splits = computeActivitySplits(
      workout: workout,
      speedSamples: speed,
      heartRateSamples: const [],
      splitDistanceMeters: 1000,
    );

    expect(splits.source, isNot(SplitSource.estimated),
        reason: 'The splits are still evenly estimated even though the speed '
            'samples are right there. This is the bug the user reported.');
  });

  test('a strength session gets NO splits, however much GPS drift it picked up',
      () async {
    // A phone left on a bench picks up a couple of hundred metres of GPS drift.
    // Health Connect faithfully records it, and the activity page then cut a
    // lifting session into "1.0 km" and "181 m" splits at a 30:29 min/km pace. The
    // distance was real data; the splits were nonsense. Whether an activity HAS
    // splits is a question about its KIND, not about whether a distance exists.
    final h = await bootContainer();
    final workout = h.fixture.swallowedWorkout;
    final start =
        DateTime.fromMillisecondsSinceEpoch(workout['start']! as int, isUtc: true);
    final end =
        DateTime.fromMillisecondsSinceEpoch(workout['end']! as int, isUtc: true);

    final workouts = (await h.container
            .read(activityRepositoryProvider)
            // `.toLocal()` per LocalDate.fromDateTime's contract: the repository
            // expands these to LOCAL-day windows, and the UTC calendar date of
            // an instant is a different day east of UTC (at UTC+14 the workout
            // fell on the next local day and this lookup found nothing).
            .loadWorkouts(LocalDate.fromDateTime(start.toLocal()),
                LocalDate.fromDateTime(end.toLocal())))
        .orThrow();
    final session = workouts.firstWhere((w) => w.id == workout['id']);

    final splits = computeActivitySplits(
      workout: session.copyWith(totalDistanceMeters: 181.0),
      speedSamples: const [],
      heartRateSamples: const [],
      splitDistanceMeters: 1000,
    );

    // Only if this session's type is genuinely non-distance-based. The fixture's
    // types are real Health Connect types from a real export.
    if (!isDistanceBasedExercise(session.exerciseType)) {
      expect(splits.splits, isEmpty,
          reason: 'A session that does not travel was cut into distance splits '
              'from GPS drift.');
    }
  });

  test('sessions keep the provenance that decides dedup and the manual count',
      () async {
    // recordingMethod and lastModifiedTime were declared, rendered, and populated by
    // NOTHING — the Pigeon message never carried them. The manual-entry count was
    // therefore always zero however many manual entries you had, and duplicate
    // resolution fell back to list order because the tie-break compared null to null.
    final h = await bootContainer();
    final hr = h.fixture.swallowingHeartRate;
    final start = DateTime.fromMillisecondsSinceEpoch(hr['start']! as int, isUtc: true);

    final workouts =
        (await h.container.read(activityRepositoryProvider).loadWorkouts(
              LocalDate.fromDateTime(start.subtract(const Duration(days: 7))),
              LocalDate.fromDateTime(start.add(const Duration(days: 7))),
            ))
            .orThrow();

    expect(workouts, isNotEmpty);
    expect(workouts.where((w) => w.recordingMethod != null), isNotEmpty,
        reason: 'No workout carries a recordingMethod, so the activities screen '
            'counts zero manual entries no matter how many there are.');
    expect(workouts.where((w) => w.lastModifiedTime != null), isNotEmpty,
        reason: 'No workout carries a lastModifiedTime, so the dedup tie-break is '
            'always a draw and the survivor is decided by list order.');
  });
}
