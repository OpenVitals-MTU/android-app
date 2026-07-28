/// What the user decided each CSV column means, and whether that decision is
/// usable.
///
/// Plain immutable classes rather than `freezed`: these are pure domain values
/// with no JSON, no unions and no generated helpers worth the codegen round —
/// the same call `apple_health_import_records.dart` makes. `freezed` is still
/// mandatory for the view-model STATE that holds these (AGENTS.md), and value
/// equality is implemented by hand here so that state's `==` works.
library;

import 'csv_datetime_format.dart';
import 'csv_import_metric.dart';

/// What a column is used for.
enum CsvColumnRole {
  /// Not imported. The default — a column has to be opted in.
  ignore,

  /// The measurement's date and time.
  timestamp,

  /// A body metric.
  metric,
}

/// One column's assignment.
class CsvColumnMapping {
  const CsvColumnMapping({
    required this.columnIndex,
    this.role = CsvColumnRole.ignore,
    this.metric,
    this.interpretation,
  });

  final int columnIndex;
  final CsvColumnRole role;

  /// Set only when [role] is [CsvColumnRole.metric].
  final CsvImportMetric? metric;

  /// How this column's number becomes the metric's canonical value. Set only
  /// when [role] is [CsvColumnRole.metric].
  final CsvValueInterpretation? interpretation;

  bool get isMetric => role == CsvColumnRole.metric && metric != null;

  bool get isTimestamp => role == CsvColumnRole.timestamp;

  /// The interpretation to actually use, falling back to the metric's default so
  /// a mapping is never half-specified.
  CsvValueInterpretation? get effectiveInterpretation {
    final selected = metric;
    if (selected == null) return null;
    return interpretation ?? kCsvMetricCatalog[selected]?.defaultInterpretation;
  }

  CsvColumnMapping copyWith({
    CsvColumnRole? role,
    CsvImportMetric? metric,
    CsvValueInterpretation? interpretation,
    bool clearMetric = false,
    bool clearInterpretation = false,
  }) =>
      CsvColumnMapping(
        columnIndex: columnIndex,
        role: role ?? this.role,
        metric: clearMetric ? null : (metric ?? this.metric),
        interpretation: clearInterpretation
            ? null
            : (interpretation ?? this.interpretation),
      );

  @override
  bool operator ==(Object other) =>
      other is CsvColumnMapping &&
      other.columnIndex == columnIndex &&
      other.role == role &&
      other.metric == metric &&
      other.interpretation == interpretation;

  @override
  int get hashCode => Object.hash(columnIndex, role, metric, interpretation);
}

/// The complete decision: every column, plus how to read the timestamps.
class CsvImportMapping {
  const CsvImportMapping({
    required this.columns,
    this.dateTime = const CsvDateTimeSettings(),
  });

  final List<CsvColumnMapping> columns;
  final CsvDateTimeSettings dateTime;

  /// Every column mapped to a metric, in column order.
  List<CsvColumnMapping> get metricColumns =>
      columns.where((it) => it.isMetric).toList(growable: false);

  /// The single timestamp column, or null when none or several are set.
  CsvColumnMapping? get timestampColumn {
    final found = columns.where((it) => it.isTimestamp).toList();
    return found.length == 1 ? found.single : null;
  }

  /// The column supplying body weight, which a mass-share derivation needs.
  CsvColumnMapping? get weightColumn {
    for (final column in metricColumns) {
      if (column.metric == CsvImportMetric.weight) return column;
    }
    return null;
  }

  /// Whether any mapped metric derives its value from the row's weight.
  bool get needsWeightColumn => metricColumns.any(
        (it) => it.effectiveInterpretation?.needsRowWeight ?? false,
      );

  /// The Health Connect write permissions this mapping actually needs — not all
  /// seven body-composition writes, only the metrics in use.
  Set<String> get requiredWritePermissions => {
        for (final column in metricColumns)
          if (kCsvMetricCatalog[column.metric!] case final spec?)
            spec.writePermission,
      };

  CsvImportMapping copyWith({
    List<CsvColumnMapping>? columns,
    CsvDateTimeSettings? dateTime,
  }) =>
      CsvImportMapping(
        columns: columns ?? this.columns,
        dateTime: dateTime ?? this.dateTime,
      );

  /// Replaces the mapping for one column.
  CsvImportMapping withColumn(CsvColumnMapping column) => copyWith(
        columns: [
          for (final existing in columns)
            if (existing.columnIndex == column.columnIndex) column else existing,
        ],
      );

  @override
  bool operator ==(Object other) =>
      other is CsvImportMapping &&
      other.dateTime == dateTime &&
      _listEquals(other.columns, columns);

  @override
  int get hashCode => Object.hash(Object.hashAll(columns), dateTime);
}

/// Why a mapping cannot be imported yet. Each maps to one ARB string.
enum CsvMappingIssue {
  /// Nothing says when the measurements happened.
  noTimestampColumn,

  /// More than one column claims to be the timestamp.
  multipleTimestampColumns,

  /// Nothing to import.
  noMetricColumns,

  /// Two columns map to the same metric, so one would overwrite the other.
  duplicateMetric,

  /// Body fat is given as a mass but no weight column is mapped to divide by.
  massShareNeedsWeightColumn,

  /// The chosen timestamp format parses none of the sampled rows.
  timestampFormatMatchesNoSampleRow,

  /// Day-first and month-first both fit; the user has to say which.
  ambiguousDayMonthOrder,
}

/// Checks [mapping] against [sample] rows, returning every reason it cannot run.
///
/// Pure — no repository, no clock, no context — so the screen can call it on
/// every edit and the tests can assert one issue at a time.
List<CsvMappingIssue> validateCsvMapping(
  CsvImportMapping mapping,
  List<List<String>> sample,
) {
  final issues = <CsvMappingIssue>[];

  final timestamps = mapping.columns.where((it) => it.isTimestamp).toList();
  if (timestamps.isEmpty) {
    issues.add(CsvMappingIssue.noTimestampColumn);
  } else if (timestamps.length > 1) {
    issues.add(CsvMappingIssue.multipleTimestampColumns);
  }

  final metricColumns = mapping.metricColumns;
  if (metricColumns.isEmpty) {
    issues.add(CsvMappingIssue.noMetricColumns);
  }

  final seen = <CsvImportMetric>{};
  for (final column in metricColumns) {
    if (!seen.add(column.metric!)) {
      issues.add(CsvMappingIssue.duplicateMetric);
      break;
    }
  }

  // Asked of the INTERPRETATION, not the metric: "body fat as a percentage"
  // needs no weight column, "body fat as a mass in kg" does. Modelling the
  // requirement on the value is what keeps this from being a special case.
  if (mapping.needsWeightColumn && mapping.weightColumn == null) {
    issues.add(CsvMappingIssue.massShareNeedsWeightColumn);
  }

  if (timestamps.length == 1 && sample.isNotEmpty) {
    final index = timestamps.single.columnIndex;
    final values = [
      for (final row in sample)
        if (index < row.length && row[index].trim().isNotEmpty) row[index].trim(),
    ];
    if (values.isNotEmpty) {
      final parsed = values
          .where(
            (it) =>
                parseCsvWallClock(
                  it,
                  mapping.dateTime.format,
                  customPattern: mapping.dateTime.customPattern,
                ) !=
                null,
          )
          .length;
      if (parsed == 0) {
        issues.add(CsvMappingIssue.timestampFormatMatchesNoSampleRow);
      } else if (mapping.dateTime.format == CsvDateTimeFormat.auto &&
          detectCsvDateTimeFormat(values).ambiguousDayMonth) {
        // Only while the format is still `auto`. Once the user has picked
        // day-first or month-first they have answered the question, and
        // repeating it would block a mapping that is now fully specified.
        issues.add(CsvMappingIssue.ambiguousDayMonthOrder);
      }
    }
  }

  return issues;
}

/// A starting mapping for [headerRow]: everything ignored, except the first
/// column that parses as a date, which is pre-selected as the timestamp.
///
/// Deliberately does NOT guess metrics from header text — that would be the
/// vendor-preset behaviour this importer does without. The date guess is safe
/// because it is checked against the DATA, not the label.
CsvImportMapping initialCsvMapping({
  required List<String> headerRow,
  required List<List<String>> sample,
}) {
  var timestampIndex = -1;
  for (var index = 0; index < headerRow.length; index++) {
    final values = [
      for (final row in sample)
        if (index < row.length && row[index].trim().isNotEmpty) row[index].trim(),
    ];
    if (values.isEmpty) continue;
    final detection = detectCsvDateTimeFormat(values);
    if (detection.matchedRows == detection.totalRows) {
      timestampIndex = index;
      break;
    }
  }

  return CsvImportMapping(
    columns: [
      for (var index = 0; index < headerRow.length; index++)
        CsvColumnMapping(
          columnIndex: index,
          role: index == timestampIndex
              ? CsvColumnRole.timestamp
              : CsvColumnRole.ignore,
        ),
    ],
  );
}

bool _listEquals<T>(List<T> a, List<T> b) {
  if (identical(a, b)) return true;
  if (a.length != b.length) return false;
  for (var i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}
