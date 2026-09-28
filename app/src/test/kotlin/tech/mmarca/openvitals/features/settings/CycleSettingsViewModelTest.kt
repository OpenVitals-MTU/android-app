package tech.mmarca.openvitals.features.settings

import android.content.Context
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.FakeCycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.FakePillIntakeRepository
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.CycleReminderConfig
import tech.mmarca.openvitals.domain.model.PillPlan
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.features.cycle.cycleJournalExportJson
import tech.mmarca.openvitals.features.cycle.parseCycleJournalExport
import tech.mmarca.openvitals.features.cycle.reminders.FakeCycleReminderSettings
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler
import tech.mmarca.openvitals.util.MainDispatcherRule

/** Settings → Cycle: contexts, the band, the permission-first reminders, and the journal's off switch. */
@OptIn(ExperimentalCoroutinesApi::class)
class CycleSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val today = LocalDate.now()

    @Test
    fun `a context change is stored, re-plans the reminders and redraws the widget`() {
        val preferences = FakePreferences()
        val reminders = FakeCycleReminderSettings()
        val widgets = mockk<HomeWidgetRefreshScheduler>(relaxed = true)
        val vm = viewModel(preferences = preferences, reminders = reminders, widgets = widgets)

        vm.setContext(TrackingContext.PCOS, enabled = true)

        assertEquals(setOf(TrackingContext.PCOS), preferences.cycleTrackingProfile().contexts)
        assertEquals(setOf(TrackingContext.PCOS), vm.uiState.value.profile.contexts)
        assertEquals(1, reminders.applied)
        verify(exactly = 1) { widgets.refreshNow() }
    }

    @Test
    fun `a pill plan change is stored, normalized and re-plans the reminders`() {
        val preferences = FakePreferences()
        val reminders = FakeCycleReminderSettings()
        val vm = viewModel(preferences = preferences, reminders = reminders)

        vm.setPillEnabled(true)
        vm.setPillPauseDays(99)

        assertEquals(true, preferences.pillPlan().enabled)
        assertEquals(today, preferences.pillPlan().packStart)
        assertEquals(PillPlan.PauseDaysRange.last, vm.uiState.value.pill.pauseDays)
        assertEquals(2, reminders.applied)
    }

    @Test
    fun `the band follows the body profile's birth year when one is set`() {
        val preferences = FakePreferences()
        assertNull(viewModel(preferences = preferences).uiState.value.derivedAgeBand)

        preferences.setBodyProfile(BodyProfile(birthYear = today.year - 30))

        assertEquals(AgeBand.AGE_30_34, viewModel(preferences = preferences).uiState.value.derivedAgeBand)
    }

    @Test
    fun `reminders are enabled only once the notification grant lands`() {
        val reminders = FakeCycleReminderSettings()
        val vm = viewModel(reminders = reminders)

        vm.requestEnableAfterPermission()
        vm.onNotificationPermissionResult(granted = false)
        assertFalse(reminders.config().enabled)

        vm.requestEnableAfterPermission()
        vm.onNotificationPermissionResult(granted = true)
        assertTrue(reminders.config().enabled)
        assertTrue(vm.uiState.value.reminders.enabled)

        // A grant that nobody asked for changes nothing.
        reminders.updateConfig(CycleReminderConfig())
        vm.onNotificationPermissionResult(granted = true)
        assertFalse(reminders.config().enabled)
    }

    @Test
    fun `deleting the journal wipes rows, exclusions and settings, then turns the reminders off`() = runTest {
        val journal = FakeCycleJournalRepository(
            initialEntries = listOf(CycleJournalEntry(date = today, painLevel = 3)),
            initialExclusions = mapOf(today.minusDays(40) to CycleExclusionReason.ILLNESS),
        )
        val preferences = FakePreferences()
        preferences.setCycleTrackingProfile(CycleTrackingProfile(contexts = setOf(TrackingContext.PMS), ageBand = AgeBand.AGE_25_29))
        val reminders = FakeCycleReminderSettings(CycleReminderConfig(enabled = true, dailyCheckInEnabled = true))
        val widgets = mockk<HomeWidgetRefreshScheduler>(relaxed = true)
        val vm = viewModel(preferences = preferences, journal = journal, reminders = reminders, widgets = widgets)

        vm.deleteCycleJournal()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(journal.entries.isEmpty())
        assertTrue(journal.exclusionsByDate.isEmpty())
        assertEquals(CycleTrackingProfile(), preferences.cycleTrackingProfile())
        assertFalse(reminders.config().enabled)
        assertEquals(CycleTrackingProfile(), vm.uiState.value.profile)
        assertFalse(vm.uiState.value.isDeletingJournal)
        verify(exactly = 1) { widgets.refreshNow() }
    }

    @Test
    fun `importing a backup keeps the newer edit per day, adds exclusions and fills an empty profile`() = runTest {
        val kept = CycleJournalEntry(date = today.minusDays(2), painLevel = 5, updatedAt = Instant.parse("2026-09-20T10:00:00Z"))
        val journal = FakeCycleJournalRepository(initialEntries = listOf(kept))
        val preferences = FakePreferences()
        val reminders = FakeCycleReminderSettings()
        val widgets = mockk<HomeWidgetRefreshScheduler>(relaxed = true)
        val vm = viewModel(preferences = preferences, journal = journal, reminders = reminders, widgets = widgets)
        val file = cycleJournalExportJson(
            entries = listOf(
                kept.copy(painLevel = 1, updatedAt = Instant.parse("2026-09-19T10:00:00Z")),
                CycleJournalEntry(date = today.minusDays(1), moodLevel = 4, updatedAt = Instant.parse("2026-09-21T10:00:00Z")),
            ),
            exclusions = mapOf(today.minusDays(40) to CycleExclusionReason.OTHER),
            profile = CycleTrackingProfile(contexts = setOf(TrackingContext.PCOS)),
            exportedAt = Instant.parse("2026-09-22T10:00:00Z"),
        )

        vm.importJson(file)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(5, journal.entries[today.minusDays(2)]?.painLevel)
        assertEquals(4, journal.entries[today.minusDays(1)]?.moodLevel)
        assertEquals(CycleExclusionReason.OTHER, journal.exclusionsByDate[today.minusDays(40)])
        assertEquals(setOf(TrackingContext.PCOS), preferences.cycleTrackingProfile().contexts)
        assertEquals(CycleBackupMessage.IMPORTED, vm.uiState.value.backupMessage)
        assertEquals(1, vm.uiState.value.importedDays)
        assertEquals(1, reminders.applied)
        verify(exactly = 1) { widgets.refreshNow() }
    }

    @Test
    fun `a file that is not a journal export imports nothing`() = runTest {
        val journal = FakeCycleJournalRepository()
        val vm = viewModel(journal = journal)

        vm.importJson("{\"format\":1,\"plans\":[]}")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(journal.entries.isEmpty())
        assertEquals(CycleBackupMessage.IMPORT_FAILED, vm.uiState.value.backupMessage)
    }

    @Test
    fun `the export carries the whole journal`() = runTest {
        val journal = FakeCycleJournalRepository(initialEntries = listOf(CycleJournalEntry(date = today, energyLevel = 2)))
        val vm = viewModel(journal = journal)

        val text = vm.exportJson()

        assertEquals(1, parseCycleJournalExport(text)!!.entries.size)
        vm.onExported()
        assertEquals(CycleBackupMessage.EXPORTED, vm.uiState.value.backupMessage)
    }

    private fun viewModel(
        preferences: FakePreferences = FakePreferences(),
        journal: FakeCycleJournalRepository = FakeCycleJournalRepository(),
        reminders: FakeCycleReminderSettings = FakeCycleReminderSettings(),
        widgets: HomeWidgetRefreshScheduler = mockk(relaxed = true),
    ) = CycleSettingsViewModel(
        context = context,
        preferences = preferences,
        bodyProfilePreferences = preferences,
        journal = journal,
        pillIntakes = FakePillIntakeRepository(),
        reminders = reminders,
        homeWidgetRefreshScheduler = widgets,
    )
}
