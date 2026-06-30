package tech.mmarca.openvitals.features.dashboard

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.mergeLoaded
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.usecase.LoadDashboardDayUseCase
import java.time.LocalDate

internal data class DeferredDashboardLoadContext(
    val date: LocalDate,
    val sleepRangeMode: SleepRangeMode,
    val activityWeekMode: ActivityWeekMode,
    val refreshMode: RefreshMode,
    val generation: Long,
)

internal data class DeferredDashboardProgressUpdate(
    val data: DashboardData,
    val pendingWidgets: Set<DashboardWidgetId>,
    val display: DashboardDisplayState,
    val unacknowledgedWidgetPermissions: Set<String>,
    val error: ScreenError? = null,
)

internal class DashboardDeferredLoadCoordinator(
    private val loadDashboardDayUseCase: LoadDashboardDayUseCase,
) {
    private val mutex = Mutex()
    private var context: DeferredDashboardLoadContext? = null
    private var requestedWidgets: Set<DashboardWidgetId> = emptySet()
    private var loadedWidgets: Set<DashboardWidgetId> = emptySet()

    fun beginLoad(context: DeferredDashboardLoadContext) {
        this.context = context
        requestedWidgets = emptySet()
        loadedWidgets = emptySet()
    }

    val activeContext: DeferredDashboardLoadContext?
        get() = context

    fun reset() {
        context = null
        requestedWidgets = emptySet()
        loadedWidgets = emptySet()
    }

    fun retainWidgets(widgetIds: Set<DashboardWidgetId>) {
        requestedWidgets = requestedWidgets intersect widgetIds
        loadedWidgets = loadedWidgets intersect widgetIds
    }

    fun isCurrent(context: DeferredDashboardLoadContext): Boolean =
        this.context?.generation == context.generation

    fun pendingWidgets(context: DeferredDashboardLoadContext): Set<DashboardWidgetId> =
        if (isCurrent(context)) {
            requestedWidgets - loadedWidgets
        } else {
            emptySet()
        }

    fun widgetsToLoad(
        context: DeferredDashboardLoadContext,
        visibleWidgetIds: Set<DashboardWidgetId>,
        currentData: DashboardData,
    ): Set<DashboardWidgetId> {
        if (!isCurrent(context)) return emptySet()
        return visibleWidgetIds
            .filter { widgetId ->
                val metric = widgetId.toDashboardMetricOrNull() ?: return@filter false
                metric !in DashboardFastMetrics &&
                    metric !in currentData.loadedMetrics &&
                    widgetId !in requestedWidgets &&
                    widgetId !in loadedWidgets
            }
            .toSet()
    }

    fun markRequested(widgetIds: Set<DashboardWidgetId>) {
        requestedWidgets = requestedWidgets + widgetIds
    }

    suspend fun loadDeferredMetrics(
        context: DeferredDashboardLoadContext,
        widgets: List<DashboardWidgetId>,
        currentData: DashboardData,
        dailyGoals: DashboardDailyGoals,
        buildDisplay: suspend (DashboardData, DashboardDailyGoals, Set<DashboardWidgetId>) -> DashboardDisplayState,
        unacknowledgedPermissions: (Set<String>) -> Set<String>,
        onProgress: suspend (DeferredDashboardProgressUpdate) -> Unit,
    ) {
        mutex.withLock {
            if (!isCurrent(context)) return
            if (widgets.isEmpty()) return

            val metricsByWidget = widgets
                .mapNotNull { widgetId -> widgetId.toDashboardMetricOrNull()?.let { widgetId to it } }
                .filter { (_, metric) -> metric !in currentData.loadedMetrics }
            val skippedWidgets = widgets.toSet() - metricsByWidget.map { (widgetId, _) -> widgetId }.toSet()
            if (skippedWidgets.isNotEmpty()) {
                loadedWidgets = loadedWidgets + skippedWidgets
            }
            if (metricsByWidget.isEmpty()) {
                onProgress(
                    DeferredDashboardProgressUpdate(
                        data = currentData,
                        pendingWidgets = pendingWidgets(context),
                        display = buildDisplay(currentData, dailyGoals, pendingWidgets(context)),
                        unacknowledgedWidgetPermissions = unacknowledgedPermissions(currentData.missingPermissions),
                    )
                )
                return
            }

            val orderedMetrics = metricsByWidget
                .map { (_, metric) -> metric }
                .distinct()

            coroutineScope {
                var mergedData = currentData
                var debounceJob: Job? = null

                suspend fun publishDeferredProgress(force: Boolean) {
                    if (!isCurrent(context)) return
                    val pending = pendingWidgets(context)
                    suspend fun publishNow() {
                        if (!isCurrent(context)) return
                        onProgress(
                            DeferredDashboardProgressUpdate(
                                data = mergedData,
                                pendingWidgets = pending,
                                display = buildDisplay(mergedData, dailyGoals, pending),
                                unacknowledgedWidgetPermissions =
                                    unacknowledgedPermissions(mergedData.missingPermissions),
                            )
                        )
                    }
                    if (force) {
                        debounceJob?.cancel()
                        publishNow()
                    } else {
                        debounceJob?.cancel()
                        debounceJob = launch {
                            delay(DeferredDashboardUiDebounceMillis)
                            publishNow()
                        }
                    }
                }

                deferredMetricLoadGroups(orderedMetrics).forEach { metricGroup ->
                    if (!isCurrent(context)) return@coroutineScope
                    runCatching {
                        loadDashboardDayUseCase(
                            DashboardQuery(
                                date = context.date,
                                sleepRangeMode = context.sleepRangeMode,
                                activityWeekMode = context.activityWeekMode,
                                visibleMetrics = metricGroup,
                                refreshMode = context.refreshMode,
                            )
                        )
                    }
                        .onSuccess { remainingData ->
                            if (!isCurrent(context)) return@coroutineScope
                            val loadedWidgetBatch = metricsByWidget
                                .filter { (_, widgetMetric) -> widgetMetric in metricGroup }
                                .map { (widgetId, _) -> widgetId }
                                .toSet()
                            mergedData = mergedData.mergeLoaded(remainingData)
                            loadedWidgets = loadedWidgets + loadedWidgetBatch
                            publishDeferredProgress(force = false)
                        }
                        .onFailure { error ->
                            if (!isCurrent(context)) return@coroutineScope
                            val failedWidgets = metricsByWidget
                                .filter { (_, widgetMetric) -> widgetMetric in metricGroup }
                                .map { (widgetId, _) -> widgetId }
                                .toSet()
                            loadedWidgets = loadedWidgets + failedWidgets
                            onProgress(
                                DeferredDashboardProgressUpdate(
                                    data = mergedData,
                                    pendingWidgets = pendingWidgets(context),
                                    display = buildDisplay(mergedData, dailyGoals, pendingWidgets(context)),
                                    unacknowledgedWidgetPermissions =
                                        unacknowledgedPermissions(mergedData.missingPermissions),
                                    error = error.toScreenError("Unknown error"),
                                )
                            )
                        }
                }
                publishDeferredProgress(force = true)
            }
        }
    }
}

private val DashboardWeeklyTrainingDeferredMetrics = setOf(
    DashboardMetric.WEEKLY_CARDIO_LOAD,
    DashboardMetric.INTENSITY_MINUTES,
)

internal val DashboardFastMetrics = setOf(
    DashboardMetric.STEPS,
    DashboardMetric.DISTANCE,
    DashboardMetric.CALORIES_OUT,
    DashboardMetric.WHEELCHAIR_PUSHES,
    DashboardMetric.WORKOUT,
    DashboardMetric.AVG_HEART_RATE,
)

private fun deferredMetricLoadGroups(metrics: List<DashboardMetric>): List<Set<DashboardMetric>> {
    val remaining = metrics.toMutableList()
    val groups = mutableListOf<Set<DashboardMetric>>()
    if (DashboardWeeklyTrainingDeferredMetrics.all { it in remaining }) {
        groups += DashboardWeeklyTrainingDeferredMetrics
        remaining.removeAll(DashboardWeeklyTrainingDeferredMetrics)
    }
    remaining.forEach { metric ->
        groups += setOf(metric)
    }
    return groups
}

private const val DeferredDashboardUiDebounceMillis = 150L
