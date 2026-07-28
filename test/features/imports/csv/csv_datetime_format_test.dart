import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/imports/csv/csv_datetime_format.dart';

/// Every assertion here pins a FIXED offset rather than the device zone.
///
/// `CsvTimeZoneMode.device` resolves against whatever tz database the host has,
/// so asserting a UTC instant under it would pass in Madrid and fail in UTC CI —
/// the same class of flake as `DateTime.now()`, which AGENTS.md bans outright.
/// The one device-mode test below asserts the RELATIONSHIP (offset round-trips
/// back to the same wall clock), which holds in every zone.
void main() {
  group('resolveCsvInstant', () {
    test(
      'a timezone-less timestamp at a fixed +02:00 resolves two hours earlier in UTC',
      () {
        final resolved = resolveCsvInstant(
          '2026-07-01 08:12:00',
          const CsvDateTimeSettings(
            format: CsvDateTimeFormat.yearFirst,
            zone: CsvTimeZoneMode.fixedOffset,
            fixedOffset: Duration(hours: 2),
          ),
        );

        expect(resolved, isNotNull);
        expect(resolved!.utc, DateTime.utc(2026, 7, 1, 6, 12));
        expect(resolved.offset, const Duration(hours: 2));
      },
    );

    test('a timestamp read as UTC keeps its wall clock and a zero offset', () {
      final resolved = resolveCsvInstant(
        '2026-07-01 08:12:00',
        const CsvDateTimeSettings(
          format: CsvDateTimeFormat.yearFirst,
          zone: CsvTimeZoneMode.utc,
        ),
      );

      expect(resolved!.utc, DateTime.utc(2026, 7, 1, 8, 12));
      expect(resolved.offset, Duration.zero);
    });

    test('an ISO timestamp carrying +05:30 overrides the selected UTC mode', () {
      final resolved = resolveCsvInstant(
        '2026-07-01T08:12:00+05:30',
        const CsvDateTimeSettings(
          format: CsvDateTimeFormat.iso8601,
          zone: CsvTimeZoneMode.utc,
        ),
      );

      expect(resolved!.utc, DateTime.utc(2026, 7, 1, 2, 42));
      expect(resolved.offset, const Duration(hours: 5, minutes: 30));
    });

    test('an ISO timestamp ending in Z resolves to that instant with no offset',
        () {
      final resolved = resolveCsvInstant(
        '2026-07-01T08:12:00Z',
        const CsvDateTimeSettings(
          format: CsvDateTimeFormat.iso8601,
          zone: CsvTimeZoneMode.fixedOffset,
          fixedOffset: Duration(hours: 9),
        ),
      );

      expect(resolved!.utc, DateTime.utc(2026, 7, 1, 8, 12));
      expect(resolved.offset, Duration.zero);
    });

    test(
      'the device zone reports an offset that maps the instant back to the '
      'wall clock in the file',
      () {
        final resolved = resolveCsvInstant(
          '2026-07-01 08:12:00',
          const CsvDateTimeSettings(
            format: CsvDateTimeFormat.yearFirst,
            zone: CsvTimeZoneMode.device,
          ),
        );

        expect(resolved, isNotNull);
        final wallClock = resolved!.utc.add(resolved.offset);
        expect(wallClock, DateTime.utc(2026, 7, 1, 8, 12));
      },
    );

    test('epoch seconds resolve to the matching UTC instant', () {
      final resolved = resolveCsvInstant(
        '1782000000',
        const CsvDateTimeSettings(
          format: CsvDateTimeFormat.epochSeconds,
          zone: CsvTimeZoneMode.utc,
        ),
      );

      expect(
        resolved!.utc,
        DateTime.fromMillisecondsSinceEpoch(1782000000 * 1000, isUtc: true),
      );
    });

    test('a date-only cell resolves to midnight of that day', () {
      final resolved = resolveCsvInstant(
        '2026-07-01',
        const CsvDateTimeSettings(
          format: CsvDateTimeFormat.yearFirst,
          zone: CsvTimeZoneMode.utc,
        ),
      );

      expect(resolved!.utc, DateTime.utc(2026, 7, 1));
    });

    test('a cell that does not match the chosen format resolves to null', () {
      final resolved = resolveCsvInstant(
        'not a date',
        const CsvDateTimeSettings(format: CsvDateTimeFormat.yearFirst),
      );

      expect(resolved, isNull);
    });

    test('a custom pattern parses a shape none of the families cover', () {
      final resolved = resolveCsvInstant(
        '01 Jul 2026 08:12',
        const CsvDateTimeSettings(
          format: CsvDateTimeFormat.custom,
          customPattern: 'dd MMM yyyy HH:mm',
          zone: CsvTimeZoneMode.utc,
        ),
      );

      expect(resolved!.utc, DateTime.utc(2026, 7, 1, 8, 12));
    });
  });

  group('parseCsvWallClock', () {
    test('a date-only pattern does not silently swallow a trailing time', () {
      // parseStrict must reject the leftover ' 08:12:00' rather than drop it;
      // dropping it would move every reading to midnight.
      expect(
        parseCsvWallClock('2026-07-01 08:12:00', CsvDateTimeFormat.yearFirst),
        DateTime.utc(2026, 7, 1, 8, 12),
      );
    });

    test('a ten-digit epoch value is not misread as milliseconds', () {
      expect(
        parseCsvWallClock('1782000000', CsvDateTimeFormat.epochMillis),
        isNull,
      );
    });

    test('a small counting number is not accepted as an epoch timestamp', () {
      // Without a plausibility bound, `1` is a valid epoch second, so a column
      // of step counts or reps would be auto-detected as the timestamp and
      // every reading dated to 1970.
      expect(parseCsvWallClock('1', CsvDateTimeFormat.epochSeconds), isNull);
      expect(parseCsvWallClock('250', CsvDateTimeFormat.epochSeconds), isNull);
      expect(parseCsvWallClock('1', CsvDateTimeFormat.auto), isNull);
    });

    test('a real epoch second inside the plausible window still parses', () {
      expect(
        parseCsvWallClock('1782000000', CsvDateTimeFormat.epochSeconds),
        isNotNull,
      );
    });
  });

  group('csvTimestampHasExplicitOffset', () {
    test('a bare ISO date is not mistaken for carrying an offset', () {
      // '2026-07-01' ends in '-01', which looks like an offset to a naive regex.
      expect(csvTimestampHasExplicitOffset('2026-07-01'), isFalse);
    });

    test('an offset suffix on a full timestamp is detected', () {
      expect(
        csvTimestampHasExplicitOffset('2026-07-01T08:12:00+05:30'),
        isTrue,
      );
      expect(csvTimestampHasExplicitOffset('2026-07-01T08:12:00Z'), isTrue);
    });

    test('a timestamp with no offset suffix is reported as carrying none', () {
      expect(csvTimestampHasExplicitOffset('2026-07-01 08:12:00'), isFalse);
    });
  });

  group('detectCsvDateTimeFormat', () {
    test('a year-first sample is detected as year-first', () {
      final detection = detectCsvDateTimeFormat([
        '2026-07-01 08:12:00',
        '2026-07-02 08:14:00',
        '2026-07-03 08:11:00',
      ]);

      expect(detection.format, CsvDateTimeFormat.yearFirst);
      expect(detection.matchedRows, 3);
      expect(detection.ambiguousDayMonth, isFalse);
    });

    test(
      'a sample where both day-first and month-first parse every row is '
      'reported ambiguous rather than guessed',
      () {
        final detection = detectCsvDateTimeFormat([
          '01/07/2026',
          '02/08/2026',
          '03/09/2026',
        ]);

        expect(detection.ambiguousDayMonth, isTrue);
      },
    );

    test('a day above twelve resolves the ordering to day-first', () {
      final detection = detectCsvDateTimeFormat([
        '01/07/2026',
        '25/07/2026',
        '13/08/2026',
      ]);

      expect(detection.format, CsvDateTimeFormat.dayFirst);
      expect(detection.ambiguousDayMonth, isFalse);
    });

    test('an unparsable sample reports that nothing matched', () {
      final detection = detectCsvDateTimeFormat(['banana', 'apple']);

      expect(detection.matchedNothing, isTrue);
    });

    test('an empty sample reports that nothing matched', () {
      final detection = detectCsvDateTimeFormat([]);

      expect(detection.matchedNothing, isTrue);
      expect(detection.totalRows, 0);
    });
  });
}
