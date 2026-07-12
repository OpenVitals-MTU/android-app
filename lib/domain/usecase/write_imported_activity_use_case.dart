import '../../data/repository/contract/activity_repository.dart';
import '../model/activity_models.dart';

/// The activity could not be written because Health Connect has not granted the
/// permissions this particular record needs.
///
/// A distinct exception, not a bare failure: a bulk import tolerates one bad file
/// and carries on, but a missing permission is not a bad file — it will fail every
/// remaining file in the batch for the same reason, and the user needs to be told
/// that rather than shown a parse error.
class MissingActivityWritePermissionException implements Exception {
  const MissingActivityWritePermissionException();
}

/// Writes an activity assembled from an imported route file.
///
/// The permission check is *per request*, not once for "activities". A route file
/// carries whatever its author recorded — a bare track, or a track with distance,
/// elevation, calories and steps — and each of those is a separate Health Connect
/// record with a separate write permission. A batch that checked a fixed permission
/// set up front would either refuse files it could have written, or write half of
/// one and fail on the rest.
///
/// Throwing (rather than returning false) is what lets the importer treat a
/// permission refusal exactly like a malformed file: one failed file in a batch
/// that keeps going.
class WriteImportedActivityUseCase {
  const WriteImportedActivityUseCase(this._activityRepository);

  final ActivityRepository _activityRepository;

  Future<void> call(ActivityWriteRequest request) async {
    final hasPermission =
        await _activityRepository.hasActivityWritePermissionForRequest(request);
    if (!hasPermission) throw const MissingActivityWritePermissionException();
    await _activityRepository.writeActivityEntry(request);
  }
}
