package tech.mmarca.openvitals.features.caffeine

import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import java.time.LocalDate
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.CaffeineRepository
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeinePeriodData
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.AppBarState
import tech.mmarca.openvitals.ui.components.LocalAppBarState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * This screen reads the entry list the caffeine screen loaded. An id no longer in that list
 * must say "no data" rather than show someone else's drink.
 */
class CaffeineDrinkScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appBarState = AppBarState()

    /** The app bar reads a screen's declaration by the destination it belongs to. */
    private val destination = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    private fun declaredTitle(): String? = appBarState.of(destination)?.title

    @Test
    fun theScreenShowsTheDrinkItWasOpenedForByNameAndDose() {
        setScreen(entryId = FLAT_WHITE.id)

        // The screen declares its own app bar title.
        composeRule.waitUntil(TIMEOUT_MS) { declaredTitle() == FLAT_WHITE.name }
        // The dose is the whole reason someone taps a row.
        val dose = FORMATTER.count(FLAT_WHITE.caffeineMg.toInt())
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(dose, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun aDrinkDeletedWhileItsScreenWasOpenDegradesToNoData() {
        setScreen(entryId = "gone-drink")

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(string(R.string.no_data))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        // Falling back to the first drink would have the user read a stranger's dose.
        composeRule.onNodeWithText(FLAT_WHITE.name!!).assertDoesNotExist()
        // No name to show: the app bar keeps the route's own title.
        assertNull(declaredTitle())
    }

    private fun setScreen(entryId: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = PreferencesRepository(context)
        val viewModel = CaffeineViewModel(
            repository = FakeCaffeineRepository(listOf(FLAT_WHITE)),
            caffeineModel = preferences,
            bodyProfilePreferences = preferences,
            nutritionRepository = UnusedNutritionRepository,
        )
        composeRule.setContent {
            CompositionLocalProvider(
                LocalAppBarState provides appBarState,
                LocalViewModelStoreOwner provides destination,
            ) {
                OpenVitalsTheme {
                    CaffeineDrinkScreen(
                        viewModel = viewModel,
                        entryId = entryId,
                        unitFormatter = FORMATTER,
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    )
                }
            }
        }
    }

    private class FakeCaffeineRepository(
        private val entries: List<CaffeineEntry>,
    ) : CaffeineRepository {
        override suspend fun loadCaffeineData(
            period: DatePeriod,
            refreshMode: RefreshMode,
        ): CaffeinePeriodData = CaffeinePeriodData(entries = entries)
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L

        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        /** A fixed morning, so the profile never depends on when the suite runs. */
        val DRANK_AT: Instant = Instant.parse("2026-06-23T08:00:00Z")

        val FLAT_WHITE = CaffeineEntry(
            id = "drink-1",
            startTime = DRANK_AT,
            endTime = DRANK_AT,
            caffeineMg = 128.0,
            name = "Flat white",
            source = "Test source",
            mealType = 0,
        )
    }
}

/** The drink screen never deletes here, so no nutrition record is ever touched. */
private object UnusedNutritionRepository : NutritionRepository {
    override val nutritionWritePermissions: Set<String> = emptySet()

    override suspend fun hasNutritionWritePermission(): Boolean = false

    override suspend fun loadNutritionPeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode,
    ) = error("unused")

    override suspend fun loadDailyMacros(start: LocalDate, end: LocalDate) = error("unused")

    override suspend fun loadNutritionEntries(start: LocalDate, end: LocalDate) = error("unused")

    override suspend fun writeCarbsEntry(request: NutritionWriteRequest): String = error("unused")

    override suspend fun writeNutritionEntry(request: NutritionWriteRequest): String = error("unused")

    override suspend fun deleteNutritionEntry(id: String) = error("unused")
}
