import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/garmin/garmin_byte_writer.dart';
import 'package:openvitals/devices/garmin/garmin_cobs.dart';
import 'package:openvitals/devices/garmin/garmin_gfdi_frame.dart';
import 'package:openvitals/devices/garmin/garmin_ml_transport.dart';

Uint8List _b(List<int> xs) => Uint8List.fromList(xs);

const int _clientId = 2;
const int _gfdiServiceCode = 1;

/// The control response the watch sends to grant a handle:
/// `[handle 0][REGISTER_ML_RESP][u64 client][u16 service][status][handle][reliable]`
Uint8List _registerResponse({
  int handle = 3,
  int status = 0,
  int serviceCode = _gfdiServiceCode,
  int clientId = _clientId,
}) =>
    (GarminByteWriter()
          ..writeByte(0)
          ..writeByte(1) // REGISTER_ML_RESP
          ..writeLong(clientId)
          ..writeShort(serviceCode)
          ..writeByte(status)
          ..writeByte(handle)
          ..writeByte(0))
        .toBytes();

/// Wraps [frame] as the watch would: COBS, then handle-prefixed packets.
List<Uint8List> _inboundPackets(Uint8List frame, int handle, int chunkSize) {
  final encoded = GarminCobs.encode(frame);
  final packets = <Uint8List>[];
  for (var offset = 0; offset < encoded.length; offset += chunkSize) {
    final end = (offset + chunkSize).clamp(0, encoded.length);
    packets.add((GarminByteWriter()
          ..writeByte(handle)
          ..writeBytes(Uint8List.sublistView(encoded, offset, end)))
        .toBytes());
  }
  return packets;
}

void main() {
  late List<Uint8List> written;
  late List<GarminGfdiFrame> frames;
  late List<String> logs;
  late GarminMlTransport transport;

  setUp(() {
    written = [];
    frames = [];
    logs = [];
    transport = GarminMlTransport(
      write: (packet) async => written.add(packet),
      onFrame: frames.add,
      onLog: logs.add,
    );
  });

  group('opening the GFDI channel', () {
    test('closes stale handles before registering', () async {
      await transport.open();

      expect(written, hasLength(2));
      // Both are 13-byte control packets on handle 0.
      expect(written.every((p) => p.length == 13), isTrue);
      expect(written.every((p) => p[0] == 0), isTrue);
      expect(written[0][1], 5); // CLOSE_ALL_REQ
      expect(written[1][1], 0); // REGISTER_ML_REQ
    });

    test('the register request names GFDI and asks for plain ML', () async {
      await transport.open();

      final register = written[1];
      // [handle][req][u64 client][u16 service][trailing]
      expect(register[2], _clientId); // little-endian u64, low byte first
      expect(register[10] | (register[11] << 8), _gfdiServiceCode);
      expect(register[12], 0, reason: '0 = plain ML; 2 would request reliable');
    });

    test('becomes ready when the watch grants a handle', () async {
      await transport.open();
      expect(transport.isReady, isFalse);

      transport.handleInbound(_registerResponse(handle: 3));

      await transport.ready;
      expect(transport.isReady, isTrue);
    });

    test('a refused registration surfaces as an error, not a hang', () async {
      await transport.open();

      transport.handleInbound(_registerResponse(status: 1));

      await expectLater(transport.ready, throwsA(isA<StateError>()));
    });

    test('ignores control traffic belonging to another client', () async {
      await transport.open();

      transport.handleInbound(_registerResponse(clientId: 99));

      expect(transport.isReady, isFalse);
      expect(logs.any((l) => l.contains('client 99')), isTrue);
    });

    test('sending before the channel opens is a StateError, not a silent drop',
        () async {
      await expectLater(
        transport.sendFrame(GarminGfdiFrame.build(5031, Uint8List(0))),
        throwsA(isA<StateError>()),
      );
    });
  });

  group('sending frames', () {
    setUp(() async {
      await transport.open();
      transport.handleInbound(_registerResponse(handle: 3));
      await transport.ready;
      written.clear();
    });

    test('prefixes every write with the granted handle', () async {
      await transport.sendFrame(GarminGfdiFrame.build(5031, Uint8List(0)));

      expect(written, isNotEmpty);
      expect(written.every((p) => p[0] == 3), isTrue);
    });

    test('a small frame fits one write and round-trips through COBS', () async {
      final frame = GarminGfdiFrame.build(5002, _b([1, 2, 3, 4]));

      await transport.sendFrame(frame);

      expect(written, hasLength(1));
      // Strip the handle byte and COBS-decode: the original frame must return.
      final decoder = GarminCobsDecoder()
        ..addBytes(Uint8List.sublistView(written.single, 1));
      expect(decoder.pull(), frame);
    });

    test('a frame larger than the MTU is split, and reassembles exactly',
        () async {
      // Default MTU gives 20-byte writes, so 19 payload bytes each.
      final frame = GarminGfdiFrame.build(
        5004,
        _b([for (var i = 0; i < 200; i++) (i * 7) & 0xFF]),
      );

      await transport.sendFrame(frame);

      expect(written.length, greaterThan(1));
      expect(written.every((p) => p.length <= 20), isTrue);
      // Concatenate the payloads and decode: byte-identical to what went in.
      final joined = BytesBuilder();
      for (final packet in written) {
        joined.add(Uint8List.sublistView(packet, 1));
      }
      final decoder = GarminCobsDecoder()..addBytes(joined.toBytes());
      expect(decoder.pull(), frame);
    });

    test('a negotiated MTU widens the writes', () async {
      transport.onMtuChanged(515);
      final frame = GarminGfdiFrame.build(
        5004,
        _b([for (var i = 0; i < 200; i++) i & 0xFF]),
      );

      await transport.sendFrame(frame);

      // 512-byte writes now hold the whole thing.
      expect(written, hasLength(1));
    });

    test('MTU is clamped to the spec floor and ceiling', () async {
      transport.onMtuChanged(9999);
      expect(logs.last, contains('maxWrite=512'));
      transport.onMtuChanged(5);
      expect(logs.last, contains('maxWrite=20'));
    });
  });

  group('receiving frames', () {
    setUp(() async {
      await transport.open();
      transport.handleInbound(_registerResponse(handle: 3));
      await transport.ready;
    });

    test('reassembles a frame split across several packets', () {
      final frame = GarminGfdiFrame.build(
        5004,
        _b([for (var i = 0; i < 100; i++) (i * 3) & 0xFF]),
      );

      for (final packet in _inboundPackets(frame, 3, 15)) {
        transport.handleInbound(packet);
      }

      expect(frames, hasLength(1));
      expect(frames.single.messageType, 5004);
      expect(frames.single.payload,
          _b([for (var i = 0; i < 100; i++) (i * 3) & 0xFF]));
    });

    test('emits two frames delivered back to back', () {
      final a = GarminGfdiFrame.build(5024, _b([1]));
      final b = GarminGfdiFrame.build(5101, _b([2]));

      for (final packet in [
        ..._inboundPackets(a, 3, 64),
        ..._inboundPackets(b, 3, 64),
      ]) {
        transport.handleInbound(packet);
      }

      expect(frames.map((f) => f.messageType), [5024, 5101]);
    });

    test('a packet for an unknown handle is dropped, not misrouted', () {
      final frame = GarminGfdiFrame.build(5024, _b([1, 2]));

      for (final packet in _inboundPackets(frame, 7, 64)) {
        transport.handleInbound(packet);
      }

      expect(frames, isEmpty);
      expect(logs.any((l) => l.contains('unknown handle 7')), isTrue);
    });

    test('a corrupt frame is dropped and the stream keeps running', () {
      final bad = GarminGfdiFrame.build(5024, _b([1, 2, 3]));
      bad[4] ^= 0xFF; // Break the CRC.
      final good = GarminGfdiFrame.build(5101, _b([9]));

      for (final packet in _inboundPackets(bad, 3, 64)) {
        transport.handleInbound(packet);
      }
      for (final packet in _inboundPackets(good, 3, 64)) {
        transport.handleInbound(packet);
      }

      // One bad packet must not take the sync down with it.
      expect(logs.any((l) => l.contains('dropped bad frame')), isTrue);
      expect(frames.map((f) => f.messageType), [5101]);
    });

    test('an empty packet is ignored', () {
      transport.handleInbound(Uint8List(0));
      expect(frames, isEmpty);
    });
  });

  test('a full send/receive loop survives the real chunking both ways',
      () async {
    await transport.open();
    transport.handleInbound(_registerResponse(handle: 5));
    await transport.ready;
    written.clear();

    // Send a large frame, then feed its own bytes back as if echoed by the
    // watch — end-to-end proof that chunking and reassembly agree.
    final frame = GarminGfdiFrame.build(
      5004,
      _b([for (var i = 0; i < 500; i++) (i * 11) & 0xFF]),
    );
    await transport.sendFrame(frame);

    for (final packet in written) {
      transport.handleInbound(packet);
    }

    expect(frames, hasLength(1));
    expect(frames.single.payload,
        _b([for (var i = 0; i < 500; i++) (i * 11) & 0xFF]));
  });
}
