import 'dart:typed_data';

import 'garmin_byte_reader.dart';
import 'garmin_byte_writer.dart';
import 'garmin_crc.dart';

/// A decoded GFDI frame: a message type and its raw payload.
///
/// The frame is what COBS wraps. Layout (little-endian), from Gadgetbridge's
/// `GFDIMessage`:
///
///   `[u16 length][u16 messageType][payload…][u16 crc]`
///
/// where `length` is the WHOLE frame (its own 2 bytes, the type, the payload and
/// the CRC), and `crc` covers everything before it.
///
/// The `messageType` high-bit convention is handled here so the message layer
/// sees a single flat id: an incoming type with `0x8000` set is a status/ack
/// response, remapped to `(type & 0xff) + 5000` exactly as
/// `GFDIMessage.parseIncoming` does.
class GarminGfdiFrame {
  const GarminGfdiFrame({required this.messageType, required this.payload});

  final int messageType;
  final Uint8List payload;

  static const int _statusFlag = 0x8000;
  static const int _statusBase = 5000;

  /// Parses one COBS-decoded frame. Throws [GarminGfdiFrameException] on a
  /// length or CRC mismatch — a corrupt frame must not be mistaken for a short
  /// one and silently mis-dispatched.
  factory GarminGfdiFrame.parse(Uint8List bytes) {
    if (bytes.length < 6) {
      throw GarminGfdiFrameException(
        'Frame too short: ${bytes.length} bytes',
      );
    }
    final reader = GarminByteReader(bytes);
    final length = reader.readShort();
    if (length != bytes.length) {
      throw GarminGfdiFrameException(
        'Frame length field $length != actual ${bytes.length}',
      );
    }
    final expectedCrc = GarminCrc.compute(bytes, length: length - 2);
    final actualCrc = ByteData.sublistView(bytes)
        .getUint16(length - 2, Endian.little);
    if (expectedCrc != actualCrc) {
      throw GarminGfdiFrameException(
        'Frame CRC $actualCrc != computed $expectedCrc',
      );
    }

    var messageType = reader.readShort();
    if ((messageType & _statusFlag) != 0) {
      messageType = (messageType & 0xff) + _statusBase;
    }
    // Payload is everything between the type and the CRC.
    final payload = Uint8List.sublistView(bytes, 4, length - 2);
    return GarminGfdiFrame(messageType: messageType, payload: payload);
  }

  /// Builds a wire frame for [messageType] carrying [payload]: writes a
  /// placeholder length, the type, the payload, backfills the real length, then
  /// appends the CRC over everything so far.
  static Uint8List build(int messageType, Uint8List payload) {
    final writer = GarminByteWriter(payload.length + 8)
      ..writeShort(0) // Length placeholder, patched below.
      ..writeShort(messageType)
      ..writeBytes(payload);
    final length = writer.length + 2; // + the CRC about to be written.
    writer.patchShort(0, length);
    final crc = GarminCrc.compute(writer.toBytes());
    writer.writeShort(crc);
    return writer.toBytes();
  }
}

class GarminGfdiFrameException implements Exception {
  const GarminGfdiFrameException(this.message);
  final String message;
  @override
  String toString() => 'GarminGfdiFrameException: $message';
}
