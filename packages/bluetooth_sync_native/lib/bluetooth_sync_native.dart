/// Flutter bridge to Android Bluetooth Classic (RFCOMM) for phone-to-phone
/// Health Connect sync.
///
/// This library re-exports the Pigeon-generated [BluetoothSyncHostApi] (Dart →
/// Kotlin calls) and [BluetoothSyncFlutterApi] (Kotlin → Dart events). The
/// plugin moves raw bytes only; the framing/handshake/dedup wire protocol lives
/// in the app under `lib/data/source/sync/`.
///
/// ANDROID-ONLY: on non-Android platforms the underlying platform channel has no
/// host implementation and calls will throw. Guard usage with
/// `defaultTargetPlatform == TargetPlatform.android`.
library;

import 'src/messages.g.dart';

// Export the whole generated surface: the host/flutter APIs plus the typed
// message classes/enums (`SyncDeviceMsg`, `SyncConnectionStateMsg`).
export 'src/messages.g.dart';

/// Thin, app-facing client owning a [BluetoothSyncHostApi] instance.
///
/// Kept minimal: callers use [api] directly, and register a
/// [BluetoothSyncFlutterApi] implementation for inbound events via
/// `BluetoothSyncFlutterApi.setUp(...)`.
class BluetoothSyncNative {
  /// Creates a client, optionally injecting a custom [BluetoothSyncHostApi]
  /// (e.g. a fake in tests).
  BluetoothSyncNative({BluetoothSyncHostApi? api})
    : api = api ?? BluetoothSyncHostApi();

  /// The generated Pigeon host API bound to the default binary messenger.
  final BluetoothSyncHostApi api;
}
