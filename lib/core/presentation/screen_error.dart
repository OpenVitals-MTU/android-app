/// A UI-facing error classification, mirroring the Kotlin `ScreenError` sealed
/// interface. Localized rendering lives in the UI/l10n layer (the Kotlin
/// `@Composable resolve()` extension is intentionally not ported here).
sealed class ScreenError {
  const ScreenError();
}

class ScreenErrorMessage extends ScreenError {
  const ScreenErrorMessage(this.text);

  final String text;

  @override
  bool operator ==(Object other) =>
      other is ScreenErrorMessage && other.text == text;

  @override
  int get hashCode => text.hashCode;
}

class ScreenErrorNotFound extends ScreenError {
  const ScreenErrorNotFound();
}

class ScreenErrorMissingArgument extends ScreenError {
  const ScreenErrorMissingArgument();
}

class ScreenErrorPermissionDenied extends ScreenError {
  const ScreenErrorPermissionDenied();
}

class ScreenErrorHealthConnectUnavailable extends ScreenError {
  const ScreenErrorHealthConnectUnavailable();
}

const String _defaultFallback = 'Unable to complete the request.';

/// Converts a thrown error into a [ScreenError], rethrowing cancellations so
/// they propagate (mirrors the Kotlin `CancellationException` handling).
ScreenError throwableToScreenError(
  Object throwable, {
  String fallback = _defaultFallback,
}) {
  final message = throwable is Exception || throwable is Error
      ? throwable.toString()
      : throwable.toString();
  final trimmed = message.trim();
  if (trimmed.isNotEmpty && trimmed != 'null') {
    return ScreenErrorMessage(trimmed);
  }
  return ScreenErrorMessage(fallback);
}
