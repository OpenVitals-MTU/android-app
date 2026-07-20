import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/source/sync/sync_frame.dart';

void main() {
  group('SyncFrame.encode', () {
    test('lays out big-endian length, type byte, then payload', () {
      final frame = SyncFrame(
        SyncFrameType.batch,
        Uint8List.fromList([1, 2, 3]),
      );
      final bytes = frame.encode();
      expect(bytes.length, 5 + 3);
      final view = ByteData.view(bytes.buffer);
      expect(view.getUint32(0), 3);
      expect(view.getUint8(4), SyncFrameType.batch.index);
      expect(bytes.sublist(5), [1, 2, 3]);
    });

    test('encodes an empty payload as a 5-byte header', () {
      final bytes = SyncFrame(SyncFrameType.sendDone, Uint8List(0)).encode();
      expect(bytes.length, 5);
      expect(ByteData.view(bytes.buffer).getUint32(0), 0);
    });
  });

  group('SyncFrameReader', () {
    test('round-trips a single frame', () {
      final reader = SyncFrameReader();
      final frame = SyncFrame(SyncFrameType.hello, Uint8List.fromList([9, 8, 7]));
      final frames = reader.addChunk(frame.encode());
      expect(frames, hasLength(1));
      expect(frames.single.type, SyncFrameType.hello);
      expect(frames.single.payload, [9, 8, 7]);
      expect(reader.bufferedBytes, 0);
    });

    test('reassembles a frame split across many chunks', () {
      final reader = SyncFrameReader();
      final encoded = SyncFrame(
        SyncFrameType.auth,
        Uint8List.fromList(List.generate(300, (i) => i % 256)),
      ).encode();

      final collected = <SyncFrame>[];
      // Feed one byte at a time — the worst case for a byte-stream reader.
      for (final byte in encoded) {
        collected.addAll(reader.addChunk([byte]));
      }
      expect(collected, hasLength(1));
      expect(collected.single.type, SyncFrameType.auth);
      expect(collected.single.payload, hasLength(300));
      expect(reader.bufferedBytes, 0);
    });

    test('splits multiple frames coalesced into one chunk', () {
      final reader = SyncFrameReader();
      final a = SyncFrame(SyncFrameType.batch, Uint8List.fromList([1]));
      final b = SyncFrame(SyncFrameType.batchAck, Uint8List.fromList([2, 2]));
      final c = SyncFrame(SyncFrameType.sendDone, Uint8List(0));
      final blob = BytesBuilder()
        ..add(a.encode())
        ..add(b.encode())
        ..add(c.encode());

      final frames = reader.addChunk(blob.toBytes());
      expect(frames.map((f) => f.type), [
        SyncFrameType.batch,
        SyncFrameType.batchAck,
        SyncFrameType.sendDone,
      ]);
      expect(frames[1].payload, [2, 2]);
    });

    test('holds a partial trailing frame until the rest arrives', () {
      final reader = SyncFrameReader();
      final full = SyncFrame(
        SyncFrameType.batch,
        Uint8List.fromList([5, 6, 7, 8]),
      ).encode();

      final firstHalf = full.sublist(0, 6);
      final secondHalf = full.sublist(6);
      expect(reader.addChunk(firstHalf), isEmpty);
      expect(reader.bufferedBytes, firstHalf.length);

      final frames = reader.addChunk(secondHalf);
      expect(frames, hasLength(1));
      expect(frames.single.payload, [5, 6, 7, 8]);
    });

    test('rejects an unknown frame type byte', () {
      final reader = SyncFrameReader();
      final bogus = Uint8List(6);
      ByteData.view(bogus.buffer)
        ..setUint32(0, 1)
        ..setUint8(4, 250) // no such SyncFrameType
        ..setUint8(5, 0);
      expect(
        () => reader.addChunk(bogus),
        throwsA(isA<SyncFrameFormatException>()),
      );
    });

    test('rejects an oversized length prefix', () {
      final reader = SyncFrameReader();
      final header = Uint8List(5);
      ByteData.view(header.buffer)
        ..setUint32(0, kMaxSyncFramePayload + 1)
        ..setUint8(4, SyncFrameType.batch.index);
      expect(
        () => reader.addChunk(header),
        throwsA(isA<SyncFrameFormatException>()),
      );
    });
  });
}
