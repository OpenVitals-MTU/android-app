import '../../data/repository/contract/activity_repository.dart';

/// The permissions an activity form must hold before it can save anything —
/// **synchronously**, because they are a static list, not a question for the
/// platform.
///
/// This is the *baseline* set: what any activity needs. What a specific record
/// needs is a narrower question that depends on the fields it carries, and it is
/// answered per request when the record is actually written (see
/// `WriteImportedActivityUseCase`). The baseline is what the form shows behind its
/// "Grant access" button, before it knows what the user is going to fill in.
class ReadActivityWritePermissionsUseCase {
  const ReadActivityWritePermissionsUseCase(this._activityRepository);

  final ActivityRepository _activityRepository;

  Set<String> call() => _activityRepository.activityWritePermissions();
}
