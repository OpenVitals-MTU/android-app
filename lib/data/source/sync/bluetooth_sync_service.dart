/// The Bluetooth side of phone-to-phone sync: wraps `bluetooth_sync_native` to
/// drive discoverability, discovery, and one RFCOMM connection, and exposes the
/// live connection as a [SyncByteTransport] the protocol layer consumes.
///
/// This owns the single [BluetoothSyncFlutterApi] event sink and fans its events
/// out to typed streams — discovered devices, connection state, and (once
/// connected) the inbound byte stream that backs [BluetoothSyncTransport].
///
/// ANDROID-ONLY. The Dart caller must hold the runtime Bluetooth permissions
/// (SCAN / CONNECT / ADVERTISE) before scanning or connecting.
library;

import 'dart:async';
import 'dart:typed_data';

import 'package:bluetooth_sync_native/bluetooth_sync_native.dart';

import 'sync_transport.dart';

/// A device seen during discovery, surfaced to the pairing UI.
class DiscoveredSyncDevice {
  const DiscoveredSyncDevice({
    required this.address,
    required this.name,
    required this.bonded,
  });

  final String address;
  final String? name;
  final bool bonded;
}

/// High-level RFCOMM connection lifecycle for the UI.
enum SyncConnectionState { idle, connecting, connected, disconnected, failed }

class BluetoothSyncService implements BluetoothSyncFlutterApi {
  BluetoothSyncService({BluetoothSyncHostApi? api})
      : _api = api ?? BluetoothSyncHostApi() {
    BluetoothSyncFlutterApi.setUp(this);
  }

  final BluetoothSyncHostApi _api;

  final _devices = StreamController<DiscoveredSyncDevice>.broadcast();
  final _discoveryFinished = StreamController<void>.broadcast();
  final _connection = StreamController<SyncConnectionState>.broadcast();
  // Single-subscription (NOT broadcast) so inbound bytes that arrive after the
  // socket opens but before the SyncSession attaches its listener are BUFFERED
  // and replayed on listen, instead of being silently dropped. The native reader
  // starts pumping the moment the socket connects — seconds before the session
  // runs — so a broadcast controller here loses the peer's opening frames.
  final _inbound = StreamController<Uint8List>();

  /// Devices found during the current/most-recent discovery.
  Stream<DiscoveredSyncDevice> get devices => _devices.stream;

  /// Fires when a discovery scan window ends.
  Stream<void> get discoveryFinished => _discoveryFinished.stream;

  /// RFCOMM connection lifecycle.
  Stream<SyncConnectionState> get connectionState => _connection.stream;

  Future<bool> isBluetoothSupported() => _api.isBluetoothSupported();
  Future<bool> isBluetoothEnabled() => _api.isBluetoothEnabled();

  /// Prompts to make this phone discoverable for [seconds]; resolves with the
  /// granted window (0 if declined).
  Future<int> requestDiscoverable(int seconds) =>
      _api.requestDiscoverable(seconds);

  /// Starts listening for one inbound RFCOMM connection (host role).
  Future<void> startServer() => _api.startServer();
  Future<void> stopServer() => _api.stopServer();

  Future<void> startDiscovery() => _api.startDiscovery();
  Future<void> cancelDiscovery() => _api.cancelDiscovery();

  /// Connects to [address] (guest role).
  Future<void> connect(String address) => _api.connect(address);

  /// The transport over the live connection. Valid only while
  /// [connectionState] is [SyncConnectionState.connected]. Cached so its send
  /// serialization chain is stable across accesses.
  late final _BluetoothSyncTransport _transport =
      _BluetoothSyncTransport(_api, _inbound.stream);
  SyncByteTransport get transport => _transport;

  Future<void> dispose() async {
    BluetoothSyncFlutterApi.setUp(null);
    await _api.disconnect();
    await _devices.close();
    await _discoveryFinished.close();
    await _connection.close();
    // A never-listened single-subscription controller's close() only completes
    // once listened, so don't await it (mirrors the SyncSession cleanup guard).
    unawaited(_inbound.close());
  }

  // ── BluetoothSyncFlutterApi (events from Kotlin) ──────────────────────────

  @override
  void onDeviceDiscovered(SyncDeviceMsg device) {
    if (!_devices.isClosed) {
      _devices.add(DiscoveredSyncDevice(
        address: device.address,
        name: device.name,
        bonded: device.bonded,
      ));
    }
  }

  @override
  void onDiscoveryFinished() {
    if (!_discoveryFinished.isClosed) _discoveryFinished.add(null);
  }

  @override
  void onConnectionStateChanged(SyncConnectionStateMsg state) {
    // A dropped or failed link MUST end the inbound byte stream. The native side
    // never closes it, and a SyncSession parked in its receiver loop learns of a
    // dead link only via inbound's onDone — without this it hangs forever after
    // the peer disconnects mid-transfer, holding the wake-locked foreground
    // service and spinning the UI with no way out.
    if (state == SyncConnectionStateMsg.disconnected ||
        state == SyncConnectionStateMsg.connectFailed) {
      if (!_inbound.isClosed) unawaited(_inbound.close());
    }
    if (_connection.isClosed) return;
    _connection.add(switch (state) {
      SyncConnectionStateMsg.connected => SyncConnectionState.connected,
      SyncConnectionStateMsg.disconnected => SyncConnectionState.disconnected,
      SyncConnectionStateMsg.connectFailed => SyncConnectionState.failed,
    });
  }

  @override
  void onBytesReceived(Uint8List chunk) {
    if (!_inbound.isClosed) _inbound.add(chunk);
  }
}

/// The [SyncByteTransport] adapter over a live RFCOMM connection.
class _BluetoothSyncTransport implements SyncByteTransport {
  _BluetoothSyncTransport(this._api, this._inbound);

  final BluetoothSyncHostApi _api;
  final Stream<Uint8List> _inbound;
  Future<void> _sendChain = Future<void>.value();

  @override
  Stream<Uint8List> get inbound => _inbound;

  @override
  Future<void> send(Uint8List bytes) {
    // Serialize every outbound write. The session's sender and receiver loops
    // both call send() concurrently over ONE RFCOMM socket; unordered concurrent
    // writes corrupt frames AND let a flood of acks starve a data batch (the
    // small side's batch was sent last and its ack timed out). Chaining sends
    // preserves call order, one write at a time — so a data batch enqueued
    // before the acks goes out first.
    final result = _sendChain.then((_) => _api.sendBytes(bytes));
    _sendChain = result.then((_) {}, onError: (_) {});
    return result;
  }

  @override
  Future<void> close() => _api.disconnect();
}
