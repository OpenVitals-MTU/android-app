package tech.mmarca.openvitals.data.cache

import java.time.LocalDate
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.domain.model.DerivedMetricKey
import tech.mmarca.openvitals.domain.model.RefreshMode

const val DerivedMetricSchemaVersion = 1

data class DerivedMetricCacheKey(
    val metricKey: DerivedMetricKey,
    val date: LocalDate,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val permissionFingerprint: String,
    val configHash: String,
    val schemaVersion: Int = DerivedMetricSchemaVersion,
)

data class DerivedMetricEntry(
    val key: DerivedMetricCacheKey,
    val payloadJson: String,
    val writtenAtMillis: Long,
    val sourceSummary: String?,
)

data class DerivedMetricRead(
    val entry: DerivedMetricEntry?,
    val freshness: CachedSummaryFreshness,
) {
    val isUsable: Boolean
        get() = freshness == CachedSummaryFreshness.FRESH || freshness == CachedSummaryFreshness.STALE
}

class DerivedMetricStore(
    private val dao: DerivedMetricDao,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) {
    suspend fun read(
        key: DerivedMetricCacheKey,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): DerivedMetricRead = withContext(dispatchers.io) {
        if (!refreshMode.allowsPersistentCache()) return@withContext DerivedMetricRead(null, CachedSummaryFreshness.MISS)
        val entity = dao.get(
            metricKey = key.metricKey.name,
            date = key.date.toString(),
            periodStart = key.periodStart.toString(),
            periodEnd = key.periodEnd.toString(),
            permissionFingerprint = key.permissionFingerprint,
            configHash = key.configHash,
            schemaVersion = key.schemaVersion,
        ) ?: return@withContext DerivedMetricRead(null, CachedSummaryFreshness.MISS)

        DerivedMetricRead(entity.toModel(key), freshnessFor(key, entity.writtenAtMillis))
    }

    suspend fun readAll(
        keys: Collection<DerivedMetricCacheKey>,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): Map<DerivedMetricCacheKey, DerivedMetricRead> = withContext(dispatchers.io) {
        if (keys.isEmpty()) return@withContext emptyMap()
        if (!refreshMode.allowsPersistentCache()) {
            return@withContext keys.associateWith { DerivedMetricRead(null, CachedSummaryFreshness.MISS) }
        }
        keys.groupBy {
            listOf(
                it.date.toString(),
                it.periodStart.toString(),
                it.periodEnd.toString(),
                it.permissionFingerprint,
                it.configHash,
                it.schemaVersion.toString(),
            )
        }.flatMap { (_, groupKeys) ->
            val first = groupKeys.first()
            val entities = dao.getAll(
                metricKeys = groupKeys.map { it.metricKey.name },
                date = first.date.toString(),
                periodStart = first.periodStart.toString(),
                periodEnd = first.periodEnd.toString(),
                permissionFingerprint = first.permissionFingerprint,
                configHash = first.configHash,
                schemaVersion = first.schemaVersion,
            ).associateBy { it.metricKey }
            groupKeys.map { key ->
                val entity = entities[key.metricKey.name]
                key to if (entity == null) {
                    DerivedMetricRead(null, CachedSummaryFreshness.MISS)
                } else {
                    DerivedMetricRead(entity.toModel(key), freshnessFor(key, entity.writtenAtMillis))
                }
            }
        }.toMap()
    }

    suspend fun write(
        key: DerivedMetricCacheKey,
        payloadJson: String,
        sourceSummary: String? = null,
    ) = withContext(dispatchers.io) {
        dao.upsert(
            DerivedMetricEntity(
                metricKey = key.metricKey.name,
                date = key.date.toString(),
                periodStart = key.periodStart.toString(),
                periodEnd = key.periodEnd.toString(),
                permissionFingerprint = key.permissionFingerprint,
                configHash = key.configHash,
                schemaVersion = key.schemaVersion,
                payloadJson = payloadJson,
                writtenAtMillis = nowMillis(),
                sourceSummary = sourceSummary,
            )
        )
    }

    suspend fun invalidate(key: DerivedMetricCacheKey) = withContext(dispatchers.io) {
        dao.delete(
            metricKey = key.metricKey.name,
            date = key.date.toString(),
            periodStart = key.periodStart.toString(),
            periodEnd = key.periodEnd.toString(),
            permissionFingerprint = key.permissionFingerprint,
            configHash = key.configHash,
            schemaVersion = key.schemaVersion,
        )
    }

    suspend fun prune(beforeMillis: Long): Int = withContext(dispatchers.io) {
        dao.deleteOlderThan(beforeMillis)
    }

    private fun freshnessFor(
        key: DerivedMetricCacheKey,
        writtenAtMillis: Long,
    ): CachedSummaryFreshness {
        val ageMillis = nowMillis() - writtenAtMillis
        val policy = summaryPolicyFor(key.periodEnd, today())
        return when {
            ageMillis <= policy.freshMillis -> CachedSummaryFreshness.FRESH
            ageMillis <= policy.staleUsableMillis -> CachedSummaryFreshness.STALE
            else -> CachedSummaryFreshness.EXPIRED
        }
    }
}

private fun DerivedMetricEntity.toModel(key: DerivedMetricCacheKey): DerivedMetricEntry =
    DerivedMetricEntry(
        key = key,
        payloadJson = payloadJson,
        writtenAtMillis = writtenAtMillis,
        sourceSummary = sourceSummary,
    )
