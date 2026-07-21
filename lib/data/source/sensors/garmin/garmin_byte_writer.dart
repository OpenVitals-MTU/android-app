import 'dart:typed_data';

/// Little-endian write cursor.
///
/// Port of Gadgetbridge's `MessageWriter`. Grows as needed; [toBytes] returns
/// exactly what was written.
class GarminByteWriter {
  GarminByteWriter([int initialCapacity = 256])
      : _view = ByteData(initialCapacity);

  ByteData _view;
  int _pos = 0;

  int get length => _pos;

  void _ensure(int extra) {
    if (_pos + extra <= _view.lengthInBytes) return;
    var next = _view.lengthInBytes * 2;
    while (next < _pos + extra) {
      next *= 2;
    }
    final grown = ByteData(next)
      ..buffer.asUint8List().setRange(0, _pos, _view.buffer.asUint8List());
    _view = grown;
  }

  void writeByte(int value) {
    _ensure(1);
    _view.setUint8(_pos, value & 0xFF);
    _pos += 1;
  }

  void writeShort(int value) {
    _ensure(2);
    _view.setUint16(_pos, value & 0xFFFF, Endian.little);
    _pos += 2;
  }

  void writeInt(int value) {
    _ensure(4);
    _view.setUint32(_pos, value & 0xFFFFFFFF, Endian.little);
    _pos += 4;
  }

  void writeLong(int value) {
    _ensure(8);
    _view.setUint64(_pos, value, Endian.little);
    _pos += 8;
  }

  void writeBytes(Uint8List bytes) {
    _ensure(bytes.length);
    _view.buffer.asUint8List().setRange(_pos, _pos + bytes.length, bytes);
    _pos += bytes.length;
  }

  /// Overwrites 2 bytes at [offset] — used to backfill the frame length once the
  /// payload is written.
  void patchShort(int offset, int value) =>
      _view.setUint16(offset, value & 0xFFFF, Endian.little);

  Uint8List toBytes() => Uint8List.sublistView(_view, 0, _pos);
}
