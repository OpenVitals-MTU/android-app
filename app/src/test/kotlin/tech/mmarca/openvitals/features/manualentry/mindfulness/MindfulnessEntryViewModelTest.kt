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
import tech.mmarca.openvitals.core.presentation.ScreenError
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
        // At rest the save command is idle: nothing saving, nothing completed, no error.
        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.writeError)
    }

    @Test fun `initial state seeds fields from persisted timer config`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(
                config = MindfulnessTimerConfig(
                    durationMinutes = 12,
                    intervalMinutes = 3,
                    bellSound = MindfulnessBellSound.TEMPLE,
                    backgroundSound = MindfulnessBackgroundSound.CHIMES,
                )
            ),
        )
        advanceUntilIdle()

        assertEquals("12", vm.uiState.value.durationMinutesText)
        assertTrue(vm.uiState.value.intervalEnabled)
        assertEquals("3", vm.uiState.value.intervalMinutesText)
        assertEquals(MindfulnessBellSound.TEMPLE, vm.uiState.value.bellSound)
        assertEquals(MindfulnessBackgroundSound.CHIMES, vm.uiState.value.backgroundSound)
        assertEquals(12 * 60, vm.uiState.value.totalSeconds)
        assertEquals(12 * 60, vm.uiState.value.remainingSeconds)
    }

    @Test fun `initial load marks mindfulness unavailable without write permissions`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(
                canWrite = false,
                available = false,
                writePermissions = emptySet(),
            ),
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingPermission)
        assertFalse(vm.uiState.value.mindfulnessAvailable)
        assertFalse(vm.uiState.value.canWrite)
        assertEquals(emptySet<String>(), vm.uiState.value.writePermissions)
        assertEquals(MindfulnessEntryError.UNAVAILABLE, vm.uiState.value.entryError)
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

    @Test fun `non-positive duration cannot start timer`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateDurationMinutes("0")
        vm.startTimer()

        assertEquals(MindfulnessEntryError.INVALID_TIMER, vm.uiState.value.entryError)
        assertFalse(vm.uiState.value.isTimerRunning)
    }

    @Test fun `interval at or above duration is rejected`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateDurationMinutes("5")
        vm.updateIntervalEnabled(true)
        vm.updateIntervalMinutes("5")
        vm.startTimer()

        assertEquals(MindfulnessEntryError.INVALID_TIMER, vm.uiState.value.entryError)
        assertFalse(vm.uiState.value.isTimerRunning)
    }

    @Test fun `timer fields are frozen while timer runs`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateDurationMinutes("1")
        vm.startTimer()

        vm.updateDurationMinutes("99")
        vm.updateBellSound(MindfulnessBellSound.HARMONY)

        assertEquals("1", vm.uiState.value.durationMinutesText)
        assertEquals(MindfulnessBellSound.STRUCK, vm.uiState.value.bellSound)
        // The frozen bell pick emits no preview either.
        assertNull(vm.uiState.value.bellEvent)
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

        // Re-picking the same bell must still re-ring it, hence a fresh id.
        vm.updateBellSound(MindfulnessBellSound.TEMPLE)
        val repicked = vm.uiState.value.bellEvent
        assertTrue(checkNotNull(repicked).id > checkNotNull(event).id)
    }

    @Test fun `interval bell rings mid-session but not at the end`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        // 2 minutes, bell every minute: one interval bell at 60 s, then the completion bell at 120 s.
        vm.updateDurationMinutes("2")
        vm.updateIntervalEnabled(true)
        vm.updateIntervalMinutes("1")
        vm.startTimer()

        advanceTimeBy(60_000L)
        runCurrent()
        // Bell event ids are a monotonic counter, so the id counts the rings.
        assertEquals(1L, vm.uiState.value.bellEvent?.id)
        assertFalse(vm.uiState.value.timerCompleted)

        advanceTimeBy(60_000L)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.timerCompleted)
        // Exactly two rings in total: the 60-second interval and the completion.
        assertEquals(2L, vm.uiState.value.bellEvent?.id)
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

    @Test fun `manual entry writes trimmed notes and clears the field on save`() = runTest {
        val repository = repo(canWrite = true)
        val vm = MindfulnessEntryViewModel(
            repository = repository,
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateManualMinutes("12")
        vm.updateNotes("  calm morning sit  ")
        vm.addManualEntry()
        advanceUntilIdle()

        coVerify {
            repository.writeMindfulnessSessionEntry(match<MindfulnessSessionWriteRequest> { request ->
                request.notes == "calm morning sit"
            })
        }
        assertEquals("", vm.uiState.value.notesText)
    }

    @Test fun `blank notes are written as null`() = runTest {
        val repository = repo(canWrite = true)
        val vm = MindfulnessEntryViewModel(
            repository = repository,
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateManualMinutes("12")
        vm.updateNotes("   ")
        vm.addManualEntry()
        advanceUntilIdle()

        coVerify {
            repository.writeMindfulnessSessionEntry(match<MindfulnessSessionWriteRequest> { request ->
                request.notes == null
            })
        }
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

        // Ran down and banked the session: not running, not paused, countdown at zero.
        assertFalse(vm.uiState.value.isTimerRunning)
        assertFalse(vm.uiState.value.isTimerPaused)
        assertTrue(vm.uiState.value.timerCompleted)
        assertEquals(0, vm.uiState.value.remainingSeconds)

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
        // Rewound to the configured duration, ready for the next session.
        assertEquals(60, vm.uiState.value.remainingSeconds)

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
                    backgroundSound = MindfulnessBackgroundSound.CHIMES,
                )
            ),
        )
        advanceUntilIdle()

        vm.startTimer()

        // Running state carries the ambient sound the UI loops while in session.
        assertTrue(vm.uiState.value.isTimerRunning)
        assertFalse(vm.uiState.value.isTimerPaused)
        assertFalse(vm.uiState.value.timerCompleted)
        assertEquals(MindfulnessBackgroundSound.CHIMES, vm.uiState.value.backgroundSound)
        assertNull(vm.uiState.value.entryError)

        advanceTimeBy(70_000L)
        runCurrent()
        vm.stopTimer()

        assertFalse(vm.uiState.value.isTimerRunning)
        assertTrue(vm.uiState.value.isTimerPaused)
        assertEquals(50, vm.uiState.value.remainingSeconds)
        val atPause = vm.uiState.value.remainingSeconds

        vm.resumeTimer()

        assertTrue(vm.uiState.value.isTimerRunning)
        assertFalse(vm.uiState.value.isTimerPaused)
        // Resume continues from where it paused; it never rewinds.
        assertTrue(vm.uiState.value.remainingSeconds <= atPause)
    }

    @Test fun `resume on finished countdown is rejected`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateDurationMinutes("5")
        vm.startTimer()
        advanceTimeBy(2_000L)
        runCurrent()
        vm.stopTimer()
        vm.discardTimer()

        vm.resumeTimer()

        // Not paused any more, so resume is a no-op rather than an error.
        assertFalse(vm.uiState.value.isTimerRunning)
        assertNull(vm.uiState.value.entryError)
    }

    @Test fun `discard rewinds timer to configured duration`() = runTest {
        val vm = MindfulnessEntryViewModel(
            repository = repo(canWrite = true),
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateDurationMinutes("3")
        vm.startTimer()
        advanceTimeBy(10_000L)
        runCurrent()
        assertTrue(vm.uiState.value.remainingSeconds < 3 * 60)

        vm.discardTimer()

        assertFalse(vm.uiState.value.isTimerRunning)
        assertFalse(vm.uiState.value.isTimerPaused)
        assertFalse(vm.uiState.value.timerCompleted)
        assertEquals(3 * 60, vm.uiState.value.remainingSeconds)
        assertEquals(3 * 60, vm.uiState.value.totalSeconds)
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

    @Test fun `timer session under a minute is rejected not rounded to zero`() = runTest {
        val repository = repo(canWrite = true)
        val vm = MindfulnessEntryViewModel(
            repository = repository,
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateDurationMinutes("5")
        vm.startTimer()
        advanceTimeBy(30_000L)
        runCurrent()
        vm.stopTimer()

        vm.saveTimerSession()
        advanceUntilIdle()

        assertEquals(MindfulnessEntryError.TIMER_TOO_SHORT, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repository.writeMindfulnessSessionEntry(any()) }
    }

    @Test fun `saving without banked session is a no-op`() = runTest {
        val repository = repo(canWrite = true)
        val vm = MindfulnessEntryViewModel(
            repository = repository,
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.saveTimerSession()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.writeMindfulnessSessionEntry(any()) }
        assertNull(vm.uiState.value.entryError)
    }

    @Test fun `unavailable device reports unavailable on timer save`() = runTest {
        val repository = repo(canWrite = true, available = false)
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
        assertTrue(vm.uiState.value.timerCompleted)

        vm.saveTimerSession()
        advanceUntilIdle()

        // Unavailable, not a permission error.
        assertEquals(MindfulnessEntryError.UNAVAILABLE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repository.writeMindfulnessSessionEntry(any()) }
    }

    @Test fun `missing write permission blocks timer save`() = runTest {
        val repository = repo(canWrite = false)
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
        assertTrue(vm.uiState.value.timerCompleted)

        vm.saveTimerSession()
        advanceUntilIdle()

        assertEquals(MindfulnessEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repository.writeMindfulnessSessionEntry(any()) }
    }

    @Test fun `failed manual save carries failure to the form`() = runTest {
        val repository = repo(canWrite = true).also {
            coEvery { it.writeMindfulnessSessionEntry(any()) } throws
                RuntimeException("the provider hung up")
        }
        val vm = MindfulnessEntryViewModel(
            repository = repository,
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateManualMinutes("20")
        vm.addManualEntry()
        advanceUntilIdle()

        // The failure lands on the form as state, not as an exception.
        assertEquals(MindfulnessEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertEquals(ScreenError.Message("the provider hung up"), vm.uiState.value.writeError)
        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `editing a field clears prior save failure`() = runTest {
        val repository = repo(canWrite = true).also {
            coEvery { it.writeMindfulnessSessionEntry(any()) } throws RuntimeException("boom")
        }
        val vm = MindfulnessEntryViewModel(
            repository = repository,
            preferencesRepository = prefs(),
        )
        advanceUntilIdle()

        vm.updateManualMinutes("20")
        vm.addManualEntry()
        advanceUntilIdle()
        assertEquals(MindfulnessEntryError.WRITE_FAILED, vm.uiState.value.entryError)

        vm.updateManualMinutes("25")

        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.writeError)
        assertFalse(vm.uiState.value.saveCompleted)
    }

    private fun repo(
        canWrite: Boolean = true,
        available: Boolean = true,
        writePermissions: Set<String> = setOf(WriteMindfulnessPermission),
    ): MindfulnessRepository =
        mockk<MindfulnessRepository>().also { repo ->
            every { repo.mindfulnessWritePermissions } returns writePermissions
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
