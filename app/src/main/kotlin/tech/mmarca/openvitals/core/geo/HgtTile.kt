package tech.mmarca.openvitals.core.geo

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.floor

/**
 * The one-degree cell an SRTM `.hgt` tile covers, named by its south-west
 * corner: N45E007 spans 45..46 N and 7..8 E.
 */
data class HgtTileKey(val latitude: Int, val longitude: Int) {

    /** The canonical file name, e.g. `N45E007.hgt` or `S33W070.hgt`. */
    val fileName: String
        get() = buildString {
            append(if (latitude < 0) 'S' else 'N')
            append(abs(latitude).toString().padStart(2, '0'))
            append(if (longitude < 0) 'W' else 'E')
            append(abs(longitude).toString().padStart(3, '0'))
            append(FileExtension)
        }

    companion object {
        const val FileExtension = ".hgt"

        /** The tile that holds a position. A point on a shared edge goes to the south-west tile. */
        fun forPosition(latitude: Double, longitude: Double): HgtTileKey =
            HgtTileKey(floor(latitude).toInt(), floor(longitude).toInt())

        /**
         * Reads the leading `N45E007` from names like `N45E007.hgt`,
         * `n45e007.hgt`, `N45E007.SRTMGL1.hgt` or `N45E007.hgt.zip`.
         * Null when the name has no such prefix or it is out of range.
         */
        fun parse(fileName: String): HgtTileKey? {
            val match = NamePattern.find(fileName.trim()) ?: return null
            val (latSign, latDigits, lonSign, lonDigits) = match.destructured
            val latitude = latDigits.toInt().let { if (latSign.equals("S", ignoreCase = true)) -it else it }
            val longitude = lonDigits.toInt().let { if (lonSign.equals("W", ignoreCase = true)) -it else it }
            if (latitude !in -90..89 || longitude !in -180..179) return null
            return HgtTileKey(latitude, longitude)
        }

        private val NamePattern = Regex("""^([NS])(\d{2})([EW])(\d{3})(?=\.|$)""", RegexOption.IGNORE_CASE)
    }
}

/** The two SRTM sample grids. A tile's size on disk tells them apart. */
enum class HgtResolution(val samplesPerSide: Int, val arcSeconds: Int) {
    ONE_ARC_SECOND(3601, 1),
    THREE_ARC_SECOND(1201, 3),
    ;

    val byteSize: Long
        get() = samplesPerSide.toLong() * samplesPerSide * 2L

    companion object {
        fun forByteSize(size: Long): HgtResolution? =
            entries.firstOrNull { it.byteSize == size }
    }
}

/**
 * One SRTM `.hgt` tile: big-endian signed 16-bit samples, row 0 on the
 * north edge, both edges included. Reads are absolute, so one instance is
 * safe to share between threads.
 */
class HgtTile private constructor(
    val key: HgtTileKey,
    val resolution: HgtResolution,
    private val samples: ByteBuffer,
) {

    /** Bilinear elevation in meters. Null outside the tile or next to a void sample. */
    fun elevationAt(latitude: Double, longitude: Double): Double? {
        val n = resolution.samplesPerSide
        val x = (longitude - key.longitude) * (n - 1)
        val y = (key.latitude + 1 - latitude) * (n - 1)
        if (x < 0.0 || y < 0.0 || x > n - 1.0 || y > n - 1.0) return null
        if (x.isNaN() || y.isNaN()) return null

        // Clamp the base cell so a point on the south or east edge reads the
        // last cell at fraction 1.0 instead of indexing off the tile.
        val col0 = floor(x).toInt().coerceAtMost(n - 2)
        val row0 = floor(y).toInt().coerceAtMost(n - 2)
        val fx = x - col0
        val fy = y - row0

        val z00 = sampleAt(row0, col0) ?: return null
        val z01 = sampleAt(row0, col0 + 1) ?: return null
        val z10 = sampleAt(row0 + 1, col0) ?: return null
        val z11 = sampleAt(row0 + 1, col0 + 1) ?: return null

        val north = z00 * (1.0 - fx) + z01 * fx
        val south = z10 * (1.0 - fx) + z11 * fx
        return north * (1.0 - fy) + south * fy
    }

    /** The raw sample at a grid node, or null for the void marker. */
    fun sampleAt(row: Int, col: Int): Int? {
        val n = resolution.samplesPerSide
        require(row in 0 until n && col in 0 until n) { "Sample ($row, $col) is outside a $n x $n tile." }
        val value = samples.getShort((row * n + col) * 2).toInt()
        return value.takeIf { it != VoidValue }
    }

    companion object {
        const val VoidValue = -32768

        /** Wraps an in-memory tile. The buffer must be exactly one valid tile size. */
        fun fromBuffer(key: HgtTileKey, buffer: ByteBuffer): HgtTile {
            val resolution = HgtResolution.forByteSize(buffer.limit().toLong())
            require(resolution != null) { "${key.fileName}: ${buffer.limit()} bytes is not an SRTM tile." }
            return HgtTile(key, resolution, buffer.order(ByteOrder.BIG_ENDIAN))
        }

        /** Memory-maps a tile read-only. The mapping outlives the closed channel. */
        fun open(key: HgtTileKey, file: File): HgtTile {
            val resolution = HgtResolution.forByteSize(file.length())
            require(resolution != null) { "${file.name}: ${file.length()} bytes is not an SRTM tile." }
            val mapped = RandomAccessFile(file, "r").use { raf ->
                raf.channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, 0L, resolution.byteSize)
                }
            }
            return HgtTile(key, resolution, mapped.order(ByteOrder.BIG_ENDIAN))
        }
    }
}
