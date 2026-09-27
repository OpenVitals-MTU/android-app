package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.data.repository.contract.FakeCycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.domain.model.CycleDayLog
import tech.mmarca.openvitals.domain.model.CycleDayLogWrite
import tech.mmarca.openvitals.domain.model.CycleEntry
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.domain.model.DayBleedingChoice
import tech.mmarca.openvitals.domain.model.MenstruationFlowEntry
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.cycle.reminders.FakeCycleReminderSettings
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler
import tech.mmarca.openvitals.navigation.CYCLE_ENTRY_DATE_ARG
import tech.mmarca.openvitals.navigation.CYCLE_ENTRY_PRESET_ARG
import tech.mmarca.openvitals.navigation.CycleEntryPreset
import tech.mmarca.openvitals.util.MainDispatcherRule

/** The day log: what loads, what saves, and what is refused. */
@OptIn(ExperimentalCoroutinesApi::class)
class CycleEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val zone = ZoneId.systemDefault()

    private fun repository(
        granted: Set<CycleEntryKind> = CycleEntryKind.entries.toSet(),
        log: CycleDayLog? = null,
    ): CycleRepository = mockk(relaxed = true) {
        every { cycleWritePermissions(any()) } answers { setOf("write:${firstArg<CycleEntryKind>().name}") }
        coEvery { hasCycleWritePermission(any()) } answers { firstArg<CycleEntryKind>() in granted }
        coEvery { loadDayLog(any()) } answers { log ?: CycleDayLog(date = firstArg()) }
    }

    private fun viewModel(
        repository: CycleRepository,
        journal: FakeCycleJournalRepository = FakeCycleJournalRepository(),
        preferences: FakePreferences = FakePreferences(),
        handle: SavedStateHandle = SavedStateHandle(),
        reminders: FakeCycleReminderSettings = FakeCycleReminderSettings(),
        widgets: HomeWidgetRefreshScheduler = mockk(relaxed = true),
    ) = CycleEntryViewModel(repository, journal, preferences, reminders, handle, widgets)

    @Test fun `start probes every kind and loads the day`() = runTest {
        val repository = repository(granted = setOf(CycleEntryKind.MENSTRUATION_FLOW))
        val vm = viewModel(repository)

        vm.start()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isCheckingPermission)
        assertFalse(state.isLoadingDay)
        assertEquals(setOf(CycleEntryKind.MENSTRUATION_FLOW), state.grantedKinds)
        assertEquals(CycleEntryKind.entries.size, state.writePermissions.size)
        assertEquals(today, state.date)
        coVerify(exactly = 1) { repository.loadDayLog(today) }
    }

    @Test fun `an empty day with nothing typed is NOTHING_TO_SAVE`() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        vm.start()
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        assertEquals(CycleEntryError.NOTHING_TO_SAVE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repository.saveDayLog(any(), any()) }
    }

    @Test fun `saving writes the whole day and completes`() = runTest {
        val repository = repository()
        val written = slot<CycleDayLogWrite>()
        coEvery { repository.saveDayLog(any(), capture(written)) } returns Unit
        val reminders = FakeCycleReminderSettings()
        val widgets = mockk<HomeWidgetRefreshScheduler>(relaxed = true)
        val vm = viewModel(repository, reminders = reminders, widgets = widgets)
        vm.start()
        advanceUntilIdle()

        vm.setBleeding(BleedingOption.MEDIUM)
        vm.setPain(3)
        vm.toggleSymptom(CycleSymptom.CRAMPS)
        vm.setNotes("  quiet day ")
        vm.save()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.saveCompleted)
        assertNull(state.entryError)
        assertFalse(state.hasChanges)
        assertEquals(DayBleedingChoice.Flow(CycleRecordValues.FLOW_MEDIUM), written.captured.bleeding)
        assertEquals(3, written.captured.journal.painLevel)
        assertEquals(setOf(CycleSymptom.CRAMPS), written.captured.journal.symptoms)
        assertEquals("quiet day", written.captured.journal.notes)
        assertEquals(today, written.captured.journal.date)
        // A saved day moves the reminders and the home widget.
        assertEquals(1, reminders.applied)
        verify(exactly = 1) { widgets.refreshNow() }
    }

    @Test fun `a changed kind without its permission is refused before any write`() = runTest {
        val repository = repository(granted = setOf(CycleEntryKind.MENSTRUATION_FLOW))
        val vm = viewModel(repository)
        vm.start()
        advanceUntilIdle()

        vm.setBleeding(BleedingOption.SPOTTING)
        vm.save()
        advanceUntilIdle()

        assertEquals(CycleEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repository.saveDayLog(any(), any()) }
    }

    @Test fun `an unchanged kind does not need its permission`() = runTest {
        val flow = MenstruationFlowEntry(
            time = today.atTime(LocalTime.NOON).atZone(zone).toInstant(),
            flow = CycleRecordValues.FLOW_LIGHT,
            source = "OpenVitals",
            id = "own",
            isOpenVitalsEntry = true,
        )
        val repository = repository(granted = emptySet(), log = CycleDayLog(date = today, ownFlow = flow))
        val vm = viewModel(repository)
        vm.start()
        advanceUntilIdle()
        assertEquals(BleedingOption.LIGHT, vm.uiState.value.form.bleeding)

        vm.setMood(4)
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.saveCompleted)
        coVerify(exactly = 1) { repository.saveDayLog(today, any()) }
    }

    @Test fun `an invalid temperature reports INVALID_VALUE and writes nothing`() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        vm.start()
        advanceUntilIdle()

        vm.setBbtInput("42")
        vm.save(UnitSystem.METRIC)
        advanceUntilIdle()

        assertEquals(CycleEntryError.INVALID_VALUE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repository.saveDayLog(any(), any()) }
    }

    @Test fun `a fahrenheit temperature is converted on save`() = runTest {
        val repository = repository()
        val written = slot<CycleDayLogWrite>()
        coEvery { repository.saveDayLog(any(), capture(written)) } returns Unit
        val vm = viewModel(repository)
        vm.start(UnitSystem.IMPERIAL)
        advanceUntilIdle()

        vm.setBbtInput("98.6")
        vm.save(UnitSystem.IMPERIAL)
        advanceUntilIdle()

        assertEquals(37.0, written.captured.basalBodyTemperatureCelsius!!, 0.01)
    }

    @Test fun `a failed save keeps the form and surfaces the error`() = runTest {
        val repository = repository()
        coEvery { repository.saveDayLog(any(), any()) } throws IllegalStateException("boom")
        val vm = viewModel(repository)
        vm.start()
        advanceUntilIdle()

        vm.setEnergy(2)
        vm.save()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(CycleEntryError.WRITE_FAILED, state.entryError)
        assertEquals(2, state.form.energy)
        assertTrue(state.hasChanges)
        assertFalse(state.saveCompleted)
    }

    @Test fun `editing clears a previous failure`() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        vm.start()
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertEquals(CycleEntryError.NOTHING_TO_SAVE, vm.uiState.value.entryError)

        vm.setPain(1)

        assertNull(vm.uiState.value.entryError)
    }

    @Test fun `the offered symptoms follow the declared contexts and keep recorded ones`() = runTest {
        val preferences = FakePreferences().apply {
            setCycleTrackingProfile(CycleTrackingProfile(contexts = setOf(TrackingContext.PMS)))
        }
        val journal = FakeCycleJournalRepository(
            initialEntries = listOf(CycleJournalEntry(today.minusDays(1), symptoms = setOf(CycleSymptom.HEADACHE))),
        )
        val log = CycleDayLog(date = today, journal = CycleJournalEntry(today, symptoms = setOf(CycleSymptom.PELVIC_PAIN_OUTSIDE_PERIOD)))
        val vm = viewModel(repository(log = log), journal, preferences)
        vm.start()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(CycleSymptom.BREAST_TENDERNESS in state.offeredSymptoms)
        assertTrue(CycleSymptom.PELVIC_PAIN_OUTSIDE_PERIOD in state.offeredSymptoms)
        assertEquals(setOf(CycleSymptom.HEADACHE), state.previousDaySymptoms)
        assertTrue(state.showMore)

        vm.copyPreviousDaySymptoms()
        assertEquals(setOf(CycleSymptom.PELVIC_PAIN_OUTSIDE_PERIOD, CycleSymptom.HEADACHE), vm.uiState.value.form.symptoms)
    }

    @Test fun `a requested date opens that day and cannot pass today`() = runTest {
        val repository = repository()
        val handle = SavedStateHandle(mapOf(CYCLE_ENTRY_DATE_ARG to today.plusDays(3).toString()))
        val vm = viewModel(repository, handle = handle)
        vm.start()
        advanceUntilIdle()

        assertEquals(today, vm.uiState.value.date)

        vm.updateDate(today.minusDays(2))
        advanceUntilIdle()
        assertEquals(today.minusDays(2), vm.uiState.value.date)
        coVerify(exactly = 1) { repository.loadDayLog(today.minusDays(2)) }
    }

    @Test fun `the period-start preset preselects light flow on an empty day`() = runTest {
        val repository = repository()
        val handle = SavedStateHandle(mapOf(CYCLE_ENTRY_PRESET_ARG to CycleEntryPreset.PERIOD_START))
        val vm = viewModel(repository, handle = handle)
        vm.start()
        advanceUntilIdle()

        assertEquals(BleedingOption.LIGHT, vm.uiState.value.form.bleeding)
        assertNull(vm.uiState.value.loadedForm.bleeding)
        assertTrue(vm.uiState.value.hasChanges)

        // Switching the day drops the preset: it applies to the first load only.
        vm.updateDate(today.minusDays(1))
        advanceUntilIdle()
        assertNull(vm.uiState.value.form.bleeding)
    }

    @Test fun `the period-start preset never overrides recorded bleeding`() = runTest {
        val flow = MenstruationFlowEntry(
            time = today.atTime(LocalTime.NOON).atZone(zone).toInstant(),
            flow = CycleRecordValues.FLOW_HEAVY,
            source = "OpenVitals",
            id = "own",
            isOpenVitalsEntry = true,
        )
        val handle = SavedStateHandle(mapOf(CYCLE_ENTRY_PRESET_ARG to CycleEntryPreset.PERIOD_START))
        val own = viewModel(repository(log = CycleDayLog(date = today, ownFlow = flow)), handle = handle)
        own.start()
        advanceUntilIdle()
        assertEquals(BleedingOption.HEAVY, own.uiState.value.form.bleeding)
        assertFalse(own.uiState.value.hasChanges)

        val foreign = viewModel(repository(log = CycleDayLog(date = today, foreignFlowLevel = CycleRecordValues.FLOW_MEDIUM)), handle = handle)
        foreign.start()
        advanceUntilIdle()
        assertNull(foreign.uiState.value.form.bleeding)
        assertFalse(foreign.uiState.value.hasChanges)
    }
}
