package tech.mmarca.openvitals.data.local.cycle

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleJournalDao {
    @Query("SELECT * FROM cycle_journal_entries WHERE date = :date LIMIT 1")
    suspend fun entry(date: String): CycleJournalEntryEntity?

    /** Entries between two ISO dates, inclusive. ISO dates sort as text. */
    @Query("SELECT * FROM cycle_journal_entries WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun entriesBetween(start: String, end: String): List<CycleJournalEntryEntity>

    @Query("SELECT * FROM cycle_journal_entries ORDER BY date ASC")
    fun observeEntries(): Flow<List<CycleJournalEntryEntity>>

    @Query("SELECT * FROM cycle_journal_entries ORDER BY date ASC")
    suspend fun allEntries(): List<CycleJournalEntryEntity>

    @Upsert
    suspend fun upsert(entry: CycleJournalEntryEntity)

    @Query("DELETE FROM cycle_journal_entries WHERE date = :date")
    suspend fun delete(date: String)

    @Query("SELECT * FROM cycle_exclusions")
    suspend fun exclusions(): List<CycleExclusionEntity>

    @Query("SELECT * FROM cycle_exclusions")
    fun observeExclusions(): Flow<List<CycleExclusionEntity>>

    @Upsert
    suspend fun upsertExclusion(exclusion: CycleExclusionEntity)

    /** Removes every exclusion dated between two ISO dates, inclusive. */
    @Query("DELETE FROM cycle_exclusions WHERE start_date BETWEEN :start AND :end")
    suspend fun deleteExclusionsBetween(start: String, end: String)

    @Query("DELETE FROM cycle_journal_entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM cycle_exclusions")
    suspend fun deleteAllExclusions()
}
