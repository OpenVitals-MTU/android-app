package tech.mmarca.openvitals.core.presentation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId

class MetricDetailSectionOrderViewModelTest {

    @Test
    fun moveSectionToTarget_reordersAndPersists() {
        val prefs = mockk<PreferencesRepository>(relaxed = true) {
            every { metricDetailSectionOrder() } returns listOf(
                MetricDetailSectionId.DAILY_GOAL.name,
                MetricDetailSectionId.STATISTICS.name,
                MetricDetailSectionId.ENTRIES.name,
            )
        }
        val viewModel = MetricDetailSectionOrderViewModel(prefs)

        viewModel.moveSectionToTarget(
            MetricDetailSectionId.ENTRIES,
            MetricDetailSectionId.DAILY_GOAL,
        )

        assertEquals(
            listOf(
                MetricDetailSectionId.ENTRIES,
                MetricDetailSectionId.DAILY_GOAL,
                MetricDetailSectionId.STATISTICS,
            ),
            viewModel.sectionOrder.value.filter {
                it in setOf(
                    MetricDetailSectionId.DAILY_GOAL,
                    MetricDetailSectionId.STATISTICS,
                    MetricDetailSectionId.ENTRIES,
                )
            },
        )
        verify {
            prefs.setMetricDetailSectionOrder(
                viewModel.sectionOrder.value.map { it.name },
            )
        }
    }

    @Test
    fun toggleSectionEdit_switchesEditingState() {
        val prefs = mockk<PreferencesRepository>(relaxed = true) {
            every { metricDetailSectionOrder() } returns null
        }
        val viewModel = MetricDetailSectionOrderViewModel(prefs)

        assertFalse(viewModel.isEditingSections.value)
        viewModel.toggleSectionEdit()
        assertTrue(viewModel.isEditingSections.value)
    }

    @Test
    fun initialOrder_usesDefaultWhenPreferencesMissing() {
        val prefs = mockk<PreferencesRepository>(relaxed = true) {
            every { metricDetailSectionOrder() } returns null
        }
        val viewModel = MetricDetailSectionOrderViewModel(prefs)

        assertEquals(DefaultMetricDetailSectionOrder, viewModel.sectionOrder.value)
    }
}
