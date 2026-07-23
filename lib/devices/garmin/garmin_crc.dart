import 'dart:typed_data';

/// Garmin GFDI packet checksum.
///
/// Byte-for-byte port of Gadgetbridge's `ChecksumCalculator.computeCrc`
/// (AGPLv3) — a nibble-table CRC, not a standard CRC-16, so it must be ported
/// literally rather than swapped for a package.
///
/// The Kotlin runs over signed `byte`s; here bytes are unsigned (0..255), which
/// gives the identical result — `b & 15` and `(b >> 4) & 15` select the same two
/// nibbles either way, and that is all the algorithm reads from each byte.
class GarminCrc {
  const GarminCrc._();

  static const List<int> _constants = [
    0x0000, 0xCC01, 0xD801, 0x1400, 0xF001, 0x3C00, 0x2800, 0xE401, //
    0xA001, 0x6C00, 0x7800, 0xB401, 0x5000, 0x9C01, 0x8801, 0x4400,
  ];

  /// CRC over `data[offset .. offset+length)`, seeded with [initialCrc]
  /// (0 for a whole packet). Masked to 16 bits, matching the Kotlin's
  /// `(short)` truncation before it is written into the frame.
  static int compute(
    Uint8List data, {
    int offset = 0,
    int? length,
    int initialCrc = 0,
  }) {
    final end = offset + (length ?? data.length - offset);
    var crc = initialCrc;
    for (var i = offset; i < end; i++) {
      final b = data[i];
      crc = (((crc >> 4) & 4095) ^ _constants[crc & 15]) ^ _constants[b & 15];
      crc =
          (((crc >> 4) & 4095) ^ _constants[crc & 15]) ^ _constants[(b >> 4) & 15];
    }
    return crc & 0xFFFF;
  }
}
