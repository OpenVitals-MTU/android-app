import '../../data/repository/contract/vitals_repository.dart';
import '../model/vitals_models.dart';
import '../model/write_permission_status.dart';

/// Asks whether a vitals measurement of [VitalsMeasurementType] may be written.
///
/// Per type, not once for "vitals": a blood-pressure reading and an SpO2 reading
/// need different Health Connect permissions, and a device may well have granted
/// one and not the other.
///
/// The check does not throw — see [WritePermissionStatus]: an entry screen whose
/// permission probe failed must still be able to tell the user what to grant.
class CheckVitalsWritePermissionUseCase {
  const CheckVitalsWritePermissionUseCase(this._vitalsRepository);

  final VitalsRepository _vitalsRepository;

  Future<WritePermissionStatus> call(VitalsMeasurementType type) async {
    final permissions = _vitalsRepository.vitalsWritePermissions(type);
    try {
      return WritePermissionStatus(
        permissions: permissions,
        granted: await _vitalsRepository.hasVitalsWritePermission(type),
      );
    } catch (error) {
      return WritePermissionStatus.failed(permissions, error);
    }
  }
}
