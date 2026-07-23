import 'dart:typed_data';

/// Garmin's COBS variant, the framing under every GFDI packet.
///
/// Port of Gadgetbridge's `CobsCoDec` (AGPLv3). It is NOT textbook COBS: a frame
/// is bracketed by a LEADING and a trailing `0x00` (standard COBS has only the
/// trailing delimiter), and a payload ending in a zero gets an extra `0x01`
/// group so the decoder can tell "ends in zero" from "ends at a block boundary".
/// Both quirks are load-bearing, so this is ported literally rather than swapped
/// for a package.
///
/// Split into a pure [encode] and a streaming [GarminCobsDecoder]: bytes arrive
/// from BLE in arbitrary chunks, so the decoder buffers until it sees the
/// trailing `0x00` that ends a frame.
class GarminCobs {
  const GarminCobs._();

  /// Encodes one GFDI packet into a wire frame: `00 <cobs groups> 00`.
  static Uint8List encode(Uint8List data) {
    // Worst case is ~2x plus the two delimiters; the builder grows anyway.
    final out = BytesBuilder(copy: false)..addByte(0); // Garmin leading pad.
    var lastByteWasZero = false;
    var pos = 0;
    while (pos < data.length) {
      final start = pos;
      var zeroIndex = pos;
      while (zeroIndex < data.length && data[zeroIndex] != 0) {
        zeroIndex++;
      }
      // The scan stopped either at a zero or at the end; only a zero advances
      // `pos` past `zeroIndex` below, which is how the trailing-zero flag is set.
      lastByteWasZero = zeroIndex < data.length;

      var payloadSize = zeroIndex - start;
      var blockStart = start;
      while (payloadSize >= 0xFE) {
        out.addByte(0xFF); // Max-length group: 254 literal bytes, no implied 0.
        out.add(Uint8List.sublistView(data, blockStart, blockStart + 0xFE));
        payloadSize -= 0xFE;
        blockStart += 0xFE;
      }
      out
        ..addByte(payloadSize + 1)
        ..add(Uint8List.sublistView(data, blockStart, blockStart + payloadSize));

      pos = zeroIndex + (lastByteWasZero ? 1 : 0);
    }

    if (lastByteWasZero) out.addByte(0x01);
    out.addByte(0); // Trailing delimiter.
    return out.toBytes();
  }
}

/// Reassembles COBS frames from an arbitrarily-chunked byte stream.
///
/// Feed it whatever BLE delivers; [pull] returns the next fully-decoded GFDI
/// packet or null. Stateful and single-consumer, mirroring Gadgetbridge's
/// `CobsCoDec` which holds one accumulation buffer per connection.
class GarminCobsDecoder {
  final BytesBuilder _buffer = BytesBuilder(copy: false);

  /// Appends received [bytes] to the internal buffer.
  void addBytes(Uint8List bytes) => _buffer.add(bytes);

  /// Decodes and returns the next complete packet, or null when the buffer does
  /// not yet hold a full frame (no `0x00` delimiter past the leading pad).
  ///
  /// Call repeatedly until it returns null — one BLE chunk can complete more
  /// than one frame, and the decoded packet keeps whatever follows it buffered.
  Uint8List? pull() {
    final data = _buffer.toBytes();
    if (data.length < 4) return null; // Min frame: pad + group + byte + delim.
    if (data[0] != 0) {
      // No leading 0 → the buffer is desynchronised. Drop it rather than loop
      // forever on a frame that can never decode.
      _buffer.clear();
      return null;
    }

    final out = BytesBuilder(copy: false);
    // Walk groups after the leading pad. A `0x00` code IS the frame delimiter
    // (internal — the codec's leading/trailing pads bracket each frame), so it
    // ends this frame and marks where the leftover begins.
    var pos = 1;
    var frameEnd = -1;
    while (pos < data.length) {
      final code = data[pos++];
      if (code == 0) {
        frameEnd = pos;
        break;
      }
      final payloadSize = code - 1;
      if (pos + payloadSize > data.length) {
        // Group runs past the buffer: either still arriving, or corrupt. Waiting
        // is safe — a real frame ends in a delimiter the scan has not reached.
        return null;
      }
      out.add(Uint8List.sublistView(data, pos, pos + payloadSize));
      pos += payloadSize;
      // A non-max group implies a zero after its payload — unless the next byte
      // is the delimiter, i.e. this was the frame's last group. (Standard COBS:
      // the final block carries no implied zero.)
      if (code != 0xFF && pos < data.length && data[pos] != 0) {
        out.addByte(0);
      }
    }
    if (frameEnd < 0) return null; // No delimiter yet → frame still incomplete.

    // Consume through the delimiter; re-buffer whatever came after it.
    final leftover = Uint8List.sublistView(data, frameEnd);
    _buffer.clear();
    if (leftover.isNotEmpty) _buffer.add(leftover);
    return out.toBytes();
  }
}
