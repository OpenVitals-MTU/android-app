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
// This plugin is the app's escape hatch for Bluetooth capabilities
// `flutter_blue_plus` cannot reach. It holds NO protocol logic of its own: every
// wire format, handshake, framing and dedup rule lives in pure Dart so it can be
// unit-tested with no Bluetooth at all. Two unrelated features share it because
// they share that one constraint:
//
//   1. RFCOMM byte transport for phone-to-phone Health Connect sync. Makes this
//      phone discoverable, finds nearby phones, opens ONE socket (server or
//      client) and pumps raw chunks. Protocol in `lib/data/source/sync/`.
//   2. CompanionDeviceManager association for onboarding a Garmin watch.
//      `flutter_blue_plus` covers scanning and bonding (`createBond`), but the
//      companion association — which is what earns the app background
//      reconnect priority — is a platform API with no plugin.
//
// Records and GFDI frames alike travel as opaque `Uint8List` payloads.
//
// Bluetooth Classic RFCOMM (not BLE, not Wi-Fi) is deliberate for (1): the app
// ships NO `android.permission.INTERNET`, and any TCP/UDP socket on Android
// requires it. RFCOMM does not, so peer-to-peer transfer preserves the app's
// no-network stance. `flutter_blue_plus` is central-only — it cannot advertise
// or open a server socket — so this native code is the only way to reach RFCOMM
// from Flutter.
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

  // --- CompanionDeviceManager (Garmin watch onboarding) ---------------------
  //
  // An association is what lets the OS keep this app alive to talk to the watch
  // and raises its process priority when the watch comes into range. It is
  // ALWAYS OPTIONAL: the user can decline the system dialog, and the API only
  // exists on API 26+ (presence observation on 31+). Every method below degrades
  // to "not associated" rather than failing, so a watch stays usable either way.

  /// Shows the system `Allow <app> to access <device>?` dialog for [address]
  /// and resolves true once the user allows it.
  ///
  /// Resolves false when the user declines, when the OS is older than API 26,
  /// or when no host Activity is attached. Resolves true immediately — with no
  /// dialog — if an association for [address] already exists, which is also
  /// what makes calling this on every onboarding idempotent.
  ///
  /// [displayName] is only used for logging; the system dialog shows the
  /// device's own advertised name.
  @async
  bool associateCompanionDevice(String address, String? displayName);

  /// Whether this app already holds a companion association for [address].
  /// False on API < 26.
  bool isCompanionAssociated(String address);

  /// Drops the association for [address] (used when the user forgets a watch).
  /// Silent no-op when there is nothing associated or the API is too old.
  void disassociateCompanionDevice(String address);
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
