package tech.mmarca.openvitals.features.homewidgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
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
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.MainActivity
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.repository.dashboard.DashboardDataLoader
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.features.dashboard.toDashboardMetricOrNull
import tech.mmarca.openvitals.navigation.EXTRA_OPENVITALS_ROUTE
import tech.mmarca.openvitals.navigation.Screen

class HomeMetricWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<Preferences> = HomeMetricWidgetState.definition

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(140.dp, 88.dp),
            DpSize(220.dp, 110.dp),
            DpSize(320.dp, 140.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val preferences = currentState<Preferences>()
            val snapshot = preferences.toWidgetSnapshot(context)
                ?: HomeMetricWidgetSnapshot(
                    title = context.getString(R.string.home_metric_widget_config_title),
                    value = "--",
                    unit = "",
                    subtitle = context.getString(R.string.home_metric_widget_not_configured),
                    route = Screen.Dashboard.route,
                )
            HomeMetricWidgetContent(snapshot = snapshot)
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        homeMetricWidgetSelection(context).clearMetric(glanceId)
        super.onDelete(context, glanceId)
    }
}

class HomeMetricWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HomeMetricWidget()

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
                    refreshHomeMetricWidget(context, appWidgetId)
                }
            } catch (throwable: Throwable) {
                Log.e(HomeWidgetLogTag, "Home metric widget update failed", throwable)
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

object HomeMetricWidgetState {
    const val metricIdOptionKey = "metric_id"
    val metricIdKey = stringPreferencesKey(metricIdOptionKey)
    val titleKey = stringPreferencesKey("title")
    val valueKey = stringPreferencesKey("value")
    val unitKey = stringPreferencesKey("unit")
    val subtitleKey = stringPreferencesKey("subtitle")
    val routeKey = stringPreferencesKey("route")
    val rowCountKey = intPreferencesKey("row_count")
    val definition = androidx.glance.state.PreferencesGlanceStateDefinition
}

class HomeMetricWidgetSelection(private val context: Context) {
    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
    private val preferences = context.applicationContext.getSharedPreferences(
        "home_metric_widgets",
        Context.MODE_PRIVATE,
    )

    fun metricFor(glanceId: GlanceId): DashboardWidgetId? =
        glanceId.appWidgetIdOrNull()
            ?.let(::metricForAppWidgetId)

    private fun metricForAppWidgetId(appWidgetId: Int): DashboardWidgetId? {
        val optionMetricId = appWidgetManager.getAppWidgetOptions(appWidgetId)
            ?.getString(HomeMetricWidgetState.metricIdOptionKey)
            .toDashboardWidgetIdOrNull()
        val sharedMetricId = preferences.getString(metricKey(appWidgetId), null)
            .toDashboardWidgetIdOrNull()
            ?.also { metricId -> updateMetricOptions(appWidgetId, metricId) }
        val pendingMetricId = if (optionMetricId == null && sharedMetricId == null) {
            adoptPendingMetric(appWidgetId)
        } else {
            null
        }
        return optionMetricId ?: sharedMetricId ?: pendingMetricId
    }

    @SuppressLint("ApplySharedPref")
    fun setMetric(appWidgetId: Int, metricId: DashboardWidgetId) {
        preferences.edit(commit = true) {
            putString(metricKey(appWidgetId), metricId.name)
            putString(PendingMetricIdKey, metricId.name)
            putLong(PendingMetricTimestampKey, System.currentTimeMillis())
        }
    }

    fun clearMetric(glanceId: GlanceId) {
        val appWidgetId = glanceId.appWidgetIdOrNull() ?: return
        preferences.edit {
            remove(metricKey(appWidgetId))
        }
        val options = Bundle(appWidgetManager.getAppWidgetOptions(appWidgetId))
        options.remove(HomeMetricWidgetState.metricIdOptionKey)
        appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
    }

    private fun metricKey(appWidgetId: Int): String = "metric_id_$appWidgetId"

    private fun adoptPendingMetric(appWidgetId: Int): DashboardWidgetId? {
        val timestamp = preferences.getLong(PendingMetricTimestampKey, 0L)
        if (System.currentTimeMillis() - timestamp > PendingMetricTtlMillis) return null
        val metricId = preferences.getString(PendingMetricIdKey, null).toDashboardWidgetIdOrNull() ?: return null

        preferences.edit {
            putString(metricKey(appWidgetId), metricId.name)
        }
        updateMetricOptions(appWidgetId, metricId)

        return metricId
    }

    private fun updateMetricOptions(appWidgetId: Int, metricId: DashboardWidgetId) {
        val options = Bundle(appWidgetManager.getAppWidgetOptions(appWidgetId))
        options.putString(HomeMetricWidgetState.metricIdOptionKey, metricId.name)
        appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
    }

    private companion object {
        const val PendingMetricIdKey = "pending_metric_id"
        const val PendingMetricTimestampKey = "pending_metric_timestamp"
        const val PendingMetricTtlMillis = 2 * 60 * 1_000L
    }
}

fun homeMetricWidgetSelection(context: Context): HomeMetricWidgetSelection =
    HomeMetricWidgetSelection(context)

data class HomeMetricWidgetSnapshot(
    val title: String,
    val value: String,
    val unit: String,
    val subtitle: String,
    val route: String,
    val rows: List<HomeMetricWidgetRow> = emptyList(),
)

data class HomeMetricWidgetRow(
    val label: String,
    val value: String,
    val subtitle: String = "",
)

suspend fun refreshHomeMetricWidget(
    context: Context,
    appWidgetId: Int,
    metricId: DashboardWidgetId? = null,
) {
    if (!hasAppWidgetInfo(context, appWidgetId)) return

    val glanceId = glanceAppWidgetId(appWidgetId)
    val preferencesMetricId = getAppWidgetState(context, HomeMetricWidgetState.definition, glanceId)[HomeMetricWidgetState.metricIdKey]
        .toDashboardWidgetIdOrNull()
    val resolvedMetricId = metricId
        ?: homeMetricWidgetSelection(context).metricFor(glanceId)
        ?: preferencesMetricId

    if (resolvedMetricId == null) {
        HomeMetricWidget().update(context, glanceId)
        return
    }

    val snapshot = loadSnapshot(context, resolvedMetricId)
    writeHomeWidgetSnapshot(context, glanceId, resolvedMetricId.name, snapshot)
    HomeMetricWidget().update(context, glanceId)
}

internal fun hasAppWidgetInfo(context: Context, appWidgetId: Int): Boolean =
    AppWidgetManager.getInstance(context.applicationContext).getAppWidgetInfo(appWidgetId) != null

internal suspend fun writeHomeWidgetSnapshot(
    context: Context,
    glanceId: GlanceId,
    metricId: String?,
    snapshot: HomeMetricWidgetSnapshot,
) {
    updateAppWidgetState(
        context = context,
        glanceId = glanceId,
    ) { preferences ->
        if (metricId == null) {
            preferences.remove(HomeMetricWidgetState.metricIdKey)
        } else {
            preferences[HomeMetricWidgetState.metricIdKey] = metricId
        }
        preferences[HomeMetricWidgetState.titleKey] = snapshot.title
        preferences[HomeMetricWidgetState.valueKey] = snapshot.value
        preferences[HomeMetricWidgetState.unitKey] = snapshot.unit
        preferences[HomeMetricWidgetState.subtitleKey] = snapshot.subtitle
        preferences[HomeMetricWidgetState.routeKey] = snapshot.route
        preferences[HomeMetricWidgetState.rowCountKey] = snapshot.rows.size.coerceAtMost(MaxHomeWidgetRows)
        for (index in 0 until MaxHomeWidgetRows) {
            val row = snapshot.rows.getOrNull(index)
            if (row == null) {
                preferences.remove(homeWidgetRowLabelKey(index))
                preferences.remove(homeWidgetRowValueKey(index))
                preferences.remove(homeWidgetRowSubtitleKey(index))
            } else {
                preferences[homeWidgetRowLabelKey(index)] = row.label
                preferences[homeWidgetRowValueKey(index)] = row.value
                preferences[homeWidgetRowSubtitleKey(index)] = row.subtitle
            }
        }
    }
}

internal fun Preferences.toWidgetSnapshot(context: Context): HomeMetricWidgetSnapshot? {
    val title = this[HomeMetricWidgetState.titleKey] ?: return null
    val rowCount = (this[HomeMetricWidgetState.rowCountKey] ?: 0).coerceIn(0, MaxHomeWidgetRows)
    val rows = (0 until rowCount).mapNotNull { index ->
        val label = this[homeWidgetRowLabelKey(index)] ?: return@mapNotNull null
        val value = this[homeWidgetRowValueKey(index)] ?: return@mapNotNull null
        HomeMetricWidgetRow(
            label = label,
            value = value,
            subtitle = this[homeWidgetRowSubtitleKey(index)].orEmpty(),
        )
    }
    return HomeMetricWidgetSnapshot(
        title = title,
        value = this[HomeMetricWidgetState.valueKey] ?: "--",
        unit = this[HomeMetricWidgetState.unitKey].orEmpty(),
        subtitle = this[HomeMetricWidgetState.subtitleKey].orEmpty(),
        route = this[HomeMetricWidgetState.routeKey] ?: Screen.Dashboard.route,
        rows = rows,
    )
}

@Composable
internal fun HomeMetricWidgetContent(snapshot: HomeMetricWidgetSnapshot) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .clickable(actionStartActivity(openMetricIntent(context, snapshot.route)))
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = snapshot.title,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(WidgetMutedText),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (snapshot.value.isNotBlank()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.Bottom,
            ) {
                Text(
                    text = snapshot.value,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(WidgetPrimaryText),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                if (snapshot.unit.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = snapshot.unit,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(WidgetMutedText),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
        if (snapshot.subtitle.isNotBlank()) {
            Text(
                text = snapshot.subtitle,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WidgetMutedText),
                    fontSize = 12.sp,
                ),
            )
        }
        if (snapshot.rows.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            snapshot.rows.forEach { row ->
                Text(
                    text = row.displayText(),
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(WidgetPrimaryText),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
        }
    }
}

internal suspend fun loadSnapshot(
    context: Context,
    metricId: DashboardWidgetId,
): HomeMetricWidgetSnapshot {
    val title = context.getString(metricId.homeMetricTitleRes())
    val route = Screen.Metric.createRoute(metricId.name)
    return runCatching {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HomeMetricWidgetEntryPoint::class.java,
        )
        val metric = metricId.toDashboardMetricOrNull()
        val data = if (metric != null) {
            withTimeoutOrNull(WidgetLoadTimeoutMillis) {
                entryPoint.dashboardDataLoader().loadDashboard(
                    DashboardQuery(
                        date = LocalDate.now(),
                        sleepRangeMode = entryPoint.preferencesRepository().sleepRangeMode,
                        activityWeekMode = entryPoint.preferencesRepository().activityWeekMode,
                        visibleMetrics = setOf(metric),
                        refreshMode = RefreshMode.FORCE,
                        includeHistoricalBaselines = false,
                        includeWeeklyTrainingSignals = metric == DashboardMetric.WEEKLY_CARDIO_LOAD,
                    )
                )
            } ?: return@runCatching HomeMetricWidgetSnapshot(
                title = title,
                value = "--",
                unit = "",
                subtitle = context.getString(R.string.home_metric_widget_open_for_details),
                route = route,
            )
        } else {
            DashboardData(date = LocalDate.now())
        }
        if (data.missingPermissions.isNotEmpty()) {
            HomeMetricWidgetSnapshot(
                title = title,
                value = "--",
                unit = "",
                subtitle = context.getString(R.string.home_metric_widget_permission_needed),
                route = route,
            )
        } else {
            data.toSnapshot(
                context = context,
                metricId = metricId,
                title = title,
                route = route,
                unitFormatter = entryPoint.unitFormatter(),
            )
        }
    }.getOrElse {
        HomeMetricWidgetSnapshot(
            title = title,
            value = "--",
            unit = "",
            subtitle = context.getString(R.string.home_metric_widget_update_failed),
            route = route,
        )
    }
}

internal fun DashboardData.toSnapshot(
    context: Context,
    metricId: DashboardWidgetId,
    title: String,
    route: String,
    unitFormatter: UnitFormatter,
): HomeMetricWidgetSnapshot {
    fun snapshot(displayValue: DisplayValue?, subtitle: String = context.getString(R.string.period_today)) =
        HomeMetricWidgetSnapshot(
            title = title,
            value = displayValue?.value ?: "--",
            unit = displayValue?.unit.orEmpty(),
            subtitle = if (displayValue == null) context.getString(R.string.no_data) else subtitle,
            route = route,
        )

    fun count(value: Long?, unit: String): HomeMetricWidgetSnapshot =
        snapshot(value?.let { DisplayValue(unitFormatter.count(it), unit) })

    fun count(value: Int?, unit: String): HomeMetricWidgetSnapshot =
        snapshot(value?.let { DisplayValue(unitFormatter.count(it), unit) })

    fun countOnly(value: Long?): HomeMetricWidgetSnapshot =
        snapshot(value?.let { DisplayValue(unitFormatter.count(it), "") })

    fun countOnly(value: Int?): HomeMetricWidgetSnapshot =
        snapshot(value?.let { DisplayValue(unitFormatter.count(it), "") })

    return when (metricId) {
        DashboardWidgetId.STEPS -> countOnly(steps)
        DashboardWidgetId.DISTANCE -> snapshot(unitFormatter.distance(distanceMeters).takeIf { distanceMeters > 0.0 })
        DashboardWidgetId.CALORIES_OUT -> snapshot(unitFormatter.energy(caloriesKcal).takeIf { caloriesKcal > 0.0 })
        DashboardWidgetId.ACTIVE_CALORIES -> snapshot(activeCaloriesKcal?.let(unitFormatter::energy))
        DashboardWidgetId.FLOORS -> countOnly(floorsClimbed)
        DashboardWidgetId.ELEVATION -> snapshot(elevationGainedMeters?.let(unitFormatter::elevation))
        DashboardWidgetId.WHEELCHAIR_PUSHES -> countOnly(wheelchairPushes)
        DashboardWidgetId.WORKOUT -> countOnly(workouts.size.takeIf { it > 0 })
        DashboardWidgetId.SLEEP -> snapshot(sleep?.let { DisplayValue(unitFormatter.duration(it.durationMs), "") })
        DashboardWidgetId.HYDRATION -> snapshot(unitFormatter.hydration(hydrationLiters).takeIf { hydrationLiters > 0.0 })
        DashboardWidgetId.CALORIES_IN -> snapshot(caloriesInKcal?.let(unitFormatter::energy))
        DashboardWidgetId.PROTEIN -> snapshot(proteinGrams?.let { DisplayValue(unitFormatter.decimal(it, 0), context.getString(R.string.unit_grams)) })
        DashboardWidgetId.CARBS -> snapshot(carbsGrams?.let { DisplayValue(unitFormatter.decimal(it, 0), context.getString(R.string.unit_grams)) })
        DashboardWidgetId.FAT -> snapshot(fatGrams?.let { DisplayValue(unitFormatter.decimal(it, 0), context.getString(R.string.unit_grams)) })
        DashboardWidgetId.WEIGHT -> snapshot(weightKg?.let(unitFormatter::weight))
        DashboardWidgetId.HEIGHT -> snapshot(heightCm?.let(unitFormatter::height))
        DashboardWidgetId.BMI -> snapshot(bmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") })
        DashboardWidgetId.FFMI -> snapshot(ffmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") })
        DashboardWidgetId.BODY_FAT -> snapshot(bodyFatPercent.takeIf { it > 0.0 }?.let { unitFormatter.percent(it) })
        DashboardWidgetId.LEAN_MASS -> snapshot(leanMassKg?.let(unitFormatter::bodyMass))
        DashboardWidgetId.BMR -> snapshot(bmrKcal?.let(unitFormatter::energy))
        DashboardWidgetId.BONE_MASS -> snapshot(boneMassKg?.let(unitFormatter::bodyMass))
        DashboardWidgetId.BODY_WATER_MASS -> snapshot(bodyWaterMassKg?.let(unitFormatter::bodyMass))
        DashboardWidgetId.AVG_HEART_RATE -> snapshot(avgHeartRateBpm.takeIf { it > 0 }?.let(unitFormatter::heartRate))
        DashboardWidgetId.RESTING_HEART_RATE -> snapshot(restingHeartRateBpm.takeIf { it > 0 }?.let(unitFormatter::heartRate))
        DashboardWidgetId.HRV -> snapshot(hrvRmssdMs?.let(unitFormatter::hrv))
        DashboardWidgetId.BLOOD_PRESSURE -> snapshot(
            latestSystolicMmHg?.let { systolic ->
                latestDiastolicMmHg?.let { diastolic -> unitFormatter.bloodPressure(systolic, diastolic) }
            }
        )
        DashboardWidgetId.SPO2 -> snapshot(latestSpO2Percent?.let { unitFormatter.percent(it, decimals = 0) })
        DashboardWidgetId.VO2_MAX -> snapshot(latestVo2Max?.let(unitFormatter::vo2Max))
        DashboardWidgetId.RESPIRATORY_RATE -> snapshot(avgRespiratoryRate?.let(unitFormatter::respiratoryRate))
        DashboardWidgetId.BODY_TEMPERATURE -> snapshot(latestBodyTemperatureCelsius?.let(unitFormatter::temperature))
        DashboardWidgetId.BLOOD_GLUCOSE -> snapshot(latestBloodGlucoseMillimolesPerLiter?.let(unitFormatter::bloodGlucose))
        DashboardWidgetId.SKIN_TEMPERATURE -> snapshot(latestSkinTemperatureDeltaCelsius?.let(unitFormatter::temperatureDelta))
        DashboardWidgetId.WEEKLY_CARDIO_LOAD,
        DashboardWidgetId.CARDIO_LOAD -> snapshot(
            weeklyCardioLoad?.let { load ->
                DisplayValue(
                    value = context.getString(
                        R.string.dashboard_weekly_cardio_load_progress,
                        load.currentScore,
                        load.targetScore,
                    ),
                    unit = "",
                )
            },
            subtitle = weeklyCardioLoad?.let { context.getString(R.string.dashboard_cardio_load_percent, it.progressPercent) }
                ?: context.getString(R.string.no_data),
        )
        DashboardWidgetId.MINDFULNESS -> count(mindfulnessMinutes, "min")
        DashboardWidgetId.CYCLE -> {
            val displayValue = menstruationPeriodDays?.let { DisplayValue(unitFormatter.count(it), context.getString(R.string.unit_days)) }
                ?: ovulationTestCount?.let { DisplayValue(unitFormatter.count(it), context.getString(R.string.unit_tests)) }
                ?: latestBasalBodyTemperatureCelsius?.let(unitFormatter::temperature)
            snapshot(displayValue)
        }
    }
}

fun DashboardWidgetId.homeMetricTitleRes(): Int = when (this) {
    DashboardWidgetId.STEPS -> R.string.metric_steps
    DashboardWidgetId.DISTANCE -> R.string.metric_distance
    DashboardWidgetId.CALORIES_OUT -> R.string.metric_calories_out
    DashboardWidgetId.ACTIVE_CALORIES -> R.string.metric_active_calories
    DashboardWidgetId.FLOORS -> R.string.metric_floors_climbed
    DashboardWidgetId.ELEVATION -> R.string.metric_elevation_gained
    DashboardWidgetId.WHEELCHAIR_PUSHES -> R.string.metric_wheelchair_pushes
    DashboardWidgetId.WORKOUT -> R.string.metric_workout
    DashboardWidgetId.SLEEP -> R.string.metric_sleep
    DashboardWidgetId.HYDRATION -> R.string.metric_hydration
    DashboardWidgetId.CALORIES_IN -> R.string.metric_calories_in
    DashboardWidgetId.PROTEIN -> R.string.metric_protein
    DashboardWidgetId.CARBS -> R.string.metric_carbs
    DashboardWidgetId.FAT -> R.string.metric_fat
    DashboardWidgetId.WEIGHT -> R.string.metric_weight
    DashboardWidgetId.HEIGHT -> R.string.metric_height
    DashboardWidgetId.BMI -> R.string.metric_bmi
    DashboardWidgetId.FFMI -> R.string.metric_ffmi
    DashboardWidgetId.BODY_FAT -> R.string.metric_body_fat
    DashboardWidgetId.LEAN_MASS -> R.string.metric_lean_mass
    DashboardWidgetId.BMR -> R.string.metric_bmr
    DashboardWidgetId.BONE_MASS -> R.string.metric_bone_mass
    DashboardWidgetId.BODY_WATER_MASS -> R.string.metric_body_water_mass
    DashboardWidgetId.AVG_HEART_RATE -> R.string.metric_avg_heart_rate
    DashboardWidgetId.RESTING_HEART_RATE -> R.string.metric_resting_heart_rate
    DashboardWidgetId.HRV -> R.string.metric_hrv
    DashboardWidgetId.BLOOD_PRESSURE -> R.string.metric_blood_pressure
    DashboardWidgetId.SPO2 -> R.string.metric_spo2
    DashboardWidgetId.VO2_MAX -> R.string.metric_vo2_max
    DashboardWidgetId.RESPIRATORY_RATE -> R.string.metric_respiratory_rate
    DashboardWidgetId.BODY_TEMPERATURE -> R.string.metric_body_temp
    DashboardWidgetId.BLOOD_GLUCOSE -> R.string.metric_blood_glucose
    DashboardWidgetId.SKIN_TEMPERATURE -> R.string.metric_skin_temperature
    DashboardWidgetId.WEEKLY_CARDIO_LOAD,
    DashboardWidgetId.CARDIO_LOAD -> R.string.metric_weekly_cardio_load
    DashboardWidgetId.MINDFULNESS -> R.string.metric_mindfulness
    DashboardWidgetId.CYCLE -> R.string.metric_cycle
}

fun homeMetricWidgetCatalog(): List<DashboardWidgetId> =
    DashboardWidgetId.entries.filterNot { it == DashboardWidgetId.CARDIO_LOAD }

private fun String?.toDashboardWidgetIdOrNull(): DashboardWidgetId? =
    this?.let { stored -> runCatching { DashboardWidgetId.valueOf(stored) }.getOrNull() }

@SuppressLint("RestrictedApi")
internal fun glanceAppWidgetId(appWidgetId: Int): GlanceId = AppWidgetId(appWidgetId)

@SuppressLint("RestrictedApi")
private fun GlanceId.appWidgetIdOrNull(): Int? = (this as? AppWidgetId)?.appWidgetId

internal fun openMetricIntent(context: Context, route: String): Intent =
    Intent(Intent.ACTION_VIEW).apply {
        setClassName(context.packageName, MainActivity::class.java.name)
        putExtra(EXTRA_OPENVITALS_ROUTE, route)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeMetricWidgetEntryPoint {
    fun dashboardDataLoader(): DashboardDataLoader
    fun preferencesRepository(): PreferencesRepository
    fun unitFormatter(): UnitFormatter
}

internal val WidgetBackground = Color(0xFF101820)
internal val WidgetPrimaryText = Color(0xFFF7FAFC)
internal val WidgetMutedText = Color(0xFFC9D7DD)
internal const val WidgetLoadTimeoutMillis = 8_000L
internal const val HomeWidgetLogTag = "HomeWidget"
private const val MaxHomeWidgetRows = 12

private fun homeWidgetRowLabelKey(index: Int) = stringPreferencesKey("row_${index}_label")

private fun homeWidgetRowValueKey(index: Int) = stringPreferencesKey("row_${index}_value")

private fun homeWidgetRowSubtitleKey(index: Int) = stringPreferencesKey("row_${index}_subtitle")

private fun HomeMetricWidgetRow.displayText(): String =
    buildString {
        append(label)
        append(": ")
        append(value)
        if (subtitle.isNotBlank()) {
            append(" - ")
            append(subtitle)
        }
    }
