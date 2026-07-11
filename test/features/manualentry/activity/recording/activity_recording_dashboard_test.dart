import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/preferences/activity_recording_dashboard_layout.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_dashboard.dart';

/// Unit coverage for the pure half of the Kotlin `ActivityRecordingDashboard.kt`.
void main() {
  const heartRate = ActivityRecordingDashboardField.heartRate;
  const cadence = ActivityRecordingDashboardField.cadence;
  const speed = ActivityRecordingDashboardField.speed;
  const distance = ActivityRecordingDashboardField.distance;
  const duration = ActivityRecordingDashboardField.duration;
  const power = ActivityRecordingDashboardField.power;
  const steps = ActivityRecordingDashboardField.steps;

  ActivityRecordingDashboardItemSize size(int columns, int rows) =>
      ActivityRecordingDashboardItemSize(columnSpan: columns, rowSpan: rows);

  ActivityRecordingDashboardLayout layoutOf(
    List<ActivityRecordingDashboardField> fields,
  ) =>
      ActivityRecordingDashboardLayout(fields: fields);

  group('formatRecordingElapsed', () {
    test('drops the hour segment under an hour', () {
      expect(formatRecordingElapsed(Duration.zero), '0:00');
      expect(formatRecordingElapsed(const Duration(seconds: 9)), '0:09');
      expect(
        formatRecordingElapsed(const Duration(minutes: 12, seconds: 5)),
        '12:05',
      );
    });

    test('shows zero-padded minutes once there are hours', () {
      expect(
        formatRecordingElapsed(const Duration(hours: 1, minutes: 2, seconds: 3)),
        '1:02:03',
      );
      expect(
        formatRecordingElapsed(const Duration(hours: 10, minutes: 0, seconds: 0)),
        '10:00:00',
      );
    });

    test('floors a negative duration at zero', () {
      expect(formatRecordingElapsed(const Duration(seconds: -5)), '0:00');
    });
  });

  group('dragSteps', () {
    test('is zero inside the dead zone, in both directions', () {
      expect(dragSteps(43, 44), 0);
      expect(dragSteps(-43, 44), 0);
      expect(dragSteps(0, 44), 0);
    });

    test('counts whole steps only, truncating toward zero', () {
      expect(dragSteps(44, 44), 1);
      expect(dragSteps(87, 44), 1);
      expect(dragSteps(88, 44), 2);
      expect(dragSteps(-88, 44), -2);
    });

    test('never divides by a zero step', () {
      expect(dragSteps(100, 0), 0);
    });
  });

  group('item size', () {
    test('grows across before it grows down, and stops at the template', () {
      // largeTop is 4 columns x 6 rows.
      expect(size(1, 1).nextSize(), size(2, 1));
      expect(size(3, 1).nextSize(), size(4, 1));
      expect(size(4, 1).nextSize(), size(4, 2));
      expect(size(4, 6).nextSize(), size(4, 6));
    });

    test('shrinks height before width, and stops at 1x1', () {
      expect(size(4, 2).previousSize(), size(4, 1));
      expect(size(4, 1).previousSize(), size(3, 1));
      expect(size(1, 1).previousSize(), size(1, 1));
    });

    test('canGrow / canShrink bound the ends', () {
      expect(size(1, 1).canGrow(), isTrue);
      expect(size(1, 1).canShrink(), isFalse);
      expect(size(4, 6).canGrow(), isFalse);
      expect(size(4, 6).canShrink(), isTrue);
    });

    test('text emphasis follows the cell shape', () {
      // A single-column or single-row cell is compact and never roomy.
      expect(size(1, 4).hasCompactMetricText(), isTrue);
      expect(size(4, 1).hasCompactMetricText(), isTrue);
      expect(size(2, 2).hasCompactMetricText(), isFalse);

      expect(size(2, 2).hasRoomyMetricText(), isTrue);
      expect(size(3, 1).hasRoomyMetricText(), isTrue);
      expect(size(1, 1).hasRoomyMetricText(), isFalse);
      expect(size(2, 1).hasRoomyMetricText(), isFalse);
    });

    test('a resize drag maps offsets onto spans', () {
      expect(size(1, 1).sizeForResizeDrag(const Offset(88, 0), 44), size(3, 1));
      expect(size(2, 2).sizeForResizeDrag(const Offset(-44, -44), 44), size(1, 1));
      // Clamped to the template, not extrapolated.
      expect(size(1, 1).sizeForResizeDrag(const Offset(999, 999), 44), size(4, 6));
    });
  });

  group('recordingDashboardLazyGridRows', () {
    ActivityRecordingDashboardItem item(
      ActivityRecordingDashboardField field,
      int columns,
      int rows,
    ) =>
        ActivityRecordingDashboardItem(field: field, size: size(columns, rows));

    test('a full row of single cells is one row', () {
      expect(
        recordingDashboardLazyGridRows(
          items: [
            item(heartRate, 1, 1),
            item(cadence, 1, 1),
            item(speed, 1, 1),
            item(distance, 1, 1),
          ],
          columns: 4,
        ),
        1,
      );
    });

    test('a tall cell makes its whole line tall', () {
      expect(
        recordingDashboardLazyGridRows(
          items: [item(heartRate, 2, 2), item(cadence, 2, 1)],
          columns: 4,
        ),
        2,
      );
    });

    test('an item that does not fit wraps onto the next line', () {
      expect(
        recordingDashboardLazyGridRows(
          items: [item(heartRate, 3, 1), item(cadence, 2, 1)],
          columns: 4,
        ),
        2,
      );
    });

    test('is at least one row, even with no items', () {
      expect(recordingDashboardLazyGridRows(items: const [], columns: 4), 1);
    });
  });

  group('layout operations', () {
    test('withRemovedField refuses to empty the dashboard', () {
      final single = layoutOf([heartRate]);
      expect(single.withRemovedField(heartRate).fields, [heartRate]);

      final two = layoutOf([heartRate, cadence]);
      expect(two.withRemovedField(heartRate).fields, [cadence]);
    });

    test('withAddedField is a no-op for a field already present', () {
      final layout = layoutOf([heartRate, cadence]);
      expect(identical(layout.withAddedField(heartRate), layout), isTrue);
    });

    test('withAddedField appends a new field', () {
      final layout = layoutOf([heartRate, cadence]);
      expect(layout.withAddedField(speed).fields, contains(speed));
    });

    test('withMovedFieldToTarget lands the field on the target index', () {
      final layout = layoutOf([heartRate, cadence, speed, distance]);
      // Drop-on-target: heartRate takes distance's slot, the rest shift left.
      expect(
        layout.withMovedFieldToTarget(heartRate, distance).fields,
        [cadence, speed, distance, heartRate],
      );
    });

    test('withMovedFieldToTarget is a no-op for an unknown or same field', () {
      final layout = layoutOf([heartRate, cadence]);
      expect(identical(layout.withMovedFieldToTarget(heartRate, heartRate), layout),
          isTrue);
      expect(identical(layout.withMovedFieldToTarget(heartRate, power), layout),
          isTrue);
    });

    test('withAvailableFields drops what the activity cannot measure', () {
      final layout = layoutOf([heartRate, speed, distance]);
      final narrowed = layout.withAvailableFields([heartRate, duration, power]);
      expect(narrowed.fields, [heartRate]);
    });

    test('withAvailableFields falls back to the defaults when nothing survives',
        () {
      final layout = layoutOf([speed, distance]);
      final narrowed = layout.withAvailableFields([duration, power]);
      // Neither speed nor distance is available; DefaultFields ∩ available.
      expect(narrowed.fields, [duration]);
    });

    test('withAvailableFields falls back to the available list when even the '
        'defaults do not intersect', () {
      final layout = layoutOf([speed]);
      final narrowed = layout.withAvailableFields([power]);
      expect(narrowed.fields, [power]);
    });
  });

  group('availableRecordingDashboardFields', () {
    test('a timed activity has no distance or speed', () {
      const state = ActivityRecordingState(
        recordingKind: ActivityRecordingKind.timed,
      );
      expect(availableRecordingDashboardFields(state),
          [heartRate, duration, ActivityRecordingDashboardField.movingTime, power]);
    });

    test('a GPS activity offers the full set, without steps', () {
      const state = ActivityRecordingState(
        recordingKind: ActivityRecordingKind.gpsRoute,
      );
      final fields = availableRecordingDashboardFields(state);
      expect(fields, contains(distance));
      expect(fields, contains(speed));
      expect(fields, isNot(contains(steps)));
    });

    test('a step-counted activity adds steps', () {
      const state = ActivityRecordingState(
        recordingKind: ActivityRecordingKind.gpsRoute,
        activityTypeId: 'treadmill',
      );
      expect(availableRecordingDashboardFields(state), contains(steps));
    });
  });
}
