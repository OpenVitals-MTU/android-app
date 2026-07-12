/// Thrown when a write/update/delete is attempted without the required Health
/// Connect write permission. Mirrors the Kotlin repositories which throw
/// `SecurityException` / `IllegalStateException` in the same situations.
class MissingHealthPermissionException implements Exception {
  const MissingHealthPermissionException(this.message);

  final String message;

  @override
  String toString() => 'MissingHealthPermissionException: $message';
}
