package tech.mmarca.openvitals.data.local.food

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface FoodDao {
    @Transaction
    @Query("SELECT * FROM foods WHERE is_deleted = 0 ORDER BY sort_order ASC, name COLLATE NOCASE ASC")
    suspend fun activeFoods(): List<FoodWithNutrients>

    @Query("SELECT * FROM foods WHERE id = :id LIMIT 1")
    suspend fun foodById(id: String): FoodEntity?

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM foods")
    suspend fun nextSortOrder(): Int

    @Upsert
    suspend fun upsertFood(food: FoodEntity)

    @Query("DELETE FROM food_nutrients WHERE food_id = :foodId")
    suspend fun deleteNutrients(foodId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutrients(nutrients: List<FoodNutrientEntity>)

    /** The food and its nutrients land together. The old nutrient rows go, so an edit cannot leave one behind. */
    @Transaction
    suspend fun save(food: FoodEntity, nutrients: List<FoodNutrientEntity>) {
        upsertFood(food)
        deleteNutrients(food.id)
        insertNutrients(nutrients)
    }

    @Query("UPDATE foods SET is_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)
}
