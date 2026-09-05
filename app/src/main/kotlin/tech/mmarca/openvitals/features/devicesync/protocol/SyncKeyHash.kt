package tech.mmarca.openvitals.features.devicesync.protocol

import java.security.MessageDigest

/**
 * A dedup key reduced to 128 bits: a hundred thousand fingerprint strings
 * was tens of megabytes on low-RAM phones. MD5 as a mixer, not for security.
 */
internal data class SyncKeyHash(val hi: Long, val lo: Long)

/** Hashes dedup keys. Not thread-safe; the session confines it. */
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
