package tech.mmarca.openvitals.features.cycle

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.domain.model.BbtDisturbance
import tech.mmarca.openvitals.domain.model.CervicalSensation
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.HcgTestResult

/** The backup file round-trips every field; a file that is not a journal export parses to nothing. */
class CycleJournalExportTest {
    private val entries = listOf(
        CycleJournalEntry(
            date = LocalDate.of(2026, 7, 10),
            bleedingNone = true,
            painLevel = 3,
            moodLevel = 2,
            energyLevel = 4,
            symptoms = setOf(CycleSymptom.CRAMPS, CycleSymptom.FATIGUE),
            notes = "quiet day",
            hcgTest = HcgTestResult.NEGATIVE,
            bbtDisturbances = setOf(BbtDisturbance.ALCOHOL, BbtDisturbance.POOR_SLEEP),
            cervicalSensation = CervicalSensation.DRY,
            updatedAt = Instant.parse("2026-07-10T20:00:00Z"),
        ),
        CycleJournalEntry(date = LocalDate.of(2026, 7, 11), painLevel = 1, updatedAt = Instant.parse("2026-07-11T21:00:00Z")),
    )
    private val exclusions = mapOf(LocalDate.of(2026, 5, 3) to CycleExclusionReason.ILLNESS, LocalDate.of(2026, 3, 1) to null)
    private val profile = CycleTrackingProfile(contexts = setOf(TrackingContext.PMS, TrackingContext.PCOS), ageBand = AgeBand.AGE_30_34)

    @Test
    fun `the file round-trips entries, exclusions and the profile`() {
        val text = cycleJournalExportJson(entries, exclusions, profile, Instant.parse("2026-07-12T08:00:00Z"))

        val import = parseCycleJournalExport(text)!!

        assertEquals(entries, import.entries)
        assertEquals(exclusions.entries.sortedBy { it.key }.map { it.key to it.value }, import.exclusions)
        assertEquals(profile, import.profile)
        assertTrue(text.contains("\"format\": 1"))
    }

    @Test
    fun `an empty day is dropped, an unknown symptom is skipped, a missing field is its default`() {
        val text = """{"format":1,"entries":[{"date":"2026-07-10","symptoms":["cramps","not_a_symptom"]},{"date":"2026-07-11"}]}"""

        val import = parseCycleJournalExport(text)!!

        assertEquals(1, import.entries.size)
        assertEquals(setOf(CycleSymptom.CRAMPS), import.entries.single().symptoms)
        assertEquals(Instant.EPOCH, import.entries.single().updatedAt)
        assertTrue(import.exclusions.isEmpty())
        assertNull(import.profile)
    }

    @Test
    fun `not a journal export means nothing to import`() {
        assertNull(parseCycleJournalExport(""))
        assertNull(parseCycleJournalExport("{}"))
        assertNull(parseCycleJournalExport("""{"format":99,"entries":[]}"""))
        assertNull(parseCycleJournalExport("""{"format":1,"plans":[]}"""))
    }
}
