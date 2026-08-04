package tech.mmarca.openvitals.features.manualentry.nutrition

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of "re-checks the write permission when the screen resumes" from
 * Flutter's `test/features/manualentry/manual_entry_forms_test.dart`.
 *
 * Granting a Health Connect permission happens in another app. The user comes
 * back to a screen that has been alive the whole time, so nothing re-runs on its
 * own — and a form that keeps saying "permission needed" after the permission
 * was just granted looks broken in exactly the way that makes people stop
 * trying.
 */
class CarbsEntryResumeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aPermissionGrantedWhileAwayIsPickedUpWhenTheScreenComesBack() {
        val repository = FakeNutritionRepository(canWrite = false)
        val viewModel = CarbsEntryViewModel(repository)
        val owner = TestLifecycleOwner()

        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                OpenVitalsTheme {
                    CarbsEntryScreen(viewModel = viewModel, unitFormatter = FORMATTER)
                }
            }
        }
        // Started but not resumed: the screen is showing what it knew when it
        // was built, which is that it cannot write.
        moveTo(owner, Lifecycle.State.STARTED)
        awaitText(string(R.string.carbs_entry_permission_needed))

        // The user leaves, grants the permission, and comes back.
        repository.canWrite = true
        moveTo(owner, Lifecycle.State.RESUMED)

        awaitText(string(R.string.carbs_entry_subtitle))
        composeRule.onNodeWithText(string(R.string.carbs_entry_subtitle)).assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.carbs_entry_permission_needed))
            .assertDoesNotExist()
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun moveTo(owner: TestLifecycleOwner, state: Lifecycle.State) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            owner.registry.currentState = state
        }
    }

    /** A lifecycle the test drives by hand, standing in for the hosting screen. */
    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private class FakeNutritionRepository(var canWrite: Boolean) : NutritionRepository {
        override val nutritionWritePermissions: Set<String> = setOf("write-nutrition")

        override suspend fun hasNutritionWritePermission(): Boolean = canWrite

        override suspend fun loadNutritionPeriod(
            query: PeriodLoadQuery,
            refreshMode: RefreshMode,
        ) = error("unused")

        override suspend fun loadDailyMacros(start: LocalDate, end: LocalDate) = error("unused")

        override suspend fun loadNutritionEntries(start: LocalDate, end: LocalDate) =
            error("unused")

        override suspend fun writeCarbsEntry(request: NutritionWriteRequest): String =
            error("unused")

        override suspend fun writeNutritionEntry(request: NutritionWriteRequest): String =
            error("unused")

        override suspend fun deleteNutritionEntry(id: String) = error("unused")
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L

        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
    }
}
