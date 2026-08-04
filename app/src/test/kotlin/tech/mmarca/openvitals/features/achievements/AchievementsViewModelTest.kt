package tech.mmarca.openvitals.features.achievements

import tech.mmarca.openvitals.core.presentation.ScreenError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `an empty history unlocks nothing and reports no activity`() = runTest {
        val vm = AchievementsViewModel(repo(), mainDispatcherRule.dispatcherProvider)
        val state = vm.uiState.value

        assertFalse(state.isLoading)
        assertTrue("the catalog still renders", state.badges.isNotEmpty())
        assertEquals(0, state.unlockedCount)
        assertEquals(0f, state.completionRatio, 0f)
        assertFalse(state.hasActivityHistory)
        assertFalse(state.hasFloorHistory)
        assertEquals(0, state.stats.trackedDays)
        assertEquals(0L, state.stats.maxDailySteps)
        assertEquals(0.0, state.stats.totalDistanceMeters, 0.0)
        for (badge in state.badges) {
            assertFalse(badge.definition.name, badge.isUnlocked)
            assertEquals(badge.definition.name, 0f, badge.progressRatio, 0f)
            assertNull(badge.definition.name, badge.achievedOn)
            assertEquals(badge.definition.name, 0, badge.timesEarned)
        }
    }

    @Test
    fun `the stats aggregate the whole window`() = runTest {
        val repo = repo(
            listOf(
                DailySteps(date = today.minusDays(1), steps = 12_000L, distanceMeters = 8_000.0, floorsClimbed = 12),
                DailySteps(date = today.minusDays(3), steps = 5_000L, distanceMeters = 3_000.0),
                // A blank day is not a tracked one.
                DailySteps(date = today.minusDays(2), steps = 0L, distanceMeters = 0.0),
            )
        )

        val vm = AchievementsViewModel(repo, mainDispatcherRule.dispatcherProvider)
        val stats = vm.uiState.value.stats

        assertEquals(2, stats.trackedDays)
        assertEquals(12_000L, stats.maxDailySteps)
        assertEquals(11_000.0, stats.totalDistanceMeters, 1e-9)
        assertEquals(12, stats.maxDailyFloors)
        assertEquals(12, stats.totalFloors)
        assertTrue(stats.hasFloorData)
        assertEquals(LocalDate.of(2009, 1, 1), stats.startDate)
        assertEquals(today, stats.endDate)
    }

    @Test
    fun `a badge is earned on the first day that reaches its target`() = runTest {
        val repo = repo(
            listOf(
                DailySteps(date = today.minusDays(2), steps = 4_000L, distanceMeters = 0.0),
                DailySteps(date = today.minusDays(1), steps = 12_000L, distanceMeters = 0.0),
                DailySteps(date = today, steps = 9_000L, distanceMeters = 0.0),
            )
        )

        val vm = AchievementsViewModel(repo, mainDispatcherRule.dispatcherProvider)
        val state = vm.uiState.value

        // Boat Shoes is the 5k daily-steps badge.
        val boatShoes = state.badge("boat_shoes")
        assertTrue(boatShoes.isUnlocked)
        assertEquals(12_000.0, boatShoes.currentValue, 0.0)
        assertEquals(1f, boatShoes.progressRatio, 0f)
        // Earned on both the 12k and the 9k day, first reached on the 12k one.
        assertEquals(2, boatShoes.timesEarned)
        assertEquals(today.minusDays(1), boatShoes.achievedOn)

        assertTrue(state.unlockedCount > 0)
        assertEquals(state.unlockedCount.toFloat() / state.totalCount, state.completionRatio, 1e-9f)
    }

    @Test
    fun `a locked badge carries its partial progress, clamped`() = runTest {
        val repo = repo(listOf(DailySteps(date = today, steps = 5_000L, distanceMeters = 0.0)))

        val vm = AchievementsViewModel(repo, mainDispatcherRule.dispatcherProvider)
        val state = vm.uiState.value

        // Sneakers is the 10k daily-steps badge: half way there.
        val sneakers = state.badge("sneakers")
        assertFalse(sneakers.isUnlocked)
        assertEquals(0.5f, sneakers.progressRatio, 1e-9f)
        assertNull(sneakers.achievedOn)
        // Every ratio stays clamped to the unit interval.
        for (badge in state.badges) {
            assertTrue(badge.definition.name, badge.progressRatio in 0f..1f)
        }
    }

    @Test
    fun `the category filter is precomputed per chip`() = runTest {
        val repo = repo(listOf(DailySteps(date = today, steps = 5_000L, distanceMeters = 0.0)))

        val vm = AchievementsViewModel(repo, mainDispatcherRule.dispatcherProvider)
        val state = vm.uiState.value

        // "All" is the whole list; a chip is exactly its category's badges.
        assertEquals(state.badges, state.badgesFor(null))
        for (category in AchievementCategory.values()) {
            assertEquals(
                category.name,
                state.badges.filter { it.definition.category == category },
                state.badgesFor(category),
            )
        }
    }

    @Test
    fun `a history with no floor data leaves the floor badges unearned`() = runTest {
        val repo = repo(
            listOf(DailySteps(date = today, steps = 20_000L, distanceMeters = 15_000.0))
        )

        val vm = AchievementsViewModel(repo, mainDispatcherRule.dispatcherProvider)
        val state = vm.uiState.value

        assertFalse(state.hasFloorHistory)
        assertEquals(0, state.stats.maxDailyFloors)
        for (badge in state.badges.filter { it.definition.category == AchievementCategory.DAILY_FLOORS }) {
            assertFalse(badge.definition.name, badge.isUnlocked)
        }
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
