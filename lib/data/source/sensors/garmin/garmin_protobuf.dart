/// A minimal protobuf writer/reader, for the handful of Garmin messages this
/// app sends and reads.
///
/// Deliberately hand-rolled rather than generated. The alternative is the
/// `protobuf` package plus a `protoc` step in the build, to encode messages that
/// amount to a nested field and an integer — a code generator, a toolchain
/// dependency and a build phase, for a few dozen bytes. Everything here is wire
/// types 0 (varint) and 2 (length-delimited); nothing Garmin sends on the paths
/// this app uses needs more.
///
/// Field numbers come from Gadgetbridge's `.proto` files (AGPLv3), named at each
/// call site so a reader can check them against the schema.
library;

import 'dart:typed_data';

/// Builds one protobuf message.
class ProtobufWriter {
  final List<int> _bytes = [];

  /// A varint field (wire type 0) — the only numeric encoding used here.
  void varint(int field, int value) {
    _key(field, 0);
    _varint(value);
  }

  /// A length-delimited field (wire type 2): a nested message, or bytes.
  void nested(int field, Uint8List value) {
    _key(field, 2);
    _varint(value.length);
    _bytes.addAll(value);
  }

  /// A nested message with no fields set.
  ///
  /// Not the same as omitting it: protobuf distinguishes an absent field from a
  /// present-but-empty one, and Garmin uses the empty message as the whole
  /// request for actions that take no arguments (cancelling a find, for one).
  void emptyMessage(int field) => nested(field, Uint8List(0));

  void string(int field, String value) =>
      nested(field, Uint8List.fromList(value.codeUnits));

  void _key(int field, int wireType) => _varint((field << 3) | wireType);

  void _varint(int value) {
    var v = value;
    while (v >= 0x80) {
      _bytes.add((v & 0x7F) | 0x80);
      v >>= 7;
    }
    _bytes.add(v);
  }

  Uint8List toBytes() => Uint8List.fromList(_bytes);
}

/// One decoded protobuf field: its number, and its value in whichever form its
/// wire type carried.
class ProtobufField {
  const ProtobufField({
    required this.field,
    required this.wireType,
    this.varint,
    this.bytes,
  });

  final int field;
  final int wireType;
  final int? varint;
  final Uint8List? bytes;
}

/// Reads the top-level fields of a protobuf message.
///
/// Shallow on purpose: nesting is resolved by calling this again on a field's
/// bytes, which keeps the reader honest about not knowing the schema.
List<ProtobufField> readProtobuf(Uint8List data) {
  final out = <ProtobufField>[];
  var i = 0;

  int? readVarint() {
    var result = 0;
    var shift = 0;
    while (i < data.length) {
      final byte = data[i++];
      result |= (byte & 0x7F) << shift;
      if (byte & 0x80 == 0) return result;
      shift += 7;
      // A varint longer than ten bytes is corrupt, not a big number.
      if (shift > 63) return null;
    }
    return null;
  }

  while (i < data.length) {
    final key = readVarint();
    if (key == null) break;
    final field = key >> 3;
    final wireType = key & 0x07;
    switch (wireType) {
      case 0:
        final value = readVarint();
        if (value == null) return out;
        out.add(ProtobufField(field: field, wireType: 0, varint: value));
      case 2:
        final length = readVarint();
        if (length == null || i + length > data.length) return out;
        out.add(ProtobufField(
          field: field,
          wireType: 2,
          bytes: Uint8List.sublistView(data, i, i + length),
        ));
        i += length;
      case 5:
        if (i + 4 > data.length) return out;
        i += 4;
      case 1:
        if (i + 8 > data.length) return out;
        i += 8;
      default:
        // An unknown wire type means the rest cannot be located; stop rather
        // than walk off into noise.
        return out;
    }
  }
  return out;
}

/// The first field numbered [field], or null.
ProtobufField? protobufField(List<ProtobufField> fields, int field) {
  for (final f in fields) {
    if (f.field == field) return f;
  }
  return null;
}

/// Field numbers in Garmin's top-level `Smart` message
/// (`gdi_smart_proto.proto`). Only the services this app speaks are listed.
class GarminSmartService {
  const GarminSmartService._();

  static const int findMyWatch = 12;
  static const int settings = 42;
}
