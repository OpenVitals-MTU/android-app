package tech.mmarca.openvitals.features.activity.elevation

import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream
import tech.mmarca.openvitals.core.geo.HgtResolution
import tech.mmarca.openvitals.core.geo.HgtTile
import tech.mmarca.openvitals.core.geo.HgtTileKey

/**
 * The SRTM tiles stored in one directory, plus a small cache of open
 * mappings. Pure JVM: the repository adds Android on top.
 *
 * There is no metadata file. The name is the key, the size is the
 * resolution and the modification time is the import time, so a directory
 * scan is the whole truth and a half-written `.tmp` is simply not listed.
 */
class HgtTileLibrary(
    private val directory: File,
    private val maxOpenTiles: Int = DefaultMaxOpenTiles,
) {
    private val lock = Any()
    private val open = object : LinkedHashMap<HgtTileKey, HgtTile>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<HgtTileKey, HgtTile>): Boolean =
            size > maxOpenTiles
    }

    /** The last tile hit, so a route of thousands of points does one lookup per tile crossing. */
    @Volatile
    private var last: HgtTile? = null

    /** Every valid tile in the directory, newest import first. */
    fun scan(): List<ElevationTile> =
        directory.listFiles()
            .orEmpty()
            .mapNotNull { file -> file.toElevationTile() }
            .sortedByDescending { it.importedAtMillis }

    fun fileFor(key: HgtTileKey): File = File(directory, key.fileName)

    /** Bilinear elevation in meters, or null when no imported tile covers the position. */
    fun elevationAt(latitude: Double, longitude: Double): Double? {
        val key = HgtTileKey.forPosition(latitude, longitude)
        val tile = last?.takeIf { it.key == key } ?: tileFor(key) ?: return null
        last = tile
        return tile.elevationAt(latitude, longitude)
    }

    /** The open tile for a cell, or null when its file is absent or malformed. */
    fun tileFor(key: HgtTileKey): HgtTile? = synchronized(lock) {
        open[key]?.let { return it }
        val file = fileFor(key)
        if (!file.isFile) return null
        val tile = runCatching { HgtTile.open(key, file) }.getOrNull() ?: return null
        open[key] = tile
        tile
    }

    /** Drops a cached mapping so a replaced or deleted file is not read through the old buffer. */
    fun evict(key: HgtTileKey) {
        synchronized(lock) {
            open.remove(key)
            if (last?.key == key) last = null
        }
    }

    /**
     * Copies one tile into the directory: a raw `.hgt`, or a zip holding
     * exactly one. The key comes from [displayName], else from the zip entry.
     * Replaces an existing tile of the same key.
     */
    @Throws(IOException::class)
    fun importTile(displayName: String, input: InputStream): ElevationTile {
        directory.mkdirs()
        val stream = BufferedInputStream(input, PeekBytes)
        val nameKey = HgtTileKey.parse(displayName)
        val isZip = displayName.endsWith(".zip", ignoreCase = true) || stream.looksLikeZip()

        val key: HgtTileKey
        val source: InputStream
        var zip: ZipInputStream? = null
        if (isZip) {
            zip = ZipInputStream(stream)
            val entry = zip.firstHgtEntry()
            val entryKey = HgtTileKey.parse(File(entry).name)
            require(entryKey != null) { "The zip entry $entry is not named like an SRTM tile (N45E007.hgt)." }
            require(nameKey == null || nameKey == entryKey) {
                "The zip is named $displayName but holds $entry."
            }
            key = entryKey
            source = zip
        } else {
            require(nameKey != null) { "$displayName is not named like an SRTM tile (N45E007.hgt)." }
            key = nameKey
            source = stream
        }

        val finalFile = fileFor(key)
        val tempFile = File(directory, "${key.fileName}.tmp")
        try {
            tempFile.delete()
            val copied = copyBounded(source, tempFile)
            require(HgtResolution.forByteSize(copied) != null) {
                "This is not a 1 or 3 arc-second SRTM .hgt tile."
            }
            // A zip is sequential: only after the copy can the rest be walked.
            require(zip == null || !zip.hasAnotherHgtEntry()) {
                "The zip holds more than one .hgt tile; import them one at a time."
            }
            evict(key)
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }
        } catch (error: Throwable) {
            tempFile.delete()
            throw error
        }
        return finalFile.toElevationTile()
            ?: throw IOException("${key.fileName} could not be read back after import.")
    }

    fun deleteTile(key: HgtTileKey): Boolean {
        evict(key)
        return fileFor(key).delete()
    }

    private fun File.toElevationTile(): ElevationTile? {
        if (!isFile) return null
        val key = HgtTileKey.parse(name) ?: return null
        if (name != key.fileName) return null
        val resolution = HgtResolution.forByteSize(length()) ?: return null
        return ElevationTile(
            key = key,
            resolution = resolution,
            sizeBytes = length(),
            importedAtMillis = lastModified(),
            path = absolutePath,
        )
    }

    /** Positions the stream on the first `.hgt` entry and returns its name, or fails. */
    private fun ZipInputStream.firstHgtEntry(): String =
        nextHgtEntryName() ?: throw IllegalArgumentException("The zip holds no .hgt tile.")

    private fun ZipInputStream.hasAnotherHgtEntry(): Boolean =
        nextHgtEntryName() != null

    /** Skips directories and other files. Null at the end of the archive. */
    private fun ZipInputStream.nextHgtEntryName(): String? {
        while (true) {
            val entry = nextEntry ?: return null
            if (entry.isDirectory) continue
            if (entry.name.endsWith(HgtTileKey.FileExtension, ignoreCase = true)) return entry.name
        }
    }

    private fun copyBounded(input: InputStream, destination: File): Long {
        val limit = HgtResolution.ONE_ARC_SECOND.byteSize
        var total = 0L
        destination.outputStream().use { output ->
            val buffer = ByteArray(CopyBufferSize)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= limit) { "This is not a 1 or 3 arc-second SRTM .hgt tile." }
                output.write(buffer, 0, read)
            }
        }
        return total
    }

    private fun BufferedInputStream.looksLikeZip(): Boolean {
        mark(PeekBytes)
        val first = read()
        val second = read()
        reset()
        return first == 'P'.code && second == 'K'.code
    }

    private companion object {
        const val DefaultMaxOpenTiles = 4
        const val CopyBufferSize = 128 * 1024
        const val PeekBytes = 16
    }
}
