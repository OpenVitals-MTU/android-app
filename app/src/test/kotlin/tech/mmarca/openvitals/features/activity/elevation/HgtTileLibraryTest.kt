package tech.mmarca.openvitals.features.activity.elevation

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.core.geo.HgtResolution
import tech.mmarca.openvitals.core.geo.HgtTileKey

/** Import, replace, delete and lookup over a directory of tiles. */
class HgtTileLibraryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val key = HgtTileKey(45, 7)
    private lateinit var directory: File
    private lateinit var library: HgtTileLibrary

    @org.junit.Before
    fun setUp() {
        directory = temporaryFolder.newFolder("elevation_tiles")
        library = HgtTileLibrary(directory)
    }

    @Test fun `a raw tile is stored under its canonical name`() {
        val tile = library.importTile("n45e007.hgt", flatTile(100).stream())

        assertEquals(key, tile.key)
        assertEquals(HgtResolution.THREE_ARC_SECOND, tile.resolution)
        assertEquals("N45E007", tile.displayName)
        assertTrue(File(directory, "N45E007.hgt").isFile)
        assertEquals(listOf(key), library.scan().map { it.key })
        assertEquals(100.0, library.elevationAt(45.5, 7.5)!!, 1e-6)
    }

    @Test fun `a wrong size is refused and leaves nothing behind`() {
        val failure = runCatching {
            library.importTile("N45E007.hgt", ByteArray(1000).stream())
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(emptyList<File>(), directory.listFiles().orEmpty().toList())
    }

    @Test fun `an oversized stream is cut off before the disk fills`() {
        val oversized = ByteArray(HgtResolution.ONE_ARC_SECOND.byteSize.toInt() + 1)

        val failure = runCatching { library.importTile("N45E007.hgt", oversized.stream()) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(emptyList<File>(), directory.listFiles().orEmpty().toList())
    }

    @Test fun `a bad name is refused before anything is copied`() {
        val failure = runCatching {
            library.importTile("srtm_38_03.hgt", flatTile(100).stream())
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(emptyList<File>(), directory.listFiles().orEmpty().toList())
    }

    @Test fun `a zip with one tile is stored as the raw tile`() {
        val zip = zip("folder/N45E007.hgt" to flatTile(200))

        val tile = library.importTile("N45E007.SRTMGL1.hgt.zip", zip.stream())

        assertEquals(key, tile.key)
        assertEquals(HgtResolution.THREE_ARC_SECOND.byteSize, File(directory, "N45E007.hgt").length())
        assertEquals(200.0, library.elevationAt(45.5, 7.5)!!, 1e-6)
    }

    @Test fun `a zip named by region takes its key from the entry`() {
        val zip = zip("readme.txt" to "hello".toByteArray(), "N45E007.hgt" to flatTile(5))

        val tile = library.importTile("alps.zip", zip.stream())

        assertEquals(key, tile.key)
    }

    @Test fun `a zip with two tiles or none is refused`() {
        val two = zip("N45E007.hgt" to flatTile(1), "N45E008.hgt" to flatTile(2))
        val none = zip("readme.txt" to "hello".toByteArray())

        assertTrue(runCatching { library.importTile("two.zip", two.stream()) }.isFailure)
        assertTrue(runCatching { library.importTile("none.zip", none.stream()) }.isFailure)
        assertEquals(emptyList<File>(), directory.listFiles().orEmpty().toList())
    }

    @Test fun `a zip whose name disagrees with its entry is refused`() {
        val zip = zip("N45E008.hgt" to flatTile(1))

        val failure = runCatching { library.importTile("N45E007.hgt.zip", zip.stream()) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test fun `re-importing a cell replaces the file and the cached mapping`() {
        library.importTile("N45E007.hgt", flatTile(100).stream())
        assertEquals(100.0, library.elevationAt(45.5, 7.5)!!, 1e-6)

        library.importTile("N45E007.hgt", flatTile(300).stream())

        assertEquals(300.0, library.elevationAt(45.5, 7.5)!!, 1e-6)
        assertEquals(1, library.scan().size)
    }

    @Test fun `scan ignores stray files`() {
        File(directory, "notes.txt").writeText("x")
        File(directory, "N45E007.hgt.tmp").writeBytes(flatTile(1))
        File(directory, "N45E008.hgt").writeBytes(ByteArray(10))
        File(directory, "N45E007.hgt").writeBytes(flatTile(1))

        assertEquals(listOf(key), library.scan().map { it.key })
    }

    @Test fun `delete removes the file and forgets the mapping`() {
        library.importTile("N45E007.hgt", flatTile(100).stream())

        assertTrue(library.deleteTile(key))

        assertFalse(File(directory, "N45E007.hgt").exists())
        assertEquals(emptyList<ElevationTile>(), library.scan())
        assertNull(library.elevationAt(45.5, 7.5))
    }

    @Test fun `positions without a tile are null`() {
        library.importTile("N45E007.hgt", flatTile(100).stream())

        assertNull(library.elevationAt(46.5, 7.5))
    }

    @Test fun `a one-tile cache still serves two tiles correctly`() {
        val small = HgtTileLibrary(directory, maxOpenTiles = 1)
        small.importTile("N45E007.hgt", flatTile(100).stream())
        small.importTile("N45E008.hgt", flatTile(200).stream())

        assertEquals(100.0, small.elevationAt(45.5, 7.5)!!, 1e-6)
        assertEquals(200.0, small.elevationAt(45.5, 8.5)!!, 1e-6)
        assertEquals(100.0, small.elevationAt(45.5, 7.5)!!, 1e-6)
    }

    private fun flatTile(value: Int, resolution: HgtResolution = HgtResolution.THREE_ARC_SECOND): ByteArray {
        val n = resolution.samplesPerSide
        val buffer = ByteBuffer.allocate(resolution.byteSize.toInt()).order(ByteOrder.BIG_ENDIAN)
        repeat(n * n) { buffer.putShort(value.toShort()) }
        return buffer.array()
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun ByteArray.stream() = ByteArrayInputStream(this)
}
