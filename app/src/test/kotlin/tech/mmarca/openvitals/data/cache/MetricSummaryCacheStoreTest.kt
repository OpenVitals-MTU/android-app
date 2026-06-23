package tech.mmarca.openvitals.data.cache

import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.RefreshMode

class MetricSummaryCacheStoreTest {
    private val key = CachedSummaryKey(
        surface = "dashboard",
        startDate = LocalDate.of(2026, 6, 23),
        endDate = LocalDate.of(2026, 6, 23),
        metricSet = "STEPS",
        permissionFingerprint = "steps",
        configHash = "default",
        schemaVersion = 1,
    )

    @Test
    fun `freshness follows today stale and expired windows`() = runTest {
        var now = 1_000_000L
        val dao = FakeMetricSummaryCacheDao()
        val store = MetricSummaryCacheStore(
            dao = dao,
            nowMillis = { now },
            today = { LocalDate.of(2026, 6, 23) },
        )

        store.write(key, """{"value":1}""")

        assertEquals(CachedSummaryFreshness.FRESH, store.read(key).freshness)

        now += 6 * 60_000L
        assertEquals(CachedSummaryFreshness.STALE, store.read(key).freshness)

        now += 25 * 60 * 60_000L
        assertEquals(CachedSummaryFreshness.EXPIRED, store.read(key).freshness)
    }

    @Test
    fun `force refresh bypasses stored cache`() = runTest {
        val dao = FakeMetricSummaryCacheDao()
        val store = MetricSummaryCacheStore(
            dao = dao,
            today = { LocalDate.of(2026, 6, 23) },
        )

        store.write(key, """{"value":1}""")
        val read = store.read(key, refreshMode = RefreshMode.FORCE)

        assertEquals(CachedSummaryFreshness.MISS, read.freshness)
        assertNull(read.entry)
    }

    @Test
    fun `prune removes old entries`() = runTest {
        val dao = FakeMetricSummaryCacheDao()
        val store = MetricSummaryCacheStore(dao = dao)

        store.write(key, """{"value":1}""")
        assertEquals(1, store.prune(System.currentTimeMillis() + 1_000L))
        assertTrue(dao.entries.isEmpty())
    }
}

internal class FakeMetricSummaryCacheDao : MetricSummaryCacheDao {
    val entries = mutableListOf<CachedSummaryEntity>()

    override suspend fun get(
        surface: String,
        startDate: String,
        endDate: String,
        metricSet: String,
        permissionFingerprint: String,
        configHash: String,
        schemaVersion: Int,
    ): CachedSummaryEntity? =
        entries.firstOrNull {
            it.surface == surface &&
                it.startDate == startDate &&
                it.endDate == endDate &&
                it.metricSet == metricSet &&
                it.permissionFingerprint == permissionFingerprint &&
                it.configHash == configHash &&
                it.schemaVersion == schemaVersion
        }

    override suspend fun upsert(entity: CachedSummaryEntity) {
        delete(
            surface = entity.surface,
            startDate = entity.startDate,
            endDate = entity.endDate,
            metricSet = entity.metricSet,
            permissionFingerprint = entity.permissionFingerprint,
            configHash = entity.configHash,
            schemaVersion = entity.schemaVersion,
        )
        entries += entity
    }

    override suspend fun delete(
        surface: String,
        startDate: String,
        endDate: String,
        metricSet: String,
        permissionFingerprint: String,
        configHash: String,
        schemaVersion: Int,
    ) {
        entries.removeAll {
            it.surface == surface &&
                it.startDate == startDate &&
                it.endDate == endDate &&
                it.metricSet == metricSet &&
                it.permissionFingerprint == permissionFingerprint &&
                it.configHash == configHash &&
                it.schemaVersion == schemaVersion
        }
    }

    override suspend fun deleteSurface(surface: String) {
        entries.removeAll { it.surface == surface }
    }

    override suspend fun deleteOlderThan(beforeMillis: Long): Int {
        val before = entries.size
        entries.removeAll { it.writtenAtMillis < beforeMillis }
        return before - entries.size
    }
}
