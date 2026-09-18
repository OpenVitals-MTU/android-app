package tech.mmarca.openvitals.healthconnect

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Origin package per synced record, packed. A sync id is `sync_` plus 32
 * hex digits, so it fits in two longs; a package name repeats across
 * records, so it is stored once and referenced by a short. About 36 bytes
 * a row against about 200 for a `HashMap<String, String>`, which ran the
 * app out of memory at a million rows.
 *
 * Open addressing over primitive arrays. Readers and the one writer share
 * a read-write lock; a lookup is a few array reads.
 */
internal class SyncedOriginIndex(expectedRows: Int = 0) {
    private val lock = ReentrantReadWriteLock()
    private var high = LongArray(0)
    private var low = LongArray(0)
    /** 0 marks an empty slot; otherwise the package's position in [packages] plus one. */
    private var packageRef = ShortArray(0)
    private var packed = 0

    private val packages = ArrayList<String>()
    private val packageRefs = HashMap<String, Short>()

    /** Ids that are not `sync_<32 hex>`. None in practice; kept so no origin is ever dropped. */
    private val unpacked = HashMap<String, String>()

    init {
        allocate(capacityFor(expectedRows))
    }

    val size: Int get() = lock.read { packed + unpacked.size }

    operator fun get(clientRecordId: String): String? = lock.read {
        val key = SyncIdKey.parse(clientRecordId) ?: return unpacked[clientRecordId]
        val slot = find(key.high, key.low)
        val ref = packageRef[slot].toInt()
        if (ref == 0) null else packages[ref - 1]
    }

    fun putAll(origins: Map<String, String>) = lock.write {
        origins.forEach { (clientRecordId, originPackage) -> putLocked(clientRecordId, originPackage) }
    }

    fun put(clientRecordId: String, originPackage: String) = lock.write {
        putLocked(clientRecordId, originPackage)
    }

    private fun putLocked(clientRecordId: String, originPackage: String) {
        val key = SyncIdKey.parse(clientRecordId)
        val ref = if (key == null) null else refFor(originPackage)
        if (key == null || ref == null) {
            unpacked[clientRecordId] = originPackage
            return
        }
        if ((packed + 1) * LoadDenominator > high.size * LoadNumerator) grow()
        val slot = find(key.high, key.low)
        if (packageRef[slot].toInt() == 0) {
            high[slot] = key.high
            low[slot] = key.low
            packed++
        }
        packageRef[slot] = ref
    }

    /** The short for [originPackage], or null once the short range is used up. */
    private fun refFor(originPackage: String): Short? {
        packageRefs[originPackage]?.let { return it }
        if (packages.size >= Short.MAX_VALUE) return null
        packages += originPackage
        val ref = packages.size.toShort()
        packageRefs[originPackage] = ref
        return ref
    }

    /** The slot holding the key, or the empty slot where it belongs. */
    private fun find(keyHigh: Long, keyLow: Long): Int {
        val mask = high.size - 1
        var slot = mix(keyHigh, keyLow) and mask
        while (packageRef[slot].toInt() != 0 && (high[slot] != keyHigh || low[slot] != keyLow)) {
            slot = (slot + 1) and mask
        }
        return slot
    }

    private fun grow() {
        val oldHigh = high
        val oldLow = low
        val oldRef = packageRef
        allocate(oldHigh.size * 2)
        for (index in oldHigh.indices) {
            if (oldRef[index].toInt() == 0) continue
            val slot = find(oldHigh[index], oldLow[index])
            high[slot] = oldHigh[index]
            low[slot] = oldLow[index]
            packageRef[slot] = oldRef[index]
        }
    }

    private fun allocate(capacity: Int) {
        high = LongArray(capacity)
        low = LongArray(capacity)
        packageRef = ShortArray(capacity)
    }

    // The id is already a hash, so folding the two halves spreads well.
    private fun mix(keyHigh: Long, keyLow: Long): Int {
        val folded = keyHigh xor keyLow
        return (folded xor (folded ushr 32)).toInt()
    }

    private companion object {
        const val MinCapacity = 16
        const val LoadNumerator = 3
        const val LoadDenominator = 5

        /** The smallest power of two that holds [rows] under the load limit. */
        fun capacityFor(rows: Int): Int {
            val needed = rows.toLong() * LoadDenominator / LoadNumerator + 1
            var capacity = MinCapacity
            while (capacity < needed) capacity = capacity shl 1
            return capacity
        }
    }
}

/** `sync_` plus 32 lowercase hex digits, as two longs. Anything else does not pack. */
internal class SyncIdKey private constructor(val high: Long, val low: Long) {
    companion object {
        private const val Prefix = "sync_"
        private const val HexDigits = 32

        fun parse(clientRecordId: String): SyncIdKey? {
            if (clientRecordId.length != Prefix.length + HexDigits || !clientRecordId.startsWith(Prefix)) return null
            val high = hexToLong(clientRecordId, Prefix.length) ?: return null
            val low = hexToLong(clientRecordId, Prefix.length + HexDigits / 2) ?: return null
            return SyncIdKey(high, low)
        }

        private fun hexToLong(text: String, from: Int): Long? {
            var value = 0L
            for (index in from until from + HexDigits / 2) {
                val digit = when (val char = text[index]) {
                    in '0'..'9' -> char - '0'
                    // Lowercase only, as the codec writes. An uppercase id is a different string.
                    in 'a'..'f' -> char - 'a' + 10
                    else -> return null
                }
                value = (value shl 4) or digit.toLong()
            }
            return value
        }
    }
}
