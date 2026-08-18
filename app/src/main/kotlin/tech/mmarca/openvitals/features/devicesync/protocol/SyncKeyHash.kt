package tech.mmarca.openvitals.features.devicesync.protocol

import java.security.MessageDigest

/**
 * A dedup key reduced to 128 bits.
 *
 * The session's dedup baseline holds one entry for EVERY local record in the
 * sync window — a data-dense year is easily a hundred thousand keys, and as
 * fingerprint strings that was tens of megabytes on exactly the low-RAM phones
 * the phone-to-phone sync targets. Two longs per key is the same information
 * at a fraction of the footprint.
 *
 * MD5 is used as a 128-bit mixer, not for security: dedup only needs equal
 * keys to collapse to equal hashes and unequal keys to collide never-in-
 * practice. The peer is authenticated before any of its keys are hashed.
 */
internal data class SyncKeyHash(val hi: Long, val lo: Long)

/**
 * Hashes dedup keys. NOT thread-safe (one shared [MessageDigest]) — the
 * session confines it: the baseline seeding completes before the receiver
 * loop starts, and the receiver is the only other caller.
 */
internal class SyncKeyHasher {
    private val digest = MessageDigest.getInstance("MD5")

    fun hash(key: String): SyncKeyHash {
        val bytes = digest.digest(key.toByteArray(Charsets.UTF_8))
        return SyncKeyHash(hi = bytes.longAt(0), lo = bytes.longAt(8))
    }
}

private fun ByteArray.longAt(offset: Int): Long {
    var value = 0L
    for (index in offset until offset + 8) {
        value = (value shl 8) or (this[index].toLong() and 0xFF)
    }
    return value
}
