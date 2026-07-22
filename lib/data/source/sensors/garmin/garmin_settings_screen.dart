import 'package:flutter/foundation.dart';

import 'garmin_protobuf.dart';
import 'garmin_settings_service.dart';

/// What a settings entry actually is, once its target has been read.
///
/// The watch decides this, not the app: it sends a menu whose every row declares
/// its own control. An entry read as the wrong kind produces a plausible screen
/// with the wrong widget on it — a list where a time should be — which is worse
/// than one that fails to render.
enum GarminEntryKind {
  /// Opens another screen. Alarms is one of these on the Clocks screen.
  subscreen,

  /// One of a set of options the watch supplies with the entry — never a list
  /// this app invents.
  options,

  /// A time of day, changed as seconds since midnight.
  time,

  /// An on/off switch, which carries no target at all.
  toggle,

  /// A number the watch bounds itself.
  number,

  /// A button rather than a setting: no target, and no value to show. The
  /// watch's own "Delete" row on an alarm is one.
  action,

  /// Present on the screen but not something a phone can act on: it opens
  /// something ON the watch, or is hidden outright.
  inert,
}

/// One option the watch offered for an [GarminEntryKind.options] entry.
@immutable
class GarminSettingsOption {
  const GarminSettingsOption({required this.index, required this.title});

  /// Position in the list the watch sent. A change names THIS, so it must never
  /// be a guessed ordinal.
  final int index;
  final String title;
}

/// One row on a settings screen.
@immutable
class GarminSettingsEntry {
  const GarminSettingsEntry({
    required this.id,
    required this.kind,
    this.title,
    this.summary,
    this.subscreenId,
    this.options = const [],
    this.switchedOn,
    this.rawTargetType,
  });

  /// Identifies the entry within its screen — what a change names.
  final int id;
  final GarminEntryKind kind;

  /// What the watch calls it, already in the requested language.
  final String? title;

  /// The current value as the watch renders it, when it sent one. Display text,
  /// never parsed: "7:00 am" is the watch's formatting, not ours to reproduce.
  final String? summary;

  final int? subscreenId;
  final List<GarminSettingsOption> options;

  /// Only meaningful for [GarminEntryKind.toggle]; null when the state has not
  /// been read.
  final bool? switchedOn;

  /// The target type the watch declared, kept even when it is one this app does
  /// not handle. An entry that came out [GarminEntryKind.inert] is otherwise
  /// indistinguishable from a hidden row, and the number is what says which
  /// control it really is.
  final int? rawTargetType;

  /// A row carrying nothing a person could read — an unused slot in a list that
  /// reserves one per position. Worth hiding rather than drawing as a blank.
  ///
  /// An untitled inert row counts even when it carries a summary: a value with
  /// no name is not something anybody can read. Real alarms put their time in
  /// the TITLE, so nothing legible is lost.
  bool get isBlank =>
      kind == GarminEntryKind.inert && (title == null || title!.trim().isEmpty);

  /// Whether a phone can do anything with this row.
  bool get isActionable => kind != GarminEntryKind.inert;
}

/// One screen: what it is called, and the rows on it.
@immutable
class GarminSettingsScreen {
  const GarminSettingsScreen({
    required this.screenId,
    this.title,
    this.entries = const [],
    this.hasState = true,
  });

  final int screenId;
  final String? title;
  final List<GarminSettingsEntry> entries;

  /// Whether the watch supplied the CURRENT VALUES alongside the layout.
  ///
  /// False leaves every switch inert, which on its own looks like a bug rather
  /// than a missing reply — so the screen says which it is.
  final bool hasState;

  bool get isEmpty => entries.isEmpty;
}

/// Turns a definition reply — and optionally the matching state reply — into a
/// screen.
///
/// The two are separate requests because they answer different questions: the
/// definition says there is a "Repeat" row with four options, the state says it
/// is currently "Weekday". A screen built from the definition alone cannot show
/// what anything is set to.
GarminSettingsScreen? parseGarminSettingsScreen(
  Uint8List? definitionReply, {
  Uint8List? stateReply,
}) {
  final definition = _definitionOf(definitionReply);
  if (definition == null) return null;

  final fields = readProtobuf(definition);
  final screenId = protobufField(fields, _screenId)?.varint;
  if (screenId == null) return null;

  final states = _statesById(stateReply);
  final entries = <GarminSettingsEntry>[];
  for (final field in fields) {
    if (field.field != _entry) continue;
    final entry = _parseEntry(field.bytes, states, stateReply != null);
    if (entry != null) entries.add(entry);
  }

  return GarminSettingsScreen(
    screenId: screenId,
    title: _labelText(protobufField(fields, _screenTitle)?.bytes),
    entries: entries,
    hasState: stateReply != null && states.isNotEmpty,
  );
}

GarminSettingsEntry? _parseEntry(
  Uint8List? bytes,
  Map<int, _EntryState> states,
  bool stateAvailable,
) {
  if (bytes == null) return null;
  final fields = readProtobuf(bytes);
  final id = protobufField(fields, _entryId)?.varint;
  if (id == null) return null;

  final title = _labelText(protobufField(fields, _entryTitle)?.bytes);
  final state = states[id];
  final target = protobufField(fields, _entryTarget)?.bytes;

  // No target at all: a switch, which carries its value in the STATE rather
  // than declaring anything in the definition.
  if (target == null) {
    // A switch and a button are both target-less; what separates them is the
    // STATE. A switch has a position to report, a button has nothing to report
    // because there is nothing to be in.
    //
    // So a button can only be identified when the state ARRIVED. Without it the
    // two are indistinguishable, and calling a switch a button would offer to
    // "Status" the action reserved for "Delete" — which is how a missing reply
    // turns into a deleted alarm.
    final GarminEntryKind kind;
    if (state?.switchedOn != null) {
      kind = GarminEntryKind.toggle;
    } else if (stateAvailable && title != null && title.isNotEmpty) {
      kind = GarminEntryKind.action;
    } else {
      kind = GarminEntryKind.inert;
    }
    return GarminSettingsEntry(
      id: id,
      kind: kind,
      title: title,
      summary: state?.summary,
      switchedOn: state?.switchedOn,
    );
  }

  final targetFields = readProtobuf(target);
  final targetType = protobufField(targetFields, _targetType)?.varint;
  final subscreen = protobufField(targetFields, _targetSubscreen)?.varint;

  switch (targetType) {
    case _targetSubscreenPlain:
    case _targetSubscreenWithOptions:
      // Screen zero is how an empty slot is written — a row that leads nowhere.
      if (subscreen == null || subscreen == 0) {
        return GarminSettingsEntry(
          id: id,
          kind: GarminEntryKind.inert,
          title: title,
          summary: state?.summary,
        );
      }
      return GarminSettingsEntry(
        id: id,
        kind: GarminEntryKind.subscreen,
        title: title,
        summary: state?.summary,
        subscreenId: subscreen,
      );
    case _targetOptions:
      return GarminSettingsEntry(
        id: id,
        kind: GarminEntryKind.options,
        title: title,
        summary: state?.summary,
        options: _options(protobufField(targetFields, _targetOptionList)?.bytes),
      );
    case _targetTime:
      return GarminSettingsEntry(
        id: id,
        kind: GarminEntryKind.time,
        title: title,
        summary: state?.summary,
      );
    case _targetNumberPicker:
      return GarminSettingsEntry(
        id: id,
        kind: GarminEntryKind.number,
        title: title,
        summary: state?.summary,
      );
    default:
      // Type 6 opens something ON the watch and 7 is hidden. Anything else is a
      // control this app has never seen, and rendering a guess at it would put
      // the wrong widget in front of a real setting. The declared type is kept
      // so it can be identified rather than merely dismissed.
      return GarminSettingsEntry(
        id: id,
        kind: GarminEntryKind.inert,
        title: title,
        summary: state?.summary,
        rawTargetType: targetType,
      );
  }
}

List<GarminSettingsOption> _options(Uint8List? bytes) {
  if (bytes == null) return const [];
  final out = <GarminSettingsOption>[];
  var index = 0;
  for (final field in readProtobuf(bytes)) {
    if (field.field != _optionEntry) continue;
    final title = _labelText(
      protobufField(readProtobuf(field.bytes!), _entryTitle)?.bytes,
    );
    out.add(GarminSettingsOption(index: index, title: title ?? '?'));
    index++;
  }
  return out;
}

/// The current value behind each entry, keyed by entry id.
Map<int, _EntryState> _statesById(Uint8List? reply) {
  final service = GarminSettingsService.unwrap(reply);
  if (service == null) return const {};
  final response =
      protobufField(readProtobuf(service), GarminSettingsService.stateResponseField)
          ?.bytes;
  if (response == null) return const {};
  final state = protobufField(readProtobuf(response), _stateInner)?.bytes;
  if (state == null) return const {};

  final out = <int, _EntryState>{};
  for (final field in readProtobuf(state)) {
    if (field.field != _entryState) continue;
    final fields = readProtobuf(field.bytes!);
    final id = protobufField(fields, _entryId)?.varint;
    if (id == null) continue;
    final switchBytes = protobufField(fields, _stateSwitch)?.bytes;
    final summaryBytes = protobufField(fields, _stateSummary)?.bytes;
    out[id] = _EntryState(
      switchedOn: switchBytes == null
          ? null
          : (protobufField(readProtobuf(switchBytes), 1)?.varint ?? 0) != 0,
      summary: _labelText(summaryBytes) ??
          _labelText(protobufField(readProtobuf(summaryBytes ?? Uint8List(0)), 1)
              ?.bytes),
    );
  }
  return out;
}

@immutable
class _EntryState {
  const _EntryState({this.switchedOn, this.summary});

  final bool? switchedOn;
  final String? summary;
}

Uint8List? _definitionOf(Uint8List? reply) {
  final service = GarminSettingsService.unwrap(reply);
  if (service == null) return null;
  final response = protobufField(
    readProtobuf(service),
    GarminSettingsService.definitionResponseField,
  )?.bytes;
  if (response == null) return null;
  return protobufField(readProtobuf(response), _definitionInner)?.bytes;
}

/// A `Label`'s display text — field 2, with field 1 being an opaque id.
String? _labelText(Uint8List? bytes) {
  if (bytes == null) return null;
  final text = protobufField(readProtobuf(bytes), _labelTextField)?.bytes;
  if (text == null) return null;
  return String.fromCharCodes(text);
}

const int _definitionInner = 2;
const int _stateInner = 2;
const int _screenId = 1;
const int _screenTitle = 4;
const int _entry = 5;
const int _entryId = 1;
const int _entryTitle = 3;
const int _entryTarget = 9;
const int _labelTextField = 2;

const int _targetType = 1;
const int _targetSubscreen = 2;
const int _targetOptionList = 4;
const int _targetSubscreenPlain = 0;
const int _targetOptions = 1;
const int _targetTime = 3;
const int _targetSubscreenWithOptions = 9;
const int _targetNumberPicker = 8;
const int _optionEntry = 1;

const int _entryState = 4;
const int _stateSwitch = 3;
const int _stateSummary = 4;
