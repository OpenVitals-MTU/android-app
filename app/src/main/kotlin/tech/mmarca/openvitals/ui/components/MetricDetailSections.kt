package tech.mmarca.openvitals.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import kotlinx.coroutines.delay

private const val MetricSectionDragLongPressMillis = 500L
private const val MetricSectionEditWiggleDegrees = 0.35f
private const val MetricSectionEdgeScrollDelayMillis = 16L
private val MetricSectionEdgeScrollThreshold = 72.dp
private val MetricSectionEdgeScrollSpeed = 20.dp

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
    internal val sectionBounds = mutableStateMapOf<MetricDetailSectionId, Rect>()
    internal var draggingSectionId by mutableStateOf<MetricDetailSectionId?>(null)
    internal var dragOffset by mutableStateOf(Offset.Zero)
    internal var viewportBounds by mutableStateOf<Rect?>(null)
}

@Composable
fun rememberMetricDetailSectionListState(): MetricDetailSectionListState {
    val lazyListState = rememberLazyListState()
    return remember(lazyListState) { MetricDetailSectionListState(lazyListState) }
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
    val sectionBounds = listState.sectionBounds

    item(key = "metric_section_bounds_sync") {
        LaunchedEffect(visibleOrder) {
            val visibleSet = visibleOrder.toSet()
            sectionBounds.keys.toList().forEach { sectionId ->
                if (sectionId !in visibleSet) {
                    sectionBounds.remove(sectionId)
                }
            }
        }
    }

    item(key = "metric_section_edge_scroll") {
        MetricDetailSectionEdgeScrollEffect(listState = listState)
    }

    visibleOrder.forEachIndexed { index, sectionId ->
        item(key = sectionId.name) {
            ReorderableMetricDetailSection(
                sectionId = sectionId,
                listState = listState,
                isEditingSections = isEditingSections,
                sectionBounds = sectionBounds,
                onPositioned = { bounds -> sectionBounds[sectionId] = bounds },
                onMoveSectionToTarget = onMoveSectionToTarget,
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

@Composable
private fun MetricDetailSectionEdgeScrollEffect(listState: MetricDetailSectionListState) {
    val density = LocalDensity.current
    val edgeScrollThresholdPx = with(density) { MetricSectionEdgeScrollThreshold.toPx() }
    val edgeScrollSpeedPx = with(density) { MetricSectionEdgeScrollSpeed.toPx() }
    val draggingSectionId = listState.draggingSectionId

    LaunchedEffect(draggingSectionId) {
        if (draggingSectionId == null) return@LaunchedEffect

        while (listState.draggingSectionId != null) {
            val currentDraggingId = listState.draggingSectionId ?: break
            val draggedBounds = listState.sectionBounds[currentDraggingId]
            val viewport = listState.viewportBounds
            val dragOffset = listState.dragOffset
            if (draggedBounds != null && viewport != null) {
                val draggedCenterY = draggedBounds.center.y + dragOffset.y
                when {
                    draggedCenterY <= viewport.top + edgeScrollThresholdPx &&
                        listState.lazyListState.canScrollBackward -> {
                        listState.lazyListState.scrollBy(-edgeScrollSpeedPx)
                    }
                    draggedCenterY >= viewport.bottom - edgeScrollThresholdPx &&
                        listState.lazyListState.canScrollForward -> {
                        listState.lazyListState.scrollBy(edgeScrollSpeedPx)
                    }
                }
            }
            delay(MetricSectionEdgeScrollDelayMillis)
        }
    }
}

@Composable
private fun ReorderableMetricDetailSection(
    sectionId: MetricDetailSectionId,
    listState: MetricDetailSectionListState,
    isEditingSections: Boolean,
    sectionBounds: Map<MetricDetailSectionId, Rect>,
    onPositioned: (Rect) -> Unit,
    onMoveSectionToTarget: (MetricDetailSectionId, MetricDetailSectionId) -> Unit,
    onMovePrevious: (() -> Unit)?,
    onMoveNext: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    var dragOffset by remember(sectionId, isEditingSections) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(sectionId, isEditingSections) { mutableStateOf(false) }
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
    val viewConfiguration = LocalViewConfiguration.current
    val dragViewConfiguration = remember(viewConfiguration) {
        object : androidx.compose.ui.platform.ViewConfiguration by viewConfiguration {
            override val longPressTimeoutMillis: Long = MetricSectionDragLongPressMillis
        }
    }
    val movePreviousLabel = stringResource(R.string.cd_move_section_up)
    val moveNextLabel = stringResource(R.string.cd_move_section_down)
    val dragModifier = if (isEditingSections) {
        Modifier.pointerInput(sectionId) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    isDragging = true
                    dragOffset = Offset.Zero
                    listState.draggingSectionId = sectionId
                    listState.dragOffset = Offset.Zero
                },
                onDragCancel = {
                    isDragging = false
                    dragOffset = Offset.Zero
                    listState.draggingSectionId = null
                    listState.dragOffset = Offset.Zero
                },
                onDragEnd = {
                    closestMetricDetailSectionId(
                        draggedId = sectionId,
                        dragOffset = dragOffset,
                        sectionBounds = sectionBounds,
                        targetIds = sectionBounds.keys.toList(),
                    )?.let { targetId ->
                        onMoveSectionToTarget(sectionId, targetId)
                    }
                    isDragging = false
                    dragOffset = Offset.Zero
                    listState.draggingSectionId = null
                    listState.dragOffset = Offset.Zero
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount
                    listState.dragOffset = dragOffset
                },
            )
        }
    } else {
        Modifier
    }
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

    CompositionLocalProvider(
        LocalViewConfiguration provides if (isEditingSections) dragViewConfiguration else viewConfiguration,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates -> onPositioned(coordinates.boundsInRoot()) }
                .zIndex(if (isDragging) 1f else 0f)
                .graphicsLayer {
                    alpha = if (isDragging) 0.35f else 1f
                    translationX = if (isDragging) dragOffset.x else 0f
                    translationY = if (isDragging) dragOffset.y else 0f
                    rotationZ = if (isEditingSections && !isDragging) wiggleRotation else 0f
                }
                .then(editSemanticsModifier)
                .then(dragModifier)
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
            content()
        }
    }
}

private fun closestMetricDetailSectionId(
    draggedId: MetricDetailSectionId,
    dragOffset: Offset,
    sectionBounds: Map<MetricDetailSectionId, Rect>,
    targetIds: List<MetricDetailSectionId>,
): MetricDetailSectionId? {
    val draggedBounds = sectionBounds[draggedId] ?: return null
    val dropCenter = draggedBounds.center + dragOffset

    return targetIds
        .filter { it in sectionBounds && it != draggedId }
        .minByOrNull { sectionId ->
            val center = sectionBounds.getValue(sectionId).center
            val delta = dropCenter - center
            delta.x * delta.x + delta.y * delta.y
        }
}
