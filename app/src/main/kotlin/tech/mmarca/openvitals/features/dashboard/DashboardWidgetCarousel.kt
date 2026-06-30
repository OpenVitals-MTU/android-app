package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DashboardWidgetCarousel(
    visibleIds: List<DashboardWidgetId>,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    isEditingDashboard: Boolean,
    onMoveWidgetToTarget: (DashboardWidgetId, DashboardWidgetId) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
    visibleWidgetLoadToken: Long,
    onVisibleWidgetsChanged: (Set<DashboardWidgetId>) -> Unit,
    actionContent: @Composable () -> Unit,
    hiddenContent: @Composable () -> Unit,
) {
    val fixedIds = dashboardWidgetIdsThatFitRows(
        widgetIds = visibleIds,
        rows = DashboardFixedWidgetRows,
    )
    val fixedIdSet = fixedIds.toSet()
    val carouselIds = visibleIds.filterNot { it in fixedIdSet }
    val carouselPages = dashboardWidgetIdsInGridPages(
        widgetIds = carouselIds,
        rows = DashboardCarouselWidgetRows,
    )
    val pagerState = rememberPagerState(pageCount = { carouselPages.size.coerceAtLeast(1) })
    val widgetBounds = remember { mutableStateMapOf<DashboardWidgetId, Rect>() }
    var fixedSectionBounds by remember { mutableStateOf<Rect?>(null) }
    var carouselSectionBounds by remember { mutableStateOf<Rect?>(null) }
    var draggingWidgetId by remember { mutableStateOf<DashboardWidgetId?>(null) }
    var draggingWidgetBounds by remember { mutableStateOf<Rect?>(null) }
    val onVisibleWidgetsChangedState = rememberUpdatedState(onVisibleWidgetsChanged)
    val density = LocalDensity.current
    val edgeScrollThresholdPx = with(density) { DashboardCarouselEdgeScrollThreshold.toPx() }

    LaunchedEffect(visibleIds) {
        val visibleSet = visibleIds.toSet()
        widgetBounds.keys.toList().forEach { widgetId ->
            if (widgetId !in visibleSet) {
                widgetBounds.remove(widgetId)
            }
        }
        if (draggingWidgetId !in visibleSet) {
            draggingWidgetId = null
            draggingWidgetBounds = null
        }
    }

    LaunchedEffect(isEditingDashboard) {
        if (!isEditingDashboard) {
            draggingWidgetId = null
            draggingWidgetBounds = null
        }
    }

    LaunchedEffect(visibleWidgetLoadToken, fixedIds, carouselPages, pagerState) {
        fun visibleWidgetsFor(page: Int): Set<DashboardWidgetId> =
            (
                fixedIds +
                    carouselPages.getOrNull(page).orEmpty()
            ).toSet()

        onVisibleWidgetsChangedState.value(visibleWidgetsFor(pagerState.currentPage))
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onVisibleWidgetsChangedState.value(visibleWidgetsFor(page))
            }
    }

    LaunchedEffect(draggingWidgetId, carouselPages.size, carouselSectionBounds, edgeScrollThresholdPx) {
        if (draggingWidgetId == null || carouselPages.size <= 1 || carouselSectionBounds == null) {
            return@LaunchedEffect
        }

        val carouselBounds = carouselSectionBounds ?: return@LaunchedEffect
        while (true) {
            val widgetId = draggingWidgetId ?: return@LaunchedEffect
            val draggedBounds = draggingWidgetBounds ?: widgetBounds[widgetId]
            val overlapsCarouselY = draggedBounds?.let { bounds ->
                bounds.bottom >= carouselBounds.top && bounds.top <= carouselBounds.bottom
            } == true
            if (draggedBounds != null && overlapsCarouselY && !pagerState.isScrollInProgress) {
                val draggedCenter = draggedBounds.center
                val currentPage = pagerState.currentPage
                val targetPage = when {
                    draggedCenter.x <= carouselBounds.left + edgeScrollThresholdPx -> currentPage - 1
                    draggedCenter.x >= carouselBounds.right - edgeScrollThresholdPx -> currentPage + 1
                    else -> currentPage
                }.coerceIn(0, carouselPages.lastIndex)

                if (targetPage != currentPage) {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
            delay(DashboardCarouselEdgeScrollDelayMillis)
        }
    }

    fun dropTargetIdsFor(draggedId: DashboardWidgetId, draggedBounds: Rect): List<DashboardWidgetId> {
        val currentPageIds = carouselPages.getOrNull(pagerState.currentPage).orEmpty()
        val dropCenter = draggedBounds.center
        val isOverFixedSection = fixedSectionBounds?.containsPoint(dropCenter) == true
        val isOverCarouselSection = carouselSectionBounds?.containsPoint(dropCenter) == true

        return when {
            draggedId in fixedIds && isOverCarouselSection -> currentPageIds
            draggedId in fixedIds -> fixedIds
            isOverFixedSection -> fixedIds
            else -> currentPageIds
        }
    }

    fun dropWidget(widgetId: DashboardWidgetId) {
        val draggedBounds = draggingWidgetBounds ?: widgetBounds[widgetId] ?: return
        val targetIds = dropTargetIdsFor(widgetId, draggedBounds)
        if (widgetId in targetIds) return

        closestDashboardWidgetId(
            draggedId = widgetId,
            dropCenter = draggedBounds.center,
            targetIds = targetIds,
            widgetBounds = widgetBounds,
        )?.let { targetId ->
            onMoveWidgetToTarget(widgetId, targetId)
        }
    }

    val onGridDraggingWidgetChanged: (DashboardWidgetId?) -> Unit = { widgetId ->
        draggingWidgetId = widgetId
        if (widgetId == null) {
            draggingWidgetBounds = null
        } else {
            draggingWidgetBounds = widgetBounds[widgetId]
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Column {
            DashboardWidgetGrid(
                ids = fixedIds,
                rows = DashboardFixedWidgetRows,
                specsById = specsById,
                isEditingDashboard = isEditingDashboard,
                widgetBounds = widgetBounds,
                onDraggingWidgetChanged = onGridDraggingWidgetChanged,
                onDraggingWidgetBoundsChanged = { widgetId, bounds ->
                    if (draggingWidgetId == widgetId) {
                        draggingWidgetBounds = bounds
                    }
                },
                onDropWidget = ::dropWidget,
                onMoveWidgetToTarget = onMoveWidgetToTarget,
                onRemoveWidget = onRemoveWidget,
                modifier = Modifier
                    .onGloballyPositioned { coordinates -> fixedSectionBounds = coordinates.boundsInRoot() }
                    .zIndex(if (draggingWidgetId in fixedIds) 2f else 0f),
            )

            actionContent()

            if (carouselPages.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = DashboardScreenPadding,
                        vertical = DashboardSectionSeparatorSpacing,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates -> carouselSectionBounds = coordinates.boundsInRoot() }
                        .zIndex(if (draggingWidgetId in carouselIds) 2f else 0f),
                    pageSpacing = 12.dp,
                    beyondViewportPageCount = 1.coerceAtMost(carouselPages.lastIndex),
                    userScrollEnabled = draggingWidgetId == null,
                ) { page ->
                    val pageIds = carouselPages.getOrNull(page).orEmpty()
                    DashboardWidgetGrid(
                        ids = pageIds,
                        rows = DashboardCarouselWidgetRows,
                        specsById = specsById,
                        isEditingDashboard = isEditingDashboard,
                        widgetBounds = widgetBounds,
                        onDraggingWidgetChanged = onGridDraggingWidgetChanged,
                        onDraggingWidgetBoundsChanged = { widgetId, bounds ->
                            if (draggingWidgetId == widgetId) {
                                draggingWidgetBounds = bounds
                            }
                        },
                        onDropWidget = ::dropWidget,
                        onMoveWidgetToTarget = onMoveWidgetToTarget,
                        onRemoveWidget = onRemoveWidget,
                    )
                }

                if (carouselPages.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 6.dp,
                                bottom = 6.dp,
                            ),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        carouselPages.forEachIndexed { page, _ ->
                            val color = if (page == pagerState.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            }
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(6.dp)
                                    .background(color, CircleShape)
                            )
                        }
                    }
                }
            }

            hiddenContent()
        }
    }
}

private fun closestDashboardWidgetId(
    draggedId: DashboardWidgetId,
    dropCenter: Offset,
    targetIds: List<DashboardWidgetId>,
    widgetBounds: Map<DashboardWidgetId, Rect>,
): DashboardWidgetId? =
    targetIds
        .filter { widgetId -> widgetId != draggedId && widgetId in widgetBounds }
        .minByOrNull { widgetId ->
            val center = widgetBounds.getValue(widgetId).center
            val delta = dropCenter - center
            delta.x * delta.x + delta.y * delta.y
        }

private fun Rect.containsPoint(point: Offset): Boolean =
    point.x >= left && point.x <= right && point.y >= top && point.y <= bottom
