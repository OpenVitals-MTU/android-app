package tech.mmarca.openvitals.healthconnect

import java.time.LocalDate
import java.util.LinkedHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.mmarca.openvitals.core.performance.RefreshMode

data class HealthConnectQueryKey(
    val operation: String,
    val parts: List<String> = emptyList(),
    val permissions: String = "",
)

class HealthConnectQueryCache(
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val maxEntries: Int = DefaultMaxEntries,
) {
    private val mutex = Mutex()
    private val entries = object : LinkedHashMap<HealthConnectQueryKey, CacheEntry>(
        maxEntries.coerceAtLeast(1),
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<HealthConnectQueryKey, CacheEntry>?): Boolean =
            size > maxEntries.coerceAtLeast(1)
    }
    private val inFlight = mutableMapOf<HealthConnectQueryKey, CompletableDeferred<Any?>>()

    suspend fun <T> getOrPut(
        key: HealthConnectQueryKey,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
        ttlMillis: Long? = null,
        loader: suspend () -> T,
    ): T {
        val now = nowMillis()
        val lookup = mutex.withLock {
            if (refreshMode == RefreshMode.FORCE) {
                entries.remove(key)
                inFlight.remove(key)
            }

            entries[key]
                ?.takeUnless { it.isExpired(now, ttlMillis) }
                ?.let { cached -> return cached.value.uncheckedCast() }

            if (refreshMode != RefreshMode.FORCE) {
                inFlight[key]?.let { return@withLock CacheLookup.Pending(it) }
            }

            CompletableDeferred<Any?>().also { deferred ->
                inFlight[key] = deferred
            }.let(CacheLookup::Owner)
        }

        if (lookup is CacheLookup.Pending) {
            return lookup.deferred.await().uncheckedCast()
        }

        val pending = (lookup as CacheLookup.Owner).deferred
        return try {
            val value = loader()
            mutex.withLock {
                if (inFlight[key] === pending) {
                    entries[key] = CacheEntry(value, nowMillis())
                    inFlight.remove(key)
                }
            }
            pending.complete(value)
            value
        } catch (t: Throwable) {
            mutex.withLock {
                if (inFlight[key] === pending) {
                    inFlight.remove(key)
                }
            }
            pending.completeExceptionally(t)
            throw t
        }
    }

    suspend fun invalidate(key: HealthConnectQueryKey) {
        mutex.withLock {
            entries.remove(key)
            inFlight.remove(key)
        }
    }

    suspend fun invalidateOperations(vararg operations: String) {
        val operationSet = operations.toSet()
        if (operationSet.isEmpty()) return
        mutex.withLock {
            entries.keys.removeAll { it.operation in operationSet }
            inFlight.keys.removeAll { it.operation in operationSet }
        }
    }

    suspend fun invalidateAll() {
        mutex.withLock {
            entries.clear()
            inFlight.clear()
        }
    }

    private data class CacheEntry(
        val value: Any?,
        val createdAtMillis: Long,
    ) {
        fun isExpired(nowMillis: Long, ttlMillis: Long?): Boolean =
            ttlMillis != null && nowMillis - createdAtMillis > ttlMillis
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Any?.uncheckedCast(): T = this as T

    private sealed interface CacheLookup {
        data class Pending(val deferred: CompletableDeferred<Any?>) : CacheLookup
        data class Owner(val deferred: CompletableDeferred<Any?>) : CacheLookup
    }

    private companion object {
        private const val DefaultMaxEntries = 128
    }
}

fun Set<String>.permissionFingerprint(): String =
    sorted().joinToString(separator = "|")

fun currentDayTtlMillis(
    start: LocalDate,
    end: LocalDate = start,
    today: LocalDate = LocalDate.now(),
): Long? =
    if (!end.isBefore(today) && !start.isAfter(today)) {
        CurrentDayCacheTtlMillis
    } else {
        null
    }

private const val CurrentDayCacheTtlMillis = 60_000L
