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
    final type = error.runtimeType.toString();
    final message = _messageOf(error);
    if (message != null && message.isNotEmpty) return '$type: $message';
    if (type.isNotEmpty) return type;
    return _fallbackMessage;
  }

  static String details(Object error, [StackTrace? stackTrace]) {
    final buffer = StringBuffer(summary(error));
    if (stackTrace != null) {
      buffer.write('\n');
      buffer.write(stackTrace);
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

  static String? _messageOf(Object error) =>
      error is AppleHealthImportException ? error.message : null;

  static Object? _causeOf(Object error) =>
      error is AppleHealthImportException ? error.cause : null;
}
