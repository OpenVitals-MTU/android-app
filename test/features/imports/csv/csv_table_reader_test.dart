import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/imports/csv/csv_table_reader.dart';

void main() {
  late Directory dir;

  setUp(() => dir = Directory.systemTemp.createTempSync('csv_reader_test'));
  tearDown(() => dir.deleteSync(recursive: true));

  File write(String name, String content) =>
      File('${dir.path}/$name')..writeAsStringSync(content);

  const reader = CsvTableReader();

  group('sniffDialect', () {
    test('a comma file with quoted headers is sniffed as comma-delimited', () {
      final file = write(
        'withings.csv',
        'Date,"Weight (kg)","Fat mass (kg)",Comments\n2026-07-01,78.4,15.2,\n',
      );

      expect(
        reader.sniffDialect(file.path),
        completion(
          isA<CsvDialect>().having((it) => it.fieldDelimiter, 'delimiter', ','),
        ),
      );
    });

    test(
      'a semicolon file whose quoted headers contain commas is sniffed as '
      'semicolon-delimited',
      () async {
        // The commas live INSIDE quotes; counting them would pick the wrong
        // delimiter and collapse the file to two columns.
        final file = write(
          'euro.csv',
          'Datum;"Gewicht (kg), netto";"Fett (kg), gesamt"\n'
              '2026-07-01;78,4;15,2\n',
        );

        final dialect = await reader.sniffDialect(file.path);

        expect(dialect.fieldDelimiter, ';');
      },
    );

    test('a CRLF file is sniffed as CRLF', () async {
      final file = write('crlf.csv', 'Date,Weight\r\n2026-07-01,78.4\r\n');

      expect((await reader.sniffDialect(file.path)).eol, '\r\n');
    });

    test('an LF file is sniffed as LF', () async {
      // Getting this wrong does not throw: package:csv returns ONE row with the
      // whole file in the last field. That silent-corruption mode is why the
      // line ending is sniffed rather than assumed.
      final file = write('lf.csv', 'Date,Weight\n2026-07-01,78.4\n');

      expect((await reader.sniffDialect(file.path)).eol, '\n');
    });
  });

  group('sample', () {
    test('a quoted header containing a comma reads as a single column', () async {
      final file = write(
        'withings.csv',
        'Date,"Weight (kg)","Fat mass (kg)",Comments\n2026-07-01,78.4,15.2,\n',
      );

      final sample = await reader.sample(file.path);

      expect(sample.headerRow, [
        'Date',
        'Weight (kg)',
        'Fat mass (kg)',
        'Comments',
      ]);
      expect(sample.columnCount, 4);
    });

    test('a UTF-8 BOM does not leak into the first header cell', () async {
      final file = write(
        'bom.csv',
        '﻿Date,Weight\n2026-07-01,78.4\n',
      );

      final sample = await reader.sample(file.path);

      expect(sample.headerRow.first, 'Date');
    });

    test(
      'a quoted field containing newlines survives a chunk boundary intact',
      () async {
        // The interesting row sits well past the 64 KB read boundary, so this
        // fails if the tokenizer does not carry state across chunks.
        final buffer = StringBuffer('Date,Weight,Comments\n');
        for (var i = 0; i < 4000; i++) {
          buffer.writeln('2026-07-01,78.4,filler row $i padding padding padding');
        }
        buffer.writeln('2026-07-02,79.0,"multi\nline\ncomment, with comma"');
        final file = write('big.csv', buffer.toString());

        final rows = await reader
            .rows(file.path, dialect: const CsvDialect(fieldDelimiter: ',', eol: '\n'))
            .toList();

        expect(rows, hasLength(4001));
        expect(rows.last.fields, hasLength(3));
        expect(rows.last.fields[2], 'multi\nline\ncomment, with comma');
      },
    );

    test('sampling a file with thousands of rows stops at the preview limit',
        () async {
      final buffer = StringBuffer('Date,Weight\n');
      for (var i = 0; i < 5000; i++) {
        buffer.writeln('2026-07-01,7$i');
      }
      final file = write('long.csv', buffer.toString());

      final sample = await reader.sample(file.path);

      expect(sample.dataRows, hasLength(kCsvPreviewRows));
    });

    test('a file with no header row gets synthesised column labels', () async {
      final file = write('headerless.csv', '2026-07-01,78.4\n2026-07-02,79.0\n');

      final sample = await reader.sample(file.path, hasHeaderRow: false);

      expect(sample.headerRow, ['Column 1', 'Column 2']);
      expect(sample.dataRows, hasLength(2));
    });

    test('a file containing only a header row samples as empty', () async {
      final file = write('empty.csv', 'Date,Weight\n');

      final sample = await reader.sample(file.path);

      expect(sample.isEmpty, isTrue);
    });

    test('columnValues skips blank cells in the requested column', () async {
      final file = write(
        'gaps.csv',
        'Date,Fat\n2026-07-01,15.2\n2026-07-02,\n2026-07-03,15.4\n',
      );

      final sample = await reader.sample(file.path);

      expect(sample.columnValues(1), ['15.2', '15.4']);
    });
  });

  group('rows', () {
    test('the header row is not emitted as data', () async {
      final file = write('h.csv', 'Date,Weight\n2026-07-01,78.4\n');

      final rows = await reader
          .rows(file.path, dialect: const CsvDialect(fieldDelimiter: ',', eol: '\n'))
          .toList();

      expect(rows, hasLength(1));
      expect(rows.single.fields, ['2026-07-01', '78.4']);
      // 1-based and counted over the FILE, so a diagnostic names the line the
      // user has to open.
      expect(rows.single.rowNumber, 2);
    });

    test('bytes read grow as rows are emitted', () async {
      final buffer = StringBuffer('Date,Weight\n');
      for (var i = 0; i < 2000; i++) {
        buffer.writeln('2026-07-01,7$i');
      }
      final file = write('progress.csv', buffer.toString());

      final rows = await reader
          .rows(file.path, dialect: const CsvDialect(fieldDelimiter: ',', eol: '\n'))
          .toList();

      expect(rows.last.bytesRead, greaterThan(0));
      expect(rows.last.bytesRead, lessThanOrEqualTo(file.lengthSync()));
      expect(await reader.byteLength(file.path), file.lengthSync());
    });

    test('a missing file reports a read failure rather than hanging', () async {
      expect(
        reader
            .rows(
              '${dir.path}/nope.csv',
              dialect: const CsvDialect(fieldDelimiter: ',', eol: '\n'),
            )
            .toList(),
        throwsA(isA<CsvReadException>()),
      );
    });
  });

  group('CsvRow.cell', () {
    test('a short row reports null rather than throwing', () {
      const row = CsvRow(rowNumber: 2, fields: ['2026-07-01', '78.4']);

      expect(row.cell(5), isNull);
      expect(row.cell(1), '78.4');
    });

    test('a blank cell reads as null so a gap is not parsed as zero', () {
      const row = CsvRow(rowNumber: 2, fields: ['2026-07-01', '   ']);

      expect(row.cell(1), isNull);
    });
  });
}
