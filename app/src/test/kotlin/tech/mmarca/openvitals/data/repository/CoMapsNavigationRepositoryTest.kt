package tech.mmarca.openvitals.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.comaps.CoMapsLiveEvent
import tech.mmarca.openvitals.comaps.CoMapsNavigationSource
import tech.mmarca.openvitals.comaps.CoMapsProviderAnswer
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState

class CoMapsNavigationRepositoryTest {

    private val source = mockk<CoMapsNavigationSource>(relaxed = true)

    private fun repository(): CoMapsNavigationRepositoryImpl {
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val context = mockk<Context> {
            every { getSharedPreferences(any(), any()) } returns prefs
        }
        return CoMapsNavigationRepositoryImpl(context, source)
    }

    private fun guidingRow(
        sessionState: String = "OnRoute",
        extras: Map<String, Any?> = emptyMap(),
    ): Map<String, Any?> = mapOf(
        "session_state" to sessionState,
        "current_street" to "Tartu mnt",
        "next_street" to "Liivalaia",
        "dist_to_turn" to "450 m",
        "dist_to_target" to "1.2 km",
        "total_time_seconds" to 900L,
        "completion_percent" to 63.2,
        "car_direction" to "TurnRight",
        "exit_num" to 0L,
    ) + extras

    @Test fun `each provider status maps to its own state`() = runTest {
        val repo = repository()
        val expectations = mapOf(
            CoMapsProviderAnswer.AppUnavailable to CoMapsNavigationState.AppUnavailable,
            CoMapsProviderAnswer.ProviderUnavailable to CoMapsNavigationState.ProviderUnavailable,
            CoMapsProviderAnswer.PermissionMissing to CoMapsNavigationState.PermissionMissing,
            CoMapsProviderAnswer.NotNavigating to CoMapsNavigationState.NotNavigating,
        )
        for ((answer, expected) in expectations) {
            coEvery { source.queryLive() } returns answer
            assertEquals(expected, repo.readLive())
        }
        coEvery { source.queryLive() } returns CoMapsProviderAnswer.Failure("boom")
        assertEquals(CoMapsNavigationState.Error("boom"), repo.readLive())
    }

    @Test fun `a guiding row becomes Active with the geometry extras read off the row`() = runTest {
        val repo = repository()
        coEvery { source.queryLive() } returns CoMapsProviderAnswer.Active(
            guidingRow(
                extras = mapOf(
                    "route_revision" to 7L,
                    "dest_lat" to 59.437,
                    "dest_lon" to 24.7536,
                    "dest_title" to " Old Town ",
                ),
            ),
        )

        val state = repo.readLive() as CoMapsNavigationState.Active

        assertEquals("Tartu mnt", state.snapshot.currentStreet)
        assertEquals("450 m", state.snapshot.distanceToTurn)
        assertEquals(900, state.snapshot.totalTimeSeconds)
        assertEquals(7, state.routeRevision)
        assertEquals(59.437, state.destination!!.latitude, 1e-9)
        assertEquals("Old Town", state.destinationName)
    }

    @Test fun `a cached row whose session is over reads as not navigating`() = runTest {
        // CoMaps never clears its routing cache: a finished route keeps being
        // served, session_state and all. The state column is the only signal.
        val repo = repository()
        coEvery { source.queryLive() } returns
            CoMapsProviderAnswer.Active(guidingRow(sessionState = "RouteFinished"))

        assertEquals(CoMapsNavigationState.NotNavigating, repo.readLive())
    }

    @Test fun `an integer exit number and a zero exit both survive`() = runTest {
        // exit_num arrives as an int straight off RoutingInfo.exitNum; casting
        // it as a string took the whole panel down for entire routes once.
        val repo = repository()
        coEvery { source.queryLive() } returns
            CoMapsProviderAnswer.Active(guidingRow(extras = mapOf("exit_num" to 3L)))
        assertEquals("3", (repo.readLive() as CoMapsNavigationState.Active).snapshot.exitNumber)

        coEvery { source.queryLive() } returns
            CoMapsProviderAnswer.Active(guidingRow(extras = mapOf("exit_num" to 0L)))
        // "Exit 0" is not a thing: zero reads as no exit at all.
        assertEquals("", (repo.readLive() as CoMapsNavigationState.Active).snapshot.exitNumber)
    }

    @Test fun `while observed, a row with no recent change is not believed`() = runTest {
        val repo = repository()
        var clock = Instant.parse("2026-08-06T10:00:00Z")
        repo.now = { clock }
        val row = CoMapsProviderAnswer.Active(guidingRow())

        // The feed opens observing, and a live change arrives.
        val opening = repo.watchLiveOf(
            CoMapsLiveEvent(row, live = false, observing = true, initial = true),
            CoMapsLiveEvent(row, live = true, observing = true, initial = false),
        )
        // The initial read is within no window yet -> not navigating; the live
        // change is evidence -> active.
        assertEquals(CoMapsNavigationState.NotNavigating, opening[0])
        assertTrue(opening[1] is CoMapsNavigationState.Active)

        // The safety poll asks again 20 seconds later. Nothing has changed
        // since — the route ended — so the same row is no longer believed.
        clock = clock.plusSeconds(20)
        coEvery { source.queryLive() } returns row
        assertEquals(CoMapsNavigationState.NotNavigating, repo.readLive())
    }

    @Test fun `a new watch does not inherit the last recording's evidence`() = runTest {
        val repo = repository()
        var clock = Instant.parse("2026-08-06T10:00:00Z")
        repo.now = { clock }
        val row = CoMapsProviderAnswer.Active(guidingRow())

        // First recording: a live change makes the route believable.
        repo.watchLiveOf(CoMapsLiveEvent(row, live = true, observing = true, initial = false))

        // Ten seconds later a NEW watch opens — still inside the old window.
        clock = clock.plusSeconds(10)
        val fresh = repo.watchLiveOf(
            CoMapsLiveEvent(row, live = false, observing = true, initial = true),
        )

        // Without the initial reset this would be the previous route, live.
        assertEquals(CoMapsNavigationState.NotNavigating, fresh[0])
    }

    @Test fun `without an observer the read is believed as-is`() = runTest {
        val repo = repository()
        val row = CoMapsProviderAnswer.Active(guidingRow())

        val states = repo.watchLiveOf(
            CoMapsLiveEvent(row, live = false, observing = false, initial = true),
        )

        // No observer means no evidence can ever arrive; the row is all there is.
        assertTrue(states[0] is CoMapsNavigationState.Active)
    }

    @Test fun `route geometry needs at least two points`() = runTest {
        val repo = repository()
        coEvery { source.queryRoute() } returns DoubleArray(2) { it.toDouble() }
        assertNull(repo.readRouteGeometry(revision = 1))

        // A real corner (the middle point sits ~100 m off the chord), so the
        // sub-pixel simplification must keep all three.
        coEvery { source.queryRoute() } returns
            doubleArrayOf(59.0, 24.0, 59.001, 24.002, 59.0, 24.004)
        val polyline = repo.readRouteGeometry(revision = 1)!!
        assertEquals(3, polyline.pointCount)
        assertEquals(1, polyline.revision)
    }

    @Test fun `route geometry drops what the eye cannot see`() = runTest {
        // Three collinear points: the middle one adds nothing to the line.
        val repo = repository()
        coEvery { source.queryRoute() } returns
            doubleArrayOf(59.0, 24.0, 59.001, 24.001, 59.002, 24.002)

        val polyline = repo.readRouteGeometry(revision = 2)!!

        assertEquals(2, polyline.pointCount)
        assertEquals(59.0, polyline.latitudeAt(0), 1e-9)
        assertEquals(59.002, polyline.latitudeAt(1), 1e-9)
    }

    @Test fun `samples round-trip through the compact encoding`() {
        val samples = listOf(
            CoMapsNavigationSnapshot(
                sampledAt = Instant.parse("2026-08-06T10:00:00Z"),
                sessionState = "OnRoute",
                currentStreet = "Calle de Alcalá, 42",
                nextStreet = "Gran Vía",
                distanceToTurn = "450 m",
                distanceToTarget = "1,2 km",
                totalTimeSeconds = 900,
                completionPercent = 63.2,
                carDirection = "TurnRight",
                exitNumber = "3",
            ),
            CoMapsNavigationSnapshot(
                sampledAt = Instant.parse("2026-08-06T10:00:15Z"),
                sessionState = "OffRoute",
            ),
        )

        val decoded = decodeCoMapsSamples(encodeCoMapsSamples(samples))

        assertEquals(samples, decoded)
    }

    @Test fun `decoding sorts by time and drops what it cannot read`() {
        val early = CoMapsNavigationSnapshot(
            sampledAt = Instant.parse("2026-08-06T10:00:00Z"),
            sessionState = "OnRoute",
        )
        val late = CoMapsNavigationSnapshot(
            sampledAt = Instant.parse("2026-08-06T11:00:00Z"),
            sessionState = "OnRoute",
        )
        val encoded = encodeCoMapsSamples(listOf(late, early))
        val withJunk = "not,a,sample\n$encoded\n\ngarbage"

        val decoded = decodeCoMapsSamples(withJunk)

        assertEquals(listOf(early, late), decoded)
        assertTrue(decodeCoMapsSamples("").isEmpty())
    }

    private suspend fun CoMapsNavigationRepositoryImpl.watchLiveOf(
        vararg events: CoMapsLiveEvent,
    ): List<CoMapsNavigationState> {
        every { source.liveUpdates() } returns flowOf(*events)
        return watchLive().toList()
    }
}
