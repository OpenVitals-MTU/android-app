import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/domain/insights/heart_rate_recovery.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/preferences/body_profile.dart';
import 'package:openvitals/domain/usecase/load_heart_rate_recovery_period_use_case.dart';

/// The heart-rate-recovery history.
///
/// The case that matters most here is the EMPTY one, because for a watch user it is the
/// usual one: the device stops recording heart rate the moment a workout ends, so there
/// is nothing to measure the fall from. The screen has to be able to tell "you did no
/// hard workouts" apart from "you did six and none of them could be measured" — the
/// second is the truth about the data, and dropping those six would make it look like
/// the first.

final LocalDate _today = LocalDate(2026, 7, 14);

DateTime _sessionStart(int dayOffset) =>
    DateTime.utc(2026, 7, 14 - dayOffset, 18, 0);

ExerciseData _workout(int dayOffset, {Duration duration = const Duration(minutes: 40)}) {
  final start = _sessionStart(dayOffset);
  return ExerciseData(
    id: 'w$dayOffset',
    title: 'Ride',
    exerciseType: 8,
    startTime: start,
    endTime: start.add(duration),
    durationMs: duration.inMilliseconds,
    source: 'test',
  );
}

/// A strap: 1Hz right through the recovery. Peak 180, down to 145 at a minute.
List<HeartRateSample> _strapSamples(DateTime sessionEnd) => [
      for (var t = -60; t <= 0; t++)
        HeartRateSample(
          time: sessionEnd.add(Duration(seconds: t)),
          beatsPerMinute: t == -5 ? 180 : 178,
          source: 'strap',
        ),
      for (var t = 1; t <= 330; t++)
        HeartRateSample(
          time: sessionEnd.add(Duration(seconds: t)),
          beatsPerMinute: (178 - t * 0.55).round().clamp(120, 178),
          source: 'strap',
        ),
    ];

/// A watch: dense during the effort, then nothing at all afterwards.
List<HeartRateSample> _watchSamples(DateTime sessionEnd) => [
      for (var t = -300; t <= 0; t += 5)
        HeartRateSample(
          time: sessionEnd.add(Duration(seconds: t)),
          beatsPerMinute: t == -10 ? 175 : 170,
          source: 'watch',
        ),
    ];

class _FakeActivityRepository implements ActivityRepository {
  _FakeActivityRepository(this.workouts);

  final List<ExerciseData> workouts;

  @override
  Future<Result<List<ExerciseData>>> loadWorkouts(
          LocalDate start, LocalDate end) async =>
      Ok(workouts);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeHeartRepository implements HeartRepository {
  _FakeHeartRepository(this.samplesFor);

  final List<HeartRateSample> Function(DateTime end) samplesFor;
  int reads = 0;

  @override
  Future<Result<List<HeartRateSample>>> loadHeartRateSamplesInstant(
    DateTime start,
    DateTime end,
  ) async {
    reads += 1;
    // The window runs past the session end, so recover the session end from it.
    final sessionEnd = end.subtract(const Duration(minutes: 5, seconds: 30));
    return Ok(samplesFor(sessionEnd)
        .where((s) => !s.time.isBefore(start) && !s.time.isAfter(end))
        .toList());
  }

  @override
  Future<Result<List<HeartRateSummary>>> loadDailyHeartRateSummaries(
          LocalDate start, LocalDate end) async =>
      const Ok(<HeartRateSummary>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<HeartRateRecoveryPeriodData> _load(
  List<ExerciseData> workouts,
  List<HeartRateSample> Function(DateTime) samples, {
  _FakeHeartRepository? heart,
}) async {
  final heartRepository = heart ?? _FakeHeartRepository(samples);
  final useCase = LoadHeartRateRecoveryPeriodUseCase(
    _FakeActivityRepository(workouts),
    heartRepository,
  );
  final result = await useCase(
    PeriodLoadQuery(
      range: TimeRange.month,
      anchorDate: _today,
      today: _today,
    ),
    profile: const BodyProfile(maxHeartRateBpm: 190, restingHeartRateBpm: 55),
  );
  return result.getOrNull()!;
}

void main() {
  test('a strap gives a chartable point per workout', () async {
    final data = await _load([_workout(1), _workout(3)], _strapSamples);

    expect(data.readings, hasLength(2));
    expect(data.comparable, hasLength(2));
    for (final entry in data.comparable) {
      expect(entry.reading.headlineDropBpm, isNotNull);
    }
    // Oldest first, so the chart reads left to right.
    expect(
      data.readings.first.startTime.isBefore(data.readings.last.startTime),
      isTrue,
    );
  });

  test(
      'a watch that stops recording gives workouts with NO chartable point — and they '
      'are still counted', () async {
    final data = await _load(
      [_workout(1), _workout(2), _workout(3)],
      _watchSamples,
    );

    // Nothing to plot...
    expect(data.comparable, isEmpty);
    // ...but the three hard workouts are still THERE. Dropping them would leave the
    // screen looking as though the user had not trained, rather than as though their
    // watch had not recorded.
    expect(data.readings, hasLength(3));
    for (final entry in data.readings) {
      expect(entry.reading.quality, HeartRateRecoveryQuality.noData);
    }
  });

  test('a session too short to have a recovery is never even read', () async {
    final heart = _FakeHeartRepository(_strapSamples);
    final data = await _load(
      [
        _workout(1, duration: const Duration(minutes: 2)),
        _workout(2),
      ],
      _strapSamples,
      heart: heart,
    );

    expect(data.readings, hasLength(1),
        reason: 'a two-minute session has no recovery worth reading heart rate for');
    expect(heart.reads, 1, reason: 'and no Health Connect call was spent on it');
  });

  test('a period bigger than the cap says so rather than quietly showing less',
      () async {
    final data = await _load(
      [for (var day = 1; day <= maxHeartRateRecoverySessions + 5; day++) _workout(day)],
      _strapSamples,
    );

    expect(data.readings, hasLength(maxHeartRateRecoverySessions));
    expect(data.truncated, isTrue,
        reason: 'a silently short chart is a chart that lies about what it looked at');
  });
}
