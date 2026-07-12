import '../../data/repository/contract/vitals_repository.dart';
import '../model/vitals_models.dart';

/// Writes a vitals measurement, or updates one already written.
///
/// As with the body measurements: create and edit are one operation to the entry
/// screen and two to Health Connect, so the branch on [editRecordId] is decided
/// here.
class SaveVitalsMeasurementUseCase {
  const SaveVitalsMeasurementUseCase(this._vitalsRepository);

  final VitalsRepository _vitalsRepository;

  Future<void> call(
    VitalsMeasurementWriteRequest request, {
    String? editRecordId,
  }) async {
    if (editRecordId == null) {
      await _vitalsRepository.writeVitalsMeasurementEntry(request);
    } else {
      await _vitalsRepository.updateVitalsMeasurementEntry(
        editRecordId,
        request,
      );
    }
  }
}
