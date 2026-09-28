package tech.mmarca.openvitals.data.local.cycle

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PillIntakeDao {
    @Query("SELECT * FROM pill_intakes WHERE date = :date LIMIT 1")
    suspend fun intake(date: String): PillIntakeEntity?

    /** ISO dates between two ISO dates, inclusive. ISO dates sort as text. */
    @Query("SELECT date FROM pill_intakes WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun datesBetween(start: String, end: String): List<String>

    @Upsert
    suspend fun upsert(intake: PillIntakeEntity)

    @Query("DELETE FROM pill_intakes WHERE date = :date")
    suspend fun delete(date: String)

    @Query("DELETE FROM pill_intakes")
    suspend fun deleteAll()
}
