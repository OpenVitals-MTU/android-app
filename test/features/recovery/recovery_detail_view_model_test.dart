import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/sleep_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/sleep_score.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/features/recovery/application/recovery_detail_display.dart';
import 'package:openvitals/features/recovery/application/recovery_detail_view_model.dart';

/// Returns whatever it is told to, so the view-model's own behaviour — the
/// display precompute, the failure mapping, the surviving days — is what is
/// under test.
class _FakeSleepRepository implements SleepRepository {
  _FakeSleepRepository(this.answer);

  Result<List<SleepData>> answer;
  int loads = 0;

  @override
  Future<Result<List<SleepData>>> loadSleepSessions(
    LocalDate start,
    LocalDate end,
  ) async {
    loads += 1;
    return answer;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// One night, ending at 07:00 on [date] — the day it delivers.
SleepData _night(LocalDate date, {double hours = 8, List<SleepStage>? stages}) {
  final end = DateTime(date.year, date.month, date.day, 7);
  final durationMs = (hours * 3600000).round();
  final start = end.subtract(Duration(milliseconds: durationMs));
  return SleepData(
    id: 'night-$date-$hours',
    startTime: start,
    endTime: end,
    durationMs: durationMs,
    source: 'test',
    stages: stages ??
        [
          SleepStage(
            startTime: start,
            endTime: end,
            stageType: SleepStage.stageLight,
          ),
        ],
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('buildRecoveryDetailDisplay', () {
    const today = LocalDate(2026, 3, 4);

    test('a day the lookback never reached is blank, not an error', () {
      final display = buildRecoveryDetailDisplay(const <RecoveryDay>[], today);

      expect(display.day.date, today);
      expect(display.day.sessions, isEmpty);
      expect(display.estimate.confidence, SleepScoreConfidence.noData);
      expect(display.mainSleepSession, isNull);
      expect(display.hasScore, isFalse);
    });

    test('the selected day is picked out of the week, and scored', () {
      final yesterday = today.minusDays(1);
      final days = [
        RecoveryDay(date: yesterday, sessions: [_night(yesterday, hours: 5)]),
        RecoveryDay(
          date: today,
          sessions: [_night(today, hours: 8)],
          sleepScore: const SleepScoreEstimate(
            score: 82,
            confidence: SleepScoreConfidence.high,
          ),
        ),
      ];

      final display = buildRecoveryDetailDisplay(days, today);

      expect(display.day.date, today);
      expect(display.estimate.score, 82);
      expect(display.hasScore, isTrue);
      expect(display.mainSleepSession, isNotNull);
      expect(display.mainSleepSession!.durationMs,
          const Duration(hours: 8).inMilliseconds);
    });

    test('the main session of a night is the one with the most sleep in it', () {
      final nap = _night(today, hours: 2);
      final night = _night(today, hours: 7);
      final days = [
        RecoveryDay(date: today, sessions: [nap, night]),
      ];

      final display = buildRecoveryDetailDisplay(days, today);

      expect(display.mainSleepSession!.id, night.id);
    });
  });

  group('RecoveryDetailViewModel', () {
    late _FakeSleepRepository repository;
    late ProviderContainer container;

    /// `build()` kicks off the first load itself (the screens never call it), so
    /// every test drains that one before doing anything of its own.
    Future<RecoveryDetailViewModel> boot(Result<List<SleepData>> answer) async {
      SharedPreferences.setMockInitialValues(const <String, Object>{});
      final prefs = await SharedPreferences.getInstance();
      repository = _FakeSleepRepository(answer);
      container = ProviderContainer(overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        preferencesRepositoryProvider
            .overrideWithValue(PreferencesRepository(prefs)),
        sleepRepositoryProvider.overrideWithValue(repository),
      ]);
      addTearDown(container.dispose);
      container.listen(recoveryDetailProvider, (_, _) {});
      final viewModel = container.read(recoveryDetailProvider.notifier);
      for (var i = 0; i < 5; i++) {
        await Future<void>.delayed(Duration.zero);
      }
      return viewModel;
    }

    test('a loaded week lands with the selected day precomputed', () async {
      final today = LocalDate.now();
      await boot(Ok([_night(today, hours: 8)]));

      final state = container.read(recoveryDetailProvider);
      expect(state.isLoading, isFalse);
      expect(state.error, isNull);
      expect(state.days.length, recoveryLookbackDays);
      // The two detail screens render this; it must exist by the time the load
      // ends, and it must be the day that was asked for.
      expect(state.display, isNotNull);
      expect(state.display!.day.date, today);
      expect(state.display!.mainSleepSession, isNotNull);
    });

    test('a permission failure becomes ScreenErrorPermissionDenied', () async {
      await boot(const Err(PermissionFailure('sleep read')));

      final state = container.read(recoveryDetailProvider);
      expect(state.isLoading, isFalse);
      expect(state.error, const ScreenErrorPermissionDenied());
      // The screens fall back to the error only when there is nothing to show.
      expect(state.days, isEmpty);
      expect(state.display!.hasScore, isFalse);
    });

    test('a failed reload keeps the week already on screen', () async {
      final today = LocalDate.now();
      final viewModel = await boot(Ok([_night(today, hours: 8)]));

      repository.answer = const Err(UnexpectedFailure('the provider hung up'));
      await viewModel.load(today);

      final state = container.read(recoveryDetailProvider);
      expect(state.error, const ScreenErrorMessage('the provider hung up'));
      expect(state.days.length, recoveryLookbackDays);
      expect(state.display!.mainSleepSession, isNotNull);
    });
  });
}
