package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.insights.RouteElevation
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.activity.elevation.ElevationTileRepository

/** DEM altitudes replace the file's only when every point is covered; the gain follows. */
class RouteElevationCorrectionTest {

    private val start = Instant.parse("2026-05-26T08:30:00Z")

    private fun point(index: Int, altitude: Double?) = ExerciseRoutePoint(
        time = start.plusSeconds(index.toLong()),
        latitude = 59.0 + index * 0.001,
        longitude = 24.0 + index * 0.001,
        altitudeMeters = altitude,
        horizontalAccuracyMeters = null,
        verticalAccuracyMeters = null,
    )

    private fun import(points: List<ExerciseRoutePoint>, gain: Double = 999.0) = RouteFileImport(
        fileName = "ride.fit",
        points = points,
        distanceMeters = 1234.0,
        elevationGainedMeters = gain,
        startTime = start,
        endTime = start.plusSeconds(60),
    )

    @Test fun `every covered point takes the DEM altitude`() {
        val points = listOf(point(0, 10.0), point(1, 12.0), point(2, 10.0))

        val corrected = correctRouteAltitudes(points) { lat, _ -> (lat - 59.0) * 100_000 }!!

        assertEquals(listOf(0.0, 100.0, 200.0), corrected.map { it.altitudeMeters!! }.map { Math.round(it).toDouble() })
        assertEquals(points.map { it.time }, corrected.map { it.time })
    }

    @Test fun `one uncovered point drops the whole correction`() {
        val points = listOf(point(0, 10.0), point(1, 12.0), point(2, 10.0))

        assertNull(correctRouteAltitudes(points) { lat, _ -> if (lat > 59.0015) null else 5.0 })
        assertNull(correctRouteAltitudes(emptyList()) { _, _ -> 5.0 })
    }

    @Test fun `the gain is recomputed and everything else is kept`() {
        val original = import(listOf(point(0, 0.0), point(1, 0.0)))
        val corrected = listOf(point(0, 100.0), point(1, 130.0))

        val result = original.withDemElevation(corrected)

        assertEquals(corrected, result.points)
        assertEquals(RouteElevation.routeElevationGain(corrected), result.elevationGainedMeters, 1e-9)
        assertEquals(30.0, result.elevationGainedMeters, 1e-9)
        assertEquals(original.distanceMeters, result.distanceMeters, 1e-9)
        assertEquals(original.startTime, result.startTime)
        assertEquals(original.bleSamples, result.bleSamples)
    }

    @Test fun `the corrector overrides the file's own ascent when tiles cover the route`() {
        val tiles = mockk<ElevationTileRepository>()
        every { tiles.hasTiles } returns true
        every { tiles.elevationAt(any(), any()) } answers { (firstArg<Double>() - 59.0) * 10_000 }
        val corrector = RouteElevationCorrector(tiles, preferences(enabled = true))
        val original = import(listOf(point(0, 10.0), point(1, 12.0)), gain = 999.0)

        val result = corrector.correct(original)

        assertEquals(10.0, result.elevationGainedMeters, 1e-6)
        assertEquals(listOf(0.0, 10.0), result.points.map { Math.round(it.altitudeMeters!!).toDouble() })
    }

    @Test fun `the corrector keeps the file when coverage is incomplete`() {
        val tiles = mockk<ElevationTileRepository>()
        every { tiles.hasTiles } returns true
        every { tiles.elevationAt(any(), any()) } returns null
        val corrector = RouteElevationCorrector(tiles, preferences(enabled = true))
        val original = import(listOf(point(0, 10.0), point(1, 12.0)), gain = 999.0)

        assertSame(original, corrector.correct(original))
    }

    @Test fun `the corrector is a no-op when off, without tiles, or without points`() {
        val tiles = mockk<ElevationTileRepository>()
        every { tiles.hasTiles } returns true
        every { tiles.elevationAt(any(), any()) } returns 1.0
        val original = import(listOf(point(0, 10.0), point(1, 12.0)))

        assertSame(original, RouteElevationCorrector(tiles, preferences(enabled = false)).correct(original))

        val noTiles = mockk<ElevationTileRepository>()
        every { noTiles.hasTiles } returns false
        assertSame(original, RouteElevationCorrector(noTiles, preferences(enabled = true)).correct(original))
        verify(exactly = 0) { noTiles.elevationAt(any(), any()) }

        val routeless = import(emptyList())
        assertSame(routeless, RouteElevationCorrector(tiles, preferences(enabled = true)).correct(routeless))
    }

    private fun preferences(enabled: Boolean) = mockk<PreferencesRepository> {
        every { elevationCorrectionEnabled } returns enabled
    }
}
