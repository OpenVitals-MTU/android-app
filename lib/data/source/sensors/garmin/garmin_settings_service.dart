import 'package:flutter/foundation.dart';

import 'garmin_protobuf.dart';

/// The watch's own settings tree, over the protobuf settings service.
///
/// The app defines none of this: the watch sends a MENU — screens, entries,
/// titles and option lists — already translated into whatever locale it is
/// handed. This layer asks for a screen and reports what came back; deciding
/// how to draw it is somebody else's job.
///
/// Field numbers from Gadgetbridge's `gdi_settings_service.proto` (AGPLv3).
/// That schema is older than this watch's firmware, so anything unrecognised is
/// carried rather than dropped — a settings tree read wrongly produces a screen
/// of plausible but incorrect controls, which is worse than one that is missing.
class GarminSettingsService {
  const GarminSettingsService._();

  // SettingsService fields.
  static const int _definitionRequest = 1;
  static const int _definitionResponse = 2;
  static const int _stateRequest = 3;
  static const int _stateResponse = 4;
  static const int _initRequest = 8;

  // ScreenDefinitionRequest fields.
  static const int _reqScreenId = 1;
  static const int _reqUnk2 = 2;
  static const int _reqLanguage = 3;

  // InitRequest fields.
  static const int _initLanguage = 1;
  static const int _initRegion = 2;

  /// The tree's root, from Gadgetbridge's `GarminRealtimeSettingsFragment`.
  static const int rootScreenId = 36352;

  /// Opens the settings service for a locale.
  ///
  /// The watch translates every title it later sends using this, so it decides
  /// what language the whole tree comes back in.
  static Uint8List init({String language = 'en_US', String region = 'us'}) {
    final request = (ProtobufWriter()
          ..string(_initLanguage, language)
          ..string(_initRegion, region))
        .toBytes();
    final service = (ProtobufWriter()..nested(_initRequest, request)).toBytes();
    return _smart(service);
  }

  /// Asks for one screen's DEFINITION — its title and the entries on it.
  static Uint8List screenDefinition(int screenId, {String language = 'en_US'}) {
    final request = (ProtobufWriter()
          ..varint(_reqScreenId, screenId)
          ..varint(_reqUnk2, 0)
          ..string(_reqLanguage, language))
        .toBytes();
    final service =
        (ProtobufWriter()..nested(_definitionRequest, request)).toBytes();
    return _smart(service);
  }

  /// Asks for one screen's STATE — the current value behind each entry.
  ///
  /// Separate from the definition because the two change on different clocks:
  /// the layout is fixed for a firmware, the values move as the watch is used.
  static Uint8List screenState(int screenId) {
    final request =
        (ProtobufWriter()..varint(_reqScreenId, screenId)).toBytes();
    final service =
        (ProtobufWriter()..nested(_stateRequest, request)).toBytes();
    return _smart(service);
  }

  static Uint8List _smart(Uint8List service) =>
      (ProtobufWriter()..nested(GarminSmartService.settings, service)).toBytes();

  /// The settings payload inside a `Smart` reply, or null if it carries none.
  static Uint8List? unwrap(Uint8List? reply) {
    if (reply == null) return null;
    return protobufField(readProtobuf(reply), GarminSmartService.settings)
        ?.bytes;
  }

  /// Whether a reply carries a screen definition.
  static bool hasDefinition(Uint8List? reply) {
    final service = unwrap(reply);
    if (service == null) return false;
    return protobufField(readProtobuf(service), _definitionResponse) != null;
  }

  /// Whether a reply carries a screen state.
  static bool hasState(Uint8List? reply) {
    final service = unwrap(reply);
    if (service == null) return false;
    return protobufField(readProtobuf(service), _stateResponse) != null;
  }

  /// Prints a reply's structure, field by field, without interpreting it.
  ///
  /// The point of the first exchange is to SEE what the watch sends. Naming the
  /// fields we think we recognise while still showing everything else is what
  /// separates "the schema matches" from "the schema is close enough to look
  /// like it matches".
  static void describe(Uint8List payload, {String indent = '  '}) {
    for (final field in readProtobuf(payload)) {
      final bytes = field.bytes;
      if (bytes == null) {
        debugPrint('$indent${field.field}: ${field.varint}');
        continue;
      }
      final text = _asText(bytes);
      if (text != null) {
        debugPrint('$indent${field.field}: "$text"');
        continue;
      }
      final nested = readProtobuf(bytes);
      if (nested.isEmpty) {
        debugPrint('$indent${field.field}: (${bytes.length}B) ${_hex(bytes)}');
        continue;
      }
      debugPrint('$indent${field.field}: {');
      describe(bytes, indent: '$indent  ');
      debugPrint('$indent}');
    }
  }

  /// Printable ASCII only — the watch sends titles as UTF-8 strings, and
  /// guessing that arbitrary bytes are text turns a nested message into mojibake.
  static String? _asText(Uint8List bytes) {
    if (bytes.isEmpty) return null;
    for (final byte in bytes) {
      if (byte < 0x20 || byte > 0x7E) return null;
    }
    return String.fromCharCodes(bytes);
  }

  static String _hex(Uint8List bytes) =>
      bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join(' ');
}
