import 'package:flutter/material.dart';

import '../../../../core/presentation/display_value.dart';
import '../../../../core/presentation/unit_formatter.dart';
import '../../../../domain/model/activity_models.dart';
import '../../../../domain/preferences/unit_system.dart';
import '../../../../l10n/app_localizations.dart';
import 'activity_recording_dashboard.dart';
import 'activity_recording_splits.dart';

/// Port of the Kotlin `ActivityRecordingSplitsUi.kt`: the split tables, their
/// selectors, and the marker list.

/// Kotlin `MetersPerMile`.
const double kMetersPerMile = 1609.344;

/// Kotlin `TimeSplitMinuteOptions` (declared in `ActivityRecordingGpsTabs.kt`).
const List<int> kTimeSplitMinuteOptions = [1, 5, 10];

/// Kotlin `defaultDistanceSplitMeters`.
double defaultDistanceSplitMeters(UnitSystem unitSystem) =>
    unitSystem == UnitSystem.imperial ? kMetersPerMile : 1000.0;

/// Kotlin `distanceSplitOptions`.
List<double> distanceSplitOptions(UnitSystem unitSystem) =>
    unitSystem == UnitSystem.imperial
        ? [0.5 * kMetersPerMile, kMetersPerMile, 5.0 * kMetersPerMile]
        : [500.0, 1000.0, 5000.0];

/// Kotlin `splitDistanceDecimals`: whole kilometres read "5 km", not "5.0 km".
int splitDistanceDecimals(double value) =>
    value < 1.0 || value % 1.0 != 0.0 ? 1 : 0;

/// Kotlin `distanceSplitOptionLabel`.
String distanceSplitOptionLabel(
  double meters,
  UnitSystem unitSystem,
  UnitFormatter unitFormatter,
) {
  final imperial = unitSystem == UnitSystem.imperial;
  final value = meters / (imperial ? kMetersPerMile : 1000.0);
  final unit = imperial ? 'mi' : 'km';
  return '${unitFormatter.decimal(value, splitDistanceDecimals(value))} $unit';
}

/// Kotlin `distanceRangeLabel`.
String distanceRangeLabel(
  double startMeters,
  double endMeters,
  UnitSystem unitSystem,
  UnitFormatter unitFormatter,
) {
  final imperial = unitSystem == UnitSystem.imperial;
  final divisor = imperial ? kMetersPerMile : 1000.0;
  final unit = imperial ? 'mi' : 'km';
  final start = startMeters / divisor;
  final end = endMeters / divisor;
  return '${unitFormatter.decimal(start, splitDistanceDecimals(start))}-'
      '${unitFormatter.decimal(end, splitDistanceDecimals(end))} $unit';
}

/// Kotlin `ActivityRecordingMarker.locationSummary`.
String markerLocationSummary(
  ActivityRecordingMarker marker,
  UnitFormatter unitFormatter,
) {
  final coordinate = '${marker.latitude.toStringAsFixed(5)}, '
      '${marker.longitude.toStringAsFixed(5)}';
  final altitude = marker.altitudeMeters;
  if (altitude == null) return coordinate;
  return '$coordinate • ${unitFormatter.elevation(altitude).text}';
}

/// Kotlin `RecordingSplitsTab`.
class RecordingSplitsTab extends StatelessWidget {
  const RecordingSplitsTab({
    super.key,
    required this.splits,
    required this.emptyMessage,
    required this.unitFormatter,
    required this.label,
    this.controls,
  });

  final List<ActivityRecordingSplit> splits;
  final String emptyMessage;
  final UnitFormatter unitFormatter;
  final String Function(ActivityRecordingSplit split) label;
  final Widget? controls;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 12,
      children: [
        ?controls,
        if (splits.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 24),
            child: Text(
              emptyMessage,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          )
        else
          for (var index = 0; index < splits.length; index++) ...[
            if (index > 0) Divider(color: theme.colorScheme.outlineVariant),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 12),
              child: RecordingSplitRow(
                split: splits[index],
                label: label(splits[index]),
                unitFormatter: unitFormatter,
              ),
            ),
          ],
      ],
    );
  }
}

/// Kotlin `RecordingSplitRow`.
class RecordingSplitRow extends StatelessWidget {
  const RecordingSplitRow({
    super.key,
    required this.split,
    required this.label,
    required this.unitFormatter,
  });

  final ActivityRecordingSplit split;
  final String label;
  final UnitFormatter unitFormatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 8,
      children: [
        Row(
          children: [
            Expanded(child: Text(label, style: theme.textTheme.titleSmall)),
            const SizedBox(width: 12),
            CompactSplitMetric(
              label: l10n.activityEntryRecordingDistance,
              value: unitFormatter.distance(split.distanceMeters),
            ),
            const SizedBox(width: 12),
            CompactSplitMetric(
              label: l10n.activityEntryRecordingSplitElapsed,
              value: DisplayValue(
                formatRecordingElapsed(Duration(milliseconds: split.elapsedMillis)),
                '',
              ),
            ),
          ],
        ),
        Row(
          children: [
            Expanded(
              child: CompactSplitMetric(
                label: l10n.activityEntryRecordingSplitAvg,
                value: unitFormatter.speed(split.averageSpeedMetersPerSecond),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: CompactSplitMetric(
                label: l10n.activityEntryRecordingSplitMax,
                value: unitFormatter.speed(split.maxSpeedMetersPerSecond),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: CompactSplitMetric(
                label: l10n.activityEntryRecordingElevationGain,
                value: unitFormatter.elevation(split.climbMeters),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

/// Kotlin `CompactSplitMetric`.
class CompactSplitMetric extends StatelessWidget {
  const CompactSplitMetric({
    super.key,
    required this.label,
    required this.value,
  });

  final String label;
  final DisplayValue value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          value.text,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.bodyMedium,
        ),
        Text(
          label.toUpperCase(),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.labelSmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ],
    );
  }
}

/// Kotlin `SplitSelector`, a titled segmented row.
class SplitSelector<T> extends StatelessWidget {
  const SplitSelector({
    super.key,
    required this.title,
    required this.options,
    required this.selected,
    required this.label,
    required this.onSelect,
  });

  final String title;
  final List<T> options;
  final T selected;
  final String Function(T option) label;
  final ValueChanged<T> onSelect;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      spacing: 8,
      children: [
        Text(
          title,
          style: theme.textTheme.labelLarge
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
        SegmentedButton<T>(
          segments: [
            for (final option in options)
              ButtonSegment(value: option, label: Text(label(option))),
          ],
          selected: {selected},
          showSelectedIcon: false,
          onSelectionChanged: (selection) => onSelect(selection.first),
        ),
      ],
    );
  }
}

/// Kotlin `TimeSplitSelector`.
class TimeSplitSelector extends StatelessWidget {
  const TimeSplitSelector({
    super.key,
    required this.selectedMinutes,
    required this.onSelect,
  });

  final int selectedMinutes;
  final ValueChanged<int> onSelect;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return SplitSelector<int>(
      title: l10n.activityEntryRecordingTimeSplit,
      options: kTimeSplitMinuteOptions,
      selected: selectedMinutes,
      label: l10n.activityEntryRecordingSplitMinutes,
      onSelect: onSelect,
    );
  }
}

/// Kotlin `DistanceSplitSelector`.
class DistanceSplitSelector extends StatelessWidget {
  const DistanceSplitSelector({
    super.key,
    required this.selectedMeters,
    required this.unitSystem,
    required this.unitFormatter,
    required this.onSelect,
  });

  final double selectedMeters;
  final UnitSystem unitSystem;
  final UnitFormatter unitFormatter;
  final ValueChanged<double> onSelect;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return SplitSelector<double>(
      title: l10n.activityEntryRecordingDistanceSplit,
      options: distanceSplitOptions(unitSystem),
      selected: selectedMeters,
      label: (meters) =>
          distanceSplitOptionLabel(meters, unitSystem, unitFormatter),
      onSelect: onSelect,
    );
  }
}

/// Kotlin `RecordingMarkersList`: each marker is editable in place.
class RecordingMarkersList extends StatelessWidget {
  const RecordingMarkersList({
    super.key,
    required this.markers,
    required this.unitFormatter,
    required this.onUpdateMarker,
    required this.onDeleteMarker,
  });

  final List<ActivityRecordingMarker> markers;
  final UnitFormatter unitFormatter;
  final ValueChanged<ActivityRecordingMarker> onUpdateMarker;
  final ValueChanged<String> onDeleteMarker;

  @override
  Widget build(BuildContext context) {
    if (markers.isEmpty) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final sorted = [...markers]..sort((a, b) => a.time.compareTo(b.time));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 8,
      children: [
        Text(l10n.activityEntryRecordingMarkersTitle,
            style: theme.textTheme.titleSmall),
        for (var index = 0; index < sorted.length; index++) ...[
          if (index > 0) Divider(color: theme.colorScheme.outlineVariant),
          _MarkerRow(
            // Keyed by id so editing one marker never rebinds another's fields.
            key: ValueKey(sorted[index].id),
            marker: sorted[index],
            unitFormatter: unitFormatter,
            onUpdateMarker: onUpdateMarker,
            onDeleteMarker: onDeleteMarker,
          ),
        ],
      ],
    );
  }
}

class _MarkerRow extends StatefulWidget {
  const _MarkerRow({
    super.key,
    required this.marker,
    required this.unitFormatter,
    required this.onUpdateMarker,
    required this.onDeleteMarker,
  });

  final ActivityRecordingMarker marker;
  final UnitFormatter unitFormatter;
  final ValueChanged<ActivityRecordingMarker> onUpdateMarker;
  final ValueChanged<String> onDeleteMarker;

  @override
  State<_MarkerRow> createState() => _MarkerRowState();
}

class _MarkerRowState extends State<_MarkerRow> {
  late final TextEditingController _name =
      TextEditingController(text: widget.marker.name);
  late final TextEditingController _note =
      TextEditingController(text: widget.marker.note);

  @override
  void dispose() {
    _name.dispose();
    _note.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 8,
      children: [
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _name,
                maxLines: 1,
                decoration: InputDecoration(
                  labelText: l10n.activityEntryRecordingMarkerName,
                  border: const OutlineInputBorder(),
                ),
                onChanged: (name) =>
                    widget.onUpdateMarker(widget.marker.copyWith(name: name)),
              ),
            ),
            IconButton(
              onPressed: () => widget.onDeleteMarker(widget.marker.id),
              tooltip: l10n.actionDelete,
              icon: const Icon(Icons.delete_outline),
            ),
          ],
        ),
        TextField(
          controller: _note,
          decoration: InputDecoration(
            labelText: l10n.activityEntryRecordingMarkerNote,
            border: const OutlineInputBorder(),
          ),
          onChanged: (note) =>
              widget.onUpdateMarker(widget.marker.copyWith(note: note)),
        ),
        Text(
          markerLocationSummary(widget.marker, widget.unitFormatter),
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ],
    );
  }
}
