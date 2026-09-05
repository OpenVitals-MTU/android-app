package tech.mmarca.openvitals.features.vitals

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.heart.HeartDisplayState
import tech.mmarca.openvitals.features.heart.HeartMetricDisplay
import tech.mmarca.openvitals.features.heart.HeartUiState
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** Blood pressure stands in for the vitals metrics: they share one content builder. */
class VitalsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bloodPressureShowsThePlaceholderWithNoReadings() {
        setContent(state(entries = emptyList()))

        composeRule.onNodeWithText(string(R.string.message_no_blood_pressure)).assertIsDisplayed()
    }

    @Test
    fun bloodPressureRendersItsSectionsOnceLoaded() {
        setContent(state(entries = listOf(reading())))

        composeRule.onNodeWithText(string(R.string.message_no_blood_pressure)).assertDoesNotExist()
    }

    private fun state(entries: List<BloodPressureEntry>) = HeartUiState(
        isLoading = false,
        selectedRange = TimeRange.WEEK,
        selectedDate = ANCHOR,
        bloodPressure = entries,
        display = HeartDisplayState(
            selectedPeriod = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
            metric = HeartMetricDisplay(
                hasData = entries.isNotEmpty(),
                hasVitalsEntries = entries.isNotEmpty(),
            ),
        ),
    )

    private fun reading() = BloodPressureEntry(
        time = ANCHOR.atStartOfDay(ZoneId.systemDefault()).plusHours(8).toInstant(),
        systolicMmHg = 118,
        diastolicMmHg = 76,
        source = "tech.mmarca.openvitals",
    )

    private fun setContent(state: HeartUiState) {
        composeRule.setContent {
            OpenVitalsTheme {
                val sectionContext = MetricDetailSectionContext(
                    listState = rememberMetricDetailSectionListState(),
                    order = DefaultMetricDetailSectionOrder,
                    isEditingSections = false,
                    onMoveSectionToTarget = { _, _ -> },
                    onMoveSection = { _, _ -> },
                )
                LazyColumn {
                    bloodPressureContent(
                        state = state,
                        period = state.display.selectedPeriod,
                        unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        sectionContext = sectionContext,
                        onEditVitalsMeasurement = { _, _ -> },
                        onDeleteVitalsMeasurement = { _, _ -> },
                    )
                }
            }
        }
    }

    private companion object {
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
    }
}
