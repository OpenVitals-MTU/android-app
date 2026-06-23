package tech.mmarca.openvitals.data.cache

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CachedSummaryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class OpenVitalsDatabase : RoomDatabase() {
    abstract fun metricSummaryCacheDao(): MetricSummaryCacheDao
}
