import '../../core/result/result.dart';
import '../../data/repository/contract/vitals_repository.dart';
import '../model/vitals_models.dart';

/// Deletes one OpenVitals-authored vitals measurement.
///
/// The type is part of the request, not a detail the repository can infer: a
/// blood-pressure reading, an SpO2 reading and a temperature are different Health
/// Connect record types that happen to share an id space.
///
/// Only records this app wrote can be deleted, so the caller is expected to have
/// checked `isOpenVitalsEntry` first. Failure propagates: the screen keeps the
/// entry it optimistically dropped.
class DeleteVitalsMeasurementEntryUseCase {
  const DeleteVitalsMeasurementEntryUseCase(this._vitalsRepository);

  final VitalsRepository _vitalsRepository;

  Future<Result<void>> call(VitalsMeasurementType type, String entryId) =>
      _vitalsRepository.deleteVitalsMeasurementEntry(type, entryId);
}
