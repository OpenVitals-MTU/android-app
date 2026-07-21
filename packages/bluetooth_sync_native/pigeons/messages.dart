// Pigeon contract for the `bluetooth_sync_native` plugin.
//
// This file defines the Flutter <-> Android Bluetooth Classic (RFCOMM) bridge
// used by the phone-to-phone Health Connect sync feature. It is the SINGLE
// SOURCE OF TRUTH for the generated message classes:
//
//   * Dart   -> lib/src/messages.g.dart
//   * Kotlin -> android/src/main/kotlin/tech/mmarca/openvitals/bluetooth_sync_native/Messages.g.kt
//
// Regenerate both after editing this file (run from the plugin directory):
//
//   dart run pigeon --input pigeons/messages.dart
//
// DESIGN NOTE
// -----------
// This plugin moves ONLY BYTES. It knows nothing about health records, the wire
// protocol, framing, gzip, or dedup — all of that lives in pure Dart
// (`lib/data/source/sync/`) so it can be unit-tested over an in-memory pipe with
// no Bluetooth. The plugin's whole job is: make this phone discoverable, find
// nearby phones, open one RFCOMM socket (as server or client), and pump raw
// chunks in and out. Records travel as opaque `Uint8List` payloads.
//
// Bluetooth Classic RFCOMM (not BLE, not Wi-Fi) is deliberate: the app ships NO
// `android.permission.INTERNET`, and any TCP/UDP socket on Android requires it.
// RFCOMM does not, so peer-to-peer transfer preserves the app's no-network
// stance. `flutter_blue_plus` (the app's existing BT dependency) is central-only
// — it cannot advertise or open a server socket — so this native code is the
// only way to reach RFCOMM from Flutter.
import 'package:pigeon/pigeon.dart';

/// A Bluetooth device surfaced during discovery (or a bonded peer).
class SyncDeviceMsg {
  SyncDeviceMsg(this.address, this.name, this.bonded);

  /// The device's MAC address (`BluetoothDevice.getAddress`). Stable key used to
  /// reconnect; never shown to the user directly.
  final String address;

  /// The device's advertised name, or null when the OS has not resolved it yet.
  final String? name;

  /// Whether this device is already bonded (paired) with this phone. A bonded
  /// link is what gives the RFCOMM channel its encryption + MITM resistance, so
  /// the UI can hint when a first-time OS pairing dialog is expected.
  final bool bonded;
}

/// The RFCOMM socket lifecycle, reported from Kotlin as it changes. Mapped to a
/// Dart enum at the boundary so the enum stays a single source of truth in Dart.
enum SyncConnectionStateMsg {
  /// A socket is open and ready to carry bytes (server accepted, or client
  /// connected).
  connected,

  /// The socket closed — either side disconnected, or the link dropped.
  disconnected,

  /// A connection attempt failed before a socket opened (e.g. peer not
  /// listening, out of range, or the OS pairing was declined).
  connectFailed,
}

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'lib/src/messages.g.dart',
    kotlinOut:
        'android/src/main/kotlin/tech/mmarca/openvitals/bluetooth_sync_native/Messages.g.kt',
    kotlinOptions: KotlinOptions(
      package: 'tech.mmarca.openvitals.bluetooth_sync_native',
    ),
    dartPackageName: 'bluetooth_sync_native',
  ),
)

/// Host (Android/Kotlin) API surface backed by `BluetoothAdapter` + RFCOMM
/// sockets. All methods are Android-only; on other platforms the channel has no
/// host implementation and calls throw.
@HostApi()
abstract class BluetoothSyncHostApi {
  /// True when this device has a Bluetooth adapter at all.
  bool isBluetoothSupported();

  /// True when Bluetooth is currently turned on.
  bool isBluetoothEnabled();

  /// Prompts the user (via `ACTION_REQUEST_DISCOVERABLE`) to make this phone
  /// discoverable for [seconds]. Resolves to the granted discoverable window in
  /// seconds (the OS may clamp it), or 0 if the user declined. Requires the
  /// host Activity; needs `BLUETOOTH_ADVERTISE` on API 31+.
  @async
  int requestDiscoverable(int seconds);

  /// Opens an RFCOMM server socket (`listenUsingRfcommWithServiceRecord`) on a
  /// background thread and blocks accepting ONE inbound connection. On accept,
  /// emits [SyncConnectionStateMsg.connected] and starts pumping inbound bytes
  /// via [BluetoothSyncFlutterApi.onBytesReceived]. Resolves once the server is
  /// listening (not once a peer connects).
  @async
  void startServer();

  /// Closes the server socket if still listening (no effect once a peer has been
  /// accepted — use [disconnect] for that).
  void stopServer();

  /// Begins device discovery (`BluetoothAdapter.startDiscovery`). Each found
  /// device is delivered via [BluetoothSyncFlutterApi.onDeviceDiscovered];
  /// completion via [BluetoothSyncFlutterApi.onDiscoveryFinished]. The caller
  /// (Dart) must already hold `BLUETOOTH_SCAN`.
  @async
  void startDiscovery();

  /// Cancels an in-progress discovery. Always call before [connect] — an active
  /// discovery slows and can fail an RFCOMM connect.
  void cancelDiscovery();

  /// Connects to [address] as an RFCOMM client
  /// (`createRfcommSocketToServiceRecord` + `connect`) on a background thread.
  /// Triggers the OS pairing dialog on a first-time (unbonded) peer. On success
  /// emits [SyncConnectionStateMsg.connected] and starts the inbound byte pump;
  /// resolves when the socket is open. A failure emits
  /// [SyncConnectionStateMsg.connectFailed] and rejects.
  @async
  void connect(String address);

  /// Writes [chunk] to the open socket's output stream. Suspends until the write
  /// completes, so the Dart protocol layer gets natural backpressure over the
  /// slow link. Throws if no socket is open.
  @async
  void sendBytes(Uint8List chunk);

  /// Closes the active socket (client or accepted server) and stops both byte
  /// pumps. Idempotent.
  void disconnect();
}

/// Flutter (Dart) API surface the Kotlin side calls to push events up. The Dart
/// transport session implements this and registers it via `.setUp(...)`.
@FlutterApi()
abstract class BluetoothSyncFlutterApi {
  /// A device was found during discovery. May fire multiple times for the same
  /// address as its name resolves; the Dart side dedupes by address.
  void onDeviceDiscovered(SyncDeviceMsg device);

  /// Discovery finished (the OS scan window elapsed or was cancelled).
  void onDiscoveryFinished();

  /// The RFCOMM socket lifecycle changed.
  void onConnectionStateChanged(SyncConnectionStateMsg state);

  /// A chunk of bytes arrived from the peer. Chunks are raw and unframed — the
  /// Dart protocol layer reassembles frames from the byte stream.
  void onBytesReceived(Uint8List chunk);
}
