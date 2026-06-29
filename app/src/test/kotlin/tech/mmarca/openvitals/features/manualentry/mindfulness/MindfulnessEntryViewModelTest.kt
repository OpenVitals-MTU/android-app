package tech.mmarca.openvitals.features.manualentry.mindfulness

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.model.MindfulnessBackgroundSound
import tech.mmarca.openvitals.domain.model.MindfulnessBellSound
import tech.mmarca.openvitals.domain.model.MindfulnessSessionWriteRequest
import tech.mmarca.openvitals.domain.model.MindfulnessTimerConfig
import tech.mmarca.openvitals.data.repository.contract.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class MindfulnessEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `initial load checks write permission`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingPermission)
        assertTrue(vm.uiState.value.canWrite)
        assertEquals(setOf(WriteMindfulnessPermission), vm.uiState.value.writePermissions)
    }

    @Test fun `starting timer persists timer config`() = runTest {
        val preferencesRepository = prefs()
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = preferencesRepository,
        )
        advanceUntilIdle()

        vm.updateDurationMinutes("20")
        vm.updateIntervalEnabled(true)
        vm.updateIntervalMinutes("5")
        vm.updateBellSound(MindfulnessBellSound.HARMONY)
        vm.updateBackgroundSound(MindfulnessBackgroundSound.DREAMSCAPE)
        vm.startTimer()

        verify {
            preferencesRepository.setMindfulnessTimerConfig(
                MindfulnessTimerConfig(
                    durationMinutes = 20,
                    intervalMinutes = 5,
                    bellSound = MindfulnessBellSound.HARMONY,
                    backgroundSound = MindfulnessBackgroundSound.DREAMSCAPE,
                )
            )
        }
        assertNull(vm.uiState.value.entryError)
        assertTrue(vm.uiState.value.isTimerRunning)
    }

    @Test fun `changing bell sound emits short preview`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateBellSound(MindfulnessBellSound.TEMPLE)

        val event = vm.uiState.value.bellEvent
        assertEquals(MindfulnessBellSound.TEMPLE, event?.sound)
        assertEquals(1_500L, event?.previewMillis)
    }

    @Test fun `changing background sound emits short preview`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateBackgroundSound(MindfulnessBackgroundSound.CHIMES)

        val event = vm.uiState.value.backgroundEvent
        assertEquals(MindfulnessBackgroundSound.CHIMES, event?.sound)
        assertEquals(2_000L, event?.previewMillis)
    }

    @Test fun `selecting no background sound clears background preview`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateBackgroundSound(MindfulnessBackgroundSound.DREAMSCAPE)
        vm.updateBackgroundSound(MindfulnessBackgroundSound.NONE)

        assertEquals(MindfulnessBackgroundSound.NONE, vm.uiState.value.backgroundSound)
        assertNull(vm.uiState.value.backgroundEvent)
    }

    @Test fun `manual entry writes mindfulness session duration`() = runTest {
        val repository = repo(canWrite = true)
        val vm = MindfulnessEntryViewModel(
            repository = repository,
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateManualMinutes("12")
        vm.addManualEntry()
        advanceUntilIdle()

        coVerify {
            repository.writeMindfulnessSessionEntry(match<MindfulnessSessionWriteRequest> { request ->
                request.title == "Meditation" &&
                    Duration.between(request.startTime, request.endTime).toMinutes() == 12L
            })
        }
        assertFalse(vm.uiState.value.isSavingEntry)
        assertEquals("", vm.uiState.value.manualMinutesText)
        assertTrue(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)

        vm.onSaveCompletedHandled()
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `invalid manual entry does not write`() = runTest {
        val repository = repo(canWrite = true)
        val vm = MindfulnessEntryViewModel(
            repository = repository,
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateManualMinutes("0")
        vm.addManualEntry()

        assertEquals(MindfulnessEntryError.INVALID_MANUAL_ENTRY, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repository.writeMindfulnessSessionEntry(any()) }
    }

    @Test fun `missing write permission prevents manual entry write`() = runTest {
        val repository = repo(canWrite = false)
        val vm = MindfulnessEntryViewModel(
            repository = repository,
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateManualMinutes("10")
        vm.addManualEntry()

        assertEquals(MindfulnessEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repository.writeMindfulnessSessionEntry(any()) }
    }

    @Test fun `completed timer can be saved as mindfulness session`() = runTest {
        val repository = repo(canWrite = true)
        val vm = MindfulnessEntryViewModel(
            repository = repository,
            preferencesRepository = prefs(
                config = MindfulnessTimerConfig(
                    durationMinutes = 1,
                    intervalMinutes = null,
                    bellSound = MindfulnessBellSound.STRUCK,
                )
            ),
        )
        advanceUntilIdle()

        vm.startTimer()
        advanceTimeBy(60_000L)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isTimerRunning)
        assertTrue(vm.uiState.value.timerCompleted)

        vm.saveTimerSession()
        advanceUntilIdle()

        coVerify {
            repository.writeMindfulnessSessionEntry(match<MindfulnessSessionWriteRequest> { request ->
                request.title == "Meditation" &&
                    Duration.between(request.startTime, request.endTime).toMinutes() == 1L
            })
        }
        assertFalse(vm.uiState.value.timerCompleted)
        assertTrue(vm.uiState.value.saveCompleted)

        vm.onSaveCompletedHandled()
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `stopping timer pauses with resume save and discard state`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(
                config = MindfulnessTimerConfig(
                    durationMinutes = 2,
                    intervalMinutes = null,
                    bellSound = MindfulnessBellSound.STRUCK,
                )
            ),
        )
        advanceUntilIdle()

        vm.startTimer()
        advanceTimeBy(70_000L)
        runCurrent()
        vm.stopTimer()

        assertFalse(vm.uiState.value.isTimerRunning)
        assertTrue(vm.uiState.value.isTimerPaused)
        assertEquals(50, vm.uiState.value.remainingSeconds)

        vm.resumeTimer()

        assertTrue(vm.uiState.value.isTimerRunning)
        assertFalse(vm.uiState.value.isTimerPaused)
    }

    @Test fun `stopped timer saves elapsed mindfulness session`() = runTest {
        val repository = repo(canWrite = true)
        val vm = MindfulnessEntryViewModel(
            repository = repository,
            preferencesRepository = prefs(
                config = MindfulnessTimerConfig(
                    durationMinutes = 2,
                    intervalMinutes = null,
                    bellSound = MindfulnessBellSound.STRUCK,
                )
            ),
        )
        advanceUntilIdle()

        vm.startTimer()
        advanceTimeBy(70_000L)
        runCurrent()
        vm.stopTimer()
        vm.saveTimerSession()
        advanceUntilIdle()

        coVerify {
            repository.writeMindfulnessSessionEntry(match<MindfulnessSessionWriteRequest> { request ->
                Duration.between(request.startTime, request.endTime).seconds == 70L
            })
        }
        assertFalse(vm.uiState.value.isTimerPaused)
    }

    private fun repo(
        canWrite: Boolean = true,
        available: Boolean = true,
    ): MindfulnessRepository =
        mockk<MindfulnessRepository>().also { repo ->
            every { repo.mindfulnessWritePermissions } returns setOf(WriteMindfulnessPermission)
            every { repo.isMindfulnessAvailable() } returns available
            coEvery { repo.hasMindfulnessWritePermission() } returns canWrite
            coEvery { repo.writeMindfulnessSessionEntry(any()) } returns "record-id"
        }

    private fun prefs(
        config: MindfulnessTimerConfig = MindfulnessTimerConfig(
            durationMinutes = 10,
            intervalMinutes = null,
            bellSound = MindfulnessBellSound.STRUCK,
        ),
    ): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.mindfulnessTimerConfig() } returns config
            every { prefs.setMindfulnessTimerConfig(any()) } returns Unit
        }

    private companion object {
        private const val WriteMindfulnessPermission = "write_mindfulness"
    }
}
