import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';

import '../ble/ble_uuids.dart';
import 'garmin_gfdi_frame.dart';
import 'garmin_ml_transport.dart';

/// Thrown when the watch cannot be reached or does not expose the V2 transport.
class GarminBleTransportException implements Exception {
  const GarminBleTransportException(this.message);
  final String message;
  @override
  String toString() => 'GarminBleTransportException: $message';
}

/// The one file in the Garmin stack that touches `flutter_blue_plus`.
///
/// Everything above it — [GarminMlTransport], the session, the messages, the
/// framing — moves bytes and is tested with no radio. This connects, finds the
/// multi-link characteristic pair, and wires the two together:
///
///   notify characteristic → [GarminMlTransport.handleInbound]
///   [GarminMlTransport.write] → send characteristic
///
/// Modelled on `BleGattConnection`'s connect/notify handling, minus its
/// reconnect loop: a sync is a bounded operation the user started, so a dropped
/// link should end it and report, not silently redial.
class GarminBleTransport {
  GarminBleTransport({required this.address, this.onLog});

  final String address;
  final void Function(String message)? onLog;

  /// Gadgetbridge asks for 515; Android negotiates down as needed. A bigger MTU
  /// is the single largest factor in sync speed, since every GFDI frame is
  /// chunked to fit one write.
  static const int _desiredMtu = 515;

  static const Duration _connectTimeout = Duration(seconds: 20);

  BluetoothDevice? _device;
  BluetoothCharacteristic? _send;
  StreamSubscription<List<int>>? _notifySub;
  StreamSubscription<BluetoothConnectionState>? _connectionSub;

  GarminMlTransport? _ml;
  bool _closed = false;

  /// Fires when the link drops mid-sync, so the caller can abort cleanly.
  final StreamController<String> _disconnected =
      StreamController<String>.broadcast();
  Stream<String> get onDisconnected => _disconnected.stream;

  /// The open ML channel.
  ///
  /// Lets a caller build its session BEFORE connecting and still bind `send` to
  /// the channel that does not exist yet — the closure resolves this on first
  /// use, by which point [connect] has run. Throws rather than returning null so
  /// a send that somehow beats the connect fails loudly instead of vanishing.
  GarminMlTransport get mlOrThrow {
    final ml = _ml;
    if (ml == null) {
      throw const GarminBleTransportException('GFDI channel is not open');
    }
    return ml;
  }

  void _log(String message) {
    debugPrint(message);
    onLog?.call(message);
  }

  /// Connects, opens the GFDI channel and returns the transport to send on.
  ///
  /// Throws [GarminBleTransportException] when the watch is unreachable or
  /// exposes no V2 characteristic pair — the latter meaning it is a V1 device,
  /// which this app does not implement (see [GarminMlTransport]).
  Future<GarminMlTransport> connect({
    required void Function(GarminGfdiFrame frame) onFrame,
  }) async {
    if (!await FlutterBluePlus.isSupported) {
      throw const GarminBleTransportException('Bluetooth is unavailable');
    }
    final device = BluetoothDevice.fromId(address);
    _device = device;

    try {
      await device.connect(license: License.nonprofit, timeout: _connectTimeout);
    } catch (error) {
      throw GarminBleTransportException('Could not connect: $error');
    }

    _connectionSub = device.connectionState.listen((state) {
      if (state == BluetoothConnectionState.disconnected && !_closed) {
        _log('[GARMIN-BLE] link dropped');
        if (!_disconnected.isClosed) _disconnected.add('link dropped');
      }
    });

    // Best-effort: a refused MTU request just means smaller writes.
    var mtu = 23;
    try {
      await device.requestMtu(_desiredMtu);
      mtu = device.mtuNow;
    } catch (error) {
      _log('[GARMIN-BLE] MTU request failed, using default: $error');
    }

    final services = await device.discoverServices();
    final pair = _findMlPair(services);
    if (pair == null) {
      await _teardown();
      throw const GarminBleTransportException(
        'No Garmin multi-link characteristics — this watch is not V2',
      );
    }
    final (receive, send) = pair;
    _send = send;
    _log('[GARMIN-BLE] using receive=${receive.uuid.str128} '
        'send=${send.uuid.str128} mtu=$mtu');

    final ml = GarminMlTransport(
      write: _writeToCharacteristic,
      onFrame: onFrame,
      onLog: _log,
    )..onMtuChanged(mtu);
    _ml = ml;

    // Subscribe BEFORE opening the channel: the watch's registration response
    // can arrive the instant the request lands, and a late subscription would
    // miss it and hang the handshake.
    _notifySub = receive.onValueReceived.listen(
      (value) => ml.handleInbound(Uint8List.fromList(value)),
    );
    try {
      await receive.setNotifyValue(true);
    } catch (error) {
      await _teardown();
      throw GarminBleTransportException('Could not subscribe: $error');
    }

    await ml.open();
    try {
      await ml.ready.timeout(const Duration(seconds: 15));
    } on TimeoutException {
      await _teardown();
      throw const GarminBleTransportException(
        'Watch did not open the GFDI channel',
      );
    }
    return ml;
  }

  /// The first receive/send pair present on the device, scanning the handle
  /// window in order — the same first-match rule as
  /// `CommunicatorV2.initializeDevice`.
  (BluetoothCharacteristic, BluetoothCharacteristic)? _findMlPair(
    List<BluetoothService> services,
  ) {
    final byUuid = <String, BluetoothCharacteristic>{
      for (final service in services)
        for (final characteristic in service.characteristics)
          characteristic.uuid.str128: characteristic,
    };
    for (var handle = BleUuids.garminMlFirstReceiveHandle;
        handle <= BleUuids.garminMlLastReceiveHandle;
        handle++) {
      final receive = byUuid[BleUuids.garminUuidForHandle(handle)];
      final send = byUuid[BleUuids.garminUuidForHandle(
        handle + BleUuids.garminMlSendHandleOffset,
      )];
      if (receive != null && send != null) return (receive, send);
    }
    return null;
  }

  Future<void> _writeToCharacteristic(Uint8List packet) async {
    final send = _send;
    if (send == null) {
      throw const GarminBleTransportException('Not connected');
    }
    // Write-without-response throughout: the ML layer carries its own framing
    // and the GFDI layer its own acks, so per-write confirmations would only
    // halve throughput on a link that already has to move whole FIT files.
    await send.write(packet, withoutResponse: send.properties.writeWithoutResponse);
  }

  /// Closes the link and releases everything. Idempotent.
  Future<void> close() async {
    _closed = true;
    await _teardown();
    if (!_disconnected.isClosed) await _disconnected.close();
  }

  Future<void> _teardown() async {
    _ml?.close();
    _ml = null;
    _send = null;
    await _notifySub?.cancel();
    _notifySub = null;
    await _connectionSub?.cancel();
    _connectionSub = null;
    try {
      await _device?.disconnect();
    } catch (_) {
      // Already gone.
    }
    _device = null;
  }
}
