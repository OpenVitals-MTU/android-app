package tech.mmarca.openvitals.core.geo

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Pins the SRTM layout: row 0 is north, both edges are included, samples are big-endian. */
class HgtTileTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val key = HgtTileKey(45, 7)

    @Test fun `key parses the common file name shapes`() {
        assertEquals(HgtTileKey(45, 7), HgtTileKey.parse("N45E007.hgt"))
        assertEquals(HgtTileKey(45, 7), HgtTileKey.parse("n45e007.HGT"))
        assertEquals(HgtTileKey(-33, -70), HgtTileKey.parse("S33W070.hgt"))
        assertEquals(HgtTileKey(45, 7), HgtTileKey.parse("N45E007.SRTMGL1.hgt"))
        assertEquals(HgtTileKey(45, 7), HgtTileKey.parse("N45E007.hgt.zip"))
        assertEquals(HgtTileKey(0, 0), HgtTileKey.parse("N00E000"))
    }

    @Test fun `key rejects other names and out of range cells`() {
        assertNull(HgtTileKey.parse("srtm_38_03.zip"))
        assertNull(HgtTileKey.parse("N95E007.hgt"))
        assertNull(HgtTileKey.parse("N45E181.hgt"))
        assertNull(HgtTileKey.parse("N45E007x.hgt"))
        assertNull(HgtTileKey.parse(""))
    }

    @Test fun `file name round-trips with zero padding`() {
        assertEquals("N45E007.hgt", HgtTileKey(45, 7).fileName)
        assertEquals("S33W070.hgt", HgtTileKey(-33, -70).fileName)
        assertEquals("S01W001.hgt", HgtTileKey(-1, -1).fileName)
        assertEquals(HgtTileKey(-33, -70), HgtTileKey.parse(HgtTileKey(-33, -70).fileName))
    }

    @Test fun `position maps to the south-west tile`() {
        assertEquals(HgtTileKey(45, 7), HgtTileKey.forPosition(45.5, 7.5))
        assertEquals(HgtTileKey(-34, -71), HgtTileKey.forPosition(-33.5, -70.5))
        assertEquals(HgtTileKey(46, 8), HgtTileKey.forPosition(46.0, 8.0))
    }

    @Test fun `resolution comes from the byte size alone`() {
        assertEquals(HgtResolution.THREE_ARC_SECOND, HgtResolution.forByteSize(2_884_802L))
        assertEquals(HgtResolution.ONE_ARC_SECOND, HgtResolution.forByteSize(25_934_402L))
        assertNull(HgtResolution.forByteSize(2_884_803L))
        assertNull(HgtResolution.forByteSize(0L))
    }

    @Test fun `a planar gradient is exact at nodes and averaged between them`() {
        for (resolution in HgtResolution.entries) {
            val tile = gradientTile(resolution)
            val n = resolution.samplesPerSide
            val step = 1.0 / (n - 1)

            // North-west corner is row 0, col 0.
            assertEquals(0.0, tile.elevationAt(46.0, 7.0)!!, 1e-6)
            // One node east: col 1.
            assertEquals(2.0, tile.elevationAt(46.0, 7.0 + step)!!, 1e-6)
            // One node south: row 1.
            assertEquals(1.0, tile.elevationAt(46.0 - step, 7.0)!!, 1e-6)
            // The middle of the first cell averages its four corners.
            assertEquals(1.5, tile.elevationAt(46.0 - step / 2, 7.0 + step / 2)!!, 1e-6)
        }
    }

    @Test fun `south and east edges read the last sample instead of falling off`() {
        val tile = gradientTile(HgtResolution.THREE_ARC_SECOND)
        val last = 1200.0

        assertEquals(last, tile.elevationAt(45.0, 7.0)!!, 1e-6)
        assertEquals(2 * last, tile.elevationAt(46.0, 8.0)!!, 1e-6)
        assertEquals(3 * last, tile.elevationAt(45.0, 8.0)!!, 1e-6)
    }

    @Test fun `positions outside the tile are null`() {
        val tile = gradientTile(HgtResolution.THREE_ARC_SECOND)

        assertNull(tile.elevationAt(44.999, 7.5))
        assertNull(tile.elevationAt(46.001, 7.5))
        assertNull(tile.elevationAt(45.5, 6.999))
        assertNull(tile.elevationAt(45.5, 8.001))
        assertNull(tile.elevationAt(Double.NaN, 7.5))
    }

    @Test fun `a void in any of the four neighbours makes the result null`() {
        val n = HgtResolution.THREE_ARC_SECOND.samplesPerSide
        val step = 1.0 / (n - 1)
        val tile = tile(HgtResolution.THREE_ARC_SECOND) { row, col ->
            if (row == 1 && col == 1) HgtTile.VoidValue else 100
        }

        // The first cell has the void at its south-east corner.
        assertNull(tile.elevationAt(46.0 - step / 2, 7.0 + step / 2))
        // Exactly on the void node.
        assertNull(tile.elevationAt(46.0 - step, 7.0 + step))
        // Two cells away the void plays no part.
        assertEquals(100.0, tile.elevationAt(46.0 - 2.5 * step, 7.0 + 2.5 * step)!!, 1e-6)
        assertNull(tile.sampleAt(1, 1))
        assertEquals(100, tile.sampleAt(0, 0))
    }

    @Test fun `a mapped file reads the same numbers as the buffer`() {
        val resolution = HgtResolution.THREE_ARC_SECOND
        val buffer = gradientBuffer(resolution)
        val file = temporaryFolder.newFile(key.fileName)
        file.writeBytes(buffer.array())

        val mapped = HgtTile.open(key, file)

        assertEquals(resolution, mapped.resolution)
        assertEquals(1.5, mapped.elevationAt(46.0 - 0.5 / 1200, 7.0 + 0.5 / 1200)!!, 1e-6)
        assertEquals(3 * 1200.0, mapped.elevationAt(45.0, 8.0)!!, 1e-6)
        assertNotNull(mapped.sampleAt(1200, 1200))
    }

    @Test fun `a file of the wrong size is refused`() {
        val file = temporaryFolder.newFile(key.fileName)
        file.writeBytes(ByteArray(10))

        val failure = runCatching { HgtTile.open(key, file) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    private fun gradientTile(resolution: HgtResolution): HgtTile =
        HgtTile.fromBuffer(key, gradientBuffer(resolution))

    /** z = row + 2 * col: a plane, so bilinear interpolation is exact everywhere. */
    private fun gradientBuffer(resolution: HgtResolution): ByteBuffer =
        buffer(resolution) { row, col -> row + 2 * col }

    private fun tile(resolution: HgtResolution, value: (row: Int, col: Int) -> Int): HgtTile =
        HgtTile.fromBuffer(key, buffer(resolution, value))

    private fun buffer(resolution: HgtResolution, value: (row: Int, col: Int) -> Int): ByteBuffer {
        val n = resolution.samplesPerSide
        val buffer = ByteBuffer.allocate(resolution.byteSize.toInt()).order(ByteOrder.BIG_ENDIAN)
        for (row in 0 until n) {
            for (col in 0 until n) {
                buffer.putShort(value(row, col).toShort())
            }
        }
        buffer.flip()
        return buffer
    }
}
