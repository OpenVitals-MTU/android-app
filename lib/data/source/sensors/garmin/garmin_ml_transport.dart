import 'dart:async';

import 'package:flutter/foundation.dart';

import 'garmin_byte_reader.dart';
import 'garmin_byte_writer.dart';
import 'garmin_cobs.dart';
import 'garmin_gfdi_frame.dart';
import 'garmin_log.dart';

/// Garmin's multi-link (ML) transport — the V2 layer that carries GFDI.
///
/// Port of Gadgetbridge's `CommunicatorV2` (AGPLv3), narrowed to the one channel
/// a read-only sync needs. **This is the layer a vívoactive 5 requires**: the
/// on-device GATT probe found the multi-link service `6a4e2800` with handle
/// pairs `0x2810/0x2820`…, and no V1 service. V1 watches would need
/// `CommunicatorV1` instead (a single characteristic pair, no handles) — not
/// implemented, because the device we can test says V2.
///
/// The protocol multiplexes several logical services over one characteristic
/// pair. Every packet's first byte is a handle:
///   * handle 0 is the control channel (open/close services),
///   * any other handle belongs to a service opened earlier.
///
/// So the flow is: close everything stale, ask for a handle for the GFDI
/// service, then prefix every GFDI write with the handle we were given and
/// route inbound packets by their leading handle byte.
///
/// Transport-agnostic by construction: it is handed a [write] callback and fed
/// bytes through [handleInbound], so the whole handshake is testable with no
/// Bluetooth.
class GarminMlTransport {
  GarminMlTransport({
    required this.write,
    required this.onFrame,
    this.onLog,
  });

  /// Writes one packet to the send characteristic.
  final Future<void> Function(Uint8List packet) write;

  /// Called with each fully-reassembled GFDI frame.
  final void Function(GarminGfdiFrame frame) onFrame;

  final void Function(String message)? onLog;

  /// The GFDI service's code in the ML service table (`Service.GFDI`).
  static const int _gfdiServiceCode = 1;

  /// The control channel. Registration requests and their responses ride here.
  static const int _controlHandle = 0;

  /// Identifies this client to the watch, which echoes it back on every control
  /// response so several apps can multiplex without confusing each other.
  ///
  /// Deliberately Gadgetbridge's value: it is the one empirically known to be
  /// accepted by a real watch, and an ID the firmware rejects would fail the
  /// registration outright. The cost is that OpenVitals and Gadgetbridge cannot
  /// hold ML sessions with the same watch simultaneously — they would each act
  /// on the other's control responses. Two apps syncing one watch at once is
  /// already broken territory, so proven beats theoretically-tidy here.
  static const int _clientId = 2;

  /// Control request/response codes (`CommunicatorV2.RequestType` ordinals).
  static const int _registerMlReq = 0;
  static const int _registerMlResp = 1;
  static const int _closeAllReq = 5;

  /// Marks an inbound packet as belonging to the reliable (MLR) sub-protocol.
  /// This transport registers non-reliable, so these are not expected.
  static const int _mlrFlagMask = 0x80;

  /// Conservative default: the BLE minimum MTU of 23 minus 3 bytes of ATT
  /// overhead. Raised by [onMtuChanged] once the real MTU is negotiated.
  int _maxWriteSize = 20;

  /// The handle the watch assigned to GFDI, or null before registration.
  int? _gfdiHandle;

  final GarminCobsDecoder _decoder = GarminCobsDecoder();
  final Completer<void> _ready = Completer<void>();

  /// Resolves once the GFDI service has a handle and frames can be sent.
  Future<void> get ready => _ready.future;

  bool get isReady => _gfdiHandle != null;

  /// Applies a negotiated MTU. Same formula as Gadgetbridge's
  /// `calcMaxWriteChunk`: clamp to the spec's 23-byte floor and 512-byte
  /// ceiling, minus the 3-byte ATT write header.
  void onMtuChanged(int mtu) {
    final safeMtu = mtu < 23 ? 23 : mtu;
    final chunk = safeMtu - 3;
    _maxWriteSize = chunk > 512 ? 512 : chunk;
    onLog?.call('[GARMIN-ML] mtu=$mtu maxWrite=$_maxWriteSize');
  }

  /// Opens the GFDI channel: clear any handles left by a previous session, then
  /// request one for GFDI. Completes [ready] when the watch answers.
  Future<void> open() async {
    // A watch that was mid-session (app killed, link dropped) still holds the
    // old handles; registering on top of them fails until they are released.
    await write(_controlPacket(_closeAllReq, serviceCode: 0));
    await write(_controlPacket(
      _registerMlReq,
      serviceCode: _gfdiServiceCode,
      trailing: 0, // 0 = plain ML; 2 would request the reliable (MLR) variant.
    ));
  }

  /// A 13-byte control packet on handle 0:
  /// `[handle 0][request][u64 clientId][u16 serviceCode][trailing]`.
  Uint8List _controlPacket(
    int request, {
    required int serviceCode,
    int trailing = 0,
  }) {
    final writer = GarminByteWriter(13)
      ..writeByte(_controlHandle)
      ..writeByte(request)
      ..writeLong(_clientId)
      ..writeShort(serviceCode)
      ..writeByte(trailing);
    return writer.toBytes();
  }

  /// Sends one GFDI frame: COBS-wrap it, then split into handle-prefixed writes
  /// that each fit a single characteristic write.
  Future<void> sendFrame(Uint8List frame) async {
    final handle = _gfdiHandle;
    if (handle == null) {
      throw StateError('GFDI channel not open — call open() and await ready');
    }
    final payload = GarminCobs.encode(frame);
    // One byte of every write is the handle, so the usable payload is one less.
    final chunkSize = _maxWriteSize - 1;
    for (var offset = 0; offset < payload.length; offset += chunkSize) {
      final end = offset + chunkSize < payload.length
          ? offset + chunkSize
          : payload.length;
      final packet = GarminByteWriter(end - offset + 1)
        ..writeByte(handle)
        ..writeBytes(Uint8List.sublistView(payload, offset, end));
      await write(packet.toBytes());
    }
  }

  /// Feeds one packet from the receive characteristic in.
  void handleInbound(Uint8List packet) {
    if (packet.isEmpty) return;
    final leadingByte = packet[0];

    if ((leadingByte & _mlrFlagMask) != 0) {
      // Reliable-mode traffic. We never registered for it, and the leading byte
      // of a non-MLR handle can legitimately have the high bit set, so fall
      // through rather than dropping — matching Gadgetbridge (see its #5476).
      onLog?.call('[GARMIN-ML] MLR-flagged packet, handle byte '
          '0x${leadingByte.toRadixString(16)}');
    }

    if (leadingByte == _controlHandle) {
      _handleControl(Uint8List.sublistView(packet, 1));
      return;
    }

    if (leadingByte != _gfdiHandle) {
      onLog?.call('[GARMIN-ML] packet for unknown handle $leadingByte');
      return;
    }

    // GFDI payload: feed the COBS decoder and emit whatever frames complete.
    _decoder.addBytes(Uint8List.sublistView(packet, 1));
    for (var raw = _decoder.pull(); raw != null; raw = _decoder.pull()) {
      try {
        onFrame(GarminGfdiFrame.parse(raw));
      } on GarminGfdiFrameException catch (error) {
        // A corrupt frame is survivable: drop it and keep the stream running,
        // rather than tearing down a sync over one bad packet.
        onLog?.call('[GARMIN-ML] dropped bad frame: ${error.message}');
      }
    }
  }

  void _handleControl(Uint8List body) {
    if (body.length < 9) return;
    final reader = GarminByteReader(body);
    final requestType = reader.readByte();
    final clientId = reader.readLong();
    if (clientId != _clientId) {
      // Another app's control traffic on the same watch.
      onLog?.call('[GARMIN-ML] ignoring control for client $clientId');
      return;
    }
    if (requestType != _registerMlResp) return;
    if (reader.remaining < 4) return;

    final serviceCode = reader.readShort();
    final status = reader.readByte();
    final handle = reader.readByte();

    if (serviceCode != _gfdiServiceCode) return;
    if (status != 0) {
      onLog?.call('[GARMIN-ML] GFDI registration refused, status=$status');
      if (!_ready.isCompleted) {
        _ready.completeError(
          StateError('Watch refused the GFDI service registration '
              '(status $status)'),
        );
      }
      return;
    }

    _gfdiHandle = handle;
    onLog?.call('[GARMIN-ML] GFDI open on handle $handle');
    if (!_ready.isCompleted) _ready.complete();
  }

  /// Drops the channel. The watch releases the handle itself when the link goes,
  /// so this only clears local state.
  void close() {
    _gfdiHandle = null;
    if (!_ready.isCompleted) {
      _ready.completeError(StateError('ML transport closed before it opened'));
    }
  }
}

/// Convenience for logging through [debugPrint] without the transport importing
/// Flutter into its own tests.
void garminMlDebugLog(String message) => garminLog(message);
