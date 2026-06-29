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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
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
    draggingWidgetId: DashboardWidgetId?,
    widgetBounds: MutableMap<DashboardWidgetId, Rect>,
    onDraggingWidgetChanged: (DashboardWidgetId?) -> Unit,
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
    var sectionBounds by remember { mutableStateOf<Rect?>(null) }
    var fixedSectionBounds by remember { mutableStateOf<Rect?>(null) }
    var carouselSectionBounds by remember { mutableStateOf<Rect?>(null) }
    val dragOffsetState = remember { mutableStateOf(Offset.Zero) }
    var draggedWidgetStartBounds by remember { mutableStateOf<Rect?>(null) }
    val onVisibleWidgetsChangedState = rememberUpdatedState(onVisibleWidgetsChanged)
    val density = LocalDensity.current
    val edgeScrollThresholdPx = with(density) { DashboardCarouselEdgeScrollThreshold.toPx() }
    val currentDropTargetIds: (DashboardWidgetId, Offset) -> List<DashboardWidgetId> = { draggedId, droppedOffset ->
        val currentPageIds = carouselPages.getOrNull(pagerState.currentPage).orEmpty()
        val draggedBounds = draggedWidgetStartBounds ?: widgetBounds[draggedId]
        val dropCenter = draggedBounds?.let { it.center + droppedOffset }
        val isOverFixedSection = dropCenter?.let { fixedSectionBounds?.containsPoint(it) } == true
        val isOverCarouselSection = dropCenter?.let { carouselSectionBounds?.containsPoint(it) } == true

        when {
            draggedId in fixedIds && isOverCarouselSection -> currentPageIds
            draggedId in fixedIds -> fixedIds
            isOverFixedSection -> fixedIds
            else -> currentPageIds
        }
    }

    LaunchedEffect(draggingWidgetId) {
        if (draggingWidgetId == null) {
            dragOffsetState.value = Offset.Zero
            draggedWidgetStartBounds = null
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

    LaunchedEffect(draggingWidgetId, carouselPages.size, sectionBounds, edgeScrollThresholdPx) {
        if (draggingWidgetId == null || carouselPages.size <= 1 || sectionBounds == null) {
            return@LaunchedEffect
        }

        val section = sectionBounds ?: return@LaunchedEffect
        val widgetId = draggingWidgetId ?: return@LaunchedEffect
        while (true) {
            val draggedBounds = draggedWidgetStartBounds ?: widgetBounds[widgetId]
            if (widgetId in carouselIds && draggedBounds != null && !pagerState.isScrollInProgress) {
                val draggedCenterX = draggedBounds.center.x + dragOffsetState.value.x
                val currentPage = pagerState.currentPage
                val targetPage = when {
                    draggedCenterX <= section.left + edgeScrollThresholdPx -> currentPage - 1
                    draggedCenterX >= section.right - edgeScrollThresholdPx -> currentPage + 1
                    else -> currentPage
                }.coerceIn(0, carouselPages.lastIndex)

                if (targetPage != currentPage) {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
            delay(DashboardCarouselEdgeScrollDelayMillis)
        }
    }

    val onGridDraggingWidgetChanged: (DashboardWidgetId?) -> Unit = { widgetId ->
        draggedWidgetStartBounds = widgetId?.let { widgetBounds[it] }
        onDraggingWidgetChanged(widgetId)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> sectionBounds = coordinates.boundsInRoot() },
    ) {
        Column {
            DashboardWidgetGrid(
                ids = fixedIds,
                rows = DashboardFixedWidgetRows,
                specsById = specsById,
                dropTargetIdsProvider = currentDropTargetIds,
                isEditingDashboard = isEditingDashboard,
                draggingWidgetId = draggingWidgetId,
                draggedWidgetStartBounds = draggedWidgetStartBounds,
                widgetBounds = widgetBounds,
                onDraggingWidgetChanged = onGridDraggingWidgetChanged,
                onDragOffsetChanged = { offset -> dragOffsetState.value = offset },
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
                ) { page ->
                    val pageIds = carouselPages.getOrNull(page).orEmpty()
                    DashboardWidgetGrid(
                        ids = pageIds,
                        rows = DashboardCarouselWidgetRows,
                        specsById = specsById,
                        dropTargetIdsProvider = currentDropTargetIds,
                        isEditingDashboard = isEditingDashboard,
                        draggingWidgetId = draggingWidgetId,
                        draggedWidgetStartBounds = draggedWidgetStartBounds,
                        widgetBounds = widgetBounds,
                        onDraggingWidgetChanged = onGridDraggingWidgetChanged,
                        onDragOffsetChanged = { offset -> dragOffsetState.value = offset },
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

        DashboardDraggedWidgetOverlay(
            draggingWidgetId = draggingWidgetId,
            specsById = specsById,
            widgetBounds = widgetBounds,
            draggedWidgetStartBounds = draggedWidgetStartBounds,
            sectionBounds = sectionBounds,
            dragOffsetState = dragOffsetState,
        )
    }
}
