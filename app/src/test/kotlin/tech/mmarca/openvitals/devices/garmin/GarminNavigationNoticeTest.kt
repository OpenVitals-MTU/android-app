package tech.mmarca.openvitals.devices.garmin

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.GarminNavigationRelayPolicy.Decision
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState

class GarminNavigationNoticeTest {

    private fun snapshot(
        car: String = "TURN_LEFT",
        pedestrian: String = "",
        street: String = "Calle Mayor",
        toTurn: String = "450 m",
        toTarget: String = "2.3 km",
        seconds: Int? = 720,
        exit: String = "",
    ) = CoMapsNavigationSnapshot(
        sampledAt = Instant.EPOCH,
        sessionState = "OnRoute",
        nextStreet = street,
        distanceToTurn = toTurn,
        distanceToTarget = toTarget,
        totalTimeSeconds = seconds,
        carDirection = car,
        pedestrianDirection = pedestrian,
        exitNumber = exit,
    )

    private fun active(s: CoMapsNavigationSnapshot) = CoMapsNavigationState.Active(s)

    @Test fun `a turn reads as title, distance, street and what is left`() {
        val notice = GarminNavigationNotice.from(snapshot())
        assertEquals("Turn left", notice.title)
        assertEquals("450 m", notice.subtitle)
        assertEquals("Calle Mayor\n2.3 km left · 12 min", notice.body)
    }

    @Test fun `roundabouts name the exit and pedestrian directions count too`() {
        assertEquals(
            "Roundabout, exit 3",
            GarminNavigationNotice.from(snapshot(car = "ENTER_ROUND_ABOUT", exit = "3")).title,
        )
        assertEquals(
            "Arrive at destination",
            GarminNavigationNotice.from(snapshot(car = "", pedestrian = "REACHED_YOUR_DESTINATION")).title,
        )
        // A direction with no phrase of its own still reads as words, never as an enum.
        assertEquals(
            "Exit highway to nowhere",
            GarminNavigationNotice.from(snapshot(car = "EXIT_HIGHWAY_TO_NOWHERE")).title,
        )
    }

    @Test fun `long remaining times read in hours`() {
        assertTrue(GarminNavigationNotice.from(snapshot(seconds = 3_600)).body.endsWith("1 h"))
        assertTrue(GarminNavigationNotice.from(snapshot(seconds = 5_400)).body.endsWith("1 h 30 min"))
    }

    @Test fun `a new manoeuvre goes out at once, a countdown waits, a repeat never`() {
        val policy = GarminNavigationRelayPolicy(refreshIntervalMillis = 5_000)

        val first = policy.decide(active(snapshot(toTurn = "450 m")), nowMillis = 0)
        assertTrue(first is Decision.Show && !first.notice.isUpdate)

        // Same turn, a few metres closer: not worth the radio yet.
        assertEquals(Decision.Nothing, policy.decide(active(snapshot(toTurn = "440 m")), nowMillis = 1_000))
        // Identical reading, even past the interval: nothing to say.
        assertEquals(Decision.Nothing, policy.decide(active(snapshot(toTurn = "450 m")), nowMillis = 9_000))
        // Countdown moved and the interval passed: refresh, in place.
        val refreshed = policy.decide(active(snapshot(toTurn = "300 m")), nowMillis = 9_500)
        assertTrue(refreshed is Decision.Show && refreshed.notice.isUpdate)
        assertEquals("300 m", (refreshed as Decision.Show).notice.notice.subtitle)

        // The street changed: immediately, interval or not.
        val turned = policy.decide(active(snapshot(car = "TURN_RIGHT", street = "Gran Vía", toTurn = "900 m")), nowMillis = 9_600)
        assertTrue(turned is Decision.Show && turned.notice.isUpdate)
    }

    @Test fun `guidance ending withdraws once and only once`() {
        val policy = GarminNavigationRelayPolicy()
        assertEquals(Decision.Nothing, policy.decide(CoMapsNavigationState.NotNavigating, 0))
        policy.decide(active(snapshot()), 0)
        assertEquals(Decision.Withdraw, policy.decide(CoMapsNavigationState.Disabled, 1))
        assertEquals(Decision.Nothing, policy.decide(CoMapsNavigationState.Disabled, 2))
        // And the next route starts with an ADD again.
        val again = policy.decide(active(snapshot()), 3)
        assertTrue(again is Decision.Show && !again.notice.isUpdate)
    }
}
