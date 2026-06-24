package tech.mmarca.openvitals.data.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedSummaryEntity::class,
        DerivedMetricEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class OpenVitalsDatabase : RoomDatabase() {
    abstract fun metricSummaryCacheDao(): MetricSummaryCacheDao
    abstract fun derivedMetricDao(): DerivedMetricDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `derived_metric_value` (
                        `metricKey` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `periodStart` TEXT NOT NULL,
                        `periodEnd` TEXT NOT NULL,
                        `permissionFingerprint` TEXT NOT NULL,
                        `configHash` TEXT NOT NULL,
                        `schemaVersion` INTEGER NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `writtenAtMillis` INTEGER NOT NULL,
                        `sourceSummary` TEXT,
                        PRIMARY KEY(
                            `metricKey`,
                            `date`,
                            `periodStart`,
                            `periodEnd`,
                            `permissionFingerprint`,
                            `configHash`,
                            `schemaVersion`
                        )
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
