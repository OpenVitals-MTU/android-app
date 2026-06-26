package tech.mmarca.openvitals.data.cache

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

@Entity(
    tableName = "metric_summary_cache",
    primaryKeys = [
        "surface",
        "startDate",
        "endDate",
        "metricSet",
        "permissionFingerprint",
        "configHash",
        "schemaVersion",
    ],
)
data class CachedSummaryEntity(
    val surface: String,
    val startDate: String,
    val endDate: String,
    val metricSet: String,
    val permissionFingerprint: String,
    val configHash: String,
    val schemaVersion: Int,
    val payloadJson: String,
    val writtenAtMillis: Long,
)

@Dao
interface MetricSummaryCacheDao {
    @Query(
        """
        SELECT * FROM metric_summary_cache
        WHERE surface = :surface
          AND startDate = :startDate
          AND endDate = :endDate
          AND metricSet = :metricSet
          AND permissionFingerprint = :permissionFingerprint
          AND configHash = :configHash
          AND schemaVersion = :schemaVersion
        LIMIT 1
        """
    )
    suspend fun get(
        surface: String,
        startDate: String,
        endDate: String,
        metricSet: String,
        permissionFingerprint: String,
        configHash: String,
        schemaVersion: Int,
    ): CachedSummaryEntity?

    @Upsert
    suspend fun upsert(entity: CachedSummaryEntity)

    @Query(
        """
        DELETE FROM metric_summary_cache
        WHERE surface = :surface
          AND startDate = :startDate
          AND endDate = :endDate
          AND metricSet = :metricSet
          AND permissionFingerprint = :permissionFingerprint
          AND configHash = :configHash
          AND schemaVersion = :schemaVersion
        """
    )
    suspend fun delete(
        surface: String,
        startDate: String,
        endDate: String,
        metricSet: String,
        permissionFingerprint: String,
        configHash: String,
        schemaVersion: Int,
    )

    @Query("DELETE FROM metric_summary_cache WHERE surface = :surface")
    suspend fun deleteSurface(surface: String)

    @Query("DELETE FROM metric_summary_cache WHERE writtenAtMillis < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long): Int

    @Query("DELETE FROM metric_summary_cache")
    suspend fun deleteAll()
}
