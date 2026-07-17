/// Error summarising / permission-detection for the Apple Health importer,
/// ported from the Kotlin `AppleHealthImportErrorFormatter.kt`.
///
/// Kotlin uses the JVM exception hierarchy (`SecurityException`, `cause` chain).
/// Dart has no equivalent, so the importer wraps failures in
/// [AppleHealthImportException] (carrying an optional [cause]) and marks
/// permission failures with [AppleHealthImportPermissionException].
library;

/// A failure raised by the importer, optionally wrapping a [cause] (the Dart
/// analogue of a JVM `Throwable.cause` chain).
class AppleHealthImportException implements Exception {
  AppleHealthImportException(this.message, {this.cause});

  final String? message;
  final Object? cause;

  @override
  String toString() =>
      message == null ? runtimeType.toString() : '$runtimeType: $message';
}

/// A permission-denied failure (the analogue of a JVM `SecurityException`).
class AppleHealthImportPermissionException extends AppleHealthImportException {
  AppleHealthImportPermissionException(super.message, {super.cause});
}

class AppleHealthImportErrorFormatter {
  const AppleHealthImportErrorFormatter._();

  static const String _fallbackMessage = 'Apple Health import failed.';

  static String summary(Object error) {
    // Kotlin uses `error::class.java.name` + `error.localizedMessage`. For our own
    // wrapper we keep the `Type: message` shape; for anything else (e.g. a raw
    // `PlatformException` the repository throws) we fall back to `toString()`,
    // which carries the provider's message. Returning only the runtime type here
    // — the previous behaviour — stripped the message from every `insert_failed`
    // diagnostic and `[ERROR]` log line, exactly the lines a bug report needs.
    if (error is AppleHealthImportException) {
      final type = error.runtimeType.toString();
      final message = error.message;
      if (message != null && message.isNotEmpty) return '$type: $message';
      return type.isNotEmpty ? type : _fallbackMessage;
    }
    final text = error.toString();
    return text.isNotEmpty ? text : _fallbackMessage;
  }

  static String details(Object error, [StackTrace? stackTrace]) {
    final buffer = StringBuffer(summary(error));
    // Kotlin's `details` is the full `printStackTrace`. Dart only has a stack when
    // one is handed in (from the `catch (e, s)` site) or when the error is an
    // `Error` (which carries its own `stackTrace`).
    final stack = stackTrace ?? (error is Error ? error.stackTrace : null);
    if (stack != null) {
      buffer.write('\n');
      buffer.write(stack);
    }
    final seen = <Object>{error};
    var cause = _causeOf(error);
    while (cause != null && seen.add(cause)) {
      buffer.write('\nCaused by: ');
      buffer.write(summary(cause));
      cause = _causeOf(cause);
    }
    final text = buffer.toString().trim();
    return text.isEmpty ? summary(error) : text;
  }

  static bool isPermissionDenied(Object error) {
    final seen = <Object>{};
    Object? current = error;
    while (current != null && seen.add(current)) {
      if (current is AppleHealthImportPermissionException) return true;
      current = _causeOf(current);
    }
    return false;
  }

  static Object? _causeOf(Object error) =>
      error is AppleHealthImportException ? error.cause : null;
}
