package tech.mmarca.openvitals.data.local.beverage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface BeverageDao {
    @Query("SELECT * FROM beverages WHERE is_deleted = 0 ORDER BY sort_order ASC, name COLLATE NOCASE ASC")
    suspend fun activeBeverages(): List<BeverageEntity>

    @Query("SELECT * FROM beverages WHERE id = :id LIMIT 1")
    suspend fun beverageById(id: String): BeverageEntity?

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM beverages")
    suspend fun nextSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaults(beverages: List<BeverageEntity>)

    @Upsert
    suspend fun upsert(beverage: BeverageEntity)

    @Query("UPDATE beverages SET is_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("UPDATE beverages SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: String, category: String?)

    @Query("UPDATE beverages SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)

    @Transaction
    suspend fun updateSortOrder(ids: List<String>) {
        ids.forEachIndexed { index, id ->
            updateSortOrder(id, index)
        }
    }
}
