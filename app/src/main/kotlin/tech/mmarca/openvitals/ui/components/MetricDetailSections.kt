package tech.mmarca.openvitals.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.input.pointer.positionChange
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MetricSectionDragLongPressMillis = 500L
private const val MetricSectionEditWiggleDegrees = 0.35f
private const val MetricSectionEdgeScrollDelayMillis = 16L
private val MetricSectionEdgeScrollThreshold = 96.dp
private val MetricSectionEdgeScrollSpeed = 40.dp

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
    internal val sectionOverlayContents = mutableStateMapOf<MetricDetailSectionId, @Composable () -> Unit>()
    internal var isEditingSections by mutableStateOf(false)
    internal var onMoveSectionToTarget: ((MetricDetailSectionId, MetricDetailSectionId) -> Unit)? = null
    internal var draggingSectionId by mutableStateOf<MetricDetailSectionId?>(null)
    internal var draggedSectionStartBounds by mutableStateOf<Rect?>(null)
    internal var dragOffset by mutableStateOf(Offset.Zero)
    internal var dragPointerRootY by mutableStateOf<Float?>(null)
    internal var viewportBounds by mutableStateOf<Rect?>(null)

    internal fun registerSectionOverlay(
        sectionId: MetricDetailSectionId,
        content: @Composable () -> Unit,
    ) {
        sectionOverlayContents[sectionId] = content
    }

    internal fun unregisterSectionOverlay(sectionId: MetricDetailSectionId) {
        sectionOverlayContents.remove(sectionId)
    }
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

    item(key = "metric_section_drag_config") {
        SideEffect {
            listState.isEditingSections = isEditingSections
            listState.onMoveSectionToTarget = onMoveSectionToTarget
        }
    }

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

    visibleOrder.forEachIndexed { index, sectionId ->
        item(key = sectionId.name) {
            ReorderableMetricDetailSection(
                sectionId = sectionId,
                listState = listState,
                isEditingSections = isEditingSections,
                onPositioned = { bounds -> sectionBounds[sectionId] = bounds },
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

internal fun Modifier.metricDetailSectionDragGesture(
    listState: MetricDetailSectionListState,
): Modifier {
    if (!listState.isEditingSections) return this

    return pointerInput(listState.isEditingSections) {
        val touchSlop = viewConfiguration.touchSlop
        val edgeScrollThresholdPx = MetricSectionEdgeScrollThreshold.toPx()
        val edgeScrollSpeedPx = MetricSectionEdgeScrollSpeed.toPx()

        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val viewport = listState.viewportBounds ?: return@awaitEachGesture
                val downRoot = viewport.localToRoot(down.position)
                val sectionId = metricSectionAtPoint(listState.sectionBounds, downRoot)
                    ?: return@awaitEachGesture

                var dragStarted = false
                var dragFinished = false
                val longPressDeadline = System.currentTimeMillis() + MetricSectionDragLongPressMillis

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            if (dragStarted && !dragFinished) {
                                completeMetricSectionDrag(listState, sectionId)
                                dragFinished = true
                            }
                            break
                        }

                        val pointerRoot = viewport.localToRoot(change.position)
                        val distanceFromDown = (change.position - down.position).getDistance()

                        if (!dragStarted) {
                            if (distanceFromDown > touchSlop) {
                                break
                            }
                            if (System.currentTimeMillis() >= longPressDeadline) {
                                dragStarted = true
                                listState.draggingSectionId = sectionId
                                listState.draggedSectionStartBounds = listState.sectionBounds[sectionId]
                                listState.dragOffset = Offset.Zero
                                listState.dragPointerRootY = pointerRoot.y
                            }
                            continue
                        }

                        val dragAmount = change.positionChange()
                        if (dragAmount != Offset.Zero) {
                            change.consume()
                            listState.dragOffset += dragAmount
                            listState.dragPointerRootY = pointerRoot.y

                            val edge = metricSectionEdge(pointerRoot.y, viewport, edgeScrollThresholdPx)
                            val shouldScrollAtEdge = when (edge) {
                                MetricSectionEdge.TOP ->
                                    dragAmount.y < 0f && listState.lazyListState.canScrollBackward
                                MetricSectionEdge.BOTTOM ->
                                    dragAmount.y > 0f && listState.lazyListState.canScrollForward
                                MetricSectionEdge.NONE -> false
                            }
                            if (shouldScrollAtEdge) {
                                launch {
                                    performMetricSectionEdgeScroll(
                                        listState = listState,
                                        edgeScrollThresholdPx = edgeScrollThresholdPx,
                                        edgeScrollSpeedPx = edgeScrollSpeedPx,
                                        dragAmountY = dragAmount.y,
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    if (dragStarted && !dragFinished && listState.draggingSectionId == sectionId) {
                        completeMetricSectionDrag(listState, sectionId)
                    }
                }
            }
        }
    }
}

private fun Rect.localToRoot(localPosition: Offset): Offset =
    Offset(left + localPosition.x, top + localPosition.y)

@Composable
internal fun MetricDetailSectionEdgeScrollEffect(listState: MetricDetailSectionListState) {
    val density = LocalDensity.current
    val edgeScrollThresholdPx = with(density) { MetricSectionEdgeScrollThreshold.toPx() }
    val edgeScrollSpeedPx = with(density) { MetricSectionEdgeScrollSpeed.toPx() }
    val draggingSectionId = listState.draggingSectionId

    LaunchedEffect(draggingSectionId) {
        if (draggingSectionId == null) return@LaunchedEffect

        while (listState.draggingSectionId != null) {
            performMetricSectionEdgeScroll(
                listState = listState,
                edgeScrollThresholdPx = edgeScrollThresholdPx,
                edgeScrollSpeedPx = edgeScrollSpeedPx,
            )
            delay(MetricSectionEdgeScrollDelayMillis)
        }
    }
}

@Composable
internal fun MetricDetailSectionDragOverlay(listState: MetricDetailSectionListState) {
    val draggingSectionId = listState.draggingSectionId ?: return
    val startBounds = listState.draggedSectionStartBounds ?: return
    val viewport = listState.viewportBounds ?: return
    val dragOffset = listState.dragOffset
    val overlayContent = listState.sectionOverlayContents[draggingSectionId] ?: return
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .zIndex(10f)
            .width(with(density) { startBounds.width.toDp() })
            .height(with(density) { startBounds.height.toDp() })
            .graphicsLayer {
                translationX = startBounds.left - viewport.left + dragOffset.x
                translationY = startBounds.top - viewport.top + dragOffset.y
                scaleX = 1.02f
                scaleY = 1.02f
                shadowElevation = with(density) { 12.dp.toPx() }
            }
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium,
            ),
    ) {
        overlayContent()
    }
}

private suspend fun performMetricSectionEdgeScroll(
    listState: MetricDetailSectionListState,
    edgeScrollThresholdPx: Float,
    edgeScrollSpeedPx: Float,
    dragAmountY: Float = 0f,
) {
    val viewport = listState.viewportBounds ?: return
    val pointerRootY = listState.dragPointerRootY ?: return
    val lazyListState = listState.lazyListState

    when {
        pointerRootY <= viewport.top + edgeScrollThresholdPx && lazyListState.canScrollBackward -> {
            val delta = if (dragAmountY < 0f) dragAmountY else -edgeScrollSpeedPx
            lazyListState.scrollBy(delta)
        }
        pointerRootY >= viewport.bottom - edgeScrollThresholdPx && lazyListState.canScrollForward -> {
            val delta = if (dragAmountY > 0f) dragAmountY else edgeScrollSpeedPx
            lazyListState.scrollBy(delta)
        }
    }
}

private fun completeMetricSectionDrag(
    listState: MetricDetailSectionListState,
    sectionId: MetricDetailSectionId,
) {
    val droppedOffset = listState.dragOffset
    val draggedBounds = listState.draggedSectionStartBounds ?: listState.sectionBounds[sectionId]
    closestMetricDetailSectionId(
        draggedId = sectionId,
        draggedBounds = draggedBounds,
        dragOffset = droppedOffset,
        sectionBounds = listState.sectionBounds,
        targetIds = listState.sectionBounds.keys.toList(),
    )?.let { targetId ->
        listState.onMoveSectionToTarget?.invoke(sectionId, targetId)
    }
    listState.draggingSectionId = null
    listState.draggedSectionStartBounds = null
    listState.dragOffset = Offset.Zero
    listState.dragPointerRootY = null
}

private enum class MetricSectionEdge {
    TOP,
    BOTTOM,
    NONE,
}

private fun metricSectionEdge(
    pointerRootY: Float,
    viewport: Rect,
    thresholdPx: Float,
): MetricSectionEdge =
    when {
        pointerRootY <= viewport.top + thresholdPx -> MetricSectionEdge.TOP
        pointerRootY >= viewport.bottom - thresholdPx -> MetricSectionEdge.BOTTOM
        else -> MetricSectionEdge.NONE
    }

private fun metricSectionAtPoint(
    sectionBounds: Map<MetricDetailSectionId, Rect>,
    point: Offset,
): MetricDetailSectionId? =
    sectionBounds.entries
        .filter { (_, bounds) -> bounds.contains(point) }
        .minByOrNull { (_, bounds) -> bounds.height }
        ?.key

@Composable
private fun ReorderableMetricDetailSection(
    sectionId: MetricDetailSectionId,
    listState: MetricDetailSectionListState,
    isEditingSections: Boolean,
    onPositioned: (Rect) -> Unit,
    onMovePrevious: (() -> Unit)?,
    onMoveNext: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val isDragging = listState.draggingSectionId == sectionId
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

    SideEffect {
        listState.registerSectionOverlay(sectionId, content)
    }
    DisposableEffect(sectionId) {
        onDispose {
            listState.unregisterSectionOverlay(sectionId)
        }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.boundsInRoot())
            }
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                alpha = if (isDragging) 0f else 1f
                rotationZ = if (isEditingSections && !isDragging) wiggleRotation else 0f
            }
            .then(editSemanticsModifier)
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

private fun closestMetricDetailSectionId(
    draggedId: MetricDetailSectionId,
    draggedBounds: Rect?,
    dragOffset: Offset,
    sectionBounds: Map<MetricDetailSectionId, Rect>,
    targetIds: List<MetricDetailSectionId>,
): MetricDetailSectionId? {
    val bounds = draggedBounds ?: sectionBounds[draggedId] ?: return null
    val dropCenter = bounds.center + dragOffset

    return targetIds
        .filter { it in sectionBounds && it != draggedId }
        .minByOrNull { sectionId ->
            val center = sectionBounds.getValue(sectionId).center
            val delta = dropCenter - center
            delta.x * delta.x + delta.y * delta.y
        }
}
