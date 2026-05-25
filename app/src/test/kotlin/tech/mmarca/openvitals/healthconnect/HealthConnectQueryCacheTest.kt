package tech.mmarca.openvitals.healthconnect

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.performance.RefreshMode

class HealthConnectQueryCacheTest {

    @Test fun `current day entries expire after ttl`() = runTest {
        var now = 0L
        var loads = 0
        val cache = HealthConnectQueryCache { now }
        val key = HealthConnectQueryKey("steps", listOf("2026-05-25"))

        assertEquals(1, cache.getOrPut(key, ttlMillis = 60_000L) { ++loads })
        now = 59_999L
        assertEquals(1, cache.getOrPut(key, ttlMillis = 60_000L) { ++loads })
        now = 60_001L
        assertEquals(2, cache.getOrPut(key, ttlMillis = 60_000L) { ++loads })
        assertEquals(2, loads)
    }

    @Test fun `historical entries reuse cache without ttl`() = runTest {
        var loads = 0
        val cache = HealthConnectQueryCache()
        val key = HealthConnectQueryKey("steps", listOf("2026-05-01"))

        assertEquals(1, cache.getOrPut(key) { ++loads })
        assertEquals(1, cache.getOrPut(key) { ++loads })
        assertEquals(1, loads)
    }

    @Test fun `manual refresh reloads cached query`() = runTest {
        var loads = 0
        val cache = HealthConnectQueryCache()
        val key = HealthConnectQueryKey("steps", listOf("2026-05-01"))

        assertEquals(1, cache.getOrPut(key) { ++loads })
        assertEquals(2, cache.getOrPut(key, refreshMode = RefreshMode.FORCE) { ++loads })
        assertEquals(2, loads)
    }

    @Test fun `permission fingerprint creates separate cache entries`() = runTest {
        var loads = 0
        val cache = HealthConnectQueryCache()
        val stepsOnly = HealthConnectQueryKey("dashboard", permissions = "steps")
        val stepsAndSleep = HealthConnectQueryKey("dashboard", permissions = "sleep|steps")

        assertEquals(1, cache.getOrPut(stepsOnly) { ++loads })
        assertEquals(2, cache.getOrPut(stepsAndSleep) { ++loads })
        assertEquals(1, cache.getOrPut(stepsOnly) { ++loads })
        assertEquals(2, loads)
    }

    @Test fun `in flight query is shared by concurrent callers`() = runTest {
        var loads = 0
        val cache = HealthConnectQueryCache()
        val key = HealthConnectQueryKey("steps", listOf("2026-05-01"))
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()

        val first = async {
            cache.getOrPut(key) {
                loads++
                started.complete(Unit)
                finish.await()
                "ok"
            }
        }
        started.await()
        val second = async {
            cache.getOrPut(key) {
                loads++
                "duplicate"
            }
        }
        finish.complete(Unit)

        assertEquals("ok", first.await())
        assertEquals("ok", second.await())
        assertEquals(1, loads)
    }
}
