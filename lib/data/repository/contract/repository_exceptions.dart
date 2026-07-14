/// Thrown when a write/update/delete is attempted without the required Health
/// Connect write permission. Mirrors the Kotlin repositories which throw
/// `SecurityException` / `IllegalStateException` in the same situations.
class MissingHealthPermissionException implements Exception {
  const MissingHealthPermissionException(this.message);

  final String message;

  @override
  String toString() => 'MissingHealthPermissionException: $message';
}

/// `PlatformException.code` raised by the native plugin when Health Connect
/// refuses a call because the app has spent its API-call quota.
///
/// The platform throws `HealthConnectException(ERROR_RATE_LIMIT_EXCEEDED)`, but
/// androidx rewraps it in a bare `IllegalStateException` on the way out, so by the
/// time it reaches Dart the code would otherwise be the useless
/// "IllegalStateException". The plugin digs the real code out of the cause chain
/// and re-raises under this one — see `rateLimitErrorOrNull` in
/// `HealthConnectNativePlugin.kt`, which must stay in step with this string.
///
/// `runCatching` turns it into a `RateLimitFailure`, which callers must treat as
/// "come back later" rather than "bad record": the quota is charged PER CALL (the
/// rejection reads `requested: 1` however many records the call carried) and
/// refills over time, so the very same data writes fine once it has.
const String healthConnectRateLimitedCode = 'HEALTH_CONNECT_RATE_LIMITED';
