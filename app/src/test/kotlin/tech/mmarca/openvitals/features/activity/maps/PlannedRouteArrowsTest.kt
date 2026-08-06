package tech.mmarca.openvitals.features.activity.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline

class PlannedRouteArrowsTest {

    private fun polyline(vararg latLon: Double) =
        CoMapsRoutePolyline(revision = 1, points = latLon)

    @Test fun `a straight route has no arrows`() {
        val straight = polyline(59.0, 24.0, 59.001, 24.0, 59.002, 24.0)
        assertTrue(plannedRouteTurnArrows(straight).isEmpty())
    }

    @Test fun `a right-angle bend gets one bent arrow pointing out of it`() {
        // North, then east: the shaft bends at the corner, the head points east.
        val bend = polyline(59.0, 24.0, 59.001, 24.0, 59.001, 24.002)

        val arrows = plannedRouteTurnArrows(bend)

        assertEquals(1, arrows.size)
        val arrow = arrows[0]
        // The corner is the shaft's middle point.
        assertEquals(59.001, arrow.shaft[2], 1e-9)
        assertEquals(24.0, arrow.shaft[3], 1e-9)
        // The entry arm reaches back south, the exit arm on east.
        assertTrue(arrow.shaft[0] < 59.001)
        assertEquals(24.0, arrow.shaft[1], 1e-9)
        assertEquals(59.001, arrow.shaft[4], 1e-9)
        assertTrue(arrow.shaft[5] > 24.0)
        // The head sits at the exit arm's tip and points east.
        assertEquals(arrow.shaft[4], arrow.headLatitude, 0.0)
        assertEquals(arrow.shaft[5], arrow.headLongitude, 0.0)
        assertEquals(90f, arrow.bearingDegrees, 1f)
    }

    @Test fun `an arm never swallows the next corner`() {
        // Segments only ~5.5 m long: arms must cap at 80% of the segment.
        val tight = polyline(59.0, 24.0, 59.00005, 24.0, 59.00005, 24.0001)

        val arrow = plannedRouteTurnArrows(tight).single()

        assertTrue(arrow.shaft[0] >= 59.00005 - 0.00005 * 0.8 - 1e-12)
    }

    @Test fun `a gentle drift below the threshold stays clean`() {
        // ~11 degrees of bearing change: a road curving, not a turn.
        val gentle = polyline(59.0, 24.0, 59.001, 24.0, 59.002, 24.0004)
        assertTrue(plannedRouteTurnArrows(gentle).isEmpty())
    }

    @Test fun `duplicate points do not fake a turn`() {
        val stutter = polyline(59.0, 24.0, 59.001, 24.0, 59.001, 24.0, 59.002, 24.0)
        assertTrue(plannedRouteTurnArrows(stutter).isEmpty())
    }

    @Test fun `two points cannot bend`() {
        assertTrue(plannedRouteTurnArrows(polyline(59.0, 24.0, 59.001, 24.0)).isEmpty())
        assertTrue(plannedRouteTurnArrows(null).isEmpty())
    }
}
