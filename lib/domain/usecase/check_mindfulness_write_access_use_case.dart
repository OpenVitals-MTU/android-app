import '../../core/result/result.dart';
import '../../data/repository/contract/mindfulness_repository.dart';

/// What the mindfulness entry screen may do right now.
///
/// Two gates, not one: mindfulness is an *optional* Health Connect feature, so a
/// device can be perfectly healthy and still have nowhere to put a session. That
/// is a different sentence to the user than "you have not granted permission",
/// and it must not be mistaken for one.
class MindfulnessWriteAccess {
  const MindfulnessWriteAccess({
    required this.available,
    required this.permissions,
    required this.granted,
    this.error,
  });

  /// Whether this device's Health Connect supports mindfulness sessions at all.
  final bool available;
  final Set<String> permissions;
  final bool granted;

  /// Non-null when the probe failed rather than returned a verdict.
  final Object? error;
}

/// Establishes whether a mindfulness session can be written, and why not when it
/// cannot.
///
/// Availability is asked first and dominates: an unsupported device reports
/// "unavailable", never "missing permission" — asking for a permission that
/// cannot exist is a dead end. That order is why this is one use case and not a
/// pair of them.
///
/// A failed probe collapses to unavailable-with-an-error rather than throwing:
/// we could not establish that mindfulness works here, and pretending otherwise
/// would let the screen offer a save that cannot succeed.
class CheckMindfulnessWriteAccessUseCase {
  const CheckMindfulnessWriteAccessUseCase(this._mindfulnessRepository);

  final MindfulnessRepository _mindfulnessRepository;

  Future<MindfulnessWriteAccess> call() async {
    final permissions = _mindfulnessRepository.mindfulnessWritePermissions;
    final available = _mindfulnessRepository.isMindfulnessAvailable();
    return switch (await _mindfulnessRepository.hasMindfulnessWritePermission()) {
      Ok(:final value) => MindfulnessWriteAccess(
          available: available,
          permissions: permissions,
          granted: value,
        ),
      Err(:final failure) => MindfulnessWriteAccess(
          available: false,
          permissions: permissions,
          granted: false,
          error: failure.cause ?? failure,
        ),
    };
  }
}
