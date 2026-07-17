import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_conversion_support.dart';

void main() {
  // Guards the dedup-identity fix: `clientRecordId`s are a SHA-256 over serialized
  // instants, so this format must stay byte-identical to Kotlin's
  // `java.time.Instant.toString()` or a re-import of a Kotlin-era export creates
  // duplicate Health Connect rows.
  group('appleInstantToStableString matches Kotlin Instant.toString', () {
    test('drops the fractional part for a whole-second UTC instant', () {
      expect(
        appleInstantToStableString(DateTime.utc(2011, 12, 3, 18, 15, 30)),
        '2011-12-03T18:15:30Z',
      );
    });

    test('keeps milliseconds when they are non-zero', () {
      expect(
        appleInstantToStableString(DateTime.utc(2011, 12, 3, 18, 15, 30, 120)),
        '2011-12-03T18:15:30.120Z',
      );
    });

    test('normalises a non-UTC instant to UTC before formatting', () {
      final local = DateTime.utc(2011, 12, 3, 18, 15, 30).toLocal();
      expect(appleInstantToStableString(local), '2011-12-03T18:15:30Z');
    });
  });

  // Guards the unstable-sort fix: Dart's List.sort is not stable, Kotlin's is.
  group('stableSort', () {
    test('keeps input order among elements that tie on the sort key', () {
      final items = [
        (0, 'a'),
        (1, 'b'),
        (0, 'c'),
        (1, 'd'),
        (0, 'e'),
      ];
      stableSort(items, (a, b) => a.$1.compareTo(b.$1));
      expect(items.map((e) => e.$2).toList(), ['a', 'c', 'e', 'b', 'd']);
    });

    test('leaves a list of fewer than two elements untouched', () {
      final single = ['only'];
      stableSort(single, (a, b) => a.compareTo(b));
      expect(single, ['only']);
    });
  });
}
