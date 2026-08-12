package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

/**
 * The three shapes of GPS ephemeris ("AGPS") data a Garmin watch asks for.
 *
 * Which one a watch wants is decided by its GPS chipset, not by us: the watch
 * names it in the request URL, and a file of the wrong shape is worse than no
 * file — it can leave the watch's GPS worse off than the almanac it already
 * had. So an imported file is classified by its own contents and only ever
 * served for the kind it actually is.
 */
enum class GarminAgpsKind {
    /** A tar of per-constellation CPE files, requested with `?constellations=`. */
    CONSTELLATION_TAR,

    /** A gzipped rxNetworks CPE blob, from a `/rxnetworks/` path. */
    RX_NETWORKS,

    /** A raw Sony CPE blob, from `/ephemeris/cpe/sony`. */
    SONY_CPE,
}

/** The constellations a tar can carry, by the filename Garmin uses inside it. */
enum class GarminAgpsConstellation(val fileName: String) {
    GPS("CPE_GPS.BIN"),
    GLONASS("CPE_GLO.BIN"),
    GALILEO("CPE_GAL.BIN"),
    QZSS("CPE_QZSS.BIN"),
}

/**
 * Reads and sanity-checks ephemeris data the user supplied.
 *
 * Ported from Gadgetbridge's `GarminAgpsFile` (AGPLv3), including its seven-day
 * freshness rule for rxNetworks: ephemeris is a prediction of where the
 * satellites will be, and a stale prediction is not a smaller benefit but an
 * actively wrong one.
 */
object GarminAgpsFile {

    private val GZ_HEADER = byteArrayOf(0x1f, 0x8b.toByte())
    private val RX_NETWORKS_HEADER = byteArrayOf(0x01, 0x00)
    private val SONY_CPE_HEADER = byteArrayOf(0x2a, 0x12, 0xa0.toByte(), 0x02)

    /** rxNetworks predictions are only good for a week. */
    private const val RX_NETWORKS_MAX_AGE_SECONDS = 604_800L

    /** What this file is, or null if it is not ephemeris data at all. */
    fun classify(bytes: ByteArray): GarminAgpsKind? = when {
        bytes.startsWith(SONY_CPE_HEADER) -> GarminAgpsKind.SONY_CPE
        bytes.startsWith(GZ_HEADER) -> GarminAgpsKind.RX_NETWORKS
        GarminTar.isTar(bytes) -> GarminAgpsKind.CONSTELLATION_TAR
        else -> null
    }

    /**
     * Whether [bytes] is usable for [kind] right now. Separate from
     * [classify] because freshness expires: a file that classified fine at
     * import can be too old to serve by the time the watch asks.
     */
    fun isValid(
        bytes: ByteArray,
        kind: GarminAgpsKind,
        constellations: List<String> = emptyList(),
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L,
    ): Boolean = when (kind) {
        GarminAgpsKind.SONY_CPE -> bytes.startsWith(SONY_CPE_HEADER)
        GarminAgpsKind.RX_NETWORKS -> isValidRxNetworks(bytes, nowEpochSeconds)
        GarminAgpsKind.CONSTELLATION_TAR -> isValidTar(bytes, constellations)
    }

    /** Every constellation the watch asked for has to actually be in there. */
    private fun isValidTar(bytes: ByteArray, constellations: List<String>): Boolean {
        if (!GarminTar.isTar(bytes)) return false
        val names = GarminTar.fileNames(bytes)
        for (constellation in constellations) {
            val known = GarminAgpsConstellation.entries
                .firstOrNull { it.name.equals(constellation, ignoreCase = true) }
            if (known == null) {
                GarminLog.log("[GARMIN-AGPS] watch asked for unknown constellation $constellation")
                return false
            }
            if (known.fileName !in names) {
                GarminLog.log("[GARMIN-AGPS] file is missing ${known.fileName}")
                return false
            }
        }
        return true
    }

    private fun isValidRxNetworks(bytes: ByteArray, nowEpochSeconds: Long): Boolean {
        if (!bytes.startsWith(GZ_HEADER)) return false
        return runCatching {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
                val header = ByteArray(RX_NETWORKS_HEADER.size)
                if (input.read(header) != header.size || !header.contentEquals(RX_NETWORKS_HEADER)) {
                    GarminLog.log("[GARMIN-AGPS] gzip contents are not rxNetworks ephemeris")
                    return false
                }
                val stamp = ByteArray(4)
                if (input.read(stamp) != stamp.size) return false
                val generatedAt =
                    ByteBuffer.wrap(stamp).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL
                val age = nowEpochSeconds - generatedAt
                when {
                    age < 0 -> {
                        GarminLog.log("[GARMIN-AGPS] ephemeris is dated in the future")
                        false
                    }
                    age > RX_NETWORKS_MAX_AGE_SECONDS -> {
                        GarminLog.log("[GARMIN-AGPS] ephemeris is ${age / 86_400} days old")
                        false
                    }
                    else -> true
                }
            }
        }.getOrElse {
            GarminLog.log("[GARMIN-AGPS] could not read the file as gzip: ${it.message}")
            false
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }
}

/**
 * Just enough tar to check what is inside one. The archives here are a handful
 * of small binaries, so this walks the headers rather than pulling in an
 * archive library for four filenames.
 */
internal object GarminTar {

    private const val BLOCK = 512
    private const val MAGIC_OFFSET = 257
    private const val NAME_LENGTH = 100
    private const val SIZE_OFFSET = 124
    private const val SIZE_LENGTH = 12
    private val MAGIC = "ustar".toByteArray(Charsets.US_ASCII)

    fun isTar(bytes: ByteArray): Boolean {
        if (bytes.size < MAGIC_OFFSET + MAGIC.size) return false
        return MAGIC.indices.all { bytes[MAGIC_OFFSET + it] == MAGIC[it] }
    }

    /** The names of the files in the archive, ignoring anything malformed. */
    fun fileNames(bytes: ByteArray): Set<String> {
        val names = mutableSetOf<String>()
        var offset = 0
        while (offset + BLOCK <= bytes.size) {
            val name = bytes.copyOfRange(offset, offset + NAME_LENGTH)
                .takeWhile { it != 0.toByte() }
                .toByteArray()
                .toString(Charsets.US_ASCII)
            // Two zeroed blocks mark the end of the archive; one empty name is
            // enough to stop, since nothing useful follows it.
            if (name.isEmpty()) break
            names.add(name)
            val size = bytes.copyOfRange(offset + SIZE_OFFSET, offset + SIZE_OFFSET + SIZE_LENGTH)
                .toString(Charsets.US_ASCII)
                // The octal size field is padded with NULs or spaces.
                .trim { it <= ' ' }
                .toLongOrNull(radix = 8) ?: break
            // Header block, then the contents rounded up to a whole block.
            offset += BLOCK + ((size + BLOCK - 1) / BLOCK * BLOCK).toInt()
        }
        return names
    }
}
