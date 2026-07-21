import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/usecase/save_hydration_entry_use_case.dart';

/// Records the hydration write id it hands out and any rollback it is asked for.
class _FakeHydrationRepository implements HydrationRepository {
  Result<String> writeAnswer = const Ok('client-123');
  final List<String> rolledBack = [];

  @override
  Future<Result<String>> writeHydrationEntry(HydrationWriteRequest request) async =>
      writeAnswer;

  @override
  Future<Result<void>> deleteHydrationEntryByClientRecordId(
    String clientRecordId,
  ) async {
    rolledBack.add(clientRecordId);
    return const Ok(null);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeNutritionRepository implements NutritionRepository {
  Result<String> writeAnswer = const Ok('nutrition-1');
  int writes = 0;

  @override
  Future<Result<String>> writeNutritionEntry(NutritionWriteRequest request) async {
    writes += 1;
    return writeAnswer;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  late _FakeHydrationRepository hydration;
  late _FakeNutritionRepository nutrition;

  setUp(() {
    hydration = _FakeHydrationRepository();
    nutrition = _FakeNutritionRepository();
  });

  Future<HydrationDrinkLogOutcome> log() =>
      SaveHydrationEntryUseCase(hydration, nutrition)(
        rawLiters: 0.25,
        hydrationMultiplier: 1.0,
        nutrientValues: const {NutritionNutrient.protein: 10.0},
        canWriteHydration: true,
        canWriteNutrition: true,
      );

  test('rolls the hydration half back when the nutrition write fails', () async {
    nutrition.writeAnswer = const Err(UnexpectedFailure('nutrition write failed'));

    await expectLater(log(), throwsA(isA<StateError>()));

    // The just-written hydration record is deleted by its clientRecordId, so a
    // retry cannot leave a duplicate hydration entry with no nutrition.
    expect(hydration.rolledBack, ['client-123']);
  });

  test('does not roll back when both halves succeed', () async {
    final outcome = await log();

    expect(outcome, isA<HydrationDrinkLogSuccess>());
    expect(nutrition.writes, 1);
    expect(hydration.rolledBack, isEmpty);
  });
}
