package tech.mmarca.openvitals.features.body

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
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The body overview has no single `hasData` flag: emptiness is decided across every tracked
 * measurement, so a single weight must count as data.
 */
class BodyContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anEmptyPeriodRendersTheBodyPlaceholder() {
        setContent(state())

        composeRule.onNodeWithText(string(R.string.message_no_readings_period)).assertIsDisplayed()
    }

    @Test
    fun oneTrackedMeasurementIsEnoughToCountAsData() {
        // A single weight has to be enough.
        setContent(state(weights = listOf(weight())))

        composeRule.onNodeWithText(string(R.string.message_no_readings_period)).assertDoesNotExist()
    }

    @Test
    fun aLoadingPeriodDoesNotClaimThereIsNothingToShow() {
        setContent(state(isLoading = true))

        composeRule.onNodeWithText(string(R.string.message_no_readings_period)).assertDoesNotExist()
    }

    private fun state(
        isLoading: Boolean = false,
        weights: List<WeightEntry> = emptyList(),
    ) = BodyUiState(
        isLoading = isLoading,
        selectedRange = TimeRange.MONTH,
        selectedDate = ANCHOR,
        weightEntries = weights,
    )

    private fun weight() = WeightEntry(
        id = "weight-1",
        time = ANCHOR.atStartOfDay(ZoneId.systemDefault()).plusHours(7).toInstant(),
        weightKg = 72.5,
        source = "tech.mmarca.openvitals",
        isOpenVitalsEntry = true,
    )

    private fun setContent(state: BodyUiState) {
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
                    bodyContent(
                        state = state,
                        period = DatePeriod(ANCHOR.withDayOfMonth(1), ANCHOR),
                        unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        chartDaySelection = ChartDaySelection(selectedDate = null, onDateSelected = {}),
                        sectionContext = sectionContext,
                        onEditBodyMeasurement = { _, _ -> },
                        onDeleteBodyMeasurement = { _, _ -> },
                    )
                }
            }
        }
    }

    private companion object {
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
    }
}
