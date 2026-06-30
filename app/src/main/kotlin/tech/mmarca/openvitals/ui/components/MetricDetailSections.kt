package tech.mmarca.openvitals.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

private const val MetricSectionEditWiggleDegrees = 0.35f

val LocalMetricSectionEditMode = staticCompositionLocalOf { false }

@Composable
fun metricSectionEditModeActive(): Boolean = LocalMetricSectionEditMode.current

class MetricDetailSectionBuilder {
    internal val sections = linkedMapOf<MetricDetailSectionId, @Composable () -> Unit>()

    fun section(
        id: MetricDetailSectionId,
        visible: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        if (visible) {
            sections[id] = content
        }
    }
}

class MetricDetailSectionListState(
    val lazyListState: LazyListState,
) {
    internal var isEditingSections by mutableStateOf(false)
    internal var onMoveSectionToTarget: ((MetricDetailSectionId, MetricDetailSectionId) -> Unit)? = null
    internal var reorderableState by mutableStateOf<ReorderableLazyListState?>(null)
}

@Composable
fun rememberMetricDetailSectionListState(): MetricDetailSectionListState {
    val lazyListState = rememberLazyListState()
    return remember(lazyListState) { MetricDetailSectionListState(lazyListState) }
}

fun LazyListScope.renderOrderedMetricDetailSections(
    sectionContext: MetricDetailSectionContext,
    builder: MetricDetailSectionBuilder.() -> Unit,
) {
    orderedMetricDetailSections(
        listState = sectionContext.listState,
        order = sectionContext.order,
        isEditingSections = sectionContext.isEditingSections,
        onMoveSectionToTarget = sectionContext.onMoveSectionToTarget,
        onMoveSection = sectionContext.onMoveSection,
        builder = builder,
    )
}

fun LazyListScope.orderedMetricDetailSections(
    listState: MetricDetailSectionListState,
    order: List<MetricDetailSectionId>,
    isEditingSections: Boolean,
    onMoveSectionToTarget: (MetricDetailSectionId, MetricDetailSectionId) -> Unit,
    onMoveSection: (MetricDetailSectionId, Int) -> Unit,
    builder: MetricDetailSectionBuilder.() -> Unit,
) {
    val sectionBuilder = MetricDetailSectionBuilder().apply(builder)
    val visibleOrder = order.filter { it in sectionBuilder.sections }

    item(key = "metric_section_drag_config") {
        SideEffect {
            listState.isEditingSections = isEditingSections
            listState.onMoveSectionToTarget = onMoveSectionToTarget
        }
    }

    visibleOrder.forEachIndexed { index, sectionId ->
        item(key = sectionId) {
            val reorderableState = listState.reorderableState
            if (reorderableState != null) {
                ReorderableItem(
                    state = reorderableState,
                    key = sectionId,
                    enabled = isEditingSections,
                ) { isDragging ->
                    ReorderableMetricDetailSection(
                        sectionId = sectionId,
                        isDragging = isDragging,
                        isEditingSections = isEditingSections,
                        dragHandleModifier = Modifier.longPressDraggableHandle(
                            enabled = isEditingSections,
                        ),
                        onMovePrevious = if (index > 0) {
                            { onMoveSection(sectionId, -1) }
                        } else {
                            null
                        },
                        onMoveNext = if (index < visibleOrder.lastIndex) {
                            { onMoveSection(sectionId, 1) }
                        } else {
                            null
                        },
                    ) {
                        sectionBuilder.sections.getValue(sectionId).invoke()
                    }
                }
            } else {
                ReorderableMetricDetailSection(
                    sectionId = sectionId,
                    isDragging = false,
                    isEditingSections = isEditingSections,
                    dragHandleModifier = Modifier,
                    onMovePrevious = if (index > 0) {
                        { onMoveSection(sectionId, -1) }
                    } else {
                        null
                    },
                    onMoveNext = if (index < visibleOrder.lastIndex) {
                        { onMoveSection(sectionId, 1) }
                    } else {
                        null
                    },
                ) {
                    sectionBuilder.sections.getValue(sectionId).invoke()
                }
            }
        }
    }
}

@Composable
private fun ReorderableMetricDetailSection(
    sectionId: MetricDetailSectionId,
    isDragging: Boolean,
    isEditingSections: Boolean,
    dragHandleModifier: Modifier,
    onMovePrevious: (() -> Unit)?,
    onMoveNext: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val wiggleRotation = if (isEditingSections) {
        val wiggleTransition = rememberInfiniteTransition(label = "MetricSectionWiggle")
        val rotation by wiggleTransition.animateFloat(
            initialValue = -MetricSectionEditWiggleDegrees,
            targetValue = MetricSectionEditWiggleDegrees,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 140,
                    delayMillis = (sectionId.ordinal % 4) * 35,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "MetricSectionWiggleRotation",
        )
        rotation
    } else {
        0f
    }
    val movePreviousLabel = stringResource(R.string.cd_move_section_up)
    val moveNextLabel = stringResource(R.string.cd_move_section_down)

    val editSemanticsModifier = if (isEditingSections) {
        Modifier.semantics {
            contentDescription = sectionId.name
            customActions = buildList {
                onMovePrevious?.let { action ->
                    add(
                        CustomAccessibilityAction(movePreviousLabel) {
                            action()
                            true
                        },
                    )
                }
                onMoveNext?.let { action ->
                    add(
                        CustomAccessibilityAction(moveNextLabel) {
                            action()
                            true
                        },
                    )
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                rotationZ = if (isEditingSections && !isDragging) wiggleRotation else 0f
                scaleX = if (isDragging) 1.01f else 1f
                scaleY = if (isDragging) 1.01f else 1f
                shadowElevation = if (isDragging) 12.dp.toPx() else 0f
            }
            .then(editSemanticsModifier)
            .then(dragHandleModifier)
            .then(
                if (isEditingSections) {
                    Modifier
                        .padding(horizontal = 4.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.medium,
                        )
                } else {
                    Modifier
                },
            ),
    ) {
        CompositionLocalProvider(LocalMetricSectionEditMode provides isEditingSections) {
            content()
            if (isEditingSections && !isDragging) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { /* block card taps while reordering */ })
                        },
                )
            }
        }
    }
}
