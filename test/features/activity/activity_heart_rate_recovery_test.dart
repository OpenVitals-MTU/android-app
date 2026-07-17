import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/heart_rate_recovery.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/exercise_session_metrics.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/preferences/body_profile.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/domain/usecase/load_activity_detail_use_case.dart';
import 'package:openvitals/features/activity/presentation/activity_detail_screen.dart';
import 'package:openvitals/features/activity/presentation/activity_heart_rate_recovery_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/state/app_providers.dart';

/// Heart-rate recovery on the activity detail screen.
///
/// The measurement itself is proved in test/domain/insights/heart_rate_recovery_test.dart.
/// What is guarded here is the plumbing around it:
///
///  - the recovery needs its OWN read (a bounded window around the cessation mark),
///    separate from the read that feeds the session heart-rate chart;
///  - a recovery that cannot be read costs one card, never the screen;
///  - only a guided test (a workout with a trailing rest segment) shows the card; an
///    ordinary workout shows none.

final DateTime _start = DateTime.utc(2026, 7, 14, 18, 0);

/// When hard effort stopped in the guided test — where the recovery is measured from.
final DateTime _effortEnd = DateTime.utc(2026, 7, 14, 18, 30);

/// The session runs on past the effort while the person rests.
final DateTime _sessionEnd = _effortEnd.add(const Duration(minutes: 6));

DateTime _fromEffort(int seconds) => _effortEnd.add(Duration(seconds: seconds));

/// A guided recovery test: a workout carrying a trailing rest segment from the effort
/// end to the session end. Only such a workout has a recovery to read.
ExerciseData _guidedWorkout() => ExerciseData(
      id: 'w1',
      title: 'Bike',
      exerciseType: 0,
      startTime: _start,
      endTime: _sessionEnd,
      durationMs: _sessionEnd.difference(_start).inMilliseconds,
      source: 'test',
      segments: [
        ExerciseSegmentData(
          startTime: _effortEnd,
          endTime: _sessionEnd,
          segmentType: ExerciseSegmentType.rest,
          repetitions: 0,
        ),
      ],
    );

/// An ordinary workout: no cessation mark, so no recovery is measurable.
ExerciseData _ordinaryWorkout() => ExerciseData(
      id: 'w1',
      title: 'Bike',
      exerciseType: 0,
      startTime: _start,
      endTime: _sessionEnd,
      durationMs: _sessionEnd.difference(_start).inMilliseconds,
      source: 'test',
    );

HeartRateSample _hr(DateTime time, int bpm) =>
    HeartRateSample(time: time, beatsPerMinute: bpm, source: 'strap');

/// A hard effort stopping dead at the effort end, sampled every second through the
/// recovery — a chest strap.
List<HeartRateSample> _strapSamples({int peak = 180}) => [
      for (var t = -120; t <= 0; t++)
        _hr(_fromEffort(t), t == -5 ? peak : peak - 2),
      for (var t = 1; t <= 330; t++)
        _hr(_fromEffort(t), (peak - 2 - (t * 0.35)).round().clamp(110, peak)),
    ];

/// An easy effort: a real but submaximal recovery.
List<HeartRateSample> _easySamples() => [
      for (var t = -120; t <= 0; t++) _hr(_fromEffort(t), 105),
      for (var t = 1; t <= 330; t++)
        _hr(_fromEffort(t), (105 - (t * 0.15)).round().clamp(80, 105)),
    ];

class _FakeActivityRepository implements ActivityRepository {
  _FakeActivityRepository(this.workout);

  final ExerciseData workout;

  @override
  Future<Result<ExerciseData?>> loadWorkout(String id) async => Ok(workout);

  @override
  Future<Result<ExerciseSessionMetrics>> loadWorkoutMetrics(
          DateTime start, DateTime end) async =>
      const Ok(ExerciseSessionMetrics.none);

  @override
  Future<Result<List<SpeedSample>>> loadSpeedSamples(
          DateTime start, DateTime end) async =>
      const Ok(<SpeedSample>[]);

  @override
  Future<Result<List<ActivityCadenceSample>>> loadActivityCadenceSamples(
          DateTime start, DateTime end) async =>
      const Ok(<ActivityCadenceSample>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeHeartRepository implements HeartRepository {
  _FakeHeartRepository({
    this.samples = const <HeartRateSample>[],
    this.recoveryReadFails = false,
    this.observedMaxBpm,
  });

  final List<HeartRateSample> samples;
  final bool recoveryReadFails;
  final int? observedMaxBpm;

  /// Every window the use case asked for, in order.
  final List<({DateTime start, DateTime end})> reads = [];

  @override
  Future<Result<List<HeartRateSample>>> loadHeartRateSamplesInstant(
    DateTime start,
    DateTime end,
  ) async {
    reads.add((start: start, end: end));
    // The recovery read is the narrow one around the cessation mark; the session chart
    // read starts at the session start.
    final isRecoveryRead = start.isAfter(_start);
    if (isRecoveryRead && recoveryReadFails) {
      return Err(UnexpectedFailure(
        'HEART_RATE permission denied',
        cause: StateError('HEART_RATE permission denied'),
      ));
    }
    return Ok(samples
        .where((sample) =>
            !sample.time.isBefore(start) && !sample.time.isAfter(end))
        .toList());
  }

  @override
  Future<Result<List<HeartRateSummary>>> loadDailyHeartRateSummaries(
    LocalDate start,
    LocalDate end,
  ) async {
    final max = observedMaxBpm;
    if (max == null) return const Ok(<HeartRateSummary>[]);
    return Ok([
      HeartRateSummary(date: end, avgBpm: 70, minBpm: 50, maxBpm: max),
    ]);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<ActivityDetailLoadResult?> _load(
  _FakeHeartRepository heart, {
  BodyProfile profile = const BodyProfile(maxHeartRateBpm: 190),
  ExerciseData? workout,
}) async {
  final useCase = LoadActivityDetailUseCase(
    _FakeActivityRepository(workout ?? _guidedWorkout()),
    heart,
  );
  final result = await useCase('w1', profile: profile);
  return result.getOrNull();
}

Future<void> _pumpScreen(
  WidgetTester tester,
  _FakeHeartRepository heart, {
  ExerciseData? workout,
}) async {
  tester.view.physicalSize = const Size(1200, 4000);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);

  SharedPreferences.setMockInitialValues(<String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        unitSystemProvider.overrideWithValue(UnitSystem.metric),
        activityRepositoryProvider
            .overrideWithValue(_FakeActivityRepository(workout ?? _guidedWorkout())),
        heartRepositoryProvider.overrideWithValue(heart),
      ],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: const ActivityDetailScreen(activityId: 'w1'),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  group('LoadActivityDetailUseCase heart-rate recovery', () {
    test('an ordinary workout with no cessation mark is not measured', () async {
      final heart = _FakeHeartRepository(samples: _strapSamples());

      final result = await _load(heart, workout: _ordinaryWorkout());

      expect(result!.heartRateRecovery, HeartRateRecoveryReading.noData);
      // No recovery read at all — only the session chart read.
      expect(heart.reads.every((r) => !r.start.isAfter(_start)), isTrue,
          reason: 'no recovery window means no recovery read');
    });

    test('a guided test reads the recovery on its own window', () async {
      final heart = _FakeHeartRepository(samples: _strapSamples());

      await _load(heart);

      // Two reads: the session chart, then the recovery, and the recovery read is the
      // bounded window around the cessation mark, not the whole session.
      expect(heart.reads, hasLength(2));
      expect(heart.reads.first.start, _start);
      final recovery = heart.reads.last;
      expect(recovery.start.isAfter(_start), isTrue);
      expect(recovery.start, _effortEnd.subtract(const Duration(seconds: 60)));
    });

    test('measures the fall for a strap that kept recording', () async {
      final result = await _load(_FakeHeartRepository(samples: _strapSamples()));

      final reading = result!.heartRateRecovery;
      expect(reading.peakBpm, 180);
      expect(reading.headlineDropBpm, isNotNull);
      expect(reading.quality, HeartRateRecoveryQuality.clean);
    });

    test('a failed recovery read costs the card, not the screen', () async {
      final heart = _FakeHeartRepository(
        samples: _strapSamples(),
        recoveryReadFails: true,
      );

      final result = await _load(heart);

      expect(result, isNotNull, reason: 'the workout must still load');
      expect(result!.heartRateSamples, isNotEmpty);
      expect(result.heartRateRecovery.quality, HeartRateRecoveryQuality.noData);
    });

    test('the observed maximum is only fetched when the user has not set one',
        () async {
      final stated = await _load(
        _FakeHeartRepository(samples: _strapSamples(), observedMaxBpm: 200),
        profile: const BodyProfile(maxHeartRateBpm: 190),
      );
      expect(stated!.heartRateRecovery.maxHeartRateBpmUsed, 190,
          reason: 'a stated maximum outranks an observed one');

      final observed = await _load(
        _FakeHeartRepository(samples: _strapSamples(), observedMaxBpm: 200),
        profile: const BodyProfile(restingHeartRateBpm: 55),
      );
      expect(observed!.heartRateRecovery.maxHeartRateBpmUsed, 200);
      expect(observed.heartRateRecovery.maxHeartRateEstimated, isFalse);
    });
  });

  group('ActivityHeartRateRecoveryCard', () {
    testWidgets('shows the fall after a guided test', (tester) async {
      await _pumpScreen(tester, _FakeHeartRepository(samples: _strapSamples()));

      expect(find.byType(ActivityHeartRateRecoveryCard), findsOneWidget);
      expect(find.text('Heart rate recovery'), findsOneWidget);
      expect(find.text('Peak 180 bpm'), findsOneWidget);
    });

    testWidgets(
        'a watch that stopped recording says so, and shows dashes rather than numbers',
        (tester) async {
      // Dense during the effort, nothing at all afterwards.
      final samples = [
        for (var t = -120; t <= 0; t++) _hr(_fromEffort(t), t == -5 ? 180 : 178),
      ];

      await _pumpScreen(tester, _FakeHeartRepository(samples: samples));

      expect(find.byType(ActivityHeartRateRecoveryCard), findsOneWidget);
      expect(
        find.textContaining('stopped recording heart rate'),
        findsOneWidget,
        reason: 'a blank with no explanation reads as a bug',
      );
      // Six marks, every one of them a dash. Nothing is invented.
      expect(find.text('—'), findsNWidgets(6));
    });

    testWidgets('an ordinary workout shows no card at all', (tester) async {
      await _pumpScreen(
        tester,
        _FakeHeartRepository(samples: _strapSamples()),
        workout: _ordinaryWorkout(),
      );

      expect(find.byType(ActivityHeartRateRecoveryCard), findsNothing,
          reason: 'only a guided recovery test produces a reading');
    });

    testWidgets('a submaximal guided test still shows the card, flagged', (tester) async {
      // An easy effort against a known observed max: submaximal, but shown (not hidden)
      // with a "not comparable" note.
      await _pumpScreen(
        tester,
        _FakeHeartRepository(samples: _easySamples(), observedMaxBpm: 185),
      );

      expect(find.byType(ActivityHeartRateRecoveryCard), findsOneWidget);
      expect(find.textContaining('not near your maximum'), findsOneWidget);
    });

    testWidgets(
        'with no maximum knowable at all, the card appears and asks for one',
        (tester) async {
      await _pumpScreen(tester, _FakeHeartRepository(samples: _easySamples()));

      expect(find.byType(ActivityHeartRateRecoveryCard), findsOneWidget);
      expect(find.textContaining('Set your maximum heart rate'), findsOneWidget);
    });
  });
}
