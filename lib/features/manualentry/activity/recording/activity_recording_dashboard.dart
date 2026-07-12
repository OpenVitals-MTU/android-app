import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';

import '../../../../core/presentation/display_value.dart';
import '../../../../core/presentation/elapsed_format.dart';
import '../../../../core/presentation/reorder.dart';
import '../../../../core/presentation/unit_formatter.dart';
import '../../../../domain/model/ble_sensor_models.dart';
import '../../../../domain/preferences/activity_recording_dashboard_layout.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_surface.dart';
import '../../../../ui/components/widget_edit_controls.dart';
import '../../../../domain/model/activity_entry_types.dart';
import 'activity_recording.dart';
import 'activity_recording_sensor_ui.dart';

/// Port of the Kotlin `ActivityRecordingDashboard.kt`: the live metric grid and
/// its edit mode.

/// Kotlin `RecordingDashboardResizeStep`.
// formatRecordingElapsed moved to core (the session charts label their axis with
// it, and ui/ must not reach into features/). Re-exported so the recording files
// that already import it from here keep working.
export '../../../../core/presentation/elapsed_format.dart'
    show formatRecordingElapsed;

const double kRecordingDashboardResizeStep = 44;

/// Kotlin `RecordingDashboardStat`.
class RecordingDashboardStat {
  const RecordingDashboardStat({required this.value, required this.label});

  final DisplayValue value;
  final String label;
}

/// Kotlin `ActivityRecordingDashboardField.labelRes`.
String recordingDashboardFieldLabel(
  ActivityRecordingDashboardField field,
  AppLocalizations l10n,
) =>
    switch (field) {
      ActivityRecordingDashboardField.heartRate =>
        l10n.activityRecordingLiveHeartRate,
      ActivityRecordingDashboardField.cadence => l10n.activityRecordingLiveCadence,
      ActivityRecordingDashboardField.speed => l10n.activityEntryRecordingSpeed,
      ActivityRecordingDashboardField.distance =>
        l10n.activityEntryRecordingDistance,
      ActivityRecordingDashboardField.duration =>
        l10n.activityEntryRecordingTotalTime,
      ActivityRecordingDashboardField.movingTime =>
        l10n.activityEntryRecordingMovingTime,
      ActivityRecordingDashboardField.averageSpeed =>
        l10n.activityEntryRecordingAverageSpeed,
      ActivityRecordingDashboardField.averageMovingSpeed =>
        l10n.activityEntryRecordingAverageMovingSpeed,
      ActivityRecordingDashboardField.maxSpeed =>
        l10n.activityEntryRecordingMaxSpeed,
      ActivityRecordingDashboardField.elevationGain =>
        l10n.activityEntryRecordingElevationGain,
      ActivityRecordingDashboardField.power => l10n.activityRecordingLivePower,
      ActivityRecordingDashboardField.steps => l10n.activityEntryStepsTitle,
    };

/// Kotlin `availableRecordingDashboardFields`: a timed activity has no distance
/// or speed to show.
List<ActivityRecordingDashboardField> availableRecordingDashboardFields(
  ActivityRecordingState state,
) {
  if (state.recordingKind == ActivityRecordingKind.timed) {
    return const [
      ActivityRecordingDashboardField.heartRate,
      ActivityRecordingDashboardField.duration,
      ActivityRecordingDashboardField.movingTime,
      ActivityRecordingDashboardField.power,
    ];
  }
  return [
    ActivityRecordingDashboardField.heartRate,
    ActivityRecordingDashboardField.cadence,
    ActivityRecordingDashboardField.speed,
    ActivityRecordingDashboardField.distance,
    ActivityRecordingDashboardField.duration,
    ActivityRecordingDashboardField.movingTime,
    ActivityRecordingDashboardField.averageSpeed,
    ActivityRecordingDashboardField.averageMovingSpeed,
    ActivityRecordingDashboardField.maxSpeed,
    ActivityRecordingDashboardField.elevationGain,
    ActivityRecordingDashboardField.power,
    if (activityEntryTypeById(state.activityTypeId)?.supportsStepCounting ?? false)
      ActivityRecordingDashboardField.steps,
  ];
}

/// Kotlin `recordingDashboardStats`.
Map<ActivityRecordingDashboardField, RecordingDashboardStat>
    recordingDashboardStats({
  required ActivityRecordingState state,
  required Duration totalTime,
  required Duration movingTime,
  required DateTime now,
  required UnitFormatter unitFormatter,
  required AppLocalizations l10n,
}) {
  final waiting = l10n.activityRecordingSensorsWaitingShort;
  final hasHeartRateSensor = state.bleDeviceStatuses.any(
    (status) => status.capabilities.contains(BleSensorCapability.heartRate),
  );
  final bpm = state.currentHeartRateBpm;
  final heartRate = bpm != null
      ? unitFormatter.heartRate(bpm)
      : DisplayValue(
          hasHeartRateSensor && state.bleHeartRateNoSignal
              ? l10n.activityRecordingSensorsNoSignalShort
              : waiting,
          'bpm',
        );
  final cadence = state.currentCyclingCadenceRpm ?? state.currentRunningCadenceRpm;
  final speed = state.currentSensorSpeedMetersPerSecond ??
      state.effectiveCurrentSpeedMetersPerSecond(now);
  final power = state.currentPowerWatts;

  return {
    ActivityRecordingDashboardField.heartRate: RecordingDashboardStat(
      value: heartRate,
      label: l10n.activityRecordingLiveHeartRate,
    ),
    ActivityRecordingDashboardField.cadence: RecordingDashboardStat(
      value: cadence != null
          ? unitFormatter.cadence(cadence.toDouble())
          : DisplayValue(waiting, 'rpm'),
      label: l10n.activityRecordingLiveCadence,
    ),
    ActivityRecordingDashboardField.speed: RecordingDashboardStat(
      value: unitFormatter.speed(speed),
      label: l10n.activityEntryRecordingSpeed,
    ),
    ActivityRecordingDashboardField.distance: RecordingDashboardStat(
      value: unitFormatter.distance(state.distanceMeters),
      label: l10n.activityEntryRecordingDistance,
    ),
    ActivityRecordingDashboardField.duration: RecordingDashboardStat(
      value: DisplayValue(formatRecordingElapsed(totalTime), ''),
      label: l10n.activityEntryRecordingTotalTime,
    ),
    ActivityRecordingDashboardField.movingTime: RecordingDashboardStat(
      value: DisplayValue(formatRecordingElapsed(movingTime), ''),
      label: l10n.activityEntryRecordingMovingTime,
    ),
    ActivityRecordingDashboardField.averageSpeed: RecordingDashboardStat(
      value: unitFormatter.averageSpeed(
          state.distanceMeters, totalTime.inMilliseconds),
      label: l10n.activityEntryRecordingAverageSpeed,
    ),
    ActivityRecordingDashboardField.averageMovingSpeed: RecordingDashboardStat(
      value: unitFormatter.averageSpeed(
          state.distanceMeters, movingTime.inMilliseconds),
      label: l10n.activityEntryRecordingAverageMovingSpeed,
    ),
    ActivityRecordingDashboardField.maxSpeed: RecordingDashboardStat(
      value: unitFormatter.speed(state.maxSpeedMetersPerSecond),
      label: l10n.activityEntryRecordingMaxSpeed,
    ),
    ActivityRecordingDashboardField.elevationGain: RecordingDashboardStat(
      value: unitFormatter.elevation(state.displayElevationGainedMeters()),
      label: l10n.activityEntryRecordingElevationGain,
    ),
    ActivityRecordingDashboardField.power: RecordingDashboardStat(
      value: power != null
          ? unitFormatter.power(power)
          : DisplayValue(waiting, 'W'),
      label: l10n.activityRecordingLivePower,
    ),
    ActivityRecordingDashboardField.steps: RecordingDashboardStat(
      value: DisplayValue(unitFormatter.count(state.repetitionCount), ''),
      label: l10n.activityEntryStepsTitle,
    ),
  };
}

// ── Layout operations (Kotlin extension functions) ──────────────────────────

extension RecordingDashboardLayoutOps on ActivityRecordingDashboardLayout {
  /// Kotlin `withAvailableFields`: drop fields this activity cannot measure,
  /// falling back to the defaults (then to anything available) if that empties
  /// the layout.
  ActivityRecordingDashboardLayout withAvailableFields(
    List<ActivityRecordingDashboardField> availableFields,
  ) {
    final available = availableFields.toSet();
    final normalizedLayout = normalized();
    final items = [
      for (final item in normalizedLayout.items)
        if (available.contains(item.field)) item,
    ];
    if (items.isNotEmpty) {
      return normalizedLayout
          .copyWith(
            fields: [for (final item in items) item.field],
            sizes: {for (final item in items) item.field: item.size},
          )
          .normalized();
    }
    var fields = [
      for (final field in ActivityRecordingDashboardLayout.defaultFields)
        if (available.contains(field)) field,
    ];
    if (fields.isEmpty) fields = availableFields;
    final fieldSet = fields.toSet();
    return normalizedLayout
        .copyWith(
          fields: fields,
          sizes: {
            for (final entry in normalizedLayout.sizes.entries)
              if (fieldSet.contains(entry.key)) entry.key: entry.value,
          },
        )
        .normalized();
  }

  /// Kotlin `withMovedFieldToTarget`: drop-on-target semantics — the dragged
  /// field lands exactly where the target sat.
  ///
  /// Shares [reorderOntoDropTarget] with the dashboard summary and the add-entry
  /// hub; it is the same Kotlin `moveWidgetToTarget` rule.
  ActivityRecordingDashboardLayout withMovedFieldToTarget(
    ActivityRecordingDashboardField field,
    ActivityRecordingDashboardField targetField,
  ) =>
      withMovedFieldIndex(fields.indexOf(field), fields.indexOf(targetField));

  /// The index-addressed form, for a [ReorderableEditTile] drop.
  ActivityRecordingDashboardLayout withMovedFieldIndex(int from, int to) {
    if (from < 0 || to < 0 || from == to) return this;
    return copyWith(fields: reorderOntoDropTarget(fields, from, to))
        .normalized();
  }

  /// Kotlin `withRemovedField`: the last field cannot be removed.
  ActivityRecordingDashboardLayout withRemovedField(
    ActivityRecordingDashboardField field,
  ) {
    if (fields.length <= 1) return this;
    return copyWith(
      fields: [
        for (final existing in fields)
          if (existing != field) existing,
      ],
      sizes: {
        for (final entry in sizes.entries)
          if (entry.key != field) entry.key: entry.value,
      },
    ).normalized();
  }

  /// Kotlin `withAddedField`: a no-op when the grid has no room left, since
  /// `normalized()` would silently drop the field again.
  ActivityRecordingDashboardLayout withAddedField(
    ActivityRecordingDashboardField field,
  ) {
    if (fields.contains(field)) return this;
    final updated = copyWith(
      fields: [...fields, field],
      sizes: {
        for (final entry in sizes.entries)
          if (entry.key != field) entry.key: entry.value,
      },
    ).normalized();
    return updated.fields.contains(field) ? updated : this;
  }
}

extension RecordingDashboardItemSizeOps on ActivityRecordingDashboardItemSize {
  static const ActivityRecordingDashboardTemplate _template =
      ActivityRecordingDashboardTemplate.largeTop;

  /// Kotlin `nextSize`: widen first, then grow taller.
  ActivityRecordingDashboardItemSize nextSize() =>
      columnSpan < _template.columns
          ? ActivityRecordingDashboardItemSize(
              columnSpan: columnSpan + 1, rowSpan: rowSpan)
          : ActivityRecordingDashboardItemSize(
              columnSpan: columnSpan,
              rowSpan: (rowSpan + 1).clamp(1, _template.rows),
            );

  /// Kotlin `previousSize`: shrink height first, then width.
  ActivityRecordingDashboardItemSize previousSize() => rowSpan > 1
      ? ActivityRecordingDashboardItemSize(
          columnSpan: columnSpan, rowSpan: rowSpan - 1)
      : ActivityRecordingDashboardItemSize(
          columnSpan: (columnSpan - 1).clamp(1, _template.columns),
          rowSpan: rowSpan,
        );

  bool canGrow() =>
      columnSpan < _template.columns || rowSpan < _template.rows;

  bool canShrink() => columnSpan > 1 || rowSpan > 1;

  /// Kotlin `hasCompactMetricText`.
  bool hasCompactMetricText() => columnSpan == 1 || rowSpan == 1;

  /// Kotlin `hasRoomyMetricText`.
  bool hasRoomyMetricText() =>
      columnSpan >= 3 || rowSpan >= 3 || (columnSpan >= 2 && rowSpan >= 2);

  /// Kotlin `sizeForResizeDrag`.
  ActivityRecordingDashboardItemSize sizeForResizeDrag(
    Offset dragOffset,
    double stepPx,
  ) =>
      ActivityRecordingDashboardItemSize(
        columnSpan: (columnSpan + dragSteps(dragOffset.dx, stepPx))
            .clamp(1, _template.columns),
        rowSpan:
            (rowSpan + dragSteps(dragOffset.dy, stepPx)).clamp(1, _template.rows),
      );
}

/// Kotlin `Float.dragSteps`: whole steps only, so a drag shorter than one step
/// resizes nothing.
///
/// Kotlin brackets this in an explicit `>= stepPx || <= -stepPx` check, which is
/// redundant — truncation toward zero already yields 0 inside that band — so it
/// is not reproduced here.
int dragSteps(double offset, double stepPx) {
  if (stepPx <= 0) return 0;
  return (offset / stepPx).truncate();
}

/// Kotlin `recordingDashboardLazyGridRows`: how many rows the items occupy once
/// wrapped into [columns].
int recordingDashboardLazyGridRows({
  required List<ActivityRecordingDashboardItem> items,
  required int columns,
}) {
  var committedRows = 0;
  var lineColumns = 0;
  var lineRows = 0;

  for (final item in items) {
    final columnSpan = item.size.columnSpan.clamp(1, columns);
    final rowSpan = item.size.rowSpan < 1 ? 1 : item.size.rowSpan;
    if (lineColumns > 0 && lineColumns + columnSpan > columns) {
      committedRows += lineRows;
      lineColumns = 0;
      lineRows = 0;
    }
    lineColumns += columnSpan;
    if (rowSpan > lineRows) lineRows = rowSpan;
    if (lineColumns >= columns) {
      committedRows += lineRows;
      lineColumns = 0;
      lineRows = 0;
    }
  }

  final total = committedRows + lineRows;
  return total < 1 ? 1 : total;
}

// ── The grid ────────────────────────────────────────────────────────────────

/// Kotlin `RecordingDashboardGrid`. Placement comes from the ported
/// [ActivityRecordingDashboardLayout.placements], so this only paints.
class RecordingDashboardGrid extends StatelessWidget {
  const RecordingDashboardGrid({
    super.key,
    required this.layout,
    required this.stats,
    required this.isEditingDashboard,
    required this.onUpdateLayout,
    this.fillHeight = false,
  });

  final ActivityRecordingDashboardLayout layout;
  final Map<ActivityRecordingDashboardField, RecordingDashboardStat> stats;
  final bool isEditingDashboard;
  final ValueChanged<ActivityRecordingDashboardLayout> onUpdateLayout;
  final bool fillHeight;

  static const double _spacing = 10;
  static const double _cellHeight = 78;

  @override
  Widget build(BuildContext context) {
    final normalizedLayout = layout.normalized();
    final placements = [
      for (final placement in normalizedLayout.placements())
        if (stats.containsKey(placement.item.field)) placement,
    ];
    if (placements.isEmpty) return const SizedBox.shrink();

    final columns = normalizedLayout.template.columns;
    final rows = recordingDashboardLazyGridRows(
      items: [for (final placement in placements) placement.item],
      columns: columns,
    );
    final measuredRows =
        fillHeight && normalizedLayout.template.rows > rows
            ? normalizedLayout.template.rows
            : rows;

    return LayoutBuilder(
      builder: (context, constraints) {
        final cellHeight = fillHeight && constraints.hasBoundedHeight
            ? ((constraints.maxHeight - _spacing * (measuredRows - 1)) /
                    measuredRows)
                .clamp(0.0, double.infinity)
            : _cellHeight;
        final cellWidth =
            (constraints.maxWidth - _spacing * (columns - 1)) / columns;
        final gridHeight =
            cellHeight * measuredRows + _spacing * (measuredRows - 1);

        return SizedBox(
          height: gridHeight,
          child: Stack(
            children: [
              for (final placement in placements)
                Positioned(
                  left: placement.column * (cellWidth + _spacing),
                  top: placement.row * (cellHeight + _spacing),
                  width: placement.columnSpan * cellWidth +
                      _spacing * (placement.columnSpan - 1),
                  height: placement.rowSpan * cellHeight +
                      _spacing * (placement.rowSpan - 1),
                  child: RecordingDashboardTile(
                    field: placement.item.field,
                    fieldIndex: normalizedLayout.fields
                        .indexOf(placement.item.field),
                    stat: stats[placement.item.field]!,
                    size: placement.item.size,
                    isEditingDashboard: isEditingDashboard,
                    cellSize: Size(
                      placement.columnSpan * cellWidth +
                          _spacing * (placement.columnSpan - 1),
                      placement.rowSpan * cellHeight +
                          _spacing * (placement.rowSpan - 1),
                    ),
                    onReorder: (from, to) =>
                        onUpdateLayout(normalizedLayout.withMovedFieldIndex(from, to)),
                    onRemove: () => onUpdateLayout(
                        normalizedLayout.withRemovedField(placement.item.field)),
                    onResize: (size) => onUpdateLayout(
                        normalizedLayout.withFieldSize(placement.item.field, size)),
                  ),
                ),
            ],
          ),
        );
      },
    );
  }

}

/// Kotlin `RecordingDashboardTile` + `RecordingDashboardTileContent`.
///
/// In edit mode it is a [ReorderableEditTile] carrying the shared
/// [RemoveWidgetButton] — the same long-press-drag-and-drop the dashboard
/// summary and add-entry hub use — plus the recording-only resize grip.
class RecordingDashboardTile extends StatelessWidget {
  const RecordingDashboardTile({
    super.key,
    required this.field,
    required this.fieldIndex,
    required this.stat,
    required this.size,
    required this.isEditingDashboard,
    required this.cellSize,
    required this.onReorder,
    required this.onRemove,
    required this.onResize,
  });

  final ActivityRecordingDashboardField field;

  /// Position in the layout's field order — what a drop reorders against.
  final int fieldIndex;
  final RecordingDashboardStat stat;
  final ActivityRecordingDashboardItemSize size;
  final bool isEditingDashboard;

  /// The laid-out size of this cell, so the drag feedback matches it.
  final Size cellSize;
  final void Function(int from, int to) onReorder;
  final VoidCallback onRemove;
  final ValueChanged<ActivityRecordingDashboardItemSize> onResize;

  @override
  Widget build(BuildContext context) {
    final content = _TileContent(stat: stat, size: size);
    if (!isEditingDashboard) return content;

    final l10n = AppLocalizations.of(context);
    final card = Stack(
      fit: StackFit.expand,
      children: [
        content,
        Positioned(top: 0, right: 0, child: RemoveWidgetButton(onPressed: onRemove)),
        Positioned(
          bottom: 0,
          right: 0,
          child: _ResizeHandle(size: size, onResize: onResize),
        ),
      ],
    );

    // Kotlin exposes resizing as CustomAccessibilityActions on the tile, since
    // the grip is a drag gesture a screen reader cannot perform.
    return Semantics(
      label: stat.label,
      customSemanticsActions: {
        if (size.canShrink())
          CustomSemanticsAction(
                  label: l10n.cdDecreaseRecordingDashboardWidgetSize):
              () => onResize(size.previousSize()),
        if (size.canGrow())
          CustomSemanticsAction(
                  label: l10n.cdIncreaseRecordingDashboardWidgetSize):
              () => onResize(size.nextSize()),
      },
      child: ReorderableEditTile(
        index: fieldIndex,
        onReorder: onReorder,
        feedbackSize: cellSize,
        feedbackBorderRadius: const BorderRadius.all(Radius.circular(12)),
        child: card,
      ),
    );
  }
}

class _TileContent extends StatelessWidget {
  const _TileContent({required this.stat, required this.size});

  final RecordingDashboardStat stat;
  final ActivityRecordingDashboardItemSize size;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final emphasized = size.hasRoomyMetricText();
    final compact = size.hasCompactMetricText();

    final valueStyle = emphasized
        ? theme.textTheme.displayMedium
        : compact
            ? theme.textTheme.headlineSmall
            : theme.textTheme.headlineMedium;
    final unitStyle = compact
        ? theme.textTheme.labelMedium
        : size.rowSpan == 1
            ? theme.textTheme.labelLarge
            : theme.textTheme.titleSmall;
    final labelStyle = compact || size.rowSpan == 1
        ? theme.textTheme.labelMedium
        : theme.textTheme.labelLarge;

    return OpenVitalsSurface(
      contentPadding:
          EdgeInsets.all(compact || size.rowSpan == 1 ? 8 : 10),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(
                    stat.value.value,
                    maxLines: 1,
                    style: valueStyle?.copyWith(
                      color: emphasized
                          ? activityRecordingAccentColor()
                          : theme.colorScheme.onSurface,
                    ),
                  ),
                ),
              ),
              if (stat.value.unit.trim().isNotEmpty)
                Padding(
                  padding: EdgeInsets.only(left: 3, bottom: compact ? 2 : 4),
                  child: Text(
                    stat.value.unit,
                    maxLines: 1,
                    style: unitStyle?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant),
                  ),
                ),
            ],
          ),
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Text(
              stat.label.toUpperCase(),
              maxLines: 1,
              style: labelStyle?.copyWith(color: activityRecordingAccentColor()),
            ),
          ),
        ],
      ),
    );
  }
}

/// Kotlin `RecordingDashboardResizeHandle`: a corner grip you drag to resize.
/// Reaching it without a drag is handled by the tile's custom semantics actions.
class _ResizeHandle extends StatefulWidget {
  const _ResizeHandle({required this.size, required this.onResize});

  final ActivityRecordingDashboardItemSize size;
  final ValueChanged<ActivityRecordingDashboardItemSize> onResize;

  @override
  State<_ResizeHandle> createState() => _ResizeHandleState();
}

class _ResizeHandleState extends State<_ResizeHandle> {
  ActivityRecordingDashboardItemSize? _dragStartSize;
  ActivityRecordingDashboardItemSize? _appliedSize;
  Offset _dragOffset = Offset.zero;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onPanStart: (_) {
        _dragStartSize = widget.size;
        _appliedSize = widget.size;
        _dragOffset = Offset.zero;
      },
      onPanUpdate: (details) {
        final start = _dragStartSize;
        if (start == null) return;
        _dragOffset += details.delta;
        final target =
            start.sizeForResizeDrag(_dragOffset, kRecordingDashboardResizeStep);
        if (target != _appliedSize) {
          _appliedSize = target;
          widget.onResize(target);
        }
      },
      onPanEnd: (_) => _dragStartSize = null,
      child: SizedBox(
        width: 38,
        height: 38,
        child: CustomPaint(
          painter:
              _ResizeHandlePainter(color: Theme.of(context).colorScheme.primary),
        ),
      ),
    );
  }
}

class _ResizeHandlePainter extends CustomPainter {
  const _ResizeHandlePainter({required this.color});

  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..strokeWidth = 2
      ..strokeCap = StrokeCap.round;
    for (final inset in const [10.0, 17.0, 24.0]) {
      canvas.drawLine(
        Offset(size.width - inset, size.height),
        Offset(size.width, size.height - inset),
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(_ResizeHandlePainter oldDelegate) =>
      oldDelegate.color != color;
}

/// Kotlin `RecordingDashboardEditor`: the tray of fields not on the grid.
///
/// Reuses the dashboard/add-entry [HiddenWidgetsSection] rather than Kotlin's
/// chip row, so "add a widget back" looks the same everywhere. A field the grid
/// has no room for is not offered — [withAddedField] would drop it again.
class RecordingDashboardEditor extends StatelessWidget {
  const RecordingDashboardEditor({
    super.key,
    required this.layout,
    required this.availableFields,
    required this.onUpdateLayout,
  });

  final ActivityRecordingDashboardLayout layout;
  final List<ActivityRecordingDashboardField> availableFields;
  final ValueChanged<ActivityRecordingDashboardLayout> onUpdateLayout;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final addable = <String, ActivityRecordingDashboardField>{
      for (final field in availableFields)
        if (!layout.fields.contains(field) &&
            layout.withAddedField(field).fields.contains(field))
          recordingDashboardFieldLabel(field, l10n): field,
    };

    // Kotlin renders nothing when there is nothing to add, and the shared
    // section's empty copy ("already on the summary") is about the dashboard.
    if (addable.isEmpty) return const SizedBox.shrink();

    return HiddenWidgetsSection(
      heading: l10n.activityEntryRecordingDashboardAddField,
      padding: EdgeInsets.zero,
      titles: addable.keys.toList(),
      onAdd: (title) {
        final field = addable[title];
        if (field != null) onUpdateLayout(layout.withAddedField(field));
      },
    );
  }
}

/// Kotlin `RecordingStatsTab`.
class RecordingStatsTab extends StatelessWidget {
  const RecordingStatsTab({
    super.key,
    required this.state,
    required this.totalTime,
    required this.movingTime,
    required this.now,
    required this.unitFormatter,
    required this.isEditingDashboard,
    required this.onUpdateDashboardLayout,
  });

  final ActivityRecordingState state;
  final Duration totalTime;
  final Duration movingTime;
  final DateTime now;
  final UnitFormatter unitFormatter;
  final bool isEditingDashboard;
  final ValueChanged<ActivityRecordingDashboardLayout> onUpdateDashboardLayout;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final availableFields = availableRecordingDashboardFields(state);
    final layout = state.dashboardLayout.withAvailableFields(availableFields);
    final stats = recordingDashboardStats(
      state: state,
      totalTime: totalTime,
      movingTime: movingTime,
      now: now,
      unitFormatter: unitFormatter,
      l10n: l10n,
    );
    final accuracy = state.lastAccuracyMeters;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 16,
      children: [
        RecordingDashboardGrid(
          layout: layout,
          stats: stats,
          isEditingDashboard: isEditingDashboard,
          onUpdateLayout: onUpdateDashboardLayout,
        ),
        if (state.bleDeviceStatuses.isNotEmpty) ...[
          ActivityRecordingSensorStatusCard(
              deviceStatuses: state.bleDeviceStatuses),
          if (state.bleHeartRateNoSignal && state.currentHeartRateBpm == null)
            Text(
              l10n.activityRecordingSensorsGarminBroadcastHint,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
        ],
        if (accuracy != null)
          Text(
            l10n.activityEntryRecordingAccuracy(
                unitFormatter.elevation(accuracy).text),
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
      ],
    );
  }
}
