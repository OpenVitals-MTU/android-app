package tech.mmarca.openvitals.features.caffeine

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import org.junit.Assert.assertEquals
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
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * This screen reads the entry list the caffeine screen loaded. An id no longer in that list
 * must say "no data" rather than show someone else's drink.
 */
class CaffeineDrinkScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theScreenShowsTheDrinkItWasOpenedForByNameAndDose() {
        val titles = mutableListOf<String?>()
        setScreen(entryId = FLAT_WHITE.id, onTitleChanged = { titles += it })

        // The name reaches the toolbar through the caller.
        composeRule.waitUntil(TIMEOUT_MS) { titles.contains(FLAT_WHITE.name) }
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
        val titles = mutableListOf<String?>()
        setScreen(entryId = "gone-drink", onTitleChanged = { titles += it })

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(string(R.string.no_data))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        // Falling back to the first drink would have the user read a stranger's dose.
        composeRule.onNodeWithText(FLAT_WHITE.name!!).assertDoesNotExist()
        assertEquals(listOf<String?>(null), titles.distinct())
    }

    private fun setScreen(entryId: String, onTitleChanged: (String?) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = CaffeineViewModel(
            repository = FakeCaffeineRepository(listOf(FLAT_WHITE)),
            preferencesRepository = PreferencesRepository(context),
        )
        composeRule.setContent {
            OpenVitalsTheme {
                CaffeineDrinkScreen(
                    viewModel = viewModel,
                    entryId = entryId,
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    onTitleChanged = onTitleChanged,
                )
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
