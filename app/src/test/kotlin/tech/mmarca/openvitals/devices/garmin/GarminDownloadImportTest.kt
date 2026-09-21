package tech.mmarca.openvitals.devices.garmin

import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.devices.garmin.wellness.FitWellnessImporter

/**
 * The watch archives a file once it is downloaded. A key recorded for a file that never
 * reached the app hides it for good, so the key is written last.
 */
class GarminDownloadImportTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val importer = mockk<FitWellnessImporter>()
    private val activityImporter = mockk<GarminActivityImporter>()
    private val stateStore = mockk<GarminDeviceStateStore>(relaxed = true)
    private val fileStore by lazy { GarminFileStore(resolveDirectory = { temp.root }) }

    private fun subject() = GarminDownloadImport(importer, activityImporter, stateStore, fileStore)

    private fun file(index: Int) = GarminDownloadedFile(
        entry = GarminDirectoryEntry(
            fileIndex = index,
            type = GarminFileType.ACTIVITY,
            fileNumber = index,
            specificFlags = 0,
            fileFlags = 0,
            fileSize = 3,
            fileDate = Instant.parse("2026-07-22T10:00:00Z"),
        ),
        bytes = byteArrayOf(1, 2, 3),
    )

    @Test
    fun `keys are recorded after both imports`() = runTest {
        val files = listOf(file(1), file(2))
        coEvery { importer.import(files) } returns Unit
        coEvery { activityImporter.import(files) } returns GarminActivityImportResult(written = 2)

        subject().import(WATCH, files)

        coVerifyOrder {
            importer.import(files)
            activityImporter.import(files)
            stateStore.recordSyncedFileKeys(WATCH, files.map { it.entry.dedupKey!! })
        }
    }

    @Test
    fun `a failed import records nothing and leaves the saved copies pending`() = runTest {
        val files = listOf(file(1))
        fileStore.save(files.single(), now = Instant.parse("2026-07-22T10:05:00Z"), deviceId = WATCH)
        coEvery { importer.import(files) } throws IllegalStateException("Health Connect is down")

        val failure = runCatching { subject().import(WATCH, files) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)

        verify(exactly = 0) { stateStore.recordSyncedFileKeys(any(), any()) }
        assertEquals(1, fileStore.pending(WATCH).size)
    }

    @Test
    fun `a workout that did not reach Health Connect keeps no key`() = runTest {
        val landed = file(1)
        val refused = file(2)
        val files = listOf(landed, refused)
        coEvery { importer.import(files) } returns Unit
        coEvery { activityImporter.import(files) } returns
            GarminActivityImportResult(written = 1, missingPermission = listOf(refused))

        val result = subject().import(WATCH, files)

        verify { stateStore.recordSyncedFileKeys(WATCH, listOf(landed.entry.dedupKey!!)) }
        assertEquals(listOf(refused), result.retry)
    }

    @Test
    fun `an imported file stops being pending`() = runTest {
        val files = listOf(file(1))
        fileStore.save(files.single(), now = Instant.parse("2026-07-22T10:05:00Z"), deviceId = WATCH)
        coEvery { importer.import(files) } returns Unit
        coEvery { activityImporter.import(files) } returns GarminActivityImportResult(written = 1)
        every { stateStore.recordSyncedFileKeys(any(), any()) } returns Unit

        subject().import(WATCH, files)

        assertEquals(0, fileStore.pending(WATCH).size)
    }

    private companion object {
        const val WATCH = "watch-1"
    }
}
