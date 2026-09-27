package tech.mmarca.openvitals.features.homewidgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.data.repository.contract.CycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.domain.cycle.CycleCalculations
import tech.mmarca.openvitals.domain.cycle.CycleEstimate
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.features.cycle.cycleEstimateLine
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.navigation.Screen

/**
 * The cycle on the home screen: the recorded cycle day and the estimated
 * range, never notes, symptoms, mood or energy. A hide button swaps the
 * data for a neutral line until the user shows it again.
 */
class HomeCycleWidget : GlanceAppWidget() {
    override val stateDefinition = HomeCycleWidgetState.definition
    override val sizeMode = SizeMode.Responsive(HomeCycleWidgetSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { HomeCycleWidgetContent() }
    }
}

class HomeCycleWidgetReceiver : UpdatingHomeWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HomeCycleWidget()
}

/** Hides or shows the data. The snapshot stays in the state, so showing is instant. No data is read here. */
class HomeCycleWidgetPrivacyAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appContext = context.applicationContext
        updateAppWidgetState(appContext, HomeCycleWidgetState.definition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply {
                this[HomeCycleWidgetState.concealedKey] = !(preferences[HomeCycleWidgetState.concealedKey] ?: false)
            }
        }
        HomeCycleWidget().update(appContext, glanceId)
    }
}

object HomeCycleWidgetState {
    val concealedKey = booleanPreferencesKey("cycle_concealed")
    val statusKey = stringPreferencesKey("cycle_status")
    /** ISO dates: the day and the text are computed when the widget draws, so midnight cannot stale them. */
    val cycleStartKey = stringPreferencesKey("cycle_start")
    val estimateKindKey = stringPreferencesKey("cycle_estimate_kind")
    val estimateEarliestKey = stringPreferencesKey("cycle_estimate_earliest")
    val estimateCentralKey = stringPreferencesKey("cycle_estimate_central")
    val estimateLatestKey = stringPreferencesKey("cycle_estimate_latest")
    val loggedTodayKey = booleanPreferencesKey("cycle_logged_today")
    val definition = androidx.glance.state.PreferencesGlanceStateDefinition

    const val StatusAvailable = "available"
    const val StatusNoCycle = "no_cycle"
    const val StatusPermission = "permission"
    const val StatusUnavailable = "unavailable"

    const val EstimateNeedsHistory = "needs_history"
    const val EstimateOutOfRange = "out_of_range"
    const val EstimateAvailable = "available"

    internal val dataKeys: List<Preferences.Key<*>> = listOf(
        statusKey, cycleStartKey, estimateKindKey, estimateEarliestKey, estimateCentralKey, estimateLatestKey, loggedTodayKey,
    )
}

/** What a revealed widget draws from. Only the estimate's dates are kept, never its history. */
internal data class HomeCycleSnapshot(
    val status: String,
    val cycleStart: LocalDate? = null,
    val estimate: CycleEstimateResult? = null,
    val loggedToday: Boolean = false,
) {
    companion object {
        /** Null statistics mean the read permission is missing, not that there is no cycle. */
        fun from(statistics: CycleStatistics?, loggedToday: Boolean): HomeCycleSnapshot {
            if (statistics == null) return HomeCycleSnapshot(HomeCycleWidgetState.StatusPermission)
            val current = statistics.currentCycle
            return if (current == null || statistics.currentCycleDay == null) {
                HomeCycleSnapshot(HomeCycleWidgetState.StatusNoCycle, estimate = statistics.estimate, loggedToday = loggedToday)
            } else {
                HomeCycleSnapshot(
                    HomeCycleWidgetState.StatusAvailable,
                    cycleStart = current.startDate,
                    estimate = statistics.estimate,
                    loggedToday = loggedToday,
                )
            }
        }
    }
}

internal fun MutablePreferences.clearCycleSnapshot() {
    HomeCycleWidgetState.dataKeys.forEach { key -> remove(key) }
}

internal fun MutablePreferences.putCycleSnapshot(snapshot: HomeCycleSnapshot) {
    clearCycleSnapshot()
    this[HomeCycleWidgetState.statusKey] = snapshot.status
    this[HomeCycleWidgetState.loggedTodayKey] = snapshot.loggedToday
    snapshot.cycleStart?.let { this[HomeCycleWidgetState.cycleStartKey] = it.toString() }
    when (val estimate = snapshot.estimate) {
        null -> Unit
        CycleEstimateResult.NeedsMoreHistory -> this[HomeCycleWidgetState.estimateKindKey] = HomeCycleWidgetState.EstimateNeedsHistory
        CycleEstimateResult.IntervalsOutOfRange -> this[HomeCycleWidgetState.estimateKindKey] = HomeCycleWidgetState.EstimateOutOfRange
        is CycleEstimateResult.Available -> {
            this[HomeCycleWidgetState.estimateKindKey] = HomeCycleWidgetState.EstimateAvailable
            this[HomeCycleWidgetState.estimateEarliestKey] = estimate.estimate.earliestDate.toString()
            this[HomeCycleWidgetState.estimateCentralKey] = estimate.estimate.centralDate.toString()
            this[HomeCycleWidgetState.estimateLatestKey] = estimate.estimate.latestDate.toString()
        }
    }
}

internal fun Preferences.toCycleSnapshot(): HomeCycleSnapshot = HomeCycleSnapshot(
    status = this[HomeCycleWidgetState.statusKey] ?: HomeCycleWidgetState.StatusUnavailable,
    cycleStart = isoDate(HomeCycleWidgetState.cycleStartKey),
    estimate = when (this[HomeCycleWidgetState.estimateKindKey]) {
        HomeCycleWidgetState.EstimateNeedsHistory -> CycleEstimateResult.NeedsMoreHistory
        HomeCycleWidgetState.EstimateOutOfRange -> CycleEstimateResult.IntervalsOutOfRange
        HomeCycleWidgetState.EstimateAvailable -> {
            val earliest = isoDate(HomeCycleWidgetState.estimateEarliestKey)
            val central = isoDate(HomeCycleWidgetState.estimateCentralKey)
            val latest = isoDate(HomeCycleWidgetState.estimateLatestKey)
            if (earliest != null && central != null && latest != null) {
                CycleEstimateResult.Available(CycleEstimate(earliest, central, latest, cycleCount = 0, variabilityDays = 0))
            } else {
                null
            }
        }
        else -> null
    },
    loggedToday = this[HomeCycleWidgetState.loggedTodayKey] ?: false,
)

private fun Preferences.isoDate(key: Preferences.Key<String>): LocalDate? =
    this[key]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** Day 1 is the start; blank past the displayable limit, as on the cycle screen. */
internal fun cycleDayFor(start: LocalDate, today: LocalDate): Int? =
    (ChronoUnit.DAYS.between(start, today) + 1).toInt().takeIf { it in 1..CycleCalculations.MaxDisplayableCycleDay }

/** Today's day log, resolved when the app opens: a dated route would go stale after midnight. */
internal fun homeCycleWidgetEntryRoute(): String = Screen.CycleEntry.route

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeCycleWidgetEntryPoint {
    fun cycleRepository(): CycleRepository
    fun cycleJournalRepository(): CycleJournalRepository
}

/**
 * Re-reads today's cycle facts into the widget state. The hide flag is left
 * alone. A read that throws aborts before the write, so the last snapshot stays.
 */
suspend fun refreshHomeCycleWidget(context: Context, appWidgetId: Int) {
    val appContext = context.applicationContext
    if (!hasAppWidgetInfo(appContext, appWidgetId)) return
    val glanceId = glanceAppWidgetId(appWidgetId)
    val widget = HomeCycleWidget()
    val entryPoint = EntryPointAccessors.fromApplication(appContext, HomeCycleWidgetEntryPoint::class.java)
    val today = LocalDate.now()
    val statistics = entryPoint.cycleRepository().loadCycleStatistics(today)
    val loggedToday = entryPoint.cycleJournalRepository().entry(today)?.hasObservations == true ||
        statistics?.currentCycle?.flowDays?.containsKey(today) == true
    val snapshot = HomeCycleSnapshot.from(statistics, loggedToday)
    updateAppWidgetState(appContext, HomeCycleWidgetState.definition, glanceId) { preferences ->
        preferences.toMutablePreferences().apply { putCycleSnapshot(snapshot) }
    }
    widget.update(appContext, glanceId)
}

@Composable
private fun HomeCycleWidgetContent() {
    val context = LocalContext.current
    val preferences = currentState<Preferences>()
    val concealed = preferences[HomeCycleWidgetState.concealedKey] ?: false
    val size = LocalSize.current
    val expanded = size.height >= HomeCycleWidgetSizes.maxOf { it.height }
    val openCycle = actionStartActivity(
        openMetricIntent(context, Screen.Metric.createRoute(DashboardWidgetId.CYCLE.name)),
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .clickable(openCycle)
            .padding(CyclePadding),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = context.getString(R.string.widget_cycle_name),
                style = TextStyle(color = ColorProvider(WidgetMutedText), fontSize = LabelSize),
                modifier = GlanceModifier.defaultWeight(),
            )
            Box(
                modifier = GlanceModifier
                    .height(WidgetTouchTarget)
                    .clickable(actionRunCallback<HomeCycleWidgetPrivacyAction>()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = context.getString(if (concealed) R.string.widget_cycle_reveal else R.string.widget_cycle_conceal),
                    style = TextStyle(color = ColorProvider(WidgetPrimaryText), fontSize = LabelSize, fontWeight = FontWeight.Medium),
                )
            }
        }
        Spacer(GlanceModifier.height(CycleGap))
        if (concealed) {
            Text(
                text = context.getString(R.string.widget_cycle_concealed),
                style = TextStyle(color = ColorProvider(WidgetPrimaryText), fontSize = TitleSize, fontWeight = FontWeight.Medium),
            )
            return@Column
        }
        HomeCycleRevealed(snapshot = preferences.toCycleSnapshot(), expanded = expanded)
    }
}

@Composable
private fun HomeCycleRevealed(snapshot: HomeCycleSnapshot, expanded: Boolean) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val formatter = DateTimeFormatterProvider().mediumDate()
    val cycleDay = snapshot.cycleStart?.let { cycleDayFor(it, today) }
    val hasData = snapshot.status == HomeCycleWidgetState.StatusAvailable || snapshot.status == HomeCycleWidgetState.StatusNoCycle
    when {
        snapshot.status == HomeCycleWidgetState.StatusAvailable && cycleDay != null && snapshot.cycleStart != null -> {
            Text(
                text = context.getString(R.string.widget_cycle_day, cycleDay),
                style = TextStyle(color = ColorProvider(WidgetPrimaryText), fontSize = ValueSize, fontWeight = FontWeight.Bold),
            )
            Text(
                text = context.getString(R.string.widget_cycle_recorded_since, formatter.format(snapshot.cycleStart)),
                style = TextStyle(color = ColorProvider(WidgetMutedText), fontSize = LabelSize),
            )
        }
        hasData -> Text(
            text = context.getString(R.string.widget_cycle_no_cycle),
            style = TextStyle(color = ColorProvider(WidgetPrimaryText), fontSize = TitleSize, fontWeight = FontWeight.Medium),
        )
        snapshot.status == HomeCycleWidgetState.StatusPermission -> Text(
            text = context.getString(R.string.home_metric_widget_permission_needed),
            style = TextStyle(color = ColorProvider(WidgetPrimaryText), fontSize = TitleSize),
        )
        else -> Text(
            text = context.getString(R.string.widget_cycle_unavailable),
            style = TextStyle(color = ColorProvider(WidgetPrimaryText), fontSize = TitleSize),
        )
    }
    snapshot.estimate?.let { estimate ->
        Spacer(GlanceModifier.height(CycleGap))
        Text(
            text = cycleEstimateLine(context, estimate, today, formatter),
            style = TextStyle(color = ColorProvider(WidgetMutedText), fontSize = LabelSize),
            maxLines = 2,
        )
    }
    if (expanded && hasData) {
        val logged = snapshot.loggedToday
        Spacer(GlanceModifier.height(CycleGap))
        Text(
            text = context.getString(if (logged) R.string.widget_cycle_today_recorded else R.string.widget_cycle_today_empty),
            style = TextStyle(color = ColorProvider(WidgetMutedText), fontSize = LabelSize),
        )
        Text(
            text = context.getString(if (logged) R.string.widget_cycle_edit_today else R.string.widget_cycle_add_today),
            style = TextStyle(color = ColorProvider(WidgetPrimaryText), fontSize = LabelSize, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier
                .clickable(actionStartActivity(openMetricIntent(context, homeCycleWidgetEntryRoute())))
                .padding(vertical = CycleGap),
        )
    }
}

private val HomeCycleWidgetSizes = setOf(
    DpSize(220.dp, 110.dp),
    DpSize(320.dp, 180.dp),
)
private val CyclePadding = 16.dp
private val CycleGap = 6.dp
private val WidgetTouchTarget = 48.dp
private val LabelSize = 12.sp
private val TitleSize = 15.sp
private val ValueSize = 24.sp
