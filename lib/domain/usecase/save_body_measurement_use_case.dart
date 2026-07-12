import '../../core/result/result.dart';
import '../../data/repository/contract/body_repository.dart';
import '../model/body_models.dart';

/// Writes a body measurement, or updates one already written.
///
/// Create and edit are the same operation to the caller and two different ones
/// to Health Connect — an insert versus an update of a record OpenVitals owns —
/// so the branch is decided here, on the presence of [editRecordId], rather than
/// duplicated in every entry screen.
class SaveBodyMeasurementUseCase {
  const SaveBodyMeasurementUseCase(this._bodyRepository);

  final BodyRepository _bodyRepository;

  Future<Result<void>> call(
    BodyMeasurementWriteRequest request, {
    String? editRecordId,
  }) async {
    if (editRecordId == null) {
      final written = await _bodyRepository.writeBodyMeasurementEntry(request);
      return written.map((_) {});
    }
    return _bodyRepository.updateBodyMeasurementEntry(editRecordId, request);
  }
}
