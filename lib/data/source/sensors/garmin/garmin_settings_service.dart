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
  static const int _targetSubscreenWithOptions = 9;
  static const int _labelText = 2;

  // ChangeRequest fields.
  static const int _changeRequest = 5;
  static const int _changeResponse = 6;
  static const int _changeScreenId = 1;
  static const int _changeEntryId = 2;
  static const int _changeSwitch = 3;
  static const int _changeOption = 4;
  static const int _changeTime = 6;
  static const int _changeNumber = 8;
  static const int _changePosition = 11;
  static const int _positionDelete = 2;

  // Where each response nests the thing it is about, and where that thing names
  // its screen. Read off a vívoactive 5: the definition and state responses use
  // field 2, the change response field 3.
  static const int _responseInner = 2;
  static const int _changeResponseInner = 3;
  static const int _responseScreenId = 1;

  // InitRequest fields.
  static const int _initLanguage = 1;
  static const int _initRegion = 2;

  /// The tree's root, from Gadgetbridge's `GarminRealtimeSettingsFragment`.
  static const int rootScreenId = 36352;

  /// The Alarms list, measured on a vívoactive 5 (Settings → Clocks → Alarms).
  ///
  /// A well-known id rather than a walk from the root: reaching it by walking
  /// costs four round trips every time somebody taps Alarms, and the id has been
  /// stable across every read of this watch. If a future model moves it, the
  /// screen will come back empty rather than wrong — which is why the caller
  /// reports "the watch sent nothing" instead of inventing a list.
  static const int alarmsScreenId = 68;

  /// How long the watch may take to build a screen.
  ///
  /// Measured: a root definition arrived after more than ten seconds, so the
  /// transport's default timed the request out and a later, unrelated settings
  /// message was mistaken for the answer.
  static const Duration replyTimeout = Duration(seconds: 30);

  /// The SettingsService field a reply of each kind arrives in.
  static const int definitionResponseField = _definitionResponse;
  static const int stateResponseField = _stateResponse;
  static const int changeResponseField = _changeResponse;

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

  /// Which SCREEN a reply is about, or null if it does not say.
  ///
  /// Every response nests the screen it describes at the same place, which is
  /// the only way to tell one from another: the watch retransmits anything it
  /// thinks went unacknowledged, so a definition for a screen asked about
  /// minutes ago can arrive while a different one is pending. Matching on the
  /// response field alone handed the alarm LIST's layout back as the answer for
  /// one alarm's screen, which drew a list of rows whose titles and values came
  /// from two different places.
  static int? screenIdOf(Uint8List? reply, int responseField) {
    final service = unwrap(reply);
    if (service == null) return null;
    final response =
        protobufField(readProtobuf(service), responseField)?.bytes;
    if (response == null) return null;
    final innerField = responseField == _changeResponse
        ? _changeResponseInner
        : _responseInner;
    final inner = protobufField(readProtobuf(response), innerField)?.bytes;
    if (inner == null) return null;
    return protobufField(readProtobuf(inner), _responseScreenId)?.varint;
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

  /// Changes ONE entry on ONE screen.
  ///
  /// The only write in this whole stack — everything else reads. A malformed
  /// change does not fail politely: it is applied to a real watch someone
  /// depends on, so each value kind is a separate constructor rather than one
  /// generic setter that could put a time in a switch's field.
  ///
  /// The reply carries a status AND the screen's new state, so a caller can
  /// confirm what the watch actually did rather than assume the request landed.
  static Uint8List changeSwitch({
    required int screenId,
    required int entryId,
    required bool value,
  }) =>
      _change(screenId, entryId, _changeSwitch,
          (ProtobufWriter()..varint(1, value ? 1 : 0)).toBytes());

  /// [index] is a position in the option list the DEFINITION supplied for this
  /// entry — never a guessed ordinal.
  static Uint8List changeOption({
    required int screenId,
    required int entryId,
    required int index,
  }) =>
      _change(screenId, entryId, _changeOption,
          (ProtobufWriter()..varint(1, index)).toBytes());

  /// Seconds since midnight, which is how the watch stores a time of day.
  static Uint8List changeTime({
    required int screenId,
    required int entryId,
    required Duration sinceMidnight,
  }) {
    final seconds = sinceMidnight.inSeconds;
    if (seconds < 0 || seconds >= Duration.secondsPerDay) {
      throw ArgumentError.value(
        seconds,
        'sinceMidnight',
        'must be within one day — the watch takes seconds since midnight',
      );
    }
    return _change(screenId, entryId, _changeTime,
        (ProtobufWriter()..varint(1, seconds)).toBytes());
  }

  /// Activates a row that deletes something.
  ///
  /// The row carries no target at all — it is a button, not a setting — and
  /// `ChangeRequest` has no field for "activate this". `Position { index,
  /// delete }` is its only delete-shaped member, and a vívoactive 5 accepted it
  /// and removed the alarm, so that is what this sends.
  static Uint8List changeDelete({
    required int screenId,
    required int entryId,
  }) =>
      _change(screenId, entryId, _changePosition,
          (ProtobufWriter()..varint(_positionDelete, 1)).toBytes());

  static Uint8List changeNumber({
    required int screenId,
    required int entryId,
    required int value,
  }) =>
      _change(screenId, entryId, _changeNumber,
          (ProtobufWriter()..varint(1, value)).toBytes());

  static Uint8List _change(
    int screenId,
    int entryId,
    int valueField,
    Uint8List value,
  ) {
    final request = (ProtobufWriter()
          ..varint(_changeScreenId, screenId)
          ..varint(_changeEntryId, entryId)
          ..nested(valueField, value))
        .toBytes();
    final service =
        (ProtobufWriter()..nested(_changeRequest, request)).toBytes();
    return _smart(service);
  }

  /// What the watch made of a change: null when it did not answer with one.
  ///
  /// SUCCESS is 0 here — unlike the find service, where OK is 100. Two enums,
  /// two meanings for zero, which is exactly the kind of thing that turns a
  /// refusal into a silent success.
  static bool? changeSucceeded(Uint8List? reply) {
    final service = unwrap(reply);
    if (service == null) return null;
    final response =
        protobufField(readProtobuf(service), _changeResponse)?.bytes;
    if (response == null) return null;
    final status = protobufField(readProtobuf(response), 1)?.varint;
    // Absent status on a present response means it was accepted.
    return status == null || status == 0;
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
      // Types 0 and 9 are both "another screen" — 9 carries an option list with
      // it, which is what an alarm's own screen is. Type 6 opens an activity ON
      // the watch and 7 is hidden; neither can be walked into.
      final targetType = protobufField(targetFields, _targetType)?.varint;
      if (targetType != 0 && targetType != _targetSubscreenWithOptions) continue;
      final screenId = protobufField(targetFields, _targetSubscreen)?.varint;
      // Screen zero is how an EMPTY slot is written — an alarm list reserves a
      // row per slot and points the unused ones at nothing. Requesting it would
      // ask the watch for a screen that does not exist.
      if (screenId == null || screenId == 0) continue;

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
