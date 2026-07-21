import 'dart:typed_data';

/// Little-endian read cursor over a byte list.
///
/// Port of the read half of Gadgetbridge's `GarminByteBufferReader` /
/// `MessageReader`. Garmin's wire format is little-endian throughout, so the
/// endianness is baked in rather than configurable.
class GarminByteReader {
  GarminByteReader(this._data)
      : _view = ByteData.sublistView(_data);

  final Uint8List _data;
  final ByteData _view;
  int _pos = 0;

  int get position => _pos;
  int get remaining => _data.length - _pos;
  bool get hasRemaining => _pos < _data.length;

  int readByte() => _data[_pos++];

  int readShort() {
    final v = _view.getUint16(_pos, Endian.little);
    _pos += 2;
    return v;
  }

  int readInt() {
    final v = _view.getUint32(_pos, Endian.little);
    _pos += 4;
    return v;
  }

  int readLong() {
    final v = _view.getUint64(_pos, Endian.little);
    _pos += 8;
    return v;
  }

  Uint8List readBytes(int length) {
    final slice = Uint8List.sublistView(_data, _pos, _pos + length);
    _pos += length;
    return slice;
  }

  void skip(int count) => _pos += count;
}
