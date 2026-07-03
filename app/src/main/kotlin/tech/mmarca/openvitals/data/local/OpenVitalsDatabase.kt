package tech.mmarca.openvitals.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import tech.mmarca.openvitals.data.local.beverage.BeverageDao
import tech.mmarca.openvitals.data.local.beverage.BeverageEntity

@Database(
    entities = [BeverageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class OpenVitalsDatabase : RoomDatabase() {
    abstract fun beverageDao(): BeverageDao
}
