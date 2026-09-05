package tech.mmarca.openvitals.features.homewidgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.domain.insights.BodyEnergySeedSource
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.DailyReadinessGoalInputs
import tech.mmarca.openvitals.domain.insights.DailyReadinessInsight
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.ReadinessState
import tech.mmarca.openvitals.domain.insights.calculateDailyReadiness
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.navigation.Screen

class HomeDailyReadinessWidget : GlanceAppWidget() {
    override val stateDefinition = HomeMetricWidgetState.definition
    override val sizeMode = SizeMode.Responsive(HomeStatusWidgetSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            HomeWidgetContentFromState(context.getString(R.string.screen_daily_readiness))
        }
    }
}

class HomeDailyReadinessWidgetReceiver : UpdatingHomeWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HomeDailyReadinessWidget()

    override suspend fun refreshWidget(context: Context, appWidgetId: Int) {
        refreshDailyReadinessWidget(context, appWidgetId)
    }
}

class HomeBodyEnergyWidget : GlanceAppWidget() {
    override val stateDefinition = HomeMetricWidgetState.definition

    // Exact, not Responsive: the curve is drawn to the width the widget has.
    // Responsive reports the largest declared bucket, far narrower than the card.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            HomeBodyEnergyContentFromState(context.getString(R.string.screen_body_energy))
        }
    }
}

class HomeBodyEnergyWidgetReceiver : UpdatingHomeWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HomeBodyEnergyWidget()

    override suspend fun refreshWidget(context: Context, appWidgetId: Int) {
        refreshBodyEnergyWidget(context, appWidgetId)
    }
}

class HomeTodayVitalsWidget : GlanceAppWidget() {
    override val stateDefinition = HomeMetricWidgetState.definition
    override val sizeMode = SizeMode.Responsive(HomeTodayWidgetSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            HomeTodayVitalsContentFromState()
        }
    }
}

class HomeTodayVitalsWidgetReceiver : UpdatingHomeWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HomeTodayVitalsWidget()

    override suspend fun refreshWidget(context: Context, appWidgetId: Int) {
        refreshTodayVitalsWidget(context, appWidgetId)
    }
}

abstract class UpdatingHomeWidgetReceiver : GlanceAppWidgetReceiver() {
    abstract suspend fun refreshWidget(context: Context, appWidgetId: Int)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()
        suspend fun refreshWidgets() {
            try {
                appWidgetIds.forEach { appWidgetId ->
                    if (!hasAppWidgetInfo(context, appWidgetId)) return@forEach
                    refreshWidget(context, appWidgetId)
                }
            } catch (throwable: Throwable) {
                Log.e(HomeWidgetLogTag, "Home status widget update failed", throwable)
            }
        }
        if (pendingResult == null) {
            runBlocking(Dispatchers.Default) {
                refreshWidgets()
            }
            return
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                refreshWidgets()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * A read that did not happen leaves the tile alone. The periodic tick often
 * lands mid-update or under Doze; writing the failure snapshot would replace
 * real numbers with "No data". The widget still redraws.
 */
suspend fun refreshDailyReadinessWidget(context: Context, appWidgetId: Int) {
    if (!hasAppWidgetInfo(context, appWidgetId)) return

    val glanceId = glanceAppWidgetId(appWidgetId)
    loadDailyReadinessSnapshot(context)?.let { snapshot ->
        writeHomeWidgetSnapshot(context, glanceId, "daily_readiness", snapshot)
    }
    HomeDailyReadinessWidget().update(context, glanceId)
}

suspend fun refreshBodyEnergyWidget(context: Context, appWidgetId: Int) {
    if (!hasAppWidgetInfo(context, appWidgetId)) return

    val glanceId = glanceAppWidgetId(appWidgetId)
    loadBodyEnergyTimeline(context).map { timeline ->
        val candidate = buildBodyEnergySnapshot(context, timeline)
        val previous = getAppWidgetState(context, HomeMetricWidgetState.definition, glanceId)
            .toWidgetSnapshot(context)
        bodyEnergySnapshotToWrite(candidate, timeline, previous)
    }.getOrNull()?.let { snapshot ->
        writeHomeWidgetSnapshot(context, glanceId, "body_energy", snapshot)
    }
    HomeBodyEnergyWidget().update(context, glanceId)
}

/**
 * Whether a fresh Body Energy snapshot may replace [previous]: the
 * [candidate] to write, or null to keep the tile. A snapshot whose seed
 * defaulted loses to a chained one for the same day. A different route or a
 * rowless previous always gives way.
 */
internal fun bodyEnergySnapshotToWrite(
    candidate: HomeMetricWidgetSnapshot,
    timeline: BodyEnergyTimeline?,
    previous: HomeMetricWidgetSnapshot?,
): HomeMetricWidgetSnapshot? {
    if (timeline == null) return candidate
    if (timeline.inputSummary.seedSource == BodyEnergySeedSource.CARRIED_OVER) return candidate
    if (previous == null) return candidate
    if (previous.route != candidate.route) return candidate
    if (previous.rows.isEmpty()) return candidate
    return null
}

suspend fun refreshTodayVitalsWidget(context: Context, appWidgetId: Int) {
    if (!hasAppWidgetInfo(context, appWidgetId)) return

    val glanceId = glanceAppWidgetId(appWidgetId)
    loadTodayVitalsSnapshot(context)?.let { snapshot ->
        writeHomeWidgetSnapshot(context, glanceId, "today_vitals", snapshot)
    }
    HomeTodayVitalsWidget().update(context, glanceId)
}

@Composable
private fun HomeWidgetContentFromState(fallbackTitle: String) {
    val context = LocalContext.current
    val preferences = currentState<Preferences>()
    val snapshot = preferences.toWidgetSnapshot(context)
        ?: HomeMetricWidgetSnapshot(
            title = fallbackTitle,
            value = "--",
            unit = "",
            subtitle = context.getString(R.string.home_metric_widget_open_for_details),
            route = Screen.Dashboard.route,
        )
    HomeMetricWidgetContent(snapshot = snapshot)
}

/**
 * Body Energy: the numbers, and the day's curve beside them. One layout at
 * every size: the text column keeps what it needs and the curve takes the
 * rest. Too thin a leftover drops the plot for the text layout.
 */
@Composable
private fun HomeBodyEnergyContentFromState(fallbackTitle: String) {
    val context = LocalContext.current
    val size = LocalSize.current
    val preferences = currentState<Preferences>()
    val snapshot = preferences.toWidgetSnapshot(context)
        ?: HomeMetricWidgetSnapshot(
            title = fallbackTitle,
            value = "--",
            unit = "",
            subtitle = context.getString(R.string.home_metric_widget_open_for_details),
            route = Screen.Dashboard.route,
        )

    // Sized to the widest line ("Charged: +34" at 11sp); every dp here is taken from the curve.
    val textWidth = 108.dp
    val gap = 10.dp
    val plotWidth = size.width.value - 2 * BodyEnergyPadding.value - textWidth.value - gap.value
    val plotHeight = size.height.value - 2 * BodyEnergyPadding.value

    val plot = if (snapshot.series.size >= 2 && plotWidth >= BodyEnergyMinPlotWidth) {
        renderPlot(
            context = context,
            series = snapshot.series,
            widthDp = plotWidth,
            heightDp = plotHeight,
        )
    } else {
        null
    }
    if (plot == null) {
        HomeMetricWidgetContent(snapshot = snapshot)
        return
    }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .clickable(actionStartActivity(openMetricIntent(context, snapshot.route)))
            .padding(BodyEnergyPadding),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.width(textWidth)) {
            Text(
                text = snapshot.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WidgetMutedText),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = snapshot.value,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WidgetPrimaryText),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (snapshot.subtitle.isNotBlank()) {
                Text(
                    text = snapshot.subtitle,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(WidgetMutedText),
                        fontSize = 13.sp,
                    ),
                )
            }
            // Only where there is height. On a short widget these give way first.
            if (plotHeight >= BodyEnergyRowsMinHeight) {
                snapshot.rows.take(2).forEach { row ->
                    Text(
                        text = "${row.label}: ${row.value}",
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(WidgetPrimaryText),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
        Spacer(modifier = GlanceModifier.width(gap))
        Image(
            provider = ImageProvider(plot),
            // Described by what it shows, so a screen reader reads "39 Low".
            contentDescription = "${snapshot.value} ${snapshot.subtitle}".trim(),
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.width(plotWidth.dp).height(plotHeight.dp),
        )
    }
}

/** Rasterises the curve at the exact size it will be drawn at. */
private fun renderPlot(
    context: Context,
    series: List<Int>,
    widthDp: Float,
    heightDp: Float,
): android.graphics.Bitmap? {
    if (widthDp <= 0f || heightDp <= 0f) return null
    val density = context.resources.displayMetrics.density
    return BodyEnergyPlot.render(
        series = series,
        widthPx = (widthDp * density).toInt(),
        heightPx = (heightDp * density).toInt(),
        density = density,
    )
}

@Composable
private fun HomeTodayVitalsContentFromState() {
    val context = LocalContext.current
    val preferences = currentState<Preferences>()
    val snapshot = preferences.toWidgetSnapshot(context)
        ?: todayVitalsFallbackSnapshot(context)
    HomeTodayVitalsContent(snapshot = snapshot)
}

@Composable
private fun HomeTodayVitalsContent(snapshot: HomeMetricWidgetSnapshot) {
    val context = LocalContext.current
    val size = LocalSize.current
    val useTwoColumns = size.width >= 320.dp && snapshot.rows.size > 6
    val textSpec = todayVitalsTextSpec(size)
    val columnWidth = when {
        size.width >= 440.dp -> 192.dp
        size.width >= 400.dp -> 176.dp
        else -> 136.dp
    }
    val columnGap = if (textSpec.large) 18.dp else 10.dp
    val rows = snapshot.rows.take(if (useTwoColumns) TodayVitalsWidgetMaxRows else CompactTodayVitalsWidgetMaxRows)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .clickable(actionStartActivity(openMetricIntent(context, snapshot.route)))
            .padding(textSpec.contentPadding),
        verticalAlignment = Alignment.Vertical.Top,
    ) {
        Text(
            text = snapshot.title,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(WidgetMutedText),
                fontSize = textSpec.titleFontSize,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.height(textSpec.titleBottomSpacing))
        if (useTwoColumns) {
            val splitIndex = (rows.size + 1) / 2
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                TodayVitalsColumn(
                    rows = rows.take(splitIndex),
                    modifier = GlanceModifier.width(columnWidth),
                    textSpec = textSpec,
                )
                Spacer(modifier = GlanceModifier.width(columnGap))
                TodayVitalsColumn(
                    rows = rows.drop(splitIndex),
                    modifier = GlanceModifier.width(columnWidth),
                    textSpec = textSpec,
                )
            }
        } else {
            TodayVitalsColumn(
                rows = rows,
                modifier = GlanceModifier.fillMaxWidth(),
                textSpec = textSpec,
            )
        }
    }
}

@Composable
private fun TodayVitalsColumn(
    rows: List<HomeMetricWidgetRow>,
    modifier: GlanceModifier,
    textSpec: TodayVitalsTextSpec,
) {
    Column(modifier = modifier) {
        rows.forEach { row ->
            TodayVitalsRow(row, textSpec)
        }
    }
}

@Composable
private fun TodayVitalsRow(
    row: HomeMetricWidgetRow,
    textSpec: TodayVitalsTextSpec,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = textSpec.rowBottomPadding),
    ) {
        if (textSpec.large) {
            Text(
                text = row.label,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WidgetMutedText),
                    fontSize = textSpec.labelFontSize,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = row.value,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WidgetPrimaryText),
                    fontSize = textSpec.valueFontSize,
                    fontWeight = FontWeight.Bold,
                ),
            )
        } else {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = row.label,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(WidgetMutedText),
                        fontSize = textSpec.labelFontSize,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(textSpec.labelValueSpacing))
                Text(
                    text = row.value,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(WidgetPrimaryText),
                        fontSize = textSpec.valueFontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        if (row.subtitle.isNotBlank()) {
            Text(
                text = row.subtitle,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WidgetMutedText),
                    fontSize = textSpec.subtitleFontSize,
                ),
            )
        }
    }
}

private fun todayVitalsTextSpec(size: DpSize): TodayVitalsTextSpec =
    if (size.height >= 280.dp || size.width >= 400.dp) {
        TodayVitalsTextSpec(
            large = true,
            contentPadding = 20.dp,
            titleFontSize = 22.sp,
            titleBottomSpacing = 18.dp,
            labelFontSize = 12.sp,
            valueFontSize = 18.sp,
            subtitleFontSize = 12.sp,
            rowBottomPadding = 13.dp,
            labelValueSpacing = 8.dp,
        )
    } else {
        TodayVitalsTextSpec(
            large = false,
            contentPadding = 16.dp,
            titleFontSize = 15.sp,
            titleBottomSpacing = 10.dp,
            labelFontSize = 10.sp,
            valueFontSize = 13.sp,
            subtitleFontSize = 9.sp,
            rowBottomPadding = 8.dp,
            labelValueSpacing = 6.dp,
        )
    }

private data class TodayVitalsTextSpec(
    val large: Boolean,
    val contentPadding: androidx.compose.ui.unit.Dp,
    val titleFontSize: TextUnit,
    val titleBottomSpacing: androidx.compose.ui.unit.Dp,
    val labelFontSize: TextUnit,
    val valueFontSize: TextUnit,
    val subtitleFontSize: TextUnit,
    val rowBottomPadding: androidx.compose.ui.unit.Dp,
    val labelValueSpacing: androidx.compose.ui.unit.Dp,
)

/** Null when the read failed; see [refreshDailyReadinessWidget]. */
private suspend fun loadDailyReadinessSnapshot(context: Context): HomeMetricWidgetSnapshot? =
    loadReadinessInsight(context).map { insight ->
        buildDailyReadinessSnapshot(context, insight)
    }.getOrNull()

/** The Daily Readiness snapshot from a loaded insight. Split out so tests can pin its shape. */
internal fun buildDailyReadinessSnapshot(
    context: Context,
    insight: DailyReadinessInsight?,
    date: LocalDate = LocalDate.now(),
): HomeMetricWidgetSnapshot {
    val title = context.getString(R.string.screen_daily_readiness)
    // The readiness verdict lives on the Body Energy screen.
    val route = Screen.BodyEnergyDetails.createRoute(date.toString())
    if (insight == null || insight.state == ReadinessState.UNKNOWN) {
        return fallbackStatusSnapshot(context, title, route)
    }
    return HomeMetricWidgetSnapshot(
        title = title,
        value = insight.score.toString(),
        unit = "",
        subtitle = insight.statusTitle,
        route = route,
        rows = listOf(
            HomeMetricWidgetRow(
                label = context.getString(R.string.dashboard_readiness_recommended),
                value = insight.recommendation,
            )
        ),
    )
}

/** The Body Energy widget's snapshot, from an already-loaded timeline. */
internal fun buildBodyEnergySnapshot(
    context: Context,
    timeline: BodyEnergyTimeline?,
    date: LocalDate = LocalDate.now(),
): HomeMetricWidgetSnapshot {
    val title = context.getString(R.string.screen_body_energy)
    val route = Screen.BodyEnergyDetails.createRoute(date.toString())
    if (timeline == null) {
        return fallbackStatusSnapshot(context, title, route)
    }
    return HomeMetricWidgetSnapshot(
        title = title,
        value = timeline.currentScore.toString(),
        unit = "",
        subtitle = bodyEnergyStatus(context, timeline.currentScore),
        route = route,
        series = homeWidgetSeries(timeline.points.map { it.score }),
        rows = listOf(
            HomeMetricWidgetRow(
                label = context.getString(R.string.body_energy_timeline_start),
                value = timeline.startScore.toString(),
            ),
            HomeMetricWidgetRow(
                label = context.getString(R.string.body_energy_timeline_charged),
                value = "+${timeline.charged}",
            ),
            HomeMetricWidgetRow(
                label = context.getString(R.string.body_energy_timeline_drained),
                value = "-${timeline.drained}",
            ),
        ),
    )
}

/**
 * Thins [values] to at most [MaxHomeWidgetSeriesPoints], evenly. The last
 * value always survives: it is the score printed beside the plot.
 */
internal fun homeWidgetSeries(values: List<Int>): List<Int> {
    if (values.size <= MaxHomeWidgetSeriesPoints) return values.toList()
    // Spread across the whole range, not every Nth from the start.
    val step = (values.size - 1).toDouble() / (MaxHomeWidgetSeriesPoints - 1)
    return List(MaxHomeWidgetSeriesPoints) { index -> values[(index * step).roundToInt()] }
}

internal const val MaxHomeWidgetSeriesPoints = 48

/**
 * Null when the dashboard read failed; see [refreshDailyReadinessWidget].
 * The readiness and Body Energy sub-reads may fail: two rows of nine.
 */
private suspend fun loadTodayVitalsSnapshot(context: Context): HomeMetricWidgetSnapshot? {
    val dashboardResult = loadDashboardResult(context, TodayVitalsMetrics).getOrNull() ?: return null
    return buildTodayVitalsSnapshot(
        context = context,
        data = dashboardResult.data,
        unitFormatter = dashboardResult.unitFormatter,
        readinessInsight = loadReadinessInsight(context).getOrNull(),
        bodyEnergyTimeline = loadBodyEnergyTimeline(context).getOrNull(),
    )
}

/** The Today widget's rows from loaded data. Row order is the contract, so it lives here. */
internal fun buildTodayVitalsSnapshot(
    context: Context,
    data: DashboardData,
    unitFormatter: UnitFormatter,
    readinessInsight: DailyReadinessInsight?,
    bodyEnergyTimeline: BodyEnergyTimeline?,
): HomeMetricWidgetSnapshot {
    val dashboardResult = HomeDashboardWidgetResult(data = data, unitFormatter = unitFormatter)
    val rows = buildList {
        if (readinessInsight != null && readinessInsight.state != ReadinessState.UNKNOWN) {
            add(readinessRow(context, readinessInsight))
        }
        add(bodyEnergyRow(context, bodyEnergyTimeline))
        add(dashboardResult.row(context, DashboardWidgetId.SLEEP))
        add(dashboardResult.row(context, DashboardWidgetId.STEPS))
        add(dashboardResult.row(context, DashboardWidgetId.DISTANCE))
        add(dashboardResult.row(context, DashboardWidgetId.RESTING_HEART_RATE))
        add(dashboardResult.row(context, DashboardWidgetId.HRV, label = context.getString(R.string.home_widget_hrv_short)))
        add(dashboardResult.row(context, DashboardWidgetId.WEEKLY_CARDIO_LOAD))
        add(dashboardResult.row(context, DashboardWidgetId.HYDRATION))
    }
    return HomeMetricWidgetSnapshot(
        title = context.getString(R.string.home_widget_today_title),
        value = "",
        unit = "",
        subtitle = "",
        route = Screen.Dashboard.route,
        rows = rows,
    )
}

/** A read past [WidgetLoadTimeoutMillis] is a failure, not an empty day, and must not overwrite a good tile. */
private class WidgetLoadTimeoutException : Exception("The widget read ran out of time")

private suspend fun loadReadinessInsight(context: Context): Result<DailyReadinessInsight?> =
    runCatching {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HomeMetricWidgetEntryPoint::class.java,
        )
        val preferences = entryPoint.preferencesRepository()
        val data = withTimeoutOrNull(WidgetLoadTimeoutMillis) {
            entryPoint.dashboardDataLoader().loadDashboard(
                DashboardQuery(
                    date = LocalDate.now(),
                    sleepWindow = preferences.sleepWindow,
                    activityWeekMode = preferences.activityWeekMode,
                    visibleMetrics = ReadinessWidgetMetrics,
                    refreshMode = RefreshMode.NORMAL,
                )
            )
        } ?: throw WidgetLoadTimeoutException()
        calculateDailyReadiness(data, preferences.homeReadinessGoals())
    }

private suspend fun loadBodyEnergyTimeline(context: Context): Result<BodyEnergyTimeline?> =
    runCatching {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HomeMetricWidgetEntryPoint::class.java,
        )
        val today = LocalDate.now()
        // The timeout wraps the load, not the day it produced: an empty day is a success.
        val result = withTimeoutOrNull(WidgetLoadTimeoutMillis) {
            entryPoint.bodyEnergyRepository().loadTimeline(
                BodyEnergyTimelineQuery(
                    period = DatePeriod(today, today),
                    range = TimeRange.DAY,
                    refreshMode = RefreshMode.FORCE,
                )
            )
        } ?: throw WidgetLoadTimeoutException()
        result.latestDay
    }

private suspend fun loadDashboardResult(
    context: Context,
    metrics: Set<DashboardMetric>,
): Result<HomeDashboardWidgetResult> =
    runCatching {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HomeMetricWidgetEntryPoint::class.java,
        )
        val preferences = entryPoint.preferencesRepository()
        val data = withTimeoutOrNull(WidgetLoadTimeoutMillis) {
            entryPoint.dashboardDataLoader().loadDashboard(
                DashboardQuery(
                    date = LocalDate.now(),
                    sleepWindow = preferences.sleepWindow,
                    activityWeekMode = preferences.activityWeekMode,
                    visibleMetrics = metrics,
                    refreshMode = RefreshMode.FORCE,
                    includeHistoricalBaselines = false,
                    includeWeeklyTrainingSignals = DashboardMetric.WEEKLY_CARDIO_LOAD in metrics ||
                        DashboardMetric.INTENSITY_MINUTES in metrics,
                )
            )
        } ?: throw WidgetLoadTimeoutException()
        HomeDashboardWidgetResult(
            data = data,
            unitFormatter = entryPoint.unitFormatter(),
        )
    }

private fun HomeDashboardWidgetResult.row(
    context: Context,
    metricId: DashboardWidgetId,
    label: String = context.getString(metricId.homeMetricTitleRes()),
): HomeMetricWidgetRow {
    val snapshot = data.toSnapshot(
        context = context,
        metricId = metricId,
        title = label,
        route = Screen.Metric.createRoute(metricId.name),
        unitFormatter = unitFormatter,
    )
    return HomeMetricWidgetRow(
        label = label,
        value = snapshot.displayValue(),
        subtitle = snapshot.subtitle.takeUnless { it == context.getString(R.string.period_today) }.orEmpty(),
    )
}

private fun readinessRow(context: Context, insight: DailyReadinessInsight?): HomeMetricWidgetRow =
    if (insight == null || insight.state == ReadinessState.UNKNOWN) {
        fallbackRow(context, context.getString(R.string.screen_daily_readiness))
    } else {
        HomeMetricWidgetRow(
            label = context.getString(R.string.screen_daily_readiness),
            value = insight.score.toString(),
            subtitle = insight.statusTitle,
        )
    }

private fun bodyEnergyRow(context: Context, timeline: BodyEnergyTimeline?): HomeMetricWidgetRow =
    if (timeline == null) {
        fallbackRow(context, context.getString(R.string.screen_body_energy))
    } else {
        HomeMetricWidgetRow(
            label = context.getString(R.string.screen_body_energy),
            value = timeline.currentScore.toString(),
            subtitle = "+${timeline.charged} / -${timeline.drained}",
        )
    }

private fun fallbackStatusSnapshot(
    context: Context,
    title: String,
    route: String,
): HomeMetricWidgetSnapshot =
    HomeMetricWidgetSnapshot(
        title = title,
        value = "--",
        unit = "",
        subtitle = context.getString(R.string.home_metric_widget_open_for_details),
        route = route,
    )

private fun fallbackRow(context: Context, label: String): HomeMetricWidgetRow =
    HomeMetricWidgetRow(
        label = label,
        value = "--",
        subtitle = context.getString(R.string.no_data),
    )

private fun todayVitalsFallbackSnapshot(context: Context): HomeMetricWidgetSnapshot =
    HomeMetricWidgetSnapshot(
        title = context.getString(R.string.home_widget_today_title),
        value = "",
        unit = "",
        subtitle = context.getString(R.string.home_metric_widget_open_for_details),
        route = Screen.Dashboard.route,
        rows = listOf(
            fallbackRow(context, context.getString(R.string.screen_daily_readiness)),
            fallbackRow(context, context.getString(R.string.screen_body_energy)),
        ) + todayVitalsFallbackRows(context),
    )

private fun todayVitalsFallbackRows(context: Context): List<HomeMetricWidgetRow> =
    listOf(
        fallbackRow(context, context.getString(R.string.metric_sleep)),
        fallbackRow(context, context.getString(R.string.metric_steps)),
        fallbackRow(context, context.getString(R.string.metric_distance)),
        fallbackRow(context, context.getString(R.string.metric_resting_heart_rate)),
        fallbackRow(context, context.getString(R.string.home_widget_hrv_short)),
        fallbackRow(context, context.getString(R.string.metric_weekly_cardio_load)),
        fallbackRow(context, context.getString(R.string.metric_hydration)),
    )

private fun HomeMetricWidgetSnapshot.displayValue(): String =
    if (unit.isBlank()) {
        value
    } else {
        "$value $unit"
    }

private fun bodyEnergyStatus(context: Context, score: Int): String =
    when {
        score >= 80 -> context.getString(R.string.home_widget_body_energy_charged)
        score >= 60 -> context.getString(R.string.home_widget_body_energy_steady)
        score >= 40 -> context.getString(R.string.home_widget_body_energy_limited)
        else -> context.getString(R.string.home_widget_body_energy_low)
    }

private fun PreferencesRepository.homeReadinessGoals(): DailyReadinessGoalInputs =
    DailyReadinessGoalInputs(
        stepsGoal = dailyGoalFor(MetricDailyGoalKey.STEPS),
        hydrationLitersGoal = hydrationDailyGoalLiters,
        activeMinutesGoal = dailyGoalFor(MetricDailyGoalKey.ACTIVE_CALORIES_KCAL) / 10.0,
    )

private data class HomeDashboardWidgetResult(
    val data: DashboardData,
    val unitFormatter: UnitFormatter,
)

private val ReadinessWidgetMetrics = setOf(
    DashboardMetric.SLEEP,
    DashboardMetric.WORKOUT,
    DashboardMetric.AVG_HEART_RATE,
    DashboardMetric.RESTING_HEART_RATE,
    DashboardMetric.HRV,
    DashboardMetric.BODY_TEMPERATURE,
    DashboardMetric.SKIN_TEMPERATURE,
    DashboardMetric.WEEKLY_CARDIO_LOAD,
    DashboardMetric.INTENSITY_MINUTES,
    DashboardMetric.HYDRATION,
    DashboardMetric.CALORIES_IN,
    DashboardMetric.PROTEIN,
    DashboardMetric.CARBS,
    DashboardMetric.FAT,
    DashboardMetric.MINDFULNESS,
)

private val TodayVitalsMetrics = setOf(
    DashboardMetric.SLEEP,
    DashboardMetric.STEPS,
    DashboardMetric.DISTANCE,
    DashboardMetric.RESTING_HEART_RATE,
    DashboardMetric.HRV,
    DashboardMetric.WEEKLY_CARDIO_LOAD,
    DashboardMetric.HYDRATION,
)

/** Below this the leftover strip is too thin to read a day off. */
private const val BodyEnergyMinPlotWidth = 48f

/** Below this there is no room for the start/charged lines under the score. */
private const val BodyEnergyRowsMinHeight = 96f

private val BodyEnergyPadding = 16.dp

private val HomeStatusWidgetSizes = setOf(
    DpSize(220.dp, 110.dp),
    DpSize(320.dp, 140.dp),
)

private val HomeTodayWidgetSizes = setOf(
    DpSize(320.dp, 280.dp),
    DpSize(420.dp, 320.dp),
    DpSize(520.dp, 360.dp),
)

private const val TodayVitalsWidgetMaxRows = 12
private const val CompactTodayVitalsWidgetMaxRows = 8
