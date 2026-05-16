package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DashboardData
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.features.activity.exerciseTypeLabel
import tech.mmarca.openvitals.ui.components.DayNavigator
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.SectionHeader
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
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onGrantPermissions: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenHeart: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenHydration: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenMindfulness: () -> Unit,
    onOpenCycle: () -> Unit,
    onOpenBrowse: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val dashboardData = state.data
    var showDatePicker by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPreferences()
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading && dashboardData != null,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && dashboardData == null -> FullScreenLoading()
            state.errorMessage != null && dashboardData == null ->
                ErrorMessage(state.errorMessage ?: stringResource(R.string.unknown_error))
            dashboardData != null -> DashboardContent(
                data = dashboardData,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                canGoForward = state.selectedDate.isBefore(LocalDate.now()),
                showPermissionsCallout = state.showPermissionsCallout,
                trackCycle = state.trackCycle,
                onPreviousDay = viewModel::previousDay,
                onNextDay = viewModel::nextDay,
                onOpenCalendar = { showDatePicker = true },
                onGrantPermissions = {
                    viewModel.acknowledgePermissionsCallout()
                    onGrantPermissions()
                },
                onDismissPermissionsCallout = viewModel::acknowledgePermissionsCallout,
                onOpenSteps = onOpenSteps,
                onOpenActivities = onOpenActivities,
                onOpenSleep = onOpenSleep,
                onOpenHeart = onOpenHeart,
                onOpenBody = onOpenBody,
                onOpenHydration = onOpenHydration,
                onOpenNutrition = onOpenNutrition,
                onOpenMindfulness = onOpenMindfulness,
                onOpenCycle = onOpenCycle,
                onOpenBrowse = onOpenBrowse,
            )
            else -> ErrorMessage(stringResource(R.string.message_no_dashboard_data))
        }
    }

    if (showDatePicker) {
        HealthDatePickerDialog(
            selectedDate = state.selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                viewModel.selectDate(date)
            },
        )
    }
}

@Composable
private fun DashboardContent(
    data: DashboardData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    canGoForward: Boolean,
    showPermissionsCallout: Boolean,
    trackCycle: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onGrantPermissions: () -> Unit,
    onDismissPermissionsCallout: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenHeart: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenHydration: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenMindfulness: () -> Unit,
    onOpenCycle: () -> Unit,
    onOpenBrowse: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val distance = unitFormatter.distance(data.distanceMeters)

    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            DayNavigator(
                date = data.date,
                canGoForward = canGoForward,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onOpenCalendar = onOpenCalendar,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (showPermissionsCallout) {
            item {
                PermissionCallout(
                    title = stringResource(R.string.message_missing_permissions_title),
                    body = stringResource(R.string.message_missing_permissions_body),
                    onGrant = onGrantPermissions,
                    onDismiss = onDismissPermissionsCallout,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        item { SectionHeader(stringResource(R.string.section_activity_recovery)) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = stringResource(R.string.metric_steps),
                    value = unitFormatter.count(data.steps),
                    unit = stringResource(R.string.unit_steps),
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    accentColor = StepsColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSteps,
                )
                MetricCard(
                    title = stringResource(R.string.metric_distance),
                    value = distance.value,
                    unit = distance.unit,
                    icon = Icons.Outlined.Straighten,
                    accentColor = DistanceColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSteps,
                )
            }
        }

        if (data.floorsClimbed != null || data.elevationGainedMeters != null) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (data.floorsClimbed != null) {
                        MetricCard(
                            title = stringResource(R.string.metric_floors_climbed),
                            value = data.floorsClimbed.toString(),
                            unit = stringResource(R.string.unit_floors),
                            icon = Icons.Outlined.Stairs,
                            accentColor = FloorsColor,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenSteps,
                        )
                    }
                    if (data.elevationGainedMeters != null) {
                        val elevation = unitFormatter.elevation(data.elevationGainedMeters)
                        MetricCard(
                            title = stringResource(R.string.metric_elevation),
                            value = elevation.value,
                            unit = elevation.unit,
                            icon = Icons.Outlined.Terrain,
                            accentColor = ElevationColor,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenSteps,
                        )
                    }
                    if (data.floorsClimbed != null && data.elevationGainedMeters == null) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            val workout = data.workout
            if (workout != null) {
                WorkoutCard(
                    workout = workout,
                    zone = zone,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = onOpenActivities,
                )
            } else {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_workout),
                    icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                    accentColor = WorkoutColor,
                    message = stringResource(R.string.message_no_workouts_day),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = onOpenActivities,
                )
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            val sleep = data.sleep
            if (sleep != null) {
                SleepCard(
                    sleep = sleep,
                    date = data.date,
                    zone = zone,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = onOpenSleep,
                )
            } else {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_sleep),
                    icon = Icons.Outlined.Bed,
                    accentColor = SleepColor,
                    message = stringResource(R.string.message_no_sleep_day),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = onOpenSleep,
                )
            }
        }

        item { SectionHeader(stringResource(R.string.section_body_intake)) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = stringResource(R.string.metric_calories_out),
                    value = unitFormatter.energy(data.caloriesKcal).value,
                    unit = unitFormatter.energy(data.caloriesKcal).unit,
                    icon = Icons.Outlined.LocalFireDepartment,
                    accentColor = CaloriesColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSteps,
                )
                MetricCard(
                    title = stringResource(R.string.metric_calories_in),
                    value = unitFormatter.energy(data.caloriesInKcal ?: 0.0).value,
                    unit = unitFormatter.energy(data.caloriesInKcal ?: 0.0).unit,
                    icon = Icons.Outlined.Restaurant,
                    accentColor = NutritionColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenNutrition,
                )
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = stringResource(R.string.metric_hydration),
                    value = unitFormatter.hydration(data.hydrationLiters).value,
                    unit = unitFormatter.hydration(data.hydrationLiters).unit,
                    icon = Icons.Outlined.LocalDrink,
                    accentColor = HydrationColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHydration,
                )
                MetricCard(
                    title = stringResource(R.string.metric_latest_weight),
                    value = unitFormatter.weight(data.weightKg).value,
                    unit = unitFormatter.weight(data.weightKg).unit,
                    icon = Icons.Outlined.MonitorWeight,
                    accentColor = WeightColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenBody,
                )
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            MetricCard(
                title = stringResource(R.string.metric_body_fat),
                value = unitFormatter.percent(data.bodyFatPercent).value,
                unit = unitFormatter.percent(data.bodyFatPercent).unit,
                icon = Icons.Outlined.MonitorWeight,
                accentColor = BodyFatColor,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = onOpenBody,
            )
        }

        item { SectionHeader(stringResource(R.string.section_heart)) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = stringResource(R.string.metric_avg_heart_rate),
                    value = unitFormatter.heartRate(data.avgHeartRateBpm).value,
                    unit = unitFormatter.heartRate(data.avgHeartRateBpm).unit,
                    icon = Icons.Outlined.Favorite,
                    accentColor = HeartColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHeart,
                )
                MetricCard(
                    title = stringResource(R.string.metric_resting_heart_rate),
                    value = unitFormatter.heartRate(data.restingHeartRateBpm).value,
                    unit = unitFormatter.heartRate(data.restingHeartRateBpm).unit,
                    icon = Icons.Outlined.FavoriteBorder,
                    accentColor = HeartColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHeart,
                )
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (data.latestSystolicMmHg != null && data.latestDiastolicMmHg != null) {
                    val bloodPressure = unitFormatter.bloodPressure(
                        data.latestSystolicMmHg,
                        data.latestDiastolicMmHg,
                    )
                    MetricCard(
                        title = stringResource(R.string.metric_blood_pressure),
                        value = bloodPressure.value,
                        unit = bloodPressure.unit,
                        icon = Icons.Outlined.Favorite,
                        accentColor = VitalsColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHeart,
                    )
                } else {
                    MetricCardPlaceholder(
                        title = stringResource(R.string.metric_blood_pressure),
                        icon = Icons.Outlined.Favorite,
                        accentColor = VitalsColor,
                        message = stringResource(R.string.message_no_blood_pressure),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHeart,
                    )
                }
                if (data.latestSpO2Percent != null) {
                    val spO2 = unitFormatter.percent(data.latestSpO2Percent)
                    MetricCard(
                        title = stringResource(R.string.metric_spo2),
                        value = spO2.value,
                        unit = spO2.unit,
                        icon = Icons.Outlined.FavoriteBorder,
                        accentColor = VitalsColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHeart,
                    )
                } else {
                    MetricCardPlaceholder(
                        title = stringResource(R.string.metric_spo2),
                        icon = Icons.Outlined.FavoriteBorder,
                        accentColor = VitalsColor,
                        message = stringResource(R.string.message_no_oxygen),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHeart,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            if (data.latestVo2Max != null) {
                val vo2Max = unitFormatter.vo2Max(data.latestVo2Max)
                MetricCard(
                    title = stringResource(R.string.metric_vo2_max),
                    value = vo2Max.value,
                    unit = vo2Max.unit,
                    icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                    accentColor = VitalsColor,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = onOpenHeart,
                )
            } else {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_vo2_max),
                    icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                    accentColor = VitalsColor,
                    message = stringResource(R.string.message_no_vo2_max),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = onOpenHeart,
                )
            }
        }

        item { SectionHeader(stringResource(R.string.section_mind)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = stringResource(R.string.metric_mindfulness),
                    value = unitFormatter.minutes((data.mindfulnessMinutes ?: 0).toLong()).value,
                    unit = unitFormatter.minutes((data.mindfulnessMinutes ?: 0).toLong()).unit,
                    icon = Icons.Outlined.SelfImprovement,
                    accentColor = MindfulnessColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMindfulness,
                )
                Spacer(Modifier.weight(1f))
            }
        }

        if (trackCycle) {
            item { SectionHeader(stringResource(R.string.metric_cycle)) }
            item {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_cycle),
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = CycleColor,
                    message = stringResource(R.string.message_cycle_browse),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = onOpenCycle,
                )
            }
        }

        item { SectionHeader(stringResource(R.string.section_records)) }
        item {
            MetricCardPlaceholder(
                title = stringResource(R.string.metric_browse),
                icon = Icons.Outlined.FolderOpen,
                accentColor = MaterialTheme.colorScheme.primary,
                message = stringResource(R.string.message_browse_records),
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = onOpenBrowse,
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun WorkoutCard(
    workout: ExerciseData,
    zone: ZoneId,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val start = workout.startTime.atZone(zone)
    MetricCard(
        title = stringResource(R.string.metric_workout),
        value = unitFormatter.duration(workout.durationMs),
        unit = exerciseTypeLabel(workout.exerciseType),
        icon = Icons.AutoMirrored.Outlined.DirectionsRun,
        accentColor = WorkoutColor,
        subtitle = "${dateTimeFormatterProvider.mediumDate().format(start)} · ${dateTimeFormatterProvider.shortTime().format(start)}",
        source = workout.source,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
private fun SleepCard(
    sleep: SleepData,
    date: LocalDate,
    zone: ZoneId,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val start = sleep.startTime.atZone(zone)
    val end = sleep.endTime.atZone(zone)
    MetricCard(
        title = stringResource(R.string.metric_sleep),
        value = unitFormatter.duration(sleep.durationMs),
        unit = "",
        icon = Icons.Outlined.Bed,
        accentColor = SleepColor,
        subtitle = "${dateTimeFormatterProvider.mediumDate().format(date)} · " +
            "${dateTimeFormatterProvider.shortTime().format(start)} - ${dateTimeFormatterProvider.shortTime().format(end)}",
        source = sleep.source,
        modifier = modifier,
        onClick = onClick,
    )
}
