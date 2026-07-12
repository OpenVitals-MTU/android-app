import '../../core/result/app_failure.dart';

/// Whether a manual-entry screen may write, and which permissions that verdict
/// is about.
///
/// The two travel together because the screen needs both at once: the verdict
/// decides whether the save button works, and the permission set is what the
/// "Grant access" button asks for. They come from different places — the set is
/// static configuration, known without asking anyone, while the grant is a
/// platform round-trip that can fail — and that is exactly why [error] is a field
/// here rather than a thrown exception.
///
/// A probe that threw still names its permissions. Losing them to an exception is
/// how a permission error turns into a dead end: a screen that cannot write, and
/// cannot say what to grant either.
class WritePermissionStatus {
  const WritePermissionStatus({
    required this.permissions,
    required this.granted,
    this.error,
  });

  /// The grant check itself failed. Not granted — an unanswered question is not
  /// a yes — but the permissions are still known.
  const WritePermissionStatus.failed(this.permissions, this.error)
      : granted = false;

  final Set<String> permissions;
  final bool granted;

  /// Non-null when the check failed rather than returned a verdict.
  final AppFailure? error;
}
