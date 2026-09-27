package tech.mmarca.openvitals.features.settings

import android.content.Context
import io.mockk.mockk
import io.mockk.verify
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
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.CycleReminderConfig
import tech.mmarca.openvitals.domain.preferences.BodyProfile
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
        reminders = reminders,
        homeWidgetRefreshScheduler = widgets,
    )
}
