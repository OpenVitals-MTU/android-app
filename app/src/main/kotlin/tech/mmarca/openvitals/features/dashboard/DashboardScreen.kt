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
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
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
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.data.model.DashboardData
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.SleepData
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
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.HydrationColor
import tech.mmarca.openvitals.ui.theme.SleepColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onGrantPermissions: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenHeart: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenHydration: () -> Unit,
    onOpenBrowse: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val dashboardData = state.data
    var showDatePicker by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = state.isLoading && dashboardData != null,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && dashboardData == null -> FullScreenLoading()
            state.errorMessage != null && dashboardData == null ->
                ErrorMessage(state.errorMessage ?: "Unknown error")
            dashboardData != null -> DashboardContent(
                data = dashboardData,
                canGoForward = state.selectedDate.isBefore(LocalDate.now()),
                showPermissionsCallout = state.showPermissionsCallout,
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
                onOpenBrowse = onOpenBrowse,
            )
            else -> ErrorMessage("No dashboard data available.")
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
    canGoForward: Boolean,
    showPermissionsCallout: Boolean,
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
    onOpenBrowse: () -> Unit,
) {
    val zone = ZoneId.systemDefault()

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
                    title = "Some permissions are missing",
                    body = "Grant the missing permissions to see a complete dashboard.",
                    onGrant = onGrantPermissions,
                    onDismiss = onDismissPermissionsCallout,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        item { SectionHeader("Daily summary") }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = "Steps",
                    value = "%,d".format(data.steps),
                    unit = "steps",
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    accentColor = StepsColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSteps,
                )
                MetricCard(
                    title = "Distance",
                    value = if (data.distanceMeters >= 1000) {
                        "%.1f".format(data.distanceMeters / 1000)
                    } else {
                        "%d".format(data.distanceMeters.roundToInt())
                    },
                    unit = if (data.distanceMeters >= 1000) "km" else "m",
                    icon = Icons.Outlined.Straighten,
                    accentColor = DistanceColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSteps,
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
                    title = "Calories burned",
                    value = "%,d".format(data.caloriesKcal.roundToInt()),
                    unit = "kcal",
                    icon = Icons.Outlined.LocalFireDepartment,
                    accentColor = CaloriesColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSteps,
                )
                MetricCard(
                    title = "Hydration",
                    value = "%.1f".format(data.hydrationLiters),
                    unit = "L",
                    icon = Icons.Outlined.LocalDrink,
                    accentColor = HydrationColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHydration,
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
                            title = "Floors climbed",
                            value = data.floorsClimbed.toString(),
                            unit = "floors",
                            icon = Icons.Outlined.Stairs,
                            accentColor = FloorsColor,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenSteps,
                        )
                    }
                    if (data.elevationGainedMeters != null) {
                        MetricCard(
                            title = "Elevation",
                            value = if (data.elevationGainedMeters >= 1000) {
                                "%.1f".format(data.elevationGainedMeters / 1000)
                            } else {
                                "%d".format(data.elevationGainedMeters.roundToInt())
                            },
                            unit = if (data.elevationGainedMeters >= 1000) "km" else "m",
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

        item { SectionHeader("Highlights") }

        item {
            val workout = data.workout
            if (workout != null) {
                WorkoutCard(
                    workout = workout,
                    zone = zone,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = onOpenActivities,
                )
            } else {
                MetricCardPlaceholder(
                    title = "Workout",
                    icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                    accentColor = WorkoutColor,
                    message = "No workouts recorded on this day.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = onOpenActivities,
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            val sleep = data.sleep
            if (sleep != null) {
                SleepCard(
                    sleep = sleep,
                    zone = zone,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = onOpenSleep,
                )
            } else {
                MetricCardPlaceholder(
                    title = "Sleep",
                    icon = Icons.Outlined.Bed,
                    accentColor = SleepColor,
                    message = "No sleep session ended on this day.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = onOpenSleep,
                )
            }
        }

        item { SectionHeader("Heart") }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = "Avg heart rate",
                    value = data.avgHeartRateBpm.toString(),
                    unit = "bpm",
                    icon = Icons.Outlined.Favorite,
                    accentColor = HeartColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHeart,
                )
                MetricCard(
                    title = "Resting heart rate",
                    value = data.restingHeartRateBpm.toString(),
                    unit = "bpm",
                    icon = Icons.Outlined.FavoriteBorder,
                    accentColor = HeartColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHeart,
                )
            }
        }

        item { SectionHeader("Body") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = "Latest weight",
                    value = "%.1f".format(data.weightKg),
                    unit = "kg",
                    icon = Icons.Outlined.MonitorWeight,
                    accentColor = WeightColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenBody,
                )
                MetricCard(
                    title = "Body fat",
                    value = "%.1f".format(data.bodyFatPercent),
                    unit = "%",
                    icon = Icons.Outlined.MonitorWeight,
                    accentColor = BodyFatColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenBody,
                )
            }
        }

        item { SectionHeader("Records") }
        item {
            MetricCardPlaceholder(
                title = "Browse",
                icon = Icons.Outlined.FolderOpen,
                accentColor = MaterialTheme.colorScheme.primary,
                message = "Browse all raw records from Health Connect.",
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
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val start = workout.startTime.atZone(zone)
    MetricCard(
        title = "Workout",
        value = "%dh %02dm".format(
            workout.durationMinutes / 60,
            workout.durationMinutes % 60,
        ),
        unit = exerciseTypeLabel(workout.exerciseType),
        icon = Icons.AutoMirrored.Outlined.DirectionsRun,
        accentColor = WorkoutColor,
        subtitle = "${dateFormatter.format(start)} · ${timeFormatter.format(start)}",
        source = workout.source,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
private fun SleepCard(
    sleep: SleepData,
    zone: ZoneId,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val end = sleep.endTime.atZone(zone)
    MetricCard(
        title = "Sleep",
        value = sleep.durationFormatted,
        unit = "",
        icon = Icons.Outlined.Bed,
        accentColor = SleepColor,
        subtitle = "${dateFormatter.format(end)} · ${timeFormatter.format(end)}",
        source = sleep.source,
        modifier = modifier,
        onClick = onClick,
    )
}

private fun exerciseTypeLabel(type: Int): String = when (type) {
    2 -> "Badminton"
    4 -> "Baseball"
    5 -> "Basketball"
    8 -> "Biking"
    9 -> "Biking (stationary)"
    10 -> "Boot camp"
    11 -> "Boxing"
    13 -> "Cricket"
    14 -> "Dancing"
    16 -> "Elliptical"
    17 -> "Exercise class"
    18 -> "Fencing"
    19 -> "Football (American)"
    20 -> "Football (Australian)"
    21 -> "Football (Soccer)"
    22 -> "Frisbee disc"
    23 -> "Golf"
    24 -> "Gymnastics"
    25 -> "Handball"
    26 -> "High intensity interval training"
    27 -> "Hiking"
    28 -> "Ice hockey"
    29 -> "Ice skating"
    31 -> "Martial arts"
    32 -> "Paddling"
    33 -> "Paragliding"
    34 -> "Pilates"
    35 -> "Racquetball"
    36 -> "Rock climbing"
    37 -> "Roller hockey"
    38 -> "Rowing"
    39 -> "Rowing (machine)"
    40 -> "Rugby"
    41 -> "Running"
    42 -> "Running (treadmill)"
    44 -> "Sailing"
    45 -> "Scuba diving"
    46 -> "Skating"
    47 -> "Skiing"
    48 -> "Snowboarding"
    49 -> "Snowshoeing"
    50 -> "Softball"
    51 -> "Squash"
    52 -> "Stair climbing"
    53 -> "Stair climbing (machine)"
    54 -> "Strength training"
    55 -> "Stretching"
    56 -> "Surfing"
    57 -> "Swimming (open water)"
    58 -> "Swimming (pool)"
    59 -> "Table tennis"
    60 -> "Tennis"
    61 -> "Volleyball"
    62 -> "Walking"
    63 -> "Water polo"
    64 -> "Weightlifting"
    65 -> "Wheelchair"
    66 -> "Yoga"
    else -> "Exercise"
}
