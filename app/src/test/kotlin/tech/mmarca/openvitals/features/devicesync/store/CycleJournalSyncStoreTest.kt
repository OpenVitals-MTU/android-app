package tech.mmarca.openvitals.features.devicesync.store

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.FakeCycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.FakePillIntakeRepository
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import tech.mmarca.openvitals.domain.model.PillPlan
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.domain.model.BbtDisturbance
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.HcgTestResult
import tech.mmarca.openvitals.features.devicesync.protocol.SyncItem

/** The journal on the wire: content keys both phones agree on, the newer edit wins, settings fill an empty phone. */
class CycleJournalSyncStoreTest {
    private val zone: ZoneId = ZoneId.of("UTC")
    private val windowStart: Instant = LocalDate.of(2026, 1, 1).atStartOfDay(zone).toInstant()
    private val windowEnd: Instant = LocalDate.of(2026, 7, 15).atStartOfDay(zone).toInstant()
    private val day = LocalDate.of(2026, 7, 10)
    private val entry = CycleJournalEntry(
        date = day,
        painLevel = 3,
        moodLevel = 2,
        symptoms = setOf(CycleSymptom.CRAMPS, CycleSymptom.HEADACHE),
        notes = "quiet day",
        hcgTest = HcgTestResult.NEGATIVE,
        bbtDisturbances = setOf(BbtDisturbance.POOR_SLEEP),
        updatedAt = Instant.parse("2026-07-10T20:00:00Z"),
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `the same observations give the same key on both phones, the edit time does not count`() {
        val later = entry.copy(updatedAt = Instant.parse("2026-07-11T08:00:00Z"))

        assertEquals(entryKey(entry), entryKey(later))
        assertNotEquals(entryKey(entry), entryKey(entry.copy(painLevel = 4)))
        assertTrue(entryKey(entry).startsWith(SYNC_CLIENT_RECORD_ID_PREFIX))
    }

    @Test
    fun `an entry survives the wire with every field and its edit time`() {
        val decoded = decodeEntry(encodeEntry(entry))

        assertEquals(entry, decoded)
        assertNull(decodeEntry("not json".toByteArray()))
    }

    @Test
    fun `keys and items cover the window only`() = runTest {
        val old = entry.copy(date = LocalDate.of(2025, 3, 1))
        val store = store(FakeCycleJournalRepository(initialEntries = listOf(entry, old)))

        val keys = store.readKeys(setOf(CycleJournalSyncTypes.ENTRY)).toList()
        val items = store.readItemChunks(CycleJournalSyncTypes.all.toSet(), chunkSize = 10).toList().flatten()

        assertEquals(listOf(entryKey(entry)), keys)
        assertEquals(listOf(entryKey(entry)), items.map { it.key })
    }

    @Test
    fun `a received entry lands with its own edit time, and an older edit of the same day is refused`() = runTest {
        val sender = store(FakeCycleJournalRepository(initialEntries = listOf(entry)))
        val receiverJournal = FakeCycleJournalRepository()
        val receiver = store(receiverJournal)
        receiver.readKeys(setOf(CycleJournalSyncTypes.ENTRY)).toList()
        val item = sender.readItemChunks(setOf(CycleJournalSyncTypes.ENTRY), 10).toList().flatten().single()

        assertTrue(receiver.accepts(item))
        assertEquals(setOf(item.key), receiver.writeItems(listOf(item)))
        assertEquals(entry, receiverJournal.entries[day])

        // The receiver now holds that edit; an older one for the same day is not accepted.
        val older = entryItem(entry.copy(painLevel = 1, updatedAt = Instant.parse("2026-07-09T08:00:00Z")))
        assertFalse(receiver.accepts(older))
        val newer = entryItem(entry.copy(painLevel = 5, updatedAt = Instant.parse("2026-07-12T08:00:00Z")))
        assertTrue(receiver.accepts(newer))
    }

    @Test
    fun `an entry outside the window is refused`() {
        val store = store(FakeCycleJournalRepository())

        assertFalse(store.accepts(entryItem(entry.copy(date = LocalDate.of(2025, 3, 1)))))
        // The other phone's clock may run a day ahead of Start sync.
        assertTrue(store.accepts(entryItem(entry.copy(date = LocalDate.of(2026, 7, 16)))))
        assertFalse(store.accepts(entryItem(entry.copy(date = LocalDate.of(2026, 7, 17)))))
    }

    @Test
    fun `exclusions merge as one row per start`() = runTest {
        val journal = FakeCycleJournalRepository(initialExclusions = mapOf(LocalDate.of(2026, 5, 3) to null))
        val store = store(journal)
        val item = exclusionItem(LocalDate.of(2026, 5, 3), CycleExclusionReason.ILLNESS)

        assertTrue(store.accepts(item))
        assertEquals(setOf(item.key), store.writeItems(listOf(item)))

        assertEquals(mapOf(LocalDate.of(2026, 5, 3) to CycleExclusionReason.ILLNESS), journal.exclusionsByDate)
        assertEquals(listOf(item.key), store.readKeys(setOf(CycleJournalSyncTypes.EXCLUSION)).toList())
    }

    @Test
    fun `the contexts and age band fill a phone that declared none, and never overwrite one that did`() = runTest {
        val declared = CycleTrackingProfile(contexts = setOf(TrackingContext.PCOS), ageBand = AgeBand.AGE_30_34)
        val item = profileItem(declared)!!
        assertNull(profileItem(CycleTrackingProfile()))

        val blank = FakePreferences()
        val blankStore = store(FakeCycleJournalRepository(), blank)
        assertTrue(blankStore.accepts(item))
        assertEquals(setOf(item.key), blankStore.writeItems(listOf(item)))
        assertEquals(declared, blank.cycleTrackingProfile())

        val own = FakePreferences().also { it.setCycleTrackingProfile(CycleTrackingProfile(ageBand = AgeBand.AGE_25_29)) }
        assertFalse(store(FakeCycleJournalRepository(), own).accepts(item))
    }

    @Test
    fun `the pill scheme travels once touched, the newer edit wins, and the reminder stays local`() = runTest {
        val edited = PillPlan(
            enabled = true,
            activeDays = 24,
            pauseDays = 4,
            packStart = day,
            reminderEnabled = false,
            updatedAt = Instant.parse("2026-07-10T20:00:00Z"),
        )
        assertNull(pillPlanItem(PillPlan()))
        val item = pillPlanItem(edited)!!
        assertEquals(pillPlanKey(edited), pillPlanKey(edited.copy(updatedAt = Instant.EPOCH, reminderEnabled = true)))

        val blank = FakePreferences()
        val blankStore = store(FakeCycleJournalRepository(), blank)
        assertTrue(blankStore.accepts(item))
        assertEquals(setOf(item.key), blankStore.writeItems(listOf(item)))
        val landed = blank.pillPlan()
        assertEquals(24, landed.activeDays)
        assertEquals(4, landed.pauseDays)
        assertEquals(day, landed.packStart)
        assertEquals(edited.updatedAt, landed.updatedAt)
        // This phone's own reminder switch, not the sender's.
        assertTrue(landed.reminderEnabled)

        val newer = FakePreferences().also {
            it.setPillPlan(PillPlan(enabled = true, packStart = day, updatedAt = Instant.parse("2026-07-11T08:00:00Z")))
        }
        assertFalse(store(FakeCycleJournalRepository(), newer).accepts(item))
    }

    @Test
    fun `taken days travel inside the window and merge as a union`() = runTest {
        val outside = LocalDate.of(2025, 12, 1)
        val mine = FakePillIntakeRepository().apply { taken += listOf(day, outside) }
        val sender = store(FakeCycleJournalRepository(), pillIntakes = mine)
        assertEquals(listOf(pillIntakeKey(day)), sender.readKeys(setOf(CycleJournalSyncTypes.PILL_INTAKE)).toList())

        val theirs = FakePillIntakeRepository().apply { taken += day.minusDays(1) }
        val receiver = store(FakeCycleJournalRepository(), pillIntakes = theirs)
        val item = pillIntakeItem(day)
        assertTrue(receiver.accepts(item))
        assertFalse(receiver.accepts(pillIntakeItem(outside)))
        assertEquals(setOf(item.key), receiver.writeItems(listOf(item)))
        assertEquals(setOf(day.minusDays(1), day), theirs.taken)
    }

    @Test
    fun `an unknown type is refused and a broken payload is skipped`() = runTest {
        val store = store(FakeCycleJournalRepository())
        val broken = SyncItem(key = "k", recordType = CycleJournalSyncTypes.EXCLUSION, payload = "{".toByteArray())

        assertFalse(store.accepts(SyncItem(key = "k", recordType = "StepsRecord", payload = ByteArray(0))))
        assertTrue(store.writeItems(listOf(broken)).isEmpty())
    }

    private fun store(
        journal: FakeCycleJournalRepository,
        preferences: FakePreferences = FakePreferences(),
        pillIntakes: FakePillIntakeRepository = FakePillIntakeRepository(),
    ) = CycleJournalSyncStore(journal, preferences, pillIntakes, windowStart, windowEnd, zone)
}
