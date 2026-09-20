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
            deviceId = WATCH,
        )

        assertArrayEquals(byteArrayOf(9, 8, 7), File(path).readBytes())
    }

    @Test
    fun `names files by type and index not the 65535 file number`() = runTest {
        val path = store().save(
            file(index = 113),
            now = Instant.parse("2026-07-22T10:00:00Z"),
            deviceId = WATCH,
        )

        // Several files share file number 65535, so it identifies nothing.
        assertTrue(path.contains("sleep_113_"))
        assertTrue(path.endsWith(".fit"))
    }

    @Test
    fun `a re-download does not clobber the earlier copy`() = runTest {
        val first = store().save(file(), now = Instant.parse("2026-07-22T10:00:00Z"), deviceId = WATCH)
        val second = store().save(file(), now = Instant.parse("2026-07-22T11:00:00Z"), deviceId = WATCH)

        assertNotEquals(first, second)
        assertEquals(2, temp.root.listFiles()!!.count { it.isFile && it.name.endsWith(".fit") })
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

    @Test
    fun `a saved file awaits import until the import is done with it`() = runTest {
        val store = store()
        val ride = file(index = 7, type = GarminFileType.ACTIVITY, bytes = byteArrayOf(4, 5, 6))
        store.save(ride, now = Instant.parse("2026-07-22T10:00:00Z"), deviceId = WATCH)

        // A fresh store, as after a crash: the watch has archived the file and will not offer it again.
        val pending = store().pending(WATCH)

        assertEquals(1, pending.size)
        assertEquals(ride.entry, pending.single().entry)
        assertArrayEquals(byteArrayOf(4, 5, 6), pending.single().bytes)
    }

    @Test
    fun `the import clears the note and keeps the bytes`() = runTest {
        val store = store()
        val ride = file(index = 7, type = GarminFileType.ACTIVITY)
        val path = store.save(ride, now = Instant.parse("2026-07-22T10:00:00Z"), deviceId = WATCH)

        store.markImported(listOf(ride))

        assertTrue(store().pending(WATCH).isEmpty())
        assertTrue(File(path).isFile)
    }

    @Test
    fun `a replayed file is cleared too`() = runTest {
        store().save(file(index = 7), now = Instant.parse("2026-07-22T10:00:00Z"), deviceId = WATCH)
        val store = store()

        store.markImported(store.pending(WATCH))

        assertTrue(store().pending(WATCH).isEmpty())
    }

    @Test
    fun `another watch's files are not handed out`() = runTest {
        store().save(file(index = 7), now = Instant.parse("2026-07-22T10:00:00Z"), deviceId = "other-watch")

        assertTrue(store().pending(WATCH).isEmpty())
        assertEquals(1, store().pending("other-watch").size)
    }

    @Test
    fun `a note whose bytes are gone is dropped`() = runTest {
        val path = store().save(file(index = 7), now = Instant.parse("2026-07-22T10:00:00Z"), deviceId = WATCH)
        File(path).delete()

        assertTrue(store().pending(WATCH).isEmpty())
        assertFalse(File("$path.pending").exists())
    }

    @Test
    fun `a dated file keeps its date and its dedup key through the note`() = runTest {
        val dated = GarminDownloadedFile(
            entry = GarminDirectoryEntry(
                fileIndex = 12,
                type = GarminFileType.ACTIVITY,
                fileNumber = 42,
                specificFlags = 1,
                fileFlags = 2,
                fileSize = 3,
                fileDate = Instant.parse("2026-07-21T06:30:00Z"),
            ),
            bytes = byteArrayOf(1, 2, 3),
        )
        store().save(dated, now = Instant.parse("2026-07-22T10:00:00Z"), deviceId = WATCH)

        val replayed = store().pending(WATCH).single()

        // The key decides whether the next sync fetches the file again.
        assertEquals(dated.entry.dedupKey, replayed.entry.dedupKey)
        assertEquals(dated.entry, replayed.entry)
    }

    private companion object {
        const val WATCH = "watch-1"
    }
}
