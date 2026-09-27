package tech.mmarca.openvitals.data.local.cycle

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.model.BbtDisturbance
import tech.mmarca.openvitals.domain.model.CervicalSensation
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.HcgTestResult

/**
 * The subjective half of a day's cycle observations. Health Connect has no
 * record type for any of these fields; bleeding, tests and temperatures stay
 * there. One row per local day, keyed by its ISO date.
 */
@Entity(tableName = "cycle_journal_entries")
data class CycleJournalEntryEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "bleeding_none") val bleedingNone: Boolean,
    val pain: Int?,
    val mood: Int?,
    val energy: Int?,
    /** Symptom ids, comma-separated, sorted. */
    val symptoms: String,
    val notes: String,
    @ColumnInfo(name = "hcg_test") val hcgTest: String?,
    /** Disturbance names, comma-separated, sorted. */
    @ColumnInfo(name = "bbt_disturbances") val bbtDisturbances: String,
    @ColumnInfo(name = "cervical_sensation") val cervicalSensation: String?,
    @ColumnInfo(name = "updated_at_millis") val updatedAtMillis: Long,
) {
    /** Unknown ids and names are dropped, not surfaced. */
    fun toDomain(): CycleJournalEntry = CycleJournalEntry(
        date = LocalDate.parse(date),
        bleedingNone = bleedingNone,
        painLevel = pain?.takeIf { it in CycleJournalEntry.TRACKING_SCALE },
        moodLevel = mood?.takeIf { it in CycleJournalEntry.TRACKING_SCALE },
        energyLevel = energy?.takeIf { it in CycleJournalEntry.TRACKING_SCALE },
        symptoms = symptoms.splitList().mapNotNullTo(mutableSetOf()) { CycleSymptom.fromId(it) },
        notes = notes,
        hcgTest = hcgTest?.let { name -> HcgTestResult.entries.firstOrNull { it.name == name } },
        bbtDisturbances = bbtDisturbances.splitList()
            .mapNotNullTo(mutableSetOf()) { name -> BbtDisturbance.entries.firstOrNull { it.name == name } },
        cervicalSensation = cervicalSensation?.let { name -> CervicalSensation.entries.firstOrNull { it.name == name } },
        updatedAt = Instant.ofEpochMilli(updatedAtMillis),
    )

    companion object {
        fun fromDomain(entry: CycleJournalEntry): CycleJournalEntryEntity = CycleJournalEntryEntity(
            date = entry.date.toString(),
            bleedingNone = entry.bleedingNone,
            pain = entry.painLevel,
            mood = entry.moodLevel,
            energy = entry.energyLevel,
            symptoms = entry.symptoms.map { it.id }.sorted().joinToString(SEPARATOR),
            notes = entry.notes.trim(),
            hcgTest = entry.hcgTest?.name,
            bbtDisturbances = entry.bbtDisturbances.map { it.name }.sorted().joinToString(SEPARATOR),
            cervicalSensation = entry.cervicalSensation?.name,
            updatedAtMillis = entry.updatedAt.toEpochMilli(),
        )

        private const val SEPARATOR = ","

        private fun String.splitList(): List<String> = split(SEPARATOR).filter { it.isNotBlank() }
    }
}

/**
 * A cycle the user keeps out of the estimate. Keyed by a date inside the cycle,
 * its start when written, so the exclusion survives a start moving by a day.
 */
@Entity(tableName = "cycle_exclusions")
data class CycleExclusionEntity(
    @PrimaryKey @ColumnInfo(name = "start_date") val startDate: String,
    val reason: String?,
) {
    fun toDomain(): Pair<LocalDate, CycleExclusionReason?> =
        LocalDate.parse(startDate) to CycleExclusionReason.fromName(reason)
}
