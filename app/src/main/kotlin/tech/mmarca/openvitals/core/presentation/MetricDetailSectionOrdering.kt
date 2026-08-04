package tech.mmarca.openvitals.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.MetricDetailSectionListState
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState

data class MetricDetailSectionContext(
    val listState: MetricDetailSectionListState,
    val order: List<MetricDetailSectionId>,
    val isEditingSections: Boolean,
    val onMoveSectionToTarget: (MetricDetailSectionId, MetricDetailSectionId) -> Unit,
    val onMoveSection: (MetricDetailSectionId, Int) -> Unit,
)

@Composable
fun rememberMetricDetailSectionOrdering(
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
): MetricDetailSectionContext {
    val sectionOrderViewModel = hiltViewModel<MetricDetailSectionOrderViewModel>()
    val sectionOrder by sectionOrderViewModel.sectionOrder.collectAsStateWithLifecycle()
    val isEditingSections by sectionOrderViewModel.isEditingSections.collectAsStateWithLifecycle()
    val sectionListState = rememberMetricDetailSectionListState()

    LaunchedEffect(isEditingSections) {
        onSectionEditStateChanged(isEditingSections, sectionOrderViewModel::toggleSectionEdit)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isEditingSections) {
                sectionOrderViewModel.toggleSectionEdit()
            }
        }
    }

    return MetricDetailSectionContext(
        listState = sectionListState,
        order = sectionOrder,
        isEditingSections = isEditingSections,
        onMoveSectionToTarget = sectionOrderViewModel::moveSectionToTarget,
        onMoveSection = sectionOrderViewModel::moveSection,
    )
}
