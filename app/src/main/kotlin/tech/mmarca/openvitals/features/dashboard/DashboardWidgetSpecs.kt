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
import androidx.compose.material3.CardDefaults
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
    data: DashboardData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    dailyGoals: DashboardDailyGoals,
    widgetIds: Collection<DashboardWidgetId>,
    pendingWidgets: Set<DashboardWidgetId>,
    isEditingDashboard: Boolean,
    onOpenMetric: (DashboardWidgetId) -> Unit,
): List<DashboardWidgetSpec> = buildList {
    val widgetIdsToBuild = widgetIds.toSet()
    fun shouldBuild(widgetId: DashboardWidgetId): Boolean =
        widgetId in widgetIdsToBuild
    val loadingMessage = stringResource(R.string.loading)
    fun loadingMessageFor(widgetId: DashboardWidgetId): String? =
        loadingMessage.takeIf { widgetId in pendingWidgets }
    val openMetric: (DashboardWidgetId) -> (() -> Unit)? = { widgetId ->
        if (isEditingDashboard) null else ({ onOpenMetric(widgetId) })
    }
    val sleepGoalMs = (dailyGoals.sleepHours * 60.0 * 60.0 * 1000.0).toLong()

    if (shouldBuild(DashboardWidgetId.STEPS)) {
        addMetric(
            id = DashboardWidgetId.STEPS,
            title = stringResource(R.string.metric_steps),
            value = DisplayValue(unitFormatter.count(data.steps), stringResource(R.string.unit_steps)),
            icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
            accentColor = StepsColor,
            progress = dashboardGoalProgress(
                current = data.steps.toDouble(),
                target = dailyGoals.steps,
                label = stringResource(R.string.dashboard_goal_of, unitFormatter.count(dailyGoals.steps.roundToInt())),
            ),
            style = DashboardWidgetStyle.CIRCLE,
            loadingMessage = loadingMessageFor(DashboardWidgetId.STEPS),
            onClick = openMetric(DashboardWidgetId.STEPS),
        )
    }
    if (shouldBuild(DashboardWidgetId.WEEKLY_CARDIO_LOAD)) {
        addWeeklyCardioLoadMetric(
            id = DashboardWidgetId.WEEKLY_CARDIO_LOAD,
            title = stringResource(R.string.metric_weekly_cardio_load),
            weeklyCardioLoad = data.weeklyCardioLoad,
            icon = Icons.Outlined.Favorite,
            accentColor = WorkoutColor,
            style = DashboardWidgetStyle.CIRCLE,
            loadingMessage = loadingMessageFor(DashboardWidgetId.WEEKLY_CARDIO_LOAD),
            onClick = openMetric(DashboardWidgetId.WEEKLY_CARDIO_LOAD),
        )
    }
    if (shouldBuild(DashboardWidgetId.CARDIO_LOAD)) {
        addWeeklyCardioLoadMetric(
            id = DashboardWidgetId.CARDIO_LOAD,
            title = stringResource(R.string.metric_weekly_cardio_load),
            weeklyCardioLoad = data.weeklyCardioLoad,
            icon = Icons.Outlined.Favorite,
            accentColor = WorkoutColor,
            style = DashboardWidgetStyle.PILL,
            loadingMessage = loadingMessageFor(DashboardWidgetId.CARDIO_LOAD),
            onClick = openMetric(DashboardWidgetId.CARDIO_LOAD),
        )
    }
    if (shouldBuild(DashboardWidgetId.DISTANCE)) {
        addMetric(
            id = DashboardWidgetId.DISTANCE,
            title = stringResource(R.string.metric_distance),
            value = unitFormatter.distance(data.distanceMeters),
            icon = Icons.Outlined.Straighten,
            accentColor = DistanceColor,
            progress = dashboardGoalProgress(
                current = data.distanceMeters,
                target = dailyGoals.distanceMeters,
                label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.distance(dailyGoals.distanceMeters))),
            ),
            loadingMessage = loadingMessageFor(DashboardWidgetId.DISTANCE),
            onClick = openMetric(DashboardWidgetId.DISTANCE),
        )
    }
    if (shouldBuild(DashboardWidgetId.CALORIES_OUT)) {
        val caloriesKcal = if (data.caloriesKcalSource == CaloriesBurnedSource.NO_DATA) 0.0 else data.caloriesKcal
        val caloriesValue = unitFormatter.energy(caloriesKcal)
        addOptionalMetric(
            id = DashboardWidgetId.CALORIES_OUT,
            title = stringResource(R.string.metric_calories_out),
            value = caloriesValue,
            icon = Icons.Outlined.LocalFireDepartment,
            accentColor = CaloriesColor,
            subtitle = if (data.caloriesKcalSource == CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR) {
                stringResource(R.string.calories_estimated_active_bmr)
            } else {
                null
            },
            subtitleColor = MaterialTheme.colorScheme.onSurface,
            progress = dashboardGoalProgress(
                current = caloriesKcal,
                target = dailyGoals.caloriesOutKcal,
                label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.energy(dailyGoals.caloriesOutKcal))),
            ),
            loadingMessage = loadingMessageFor(DashboardWidgetId.CALORIES_OUT),
            onClick = openMetric(DashboardWidgetId.CALORIES_OUT),
        )
    }
    if (shouldBuild(DashboardWidgetId.ACTIVE_CALORIES)) {
        addOptionalMetric(
            id = DashboardWidgetId.ACTIVE_CALORIES,
            title = stringResource(R.string.metric_active_calories),
            value = data.activeCaloriesKcal?.let(unitFormatter::energy),
            icon = Icons.Outlined.LocalFireDepartment,
            accentColor = ActiveCaloriesColor,
            progress = data.activeCaloriesKcal?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.activeCaloriesKcal,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.energy(dailyGoals.activeCaloriesKcal))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.ACTIVE_CALORIES),
            onClick = openMetric(DashboardWidgetId.ACTIVE_CALORIES),
        )
    }
    if (shouldBuild(DashboardWidgetId.FLOORS)) {
        addOptionalMetric(
            id = DashboardWidgetId.FLOORS,
            title = stringResource(R.string.metric_floors_climbed),
            value = data.floorsClimbed?.let {
                DisplayValue(unitFormatter.count(it), stringResource(R.string.unit_floors))
            },
            icon = Icons.Outlined.Stairs,
            accentColor = FloorsColor,
            progress = data.floorsClimbed?.let {
                dashboardGoalProgress(
                    current = it.toDouble(),
                    target = dailyGoals.floors,
                    label = stringResource(R.string.dashboard_goal_of, unitFormatter.count(dailyGoals.floors.roundToInt())),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.FLOORS),
            onClick = openMetric(DashboardWidgetId.FLOORS),
        )
    }
    if (shouldBuild(DashboardWidgetId.ELEVATION)) {
        addOptionalMetric(
            id = DashboardWidgetId.ELEVATION,
            title = stringResource(R.string.metric_elevation),
            value = data.elevationGainedMeters?.let(unitFormatter::elevation),
            icon = Icons.Outlined.Terrain,
            accentColor = ElevationColor,
            progress = data.elevationGainedMeters?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.elevationMeters,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.elevation(dailyGoals.elevationMeters))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.ELEVATION),
            onClick = openMetric(DashboardWidgetId.ELEVATION),
        )
    }
    if (shouldBuild(DashboardWidgetId.WHEELCHAIR_PUSHES)) {
        addOptionalMetric(
            id = DashboardWidgetId.WHEELCHAIR_PUSHES,
            title = stringResource(R.string.metric_wheelchair_pushes),
            value = data.wheelchairPushes?.let {
                DisplayValue(unitFormatter.count(it), stringResource(R.string.unit_pushes))
            },
            icon = Icons.AutoMirrored.Outlined.Accessible,
            accentColor = WheelchairPushesColor,
            progress = data.wheelchairPushes?.let {
                dashboardGoalProgress(
                    current = it.toDouble(),
                    target = dailyGoals.wheelchairPushes,
                    label = stringResource(
                        R.string.dashboard_goal_of,
                        unitFormatter.count(dailyGoals.wheelchairPushes.roundToInt()),
                    ),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.WHEELCHAIR_PUSHES),
            onClick = openMetric(DashboardWidgetId.WHEELCHAIR_PUSHES),
        )
    }
    if (shouldBuild(DashboardWidgetId.SLEEP)) {
        val sleepScoreSubtitle = data.sleepScore
            .takeIf { it.confidence != SleepScoreConfidence.NO_DATA }
            ?.let { score ->
                stringResource(
                    R.string.dashboard_sleep_score_subtitle,
                    unitFormatter.count(score.score),
                    sleepScoreRatingLabel(score.score),
                )
            }
        addOptionalMetric(
            id = DashboardWidgetId.SLEEP,
            title = stringResource(R.string.metric_sleep),
            value = data.sleep?.let { DisplayValue(unitFormatter.duration(it.durationMs), "") },
            icon = Icons.Outlined.Bed,
            accentColor = SleepColor,
            noDataMessage = stringResource(R.string.message_no_sleep_day),
            subtitle = sleepScoreSubtitle,
            subtitleColor = MaterialTheme.colorScheme.onSurface,
            showTitle = false,
            progress = data.sleep?.let {
                dashboardGoalProgress(
                    current = it.durationMs.toDouble(),
                    target = sleepGoalMs.toDouble(),
                    label = stringResource(R.string.dashboard_goal_of, unitFormatter.duration(sleepGoalMs)),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.SLEEP),
            onClick = openMetric(DashboardWidgetId.SLEEP),
        )
    }
    if (shouldBuild(DashboardWidgetId.HYDRATION)) {
        addMetric(
            id = DashboardWidgetId.HYDRATION,
            title = stringResource(R.string.metric_hydration),
            value = unitFormatter.hydration(data.hydrationLiters),
            icon = Icons.Outlined.LocalDrink,
            accentColor = HydrationColor,
            progress = dashboardGoalProgress(
                current = data.hydrationLiters,
                target = dailyGoals.hydrationLiters,
                label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.hydration(dailyGoals.hydrationLiters))),
            ),
            loadingMessage = loadingMessageFor(DashboardWidgetId.HYDRATION),
            onClick = openMetric(DashboardWidgetId.HYDRATION),
        )
    }
    if (shouldBuild(DashboardWidgetId.CALORIES_IN)) {
        addOptionalMetric(
            id = DashboardWidgetId.CALORIES_IN,
            title = stringResource(R.string.metric_calories_in),
            value = data.caloriesInKcal?.let(unitFormatter::energy),
            icon = Icons.Outlined.Restaurant,
            accentColor = NutritionColor,
            progress = data.caloriesInKcal?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.caloriesInKcal,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.energy(dailyGoals.caloriesInKcal))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.CALORIES_IN),
            onClick = openMetric(DashboardWidgetId.CALORIES_IN),
        )
    }
    if (shouldBuild(DashboardWidgetId.PROTEIN)) {
        addOptionalMetric(
            id = DashboardWidgetId.PROTEIN,
            title = stringResource(R.string.metric_protein),
            value = data.proteinGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
            icon = Icons.Outlined.Restaurant,
            accentColor = NutritionColor,
            progress = data.proteinGrams?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.proteinGrams,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(dashboardGramDisplayValue(dailyGoals.proteinGrams, unitFormatter))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.PROTEIN),
            onClick = openMetric(DashboardWidgetId.PROTEIN),
        )
    }
    if (shouldBuild(DashboardWidgetId.CARBS)) {
        addOptionalMetric(
            id = DashboardWidgetId.CARBS,
            title = stringResource(R.string.metric_carbs),
            value = data.carbsGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
            icon = Icons.Outlined.Restaurant,
            accentColor = NutritionColor,
            progress = data.carbsGrams?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.carbsGrams,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(dashboardGramDisplayValue(dailyGoals.carbsGrams, unitFormatter))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.CARBS),
            onClick = openMetric(DashboardWidgetId.CARBS),
        )
    }
    if (shouldBuild(DashboardWidgetId.FAT)) {
        addOptionalMetric(
            id = DashboardWidgetId.FAT,
            title = stringResource(R.string.metric_fat),
            value = data.fatGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
            icon = Icons.Outlined.Restaurant,
            accentColor = NutritionColor,
            progress = data.fatGrams?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.fatGrams,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(dashboardGramDisplayValue(dailyGoals.fatGrams, unitFormatter))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.FAT),
            onClick = openMetric(DashboardWidgetId.FAT),
        )
    }
    if (shouldBuild(DashboardWidgetId.WEIGHT)) {
        addOptionalMetric(
            id = DashboardWidgetId.WEIGHT,
            title = stringResource(R.string.metric_latest_weight),
            value = data.weightKg?.let(unitFormatter::weight),
            subtitle = data.weightTime?.let { dashboardMeasurementDate(it, dateTimeFormatterProvider) },
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.WEIGHT),
            onClick = openMetric(DashboardWidgetId.WEIGHT),
        )
    }
    if (shouldBuild(DashboardWidgetId.HEIGHT)) {
        addOptionalMetric(
            id = DashboardWidgetId.HEIGHT,
            title = stringResource(R.string.metric_height),
            value = data.heightCm?.let(unitFormatter::height),
            subtitle = data.heightTime?.let { dashboardMeasurementDate(it, dateTimeFormatterProvider) },
            icon = Icons.Outlined.Straighten,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.HEIGHT),
            onClick = openMetric(DashboardWidgetId.HEIGHT),
        )
    }
    if (shouldBuild(DashboardWidgetId.BMI)) {
        addOptionalMetric(
            id = DashboardWidgetId.BMI,
            title = stringResource(R.string.metric_bmi),
            value = data.bmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") },
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BMI),
            onClick = openMetric(DashboardWidgetId.BMI),
        )
    }
    if (shouldBuild(DashboardWidgetId.BODY_FAT)) {
        addMetric(
            id = DashboardWidgetId.BODY_FAT,
            title = stringResource(R.string.metric_body_fat),
            value = unitFormatter.percent(data.bodyFatPercent),
            icon = Icons.Outlined.MonitorWeight,
            accentColor = BodyFatColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BODY_FAT),
            onClick = openMetric(DashboardWidgetId.BODY_FAT),
        )
    }
    if (shouldBuild(DashboardWidgetId.LEAN_MASS)) {
        addOptionalMetric(
            id = DashboardWidgetId.LEAN_MASS,
            title = stringResource(R.string.metric_lean_mass),
            value = data.leanMassKg?.let(unitFormatter::bodyMass),
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.LEAN_MASS),
            onClick = openMetric(DashboardWidgetId.LEAN_MASS),
        )
    }
    if (shouldBuild(DashboardWidgetId.BMR)) {
        addOptionalMetric(
            id = DashboardWidgetId.BMR,
            title = stringResource(R.string.metric_bmr),
            value = data.bmrKcal?.let(unitFormatter::energy),
            icon = Icons.Outlined.LocalFireDepartment,
            accentColor = CaloriesColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BMR),
            onClick = openMetric(DashboardWidgetId.BMR),
        )
    }
    if (shouldBuild(DashboardWidgetId.BONE_MASS)) {
        addOptionalMetric(
            id = DashboardWidgetId.BONE_MASS,
            title = stringResource(R.string.metric_bone_mass),
            value = data.boneMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BONE_MASS),
            onClick = openMetric(DashboardWidgetId.BONE_MASS),
        )
    }
    if (shouldBuild(DashboardWidgetId.BODY_WATER_MASS)) {
        addOptionalMetric(
            id = DashboardWidgetId.BODY_WATER_MASS,
            title = stringResource(R.string.metric_body_water_mass),
            value = data.bodyWaterMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
            icon = Icons.Outlined.LocalDrink,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BODY_WATER_MASS),
            onClick = openMetric(DashboardWidgetId.BODY_WATER_MASS),
        )
    }
    if (shouldBuild(DashboardWidgetId.AVG_HEART_RATE)) {
        addMetric(
            id = DashboardWidgetId.AVG_HEART_RATE,
            title = stringResource(R.string.metric_avg_heart_rate),
            value = unitFormatter.heartRate(data.avgHeartRateBpm),
            icon = Icons.Outlined.Favorite,
            accentColor = HeartColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.AVG_HEART_RATE),
            onClick = openMetric(DashboardWidgetId.AVG_HEART_RATE),
        )
    }
    if (shouldBuild(DashboardWidgetId.RESTING_HEART_RATE)) {
        addMetric(
            id = DashboardWidgetId.RESTING_HEART_RATE,
            title = stringResource(R.string.metric_resting_heart_rate),
            value = unitFormatter.heartRate(data.restingHeartRateBpm),
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = HeartColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.RESTING_HEART_RATE),
            onClick = openMetric(DashboardWidgetId.RESTING_HEART_RATE),
        )
    }
    if (shouldBuild(DashboardWidgetId.HRV)) {
        addOptionalMetric(
            id = DashboardWidgetId.HRV,
            title = stringResource(R.string.metric_hrv),
            value = data.hrvRmssdMs?.let(unitFormatter::hrv),
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = HeartColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.HRV),
            onClick = openMetric(DashboardWidgetId.HRV),
        )
    }
    if (shouldBuild(DashboardWidgetId.BLOOD_PRESSURE)) {
        addOptionalMetric(
            id = DashboardWidgetId.BLOOD_PRESSURE,
            title = stringResource(R.string.metric_blood_pressure),
            value = if (data.latestSystolicMmHg != null && data.latestDiastolicMmHg != null) {
                unitFormatter.bloodPressure(data.latestSystolicMmHg, data.latestDiastolicMmHg)
            } else {
                null
            },
            icon = Icons.Outlined.Favorite,
            accentColor = VitalsColor,
            noDataMessage = stringResource(R.string.message_no_blood_pressure),
            loadingMessage = loadingMessageFor(DashboardWidgetId.BLOOD_PRESSURE),
            onClick = openMetric(DashboardWidgetId.BLOOD_PRESSURE),
        )
    }
    if (shouldBuild(DashboardWidgetId.SPO2)) {
        addOptionalMetric(
            id = DashboardWidgetId.SPO2,
            title = stringResource(R.string.metric_spo2),
            value = data.latestSpO2Percent?.let(unitFormatter::percent),
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = VitalsColor,
            noDataMessage = stringResource(R.string.message_no_oxygen),
            loadingMessage = loadingMessageFor(DashboardWidgetId.SPO2),
            onClick = openMetric(DashboardWidgetId.SPO2),
        )
    }
    if (shouldBuild(DashboardWidgetId.VO2_MAX)) {
        addOptionalMetric(
            id = DashboardWidgetId.VO2_MAX,
            title = stringResource(R.string.metric_vo2_max),
            value = data.latestVo2Max?.let(unitFormatter::vo2Max),
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = VitalsColor,
            noDataMessage = stringResource(R.string.message_no_vo2_max),
            loadingMessage = loadingMessageFor(DashboardWidgetId.VO2_MAX),
            onClick = openMetric(DashboardWidgetId.VO2_MAX),
        )
    }
    if (shouldBuild(DashboardWidgetId.RESPIRATORY_RATE)) {
        addOptionalMetric(
            id = DashboardWidgetId.RESPIRATORY_RATE,
            title = stringResource(R.string.metric_respiratory_rate),
            value = data.avgRespiratoryRate?.let(unitFormatter::respiratoryRate),
            icon = Icons.Outlined.Favorite,
            accentColor = VitalsColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.RESPIRATORY_RATE),
            onClick = openMetric(DashboardWidgetId.RESPIRATORY_RATE),
        )
    }
    if (shouldBuild(DashboardWidgetId.BODY_TEMPERATURE)) {
        addOptionalMetric(
            id = DashboardWidgetId.BODY_TEMPERATURE,
            title = stringResource(R.string.metric_body_temp),
            value = data.latestBodyTemperatureCelsius?.let(unitFormatter::temperature),
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = VitalsColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BODY_TEMPERATURE),
            onClick = openMetric(DashboardWidgetId.BODY_TEMPERATURE),
        )
    }
    if (shouldBuild(DashboardWidgetId.BLOOD_GLUCOSE)) {
        addOptionalMetric(
            id = DashboardWidgetId.BLOOD_GLUCOSE,
            title = stringResource(R.string.metric_blood_glucose),
            value = data.latestBloodGlucoseMillimolesPerLiter?.let(unitFormatter::bloodGlucose),
            icon = Icons.Outlined.Favorite,
            accentColor = VitalsColor,
            noDataMessage = stringResource(R.string.message_no_blood_glucose),
            loadingMessage = loadingMessageFor(DashboardWidgetId.BLOOD_GLUCOSE),
            onClick = openMetric(DashboardWidgetId.BLOOD_GLUCOSE),
        )
    }
    if (shouldBuild(DashboardWidgetId.SKIN_TEMPERATURE)) {
        addOptionalMetric(
            id = DashboardWidgetId.SKIN_TEMPERATURE,
            title = stringResource(R.string.metric_skin_temperature),
            value = data.latestSkinTemperatureDeltaCelsius?.let(unitFormatter::temperatureDelta),
            icon = Icons.Outlined.DeviceThermostat,
            accentColor = VitalsColor,
            noDataMessage = stringResource(R.string.message_no_skin_temperature),
            loadingMessage = loadingMessageFor(DashboardWidgetId.SKIN_TEMPERATURE),
            onClick = openMetric(DashboardWidgetId.SKIN_TEMPERATURE),
        )
    }
    if (shouldBuild(DashboardWidgetId.MINDFULNESS)) {
        addMetric(
            id = DashboardWidgetId.MINDFULNESS,
            title = stringResource(R.string.metric_mindfulness),
            value = unitFormatter.minutes((data.mindfulnessMinutes ?: 0).toLong()),
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            progress = dashboardGoalProgress(
                current = (data.mindfulnessMinutes ?: 0).toDouble(),
                target = dailyGoals.mindfulnessMinutes,
                label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.minutes(dailyGoals.mindfulnessMinutes.roundToInt().toLong()))),
            ),
            loadingMessage = loadingMessageFor(DashboardWidgetId.MINDFULNESS),
            onClick = openMetric(DashboardWidgetId.MINDFULNESS),
        )
    }
    if (shouldBuild(DashboardWidgetId.CYCLE)) {
        add(
            DashboardWidgetSpec(DashboardWidgetId.CYCLE, stringResource(R.string.metric_cycle)) { modifier ->
                val cycleValue = cycleDisplayValue(data, unitFormatter)
                DashboardPillWidget(
                    title = stringResource(R.string.metric_cycle),
                    value = cycleValue ?: DisplayValue("", ""),
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = CycleColor,
                    message = loadingMessageFor(DashboardWidgetId.CYCLE)
                        ?: if (cycleValue == null) stringResource(R.string.message_cycle_browse) else null,
                    modifier = modifier,
                    onClick = openMetric(DashboardWidgetId.CYCLE),
                )
            }
        )
    }
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

internal fun dashboardMeasurementDate(
    time: Instant,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String =
    dateTimeFormatterProvider.mediumDate().format(time.atZone(ZoneId.systemDefault()).toLocalDate())

@Composable
internal fun cycleDisplayValue(data: DashboardData, unitFormatter: UnitFormatter): DisplayValue? =
    when {
        data.menstruationPeriodDays != null && data.menstruationPeriodDays > 0 -> {
            DisplayValue(
                value = unitFormatter.count(data.menstruationPeriodDays),
                unit = stringResource(R.string.unit_days),
            )
        }
        data.ovulationTestCount != null && data.ovulationTestCount > 0 -> {
            DisplayValue(
                value = unitFormatter.count(data.ovulationTestCount),
                unit = stringResource(R.string.unit_tests),
            )
        }
        data.latestBasalBodyTemperatureCelsius != null -> unitFormatter.temperature(data.latestBasalBodyTemperatureCelsius)
        else -> null
    }

internal enum class DashboardWidgetStyle {
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
