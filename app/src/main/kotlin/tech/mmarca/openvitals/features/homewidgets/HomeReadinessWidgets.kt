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
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.insights.DailyReadinessGoalInputs
import tech.mmarca.openvitals.domain.insights.DailyReadinessInsight
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.ReadinessFactorKind
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
    override val sizeMode = SizeMode.Responsive(HomeStatusWidgetSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            HomeWidgetContentFromState(context.getString(R.string.screen_body_energy))
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

suspend fun refreshDailyReadinessWidget(context: Context, appWidgetId: Int) {
    if (!hasAppWidgetInfo(context, appWidgetId)) return

    val glanceId = glanceAppWidgetId(appWidgetId)
    val snapshot = loadDailyReadinessSnapshot(context)
    writeHomeWidgetSnapshot(context, glanceId, "daily_readiness", snapshot)
    HomeDailyReadinessWidget().update(context, glanceId)
}

suspend fun refreshBodyEnergyWidget(context: Context, appWidgetId: Int) {
    if (!hasAppWidgetInfo(context, appWidgetId)) return

    val glanceId = glanceAppWidgetId(appWidgetId)
    val snapshot = loadBodyEnergySnapshot(context)
    writeHomeWidgetSnapshot(context, glanceId, "body_energy", snapshot)
    HomeBodyEnergyWidget().update(context, glanceId)
}

suspend fun refreshTodayVitalsWidget(context: Context, appWidgetId: Int) {
    if (!hasAppWidgetInfo(context, appWidgetId)) return

    val glanceId = glanceAppWidgetId(appWidgetId)
    val snapshot = loadTodayVitalsSnapshot(context)
    writeHomeWidgetSnapshot(context, glanceId, "today_vitals", snapshot)
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

private suspend fun loadDailyReadinessSnapshot(context: Context): HomeMetricWidgetSnapshot {
    val insight = loadReadinessInsight(context)
    val title = context.getString(R.string.screen_daily_readiness)
    val route = Screen.DailyReadiness.route
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

private suspend fun loadBodyEnergySnapshot(context: Context): HomeMetricWidgetSnapshot {
    val insight = loadReadinessInsight(context)
    val title = context.getString(R.string.screen_body_energy)
    val route = Screen.BodyEnergyDetails.createRoute(LocalDate.now().toString())
    if (insight == null || insight.state == ReadinessState.UNKNOWN) {
        return fallbackStatusSnapshot(context, title, route)
    }
    return HomeMetricWidgetSnapshot(
        title = title,
        value = insight.bodyEnergyScore.toString(),
        unit = "",
        subtitle = bodyEnergyStatus(context, insight.bodyEnergyScore),
        route = route,
        rows = listOf(
            HomeMetricWidgetRow(
                label = context.getString(R.string.home_widget_context),
                value = insight.bodyEnergyContext(context),
            )
        ),
    )
}

private suspend fun loadTodayVitalsSnapshot(context: Context): HomeMetricWidgetSnapshot {
    val dashboardResult = loadDashboardResult(context, TodayVitalsMetrics) ?: return todayVitalsFallbackSnapshot(context)
    val readinessInsight = loadReadinessInsight(context)
    val rows = buildList {
        if (readinessInsight != null && readinessInsight.state != ReadinessState.UNKNOWN) {
            add(readinessRow(context, readinessInsight))
            add(bodyEnergyRow(context, readinessInsight))
        }
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

private suspend fun loadReadinessInsight(context: Context): DailyReadinessInsight? =
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
                    sleepRangeMode = preferences.sleepRangeMode,
                    activityWeekMode = preferences.activityWeekMode,
                    visibleMetrics = ReadinessWidgetMetrics,
                    refreshMode = RefreshMode.NORMAL,
                )
            )
        } ?: return@runCatching null
        calculateDailyReadiness(data, preferences.homeReadinessGoals())
    }.getOrNull()

private suspend fun loadDashboardResult(
    context: Context,
    metrics: Set<DashboardMetric>,
): HomeDashboardWidgetResult? =
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
                    sleepRangeMode = preferences.sleepRangeMode,
                    activityWeekMode = preferences.activityWeekMode,
                    visibleMetrics = metrics,
                    refreshMode = RefreshMode.FORCE,
                    includeHistoricalBaselines = false,
                    includeWeeklyTrainingSignals = DashboardMetric.WEEKLY_CARDIO_LOAD in metrics ||
                        DashboardMetric.INTENSITY_MINUTES in metrics,
                )
            )
        } ?: return@runCatching null
        HomeDashboardWidgetResult(
            data = data,
            unitFormatter = entryPoint.unitFormatter(),
        )
    }.getOrNull()

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

private fun bodyEnergyRow(context: Context, insight: DailyReadinessInsight?): HomeMetricWidgetRow =
    if (insight == null || insight.state == ReadinessState.UNKNOWN) {
        fallbackRow(context, context.getString(R.string.screen_body_energy))
    } else {
        HomeMetricWidgetRow(
            label = context.getString(R.string.screen_body_energy),
            value = insight.bodyEnergyScore.toString(),
            subtitle = bodyEnergyStatus(context, insight.bodyEnergyScore),
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

private fun DailyReadinessInsight.bodyEnergyContext(context: Context): String =
    factors.firstOrNull { factor -> factor.kind in BodyEnergyWidgetFactorKinds }
        ?.let { factor -> "${factor.label}: ${factor.detail}" }
        ?: context.getString(R.string.body_energy_details_summary)

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

private val BodyEnergyWidgetFactorKinds = setOf(
    ReadinessFactorKind.SLEEP_BELOW_BASELINE,
    ReadinessFactorKind.SLEEP_ABOVE_BASELINE,
    ReadinessFactorKind.RESTING_HR_ELEVATED,
    ReadinessFactorKind.RESTING_HR_NORMAL,
    ReadinessFactorKind.HRV_BELOW_BASELINE,
    ReadinessFactorKind.HRV_ABOVE_BASELINE,
    ReadinessFactorKind.HRV_NORMAL,
    ReadinessFactorKind.PHYSIOLOGICAL_STRESS_HIGH,
    ReadinessFactorKind.PHYSIOLOGICAL_STRESS_LOW,
    ReadinessFactorKind.STRESS_HIGH,
    ReadinessFactorKind.STRESS_LOW,
    ReadinessFactorKind.TEMPERATURE_ELEVATED,
    ReadinessFactorKind.HYDRATION_LOW,
    ReadinessFactorKind.NUTRITION_LOGGED,
    ReadinessFactorKind.MISSING_SLEEP_DATA,
    ReadinessFactorKind.MISSING_HRV_DATA,
    ReadinessFactorKind.MISSING_STRESS_DATA,
    ReadinessFactorKind.NEW_USER_NOT_ENOUGH_BASELINE,
)

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
