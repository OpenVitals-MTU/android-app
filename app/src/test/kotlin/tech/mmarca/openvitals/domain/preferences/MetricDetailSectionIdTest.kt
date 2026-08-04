package tech.mmarca.openvitals.domain.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class MetricDetailSectionIdTest {

    @Test
    fun metricDetailSectionOrderFromStored_returnsDefaultWhenNull() {
        assertEquals(DefaultMetricDetailSectionOrder, metricDetailSectionOrderFromStored(null))
    }

    @Test
    fun metricDetailSectionOrderFromStored_mergesMissingSections() {
        val stored = listOf(
            MetricDetailSectionId.ENTRIES.name,
            MetricDetailSectionId.STATISTICS.name,
        )

        val order = metricDetailSectionOrderFromStored(stored)

        assertEquals(MetricDetailSectionId.ENTRIES, order.first())
        assertEquals(MetricDetailSectionId.STATISTICS, order[1])
        assertEquals(DefaultMetricDetailSectionOrder.size, order.size)
        assertEquals(DefaultMetricDetailSectionOrder.toSet(), order.toSet())
    }

    @Test
    fun metricDetailSectionOrderFromStored_ignoresUnknownValues() {
        val order = metricDetailSectionOrderFromStored(listOf("UNKNOWN", MetricDetailSectionId.DAILY_GOAL.name))

        assertEquals(MetricDetailSectionId.DAILY_GOAL, order.first())
        assertEquals(DefaultMetricDetailSectionOrder.size, order.size)
    }
}
