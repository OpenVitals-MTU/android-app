import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/garmin/garmin_byte_reader.dart';
import 'package:openvitals/devices/garmin/garmin_byte_writer.dart';
import 'package:openvitals/devices/garmin/garmin_cobs.dart';
import 'package:openvitals/devices/garmin/garmin_crc.dart';
import 'package:openvitals/devices/garmin/garmin_gfdi_frame.dart';

Uint8List _b(List<int> xs) => Uint8List.fromList(xs);

/// Decodes all frames currently completable in [decoder].
List<Uint8List> _drain(GarminCobsDecoder decoder) {
  final out = <Uint8List>[];
  for (var frame = decoder.pull(); frame != null; frame = decoder.pull()) {
    out.add(frame);
  }
  return out;
}

void main() {
  group('GarminCrc', () {
    test('empty data is 0', () {
      expect(GarminCrc.compute(_b([])), 0);
    });

    test('is deterministic and stays within 16 bits', () {
      final data = _b([for (var i = 0; i < 64; i++) (i * 37) & 0xFF]);
      final crc = GarminCrc.compute(data);
      expect(crc, GarminCrc.compute(data));
      expect(crc, inInclusiveRange(0, 0xFFFF));
    });

    test('respects offset and length', () {
      final data = _b([0xAA, 0x01, 0x02, 0x03, 0xBB]);
      // CRC over the middle three bytes must ignore the framing bytes.
      expect(
        GarminCrc.compute(data, offset: 1, length: 3),
        GarminCrc.compute(_b([0x01, 0x02, 0x03])),
      );
    });
  });

  group('GarminByteReader/Writer round-trip', () {
    test('little-endian across every width', () {
      final writer = GarminByteWriter()
        ..writeByte(0x12)
        ..writeShort(0x3456)
        ..writeInt(0x789ABCDE)
        ..writeLong(0x0102030405060708)
        ..writeBytes(_b([0xDE, 0xAD]));
      final reader = GarminByteReader(writer.toBytes());

      expect(reader.readByte(), 0x12);
      expect(reader.readShort(), 0x3456);
      expect(reader.readInt(), 0x789ABCDE);
      expect(reader.readLong(), 0x0102030405060708);
      expect(reader.readBytes(2), _b([0xDE, 0xAD]));
      expect(reader.hasRemaining, isFalse);
    });

    test('the writer grows past its initial capacity', () {
      final writer = GarminByteWriter(4);
      for (var i = 0; i < 100; i++) {
        writer.writeInt(i);
      }
      final reader = GarminByteReader(writer.toBytes());
      for (var i = 0; i < 100; i++) {
        expect(reader.readInt(), i);
      }
    });

    test('patchShort backfills a placeholder in place', () {
      final writer = GarminByteWriter()
        ..writeShort(0)
        ..writeBytes(_b([1, 2, 3, 4]));
      writer.patchShort(0, writer.length);
      expect(GarminByteReader(writer.toBytes()).readShort(), 6);
    });
  });

  group('GarminCobs round-trip', () {
    void expectRoundTrip(List<int> payload) {
      final encoded = GarminCobs.encode(_b(payload));
      // Every frame is bracketed by 0x00 pads.
      expect(encoded.first, 0);
      expect(encoded.last, 0);
      final decoder = GarminCobsDecoder()..addBytes(encoded);
      expect(decoder.pull(), _b(payload));
      expect(decoder.pull(), isNull);
    }

    test('data with no zeros', () {
      expectRoundTrip([1, 2, 3, 4, 5]);
    });

    test('data containing zeros', () {
      expectRoundTrip([1, 0, 2, 0, 0, 3]);
    });

    test('a payload that ends in zero', () {
      // The case Garmin's extra 0x01 group exists for.
      expectRoundTrip([1, 2, 0]);
    });

    test('a payload that starts in zero', () {
      expectRoundTrip([0, 1, 2]);
    });

    test('a run longer than one max group (>254 bytes)', () {
      expectRoundTrip([for (var i = 0; i < 600; i++) (i % 255) + 1]);
    });

    test('a 254-byte zero-free run at the group boundary', () {
      expectRoundTrip([for (var i = 0; i < 254; i++) 0x41]);
    });
  });

  group('GarminCobsDecoder streaming', () {
    test('reassembles a frame split across arbitrary chunks', () {
      final encoded = GarminCobs.encode(_b([9, 0, 8, 7, 0, 6]));
      final decoder = GarminCobsDecoder();
      // Feed one byte at a time: no frame until the trailing delimiter lands.
      for (var i = 0; i < encoded.length; i++) {
        final frame = () {
          decoder.addBytes(Uint8List.sublistView(encoded, i, i + 1));
          return decoder.pull();
        }();
        expect(frame, i == encoded.length - 1 ? _b([9, 0, 8, 7, 0, 6]) : isNull);
      }
    });

    test('pulls two frames concatenated in one buffer', () {
      final decoder = GarminCobsDecoder()
        ..addBytes(GarminCobs.encode(_b([1, 2, 3])))
        ..addBytes(GarminCobs.encode(_b([4, 5, 6])));
      expect(_drain(decoder), [_b([1, 2, 3]), _b([4, 5, 6])]);
    });

    test('resynchronises when the buffer does not start with a pad', () {
      final decoder = GarminCobsDecoder()..addBytes(_b([0x05, 0x06, 0x07, 0x00]));
      expect(decoder.pull(), isNull);
      // A clean frame after the garbage still decodes.
      decoder.addBytes(GarminCobs.encode(_b([1, 2])));
      expect(decoder.pull(), _b([1, 2]));
    });
  });

  group('GarminGfdiFrame', () {
    test('build then parse preserves type and payload', () {
      final payload = _b([0xAA, 0xBB, 0xCC]);
      final frame = GarminGfdiFrame.parse(
        GarminGfdiFrame.build(5024, payload),
      );
      expect(frame.messageType, 5024);
      expect(frame.payload, payload);
    });

    test('the length field equals the whole frame', () {
      final wire = GarminGfdiFrame.build(5031, _b([1, 2, 3, 4]));
      // 2 (len) + 2 (type) + 4 (payload) + 2 (crc).
      expect(wire.length, 10);
      expect(GarminByteReader(wire).readShort(), 10);
    });

    test('a flipped payload byte fails the CRC check', () {
      final wire = GarminGfdiFrame.build(5024, _b([1, 2, 3]));
      wire[4] ^= 0xFF; // Corrupt the first payload byte.
      expect(
        () => GarminGfdiFrame.parse(wire),
        throwsA(isA<GarminGfdiFrameException>()),
      );
    });

    test('a wrong length field is rejected', () {
      final wire = GarminGfdiFrame.build(5024, _b([1, 2, 3]));
      wire[0] = 0xFF; // Bogus length low byte.
      expect(
        () => GarminGfdiFrame.parse(wire),
        throwsA(isA<GarminGfdiFrameException>()),
      );
    });

    test('an incoming status type has its high bit remapped to the 5000 range',
        () {
      // 0x8000 | 0x00 => RESPONSE (5000), per GFDIMessage.parseIncoming.
      final builder = GarminByteWriter()
        ..writeShort(0) // length placeholder
        ..writeShort(0x8000) // status type, high bit set
        ..writeBytes(_b([0x42]));
      builder.patchShort(0, builder.length + 2);
      final crc = GarminCrc.compute(builder.toBytes());
      builder.writeShort(crc);

      final frame = GarminGfdiFrame.parse(builder.toBytes());
      expect(frame.messageType, 5000);
      expect(frame.payload, _b([0x42]));
    });

    test('survives a COBS round-trip (the real transport path)', () {
      final wire = GarminGfdiFrame.build(5002, _b([0, 1, 0, 2, 0]));
      final decoder = GarminCobsDecoder()..addBytes(GarminCobs.encode(wire));
      final decoded = decoder.pull()!;
      final frame = GarminGfdiFrame.parse(decoded);
      expect(frame.messageType, 5002);
      expect(frame.payload, _b([0, 1, 0, 2, 0]));
    });
  });
}
