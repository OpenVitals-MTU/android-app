import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/import_client_record_id.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_conversion_support.dart';

/// Golden ids captured from `buildStableClientRecordId` BEFORE the hashing moved
/// into [buildImportClientRecordId].
///
/// Health Connect dedups and upserts on `clientRecordId`, so if these change,
/// every record a previous release wrote becomes unreachable: a re-import stops
/// recognising its own past output and writes duplicates instead of replacing.
/// The Kotlin→Flutter migration already depends on this being byte-stable (see
/// `appleInstantToStableString`); the extraction must not be what breaks it.
const Map<String, String> _appleGoldens = {
  'weight|2026-07-01T08:12:00Z|78.4':
      'apple_health_weight_1e4b72bbd84fa5d0f6e3153cd1dd3016',
  'HKQuantityTypeIdentifierBodyMass|2019-01-02T03:04:05Z':
      'apple_health_hkquantitytypeidentifierbodymass_'
          '9bdc0b35d955abb84ac6ec24c9389560',
};

void main() {
  group('buildImportClientRecordId', () {
    test('an id is namespace, slugged prefix and 32 hex characters', () {
      final id = buildImportClientRecordId('csv', 'WeightRecord', 'parts');

      expect(id, matches(RegExp(r'^csv_weightrecord_[0-9a-f]{32}$')));
    });

    test('the same parts always produce the same id', () {
      expect(
        buildImportClientRecordId('csv', 'WeightRecord', 'parts'),
        buildImportClientRecordId('csv', 'WeightRecord', 'parts'),
      );
    });

    test('a csv id never collides with an apple_health id for the same parts',
        () {
      expect(
        buildImportClientRecordId('csv', 'WeightRecord', 'parts'),
        isNot(buildImportClientRecordId('apple_health', 'WeightRecord', 'parts')),
      );
    });

    test('different parts produce different ids', () {
      expect(
        buildImportClientRecordId('csv', 'WeightRecord', 'a'),
        isNot(buildImportClientRecordId('csv', 'WeightRecord', 'b')),
      );
    });

    test('an empty prefix still yields a three-part id', () {
      expect(
        buildImportClientRecordId('csv', '', 'parts'),
        startsWith('csv_record_'),
      );
    });
  });

  group('toStableIdSegment', () {
    test('a mixed-case type name slugs to lowercase', () {
      expect(toStableIdSegment('WeightRecord'), 'weightrecord');
    });

    test('runs of punctuation collapse to a single underscore', () {
      expect(toStableIdSegment('Body  Fat--Record'), 'body_fat_record');
    });

    test('leading and trailing separators are dropped', () {
      expect(toStableIdSegment('--weight--'), 'weight');
    });

    test('a segment with nothing usable becomes "record"', () {
      expect(toStableIdSegment('---'), 'record');
    });
  });

  group('buildStableClientRecordId', () {
    test('the apple_health namespace still produces the ids it always has', () {
      for (final entry in _appleGoldens.entries) {
        final prefix = entry.key.split('|').first;
        expect(
          buildStableClientRecordId(prefix, entry.key),
          entry.value,
          reason: 'clientRecordId changed for ${entry.key} — every record a '
              'previous release wrote would become unreachable',
        );
      }
    });

    test('it delegates to the shared builder under the apple_health namespace',
        () {
      expect(
        buildStableClientRecordId('WeightRecord', 'parts'),
        buildImportClientRecordId('apple_health', 'WeightRecord', 'parts'),
      );
    });
  });
}
