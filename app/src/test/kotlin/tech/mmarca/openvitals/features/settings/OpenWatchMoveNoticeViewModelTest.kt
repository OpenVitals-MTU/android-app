package tech.mmarca.openvitals.features.settings

import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.LegacyDeviceDataCleaner
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import tech.mmarca.openvitals.util.MainDispatcherRule

class OpenWatchMoveNoticeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val cleaner = mockk<LegacyDeviceDataCleaner>(relaxed = true)

    private fun viewModel(preferences: FakePreferences) = OpenWatchMoveNoticeViewModel(preferences, cleaner)

    @Test
    fun `an install onboarded before the move sees the notice once`() = runTest {
        val preferences = FakePreferences(onboardingDone = true)

        val vm = viewModel(preferences)

        assertThat(vm.visible.value).isTrue()
    }

    @Test
    fun `a fresh install still in onboarding does not see it`() = runTest {
        val preferences = FakePreferences(onboardingDone = false)

        assertThat(viewModel(preferences).visible.value).isFalse()
    }

    @Test
    fun `dismissing records the flag, hides the notice and cleans up`() = runTest {
        val preferences = FakePreferences(onboardingDone = true)
        val vm = viewModel(preferences)

        vm.onDismissed()

        assertThat(vm.visible.value).isFalse()
        assertThat(preferences.openWatchMoveNoticeSeen).isTrue()
        coVerify(exactly = 1) { cleaner.clean() }
        // The next launch starts hidden.
        assertThat(viewModel(preferences).visible.value).isFalse()
    }

    @Test
    fun `a dismissal of a notice that is not showing does nothing`() = runTest {
        val preferences = FakePreferences(onboardingDone = true, openWatchMoveNoticeSeen = true)
        val vm = viewModel(preferences)

        vm.onDismissed()

        coVerify(exactly = 0) { cleaner.clean() }
    }
}
