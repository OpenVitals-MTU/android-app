/// Framing for the phone-to-phone sync wire protocol.
///
/// RFCOMM is a raw byte stream: a peer's `write()` of N bytes does NOT arrive as
/// one `onBytesReceived` of N bytes — it may be split or coalesced arbitrarily.
/// So every logical message is wrapped in a length-prefixed frame and
/// [SyncFrameReader] reassembles frames from whatever chunk boundaries arrive.
///
/// Wire format per frame (big-endian):
///
///   ┌──────────────┬────────┬─────────────────────┐
///   │ payloadLen   │ type   │ payload             │
///   │ uint32 (4B)  │ u8 (1B)│ payloadLen bytes    │
///   └──────────────┴────────┴─────────────────────┘
///
/// The type byte is [SyncFrameType.index]; the enum is append-only so the byte
/// stays stable across versions.
library;

import 'dart:typed_data';

/// The kind of a framed message. APPEND-ONLY — the ordinal is the wire type
/// byte, so never reorder or remove a value.
enum SyncFrameType {
  /// Capability + nonce exchange that opens a session.
  hello,

  /// Authentication proof derived from the 6-digit code + both nonces.
  auth,

  /// A gzipped batch of records flowing one direction.
  batch,

  /// Acknowledges a received [batch] (stop-and-wait backpressure).
  batchAck,

  /// The sender has no more batches to send this session.
  sendDone,

  /// Cooperative abort (user cancel, or a fatal protocol error).
  abort,
}

/// A single framed message: its [type] and raw [payload] bytes.
class SyncFrame {
  const SyncFrame(this.type, this.payload);

  final SyncFrameType type;
  final Uint8List payload;

  /// Encodes this frame to its on-wire bytes (header + payload).
  Uint8List encode() {
    final out = Uint8List(5 + payload.length);
    final view = ByteData.view(out.buffer);
    view.setUint32(0, payload.length, Endian.big);
    view.setUint8(4, type.index);
    out.setRange(5, out.length, payload);
    return out;
  }
}

/// The largest payload a single frame may carry, a guard against a corrupt or
/// hostile length prefix allocating unbounded memory. Batches are chunked well
/// under this (~64 KB); 16 MiB is generous headroom.
const int kMaxSyncFramePayload = 16 * 1024 * 1024;

/// Thrown when the byte stream violates the frame format (bad type byte or an
/// oversized length prefix). Fatal to the session.
class SyncFrameFormatException implements Exception {
  const SyncFrameFormatException(this.message);
  final String message;
  @override
  String toString() => 'SyncFrameFormatException: $message';
}

/// Reassembles [SyncFrame]s from a stream of arbitrary byte chunks. Feed each
/// inbound chunk to [addChunk]; it returns the frames that completed, buffering
/// any partial trailing frame for the next call.
class SyncFrameReader {
  final BytesBuilder _buffer = BytesBuilder(copy: false);

  /// Number of bytes currently buffered (a partial frame not yet complete).
  int get bufferedBytes => _buffer.length;

  /// Appends [chunk] and returns every frame that is now complete, in order.
  List<SyncFrame> addChunk(List<int> chunk) {
    if (chunk.isNotEmpty) _buffer.add(chunk);
    final frames = <SyncFrame>[];
    // Snapshot the buffer to a flat view we can index into.
    var data = _buffer.toBytes();
    var offset = 0;
    while (data.length - offset >= 5) {
      final header = ByteData.view(data.buffer, data.offsetInBytes + offset, 5);
      final payloadLen = header.getUint32(0, Endian.big);
      if (payloadLen > kMaxSyncFramePayload) {
        throw SyncFrameFormatException(
          'frame payload $payloadLen exceeds max $kMaxSyncFramePayload',
        );
      }
      final total = 5 + payloadLen;
      if (data.length - offset < total) break; // wait for more bytes
      final typeByte = header.getUint8(4);
      if (typeByte >= SyncFrameType.values.length) {
        throw SyncFrameFormatException('unknown frame type byte $typeByte');
      }
      final payload = Uint8List.sublistView(
        data,
        offset + 5,
        offset + total,
      );
      frames.add(SyncFrame(SyncFrameType.values[typeByte], payload));
      offset += total;
    }
    // Retain only the unconsumed tail.
    _buffer.clear();
    if (offset < data.length) {
      _buffer.add(Uint8List.sublistView(data, offset));
    }
    return frames;
  }
}
