package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OversizedRouteShrinkerTest {

    private val start = Instant.parse("2026-08-01T06:00:00Z")

    private fun route(points: Int): ExerciseRoute = ExerciseRoute(
        List(points) { i ->
            ExerciseRoute.Location(
                time = start.plusSeconds(i.toLong()),
                latitude = 40.0 + i * 1e-5,
                longitude = -3.0,
            )
        },
    )

    private fun session(points: Int): ExerciseSessionRecord = ExerciseSessionRecord(
        startTime = start,
        startZoneOffset = null,
        endTime = start.plusSeconds(points.toLong() + 60),
        endZoneOffset = null,
        metadata = Metadata.manualEntry(clientRecordId = "s$points"),
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        title = "Long ride",
        exerciseRoute = route(points),
    )

    private fun ExerciseSessionRecord.routePoints(): List<ExerciseRoute.Location> =
        (exerciseRouteResult as ExerciseRouteResult.Data).exerciseRoute.route

    @Test fun `reads the limit and actual size out of the platform message`() {
        val e = IllegalArgumentException("Record 0 exceeds single record size limit: 1000000, was: 1700644")
        assertEquals(1_000_000L to 1_700_644L, OversizedRouteShrinker.recordSizeOverrun(e))
    }

    @Test fun `an unrelated failure is not a size overrun`() {
        assertNull(OversizedRouteShrinker.recordSizeOverrun(IllegalStateException("rate limited")))
        assertNull(OversizedRouteShrinker.recordSizeOverrun(IllegalArgumentException("single record size limit: 10, was: 5")))
    }

    @Test fun `decimates by the overrun ratio keeping first and last point`() {
        val original = session(20_000)
        val shrunk = OversizedRouteShrinker.shrink(listOf(original), limit = 1_000_000L, was = 2_000_000L)!!
        val points = (shrunk.single() as ExerciseSessionRecord).routePoints()

        assertEquals(9_000, points.size)
        assertEquals(original.routePoints().first(), points.first())
        assertEquals(original.routePoints().last(), points.last())
        points.zipWithNext { a, b -> assertTrue(a.time.isBefore(b.time)) }
        assertEquals("Long ride", (shrunk.single() as ExerciseSessionRecord).title)
    }

    @Test fun `records without a route pass through untouched and alone yield null`() {
        val steps = StepsRecord(
            startTime = start,
            startZoneOffset = null,
            endTime = start.plusSeconds(60),
            endZoneOffset = null,
            count = 10,
            metadata = Metadata.manualEntry(),
        )
        assertNull(OversizedRouteShrinker.shrink(listOf<androidx.health.connect.client.records.Record>(steps), limit = 10L, was = 20L))

        val mixed = OversizedRouteShrinker.shrink(listOf<androidx.health.connect.client.records.Record>(steps, session(100)), limit = 10L, was = 20L)!!
        assertSame(steps, mixed[0])
        assertEquals(45, (mixed[1] as ExerciseSessionRecord).routePoints().size)
    }

    @Test fun `decimation never repeats an index even for tiny targets`() {
        val points = route(7).route
        assertEquals(listOf(0, 3, 6), OversizedRouteShrinker.decimate(points, 3).map { points.indexOf(it) })
        assertEquals(listOf(0, 6), OversizedRouteShrinker.decimate(points, 2).map { points.indexOf(it) })
        assertEquals(7, OversizedRouteShrinker.decimate(points, 50).size)
    }
}
