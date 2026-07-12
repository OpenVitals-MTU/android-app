import '../../core/result/app_failure.dart';
import '../../core/result/result.dart';
import '../../data/repository/contract/activity_repository.dart';
import '../model/activity_models.dart';

/// The activity could not be written because Health Connect has not granted the
/// permissions this particular record needs.
///
/// A distinct marker, not a bare failure: a bulk import tolerates one bad file
/// and carries on, but a missing permission is not a bad file — it will fail every
/// remaining file in the batch for the same reason, and the user needs to be told
/// that rather than shown a parse error. It travels as the failure's `cause`, so
/// the importer can still tell the two apart.
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
/// A permission refusal comes back as a failure `Result` (with the marker
/// exception as its cause), which is what lets the importer treat it exactly
/// like a malformed file: one failed file in a batch that keeps going.
class WriteImportedActivityUseCase {
  const WriteImportedActivityUseCase(this._activityRepository);

  final ActivityRepository _activityRepository;

  Future<Result<void>> call(ActivityWriteRequest request) async {
    final hasPermission =
        await _activityRepository.hasActivityWritePermissionForRequest(request);
    return hasPermission.flatMap((granted) async {
      if (!granted) {
        return const Err(PermissionFailure(
          'Missing Health Connect activity write permission for this record.',
          cause: MissingActivityWritePermissionException(),
        ));
      }
      final written = await _activityRepository.writeActivityEntry(request);
      return written.map((_) {});
    });
  }
}
