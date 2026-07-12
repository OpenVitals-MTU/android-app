import '../../core/result/result.dart';
import '../../data/repository/contract/body_repository.dart';
import '../model/body_models.dart';

/// Deletes one OpenVitals-authored body measurement.
///
/// The type is part of the request, not a detail the repository can infer: a
/// weight, a height and a body-fat reading are three different Health Connect
/// record types that happen to share an id space.
///
/// Only records this app wrote can be deleted, so the caller is expected to have
/// checked `isOpenVitalsEntry` first. Failure propagates: the screen rolls its
/// optimistic removal back.
class DeleteBodyMeasurementEntryUseCase {
  const DeleteBodyMeasurementEntryUseCase(this._bodyRepository);

  final BodyRepository _bodyRepository;

  Future<Result<void>> call(BodyMeasurementType type, String entryId) =>
      _bodyRepository.deleteBodyMeasurementEntry(type, entryId);
}
