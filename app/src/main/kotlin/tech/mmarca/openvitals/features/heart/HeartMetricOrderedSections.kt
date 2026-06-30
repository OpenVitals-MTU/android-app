package tech.mmarca.openvitals.features.heart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.MetricDetailSectionBuilder
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
import java.time.LocalDate

internal fun LazyListScope.renderHeartMetricSections(
    sectionContext: MetricDetailSectionContext,
    builder: MetricDetailSectionBuilder.() -> Unit,
) {
    renderOrderedMetricDetailSections(sectionContext, builder)
}

internal fun LazyListScope.renderChartMetricSections(
    sectionContext: MetricDetailSectionContext,
    selectedRange: TimeRange,
    period: DatePeriod,
    selectedDate: LocalDate?,
    intradayChart: (@Composable () -> Unit)? = null,
    periodChart: (@Composable () -> Unit)? = null,
    highlightCard: (@Composable () -> Unit)? = null,
    selectedDayEntries: (@Composable () -> Unit)? = null,
    dataConfidence: (@Composable () -> Unit)? = null,
    contextInsight: (@Composable () -> Unit)? = null,
    statistics: (@Composable () -> Unit)? = null,
    entries: (@Composable () -> Unit)? = null,
) {
    renderHeartMetricSections(sectionContext) {
        intradayChart?.let { chart ->
            section(MetricDetailSectionId.INTRADAY_CHART, selectedRange == TimeRange.DAY) {
                chart()
            }
        }
        periodChart?.let { chart ->
            section(MetricDetailSectionId.PERIOD_CHART, selectedRange != TimeRange.DAY || intradayChart == null) {
                chart()
            }
        }
        highlightCard?.let { card ->
            section(MetricDetailSectionId.DAILY_GOAL) {
                card()
            }
        }
        selectedDayEntries?.let { content ->
            section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, selectedDate != null) {
                content()
            }
        }
        dataConfidence?.let { content ->
            section(MetricDetailSectionId.DATA_CONFIDENCE, period.start != period.end) {
                content()
            }
        }
        contextInsight?.let { content ->
            section(MetricDetailSectionId.DAILY_GOAL, highlightCard == null) {
                content()
            }
        }
        statistics?.let { content ->
            section(MetricDetailSectionId.STATISTICS) {
                content()
            }
        }
        entries?.let { content ->
            section(MetricDetailSectionId.ENTRIES) {
                content()
            }
        }
    }
}

internal fun LazyListScope.renderDayOrPeriodChartSections(
    sectionContext: MetricDetailSectionContext,
    selectedRange: TimeRange,
    period: DatePeriod,
    selectedDate: LocalDate?,
    dayChart: (@Composable () -> Unit)? = null,
    periodChart: (@Composable () -> Unit)? = null,
    highlightCard: (@Composable () -> Unit)? = null,
    selectedDayEntries: (@Composable () -> Unit)? = null,
    dataConfidence: (@Composable () -> Unit)? = null,
    contextInsight: (@Composable () -> Unit)? = null,
    statistics: (@Composable () -> Unit)? = null,
    entries: (@Composable () -> Unit)? = null,
) {
    renderChartMetricSections(
        sectionContext = sectionContext,
        selectedRange = selectedRange,
        period = period,
        selectedDate = selectedDate,
        intradayChart = dayChart,
        periodChart = periodChart,
        highlightCard = highlightCard,
        selectedDayEntries = selectedDayEntries,
        dataConfidence = dataConfidence,
        contextInsight = contextInsight,
        statistics = statistics,
        entries = entries,
    )
}
