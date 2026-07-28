/// Turning a CSV timestamp cell into an instant plus a wall-clock offset.
///
/// Health Connect stores both: the instant says when, the `zoneOffset` says what
/// the clock on the wall read. A CSV usually supplies only the second, so the
/// zone has to come from somewhere — hence [CsvTimeZoneMode].
///
/// The one rule that overrides everything: if the text carries its own offset
/// (ISO 8601 `+05:30` or `Z`), the file wins and the selected mode is ignored.
library;

import 'package:intl/intl.dart';

/// A timestamp column's shape.
///
/// Families, not one entry per pattern: a single file often mixes
/// `2026-07-01 08:12:00` and `2026-07-01`, so each family tries its patterns in
/// order and the first that consumes the whole cell wins.
enum CsvDateTimeFormat {
  /// Try every family over the sample and pick the one that parses most rows.
  auto,

  /// `2026-07-01T08:12:00`, optionally with `Z` or `+05:30`.
  iso8601,

  /// Year first: `2026-07-01 08:12:00`, `2026/07/01`.
  yearFirst,

  /// Day first: `01/07/2026 08:12`, `01.07.2026`, `01-07-2026`.
  dayFirst,

  /// Month first: `07/01/2026 08:12` — the US ordering.
  monthFirst,

  /// Whole seconds since the Unix epoch.
  epochSeconds,

  /// Whole milliseconds since the Unix epoch.
  epochMillis,

  /// A pattern the user typed, in `intl`'s `DateFormat` syntax.
  custom,
}

/// Where the wall-clock offset comes from when the text does not carry one.
enum CsvTimeZoneMode {
  /// This phone's zone, resolved against the OS tz database AT THE ROW'S DATE —
  /// so a reading from a 2019 summer gets 2019's summer offset, not today's.
  device,

  /// The text is already UTC.
  utc,

  /// One fixed offset for the whole file, for an export from a device that
  /// lived in a single offset. No DST.
  fixedOffset,
}

/// The user's timestamp choices for one import.
class CsvDateTimeSettings {
  const CsvDateTimeSettings({
    this.format = CsvDateTimeFormat.auto,
    this.customPattern,
    this.zone = CsvTimeZoneMode.device,
    this.fixedOffset,
  });

  final CsvDateTimeFormat format;

  /// Only read when [format] is [CsvDateTimeFormat.custom].
  final String? customPattern;

  final CsvTimeZoneMode zone;

  /// Only read when [zone] is [CsvTimeZoneMode.fixedOffset].
  final Duration? fixedOffset;

  CsvDateTimeSettings copyWith({
    CsvDateTimeFormat? format,
    String? customPattern,
    CsvTimeZoneMode? zone,
    Duration? fixedOffset,
  }) =>
      CsvDateTimeSettings(
        format: format ?? this.format,
        customPattern: customPattern ?? this.customPattern,
        zone: zone ?? this.zone,
        fixedOffset: fixedOffset ?? this.fixedOffset,
      );
}

/// A resolved timestamp: the instant, and the wall-clock offset to record with
/// it.
class CsvInstant {
  const CsvInstant(this.utc, this.offset);

  /// Always a UTC [DateTime].
  final DateTime utc;

  /// What the wall clock was offset by. Stored on the Health Connect record.
  final Duration offset;
}

const List<String> _yearFirstPatterns = [
  'yyyy-MM-dd HH:mm:ss',
  'yyyy-MM-dd HH:mm',
  'yyyy-MM-dd',
  'yyyy/MM/dd HH:mm:ss',
  'yyyy/MM/dd HH:mm',
  'yyyy/MM/dd',
];

const List<String> _dayFirstPatterns = [
  'dd/MM/yyyy HH:mm:ss',
  'dd/MM/yyyy HH:mm',
  'dd/MM/yyyy',
  'dd.MM.yyyy HH:mm:ss',
  'dd.MM.yyyy HH:mm',
  'dd.MM.yyyy',
  'dd-MM-yyyy HH:mm:ss',
  'dd-MM-yyyy HH:mm',
  'dd-MM-yyyy',
];

const List<String> _monthFirstPatterns = [
  'MM/dd/yyyy HH:mm:ss',
  'MM/dd/yyyy HH:mm',
  'MM/dd/yyyy',
  'MM.dd.yyyy HH:mm:ss',
  'MM.dd.yyyy HH:mm',
  'MM.dd.yyyy',
  'MM-dd-yyyy HH:mm:ss',
  'MM-dd-yyyy HH:mm',
  'MM-dd-yyyy',
];

/// The families [CsvDateTimeFormat.auto] considers, in preference order — the
/// first family with the highest match count wins.
///
/// [CsvDateTimeFormat.yearFirst] deliberately precedes [CsvDateTimeFormat.iso8601]:
/// `DateTime.parse` accepts a space separator, so it parses `2026-07-01 08:12:00`
/// too and would otherwise take the tie and label a plain year-first file "ISO
/// 8601". Both resolve that text identically, so this only decides which name the
/// user is shown — but showing the wrong one erodes trust in a screen whose whole
/// job is to let them check the interpretation. ISO still wins where it is the
/// only match: a `T` separator or an explicit offset.
///
/// A tie between [CsvDateTimeFormat.dayFirst] and [CsvDateTimeFormat.monthFirst]
/// is NOT broken by this order — see [detectCsvDateTimeFormat].
const List<CsvDateTimeFormat> _autoCandidates = [
  CsvDateTimeFormat.yearFirst,
  CsvDateTimeFormat.iso8601,
  CsvDateTimeFormat.dayFirst,
  CsvDateTimeFormat.monthFirst,
  CsvDateTimeFormat.epochMillis,
  CsvDateTimeFormat.epochSeconds,
];

/// 1990-01-01 and 2100-01-01 as epoch milliseconds.
///
/// An epoch format that accepted ANY integer would swallow a column of step
/// counts or rep counts: `1` is a valid epoch second, and auto-detection would
/// then pick that column as the timestamp and date every reading to 1970. No
/// body measurement predates 1990 or postdates 2100, so bounding it costs
/// nothing real and removes a whole class of silent mis-detection.
const int _minPlausibleEpochMillis = 631152000000;
const int _maxPlausibleEpochMillis = 4102444800000;

bool _isPlausibleEpochMillis(int millis) =>
    millis >= _minPlausibleEpochMillis && millis <= _maxPlausibleEpochMillis;

final RegExp _explicitOffsetRegex = RegExp(r'(?:Z|[+-]\d{2}:?\d{2})$');

/// Whether [text] carries its own UTC offset, which always beats the selected
/// [CsvTimeZoneMode].
bool csvTimestampHasExplicitOffset(String text) {
  final trimmed = text.trim();
  // Guard against a bare `2026-07-01` whose `-01` is a month, not an offset.
  if (!trimmed.contains('T') && !trimmed.contains(' ')) {
    return trimmed.endsWith('Z');
  }
  return _explicitOffsetRegex.hasMatch(trimmed);
}

/// The wall-clock fields of [text] under [format], as a UTC [DateTime] — i.e.
/// the numbers as written, with no zone applied yet. Null when it does not parse.
///
/// Returning "naive" fields is what lets [resolveCsvInstant] apply the zone
/// afterwards; parsing straight to a local `DateTime` would bake in the host's
/// current offset and silently shift every historical row across a DST boundary.
DateTime? parseCsvWallClock(
  String text,
  CsvDateTimeFormat format, {
  String? customPattern,
}) {
  final trimmed = text.trim();
  if (trimmed.isEmpty) return null;

  switch (format) {
    case CsvDateTimeFormat.iso8601:
      final parsed = DateTime.tryParse(trimmed);
      if (parsed == null) return null;
      // For ISO the caller handles any explicit offset; hand back the wall clock.
      return parsed.isUtc
          ? parsed
          : DateTime.utc(
              parsed.year,
              parsed.month,
              parsed.day,
              parsed.hour,
              parsed.minute,
              parsed.second,
              parsed.millisecond,
            );
    case CsvDateTimeFormat.epochSeconds:
      final seconds = int.tryParse(trimmed);
      if (seconds == null) return null;
      if (!_isPlausibleEpochMillis(seconds * 1000)) return null;
      return DateTime.fromMillisecondsSinceEpoch(seconds * 1000, isUtc: true);
    case CsvDateTimeFormat.epochMillis:
      final millis = int.tryParse(trimmed);
      if (millis == null) return null;
      // A 10-digit number is seconds; requiring 12+ digits keeps `auto` from
      // reading every epoch-seconds file as 1970.
      if (trimmed.replaceAll('-', '').length < 12) return null;
      if (!_isPlausibleEpochMillis(millis)) return null;
      return DateTime.fromMillisecondsSinceEpoch(millis, isUtc: true);
    case CsvDateTimeFormat.custom:
      final pattern = customPattern?.trim();
      if (pattern == null || pattern.isEmpty) return null;
      return _tryPatterns(trimmed, [pattern]);
    case CsvDateTimeFormat.yearFirst:
      return _tryPatterns(trimmed, _yearFirstPatterns);
    case CsvDateTimeFormat.dayFirst:
      return _tryPatterns(trimmed, _dayFirstPatterns);
    case CsvDateTimeFormat.monthFirst:
      return _tryPatterns(trimmed, _monthFirstPatterns);
    case CsvDateTimeFormat.auto:
      for (final candidate in _autoCandidates) {
        final parsed = parseCsvWallClock(trimmed, candidate);
        if (parsed != null) return parsed;
      }
      return null;
  }
}

DateTime? _tryPatterns(String text, List<String> patterns) {
  for (final pattern in patterns) {
    try {
      // parseStrict rejects trailing input, so 'yyyy-MM-dd' does not silently
      // swallow '2026-07-01 08:12:00' and drop the time.
      return DateFormat(pattern).parseStrict(text, true);
    } catch (_) {
      continue;
    }
  }
  return null;
}

/// Resolves [text] to an instant plus the offset to store with it, or null when
/// it does not parse under [settings].
CsvInstant? resolveCsvInstant(String text, CsvDateTimeSettings settings) {
  final trimmed = text.trim();
  final wall = parseCsvWallClock(
    trimmed,
    settings.format,
    customPattern: settings.customPattern,
  );
  if (wall == null) return null;

  // The file's own offset always wins over the selected mode.
  if (csvTimestampHasExplicitOffset(trimmed)) {
    final parsed = DateTime.tryParse(trimmed);
    if (parsed != null) {
      return CsvInstant(parsed.toUtc(), _offsetFromIso(trimmed) ?? Duration.zero);
    }
  }

  // Epoch formats are instants already; there is no wall clock to reinterpret.
  final isEpoch = settings.format == CsvDateTimeFormat.epochSeconds ||
      settings.format == CsvDateTimeFormat.epochMillis ||
      (settings.format == CsvDateTimeFormat.auto &&
          int.tryParse(trimmed) != null);
  if (isEpoch) {
    return CsvInstant(wall, _offsetForInstant(wall, settings));
  }

  switch (settings.zone) {
    case CsvTimeZoneMode.utc:
      return CsvInstant(wall, Duration.zero);
    case CsvTimeZoneMode.fixedOffset:
      final offset = settings.fixedOffset ?? Duration.zero;
      return CsvInstant(wall.subtract(offset), offset);
    case CsvTimeZoneMode.device:
      // Constructing a LOCAL DateTime from the wall-clock fields makes the VM
      // resolve the offset that applied on THAT date, DST included.
      final local = DateTime(
        wall.year,
        wall.month,
        wall.day,
        wall.hour,
        wall.minute,
        wall.second,
        wall.millisecond,
      );
      return CsvInstant(local.toUtc(), local.timeZoneOffset);
  }
}

/// The offset to record for an instant that already knows when it is.
Duration _offsetForInstant(DateTime utc, CsvDateTimeSettings settings) =>
    switch (settings.zone) {
      CsvTimeZoneMode.utc => Duration.zero,
      CsvTimeZoneMode.fixedOffset => settings.fixedOffset ?? Duration.zero,
      CsvTimeZoneMode.device => utc.toLocal().timeZoneOffset,
    };

Duration? _offsetFromIso(String text) {
  final trimmed = text.trim();
  if (trimmed.endsWith('Z')) return Duration.zero;
  final match = RegExp(r'([+-])(\d{2}):?(\d{2})$').firstMatch(trimmed);
  if (match == null) return null;
  final sign = match.group(1) == '-' ? -1 : 1;
  final hours = int.parse(match.group(2)!);
  final minutes = int.parse(match.group(3)!);
  return Duration(hours: sign * hours, minutes: sign * minutes);
}

/// What [detectCsvDateTimeFormat] concluded from a sample.
class CsvDateTimeDetection {
  const CsvDateTimeDetection({
    required this.format,
    required this.matchedRows,
    required this.totalRows,
    required this.ambiguousDayMonth,
  });

  /// The best family found, or [CsvDateTimeFormat.auto] when nothing parsed.
  final CsvDateTimeFormat format;
  final int matchedRows;
  final int totalRows;

  /// Day-first and month-first BOTH parsed every sampled row, so the ordering
  /// cannot be inferred from the data.
  final bool ambiguousDayMonth;

  bool get matchedNothing => matchedRows == 0;
}

/// Picks the timestamp family that parses the most of [samples].
///
/// Refuses to guess between `dd/MM` and `MM/dd` when both parse everything:
/// `01/07/2026` is genuinely undecidable, and choosing wrong silently writes a
/// year of measurements onto the wrong days. The UI must make the user choose.
CsvDateTimeDetection detectCsvDateTimeFormat(List<String> samples) {
  final values = samples
      .map((it) => it.trim())
      .where((it) => it.isNotEmpty)
      .toList(growable: false);
  if (values.isEmpty) {
    return const CsvDateTimeDetection(
      format: CsvDateTimeFormat.auto,
      matchedRows: 0,
      totalRows: 0,
      ambiguousDayMonth: false,
    );
  }

  var best = CsvDateTimeFormat.auto;
  var bestCount = 0;
  final counts = <CsvDateTimeFormat, int>{};
  for (final candidate in _autoCandidates) {
    final count = values
        .where((it) => parseCsvWallClock(it, candidate) != null)
        .length;
    counts[candidate] = count;
    if (count > bestCount) {
      best = candidate;
      bestCount = count;
    }
  }

  final dayFirst = counts[CsvDateTimeFormat.dayFirst] ?? 0;
  final monthFirst = counts[CsvDateTimeFormat.monthFirst] ?? 0;
  final ambiguous = dayFirst == values.length &&
      monthFirst == values.length &&
      (counts[CsvDateTimeFormat.iso8601] ?? 0) < values.length &&
      (counts[CsvDateTimeFormat.yearFirst] ?? 0) < values.length;

  return CsvDateTimeDetection(
    format: best,
    matchedRows: bestCount,
    totalRows: values.length,
    ambiguousDayMonth: ambiguous,
  );
}
