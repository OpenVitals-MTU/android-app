package tech.mmarca.openvitals.data.cache

import java.time.LocalDate
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.domain.model.RefreshMode

class MetricSummaryCacheStore(
    private val dao: MetricSummaryCacheDao,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) {
    suspend fun read(
        key: CachedSummaryKey,
        referenceDate: LocalDate = key.endDate,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): CachedSummaryRead = withContext(dispatchers.io) {
        if (!refreshMode.allowsPersistentCache()) return@withContext CachedSummaryRead(null, CachedSummaryFreshness.MISS)
        val entity = dao.get(
            surface = key.surface,
            startDate = key.startDate.toString(),
            endDate = key.endDate.toString(),
            metricSet = key.metricSet,
            permissionFingerprint = key.permissionFingerprint,
            configHash = key.configHash,
            schemaVersion = key.schemaVersion,
        ) ?: return@withContext CachedSummaryRead(null, CachedSummaryFreshness.MISS)

        val ageMillis = nowMillis() - entity.writtenAtMillis
        val policy = summaryPolicyFor(referenceDate, today())
        val freshness = when {
            ageMillis <= policy.freshMillis -> CachedSummaryFreshness.FRESH
            ageMillis <= policy.staleUsableMillis -> CachedSummaryFreshness.STALE
            else -> CachedSummaryFreshness.EXPIRED
        }
        CachedSummaryRead(entity.toModel(), freshness)
    }

    suspend fun write(
        key: CachedSummaryKey,
        payloadJson: String,
    ) = withContext(dispatchers.io) {
        dao.upsert(
            CachedSummaryEntity(
                surface = key.surface,
                startDate = key.startDate.toString(),
                endDate = key.endDate.toString(),
                metricSet = key.metricSet,
                permissionFingerprint = key.permissionFingerprint,
                configHash = key.configHash,
                schemaVersion = key.schemaVersion,
                payloadJson = payloadJson,
                writtenAtMillis = nowMillis(),
            )
        )
    }

    suspend fun invalidate(key: CachedSummaryKey) = withContext(dispatchers.io) {
        dao.delete(
            surface = key.surface,
            startDate = key.startDate.toString(),
            endDate = key.endDate.toString(),
            metricSet = key.metricSet,
            permissionFingerprint = key.permissionFingerprint,
            configHash = key.configHash,
            schemaVersion = key.schemaVersion,
        )
    }

    suspend fun invalidateSurface(surface: String) = withContext(dispatchers.io) {
        dao.deleteSurface(surface)
    }

    suspend fun prune(beforeMillis: Long): Int = withContext(dispatchers.io) {
        dao.deleteOlderThan(beforeMillis)
    }

    suspend fun clearAll() = withContext(dispatchers.io) {
        dao.deleteAll()
    }
}

private fun CachedSummaryEntity.toModel(): CachedSummaryEntry =
    CachedSummaryEntry(
        key = CachedSummaryKey(
            surface = surface,
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            metricSet = metricSet,
            permissionFingerprint = permissionFingerprint,
            configHash = configHash,
            schemaVersion = schemaVersion,
        ),
        payloadJson = payloadJson,
        writtenAtMillis = writtenAtMillis,
    )
