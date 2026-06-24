package tech.mmarca.openvitals.data.cache

import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DerivedMetricKey
import tech.mmarca.openvitals.domain.model.RefreshMode

class DerivedMetricStoreTest {
    private val dayKey = DerivedMetricCacheKey(
        metricKey = DerivedMetricKey.BMI,
        date = LocalDate.of(2026, 6, 23),
        periodStart = LocalDate.of(2026, 6, 23),
        periodEnd = LocalDate.of(2026, 6, 23),
        permissionFingerprint = "body",
        configHash = "default",
    )

    @Test
    fun `freshness follows selected period policy`() = runTest {
        var now = 1_000_000L
        val dao = FakeDerivedMetricDao()
        val store = DerivedMetricStore(
            dao = dao,
            nowMillis = { now },
            today = { LocalDate.of(2026, 6, 23) },
        )

        store.write(dayKey, """{"value":1}""")

        assertEquals(CachedSummaryFreshness.FRESH, store.read(dayKey).freshness)

        now += 6 * 60_000L
        assertEquals(CachedSummaryFreshness.STALE, store.read(dayKey).freshness)

        now += 25 * 60 * 60_000L
        assertEquals(CachedSummaryFreshness.EXPIRED, store.read(dayKey).freshness)
    }

    @Test
    fun `force refresh bypasses stored derived metric`() = runTest {
        val dao = FakeDerivedMetricDao()
        val store = DerivedMetricStore(
            dao = dao,
            today = { LocalDate.of(2026, 6, 23) },
        )

        store.write(dayKey, """{"value":1}""")
        val read = store.read(dayKey, refreshMode = RefreshMode.FORCE)

        assertEquals(CachedSummaryFreshness.MISS, read.freshness)
        assertNull(read.entry)
    }

    @Test
    fun `readAll supports mixed periods`() = runTest {
        val weekKey = dayKey.copy(
            metricKey = DerivedMetricKey.WEEKLY_CARDIO_LOAD,
            periodStart = LocalDate.of(2026, 6, 22),
            periodEnd = LocalDate.of(2026, 6, 28),
        )
        val dao = FakeDerivedMetricDao()
        val store = DerivedMetricStore(
            dao = dao,
            today = { LocalDate.of(2026, 6, 23) },
        )

        store.write(dayKey, """{"value":"day"}""")
        store.write(weekKey, """{"value":"week"}""")

        val reads = store.readAll(listOf(dayKey, weekKey))

        assertEquals(CachedSummaryFreshness.FRESH, reads.getValue(dayKey).freshness)
        assertEquals(CachedSummaryFreshness.FRESH, reads.getValue(weekKey).freshness)
        assertEquals("""{"value":"day"}""", reads.getValue(dayKey).entry?.payloadJson)
        assertEquals("""{"value":"week"}""", reads.getValue(weekKey).entry?.payloadJson)
    }

    @Test
    fun `prune removes old derived rows`() = runTest {
        val dao = FakeDerivedMetricDao()
        val store = DerivedMetricStore(dao = dao)

        store.write(dayKey, """{"value":1}""")
        assertEquals(1, store.prune(System.currentTimeMillis() + 1_000L))
        assertTrue(dao.entries.isEmpty())
    }
}

internal class FakeDerivedMetricDao : DerivedMetricDao {
    val entries = mutableListOf<DerivedMetricEntity>()

    override suspend fun get(
        metricKey: String,
        date: String,
        periodStart: String,
        periodEnd: String,
        permissionFingerprint: String,
        configHash: String,
        schemaVersion: Int,
    ): DerivedMetricEntity? =
        entries.firstOrNull {
            it.metricKey == metricKey &&
                it.date == date &&
                it.periodStart == periodStart &&
                it.periodEnd == periodEnd &&
                it.permissionFingerprint == permissionFingerprint &&
                it.configHash == configHash &&
                it.schemaVersion == schemaVersion
        }

    override suspend fun getAll(
        metricKeys: List<String>,
        date: String,
        periodStart: String,
        periodEnd: String,
        permissionFingerprint: String,
        configHash: String,
        schemaVersion: Int,
    ): List<DerivedMetricEntity> =
        entries.filter {
            it.metricKey in metricKeys &&
                it.date == date &&
                it.periodStart == periodStart &&
                it.periodEnd == periodEnd &&
                it.permissionFingerprint == permissionFingerprint &&
                it.configHash == configHash &&
                it.schemaVersion == schemaVersion
        }

    override suspend fun upsert(entity: DerivedMetricEntity) {
        delete(
            metricKey = entity.metricKey,
            date = entity.date,
            periodStart = entity.periodStart,
            periodEnd = entity.periodEnd,
            permissionFingerprint = entity.permissionFingerprint,
            configHash = entity.configHash,
            schemaVersion = entity.schemaVersion,
        )
        entries += entity
    }

    override suspend fun delete(
        metricKey: String,
        date: String,
        periodStart: String,
        periodEnd: String,
        permissionFingerprint: String,
        configHash: String,
        schemaVersion: Int,
    ) {
        entries.removeAll {
            it.metricKey == metricKey &&
                it.date == date &&
                it.periodStart == periodStart &&
                it.periodEnd == periodEnd &&
                it.permissionFingerprint == permissionFingerprint &&
                it.configHash == configHash &&
                it.schemaVersion == schemaVersion
        }
    }

    override suspend fun deleteOlderThan(beforeMillis: Long): Int {
        val before = entries.size
        entries.removeAll { it.writtenAtMillis < beforeMillis }
        return before - entries.size
    }
}

