package tech.mmarca.openvitals.core.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.AppBarAction
import tech.mmarca.openvitals.ui.components.DeclareAppBar
import tech.mmarca.openvitals.ui.components.MetricDetailSectionListState
import tech.mmarca.openvitals.ui.components.ScreenAppBar
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState

data class MetricDetailSectionContext(
    val listState: MetricDetailSectionListState,
    val order: List<MetricDetailSectionId>,
    val isEditingSections: Boolean,
    val onMoveSectionToTarget: (MetricDetailSectionId, MetricDetailSectionId) -> Unit,
    val onMoveSection: (MetricDetailSectionId, Int) -> Unit,
)

/**
 * The section order of one metric screen, and the app bar toggle that arranges it.
 *
 * Every screen that orders sections gets the toggle, because the screen declares
 * it. It used to be handed up to `AppNavigation`, which showed it on three routes
 * of the five that sent it: Calories and Nutrition could not be arranged.
 */
@Composable
fun rememberMetricDetailSectionOrdering(): MetricDetailSectionContext {
    val sectionOrderViewModel = hiltViewModel<MetricDetailSectionOrderViewModel>()
    val sectionOrder by sectionOrderViewModel.sectionOrder.collectAsStateWithLifecycle()
    val isEditingSections by sectionOrderViewModel.isEditingSections.collectAsStateWithLifecycle()
    val sectionListState = rememberMetricDetailSectionListState()
    val editingTint = MaterialTheme.colorScheme.primary

    DeclareAppBar(
        remember(isEditingSections, editingTint) {
            ScreenAppBar(
                actions = listOf(
                    AppBarAction(
                        icon = if (isEditingSections) Icons.Outlined.Check else Icons.Outlined.Tune,
                        contentDescription = if (isEditingSections) {
                            R.string.cd_finish_metric_section_editing
                        } else {
                            R.string.cd_edit_metric_sections
                        },
                        tint = if (isEditingSections) editingTint else null,
                        onClick = sectionOrderViewModel::toggleSectionEdit,
                    ),
                ),
            )
        },
    )

    // The drag wiring reaches the list state from here, where it always runs. It was only set
    // from a zero-height item at the top of the list. A lazy list does not compose an item it
    // has scrolled past, so after a scroll restore a drag moved nothing and said nothing.
    SideEffect {
        sectionListState.isEditingSections = isEditingSections
        sectionListState.onMoveSectionToTarget = sectionOrderViewModel::moveSectionToTarget
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
