import '../../data/repository/contract/activity_repository.dart';

/// Deletes one OpenVitals-authored workout.
///
/// Only records this app wrote can be deleted — Health Connect refuses to touch
/// another app's records — so the caller is expected to have checked
/// `ExerciseData.isOpenVitalsEntry` first. Failure propagates: the list rolls its
/// optimistic removal back.
class DeleteActivityEntryUseCase {
  const DeleteActivityEntryUseCase(this._activityRepository);

  final ActivityRepository _activityRepository;

  Future<void> call(String entryId) =>
      _activityRepository.deleteActivityEntry(entryId);
}
