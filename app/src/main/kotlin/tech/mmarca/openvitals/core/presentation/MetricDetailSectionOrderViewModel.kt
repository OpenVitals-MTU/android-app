package tech.mmarca.openvitals.core.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.domain.preferences.metricDetailSectionOrderFromStored
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class MetricDetailSectionOrderViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _sectionOrder = MutableStateFlow(
        metricDetailSectionOrderFromStored(preferencesRepository.metricDetailSectionOrder()),
    )
    val sectionOrder: StateFlow<List<MetricDetailSectionId>> = _sectionOrder.asStateFlow()

    private val _isEditingSections = MutableStateFlow(false)
    val isEditingSections: StateFlow<Boolean> = _isEditingSections.asStateFlow()

    fun toggleSectionEdit() {
        _isEditingSections.value = !_isEditingSections.value
    }

    fun moveSectionToTarget(sectionId: MetricDetailSectionId, targetSectionId: MetricDetailSectionId) {
        val current = _sectionOrder.value
        val fromIndex = current.indexOf(sectionId)
        val targetIndex = current.indexOf(targetSectionId)
        if (fromIndex == -1 || targetIndex == -1 || fromIndex == targetIndex) return

        updateSectionOrder(
            current.toMutableList().apply {
                removeAt(fromIndex)
                add(targetIndex, sectionId)
            },
        )
    }

    fun moveSection(sectionId: MetricDetailSectionId, offset: Int) {
        val current = _sectionOrder.value
        val fromIndex = current.indexOf(sectionId)
        if (fromIndex == -1) return

        val toIndex = (fromIndex + offset).coerceIn(current.indices)
        if (fromIndex == toIndex) return

        updateSectionOrder(
            current.toMutableList().apply {
                removeAt(fromIndex)
                add(toIndex, sectionId)
            },
        )
    }

    private fun updateSectionOrder(order: List<MetricDetailSectionId>) {
        preferencesRepository.setMetricDetailSectionOrder(order.map { it.name })
        _sectionOrder.value = order
    }
}
