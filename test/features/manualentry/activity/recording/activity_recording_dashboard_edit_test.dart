import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/presentation/display_value.dart';
import 'package:openvitals/domain/preferences/activity_recording_dashboard_layout.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_dashboard.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/widget_edit_controls.dart';

/// The recording dashboard's edit mode is built from the same pieces as the
/// dashboard summary and the add-entry hub: [ReorderableEditTile] for the
/// long-press drag, [RemoveWidgetButton] for the ✕, [HiddenWidgetsSection] for
/// the add tray, and [reorderOntoDropTarget] for the drop-on-target rule.
void main() {
  const heartRate = ActivityRecordingDashboardField.heartRate;
  const cadence = ActivityRecordingDashboardField.cadence;
  const speed = ActivityRecordingDashboardField.speed;
  const distance = ActivityRecordingDashboardField.distance;
  const duration = ActivityRecordingDashboardField.duration;
  const movingTime = ActivityRecordingDashboardField.movingTime;

  /// The default five fields, each 1x1 so every tile is its own drag target.
  final layout = ActivityRecordingDashboardLayout(
    fields: const [heartRate, cadence, speed, distance, duration],
    sizes: {
      for (final field in const [heartRate, cadence, speed, distance, duration])
        field: ActivityRecordingDashboardItemSize(columnSpan: 1, rowSpan: 1),
    },
  );

  Map<ActivityRecordingDashboardField, RecordingDashboardStat> statsFor(
    List<ActivityRecordingDashboardField> fields,
  ) =>
      {
        for (final field in fields)
          field: RecordingDashboardStat(
            value: DisplayValue('${field.index}', ''),
            label: field.name,
          ),
      };

  Future<ActivityRecordingDashboardLayout?> pumpGrid(
    WidgetTester tester, {
    required bool editing,
    ActivityRecordingDashboardLayout? initial,
  }) async {
    tester.view.physicalSize = const Size(1200, 1600);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    ActivityRecordingDashboardLayout? updated;
    final current = initial ?? layout;
    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: RecordingDashboardGrid(
            layout: current,
            stats: statsFor(current.fields),
            isEditingDashboard: editing,
            onUpdateLayout: (value) => updated = value,
          ),
        ),
      ),
    );
    await tester.pump();
    return updated;
  }

  /// The centre of the tile whose stat label is [label].
  Offset tileCenter(WidgetTester tester, ActivityRecordingDashboardField field) =>
      tester.getCenter(find.text(field.name.toUpperCase()));

  testWidgets('outside edit mode the tiles are plain, undraggable and unremovable',
      (tester) async {
    await pumpGrid(tester, editing: false);

    expect(find.byType(ReorderableEditTile), findsNothing);
    expect(find.byType(RemoveWidgetButton), findsNothing);
    expect(find.byType(LongPressDraggable<int>), findsNothing);
  });

  testWidgets('edit mode wraps every tile in the shared reorderable tile',
      (tester) async {
    await pumpGrid(tester, editing: true);

    expect(find.byType(ReorderableEditTile), findsNWidgets(5));
    expect(find.byType(RemoveWidgetButton), findsNWidgets(5));
  });

  testWidgets('the shared ✕ removes that field from the layout', (tester) async {
    ActivityRecordingDashboardLayout? updated;
    tester.view.physicalSize = const Size(1200, 1600);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: RecordingDashboardGrid(
            layout: layout,
            stats: statsFor(layout.fields),
            isEditingDashboard: true,
            onUpdateLayout: (value) => updated = value,
          ),
        ),
      ),
    );
    await tester.pump();

    // The ✕ belonging to `speed` is the one inside speed's own tile.
    final speedRemove = find.descendant(
      of: find.ancestor(
        of: find.text(speed.name.toUpperCase()),
        matching: find.byType(ReorderableEditTile),
      ),
      matching: find.byType(RemoveWidgetButton),
    );
    expect(speedRemove, findsOneWidget);

    await tester.tap(speedRemove);
    await tester.pump();

    expect(updated, isNotNull);
    expect(updated!.fields, isNot(contains(speed)));
    expect(updated!.fields, containsAll([heartRate, cadence, distance, duration]));
  });

  testWidgets('dragging a tile onto another lands it on the target',
      (tester) async {
    ActivityRecordingDashboardLayout? updated;
    tester.view.physicalSize = const Size(1200, 1600);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: RecordingDashboardGrid(
            layout: layout,
            stats: statsFor(layout.fields),
            isEditingDashboard: true,
            onUpdateLayout: (value) => updated = value,
          ),
        ),
      ),
    );
    await tester.pump();

    final from = tileCenter(tester, heartRate);
    final to = tileCenter(tester, distance);

    final gesture = await tester.startGesture(from);
    // LongPressDraggable only picks up after the long-press timeout.
    await tester.pump(kLongPressTimeout + const Duration(milliseconds: 20));
    await gesture.moveTo(to);
    await tester.pump();
    await gesture.up();
    await tester.pump();

    // Drop-on-target, not insertion-gap: heartRate takes distance's slot (3)
    // and everything between shifts down one.
    expect(updated, isNotNull);
    expect(updated!.fields, [cadence, speed, distance, heartRate, duration]);
  });

  testWidgets('the add tray offers the fields not on the grid, and adds one',
      (tester) async {
    ActivityRecordingDashboardLayout? updated;
    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: SingleChildScrollView(
            child: RecordingDashboardEditor(
              layout: layout,
              availableFields: const [
                heartRate,
                cadence,
                speed,
                distance,
                duration,
                movingTime,
              ],
              onUpdateLayout: (value) => updated = value,
            ),
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.byType(HiddenWidgetsSection), findsOneWidget);
    // Kotlin's heading for this tray, not the dashboard's "Add widgets".
    expect(find.text('Add widget'), findsOneWidget);
    // Already-placed fields are not offered.
    expect(find.text('Heart rate'), findsNothing);
    expect(find.text('Moving time'), findsOneWidget);

    await tester.tap(find.text('Moving time'));
    await tester.pump();

    expect(updated, isNotNull);
    expect(updated!.fields, contains(movingTime));
  });

  testWidgets('a full grid renders no add tray at all', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: RecordingDashboardEditor(
            layout: layout,
            availableFields: const [heartRate, cadence, speed, distance, duration],
            onUpdateLayout: (_) {},
          ),
        ),
      ),
    );
    await tester.pump();

    // Kotlin shows nothing here; the shared section's empty copy talks about
    // the dashboard summary, which this is not.
    expect(find.byType(HiddenWidgetsSection), findsNothing);
    expect(find.text('Add widget'), findsNothing);
  });
}
