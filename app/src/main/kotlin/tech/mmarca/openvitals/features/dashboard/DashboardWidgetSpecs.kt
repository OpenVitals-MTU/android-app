package tech.mmarca.openvitals.features.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Accessible
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.features.activity.exerciseTypeIcon
import tech.mmarca.openvitals.features.activity.exerciseTypeLabel
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.DayNavigator
import tech.mmarca.openvitals.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.HydrationColor
import tech.mmarca.openvitals.ui.theme.MindfulnessColor
import tech.mmarca.openvitals.ui.theme.NutritionColor
import tech.mmarca.openvitals.ui.theme.SleepColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import tech.mmarca.openvitals.ui.theme.WheelchairPushesColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor

@Composable
internal fun dashboardWidgetSpecs(
    display: DashboardDisplayState,
    unitFormatter: UnitFormatter,
    widgetIds: Collection<DashboardWidgetId>,
    isEditingDashboard: Boolean,
    onOpenMetric: (DashboardWidgetId) -> Unit,
): List<DashboardWidgetSpec> = buildList {
    val loadingMessage = stringResource(R.string.loading)
    val openMetric: (DashboardWidgetId) -> (() -> Unit)? = { widgetId ->
        if (isEditingDashboard) null else ({ onOpenMetric(widgetId) })
    }
    widgetIds.forEach { widgetId ->
        val model = display.widgets[widgetId] ?: return@forEach
        val meta = dashboardWidgetMeta(widgetId)
        val title = dashboardWidgetTitle(widgetId)
        when {
            model.weeklyCardioLoad != null || model.id == DashboardWidgetId.WEEKLY_CARDIO_LOAD ||
                model.id == DashboardWidgetId.CARDIO_LOAD -> {
                addWeeklyCardioLoadMetric(
                    id = widgetId,
                    title = title,
                    weeklyCardioLoad = model.weeklyCardioLoad,
                    icon = meta.icon,
                    accentColor = meta.accentColor,
                    style = model.style,
                    loadingMessage = loadingMessage.takeIf { model.isLoading },
                    onClick = openMetric(widgetId),
                )
            }
            model.id == DashboardWidgetId.CYCLE -> {
                add(
                    DashboardWidgetSpec(widgetId, title) { modifier ->
                        val cycleValue = model.cycle?.toDisplayValue(unitFormatter)
                        DashboardPillWidget(
                            title = title,
                            value = cycleValue ?: DisplayValue("", ""),
                            icon = meta.icon,
                            accentColor = meta.accentColor,
                            message = loadingMessage.takeIf { model.isLoading }
                                ?: if (cycleValue == null) stringResource(R.string.message_cycle_browse) else null,
                            modifier = modifier,
                            onClick = openMetric(widgetId),
                        )
                    },
                )
            }
            widgetId in dashboardRequiredMetricWidgets && model.value != null -> {
                addMetric(
                    id = widgetId,
                    title = title,
                    value = model.value,
                    icon = meta.icon,
                    accentColor = meta.accentColor,
                    progress = model.progress?.toWidgetProgress(),
                    style = model.style,
                    loadingMessage = loadingMessage.takeIf { model.isLoading },
                    onClick = openMetric(widgetId),
                )
            }
            else -> {
                addOptionalMetric(
                    id = widgetId,
                    title = title,
                    value = model.value,
                    icon = meta.icon,
                    accentColor = meta.accentColor,
                    noDataMessage = noDataMessageFor(model),
                    subtitle = caloriesSubtitleFor(model)
                        ?: model.sleepScore?.toSubtitle(unitFormatter)
                        ?: model.measurementSubtitle,
                    subtitleColor = MaterialTheme.colorScheme.onSurface,
                    showTitle = model.showTitle,
                    progress = model.progress?.toWidgetProgress(),
                    loadingMessage = loadingMessage.takeIf { model.isLoading },
                    onClick = openMetric(widgetId),
                )
            }
        }
    }
}

private val dashboardRequiredMetricWidgets = setOf(
    DashboardWidgetId.STEPS,
    DashboardWidgetId.DISTANCE,
    DashboardWidgetId.HYDRATION,
    DashboardWidgetId.BODY_FAT,
    DashboardWidgetId.AVG_HEART_RATE,
    DashboardWidgetId.RESTING_HEART_RATE,
    DashboardWidgetId.MINDFULNESS,
)

@Composable
private fun caloriesSubtitleFor(model: DashboardWidgetDisplayModel): String? =
    if (model.caloriesSubtitle == CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR) {
        stringResource(R.string.calories_estimated_active_bmr)
    } else {
        null
    }

@Composable
private fun DashboardWidgetProgressModel.toWidgetProgress(): DashboardWidgetProgress =
    DashboardWidgetProgress(
        fraction = fraction,
        label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(goalLabelValue)),
    )

@Composable
private fun SleepScoreDisplay.toSubtitle(unitFormatter: UnitFormatter): String =
    stringResource(
        R.string.dashboard_sleep_score_subtitle,
        unitFormatter.count(score),
        sleepScoreRatingLabel(rating),
    )

@Composable
private fun sleepScoreRatingLabel(rating: SleepScoreRating): String =
    stringResource(
        when (rating) {
            SleepScoreRating.EXCELLENT -> R.string.sleep_score_rating_excellent
            SleepScoreRating.GOOD -> R.string.sleep_score_rating_good
            SleepScoreRating.FAIR -> R.string.sleep_score_rating_fair
            SleepScoreRating.POOR -> R.string.sleep_score_rating_poor
        },
    )

@Composable
private fun noDataMessageFor(model: DashboardWidgetDisplayModel): String? =
    when (model.id) {
        DashboardWidgetId.SLEEP -> stringResource(R.string.message_no_sleep_day)
        DashboardWidgetId.BLOOD_PRESSURE -> stringResource(R.string.message_no_blood_pressure)
        DashboardWidgetId.SPO2 -> stringResource(R.string.message_no_oxygen)
        DashboardWidgetId.VO2_MAX -> stringResource(R.string.message_no_vo2_max)
        DashboardWidgetId.BLOOD_GLUCOSE -> stringResource(R.string.message_no_blood_glucose)
        DashboardWidgetId.SKIN_TEMPERATURE -> stringResource(R.string.message_no_skin_temperature)
        else -> if (model.requiresNoDataMessage) stringResource(R.string.no_data) else null
    }

@Composable
private fun CycleWidgetDisplay.toDisplayValue(unitFormatter: UnitFormatter): DisplayValue =
    when (this) {
        is CycleWidgetDisplay.MenstruationDays ->
            DisplayValue(unitFormatter.count(days), stringResource(R.string.unit_days))
        is CycleWidgetDisplay.OvulationTests ->
            DisplayValue(unitFormatter.count(count), stringResource(R.string.unit_tests))
        is CycleWidgetDisplay.BasalTemperature ->
            unitFormatter.temperature(celsius)
    }

@Composable
private fun dashboardWidgetTitle(widgetId: DashboardWidgetId): String =
    stringResource(
        when (widgetId) {
            DashboardWidgetId.STEPS -> R.string.metric_steps
            DashboardWidgetId.DISTANCE -> R.string.metric_distance
            DashboardWidgetId.CALORIES_OUT -> R.string.metric_calories_out
            DashboardWidgetId.ACTIVE_CALORIES -> R.string.metric_active_calories
            DashboardWidgetId.FLOORS -> R.string.metric_floors_climbed
            DashboardWidgetId.ELEVATION -> R.string.metric_elevation
            DashboardWidgetId.WHEELCHAIR_PUSHES -> R.string.metric_wheelchair_pushes
            DashboardWidgetId.SLEEP -> R.string.metric_sleep
            DashboardWidgetId.BODY_ENERGY -> R.string.metric_body_energy
            DashboardWidgetId.HYDRATION -> R.string.metric_hydration
            DashboardWidgetId.CALORIES_IN -> R.string.metric_calories_in
            DashboardWidgetId.PROTEIN -> R.string.metric_protein
            DashboardWidgetId.CARBS -> R.string.metric_carbs
            DashboardWidgetId.FAT -> R.string.metric_fat
            DashboardWidgetId.WEIGHT -> R.string.metric_latest_weight
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
            DashboardWidgetId.CARDIO_LOAD,
            -> R.string.metric_weekly_cardio_load
            DashboardWidgetId.MINDFULNESS -> R.string.metric_mindfulness
            DashboardWidgetId.CYCLE -> R.string.metric_cycle
            DashboardWidgetId.WORKOUT -> R.string.metric_workout
        },
    )

private data class DashboardWidgetMeta(
    val icon: ImageVector,
    val accentColor: Color,
)

private fun dashboardWidgetMeta(widgetId: DashboardWidgetId): DashboardWidgetMeta =
    when (widgetId) {
        DashboardWidgetId.STEPS -> DashboardWidgetMeta(Icons.AutoMirrored.Outlined.DirectionsWalk, StepsColor)
        DashboardWidgetId.WEEKLY_CARDIO_LOAD,
        DashboardWidgetId.CARDIO_LOAD,
        -> DashboardWidgetMeta(Icons.Outlined.Favorite, WorkoutColor)
        DashboardWidgetId.DISTANCE -> DashboardWidgetMeta(Icons.Outlined.Straighten, DistanceColor)
        DashboardWidgetId.CALORIES_OUT,
        DashboardWidgetId.BMR,
        -> DashboardWidgetMeta(Icons.Outlined.LocalFireDepartment, CaloriesColor)
        DashboardWidgetId.ACTIVE_CALORIES -> DashboardWidgetMeta(Icons.Outlined.LocalFireDepartment, ActiveCaloriesColor)
        DashboardWidgetId.FLOORS -> DashboardWidgetMeta(Icons.Outlined.Stairs, FloorsColor)
        DashboardWidgetId.ELEVATION -> DashboardWidgetMeta(Icons.Outlined.Terrain, ElevationColor)
        DashboardWidgetId.WHEELCHAIR_PUSHES -> DashboardWidgetMeta(Icons.AutoMirrored.Outlined.Accessible, WheelchairPushesColor)
        DashboardWidgetId.SLEEP -> DashboardWidgetMeta(Icons.Outlined.Bed, SleepColor)
        DashboardWidgetId.BODY_ENERGY -> DashboardWidgetMeta(Icons.Outlined.FavoriteBorder, VitalsColor)
        DashboardWidgetId.HYDRATION -> DashboardWidgetMeta(Icons.Outlined.LocalDrink, HydrationColor)
        DashboardWidgetId.CALORIES_IN,
        DashboardWidgetId.PROTEIN,
        DashboardWidgetId.CARBS,
        DashboardWidgetId.FAT,
        -> DashboardWidgetMeta(Icons.Outlined.Restaurant, NutritionColor)
        DashboardWidgetId.WEIGHT,
        DashboardWidgetId.HEIGHT,
        DashboardWidgetId.LEAN_MASS,
        DashboardWidgetId.BONE_MASS,
        DashboardWidgetId.BODY_WATER_MASS,
        -> DashboardWidgetMeta(Icons.Outlined.MonitorWeight, WeightColor)
        DashboardWidgetId.BMI -> DashboardWidgetMeta(Icons.Outlined.MonitorWeight, WeightColor)
        DashboardWidgetId.FFMI -> DashboardWidgetMeta(Icons.Outlined.FitnessCenter, BodyFatColor)
        DashboardWidgetId.BODY_FAT -> DashboardWidgetMeta(Icons.Outlined.MonitorWeight, BodyFatColor)
        DashboardWidgetId.AVG_HEART_RATE,
        DashboardWidgetId.RESTING_HEART_RATE,
        DashboardWidgetId.BLOOD_PRESSURE,
        DashboardWidgetId.RESPIRATORY_RATE,
        DashboardWidgetId.BODY_TEMPERATURE,
        -> DashboardWidgetMeta(Icons.Outlined.Favorite, VitalsColor)
        DashboardWidgetId.HRV,
        DashboardWidgetId.SPO2,
        -> DashboardWidgetMeta(Icons.Outlined.FavoriteBorder, VitalsColor)
        DashboardWidgetId.VO2_MAX -> DashboardWidgetMeta(Icons.AutoMirrored.Outlined.DirectionsRun, VitalsColor)
        DashboardWidgetId.BLOOD_GLUCOSE -> DashboardWidgetMeta(Icons.Outlined.Favorite, VitalsColor)
        DashboardWidgetId.SKIN_TEMPERATURE -> DashboardWidgetMeta(Icons.Outlined.DeviceThermostat, VitalsColor)
        DashboardWidgetId.MINDFULNESS -> DashboardWidgetMeta(Icons.Outlined.SelfImprovement, MindfulnessColor)
        DashboardWidgetId.CYCLE -> DashboardWidgetMeta(Icons.Outlined.CalendarMonth, CycleColor)
        DashboardWidgetId.WORKOUT -> DashboardWidgetMeta(Icons.AutoMirrored.Outlined.DirectionsRun, WorkoutColor)
    }

internal fun MutableList<DashboardWidgetSpec>.addMetric(
    id: DashboardWidgetId,
    title: String,
    value: DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    progress: DashboardWidgetProgress? = null,
    style: DashboardWidgetStyle = DashboardWidgetStyle.PILL,
    loadingMessage: String? = null,
    onClick: (() -> Unit)?,
) {
    add(
        DashboardWidgetSpec(id = id, title = title, style = style) { modifier ->
            if (loadingMessage != null) {
                DashboardPillWidget(
                    title = title,
                    value = DisplayValue("", ""),
                    icon = icon,
                    accentColor = accentColor,
                    message = loadingMessage,
                    modifier = modifier,
                    onClick = onClick,
                )
            } else if (style == DashboardWidgetStyle.CIRCLE && progress != null) {
                DashboardCircleWidget(
                    title = title,
                    value = value,
                    icon = icon,
                    accentColor = accentColor,
                    progress = progress,
                    modifier = modifier,
                    onClick = onClick,
                )
            } else {
                DashboardPillWidget(
                    title = title,
                    value = value,
                    icon = icon,
                    accentColor = accentColor,
                    progress = progress,
                    modifier = modifier,
                    onClick = onClick,
                )
            }
        }
    )
}

internal fun MutableList<DashboardWidgetSpec>.addOptionalMetric(
    id: DashboardWidgetId,
    title: String,
    value: DisplayValue?,
    icon: ImageVector,
    accentColor: Color,
    noDataMessage: String? = null,
    subtitle: String? = null,
    subtitleColor: Color = accentColor,
    showTitle: Boolean = true,
    progress: DashboardWidgetProgress? = null,
    loadingMessage: String? = null,
    onClick: (() -> Unit)?,
) {
    add(
        DashboardWidgetSpec(id, title) { modifier ->
            if (loadingMessage != null) {
                DashboardPillWidget(
                    title = title,
                    value = DisplayValue("", ""),
                    icon = icon,
                    accentColor = accentColor,
                    message = loadingMessage,
                    showTitle = showTitle,
                    modifier = modifier,
                    onClick = onClick,
                )
            } else if (value != null) {
                DashboardPillWidget(
                    title = title,
                    value = value,
                    icon = icon,
                    accentColor = accentColor,
                    progress = progress,
                    subtitle = subtitle,
                    subtitleColor = subtitleColor,
                    showTitle = showTitle,
                    modifier = modifier,
                    onClick = onClick,
                )
            } else {
                DashboardPillWidget(
                    title = title,
                    value = DisplayValue("", ""),
                    icon = icon,
                    accentColor = accentColor,
                    message = noDataMessage ?: stringResource(R.string.no_data),
                    showTitle = showTitle,
                    modifier = modifier,
                    onClick = onClick,
                )
            }
        }
    )
}

enum class DashboardWidgetStyle {
    PILL,
    CIRCLE,
}

internal data class DashboardWidgetSpec(
    val id: DashboardWidgetId,
    val title: String,
    val style: DashboardWidgetStyle = DashboardWidgetStyle.PILL,
    val content: @Composable (Modifier) -> Unit,
)

internal data class DashboardWidgetProgress(
    val fraction: Float,
    val label: String,
)

internal fun MutableList<DashboardWidgetSpec>.addWeeklyCardioLoadMetric(
    id: DashboardWidgetId,
    title: String,
    weeklyCardioLoad: DashboardWeeklyCardioLoad?,
    icon: ImageVector,
    accentColor: Color,
    style: DashboardWidgetStyle,
    loadingMessage: String? = null,
    onClick: (() -> Unit)?,
) {
    add(
        DashboardWidgetSpec(id = id, title = title, style = style) { modifier ->
            if (loadingMessage != null) {
                DashboardPillWidget(
                    title = title,
                    value = DisplayValue("", ""),
                    icon = icon,
                    accentColor = accentColor,
                    message = loadingMessage,
                    modifier = modifier,
                    onClick = onClick,
                )
            } else if (weeklyCardioLoad == null) {
                DashboardPillWidget(
                    title = title,
                    value = DisplayValue("", ""),
                    icon = icon,
                    accentColor = accentColor,
                    message = stringResource(R.string.no_data),
                    modifier = modifier,
                    onClick = onClick,
                )
            } else {
                val progress = DashboardWidgetProgress(
                    fraction = weeklyCardioLoad.progressFraction,
                    label = stringResource(
                        R.string.dashboard_weekly_cardio_load_progress,
                        weeklyCardioLoad.currentScore,
                        weeklyCardioLoad.targetScore,
                    ),
                )
                if (style == DashboardWidgetStyle.CIRCLE) {
                    DashboardCircleWidget(
                        title = title,
                        value = DisplayValue(
                            value = stringResource(
                                R.string.dashboard_cardio_load_percent_only,
                                weeklyCardioLoad.progressPercent,
                            ),
                            unit = "",
                        ),
                        icon = icon,
                        accentColor = accentColor,
                        progress = progress,
                        modifier = modifier,
                        onClick = onClick,
                    )
                } else {
                    DashboardPillWidget(
                        title = title,
                        value = DisplayValue(
                            value = stringResource(
                                R.string.dashboard_cardio_load_percent,
                                weeklyCardioLoad.progressPercent,
                            ),
                            unit = "",
                        ),
                        icon = icon,
                        accentColor = accentColor,
                        progress = progress,
                        subtitle = weeklyCardioLoad.todayProgressPercent
                            .takeIf { it > 0 }
                            ?.let { todayPercent ->
                                stringResource(R.string.dashboard_cardio_load_today_delta, todayPercent)
                            },
                        modifier = modifier,
                        onClick = onClick,
                    )
                }
            }
        }
    )
}
