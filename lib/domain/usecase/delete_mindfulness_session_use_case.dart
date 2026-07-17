import '../../core/result/result.dart';
import '../../data/repository/contract/mindfulness_repository.dart';

/// Deletes one OpenVitals-authored mindfulness session.
///
/// Only records this app wrote can be deleted, so the caller is expected to have
/// checked `isOpenVitalsEntry` first. Failure propagates: the screen rolls its
/// optimistic removal back.
class DeleteMindfulnessSessionUseCase {
  const DeleteMindfulnessSessionUseCase(this._mindfulnessRepository);

  final MindfulnessRepository _mindfulnessRepository;

  Future<Result<void>> call(String entryId) =>
      _mindfulnessRepository.deleteMindfulnessSessionEntry(entryId);
}
