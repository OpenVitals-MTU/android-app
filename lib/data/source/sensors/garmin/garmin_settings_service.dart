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

  // ScreenDefinition / ScreenEntry / Target fields.
  static const int _defEntry = 5;
  static const int _entryTitle = 3;
  static const int _entryTarget = 9;
  static const int _targetType = 1;
  static const int _targetSubscreen = 2;
  static const int _labelText = 2;

  // InitRequest fields.
  static const int _initLanguage = 1;
  static const int _initRegion = 2;

  /// The tree's root, from Gadgetbridge's `GarminRealtimeSettingsFragment`.
  static const int rootScreenId = 36352;

  /// How long the watch may take to build a screen.
  ///
  /// Measured: a root definition arrived after more than ten seconds, so the
  /// transport's default timed the request out and a later, unrelated settings
  /// message was mistaken for the answer.
  static const Duration replyTimeout = Duration(seconds: 30);

  /// The SettingsService field a reply of each kind arrives in.
  static const int definitionResponseField = _definitionResponse;
  static const int stateResponseField = _stateResponse;

  /// Whether [reply] carries the response field [responseField].
  ///
  /// Correlating on "is this a settings message" was not enough: the watch
  /// sends several, and the first to arrive was a five-byte one on field 7 that
  /// answered nothing we asked.
  static bool carries(Uint8List? reply, int responseField) {
    final service = unwrap(reply);
    if (service == null) return false;
    return protobufField(readProtobuf(service), responseField) != null;
  }

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

  /// One entry on a screen that leads somewhere else.
  static List<GarminSettingsSubscreen> subscreens(Uint8List reply) {
    final service = unwrap(reply);
    if (service == null) return const [];
    final response =
        protobufField(readProtobuf(service), _definitionResponse)?.bytes;
    if (response == null) return const [];
    final definition = protobufField(readProtobuf(response), 2)?.bytes;
    if (definition == null) return const [];

    final out = <GarminSettingsSubscreen>[];
    for (final field in readProtobuf(definition)) {
      if (field.field != _defEntry) continue;
      final entry = field.bytes;
      if (entry == null) continue;
      final fields = readProtobuf(entry);
      final target = protobufField(fields, _entryTarget)?.bytes;
      if (target == null) continue;
      final targetFields = readProtobuf(target);
      // Target type 0 is "another screen"; 6 opens an activity ON the watch and
      // 7 is hidden, neither of which this app can walk into.
      if (protobufField(targetFields, _targetType)?.varint != 0) continue;
      final screenId = protobufField(targetFields, _targetSubscreen)?.varint;
      if (screenId == null) continue;

      String? title;
      final label = protobufField(fields, _entryTitle)?.bytes;
      if (label != null) {
        final text = protobufField(readProtobuf(label), _labelText)?.bytes;
        if (text != null) title = String.fromCharCodes(text);
      }
      out.add(GarminSettingsSubscreen(screenId: screenId, title: title));
    }
    return out;
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


/// An entry that leads to another screen: where it goes, and what it is called.
class GarminSettingsSubscreen {
  const GarminSettingsSubscreen({required this.screenId, this.title});

  final int screenId;
  final String? title;
}
