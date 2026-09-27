package tech.mmarca.openvitals.domain.query

import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.CycleJournalEntry

data class CyclePeriodData(
    val data: CycleData,
    val missingPermissions: Set<String>,
    val statistics: CycleStatistics? = null,
    /** The journal rows inside the loaded period. */
    val journalEntries: List<CycleJournalEntry> = emptyList(),
    /** Every journal row, for today's card, the tips and the symptom patterns. The table is small. */
    val allJournalEntries: List<CycleJournalEntry> = emptyList(),
    /** Morning temperatures since the current cycle started, for the thermal chart. */
    val currentCycleTemperatures: List<BasalBodyTemperatureEntry> = emptyList(),
)
