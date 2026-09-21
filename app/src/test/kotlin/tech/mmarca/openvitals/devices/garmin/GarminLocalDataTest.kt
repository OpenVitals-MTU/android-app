package tech.mmarca.openvitals.devices.garmin

import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.data.repository.contract.GarminSleepMinuteRepository
import tech.mmarca.openvitals.data.repository.contract.GarminWellnessRepository

/** Watch data outside Health Connect used to age out only inside a sync. A removed watch never syncs. */
class GarminLocalDataTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val sleepMinutes = mockk<GarminSleepMinuteRepository>(relaxed = true)
    private val wellness = mockk<GarminWellnessRepository>(relaxed = true)
    private val now: Instant = Instant.parse("2026-09-20T12:00:00Z")

    private fun subject() = GarminLocalData(
        fileStore = GarminFileStore(resolveDirectory = { folder.root }, retention = 30.days),
        sleepMinutes = sleepMinutes,
        wellness = wellness,
    )

    private fun file(name: String, modified: Instant): File =
        File(folder.root, name).apply {
            writeText("fit")
            setLastModified(modified.toEpochMilli())
        }

    @Test
    fun `a prune with no sync drops old files and old sleep minutes`() = runTest {
        val old = file("old.fit", now.minusSeconds(31L * 24 * 3_600))
        val oldNote = file("old.fit.pending", now.minusSeconds(31L * 24 * 3_600))
        val fresh = file("fresh.fit", now.minusSeconds(3_600))

        subject().prune(now)

        assertEquals(listOf(false, false, true), listOf(old.exists(), oldNote.exists(), fresh.exists()))
        coVerify { sleepMinutes.pruneBefore(now.minusSeconds(45L * 24 * 3_600)) }
    }

    @Test
    fun `with the last watch go its file copies and sleep minutes, not the user's history`() = runTest {
        val copy = file("activity.fit", now)
        val note = file("activity.fit.pending", now)
        val other = file("keep.txt", now)

        subject().clearAfterLastWatchRemoved(deleteWellnessHistory = false)

        assertEquals(listOf(false, false), listOf(copy.exists(), note.exists()))
        assertTrue(other.exists())
        coVerify { sleepMinutes.deleteAll() }
        coVerify(exactly = 0) { wellness.deleteAll() }
    }

    @Test
    fun `the history goes too when the user asked`() = runTest {
        subject().clearAfterLastWatchRemoved(deleteWellnessHistory = true)

        coVerify { wellness.deleteAll() }
    }
}
