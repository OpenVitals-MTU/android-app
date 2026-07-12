/// The failure channel of `Result`: a closed hierarchy describing why a data-
/// or domain-layer operation could not produce a value. It replaces thrown
/// exceptions as the error contract between repositories, use-cases and
/// view-models; the original throwable travels along as [cause] so logging and
/// the temporary `orThrow` migration bridge lose nothing.
///
/// UI code never renders an [AppFailure] directly — view-models map it to a
/// `ScreenError` via `toScreenError` at the presentation boundary.
sealed class AppFailure {
  const AppFailure({this.cause, this.stackTrace});

  /// The original thrown object, when this failure wraps one.
  final Object? cause;

  /// The stack trace captured where [cause] was thrown.
  final StackTrace? stackTrace;
}

/// A read or write was attempted without the required Health Connect
/// permission (the `MissingHealthPermissionException` of the throwing era).
class PermissionFailure extends AppFailure {
  const PermissionFailure(this.message, {super.cause, super.stackTrace});

  final String message;

  @override
  String toString() => 'PermissionFailure: $message';
}

/// Health Connect (or its provider) is not available on this device.
class HealthConnectUnavailableFailure extends AppFailure {
  const HealthConnectUnavailableFailure({super.cause, super.stackTrace});

  @override
  String toString() => 'HealthConnectUnavailableFailure';
}

/// The requested record/entity does not exist.
class NotFoundFailure extends AppFailure {
  const NotFoundFailure({super.cause, super.stackTrace});

  @override
  String toString() => 'NotFoundFailure';
}

/// Anything not covered by a more specific failure. [message] carries the
/// original error's rendering, matching what `throwableToScreenError` showed.
class UnexpectedFailure extends AppFailure {
  const UnexpectedFailure(this.message, {super.cause, super.stackTrace});

  final String message;

  @override
  String toString() => 'UnexpectedFailure: $message';
}
