import 'package:flutter/material.dart';

import '../../../../core/presentation/unit_formatter.dart';
import '../../../../domain/model/activity_models.dart';
import '../../../../domain/preferences/activity_recording_dashboard_layout.dart';
import '../../../../domain/preferences/unit_system.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/widget_edit_controls.dart';
import '../../../activity/maps/route_map_view.dart';
import 'activity_recording.dart';
import 'activity_recording_dashboard.dart';
import 'activity_recording_sensor_ui.dart';
import 'activity_recording_splits.dart';
import 'activity_recording_splits_ui.dart';

/// Port of the Kotlin `ActivityRecordingGpsTabs.kt`.

/// Kotlin `DefaultTimeSplitMinutes`.
const int kDefaultTimeSplitMinutes = 5;

/// Kotlin `ActivityRecordingTab.labelRes`.
String activityRecordingTabLabel(ActivityRecordingTab tab, AppLocalizations l10n) =>
    switch (tab) {
      ActivityRecordingTab.map => l10n.activityEntryRecordingTabMap,
      ActivityRecordingTab.stats => l10n.activityEntryRecordingTabStats,
      ActivityRecordingTab.intervals => l10n.activityEntryRecordingTabIntervals,
      ActivityRecordingTab.byTime => l10n.activityEntryRecordingTabByTime,
      ActivityRecordingTab.byDistance =>
        l10n.activityEntryRecordingTabByDistance,
    };

/// Kotlin `GpsRecordingTabs`: Map / Stats / Intervals / By time / By distance.
///
/// While the dashboard is being edited, Kotlin forces the Stats tab and swaps
/// the tab row for the field editor — there is nothing to configure elsewhere.
class GpsRecordingTabs extends StatefulWidget {
  const GpsRecordingTabs({
    super.key,
    required this.state,
    required this.preStartPoint,
    required this.totalTime,
    required this.movingTime,
    required this.now,
    required this.unitFormatter,
    required this.isEditingDashboard,
    required this.onUpdateDashboardLayout,
  });

  final ActivityRecordingState state;
  final ExerciseRoutePoint? preStartPoint;
  final Duration totalTime;
  final Duration movingTime;
  final DateTime now;
  final UnitFormatter unitFormatter;
  final bool isEditingDashboard;
  final ValueChanged<ActivityRecordingDashboardLayout> onUpdateDashboardLayout;

  @override
  State<GpsRecordingTabs> createState() => _GpsRecordingTabsState();
}

class _GpsRecordingTabsState extends State<GpsRecordingTabs> {
  ActivityRecordingTab _selectedTab = ActivityRecordingTab.stats;
  int _timeSplitMinutes = kDefaultTimeSplitMinutes;
  double? _distanceSplitMeters;
  UnitSystem? _distanceSplitUnitSystem;

  /// Kotlin re-keys `distanceSplitMeters` on the unit system, so switching to
  /// imperial reselects "1 mi" rather than leaving "1000 m" selected — a value
  /// that is not one of the imperial options and would break the selector.
  double _distanceSplit(UnitSystem unitSystem) {
    if (_distanceSplitUnitSystem != unitSystem) {
      _distanceSplitUnitSystem = unitSystem;
      _distanceSplitMeters = defaultDistanceSplitMeters(unitSystem);
    }
    return _distanceSplitMeters!;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final state = widget.state;
    final unitSystem = widget.unitFormatter.unitSystem();
    final distanceSplitMeters = _distanceSplit(unitSystem);

    if (widget.isEditingDashboard) {
      final availableFields = availableRecordingDashboardFields(state);
      return SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          spacing: 16,
          children: [
            const EditModeHint(),
            RecordingDashboardEditor(
              layout: state.dashboardLayout.withAvailableFields(availableFields),
              availableFields: availableFields,
              onUpdateLayout: widget.onUpdateDashboardLayout,
            ),
            _statsTab(isEditing: true),
          ],
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 16,
      children: [
        ActivityRecordingTabRow(
          selectedTab: _selectedTab,
          onSelect: (tab) => setState(() => _selectedTab = tab),
        ),
        switch (_selectedTab) {
          ActivityRecordingTab.map => Expanded(
              child: RouteMapView(
                points: state.points,
                routeBreakIndexes: state.routeBreakIndexes,
                currentPoint: state.latestUiPoint ?? widget.preStartPoint,
                showRecenterControl: true,
              ),
            ),
          ActivityRecordingTab.stats => _statsTab(isEditing: false),
          ActivityRecordingTab.intervals => RecordingSplitsTab(
              splits: state.manualLaps.isNotEmpty
                  ? activityRecordingLapSplits(
                      laps: state.manualLaps,
                      points: state.points,
                      routeBreakIndexes: state.routeBreakIndexes,
                      recordingStartTime: state.startTime,
                      activeEndTime: widget.now,
                    )
                  : activityRecordingIntervalSplits(
                      state.points, state.routeBreakIndexes),
              emptyMessage: l10n.activityEntryRecordingNoIntervals,
              unitFormatter: widget.unitFormatter,
              label: (split) =>
                  l10n.activityEntryRecordingSplitInterval(split.index),
            ),
          ActivityRecordingTab.byTime => RecordingSplitsTab(
              splits: activityRecordingTimeSplits(
                points: state.points,
                routeBreakIndexes: state.routeBreakIndexes,
                splitMillis: _timeSplitMinutes * 60000,
              ),
              emptyMessage: l10n.activityEntryRecordingNoTimeSplits,
              unitFormatter: widget.unitFormatter,
              label: (split) => l10n.activityEntryRecordingSplitTimeRange(
                (split.index - 1) * _timeSplitMinutes,
                split.index * _timeSplitMinutes,
              ),
              controls: TimeSplitSelector(
                selectedMinutes: _timeSplitMinutes,
                onSelect: (minutes) => setState(() => _timeSplitMinutes = minutes),
              ),
            ),
          ActivityRecordingTab.byDistance => RecordingSplitsTab(
              splits: activityRecordingDistanceSplits(
                points: state.points,
                routeBreakIndexes: state.routeBreakIndexes,
                splitMeters: distanceSplitMeters,
              ),
              emptyMessage: l10n.activityEntryRecordingNoDistanceSplits,
              unitFormatter: widget.unitFormatter,
              label: (split) {
                final endDistance = split.endDistanceMeters;
                // The final, partial split has no end distance yet.
                if (endDistance == null) {
                  return l10n.activityEntryRecordingSplitInterval(split.index);
                }
                return distanceRangeLabel(
                  split.startDistanceMeters,
                  endDistance,
                  unitSystem,
                  widget.unitFormatter,
                );
              },
              controls: DistanceSplitSelector(
                selectedMeters: distanceSplitMeters,
                unitSystem: unitSystem,
                unitFormatter: widget.unitFormatter,
                onSelect: (meters) =>
                    setState(() => _distanceSplitMeters = meters),
              ),
            ),
        },
      ],
    );
  }

  Widget _statsTab({required bool isEditing}) => RecordingStatsTab(
        state: widget.state,
        totalTime: widget.totalTime,
        movingTime: widget.movingTime,
        now: widget.now,
        unitFormatter: widget.unitFormatter,
        isEditingDashboard: isEditing,
        onUpdateDashboardLayout: widget.onUpdateDashboardLayout,
      );
}

/// Kotlin `ActivityRecordingTabRow`, a scrollable primary tab row.
class ActivityRecordingTabRow extends StatelessWidget {
  const ActivityRecordingTabRow({
    super.key,
    required this.selectedTab,
    required this.onSelect,
  });

  final ActivityRecordingTab selectedTab;
  final ValueChanged<ActivityRecordingTab> onSelect;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final scheme = Theme.of(context).colorScheme;
    const tabs = ActivityRecordingTab.values;

    return SizedBox(
      height: 48,
      child: DefaultTabController(
        length: tabs.length,
        initialIndex: tabs.indexOf(selectedTab),
        child: TabBar(
          isScrollable: true,
          tabAlignment: TabAlignment.start,
          labelColor: activityRecordingAccentColor(),
          unselectedLabelColor: scheme.onSurfaceVariant,
          indicatorColor: activityRecordingAccentColor(),
          dividerColor: Colors.transparent,
          onTap: (index) => onSelect(tabs[index]),
          tabs: [
            for (final tab in tabs) Tab(text: activityRecordingTabLabel(tab, l10n)),
          ],
        ),
      ),
    );
  }
}
