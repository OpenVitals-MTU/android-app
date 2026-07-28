/// Reading a CSV off disk: dialect sniffing, sampling for the mapping screen,
/// and a streaming row source for the import itself.
///
/// The only place that knows about `package:csv`. Callers get rows of strings
/// and a byte-progress count, so the tokenizer can be replaced without touching
/// them.
library;

import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:csv/csv.dart';

/// How many data rows the mapping screen samples. The subscription is cancelled
/// after this, so picking a 400 MB export still opens instantly.
const int kCsvPreviewRows = 50;

/// Delimiters worth guessing between. Semicolon matters: it is what a European
/// locale's spreadsheet exports, because the comma is its decimal separator.
const List<String> kCsvFieldDelimiters = [',', ';', '\t', '|'];

/// The separator and line ending a file actually uses.
///
/// Both must be right. `package:csv` does not fail on a wrong line ending — it
/// returns ONE row with the entire file crammed into the last field, which
/// downstream looks like a file with a single unparsable row rather than a
/// mis-sniffed dialect. [CsvTableReader] therefore sniffs it rather than
/// assuming, and [CsvSample.looksMisparsed] catches the case anyway.
class CsvDialect {
  const CsvDialect({required this.fieldDelimiter, required this.eol});

  final String fieldDelimiter;
  final String eol;

  CsvDialect copyWith({String? fieldDelimiter, String? eol}) => CsvDialect(
        fieldDelimiter: fieldDelimiter ?? this.fieldDelimiter,
        eol: eol ?? this.eol,
      );
}

/// What the mapping screen needs: the head of the file, already tokenised.
class CsvSample {
  const CsvSample({
    required this.dialect,
    required this.headerRow,
    required this.dataRows,
    required this.hasHeaderRow,
  });

  final CsvDialect dialect;

  /// The header cells, or synthesised `Column 1..n` labels when
  /// [hasHeaderRow] is false.
  final List<String> headerRow;

  /// Up to [kCsvPreviewRows] data rows.
  final List<List<String>> dataRows;

  final bool hasHeaderRow;

  int get columnCount => headerRow.length;

  bool get isEmpty => headerRow.isEmpty || dataRows.isEmpty;

  /// A single very wide row whose cells contain line breaks — the signature of a
  /// mis-sniffed line ending, which otherwise reads as "one unparsable row".
  bool get looksMisparsed =>
      dataRows.isEmpty &&
      headerRow.length <= 2 &&
      headerRow.any((cell) => cell.contains('\n'));

  /// The values of column [index] across the sampled rows, skipping blanks.
  List<String> columnValues(int index) => [
        for (final row in dataRows)
          if (index < row.length && row[index].trim().isNotEmpty)
            row[index].trim(),
      ];
}

/// Thrown when a picked file cannot be read at all. The view-model turns it into
/// a screen error; nothing below this layer catches it.
class CsvReadException implements Exception {
  const CsvReadException(this.message);

  final String message;

  @override
  String toString() => 'CsvReadException: $message';
}

/// Reads CSV files. Injected so tests drive it off a temp file rather than a
/// picker.
class CsvTableReader {
  const CsvTableReader();

  /// Guesses the dialect from the first chunk of [path].
  ///
  /// The delimiter is whichever candidate appears most often OUTSIDE quotes on
  /// the first line — counting inside quotes would pick the comma out of
  /// `"Weight (kg)","Fat mass (kg)"` in a semicolon file. The line ending is
  /// CRLF when the first break is preceded by a carriage return.
  Future<CsvDialect> sniffDialect(String path) async {
    final head = await _readHead(path);
    if (head.isEmpty) {
      return const CsvDialect(fieldDelimiter: ',', eol: '\n');
    }

    final firstBreak = head.indexOf('\n');
    final eol =
        firstBreak > 0 && head[firstBreak - 1] == '\r' ? '\r\n' : '\n';
    final firstLine =
        firstBreak < 0 ? head : head.substring(0, firstBreak).trimRight();

    var best = ',';
    var bestCount = 0;
    for (final candidate in kCsvFieldDelimiters) {
      final count = _countOutsideQuotes(firstLine, candidate);
      if (count > bestCount) {
        best = candidate;
        bestCount = count;
      }
    }
    return CsvDialect(fieldDelimiter: best, eol: eol);
  }

  /// Reads the header plus up to [maxRows] data rows, then stops reading.
  Future<CsvSample> sample(
    String path, {
    CsvDialect? dialect,
    bool hasHeaderRow = true,
    int maxRows = kCsvPreviewRows,
  }) async {
    final resolved = dialect ?? await sniffDialect(path);
    final rows = <List<String>>[];

    final completer = Completer<void>();
    late final StreamSubscription<List<dynamic>> subscription;
    subscription = _rowStream(path, resolved).listen(
      (row) {
        rows.add(row.map((cell) => '$cell').toList(growable: false));
        // +1 for the header, which is not a data row.
        if (rows.length >= maxRows + (hasHeaderRow ? 1 : 0)) {
          subscription.cancel();
          if (!completer.isCompleted) completer.complete();
        }
      },
      onError: (Object error) {
        if (!completer.isCompleted) completer.completeError(error);
      },
      onDone: () {
        if (!completer.isCompleted) completer.complete();
      },
      cancelOnError: true,
    );
    await completer.future;

    if (rows.isEmpty) {
      return CsvSample(
        dialect: resolved,
        headerRow: const [],
        dataRows: const [],
        hasHeaderRow: hasHeaderRow,
      );
    }

    final width = rows.map((it) => it.length).reduce((a, b) => a > b ? a : b);
    final header = hasHeaderRow
        ? _padded(rows.first, width)
        : [for (var i = 1; i <= width; i++) 'Column $i'];
    final data = hasHeaderRow ? rows.skip(1).toList() : rows;

    return CsvSample(
      dialect: resolved,
      headerRow: header,
      dataRows: data,
      hasHeaderRow: hasHeaderRow,
    );
  }

  /// Every data row in [path], in file order, with the byte offset reached so
  /// far so a caller can show determinate progress without counting rows first.
  ///
  /// The header row is dropped when [hasHeaderRow]. Rows are emitted as they are
  /// tokenised; the file is never held in memory.
  Stream<CsvRow> rows(
    String path, {
    required CsvDialect dialect,
    bool hasHeaderRow = true,
  }) async* {
    // Counted on the RAW byte stream, before decoding, so it is comparable with
    // [byteLength]. It lags the emitted row by whatever the decoder has buffered,
    // which is invisible in a progress bar and costs nothing to be wrong about.
    final progress = _ByteCounter();
    var rowNumber = 0;
    await for (final row in _rowStream(path, dialect, progress)) {
      rowNumber++;
      if (hasHeaderRow && rowNumber == 1) continue;
      yield CsvRow(
        rowNumber: rowNumber,
        fields: row.map((cell) => '$cell').toList(growable: false),
        bytesRead: progress.value,
      );
    }
  }

  /// Total bytes, for the progress denominator. Zero when unknown.
  Future<int> byteLength(String path) async {
    try {
      return await File(path).length();
    } catch (_) {
      return 0;
    }
  }

  Stream<List<dynamic>> _rowStream(
    String path,
    CsvDialect dialect, [
    _ByteCounter? progress,
  ]) {
    final file = File(path);
    var bytes = file.openRead();
    if (progress != null) {
      bytes = bytes.map((chunk) {
        progress.value += chunk.length;
        return chunk;
      });
    }
    return bytes
        // allowMalformed keeps one bad byte from killing an otherwise fine
        // export; the Utf8Decoder also strips a leading BOM for us.
        .transform(const Utf8Decoder(allowMalformed: true))
        .transform(
          CsvToListConverter(
            fieldDelimiter: dialect.fieldDelimiter,
            eol: dialect.eol,
            // Every cell stays a String: the column's interpretation decides how
            // to read it, and `4,5` in a semicolon file must not become a list.
            shouldParseNumbers: false,
          ),
        )
        .handleError(
          (Object error) => throw CsvReadException('$error'),
        );
  }

  Future<String> _readHead(String path, {int bytes = 64 * 1024}) async {
    try {
      final file = File(path);
      final chunks = <int>[];
      await for (final chunk in file.openRead(0, bytes)) {
        chunks.addAll(chunk);
        if (chunks.length >= bytes) break;
      }
      return const Utf8Decoder(allowMalformed: true).convert(chunks);
    } on FileSystemException catch (error) {
      throw CsvReadException(error.message);
    }
  }
}

/// One tokenised data row and its 1-based line number in the file, so a
/// diagnostic can name the row the user has to go look at.
class CsvRow {
  const CsvRow({
    required this.rowNumber,
    required this.fields,
    this.bytesRead = 0,
  });

  final int rowNumber;
  final List<String> fields;

  /// Raw bytes consumed from the file by the time this row was emitted, for a
  /// determinate progress bar. Approximate by however much the decoder buffered.
  final int bytesRead;

  /// The trimmed cell at [index], or null when the row is too short or blank
  /// there.
  String? cell(int index) {
    if (index < 0 || index >= fields.length) return null;
    final value = fields[index].trim();
    return value.isEmpty ? null : value;
  }
}

/// A mutable byte tally shared between the raw-byte tap and the row emitter.
class _ByteCounter {
  int value = 0;
}

List<String> _padded(List<String> row, int width) => [
      ...row,
      for (var i = row.length; i < width; i++) 'Column ${i + 1}',
    ];

int _countOutsideQuotes(String line, String needle) {
  var count = 0;
  var inQuotes = false;
  for (var i = 0; i < line.length; i++) {
    final char = line[i];
    if (char == '"') {
      inQuotes = !inQuotes;
    } else if (!inQuotes && line.startsWith(needle, i)) {
      count++;
    }
  }
  return count;
}
