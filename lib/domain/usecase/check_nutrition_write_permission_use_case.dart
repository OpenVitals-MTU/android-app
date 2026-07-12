import '../../data/repository/contract/nutrition_repository.dart';
import '../model/write_permission_status.dart';

/// Asks whether nutrition records may be written.
///
/// Health Connect has a single write permission for the whole nutrition record,
/// which is why this one takes no argument while its body and vitals siblings
/// take a type: carbohydrates, protein and a named drink's macros are all fields
/// of the same record.
///
/// The check does not throw — see [WritePermissionStatus].
class CheckNutritionWritePermissionUseCase {
  const CheckNutritionWritePermissionUseCase(this._nutritionRepository);

  final NutritionRepository _nutritionRepository;

  Future<WritePermissionStatus> call() async {
    final permissions = _nutritionRepository.nutritionWritePermissions;
    try {
      return WritePermissionStatus(
        permissions: permissions,
        granted: await _nutritionRepository.hasNutritionWritePermission(),
      );
    } catch (error) {
      return WritePermissionStatus.failed(permissions, error);
    }
  }
}
