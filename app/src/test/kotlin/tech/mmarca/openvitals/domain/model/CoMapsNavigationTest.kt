package tech.mmarca.openvitals.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoMapsNavigationTest {

    private fun snapshot(
        at: Instant,
        currentStreet: String = "Tartu mnt",
        sessionState: String = "OnRoute",
    ) = CoMapsNavigationSnapshot(
        sampledAt = at,
        sessionState = sessionState,
        currentStreet = currentStreet,
    )

    private val start: Instant = Instant.parse("2026-07-04T10:00:00Z")

    // CoMaps sends the enum name of its direction type and has spelled it differently across builds.
    @Test fun `turn kinds read both spellings CoMaps has used`() {
        assertEquals(CoMapsTurnKind.RIGHT, coMapsTurnKindForDirection("TURN_RIGHT"))
        assertEquals(CoMapsTurnKind.LEFT, coMapsTurnKindForDirection("TurnLeft"))
        assertEquals(CoMapsTurnKind.SLIGHT_RIGHT, coMapsTurnKindForDirection("TURN_SLIGHT_RIGHT"))
        assertEquals(CoMapsTurnKind.SHARP_LEFT, coMapsTurnKindForDirection("TurnSharpLeft"))
        assertEquals(CoMapsTurnKind.U_TURN, coMapsTurnKindForDirection("U_TURN_LEFT"))
        assertEquals(CoMapsTurnKind.ROUNDABOUT, coMapsTurnKindForDirection("EnterRoundabout"))
        assertEquals(CoMapsTurnKind.FINISH, coMapsTurnKindForDirection("ReachedDestination"))
        assertEquals(CoMapsTurnKind.STRAIGHT, coMapsTurnKindForDirection("GO_STRAIGHT"))
    }

    @Test fun `a qualified turn beats the bare one it contains`() {
        // "TurnSlightRight" contains "RIGHT"; if the bare match ran first, every slight turn drew the wrong arrow.
        assertEquals(CoMapsTurnKind.SLIGHT_RIGHT, coMapsTurnKindForDirection("TurnSlightRight"))
        assertEquals(CoMapsTurnKind.SHARP_RIGHT, coMapsTurnKindForDirection("TurnSharpRight"))
        assertEquals(CoMapsTurnKind.RIGHT, coMapsTurnKindForDirection("ExitHighwayToRight"))
    }

    @Test fun `an empty or unknown direction is unknown, not straight`() {
        assertEquals(CoMapsTurnKind.UNKNOWN, coMapsTurnKindForDirection(""))
        assertEquals(CoMapsTurnKind.UNKNOWN, coMapsTurnKindForDirection("  "))
        assertEquals(CoMapsTurnKind.UNKNOWN, coMapsTurnKindForDirection("SomethingCoMapsAddedLater"))
    }

    @Test fun `renders a raw direction as something a person would read`() {
        assertEquals("Turn right", coMapsReadableDirection("TURN_RIGHT"))
        assertEquals("Turn slight left", coMapsReadableDirection("TurnSlightLeft"))
        assertEquals("", coMapsReadableDirection(""))
    }

    @Test fun `the recorder keeps the first reading it is given`() {
        val recorder = CoMapsNavigationSampleRecorder()

        assertTrue(recorder.accept(snapshot(at = start)))
        assertEquals(1, recorder.samples.size)
    }

    @Test fun `the recorder drops a reading that says the same thing, too soon`() {
        val recorder = CoMapsNavigationSampleRecorder()
        recorder.accept(snapshot(at = start))

        assertFalse(recorder.accept(snapshot(at = start.plusSeconds(5))))
        assertEquals(1, recorder.samples.size)
    }

    @Test fun `the recorder keeps a reading that says something new, however soon`() {
        // A flurry of turns must never be missed to save space.
        val recorder = CoMapsNavigationSampleRecorder()
        recorder.accept(snapshot(at = start))

        assertTrue(recorder.accept(snapshot(at = start.plusSeconds(1), currentStreet = "Liivalaia")))
        assertEquals(2, recorder.samples.size)
    }

    @Test fun `the recorder keeps an unchanged reading once the interval has passed`() {
        // A long straight road costs one sample every 15 seconds, not fifteen.
        val recorder = CoMapsNavigationSampleRecorder()
        recorder.accept(snapshot(at = start))

        assertFalse(recorder.accept(snapshot(at = start.plusSeconds(14))))
        assertTrue(recorder.accept(snapshot(at = start.plusSeconds(15))))
        assertEquals(2, recorder.samples.size)
    }

    @Test fun `reset forgets the run`() {
        val recorder = CoMapsNavigationSampleRecorder()
        recorder.accept(snapshot(at = start))

        recorder.reset()

        assertTrue(recorder.samples.isEmpty())
        assertTrue(recorder.accept(snapshot(at = start)))
    }

    @Test fun `the content key ignores the clock and nothing else`() {
        val a = snapshot(at = Instant.parse("2026-07-04T10:00:00Z"))
        val b = snapshot(at = Instant.parse("2026-07-04T11:00:00Z"))
        val c = snapshot(at = Instant.parse("2026-07-04T10:00:00Z"), sessionState = "Finish")

        assertEquals(a.contentKey, b.contentKey)
        assertNotEquals(a.contentKey, c.contentKey)
    }

    // The full vocabulary from CoMaps' RoutingSessionState, so a value added upstream shows up as a decision to make.
    @Test fun `only a live session counts as guidance`() {
        listOf("OnRoute", "OffRoute", "RouteNeedsRebuild", "RouteRebuilding").forEach { state ->
            assertTrue(state, isCoMapsGuiding(state))
        }
        listOf(
            "NoValidRoute", "RouteBuilding", "RouteNotStarted", "RouteFinished", "RouteNoFollowing",
        ).forEach { state ->
            assertFalse(state, isCoMapsGuiding(state))
        }
    }

    @Test fun `an unknown state is not guidance`() {
        assertFalse(isCoMapsGuiding("SomethingNewUpstream"))
        assertFalse(isCoMapsGuiding(""))
    }

    @Test fun `the direction comes from whichever field CoMaps filled`() {
        val driving = snapshot(at = start).copy(carDirection = "TurnRight")
        val walking = snapshot(at = start).copy(pedestrianDirection = "TurnLeft")

        assertEquals("TurnRight", coMapsNavigationDirection(driving))
        assertEquals("TurnLeft", coMapsNavigationDirection(walking))
    }

    @Test fun `route polylines compare by revision and size, not point by point`() {
        val a = CoMapsRoutePolyline(revision = 3, points = DoubleArray(4000) { it.toDouble() })
        val b = CoMapsRoutePolyline(revision = 3, points = DoubleArray(4000) { -it.toDouble() })
        val c = CoMapsRoutePolyline(revision = 4, points = DoubleArray(4000) { it.toDouble() })

        assertEquals(a, b)
        assertNotEquals(a, c)
        assertTrue(CoMapsRoutePolyline(revision = 0, points = DoubleArray(2)).isEmpty)
        assertEquals(2000, a.pointCount)
    }
}
