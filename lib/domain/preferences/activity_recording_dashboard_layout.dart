import 'package:meta/meta.dart';

/// Port of the Kotlin `ActivityRecordingDashboardLayout` and its supporting
/// types — the per-activity recording dashboard grid model. Persisted through
/// `PreferencesRepository`. Plain immutable Dart (not freezed) because
/// [ActivityRecordingDashboardItemSize] is used as a `Map` key and needs a
/// validating constructor.

/// The grid template a layout is laid out on.
///
/// Note (fidelity): the Kotlin layout always coerces to [largeTop] via a private
/// `RecordingDashboardTemplate` constant, regardless of the stored/selected
/// template. That behaviour is preserved here.
enum ActivityRecordingDashboardTemplate {
  twoByFour(columns: 2, rows: 4),
  threeByFour(columns: 3, rows: 4),
  largeTop(columns: 4, rows: 6);

  const ActivityRecordingDashboardTemplate({
    required this.columns,
    required this.rows,
  });

  final int columns;
  final int rows;

  int get capacity => columns * rows;

  /// Enum name used for persistence (mirrors Kotlin `name`), e.g. `LARGE_TOP`.
  String get storageName {
    switch (this) {
      case ActivityRecordingDashboardTemplate.twoByFour:
        return 'TWO_BY_FOUR';
      case ActivityRecordingDashboardTemplate.threeByFour:
        return 'THREE_BY_FOUR';
      case ActivityRecordingDashboardTemplate.largeTop:
        return 'LARGE_TOP';
    }
  }

  static ActivityRecordingDashboardTemplate? fromStorage(String value) {
    for (final template in values) {
      if (template.storageName == value) return template;
    }
    return null;
  }
}

/// A metric shown in a recording dashboard cell.
enum ActivityRecordingDashboardField {
  heartRate('HEART_RATE'),
  cadence('CADENCE'),
  speed('SPEED'),
  distance('DISTANCE'),
  duration('DURATION'),
  movingTime('MOVING_TIME'),
  averageSpeed('AVERAGE_SPEED'),
  averageMovingSpeed('AVERAGE_MOVING_SPEED'),
  maxSpeed('MAX_SPEED'),
  elevationGain('ELEVATION_GAIN'),
  power('POWER'),
  steps('STEPS');

  const ActivityRecordingDashboardField(this.storageName);

  /// Enum name used for persistence (mirrors Kotlin `name`).
  final String storageName;

  static ActivityRecordingDashboardField? fromStorage(String value) {
    for (final field in values) {
      if (field.storageName == value) return field;
    }
    return null;
  }
}

/// A cell's column/row span. Values must be positive.
@immutable
class ActivityRecordingDashboardItemSize {
  ActivityRecordingDashboardItemSize({
    required this.columnSpan,
    required this.rowSpan,
  }) {
    if (columnSpan <= 0) {
      throw ArgumentError('columnSpan must be positive');
    }
    if (rowSpan <= 0) {
      throw ArgumentError('rowSpan must be positive');
    }
  }

  final int columnSpan;
  final int rowSpan;

  String toPreferenceString() => '${columnSpan}x$rowSpan';

  static final ActivityRecordingDashboardItemSize small =
      ActivityRecordingDashboardItemSize(columnSpan: 1, rowSpan: 1);
  static final ActivityRecordingDashboardItemSize wide =
      ActivityRecordingDashboardItemSize(columnSpan: 2, rowSpan: 1);
  static final ActivityRecordingDashboardItemSize tall =
      ActivityRecordingDashboardItemSize(columnSpan: 1, rowSpan: 2);
  static final ActivityRecordingDashboardItemSize large =
      ActivityRecordingDashboardItemSize(columnSpan: 2, rowSpan: 2);

  static ActivityRecordingDashboardItemSize? fromPreferenceString(String value) {
    switch (value) {
      case 'SMALL':
        return small;
      case 'WIDE':
        return wide;
      case 'TALL':
        return tall;
      case 'LARGE':
        return large;
    }
    final parts = value.split('x');
    if (parts.length != 2) return null;
    final columns = int.tryParse(parts[0]);
    final rows = int.tryParse(parts[1]);
    if (columns == null || rows == null) return null;
    if (columns <= 0 || rows <= 0) return null;
    return ActivityRecordingDashboardItemSize(
      columnSpan: columns,
      rowSpan: rows,
    );
  }

  ActivityRecordingDashboardItemSize _coercedFor(
    ActivityRecordingDashboardTemplate template,
  ) =>
      ActivityRecordingDashboardItemSize(
        columnSpan: columnSpan.clamp(1, template.columns),
        rowSpan: rowSpan.clamp(1, template.rows),
      );

  @override
  bool operator ==(Object other) =>
      other is ActivityRecordingDashboardItemSize &&
      other.columnSpan == columnSpan &&
      other.rowSpan == rowSpan;

  @override
  int get hashCode => Object.hash(columnSpan, rowSpan);
}

/// A dashboard cell: a [field] rendered at a [size].
@immutable
class ActivityRecordingDashboardItem {
  ActivityRecordingDashboardItem({
    required this.field,
    ActivityRecordingDashboardItemSize? size,
  }) : size = size ?? ActivityRecordingDashboardItemSize.small;

  final ActivityRecordingDashboardField field;
  final ActivityRecordingDashboardItemSize size;

  ActivityRecordingDashboardItem copyWith({
    ActivityRecordingDashboardItemSize? size,
  }) =>
      ActivityRecordingDashboardItem(field: field, size: size ?? this.size);

  @override
  bool operator ==(Object other) =>
      other is ActivityRecordingDashboardItem &&
      other.field == field &&
      other.size == size;

  @override
  int get hashCode => Object.hash(field, size);
}

/// An item placed at a concrete grid position.
@immutable
class ActivityRecordingDashboardItemPlacement {
  const ActivityRecordingDashboardItemPlacement({
    required this.item,
    required this.row,
    required this.column,
    required this.rowSpan,
    required this.columnSpan,
  });

  final ActivityRecordingDashboardItem item;
  final int row;
  final int column;
  final int rowSpan;
  final int columnSpan;

  @override
  bool operator ==(Object other) =>
      other is ActivityRecordingDashboardItemPlacement &&
      other.item == item &&
      other.row == row &&
      other.column == column &&
      other.rowSpan == rowSpan &&
      other.columnSpan == columnSpan;

  @override
  int get hashCode => Object.hash(item, row, column, rowSpan, columnSpan);
}

/// The recording dashboard layout: an ordered [fields] list plus per-field
/// [sizes], laid out on [template].
@immutable
class ActivityRecordingDashboardLayout {
  const ActivityRecordingDashboardLayout({
    this.template = ActivityRecordingDashboardTemplate.largeTop,
    List<ActivityRecordingDashboardField>? fields,
    Map<ActivityRecordingDashboardField, ActivityRecordingDashboardItemSize>?
        sizes,
  })  : fields = fields ?? defaultFields,
        sizes = sizes ?? const {};

  final ActivityRecordingDashboardTemplate template;
  final List<ActivityRecordingDashboardField> fields;
  final Map<ActivityRecordingDashboardField, ActivityRecordingDashboardItemSize>
      sizes;

  int get capacity => _recordingDashboardTemplate.capacity;

  List<ActivityRecordingDashboardItem> get items => [
        for (var index = 0; index < fields.length; index++)
          ActivityRecordingDashboardItem(
            field: fields[index],
            size: (sizes[fields[index]] ?? _defaultItemSize(index))
                ._coercedFor(_recordingDashboardTemplate),
          ),
      ];

  ActivityRecordingDashboardLayout copyWith({
    ActivityRecordingDashboardTemplate? template,
    List<ActivityRecordingDashboardField>? fields,
    Map<ActivityRecordingDashboardField, ActivityRecordingDashboardItemSize>?
        sizes,
  }) =>
      ActivityRecordingDashboardLayout(
        template: template ?? this.template,
        fields: fields ?? this.fields,
        sizes: sizes ?? this.sizes,
      );

  ActivityRecordingDashboardLayout normalized() {
    final distinctFields = <ActivityRecordingDashboardField>[];
    for (final field in fields) {
      if (!distinctFields.contains(field)) distinctFields.add(field);
    }
    var normalizedItems = _placedIn(
      [
        for (var index = 0; index < distinctFields.length; index++)
          ActivityRecordingDashboardItem(
            field: distinctFields[index],
            size: (sizes[distinctFields[index]] ?? _defaultItemSize(index))
                ._coercedFor(_recordingDashboardTemplate),
          ),
      ],
      _recordingDashboardTemplate,
    ).map((placement) => placement.item).toList();
    if (normalizedItems.isEmpty) {
      normalizedItems = _placedIn(
        [
          for (var index = 0; index < defaultFields.length; index++)
            ActivityRecordingDashboardItem(
              field: defaultFields[index],
              size: _defaultItemSize(index)
                  ._coercedFor(_recordingDashboardTemplate),
            ),
        ],
        _recordingDashboardTemplate,
      ).map((placement) => placement.item).toList();
    }
    return copyWith(
      template: _recordingDashboardTemplate,
      fields: [for (final item in normalizedItems) item.field],
      sizes: {for (final item in normalizedItems) item.field: item.size},
    );
  }

  ActivityRecordingDashboardLayout withTemplate(
    ActivityRecordingDashboardTemplate template,
  ) =>
      copyWith(template: _recordingDashboardTemplate).normalized();

  ActivityRecordingDashboardLayout withFieldSize(
    ActivityRecordingDashboardField field,
    ActivityRecordingDashboardItemSize size,
  ) {
    final updated = copyWith(
      template: _recordingDashboardTemplate,
      sizes: {
        ...sizes,
        field: size._coercedFor(_recordingDashboardTemplate),
      },
    ).normalized();
    return updated.fields.contains(field) ? updated : this;
  }

  List<ActivityRecordingDashboardItemPlacement> placements() =>
      _placedIn(normalized().items, _recordingDashboardTemplate);

  static const List<ActivityRecordingDashboardField> defaultFields = [
    ActivityRecordingDashboardField.heartRate,
    ActivityRecordingDashboardField.cadence,
    ActivityRecordingDashboardField.speed,
    ActivityRecordingDashboardField.distance,
    ActivityRecordingDashboardField.duration,
  ];

  @override
  bool operator ==(Object other) {
    if (other is! ActivityRecordingDashboardLayout) return false;
    if (other.template != template) return false;
    if (other.fields.length != fields.length) return false;
    for (var index = 0; index < fields.length; index++) {
      if (other.fields[index] != fields[index]) return false;
    }
    if (other.sizes.length != sizes.length) return false;
    for (final entry in sizes.entries) {
      if (other.sizes[entry.key] != entry.value) return false;
    }
    return true;
  }

  @override
  int get hashCode => Object.hash(
        template,
        Object.hashAll(fields),
        Object.hashAllUnordered(
          sizes.entries.map((e) => Object.hash(e.key, e.value)),
        ),
      );
}

const ActivityRecordingDashboardTemplate _recordingDashboardTemplate =
    ActivityRecordingDashboardTemplate.largeTop;

ActivityRecordingDashboardItemSize _defaultItemSize(int index) => index == 0
    ? ActivityRecordingDashboardItemSize(columnSpan: 4, rowSpan: 2)
    : ActivityRecordingDashboardItemSize.small;

List<ActivityRecordingDashboardItemPlacement> _placedIn(
  List<ActivityRecordingDashboardItem> items,
  ActivityRecordingDashboardTemplate template,
) {
  final occupied = List.generate(
    template.rows,
    (_) => List<bool>.filled(template.columns, false),
  );
  final placements = <ActivityRecordingDashboardItemPlacement>[];
  for (final item in items) {
    final size = item.size._coercedFor(template);
    final position = _firstOpenPosition(
      occupied: occupied,
      rowSpan: size.rowSpan,
      columnSpan: size.columnSpan,
    );
    if (position == null) break;
    for (var rowOffset = 0; rowOffset < size.rowSpan; rowOffset++) {
      for (var columnOffset = 0;
          columnOffset < size.columnSpan;
          columnOffset++) {
        occupied[position.$1 + rowOffset][position.$2 + columnOffset] = true;
      }
    }
    placements.add(
      ActivityRecordingDashboardItemPlacement(
        item: item.copyWith(size: size),
        row: position.$1,
        column: position.$2,
        rowSpan: size.rowSpan,
        columnSpan: size.columnSpan,
      ),
    );
  }
  return placements;
}

/// Returns the top-left `(row, column)` of the first opening that fits a
/// [rowSpan]×[columnSpan] block, or `null` if none.
(int, int)? _firstOpenPosition({
  required List<List<bool>> occupied,
  required int rowSpan,
  required int columnSpan,
}) {
  final rows = occupied.length;
  if (rows == 0) return null;
  final columns = occupied.first.length;
  for (var row = 0; row <= rows - rowSpan; row++) {
    for (var column = 0; column <= columns - columnSpan; column++) {
      var fits = true;
      for (var rowOffset = 0; rowOffset < rowSpan && fits; rowOffset++) {
        for (var columnOffset = 0;
            columnOffset < columnSpan && fits;
            columnOffset++) {
          if (occupied[row + rowOffset][column + columnOffset]) fits = false;
        }
      }
      if (fits) return (row, column);
    }
  }
  return null;
}
