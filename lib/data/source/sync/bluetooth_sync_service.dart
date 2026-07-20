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
  final _inbound = StreamController<Uint8List>.broadcast();

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
  /// [connectionState] is [SyncConnectionState.connected].
  SyncByteTransport get transport => _BluetoothSyncTransport(_api, _inbound.stream);

  Future<void> dispose() async {
    BluetoothSyncFlutterApi.setUp(null);
    await _api.disconnect();
    await _devices.close();
    await _discoveryFinished.close();
    await _connection.close();
    await _inbound.close();
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

  @override
  Stream<Uint8List> get inbound => _inbound;

  @override
  Future<void> send(Uint8List bytes) => _api.sendBytes(bytes);

  @override
  Future<void> close() => _api.disconnect();
}
