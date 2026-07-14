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
/// What is guarded here is the plumbing around it, where the mistakes would be:
///
///  - the recovery needs its OWN read, running past the end of the session, and the
///    session's heart-rate chart must NOT be widened by it;
///  - a recovery that cannot be read costs one card, never the screen;
///  - an easy session shows no card at all.

final DateTime _start = DateTime.utc(2026, 7, 14, 18, 0);
final DateTime _end = DateTime.utc(2026, 7, 14, 18, 30);

DateTime _fromEnd(int seconds) => _end.add(Duration(seconds: seconds));

ExerciseData _workout() => ExerciseData(
      id: 'w1',
      title: 'Bike',
      exerciseType: 0,
      startTime: _start,
      endTime: _end,
      durationMs: _end.difference(_start).inMilliseconds,
      source: 'test',
    );

HeartRateSample _hr(DateTime time, int bpm) =>
    HeartRateSample(time: time, beatsPerMinute: bpm, source: 'strap');

/// A hard effort, stopping dead at the session end, sampled every second through the
/// recovery — a chest strap.
List<HeartRateSample> _strapSamples({int peak = 180}) => [
      for (var t = -120; t <= 0; t++) _hr(_fromEnd(t), t == -5 ? peak : peak - 2),
      for (var t = 1; t <= 330; t++)
        _hr(_fromEnd(t), (peak - 2 - (t * 0.35)).round().clamp(110, peak)),
    ];

/// An easy session: nothing worth calling a recovery.
List<HeartRateSample> _easySamples() => [
      for (var t = -120; t <= 330; t++) _hr(_fromEnd(t), t <= 0 ? 105 : 95),
    ];

class _FakeActivityRepository implements ActivityRepository {
  @override
  Future<Result<ExerciseData?>> loadWorkout(String id) async => Ok(_workout());

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
    // The recovery read is the one that runs past the session end.
    final isRecoveryRead = end.isAfter(_end);
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
}) async {
  final useCase = LoadActivityDetailUseCase(_FakeActivityRepository(), heart);
  final result = await useCase('w1', profile: profile);
  return result.getOrNull();
}

Future<void> _pumpScreen(
  WidgetTester tester,
  _FakeHeartRepository heart,
) async {
  // Tall enough to build the whole list. The recovery card sits below several others,
  // and a ListView does not build what it cannot show — on a default 600x800 surface the
  // card would be absent from the tree whether or not the code put it there.
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
            .overrideWithValue(_FakeActivityRepository()),
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
    test('reads the recovery on its own, WITHOUT widening the session chart', () async {
      final heart = _FakeHeartRepository(samples: _strapSamples());

      final result = await _load(heart);

      // Two reads: the session, then the recovery.
      expect(heart.reads, hasLength(2));
      expect(heart.reads.first.start, _start);
      expect(heart.reads.first.end, _end,
          reason: 'the session read must stay the session');
      expect(heart.reads.last.end.isAfter(_end), isTrue,
          reason: 'the recovery happens after the workout is over');

      // The samples the chart draws are the session's, not the session plus five
      // minutes of the rider sitting down.
      expect(
        result!.heartRateSamples.every((s) => !s.time.isAfter(_end)),
        isTrue,
        reason: 'a recovery tail must never leak into the session heart-rate chart',
      );
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
      final withProfileMax = _FakeHeartRepository(
        samples: _strapSamples(),
        observedMaxBpm: 200,
      );
      final stated = await _load(
        withProfileMax,
        profile: const BodyProfile(maxHeartRateBpm: 190),
      );
      expect(stated!.heartRateRecovery.maxHeartRateBpmUsed, 190,
          reason: 'a stated maximum outranks an observed one');

      final withoutProfileMax = _FakeHeartRepository(
        samples: _strapSamples(),
        observedMaxBpm: 200,
      );
      final observed = await _load(
        withoutProfileMax,
        profile: const BodyProfile(restingHeartRateBpm: 55),
      );
      expect(observed!.heartRateRecovery.maxHeartRateBpmUsed, 200);
      expect(observed.heartRateRecovery.maxHeartRateEstimated, isFalse);
    });
  });

  group('ActivityHeartRateRecoveryCard', () {
    testWidgets('shows the fall after a hard effort', (tester) async {
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
        for (var t = -120; t <= 0; t++) _hr(_fromEnd(t), t == -5 ? 180 : 178),
      ];

      await _pumpScreen(tester, _FakeHeartRepository(samples: samples));

      expect(find.byType(ActivityHeartRateRecoveryCard), findsOneWidget);
      expect(
        find.textContaining('stopped recording heart rate'),
        findsOneWidget,
        reason: 'a blank with no explanation reads as a bug',
      );
      // Seven marks, every one of them a dash. Nothing is invented.
      expect(find.text('—'), findsNWidgets(7));
    });

    testWidgets('an easy session shows no card at all', (tester) async {
      // No maximum is set in the profile, so the effort is judged against the highest
      // heart rate on record from the past three months — 185, from the days this rider
      // went hard. A 105 bpm plod is 57% of that: nothing to recover from.
      await _pumpScreen(
        tester,
        _FakeHeartRepository(samples: _easySamples(), observedMaxBpm: 185),
      );

      expect(find.byType(ActivityHeartRateRecoveryCard), findsNothing,
          reason: 'a walk has no recovery worth reporting, and the card would be '
              'noise on every one of them');
    });

    testWidgets(
        'with no maximum knowable at all, the card appears and asks for one',
        (tester) async {
      // Nothing stated, no birth year, and no heart-rate history to observe a maximum
      // from. Effort cannot be judged, so the easy session is NOT filtered out — the
      // card appears and says what it needs to stop guessing. Rare in practice: anyone
      // with a workout in Health Connect usually has three months of heart rate behind
      // it, which is exactly what the observed maximum is read from.
      await _pumpScreen(tester, _FakeHeartRepository(samples: _easySamples()));

      expect(find.byType(ActivityHeartRateRecoveryCard), findsOneWidget);
      expect(find.textContaining('Set your maximum heart rate'), findsOneWidget);
    });
  });
}
