package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.SexualActivityRecord
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.model.CervicalSensation
import tech.mmarca.openvitals.domain.model.CervixFirmness
import tech.mmarca.openvitals.domain.model.CervixOpening
import tech.mmarca.openvitals.domain.model.CervixPosition
import tech.mmarca.openvitals.domain.model.CycleJournalEntry

/** The header drip writes, verbatim from an export. */
private const val DripHeader =
    "date,temperature.value,temperature.exclude,temperature.time,temperature.note,bleeding.value,bleeding.exclude," +
        "mucus.feeling,mucus.texture,mucus.value,mucus.exclude,cervix.opening,cervix.firmness,cervix.position,cervix.exclude," +
        "note.value,desire.value,sex.solo,sex.partner,sex.condom,sex.pill,sex.iud,sex.patch,sex.ring,sex.implant,sex.diaphragm," +
        "sex.none,sex.other,sex.note,pain.cramps,pain.ovulationPain,pain.headache,pain.backache,pain.nausea,pain.tenderBreasts," +
        "pain.migraine,pain.other,pain.note,mood.happy,mood.sad,mood.stressed,mood.balanced,mood.fine,mood.anxious," +
        "mood.energetic,mood.fatigue,mood.angry,mood.other,mood.note"

private val Header = DripHeader.split(",")
private val Zone: ZoneId = ZoneOffset.UTC

private fun plan(vararg enabled: DripImportOption): DripImportPlan {
    val base = detectDripExport(Header, Zone)!!
    return if (enabled.isEmpty()) base else base.copy(enabled = enabled.toSet())
}

/** A row with every column empty except the named ones. */
private fun row(vararg cells: Pair<String, String>, rowNumber: Int = 2): CsvRow {
    val fields = MutableList(Header.size) { "" }
    for ((name, value) in cells) fields[Header.indexOf(name)] = value
    return CsvRow(rowNumber = rowNumber, fields = fields)
}

class DripImportTest {

    @Test
    fun `the drip header is recognised and a scale export is not`() {
        assertNotNull(detectDripExport(Header, Zone))
        assertNull(detectDripExport(listOf("Date", "Weight (kg)", "Fat mass (kg)"), Zone))
        // Column lookup is by name, so a reordered or extended export still reads.
        val reordered = detectDripExport(listOf("bleeding.value", "date", "mucus.feeling", "temperature.value", "extra"), Zone)!!
        assertEquals(1, reordered.columns["date"])
    }

    @Test
    fun `flow levels map one to one and spotting or excluded bleeding becomes intermenstrual bleeding`() {
        val heavy = convertDripRow(row("date" to "2015-12-19", "bleeding.value" to "3", "bleeding.exclude" to "false"), plan())
        val flow = heavy.records.single().record as MenstruationFlowRecord
        assertEquals(MenstruationFlowRecord.FLOW_HEAVY, flow.flow)
        assertEquals(LocalDate.of(2015, 12, 19), flow.time.atZone(Zone).toLocalDate())

        val spotting = convertDripRow(row("date" to "2015-12-30", "bleeding.value" to "0", "bleeding.exclude" to "false"), plan())
        assertTrue(spotting.records.single().record is IntermenstrualBleedingRecord)

        val excluded = convertDripRow(row("date" to "2015-12-30", "bleeding.value" to "2", "bleeding.exclude" to "true"), plan())
        assertTrue(excluded.records.single().record is IntermenstrualBleedingRecord)
    }

    @Test
    fun `a temperature lands at its recorded time and an excluded one is left out`() {
        val timed = convertDripRow(
            row("date" to "2016-01-02", "temperature.value" to "36.45", "temperature.exclude" to "false", "temperature.time" to "7:30"),
            plan(),
        )
        val record = timed.records.single().record as BasalBodyTemperatureRecord
        assertEquals(36.45, record.temperature.inCelsius, 1e-9)
        assertEquals("2016-01-02T07:30:00Z", record.time.toString())

        val excluded = convertDripRow(
            row("date" to "2016-01-03", "temperature.value" to "37.9", "temperature.exclude" to "true"),
            plan(),
        )
        assertTrue(excluded.records.isEmpty())
        assertTrue(excluded.diagnostics.isEmpty())

        val wild = convertDripRow(row("date" to "2016-01-04", "temperature.value" to "98.6", "temperature.exclude" to "false"), plan())
        assertEquals(CsvImportDiagnosticReason.OUT_OF_RANGE, wild.diagnostics.single().reason)
    }

    @Test
    fun `mucus texture names the appearance, the feeling stands in and fills the journal sensation`() {
        val creamy = convertDripRow(row("date" to "2016-02-01", "mucus.feeling" to "3", "mucus.texture" to "1", "mucus.exclude" to "false"), plan())
        val record = creamy.records.single().record as CervicalMucusRecord
        assertEquals(CervicalMucusRecord.APPEARANCE_CREAMY, record.appearance)
        assertEquals(CervicalSensation.SLIPPERY, creamy.journalEntry?.cervicalSensation)

        val wet = convertDripRow(row("date" to "2016-02-02", "mucus.feeling" to "2", "mucus.texture" to "0", "mucus.exclude" to "false"), plan())
        assertEquals(CervicalMucusRecord.APPEARANCE_WATERY, (wet.records.single().record as CervicalMucusRecord).appearance)

        val nothing = convertDripRow(row("date" to "2016-02-03", "mucus.feeling" to "1", "mucus.texture" to "0", "mucus.exclude" to "false"), plan())
        assertTrue(nothing.records.isEmpty())
        assertNull(nothing.journalEntry)

        val excluded = convertDripRow(row("date" to "2016-02-04", "mucus.feeling" to "3", "mucus.texture" to "2", "mucus.exclude" to "true"), plan())
        assertTrue(excluded.records.isEmpty())
        assertNull(excluded.journalEntry)
    }

    @Test
    fun `sex is protected with a contraceptive, unprotected with none, unknown otherwise`() {
        fun protection(vararg flags: String): Int {
            val conversion = convertDripRow(row("date" to "2016-03-01", *flags.map { it to "true" }.toTypedArray()), plan())
            return (conversion.records.single().record as SexualActivityRecord).protectionUsed
        }
        assertEquals(SexualActivityRecord.PROTECTION_USED_PROTECTED, protection("sex.partner", "sex.condom"))
        assertEquals(SexualActivityRecord.PROTECTION_USED_UNPROTECTED, protection("sex.partner", "sex.none"))
        assertEquals(SexualActivityRecord.PROTECTION_USED_UNKNOWN, protection("sex.solo"))
        val none = convertDripRow(row("date" to "2016-03-02", "sex.condom" to "true"), plan())
        assertTrue(none.records.isEmpty())
    }

    @Test
    fun `pain, mood and notes become the day's journal entry`() {
        val conversion = convertDripRow(
            row(
                "date" to "2015-12-31",
                "pain.cramps" to "true",
                "pain.migraine" to "true",
                "pain.ovulationPain" to "true",
                "mood.sad" to "true",
                "mood.fatigue" to "true",
                "note.value" to "long day",
                "mood.note" to "tired",
            ),
            plan(),
        )
        val entry = conversion.journalEntry!!
        assertEquals(LocalDate.of(2015, 12, 31), entry.date)
        assertEquals(
            setOf(CycleSymptom.CRAMPS, CycleSymptom.HEADACHE, CycleSymptom.PELVIC_PAIN_OUTSIDE_PERIOD, CycleSymptom.MOOD_CHANGES, CycleSymptom.FATIGUE),
            entry.symptoms,
        )
        assertEquals(2, entry.moodLevel)
        assertEquals(2, entry.energyLevel)
        assertEquals("long day\ntired", entry.notes)

        val mixed = convertDripRow(row("date" to "2016-01-01", "mood.happy" to "true", "mood.anxious" to "true", "mood.energetic" to "true"), plan())
        assertEquals(3, mixed.journalEntry?.moodLevel)
        assertEquals(4, mixed.journalEntry?.energyLevel)
    }

    @Test
    fun `the cervix and desire land in the journal, an excluded cervix does not`() {
        val day = convertDripRow(
            row("date" to "2016-04-01", "cervix.opening" to "2", "cervix.firmness" to "1", "cervix.position" to "0", "cervix.exclude" to "false", "desire.value" to "2"),
            plan(),
        )
        val entry = day.journalEntry!!
        assertEquals(CervixOpening.OPEN, entry.cervixOpening)
        assertEquals(CervixFirmness.SOFT, entry.cervixFirmness)
        assertEquals(CervixPosition.LOW, entry.cervixPosition)
        assertEquals(4, entry.desireLevel)

        val excluded = convertDripRow(row("date" to "2016-04-02", "cervix.opening" to "0", "cervix.exclude" to "true", "desire.value" to "0"), plan())
        assertNull(excluded.journalEntry?.cervixOpening)
        assertEquals(2, excluded.journalEntry?.desireLevel)
    }

    @Test
    fun `a disabled option reads none of its columns`() {
        val conversion = convertDripRow(
            row("date" to "2015-12-31", "bleeding.value" to "1", "bleeding.exclude" to "false", "pain.cramps" to "true", "sex.partner" to "true"),
            plan(DripImportOption.BLEEDING),
        )
        assertTrue(conversion.records.single().record is MenstruationFlowRecord)
        assertNull(conversion.journalEntry)
        assertEquals(setOf(DripImportOption.BLEEDING), plan(DripImportOption.BLEEDING).enabled)
        assertEquals(2, plan(DripImportOption.BLEEDING).requiredWritePermissions.size)
        assertTrue(plan(DripImportOption.JOURNAL).requiredWritePermissions.isEmpty())
    }

    @Test
    fun `a bad date costs the row and a blank day produces nothing`() {
        val bad = convertDripRow(row("date" to "31/12/2015", "bleeding.value" to "1"), plan())
        assertEquals(CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP, bad.diagnostics.single().reason)
        assertTrue(bad.records.isEmpty())

        val blank = convertDripRow(row("date" to "2015-12-29", "bleeding.exclude" to "true"), plan())
        assertTrue(blank.records.isEmpty())
        assertNull(blank.journalEntry)
        assertTrue(blank.diagnostics.isEmpty())
    }

    @Test
    fun `the same day converts to the same ids on every run so a re-import replaces`() {
        val first = convertDripRow(row("date" to "2015-12-19", "bleeding.value" to "2", "bleeding.exclude" to "false"), plan())
        val second = convertDripRow(row("date" to "2015-12-19", "bleeding.value" to "3", "bleeding.exclude" to "false"), plan())
        assertEquals(first.records.single().clientRecordId, second.records.single().clientRecordId)
        assertTrue(first.records.single().clientRecordId.startsWith("csv_menstruationflowrecord_"))
    }

    @Test
    fun `the journal merge adds drip's observations and keeps the phone's own`() {
        val existing = CycleJournalEntry(
            date = LocalDate.of(2016, 1, 1),
            symptoms = setOf(CycleSymptom.BLOATING),
            painLevel = 4,
            moodLevel = 5,
            notes = "own note",
        )
        val imported = CycleJournalEntry(
            date = LocalDate.of(2016, 1, 1),
            symptoms = setOf(CycleSymptom.CRAMPS),
            energyLevel = 2,
            cervixOpening = CervixOpening.CLOSED,
            notes = "own note\nfrom drip",
        )
        val merged = mergeDripJournalEntry(existing, imported)!!
        assertEquals(CervixOpening.CLOSED, merged.cervixOpening)
        assertEquals(setOf(CycleSymptom.BLOATING, CycleSymptom.CRAMPS), merged.symptoms)
        assertEquals(4, merged.painLevel)
        assertEquals(5, merged.moodLevel)
        assertEquals(2, merged.energyLevel)
        assertEquals("own note\nfrom drip", merged.notes)

        // Nothing new: null, so the day's edit time is not touched.
        assertNull(mergeDripJournalEntry(merged, imported))
        assertEquals(imported, mergeDripJournalEntry(null, imported))
    }

    @Test
    fun `the sample summary counts days with content and spans their dates`() {
        val rows = listOf(
            row("date" to "2015-12-31", "bleeding.value" to "1", "bleeding.exclude" to "false").fields,
            row("date" to "2015-12-29").fields,
            row("date" to "2015-12-23", "mood.anxious" to "true").fields,
        )
        assertEquals(2, dripSampleDaysWithContent(rows, plan()))
        assertEquals(LocalDate.of(2015, 12, 23) to LocalDate.of(2015, 12, 31), dripSampleDateRange(rows, plan()))
    }
}
