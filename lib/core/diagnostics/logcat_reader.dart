import 'package:flutter/services.dart';

/// Platform-channel bridge to the current process's logcat buffer.
///
/// Native counterpart: the `tech.mmarca.openvitals/diagnostics` [MethodChannel]
/// registered in `MainActivity.kt`, which runs `logcat -d --pid <mypid> -v tag`
/// (the `LEVEL/Tag: message` format the [DebugLogSanitizer] regex expects) and
/// returns the raw lines. Privacy sanitizing happens entirely on the Dart side.
///
/// Best-effort: on platforms without the channel (iOS, desktop, tests) the call
/// resolves to `null` so callers degrade gracefully instead of throwing.
class LogcatReader {
  const LogcatReader();

  static const MethodChannel _channel =
      MethodChannel('tech.mmarca.openvitals/diagnostics');

  /// Reads the raw logcat lines for this process, or `null` when the platform
  /// channel is unavailable (non-Android host, or the native method is not
  /// registered — e.g. a release build).
  Future<List<String>?> readCurrentProcessLogcat() async {
    try {
      final raw = await _channel
          .invokeMethod<List<Object?>>('readCurrentProcessLogcat');
      if (raw == null) return null;
      return raw.map((line) => line?.toString() ?? '').toList();
    } on MissingPluginException {
      return null;
    } on PlatformException {
      return null;
    }
  }
}
