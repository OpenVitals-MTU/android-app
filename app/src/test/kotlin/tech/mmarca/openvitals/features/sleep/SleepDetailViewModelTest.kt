package tech.mmarca.openvitals.features.sleep

import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SleepDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `initial load fetches selected sleep session`() = runTest {
        val session = sleepSession(id = "sleep-1")
        val repo = mockk<SleepRepository>()
        coEvery { repo.loadSleepSession("sleep-1") } returns session

        val vm = SleepDetailViewModel(repo, "sleep-1")

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(session, vm.uiState.value.session)
        assertNull(vm.uiState.value.error)
        coVerify(exactly = 1) { repo.loadSleepSession("sleep-1") }
    }

    @Test fun `missing sleep session sets not found error`() = runTest {
        val repo = mockk<SleepRepository>()
        coEvery { repo.loadSleepSession("missing") } returns null

        val vm = SleepDetailViewModel(repo, "missing")

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.session)
        assertEquals(ScreenError.NotFound, vm.uiState.value.error)
    }

    @Test fun `blank sleep id fails without calling repository`() = runTest {
        val repo = mockk<SleepRepository>(relaxed = true)

        val vm = SleepDetailViewModel(repo, "")

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.MissingArgument, vm.uiState.value.error)
        coVerify(exactly = 0) { repo.loadSleepSession(any()) }
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<SleepRepository>()
        coEvery { repo.loadSleepSession("sleep-1") } throws RuntimeException("timeout")

        val vm = SleepDetailViewModel(repo, "sleep-1")

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.session)
        assertEquals(ScreenError.Message("timeout"), vm.uiState.value.error)
    }

    private fun sleepSession(id: String) = SleepData(
        id = id,
        title = "Night sleep",
        startTime = Instant.EPOCH,
        endTime = Instant.EPOCH.plusSeconds(28_800),
        durationMs = 28_800_000,
        source = "test",
    )
}
