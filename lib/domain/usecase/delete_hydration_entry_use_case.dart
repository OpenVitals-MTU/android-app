import '../../core/result/result.dart';
import '../../data/repository/contract/hydration_repository.dart';

/// Deletes one OpenVitals-authored hydration entry.
///
/// The repository also removes the paired nutrition record a drink logged
/// alongside its water (see [HydrationRepository.deleteHydrationEntry]), so a
/// caffeinated drink does not leave its caffeine behind.
///
/// Only records this app wrote can be deleted, so the caller is expected to have
/// checked `isOpenVitalsEntry` first. Failure propagates: the screen rolls its
/// optimistic removal back.
class DeleteHydrationEntryUseCase {
  const DeleteHydrationEntryUseCase(this._hydrationRepository);

  final HydrationRepository _hydrationRepository;

  Future<Result<void>> call(String entryId) =>
      _hydrationRepository.deleteHydrationEntry(entryId);
}
