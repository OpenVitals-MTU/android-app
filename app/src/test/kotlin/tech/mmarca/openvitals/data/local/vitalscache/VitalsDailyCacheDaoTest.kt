package tech.mmarca.openvitals.data.local.vitalscache

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * In-memory stand-in for [VitalsDailyCacheDao].
 *
 * The unit suite runs without Robolectric, so there is no SQLite to open — the
 * fake keeps the same two maps the tables are, reimplementing each `@Query`'s
 * SQL, while the `@Transaction` default methods on the interface
 * ([VitalsDailyCacheDao.replaceMetric], [VitalsDailyCacheDao.writeToken]) run
 * for real against it.
 */
private class FakeVitalsDailyCacheDao : VitalsDailyCacheDao {
    val aggregates = linkedMapOf<Pair<String, Long>, VitalsDailyAggregateEntity>()
    val cursors = linkedMapOf<String, VitalsSyncCursorEntity>()

    override suspend fun aggregatesBetween(
        metric: String,
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<VitalsDailyAggregateEntity> =
        aggregates.values
            .filter { it.metric == metric && it.epochDay in fromEpochDay..toEpochDay }
            .sortedBy { it.epochDay }

    override suspend fun upsertDay(row: VitalsDailyAggregateEntity) {
        aggregates[row.metric to row.epochDay] = row
    }

    override suspend fun deleteDay(metric: String, epochDay: Long) {
        aggregates.remove(metric to epochDay)
    }

    override suspend fun cursor(metric: String): VitalsSyncCursorEntity? = cursors[metric]

    override suspend fun writeFullSync(cursor: VitalsSyncCursorEntity) {
        cursors[cursor.metric] = cursor
    }

    override suspend fun deleteMetricRows(metric: String) {
        aggregates.keys.filter { it.first == metric }.forEach(aggregates::remove)
    }

    override suspend fun insertRows(rows: List<VitalsDailyAggregateEntity>) {
        rows.forEach { aggregates[it.metric to it.epochDay] = it }
    }

    override suspend fun deleteCursor(metric: String) {
        cursors.remove(metric)
    }

    override suspend fun updateToken(metric: String, token: String): Int {
        val existing = cursors[metric] ?: return 0
        cursors[metric] = existing.copy(changesToken = token)
        return 1
    }
}

/**
 * Port of the Flutter `vitals_daily_cache_dao_test.dart` suite, limited to the
 * cases whose behavior is real Kotlin code rather than Room-generated SQL: the
 * `@Transaction` default methods.
 */
class VitalsDailyCacheDaoTest {

    private val dao = FakeVitalsDailyCacheDao()

    private fun row(metric: String, epochDay: Long, valueSum: Double, sampleCount: Long) =
        VitalsDailyAggregateEntity(
            metric = metric,
            epochDay = epochDay,
            valueSum = valueSum,
            secondarySum = null,
            sampleCount = sampleCount,
        )

    @Test
    fun `replaceMetric atomically swaps every day for that metric only`() = runTest {
        dao.upsertDay(row("respiratoryRate", 1, 12.0, 1))
        dao.upsertDay(row("spo2", 1, 96.0, 1))

        dao.replaceMetric("respiratoryRate", listOf(row("respiratoryRate", 2, 14.0, 1)))

        // Old day 1 gone, new day 2 present.
        assertEquals(1, dao.aggregatesBetween("respiratoryRate", 0, 10).size)
        assertEquals(2L, dao.aggregatesBetween("respiratoryRate", 0, 10).single().epochDay)
        // The other metric is untouched.
        assertEquals(1, dao.aggregatesBetween("spo2", 0, 10).size)
    }

    @Test
    fun `writeFullSync sets token and stamp - writeToken preserves the stamp`() = runTest {
        dao.writeFullSync(
            VitalsSyncCursorEntity(
                metric = "respiratoryRate",
                changesToken = "tokenA",
                lastFullSyncMillis = 111,
            ),
        )
        var cursor = dao.cursor("respiratoryRate")
        assertEquals("tokenA", cursor!!.changesToken)
        assertEquals(111L, cursor.lastFullSyncMillis)

        dao.writeToken("respiratoryRate", "tokenB")
        cursor = dao.cursor("respiratoryRate")
        assertEquals("tokenB", cursor!!.changesToken)
        assertEquals(
            "an incremental token advance keeps the full-sync stamp",
            111L,
            cursor.lastFullSyncMillis,
        )
    }

    @Test
    fun `writeToken inserts a row when none exists yet`() = runTest {
        dao.writeToken("spo2", "fresh")

        assertEquals("fresh", dao.cursor("spo2")!!.changesToken)
    }
}
