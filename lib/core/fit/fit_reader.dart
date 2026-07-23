import 'dart:convert';
import 'dart:typed_data';

import 'fit_message.dart';

/// Generic FIT container reader: walks a `.FIT` byte stream and emits its data
/// messages as [FitMessage]s, knowing NOTHING about what any message means. The
/// domain interpretation (activity, Garmin wellness) lives in separate consumers
/// that switch on [FitMessage.globalMessageNumber].
///
/// Decodes EVERY field of EVERY message — there is no message allowlist — because
/// a reusable reader has no basis to guess which a consumer wants, and FIT files
/// are small enough that the extra field decoding is free. Consumers simply
/// ignore the messages and fields they do not know.
class FitReader {
  const FitReader._();

  /// True when [bytes] begins a FIT file at [offset] (a valid header carrying the
  /// `.FIT` magic). Used to spot the start of each file in a concatenated stream.
  static bool isFitFileAt(Uint8List bytes, int offset) {
    if (offset < 0 || offset + _fitMinimumHeaderSize > bytes.length) return false;
    final headerSize = bytes[offset] & 0xFF;
    return headerSize >= _fitMinimumHeaderSize &&
        offset + headerSize <= bytes.length &&
        bytes[offset + _fitHeaderDataTypeOffset] == 0x2E && // '.'
        bytes[offset + _fitHeaderDataTypeOffset + 1] == 0x46 && // 'F'
        bytes[offset + _fitHeaderDataTypeOffset + 2] == 0x49 && // 'I'
        bytes[offset + _fitHeaderDataTypeOffset + 3] == 0x54; // 'T'
  }

  /// Decodes ONE FIT file starting at [startOffset], returning its data messages
  /// (in file order) and the offset the next chained file would begin at. The
  /// caller loops this across a concatenated stream, resetting nothing — each
  /// file is self-contained (its own local-message definitions and timestamp
  /// anchor).
  static (List<FitMessage>, int) readFile(Uint8List bytes, int startOffset) =>
      _FitFileReader(bytes, startOffset)._read();
}

class _FitFileReader {
  _FitFileReader(this.fileBytes, this.startOffset);

  final Uint8List fileBytes;
  final int startOffset;

  final Map<int, _FitMessageDefinition> _definitions = {};
  final List<FitMessage> _messages = [];
  int? _lastTimestampRaw;

  (List<FitMessage>, int) _read() {
    final headerSize = fileBytes[startOffset] & 0xFF;
    if (headerSize < _fitMinimumHeaderSize ||
        startOffset + headerSize > fileBytes.length) {
      throw const FitFormatException('FIT file header is invalid.');
    }
    final dataSize = _readUint32(
      fileBytes,
      startOffset + _fitHeaderDataSizeOffset,
      true,
    );
    final dataStart = startOffset + headerSize;
    final dataEnd = dataStart + dataSize;
    if (dataEnd > fileBytes.length) {
      throw const FitFormatException('FIT file data section is incomplete.');
    }
    final reader = _FitDataReader(fileBytes, dataStart, dataEnd);
    while (reader.hasRemaining()) {
      _readRecord(reader);
    }
    final next = dataEnd + _fitCrcSize;
    return (_messages, next > fileBytes.length ? fileBytes.length : next);
  }

  void _readRecord(_FitDataReader reader) {
    final header = reader.readUnsignedByte();
    if (header & _fitCompressedHeaderFlag != 0) {
      final localMessageType = (header >> _fitCompressedLocalMessageTypeShift) &
          _fitCompressedLocalMessageTypeMask;
      final timestamp = _compressedTimestamp(header & _fitCompressedTimestampMask);
      _readDataMessage(localMessageType, timestamp, reader);
      return;
    }
    final localMessageType = header & _fitNormalLocalMessageTypeMask;
    if (header & _fitDefinitionMessageFlag != 0) {
      _definitions[localMessageType] = _readDefinitionMessage(header, reader);
    } else {
      _readDataMessage(localMessageType, null, reader);
    }
  }

  _FitMessageDefinition _readDefinitionMessage(int header, _FitDataReader reader) {
    reader.skip(1);
    final architecture = reader.readUnsignedByte();
    final bool littleEndian;
    if (architecture == _fitArchitectureLittleEndian) {
      littleEndian = true;
    } else if (architecture == _fitArchitectureBigEndian) {
      littleEndian = false;
    } else {
      throw const FitFormatException('FIT message architecture is invalid.');
    }
    final globalMessageNumber = reader.readUnsignedShort(littleEndian);
    final fieldCount = reader.readUnsignedByte();
    final fields = <_FitFieldDefinition>[];
    for (var i = 0; i < fieldCount; i++) {
      fields.add(
        _FitFieldDefinition(
          reader.readUnsignedByte(),
          reader.readUnsignedByte(),
          reader.readUnsignedByte(),
        ),
      );
    }
    final developerFieldSizes = <int>[];
    if (header & _fitDeveloperDataFlag != 0) {
      final developerFieldCount = reader.readUnsignedByte();
      for (var i = 0; i < developerFieldCount; i++) {
        reader.skip(1);
        final size = reader.readUnsignedByte();
        reader.skip(1);
        developerFieldSizes.add(size);
      }
    }
    return _FitMessageDefinition(
      globalMessageNumber: globalMessageNumber,
      littleEndian: littleEndian,
      fieldList: fields,
      developerFields: developerFieldSizes,
    );
  }

  void _readDataMessage(
    int localMessageType,
    int? compressedTimestamp,
    _FitDataReader reader,
  ) {
    final definition = _definitions[localMessageType];
    if (definition == null) {
      throw const FitFormatException('FIT data message has no definition.');
    }
    final values = <int, int>{};
    final strings = <int, String>{};
    final arrays = <int, List<int>>{};
    for (final field in definition.fieldList) {
      final fieldBytes = reader.readBytes(field.size);
      final longValue = _fitLong(fieldBytes, field, definition.littleEndian);
      if (longValue != null) values[field.number] = longValue;
      final stringValue = _fitString(fieldBytes, field);
      if (stringValue != null) strings[field.number] = stringValue;
      final arrayValue = _fitLongArray(fieldBytes, field, definition.littleEndian);
      if (arrayValue.isNotEmpty) arrays[field.number] = arrayValue;
    }
    for (final size in definition.developerFields) {
      reader.skip(size);
    }

    final explicitTimestamp = values[_fitTimestampFieldNumber];
    final messageTimestamp = explicitTimestamp ?? compressedTimestamp;
    if (messageTimestamp != null) _lastTimestampRaw = messageTimestamp;

    _messages.add(FitMessage(
      definition.globalMessageNumber,
      values,
      strings,
      arrays,
      messageTimestamp,
    ));
  }

  int? _compressedTimestamp(int offset) {
    final previous = _lastTimestampRaw;
    if (previous == null) return null;
    final previousOffset = previous & _fitCompressedTimestampMask;
    final delta = offset < previousOffset
        ? offset + _fitCompressedTimestampRollover - previousOffset
        : offset - previousOffset;
    return previous + delta;
  }
}

class _FitMessageDefinition {
  const _FitMessageDefinition({
    required this.globalMessageNumber,
    required this.littleEndian,
    required this.fieldList,
    required this.developerFields,
  });

  final int globalMessageNumber;
  final bool littleEndian;
  final List<_FitFieldDefinition> fieldList;
  final List<int> developerFields;
}

class _FitFieldDefinition {
  const _FitFieldDefinition(this.number, this.size, this.baseType);

  final int number;
  final int size;
  final int baseType;
}

class _FitDataReader {
  _FitDataReader(this.bytes, this.offset, this.endOffset);

  final Uint8List bytes;
  int offset;
  final int endOffset;

  bool hasRemaining() => offset < endOffset;

  int readUnsignedByte() {
    if (offset >= endOffset) {
      throw const FitFormatException(
        'FIT file ended before data records were complete.',
      );
    }
    return bytes[offset++] & 0xFF;
  }

  int readUnsignedShort(bool littleEndian) {
    if (offset + 2 > endOffset) {
      throw const FitFormatException(
        'FIT file ended before data records were complete.',
      );
    }
    final value = _readUint16(bytes, offset, littleEndian);
    offset += 2;
    return value;
  }

  Uint8List readBytes(int size) {
    if (size < 0 || offset + size > endOffset) {
      throw const FitFormatException(
        'FIT file ended before data records were complete.',
      );
    }
    final slice = Uint8List.sublistView(bytes, offset, offset + size);
    offset += size;
    return slice;
  }

  void skip(int size) {
    if (size < 0 || offset + size > endOffset) {
      throw const FitFormatException(
        'FIT file ended before data records were complete.',
      );
    }
    offset += size;
  }
}

int _readUint16(Uint8List bytes, int index, bool littleEndian) {
  final first = bytes[index] & 0xFF;
  final second = bytes[index + 1] & 0xFF;
  return littleEndian ? first | (second << 8) : (first << 8) | second;
}

int _readSignedShort(Uint8List bytes, int index, bool littleEndian) {
  final value = _readUint16(bytes, index, littleEndian);
  return value & 0x8000 != 0 ? value - 0x10000 : value;
}

int _readUint32(Uint8List bytes, int index, bool littleEndian) {
  final b0 = bytes[index] & 0xFF;
  final b1 = bytes[index + 1] & 0xFF;
  final b2 = bytes[index + 2] & 0xFF;
  final b3 = bytes[index + 3] & 0xFF;
  return littleEndian
      ? b0 | (b1 << 8) | (b2 << 16) | (b3 << 24)
      : (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
}

int _readInt32(Uint8List bytes, int index, bool littleEndian) {
  final raw = _readUint32(bytes, index, littleEndian);
  return raw >= 0x80000000 ? raw - 0x100000000 : raw;
}

/// Every element of an array field, invalid sentinels dropped.
///
/// FIT expresses an array as a field whose declared size is a multiple of its
/// base type's — the Health Snapshot messages pack a whole two-minute recording
/// into one record this way. [_fitLong] reads only the first element, which is
/// right for every scalar field and silently loses the rest of an array.
List<int> _fitLongArray(
  Uint8List bytes,
  _FitFieldDefinition field,
  bool littleEndian,
) {
  final baseType = field.baseType & _fitBaseTypeMask;
  final size = _fitBaseTypeSize(baseType);
  if (size <= 0) return const [];
  final out = <int>[];
  for (var offset = 0; offset + size <= bytes.length; offset += size) {
    final value = _fitLong(
      Uint8List.sublistView(bytes, offset, offset + size),
      field,
      littleEndian,
    );
    if (value != null) out.add(value);
  }
  return out;
}

int? _fitLong(Uint8List bytes, _FitFieldDefinition field, bool littleEndian) {
  final baseType = field.baseType & _fitBaseTypeMask;
  final baseTypeSize = _fitBaseTypeSize(baseType);
  if (baseTypeSize <= 0 || bytes.length < baseTypeSize) return null;
  switch (baseType) {
    case _fitBaseTypeEnum:
    case _fitBaseTypeUInt8:
      final v = bytes[0] & 0xFF;
      return v == _fitInvalidUInt8 ? null : v;
    case _fitBaseTypeSInt8:
      final v = bytes[0] & 0xFF;
      final signed = v >= 0x80 ? v - 0x100 : v;
      return signed == _fitInvalidSInt8 ? null : signed;
    case _fitBaseTypeSInt16:
      final v = _readSignedShort(bytes, 0, littleEndian);
      return v == _fitInvalidSInt16 ? null : v;
    case _fitBaseTypeUInt16:
      final v = _readUint16(bytes, 0, littleEndian);
      return v == _fitInvalidUInt16 ? null : v;
    case _fitBaseTypeSInt32:
      final v = _readInt32(bytes, 0, littleEndian);
      return v == _fitInvalidSInt32 ? null : v;
    case _fitBaseTypeUInt32:
      final v = _readUint32(bytes, 0, littleEndian);
      return v == _fitInvalidUInt32 ? null : v;
    case _fitBaseTypeUInt8z:
      final v = bytes[0] & 0xFF;
      return v == 0 ? null : v;
    case _fitBaseTypeUInt16z:
      final v = _readUint16(bytes, 0, littleEndian);
      return v == 0 ? null : v;
    case _fitBaseTypeUInt32z:
      final v = _readUint32(bytes, 0, littleEndian);
      return v == 0 ? null : v;
    default:
      return null;
  }
}

String? _fitString(Uint8List bytes, _FitFieldDefinition field) {
  final baseType = field.baseType & _fitBaseTypeMask;
  if (baseType != _fitBaseTypeString) return null;
  var decoded = utf8.decode(bytes, allowMalformed: true);
  var end = decoded.length;
  while (end > 0 && decoded.codeUnitAt(end - 1) == 0) {
    end--;
  }
  decoded = decoded.substring(0, end);
  // Inlined `cleanText`: trim, and treat an all-blank name as absent. Kept here
  // rather than importing the route-import helper so the reader stays generic.
  final trimmed = decoded.trim();
  return trimmed.isEmpty ? null : trimmed;
}

int _fitBaseTypeSize(int baseType) {
  switch (baseType) {
    case _fitBaseTypeEnum:
    case _fitBaseTypeSInt8:
    case _fitBaseTypeUInt8:
    case _fitBaseTypeString:
    case _fitBaseTypeUInt8z:
    case _fitBaseTypeByte:
      return 1;
    case _fitBaseTypeSInt16:
    case _fitBaseTypeUInt16:
    case _fitBaseTypeUInt16z:
      return 2;
    case _fitBaseTypeSInt32:
    case _fitBaseTypeUInt32:
    case _fitBaseTypeFloat32:
    case _fitBaseTypeUInt32z:
      return 4;
    case _fitBaseTypeFloat64:
    case _fitBaseTypeSInt64:
    case _fitBaseTypeUInt64:
    case _fitBaseTypeUInt64z:
      return 8;
    default:
      return 0;
  }
}

// FIT container framing.
const int _fitMinimumHeaderSize = 12;
const int _fitHeaderDataSizeOffset = 4;
const int _fitHeaderDataTypeOffset = 8;
const int _fitCrcSize = 2;
const int _fitCompressedHeaderFlag = 0x80;
const int _fitCompressedLocalMessageTypeShift = 5;
const int _fitCompressedLocalMessageTypeMask = 0x03;
const int _fitCompressedTimestampMask = 0x1F;
const int _fitCompressedTimestampRollover = 0x20;
const int _fitDefinitionMessageFlag = 0x40;
const int _fitDeveloperDataFlag = 0x20;
const int _fitNormalLocalMessageTypeMask = 0x0F;
const int _fitArchitectureLittleEndian = 0;
const int _fitArchitectureBigEndian = 1;

// `timestamp` is field 253 on every message that carries one.
const int _fitTimestampFieldNumber = 253;

// FIT base types and their invalid sentinels.
const int _fitBaseTypeMask = 0x1F;
const int _fitBaseTypeEnum = 0;
const int _fitBaseTypeSInt8 = 1;
const int _fitBaseTypeUInt8 = 2;
const int _fitBaseTypeSInt16 = 3;
const int _fitBaseTypeUInt16 = 4;
const int _fitBaseTypeSInt32 = 5;
const int _fitBaseTypeUInt32 = 6;
const int _fitBaseTypeString = 7;
const int _fitBaseTypeFloat32 = 8;
const int _fitBaseTypeFloat64 = 9;
const int _fitBaseTypeUInt8z = 10;
const int _fitBaseTypeUInt16z = 11;
const int _fitBaseTypeUInt32z = 12;
const int _fitBaseTypeByte = 13;
const int _fitBaseTypeSInt64 = 14;
const int _fitBaseTypeUInt64 = 15;
const int _fitBaseTypeUInt64z = 16;
const int _fitInvalidUInt8 = 0xFF;
const int _fitInvalidSInt8 = 0x7F;
const int _fitInvalidUInt16 = 0xFFFF;
const int _fitInvalidSInt16 = 0x7FFF;
const int _fitInvalidUInt32 = 0xFFFFFFFF;
const int _fitInvalidSInt32 = 0x7FFFFFFF;
