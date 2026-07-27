import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// A test's days must come from `LocalDate`, not from hour arithmetic on an
/// instant.
///
/// A day window is a LOCAL calendar thing; `now.subtract(Duration(hours: 26))`
/// is an absolute one. They disagree by the runner's UTC offset, which is
/// enough to put a fixture on the other side of a day boundary — so the same
/// assertion passes in one timezone and fails in another. That is the worst
/// shape a test failure can have: invisible to whoever wrote it, and reported
/// by a CI runner in a zone they are not in.
///
/// This suite has been bitten three times. Twice by "a couple of hours ago" not
/// being today when the clock read 00:32, which `earlierToday` now covers; once
/// by a fixture built 26 hours back landing on `today - 1` at UTC+3 and
/// `today - 2` at UTC, on opposite sides of a two-day grace window.
///
/// A text scan, not an analyzer pass, for the same reason as
/// `background_health_source_construction_test.dart`: the failure mode is
/// someone writing an expression, and an expression is textual. It is
/// deliberately narrow — it only fires when a value built from hour or minute
/// arithmetic is turned into a calendar day, which is the precise step where
/// the two notions of "day" are conflated.
///
/// Use [localDayBefore] / [instantDaysBefore] from `test/support/today_fixtures.dart`.
/// If a case genuinely needs the absolute form, put [_optOut] on the line.
const String _optOut = 'local-day-fixture: intentional';

/// Assignments whose right-hand side does hour or minute Duration arithmetic.
final RegExp _tainting = RegExp(
  r'\b(?:final|const|var)\s+(\w+)\s*=\s*[^;]*?Duration\(\s*(?:hours|minutes)\s*:[^;]*?;',
);

/// `LocalDate.fromDateTime(someVariable`.
final RegExp _dayFromVariable = RegExp(r'LocalDate\.fromDateTime\(\s*(\w+)');

/// The same conflation written inline, without going through a variable.
final RegExp _dayFromInlineArithmetic =
    RegExp(r'LocalDate\.fromDateTime\([^)]*Duration\(\s*(?:hours|minutes)\s*:');

void main() {
  test('test fixtures derive calendar days from LocalDate, not hour offsets',
      () {
    final offenders = <String>[];

    for (final entity in Directory('test').listSync(recursive: true)) {
      if (entity is! File || !entity.path.endsWith('.dart')) continue;
      final path = entity.path.replaceAll(r'\', '/');
      // This file names the pattern in order to look for it.
      if (path.endsWith('test/contract/local_day_fixture_test.dart')) continue;

      final source = entity.readAsStringSync();
      if (!source.contains('LocalDate.fromDateTime')) continue;

      final lines = source.split('\n');
      final tainted = {
        for (final match in _tainting.allMatches(source)) match.group(1)!,
      };

      void report(int index, String detail) {
        if (lines[index].contains(_optOut)) return;
        offenders.add('$path:${index + 1}  $detail');
      }

      for (final match in _dayFromVariable.allMatches(source)) {
        if (!tainted.contains(match.group(1))) continue;
        report(source.substring(0, match.start).split('\n').length - 1,
            'LocalDate.fromDateTime(${match.group(1)}) — '
            '${match.group(1)} is built from hour/minute arithmetic');
      }
      for (final match in _dayFromInlineArithmetic.allMatches(source)) {
        report(source.substring(0, match.start).split('\n').length - 1,
            'LocalDate.fromDateTime(...Duration(hours/minutes...))');
      }
    }

    expect(
      offenders,
      isEmpty,
      reason: 'These fixtures turn an instant built from hour or minute '
          'arithmetic into a calendar day. The offset between the two is the '
          'runner\'s timezone, so the day they land on differs between a '
          'developer machine and CI.\n\n'
          '${offenders.join('\n')}\n\n'
          'Use localDayBefore() / instantDaysBefore() from '
          'test/support/today_fixtures.dart, or earlierToday() when the fixture '
          'means "still today". If the absolute form is genuinely wanted, mark '
          'the line "$_optOut".',
    );
  });
}
