package tech.mmarca.openvitals.devices.garmin

import java.io.File
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Port of the Flutter build's `garmin_file_store_test.dart` — fixtures identical. */
class GarminFileStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun store(directory: () -> File = { temp.root }) =
        GarminFileStore(resolveDirectory = { directory() })

    private fun file(
        index: Int = 5,
        type: GarminFileType = GarminFileType.SLEEP,
        bytes: ByteArray = byteArrayOf(1, 2, 3),
    ) = GarminDownloadedFile(
        entry = GarminDirectoryEntry(
            fileIndex = index,
            type = type,
            fileNumber = 0xFFFF,
            specificFlags = 0,
            fileFlags = 0,
            fileSize = bytes.size.toLong(),
            fileDate = null,
        ),
        bytes = bytes,
    )

    @Test
    fun `writes the raw bytes creating the directory`() = runTest {
        val nested = File(temp.root, "sub")
        val nestedStore = store { nested }

        val path = nestedStore.save(
            file(bytes = byteArrayOf(9, 8, 7)),
            now = Instant.parse("2026-07-22T10:00:00Z"),
        )

        assertArrayEquals(byteArrayOf(9, 8, 7), File(path).readBytes())
    }

    @Test
    fun `names files by type and index not the 65535 file number`() = runTest {
        val path = store().save(
            file(index = 113),
            now = Instant.parse("2026-07-22T10:00:00Z"),
        )

        // Several files share file number 65535, so it identifies nothing.
        assertTrue(path.contains("sleep_113_"))
        assertTrue(path.endsWith(".fit"))
    }

    @Test
    fun `a re-download does not clobber the earlier copy`() = runTest {
        val first = store().save(file(), now = Instant.parse("2026-07-22T10:00:00Z"))
        val second = store().save(file(), now = Instant.parse("2026-07-22T11:00:00Z"))

        assertNotEquals(first, second)
        assertEquals(2, temp.root.listFiles()!!.count { it.isFile })
    }

    @Test
    fun `prune removes files past the retention window keeping recent ones`() = runTest {
        val old = File(temp.root, "old.fit").apply { writeBytes(byteArrayOf(1)) }
        val recent = File(temp.root, "recent.fit").apply { writeBytes(byteArrayOf(1)) }
        val now = Instant.parse("2026-07-22T12:00:00Z")
        old.setLastModified(now.minusSeconds(60L * 24 * 3600).toEpochMilli())
        recent.setLastModified(now.minusSeconds(2L * 24 * 3600).toEpochMilli())

        store().prune(now = now)

        assertFalse(old.exists())
        assertTrue(recent.exists())
    }

    @Test
    fun `prune leaves non-FIT files alone`() = runTest {
        val now = Instant.parse("2026-07-22T12:00:00Z")
        val other = File(temp.root, "notes.txt").apply { writeBytes(byteArrayOf(1)) }
        other.setLastModified(now.minusSeconds(60L * 24 * 3600).toEpochMilli())

        store().prune(now = now)

        assertTrue(other.exists())
    }

    @Test
    fun `prune on a directory that does not exist is a no-op`() = runTest {
        val missing = store { File(temp.root, "nope") }
        // Completes without throwing.
        missing.prune(now = Instant.parse("2026-07-22T12:00:00Z"))
    }
}
