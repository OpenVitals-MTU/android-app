/// Flutter bridge to Android's NotificationListener service, for mirroring phone
/// notifications to a paired Garmin watch.
///
/// This library re-exports the Pigeon-generated [NotificationListenerHostApi]
/// (Dart → Kotlin) and [NotificationListenerFlutterApi] (Kotlin → Dart). The
/// plugin captures and filters only; every byte that reaches the watch is
/// produced in pure Dart under `lib/devices/garmin/`.
///
/// ANDROID-ONLY: on other platforms the underlying platform channel has no host
/// implementation and calls will throw. Guard usage with
/// `defaultTargetPlatform == TargetPlatform.android`.
library;

import 'src/messages.g.dart';

// Export the whole generated surface: the host/flutter APIs plus the typed
// message classes (`NotificationMsg`, `InstalledAppMsg`).
export 'src/messages.g.dart';

/// Thin, app-facing client owning a [NotificationListenerHostApi] instance.
///
/// Kept minimal: callers use [api] directly, and register a
/// [NotificationListenerFlutterApi] implementation for inbound events via
/// `NotificationListenerFlutterApi.setUp(...)`.
class NotificationListenerNative {
  /// Creates a client, optionally injecting a custom API (e.g. a fake in tests).
  NotificationListenerNative({NotificationListenerHostApi? api})
    : api = api ?? NotificationListenerHostApi();

  /// The generated Pigeon host API bound to the default binary messenger.
  final NotificationListenerHostApi api;
}
