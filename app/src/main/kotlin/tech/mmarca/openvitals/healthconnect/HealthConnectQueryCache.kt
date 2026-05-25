package tech.mmarca.openvitals.healthconnect

import java.time.LocalDate
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
) {
    private val mutex = Mutex()
    private val entries = mutableMapOf<HealthConnectQueryKey, CacheEntry>()
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
            }

            entries[key]
                ?.takeUnless { it.isExpired(now, ttlMillis) }
                ?.let { cached -> return cached.value.uncheckedCast() }

            inFlight[key]?.let { return@withLock CacheLookup.Pending(it) }

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
                entries[key] = CacheEntry(value, nowMillis())
                inFlight.remove(key)
            }
            pending.complete(value)
            value
        } catch (t: Throwable) {
            mutex.withLock {
                inFlight.remove(key)
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
