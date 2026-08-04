package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.compose.ui.geometry.Offset
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardField
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardItem
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardItemSize
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout

/**
 * Unit coverage for the pure half of [ActivityRecordingDashboard.kt] — the
 * elapsed formatter, the drag arithmetic, the item-size ladder, the lazy-grid
 * row count and the layout operations.
 */
class ActivityRecordingDashboardTest {

    private val heartRate = ActivityRecordingDashboardField.HEART_RATE
    private val cadence = ActivityRecordingDashboardField.CADENCE
    private val speed = ActivityRecordingDashboardField.SPEED
    private val distance = ActivityRecordingDashboardField.DISTANCE
    private val duration = ActivityRecordingDashboardField.DURATION
    private val movingTime = ActivityRecordingDashboardField.MOVING_TIME
    private val power = ActivityRecordingDashboardField.POWER
    private val steps = ActivityRecordingDashboardField.STEPS

    private fun size(columns: Int, rows: Int) =
        ActivityRecordingDashboardItemSize(columnSpan = columns, rowSpan = rows)

    private fun layoutOf(fields: List<ActivityRecordingDashboardField>) =
        ActivityRecordingDashboardLayout(fields = fields)

    private fun item(
        field: ActivityRecordingDashboardField,
        columns: Int,
        rows: Int,
    ) = ActivityRecordingDashboardItem(field = field, size = size(columns, rows))

    // ---------------------------------------------------------------- format

    @Test fun `formatRecordingElapsed drops the hour segment under an hour`() {
        assertEquals("0:00", formatRecordingElapsed(Duration.ZERO))
        assertEquals("0:09", formatRecordingElapsed(Duration.ofSeconds(9)))
        assertEquals(
            "12:05",
            formatRecordingElapsed(Duration.ofMinutes(12).plusSeconds(5)),
        )
    }

    @Test fun `formatRecordingElapsed shows zero-padded minutes once there are hours`() {
        assertEquals(
            "1:02:03",
            formatRecordingElapsed(Duration.ofHours(1).plusMinutes(2).plusSeconds(3)),
        )
        assertEquals("10:00:00", formatRecordingElapsed(Duration.ofHours(10)))
    }

    @Test fun `formatRecordingElapsed floors a negative duration at zero`() {
        assertEquals("0:00", formatRecordingElapsed(Duration.ofSeconds(-5)))
    }

    // ------------------------------------------------------------- dragSteps

    @Test fun `dragSteps is zero inside the dead zone, in both directions`() {
        assertEquals(0, 43f.dragSteps(44f))
        assertEquals(0, (-43f).dragSteps(44f))
        assertEquals(0, 0f.dragSteps(44f))
    }

    @Test fun `dragSteps counts whole steps only, truncating toward zero`() {
        assertEquals(1, 44f.dragSteps(44f))
        assertEquals(1, 87f.dragSteps(44f))
        assertEquals(2, 88f.dragSteps(44f))
        assertEquals(-2, (-88f).dragSteps(44f))
    }

    @Test fun `dragSteps never divides by a zero step`() {
        assertEquals(0, 100f.dragSteps(0f))
    }

    // ------------------------------------------------------------- item size

    @Test fun `item size grows across before it grows down, and stops at the template`() {
        // largeTop is 4 columns x 6 rows.
        assertEquals(size(2, 1), size(1, 1).nextSize())
        assertEquals(size(4, 1), size(3, 1).nextSize())
        assertEquals(size(4, 2), size(4, 1).nextSize())
        assertEquals(size(4, 6), size(4, 6).nextSize())
    }

    @Test fun `item size shrinks height before width, and stops at 1x1`() {
        assertEquals(size(4, 1), size(4, 2).previousSize())
        assertEquals(size(3, 1), size(4, 1).previousSize())
        assertEquals(size(1, 1), size(1, 1).previousSize())
    }

    @Test fun `item size canGrow and canShrink bound the ends`() {
        assertTrue(size(1, 1).canGrow())
        assertFalse(size(1, 1).canShrink())
        assertFalse(size(4, 6).canGrow())
        assertTrue(size(4, 6).canShrink())
    }

    @Test fun `item size text emphasis follows the cell shape`() {
        // A single-column or single-row cell is compact and never roomy.
        assertTrue(size(1, 4).hasCompactMetricText())
        assertTrue(size(4, 1).hasCompactMetricText())
        assertFalse(size(2, 2).hasCompactMetricText())

        assertTrue(size(2, 2).hasRoomyMetricText())
        assertTrue(size(3, 1).hasRoomyMetricText())
        assertFalse(size(1, 1).hasRoomyMetricText())
        assertFalse(size(2, 1).hasRoomyMetricText())
    }

    @Test fun `item size a resize drag maps offsets onto spans`() {
        assertEquals(
            size(3, 1),
            size(1, 1).sizeForResizeDrag(dragOffset = Offset(88f, 0f), stepPx = 44f),
        )
        assertEquals(
            size(1, 1),
            size(2, 2).sizeForResizeDrag(dragOffset = Offset(-44f, -44f), stepPx = 44f),
        )
        // Clamped to the template, not extrapolated.
        assertEquals(
            size(4, 6),
            size(1, 1).sizeForResizeDrag(dragOffset = Offset(999f, 999f), stepPx = 44f),
        )
    }

    // -------------------------------------------- recordingDashboardLazyGridRows

    @Test fun `recordingDashboardLazyGridRows a full row of single cells is one row`() {
        assertEquals(
            1,
            recordingDashboardLazyGridRows(
                items = listOf(
                    item(heartRate, 1, 1),
                    item(cadence, 1, 1),
                    item(speed, 1, 1),
                    item(distance, 1, 1),
                ),
                columns = 4,
            ),
        )
    }

    @Test fun `recordingDashboardLazyGridRows a tall cell makes its whole line tall`() {
        assertEquals(
            2,
            recordingDashboardLazyGridRows(
                items = listOf(item(heartRate, 2, 2), item(cadence, 2, 1)),
                columns = 4,
            ),
        )
    }

    @Test fun `recordingDashboardLazyGridRows an item that does not fit wraps onto the next line`() {
        assertEquals(
            2,
            recordingDashboardLazyGridRows(
                items = listOf(item(heartRate, 3, 1), item(cadence, 2, 1)),
                columns = 4,
            ),
        )
    }

    @Test fun `recordingDashboardLazyGridRows is at least one row, even with no items`() {
        assertEquals(1, recordingDashboardLazyGridRows(items = emptyList(), columns = 4))
    }

    // ------------------------------------------------------ layout operations

    @Test fun `layout operations withRemovedField refuses to empty the dashboard`() {
        val single = layoutOf(listOf(heartRate))
        assertEquals(listOf(heartRate), single.withRemovedField(heartRate).fields)

        val two = layoutOf(listOf(heartRate, cadence))
        assertEquals(listOf(cadence), two.withRemovedField(heartRate).fields)
    }

    @Test fun `layout operations withAddedField is a no-op for a field already present`() {
        val layout = layoutOf(listOf(heartRate, cadence))
        assertSame(layout, layout.withAddedField(heartRate))
    }

    @Test fun `layout operations withAddedField appends a new field`() {
        val layout = layoutOf(listOf(heartRate, cadence))
        assertTrue(speed in layout.withAddedField(speed).fields)
    }

    @Test fun `layout operations withMovedFieldToTarget lands the field on the target index`() {
        val layout = layoutOf(listOf(heartRate, cadence, speed, distance))
        // Drop-on-target: heartRate takes distance's slot, the rest shift left.
        assertEquals(
            listOf(cadence, speed, distance, heartRate),
            layout.withMovedFieldToTarget(heartRate, distance).fields,
        )
    }

    @Test fun `layout operations withMovedFieldToTarget is a no-op for an unknown or same field`() {
        val layout = layoutOf(listOf(heartRate, cadence))
        assertSame(layout, layout.withMovedFieldToTarget(heartRate, heartRate))
        assertSame(layout, layout.withMovedFieldToTarget(heartRate, power))
    }

    @Test fun `layout operations withAvailableFields drops what the activity cannot measure`() {
        val layout = layoutOf(listOf(heartRate, speed, distance))
        val narrowed = layout.withAvailableFields(listOf(heartRate, duration, power))
        assertEquals(listOf(heartRate), narrowed.fields)
    }

    @Test fun `layout operations withAvailableFields falls back to the defaults when nothing survives`() {
        val layout = layoutOf(listOf(speed, distance))
        val narrowed = layout.withAvailableFields(listOf(duration, power))
        // Neither speed nor distance is available; DefaultFields intersected with available.
        assertEquals(listOf(duration), narrowed.fields)
    }

    @Test fun `layout operations withAvailableFields falls back to the available list when even the defaults do not intersect`() {
        val layout = layoutOf(listOf(speed))
        val narrowed = layout.withAvailableFields(listOf(power))
        assertEquals(listOf(power), narrowed.fields)
    }

    // ------------------------------------ availableRecordingDashboardFields

    @Test fun `availableRecordingDashboardFields a timed activity has no distance or speed`() {
        val state = ActivityRecordingState(recordingKind = ActivityRecordingKind.TIMED)
        assertEquals(
            listOf(heartRate, duration, movingTime, power),
            availableRecordingDashboardFields(state),
        )
    }

    @Test fun `availableRecordingDashboardFields a GPS activity offers the full set, without steps`() {
        val state = ActivityRecordingState(recordingKind = ActivityRecordingKind.GPS_ROUTE)
        val fields = availableRecordingDashboardFields(state)
        assertTrue(distance in fields)
        assertTrue(speed in fields)
        assertFalse(steps in fields)
    }

    @Test fun `availableRecordingDashboardFields a step-counted activity adds steps`() {
        val state = ActivityRecordingState(
            recordingKind = ActivityRecordingKind.GPS_ROUTE,
            activityTypeId = "treadmill",
        )
        assertTrue(steps in availableRecordingDashboardFields(state))
    }
}
