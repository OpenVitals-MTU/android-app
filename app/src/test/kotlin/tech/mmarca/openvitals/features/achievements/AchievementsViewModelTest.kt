package tech.mmarca.openvitals.features.achievements

import tech.mmarca.openvitals.core.presentation.ScreenError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

class AchievementsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()

    @Test
    fun `catalog includes legacy activity badge set`() {
        assertEquals(62, AchievementDefinitions.size)
        assertEquals(21, AchievementDefinitions.count { it.category == AchievementCategory.DAILY_STEPS })
        assertEquals(18, AchievementDefinitions.count { it.category == AchievementCategory.LIFETIME_DISTANCE })
        assertEquals(14, AchievementDefinitions.count { it.category == AchievementCategory.DAILY_FLOORS })
        assertEquals(9, AchievementDefinitions.count { it.category == AchievementCategory.LIFETIME_FLOORS })
    }

    @Test
    fun `load maps activity history to unlocked achievements`() = runTest {
        val repo = repo(
            listOf(
                DailySteps(
                    date = today.minusDays(1),
                    steps = 12_500L,
                    distanceMeters = 26.0 * 1_609.344,
                    floorsClimbed = 10,
                ),
                DailySteps(
                    date = today,
                    steps = 2_000L,
                    distanceMeters = 1_609.344,
                    floorsClimbed = 490,
                ),
            )
        )

        val vm = AchievementsViewModel(repo, mainDispatcherRule.dispatcherProvider)
        val state = vm.uiState.value

        assertFalse(state.isLoading)
        assertEquals(62, state.badges.size)
        assertTrue(state.badge("boat_shoes").isUnlocked)
        assertTrue(state.badge("sneakers").isUnlocked)
        assertTrue(state.badge("minions_stuart").isUnlocked)
        assertFalse(state.badge("urban_boots").isUnlocked)
        assertTrue(state.badge("marathon").isUnlocked)
        assertFalse(state.badge("penguin_march").isUnlocked)
        assertTrue(state.badge("happy_hill").isUnlocked)
        assertTrue(state.badge("helicopter").isUnlocked)
        assertFalse(state.badge("skydiver").isUnlocked)
        assertEquals(12_500L, state.stats.maxDailySteps)
        assertEquals(500, state.stats.totalFloors)
        assertEquals(2, state.stats.trackedDays)
    }

    @Test
    fun `load requests accessible legacy activity history`() = runTest {
        val repo = repo()

        AchievementsViewModel(repo, mainDispatcherRule.dispatcherProvider)

        coVerify {
            repo.loadDailySteps(LocalDate.of(2009, 1, 1), today)
        }
    }

    @Test
    fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadDailySteps(any(), any()) } throws RuntimeException("timeout")

        val vm = AchievementsViewModel(repo, mainDispatcherRule.dispatcherProvider)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.Message("timeout"), vm.uiState.value.error)
    }

    private fun repo(dailySteps: List<DailySteps> = emptyList()) =
        mockk<ActivityRepository>().also { repo ->
            coEvery { repo.loadDailySteps(any(), any()) } returns dailySteps
        }

    private fun AchievementsUiState.badge(id: String): AchievementProgress =
        badges.first { it.definition.id == id }
}
