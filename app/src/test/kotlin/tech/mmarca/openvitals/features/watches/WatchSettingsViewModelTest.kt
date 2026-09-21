package tech.mmarca.openvitals.features.watches

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.GarminGattClientException
import tech.mmarca.openvitals.devices.garmin.GarminSettingsLink
import tech.mmarca.openvitals.devices.garmin.GarminSettingsScreen
import tech.mmarca.openvitals.navigation.WATCH_DEVICE_ID_ARG
import tech.mmarca.openvitals.navigation.WATCH_SETTINGS_SCREEN_ID_ARG
import tech.mmarca.openvitals.util.MainDispatcherRule

/** One screen of the watch's settings tree: what it shows, and what it says after a change. */
@OptIn(ExperimentalCoroutinesApi::class)
class WatchSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val link = mockk<GarminSettingsLink>()
    private val links = mockk<WatchSettingsLinks>(relaxed = true)
    private val before = GarminSettingsScreen(screenId = SCREEN, title = "Alarm")
    private val after = GarminSettingsScreen(screenId = SCREEN, title = "Alarm, as the watch holds it")

    private fun viewModel(): WatchSettingsViewModel {
        coEvery { links.link(WATCH) } returns link
        return WatchSettingsViewModel(
            SavedStateHandle(mapOf(WATCH_DEVICE_ID_ARG to WATCH, WATCH_SETTINGS_SCREEN_ID_ARG to SCREEN.toString())),
            links,
        )
    }

    @Test
    fun `the first read shows the screen and holds the link`() = runTest {
        coEvery { link.screen(SCREEN) } returns before

        val vm = viewModel()

        assertSame(before, vm.uiState.value.screen)
        assertFalse(vm.uiState.value.loading)
        assertFalse(vm.uiState.value.failed)
        verify(exactly = 1) { links.retain(WATCH) }
    }

    @Test
    fun `a watch out of range shows the failure, and retry reads again`() = runTest {
        coEvery { link.screen(SCREEN) } throws GarminGattClientException("out of range") andThen before

        val vm = viewModel()
        assertTrue(vm.uiState.value.failed)
        assertNull(vm.uiState.value.screen)

        vm.refresh()

        assertFalse(vm.uiState.value.failed)
        assertSame(before, vm.uiState.value.screen)
    }

    @Test
    fun `a watch that sent nothing is empty, not failed`() = runTest {
        coEvery { link.screen(SCREEN) } returns null

        val vm = viewModel()

        assertTrue(vm.uiState.value.isEmpty)
        assertFalse(vm.uiState.value.failed)
    }

    @Test
    fun `an applied change shows what the watch holds, not what was asked`() = runTest {
        coEvery { link.screen(SCREEN) } returns before andThen after
        coEvery { link.setSwitch(SCREEN, ENTRY, true) } returns true

        val vm = viewModel()
        vm.setSwitch(ENTRY, true)

        assertSame(after, vm.uiState.value.screen)
        assertNull(vm.uiState.value.notice)
        assertTrue(vm.uiState.value.busyEntryIds.isEmpty())
    }

    @Test
    fun `a refused change says so and still reads the screen again`() = runTest {
        coEvery { link.screen(SCREEN) } returns before andThen after
        coEvery { link.setOption(SCREEN, ENTRY, 2) } returns false

        val vm = viewModel()
        vm.chooseOption(ENTRY, 2)

        assertEquals(WatchSettingsNotice.REFUSED, vm.uiState.value.notice)
        assertSame(after, vm.uiState.value.screen)
    }

    @Test
    fun `no answer and a dropped link read the same to the user`() = runTest {
        coEvery { link.screen(SCREEN) } returns before
        coEvery { link.setSwitch(SCREEN, ENTRY, true) } returns null
        coEvery { link.setSwitch(SCREEN, ENTRY, false) } throws GarminGattClientException("link dropped")

        val vm = viewModel()
        vm.setSwitch(ENTRY, true)
        assertEquals(WatchSettingsNotice.UNANSWERED, vm.uiState.value.notice)

        vm.setSwitch(ENTRY, false)
        assertEquals(WatchSettingsNotice.UNANSWERED, vm.uiState.value.notice)
    }

    @Test
    fun `a row with a change in flight ignores a second tap`() = runTest {
        val answer = CompletableDeferred<Boolean?>()
        coEvery { link.screen(SCREEN) } returns before
        coEvery { link.setSwitch(SCREEN, ENTRY, any()) } coAnswers { answer.await() }

        val vm = viewModel()
        vm.setSwitch(ENTRY, true)
        assertEquals(setOf(ENTRY), vm.uiState.value.busyEntryIds)

        vm.setSwitch(ENTRY, false)
        answer.complete(true)

        coVerify(exactly = 1) { link.setSwitch(SCREEN, ENTRY, any()) }
        assertTrue(vm.uiState.value.busyEntryIds.isEmpty())
    }

    @Test
    fun `a delete that lands closes the screen and reads nothing more`() = runTest {
        coEvery { link.screen(SCREEN) } returns before
        coEvery { link.delete(SCREEN, ENTRY) } returns true
        val vm = viewModel()
        val events = mutableListOf<WatchSettingsEvent>()
        val collecting = launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }

        vm.runAction(ENTRY)

        assertEquals(listOf(WatchSettingsEvent.CLOSE_SCREEN), events)
        // The watch answers a deleted screen's id with its parent. Reading it would show the wrong screen.
        coVerify(exactly = 1) { link.screen(SCREEN) }
        collecting.cancel()
    }

    @Test
    fun `a return to the screen reads again, but not before the first read lands`() = runTest {
        val firstRead = CompletableDeferred<GarminSettingsScreen?>()
        coEvery { link.screen(SCREEN) } coAnswers { firstRead.await() } andThen after

        val vm = viewModel()
        vm.onResumed()
        coVerify(exactly = 1) { link.screen(SCREEN) }

        firstRead.complete(before)
        vm.onResumed()

        assertSame(after, vm.uiState.value.screen)
    }

    private companion object {
        const val WATCH = "watch-1"
        const val SCREEN = 7
        const val ENTRY = 3
    }
}
