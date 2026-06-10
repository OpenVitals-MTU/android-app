package tech.mmarca.openvitals.healthconnect

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.RefreshMode

class HealthConnectQueryCacheTest {

    @Test fun `cache evicts least recently used entry when bounded`() = runTest {
        val cache = HealthConnectQueryCache(maxEntries = 2)
        val first = HealthConnectQueryKey("dashboard", listOf("first"))
        val second = HealthConnectQueryKey("dashboard", listOf("second"))
        val third = HealthConnectQueryKey("dashboard", listOf("third"))

        assertEquals("one", cache.getOrPut(first) { "one" })
        assertEquals("two", cache.getOrPut(second) { "two" })
        assertEquals("one", cache.getOrPut(first) { "one-reloaded" })
        assertEquals("three", cache.getOrPut(third) { "three" })

        assertEquals("two-reloaded", cache.getOrPut(second) { "two-reloaded" })
        assertEquals("one-reloaded", cache.getOrPut(first) { "one-reloaded" })
    }

    @Test fun `cache honors ttl and force refresh`() = runTest {
        var now = 1_000L
        val cache = HealthConnectQueryCache(nowMillis = { now })
        val key = HealthConnectQueryKey("dashboard", listOf("today"))

        assertEquals(1, cache.getOrPut(key, ttlMillis = 100L) { 1 })
        assertEquals(1, cache.getOrPut(key, ttlMillis = 100L) { 2 })

        now += 101L

        assertEquals(3, cache.getOrPut(key, ttlMillis = 100L) { 3 })
        assertEquals(4, cache.getOrPut(key, refreshMode = RefreshMode.FORCE, ttlMillis = 100L) { 4 })
    }

    @Test fun `force refresh starts new load instead of joining stale in flight load`() = runTest {
        val cache = HealthConnectQueryCache()
        val key = HealthConnectQueryKey("dashboard", listOf("today"))
        val normalStarted = CompletableDeferred<Unit>()
        val finishNormal = CompletableDeferred<Unit>()

        val normal = async {
            cache.getOrPut(key) {
                normalStarted.complete(Unit)
                finishNormal.await()
                "normal"
            }
        }
        normalStarted.await()

        assertEquals(
            "force",
            cache.getOrPut(key, refreshMode = RefreshMode.FORCE) { "force" },
        )

        finishNormal.complete(Unit)
        assertEquals("normal", normal.await())
        assertEquals("force", cache.getOrPut(key) { "reloaded" })
    }

    @Test fun `invalidateOperations clears only matching operations`() = runTest {
        val cache = HealthConnectQueryCache()
        val dashboard = HealthConnectQueryKey("dashboard", listOf("today"))
        val other = HealthConnectQueryKey("other", listOf("today"))

        assertEquals("dashboard", cache.getOrPut(dashboard) { "dashboard" })
        assertEquals("other", cache.getOrPut(other) { "other" })

        cache.invalidateOperations("dashboard")

        assertEquals("dashboard-new", cache.getOrPut(dashboard) { "dashboard-new" })
        assertEquals("other", cache.getOrPut(other) { "other-new" })
    }
}
