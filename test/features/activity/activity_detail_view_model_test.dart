import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/usecase/delete_activity_entry_use_case.dart';
import 'package:openvitals/domain/usecase/load_activities_use_case.dart';
import 'package:openvitals/domain/usecase/load_activity_detail_use_case.dart';
import 'package:openvitals/features/activity/application/activity_detail_view_model.dart';

ExerciseData _workout({required String id, required bool owned}) => ExerciseData(
      id: id,
      title: null,
      exerciseType: 56,
      startTime: DateTime(2026, 3, 2, 8),
      endTime: DateTime(2026, 3, 2, 8, 30),
      durationMs: const Duration(minutes: 30).inMilliseconds,
      source: 'test',
      isOpenVitalsEntry: owned,
    );

class _FakeLoadDetail implements LoadActivityDetailUseCase {
  _FakeLoadDetail(this.workout);
  final ExerciseData workout;

  @override
  Future<Result<ActivityDetailLoadResult?>> call(String activityId) async =>
      Ok(ActivityDetailLoadResult(
        workout: workout,
        heartRateSamples: const <HeartRateSample>[],
        speedSamples: const <SpeedSample>[],
        cadenceSamples: const <ActivityCadenceSample>[],
      ));

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeDelete implements DeleteActivityEntryUseCase {
  _FakeDelete(this.answer);
  Result<void> answer;
  final List<String> deletedIds = [];

  @override
  Future<Result<void>> call(String entryId) async {
    deletedIds.add(entryId);
    return answer;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// The detail delete refreshes the list; a no-op load keeps that harmless.
class _FakeLoadActivities implements LoadActivitiesUseCase {
  @override
  Future<Result<ActivitiesLoadResult>> call(PeriodLoadQuery query) async =>
      const Ok(ActivitiesLoadResult(
        workouts: <ExerciseData>[],
        plannedWorkouts: <PlannedExerciseData>[],
        previousWorkouts: <ExerciseData>[],
        baselineWorkouts: <ExerciseData>[],
        overviewDays: <ActivityOverviewDay>[],
        crossDailyRestingHR: [],
      ));

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeDelete delete;
  late ProviderContainer container;
  late NotifierProvider<ActivityDetailViewModel, ActivityDetailState> provider;

  Future<void> boot({required bool owned, Result<void> deleteAnswer =
      const Ok(null)}) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    delete = _FakeDelete(deleteAnswer);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      loadActivityDetailUseCaseProvider
          .overrideWithValue(_FakeLoadDetail(_workout(id: 'w1', owned: owned))),
      deleteActivityEntryUseCaseProvider.overrideWithValue(delete),
      loadActivitiesUseCaseProvider.overrideWithValue(_FakeLoadActivities()),
    ]);
    addTearDown(container.dispose);
    provider = NotifierProvider<ActivityDetailViewModel, ActivityDetailState>(
      () => ActivityDetailViewModel('w1'),
    );
    container.listen(provider, (_, _) {});
    // Let build()'s microtask load run.
    await Future<void>.delayed(Duration.zero);
  }

  test('deletes an owned activity and invokes the pop callback', () async {
    await boot(owned: true);
    var popped = false;

    await container.read(provider.notifier).deleteActivity(() => popped = true);

    expect(delete.deletedIds, ['w1']);
    expect(popped, isTrue);
    expect(container.read(provider).isDeleting, isFalse);
    expect(container.read(provider).error, isNull);
  });

  test('does not delete a foreign activity', () async {
    await boot(owned: false);
    var popped = false;

    await container.read(provider.notifier).deleteActivity(() => popped = true);

    expect(delete.deletedIds, isEmpty);
    expect(popped, isFalse);
  });

  test('keeps the screen and records the error when the delete fails', () async {
    await boot(
      owned: true,
      deleteAnswer: const Err(UnexpectedFailure('Health Connect is gone')),
    );
    var popped = false;

    await container.read(provider.notifier).deleteActivity(() => popped = true);

    expect(delete.deletedIds, ['w1']);
    expect(popped, isFalse);
    final state = container.read(provider);
    expect(state.isDeleting, isFalse);
    expect(state.error, isNotNull);
    expect(state.workout, isNotNull);
  });
}
