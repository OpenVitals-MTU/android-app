import 'package:flutter_test/flutter_test.dart';

import 'dart_fields.dart';
import 'msg_domain_bindings.dart';

/// The contract between the Pigeon messages and the domain models, enforced.
///
/// We shipped the same bug twice, three weeks apart. `SleepData.recordingMethod`
/// and `ExerciseData.recordingMethod` were declared on the model, read by the UI,
/// and populated by NOTHING — the Pigeon message they cross the native boundary on
/// simply never carried them. Health Connect had the data the whole time. Kotlin
/// had somewhere to read it from. Dart had somewhere to put it. The contract in
/// between never mentioned it, so the native reader had nowhere to write it and
/// the mapper had nothing to read.
///
/// Every layer compiled. Every test passed. The analyzer was clean. What the user
/// saw was five rows reading "Not available" on every sleep session ever recorded,
/// an activities screen that counted zero manually-entered workouts however many
/// there were, and duplicate sessions being resolved by list order.
///
/// No runtime test can catch this. A fake host API constructs the SAME `*Msg`
/// class — it cannot supply a field the class does not declare, so it would build
/// the message without `recordingMethod`, the mapper would read null, and the test
/// would pass. The gap is only visible by comparing the two DECLARATIONS.
///
/// So: parse the sources, and make an unexplained gap fail the build.
void main() {
  late final Map<String, Set<String>> msgs;
  late final Map<String, Map<String, Set<String>>> domainsByFile;

  setUpAll(() {
    msgs = dartClassFields('packages/health_connect_native/pigeons/messages.dart');
    domainsByFile = {
      for (final file in msgDomainBindings.map((b) => b.domainFile).toSet())
        file: dartClassFields(file),
    };
  });

  for (final binding in msgDomainBindings) {
    group('${binding.msg} <-> ${binding.domain}', () {
      Set<String> msgFields() {
        final fields = msgs[binding.msg];
        expect(fields, isNotNull,
            reason: 'No class ${binding.msg} in the Pigeon contract. Renamed, or '
                'is the binding stale?');
        return fields!;
      }

      Set<String> domainFields() {
        final fields = domainsByFile[binding.domainFile]![binding.domain];
        expect(fields, isNotNull,
            reason: 'No class ${binding.domain} in ${binding.domainFile}.');
        return fields!;
      }

      test('every domain field can actually be populated', () {
        // THE ONE THAT CATCHES THE BUG. A domain field that no Msg field maps to,
        // and that is not declared domainOnly with a reason, is a field nothing can
        // ever fill — it will read null forever, silently, on every record.
        final mapped = binding.map.values.toSet();
        final orphaned = domainFields()
            .where((f) => !mapped.contains(f))
            .where((f) => !binding.domainOnly.containsKey(f))
            .toList()
          ..sort();

        expect(
          orphaned,
          isEmpty,
          reason: 'These ${binding.domain} fields have no ${binding.msg} field and '
              'no declared reason, so NOTHING can ever populate them — they will '
              'read null on every record, forever, and no runtime test can see it:'
              '\n  ${orphaned.join('\n  ')}\n'
              'Either add the field to the Pigeon contract (and the native reader '
              'and the Dart mapper), or declare it in domainOnly with the reason it '
              'does not cross the bridge.',
        );
      });

      test('every Msg field is accounted for', () {
        // The other direction: a field added to the contract that nobody wired up.
        // Harmless on its own, but it means the native side is paying to send
        // something Dart throws away — usually a half-finished change.
        final unused = msgFields()
            .where((f) => !binding.map.containsKey(f))
            .where((f) => !binding.msgOnly.containsKey(f))
            .toList()
          ..sort();

        expect(
          unused,
          isEmpty,
          reason: 'These ${binding.msg} fields map to nothing on ${binding.domain} '
              'and have no declared reason — the native side sends them and Dart '
              'drops them on the floor:\n  ${unused.join('\n  ')}',
        );
      });

      test('the binding table names fields that exist', () {
        // Guards the table itself. A typo here is worse than no table: it would
        // mark a field as "mapped" that does not exist, and the orphan check above
        // would go quiet about the real one.
        final msgSide = msgFields();
        final domainSide = domainFields();

        final badMsgKeys =
            binding.map.keys.where((f) => !msgSide.contains(f)).toList()..sort();
        final badDomainValues = binding.map.values
            .where((f) => !domainSide.contains(f))
            .toList()
          ..sort();
        final badDomainOnly = binding.domainOnly.keys
            .where((f) => !domainSide.contains(f))
            .toList()
          ..sort();

        expect(badMsgKeys, isEmpty,
            reason: 'binding.map names ${binding.msg} fields that do not exist');
        expect(badDomainValues, isEmpty,
            reason: 'binding.map names ${binding.domain} fields that do not exist');
        expect(badDomainOnly, isEmpty,
            reason: 'binding.domainOnly names ${binding.domain} fields that do not '
                'exist — a field that WAS domain-only and has since been wired up '
                'or removed. Delete the entry.');
      });
    });
  }

  test('every domainOnly entry carries a reason', () {
    // A bare field name in domainOnly is how this test gets defeated: it silences
    // the orphan check without anyone having to justify it. The reason is the
    // whole point — it turns "we know about it" into something a reviewer can
    // disagree with.
    for (final binding in msgDomainBindings) {
      for (final entry in binding.domainOnly.entries) {
        expect(entry.value.trim(), isNotEmpty,
            reason: '${binding.domain}.${entry.key} is declared domainOnly with no '
                'reason. Say why it does not cross the bridge.');
      }
      for (final entry in binding.msgOnly.entries) {
        expect(entry.value.trim(), isNotEmpty,
            reason: '${binding.msg}.${entry.key} is declared msgOnly with no reason.');
      }
    }
  });
}
