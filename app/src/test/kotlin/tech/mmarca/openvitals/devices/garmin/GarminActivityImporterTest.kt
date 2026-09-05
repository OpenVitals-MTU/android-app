package tech.mmarca.openvitals.devices.garmin

import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository

/** Only ACTIVITY files are considered, and a file the parser rejects is skipped rather than sinking the sync. */
class GarminActivityImporterTest {

    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val importer = GarminActivityImporter(activityRepository, preferencesRepository)

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
        val written = importer.import(
            listOf(file(GarminFileType.SLEEP), file(GarminFileType.MONITOR)),
        )

        assertEquals(0, written)
        coVerify(exactly = 0) { activityRepository.writeActivityEntries(any()) }
        coVerify(exactly = 0) { activityRepository.writeActivityEntry(any()) }
    }

    @Test
    fun `an undecodable activity file is skipped, never thrown`() = runTest {
        // Three junk bytes are not a FIT file; the import must swallow that per file.
        val written = importer.import(listOf(file(GarminFileType.ACTIVITY)))

        assertEquals(0, written)
        coVerify(exactly = 0) { activityRepository.writeActivityEntries(any()) }
    }

    @Test
    fun `an empty download list is a no-op`() = runTest {
        assertEquals(0, importer.import(emptyList()))
    }
}
