package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.core.geo.HgtResolution
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.activity.elevation.ElevationTileRepository
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.FitTestFiles
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.FitTestPoint
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteElevationCorrector

/**
 * Only ACTIVITY files are considered, a file the parser rejects is skipped
 * rather than sinking the sync, and an imported tile corrects the route.
 */
class GarminActivityImporterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)

    private fun file(type: GarminFileType, bytes: ByteArray = byteArrayOf(1, 2, 3)) =
        GarminDownloadedFile(
            entry = GarminDirectoryEntry(
                fileIndex = 7,
                type = type,
                fileNumber = 7,
                specificFlags = 0,
                fileFlags = 0,
                fileSize = bytes.size.toLong(),
                fileDate = Instant.parse("2026-06-10T08:00:00Z"),
            ),
            bytes = bytes,
        )

    @Test
    fun `nothing to do without activity files`() = runTest {
        val written = importer().import(
            listOf(file(GarminFileType.SLEEP), file(GarminFileType.MONITOR)),
        )

        assertEquals(0, written)
        coVerify(exactly = 0) { activityRepository.writeActivityEntries(any()) }
        coVerify(exactly = 0) { activityRepository.writeActivityEntry(any()) }
    }

    @Test
    fun `an undecodable activity file is skipped, never thrown`() = runTest {
        // Three junk bytes are not a FIT file; the import must swallow that per file.
        val written = importer().import(listOf(file(GarminFileType.ACTIVITY)))

        assertEquals(0, written)
        coVerify(exactly = 0) { activityRepository.writeActivityEntries(any()) }
    }

    @Test
    fun `an empty download list is a no-op`() = runTest {
        assertEquals(0, importer().import(emptyList()))
    }

    @Test
    fun `an imported tile replaces the watch's altitudes and ascent`() = runTest {
        // Rises 10 m per grid row to the north: 6100 m at 59.5 N, 6112 m at 59.501 N.
        writeTile("N59E024.hgt") { row, _ -> 100 + (1200 - row) * 10 }
        val written = slot<List<ActivityWriteRequest>>()
        coEvery { activityRepository.writeActivityEntries(capture(written)) } returns emptyList()

        val count = importer().import(listOf(file(GarminFileType.ACTIVITY, rideWithAscent(totalAscentMeters = 999))))

        assertEquals(1, count)
        val request = written.captured.single()
        val altitudes = request.routePoints.map { it.altitudeMeters!! }
        assertEquals(6100.0, altitudes[0], 0.05)
        assertEquals(6112.0, altitudes[1], 0.05)
        assertEquals(12.0, request.elevationGainedMeters!!, 0.05)
    }

    @Test
    fun `without a tile the watch's altitudes and ascent stand`() = runTest {
        val written = slot<List<ActivityWriteRequest>>()
        coEvery { activityRepository.writeActivityEntries(capture(written)) } returns emptyList()

        importer().import(listOf(file(GarminFileType.ACTIVITY, rideWithAscent(totalAscentMeters = 999))))

        val request = written.captured.single()
        assertEquals(listOf(10.0, 22.0), request.routePoints.map { it.altitudeMeters!! })
        assertEquals(999.0, request.elevationGainedMeters!!, 0.01)
    }

    private fun importer(): GarminActivityImporter {
        every { preferencesRepository.unitSystem } returns UnitSystem.METRIC
        every { preferencesRepository.elevationCorrectionEnabled } returns true
        every { preferencesRepository.favoriteActivityExerciseType } returns null
        every { preferencesRepository.lastActivityExerciseType } returns null
        every { activityRepository.activityWritePermissions() } returns emptySet()
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true

        val context = mockk<Context>()
        every { context.filesDir } returns temporaryFolder.root
        val tiles = ElevationTileRepository(context, DefaultDispatcherProvider)
        return GarminActivityImporter(
            activityRepository,
            preferencesRepository,
            RouteElevationCorrector(tiles, preferencesRepository),
        )
    }

    private fun rideWithAscent(totalAscentMeters: Int): ByteArray =
        FitTestFiles.activity(
            sport = 2,
            points = listOf(
                FitTestPoint(
                    time = Instant.parse("2026-05-26T08:30:00Z"),
                    latitude = 59.5000,
                    longitude = 24.5000,
                    altitudeMeters = 10.0,
                ),
                FitTestPoint(
                    time = Instant.parse("2026-05-26T08:31:00Z"),
                    latitude = 59.5010,
                    longitude = 24.5020,
                    altitudeMeters = 22.0,
                ),
            ),
            totalAscentMeters = totalAscentMeters,
        )

    private fun writeTile(name: String, value: (row: Int, col: Int) -> Int) {
        val resolution = HgtResolution.THREE_ARC_SECOND
        val n = resolution.samplesPerSide
        val buffer = ByteBuffer.allocate(resolution.byteSize.toInt()).order(ByteOrder.BIG_ENDIAN)
        for (row in 0 until n) {
            for (col in 0 until n) {
                buffer.putShort(value(row, col).toShort())
            }
        }
        val directory = File(temporaryFolder.root, "elevation_tiles").apply { mkdirs() }
        File(directory, name).writeBytes(buffer.array())
    }
}
