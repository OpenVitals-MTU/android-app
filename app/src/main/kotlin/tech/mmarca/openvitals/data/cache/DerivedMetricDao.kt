package tech.mmarca.openvitals.data.cache

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

@Entity(
    tableName = "derived_metric_value",
    primaryKeys = [
        "metricKey",
        "date",
        "periodStart",
        "periodEnd",
        "permissionFingerprint",
        "configHash",
        "schemaVersion",
    ],
)
data class DerivedMetricEntity(
    val metricKey: String,
    val date: String,
    val periodStart: String,
    val periodEnd: String,
    val permissionFingerprint: String,
    val configHash: String,
    val schemaVersion: Int,
    val payloadJson: String,
    val writtenAtMillis: Long,
    val sourceSummary: String?,
)

@Dao
interface DerivedMetricDao {
    @Query(
        """
        SELECT * FROM derived_metric_value
        WHERE metricKey = :metricKey
          AND date = :date
          AND periodStart = :periodStart
          AND periodEnd = :periodEnd
          AND permissionFingerprint = :permissionFingerprint
          AND configHash = :configHash
          AND schemaVersion = :schemaVersion
        LIMIT 1
        """
    )
    suspend fun get(
        metricKey: String,
        date: String,
        periodStart: String,
        periodEnd: String,
        permissionFingerprint: String,
        configHash: String,
        schemaVersion: Int,
    ): DerivedMetricEntity?

    @Query(
        """
        SELECT * FROM derived_metric_value
        WHERE metricKey IN (:metricKeys)
          AND date = :date
          AND periodStart = :periodStart
          AND periodEnd = :periodEnd
          AND permissionFingerprint = :permissionFingerprint
          AND configHash = :configHash
          AND schemaVersion = :schemaVersion
        """
    )
    suspend fun getAll(
        metricKeys: List<String>,
        date: String,
        periodStart: String,
        periodEnd: String,
        permissionFingerprint: String,
        configHash: String,
        schemaVersion: Int,
    ): List<DerivedMetricEntity>

    @Upsert
    suspend fun upsert(entity: DerivedMetricEntity)

    @Query(
        """
        DELETE FROM derived_metric_value
        WHERE metricKey = :metricKey
          AND date = :date
          AND periodStart = :periodStart
          AND periodEnd = :periodEnd
          AND permissionFingerprint = :permissionFingerprint
          AND configHash = :configHash
          AND schemaVersion = :schemaVersion
        """
    )
    suspend fun delete(
        metricKey: String,
        date: String,
        periodStart: String,
        periodEnd: String,
        permissionFingerprint: String,
        configHash: String,
        schemaVersion: Int,
    )

    @Query("DELETE FROM derived_metric_value WHERE writtenAtMillis < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long): Int
}

