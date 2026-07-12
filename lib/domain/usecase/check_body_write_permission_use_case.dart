import '../../data/repository/contract/body_repository.dart';
import '../model/body_models.dart';
import '../model/write_permission_status.dart';

/// Asks whether a body measurement of [BodyMeasurementType] may be written.
///
/// The permission set depends on the type — weight, height and body fat are three
/// different Health Connect record types — so the question cannot be asked once
/// for "body"; it is asked per entry screen.
///
/// The check does not throw. Health Connect's grant lookup is a platform call and
/// it can fail, but the screen still has to render: it needs the permission set
/// (to offer them) even when the verdict is unknown. See [WritePermissionStatus]
/// for why that shape, and not an exception.
class CheckBodyWritePermissionUseCase {
  const CheckBodyWritePermissionUseCase(this._bodyRepository);

  final BodyRepository _bodyRepository;

  Future<WritePermissionStatus> call(BodyMeasurementType type) async {
    final permissions = _bodyRepository.bodyWritePermissions(type);
    try {
      return WritePermissionStatus(
        permissions: permissions,
        granted: await _bodyRepository.hasBodyWritePermission(type),
      );
    } catch (error) {
      return WritePermissionStatus.failed(permissions, error);
    }
  }
}
