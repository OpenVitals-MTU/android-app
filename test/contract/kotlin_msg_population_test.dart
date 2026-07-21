import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// The only cheap instrument that can see BELOW the Pigeon boundary.
///
/// `pigeon_domain_contract_test.dart` proves a field EXISTS on both sides of the
/// contract. It cannot prove the native reader ever puts anything in it — and a
/// field that Kotlin hard-codes to `null` is indistinguishable, from Dart, from a
/// field Health Connect had no value for. `ExerciseSegmentMsg.setIndex` is exactly
/// that: declared on both sides, present in the binding table, and set to a literal
/// `null` by the reader. It reads as "this segment has no set index" on every
/// strength session ever recorded, and round-trips as data loss.
///
/// A runtime fake cannot see it either, because the fake CONSTRUCTS the Msg — it
/// would happily populate `setIndex` and the test would pass while the device
/// returns null.
///
/// So: scan the readers for `SomeMsg(field = null)` and keep the answers as a
/// GOLDEN SET. It fails when a new one appears, and it fails when an existing one
/// is fixed (forcing the entry's deletion). That turns "we know about it" from
/// tribal knowledge into a checked artifact.
///
/// This is a smoke detector, not a proof: it is a text scan, so a rename or a
/// reformat can fool it. Keeping it a golden set is what makes that drift VISIBLE
/// rather than silent.
void main() {
  const readerDir =
      'packages/health_connect_native/android/src/main/kotlin/tech/mmarca/openvitals/health_connect_native';

  /// Every `field = null` appearing as a named argument inside a `*Msg(...)`
  /// construction, as `File.MsgClass.field`.
  ///
  /// Attributed to the INNERMOST enclosing Msg. Msg constructions nest — an
  /// `ExerciseDataMsg` builds its `ExerciseSegmentMsg`s inline — so a naive
  /// "walk to the matching paren" blames the outer one, and the golden set then
  /// records a field on a class that does not have it.
  Set<String> hardcodedNulls() {
    final found = <String>{};
    final msgCall = RegExp(r'\b(\w+Msg)\s*\($');
    final namedNull = RegExp(r'\b(\w+)\s*=\s*null\b');

    for (final entity in Directory(readerDir).listSync()) {
      if (entity is! File || !entity.path.endsWith('.kt')) continue;
      // Generated: it declares the fields, it does not populate them.
      if (entity.path.endsWith('Messages.g.kt')) continue;
      final file = entity.uri.pathSegments.last;
      final src = entity.readAsStringSync();

      // A stack of the Msg constructions currently open, innermost last.
      final open = <({String msg, int depth})>[];
      var depth = 0;

      for (var i = 0; i < src.length; i++) {
        final ch = src[i];
        if (ch == '(') {
          depth++;
          final call = msgCall.firstMatch(src.substring(0, i + 1));
          if (call != null) open.add((msg: call.group(1)!, depth: depth));
          continue;
        }
        if (ch == ')') {
          if (open.isNotEmpty && open.last.depth == depth) open.removeLast();
          depth--;
          continue;
        }
        if (open.isEmpty || ch != '=') continue;
        // Only a named argument of the innermost open Msg, not one nested in a
        // lambda or a sub-expression inside it.
        if (depth != open.last.depth) continue;
        final line = src.substring(src.lastIndexOf('\n', i) + 1,
            src.indexOf('\n', i) == -1 ? src.length : src.indexOf('\n', i));
        final arg = namedNull.firstMatch(line);
        if (arg != null) found.add('$file.${open.last.msg}.${arg.group(1)}');
      }
    }
    return found;
  }

  test('the fields Kotlin hard-codes to null are exactly these', () {
    // Each entry is a DECISION, with the reason. Adding one means accepting that
    // Dart will read null forever. Fixing one means deleting the entry.
    const known = <String, String>{
      'RecordToImportMsg.kt.ImportRecordMsg.plannedExerciseId':
          'Correct-by-semantics: plannedExerciseId links a COMPLETED exercise to '
              'the plan it fulfilled. A PlannedExerciseSession record IS the plan, '
              'so it has no such link. The ExerciseSession converter does populate '
              'it from record.plannedExerciseSessionId.',

      'ActivityHealthReader.kt.ExerciseSegmentMsg.setIndex':
          'REAL GAP. Health Connect ExerciseSegment has no set-index concept, so '
              'the reader has nothing to read; the WRITE path drops it too, so an '
              'OpenVitals-authored segment round-trips as data loss. Fixing it needs '
              'a place to store it (a clientRecordId scheme, or notes).',

      // EMPTY_SESSION_METRICS: the deliberate "nothing was asked for, or nothing
      // could be read" sentinel. Every field null IS the value. Not a gap.
      'ActivityHealthReader.kt.ExerciseSessionMetricsMsg.totalDistanceMeters':
          'EMPTY_SESSION_METRICS sentinel — all-null IS the value.',
      'ActivityHealthReader.kt.ExerciseSessionMetricsMsg.averageSpeedMetersPerSecond':
          'EMPTY_SESSION_METRICS sentinel.',
      'ActivityHealthReader.kt.ExerciseSessionMetricsMsg.steps':
          'EMPTY_SESSION_METRICS sentinel.',
      'ActivityHealthReader.kt.ExerciseSessionMetricsMsg.totalCaloriesKcal':
          'EMPTY_SESSION_METRICS sentinel.',
      'ActivityHealthReader.kt.ExerciseSessionMetricsMsg.activeCaloriesKcal':
          'EMPTY_SESSION_METRICS sentinel.',
      'ActivityHealthReader.kt.ExerciseSessionMetricsMsg.elevationGainedMeters':
          'EMPTY_SESSION_METRICS sentinel.',
      'ActivityHealthReader.kt.ExerciseSessionMetricsMsg.floorsClimbed':
          'EMPTY_SESSION_METRICS sentinel.',
      'ActivityHealthReader.kt.ExerciseSessionMetricsMsg.wheelchairPushes':
          'EMPTY_SESSION_METRICS sentinel.',
      // NOTE: averagePowerWatts is NOT listed. EMPTY_SESSION_METRICS does not name
      // it — it leans on the Kotlin default (`= null`) instead. Same value, but the
      // scanner cannot see it, and that asymmetry is real: the day someone gives
      // that field a non-null default, the sentinel silently stops being empty.

      // VitalsMeasurementEntryMsg is a SHARED shape across every vital, and only
      // blood pressure has a second number (diastolic). Null here is correct, not
      // missing.
      'VitalsHealthReader.kt.VitalsMeasurementEntryMsg.secondaryValue':
          'Correct: only BLOOD PRESSURE has a secondary value (diastolic). SpO2, '
              'respiratory rate, body temperature and glucose each have one number.',
    };

    final actual = hardcodedNulls();
    final unexpected = actual.difference(known.keys.toSet()).toList()..sort();
    final fixed = known.keys.toSet().difference(actual).toList()..sort();

    expect(
      unexpected,
      isEmpty,
      reason: 'A Msg field is now hard-coded to null in the native reader. From '
          'Dart this is INDISTINGUISHABLE from "Health Connect had no value" — it '
          'will read null on every record and nothing will fail:\n'
          '  ${unexpected.join('\n  ')}\n'
          'Either populate it, or add it to `known` with the reason it cannot be.',
    );

    expect(
      fixed,
      isEmpty,
      reason: 'These are no longer hard-coded to null — the gap is fixed. Delete '
          'the entry so the golden set keeps telling the truth:\n'
          '  ${fixed.join('\n  ')}',
    );
  });
}
