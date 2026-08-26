package tech.mmarca.openvitals.features.activity

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.insights.ActivitySplits
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryReading
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings

@Composable
internal fun ActivityDetailScreen(
    viewModel: ActivityDetailViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditActivity: (String) -> Unit = {},
    onDeleteActivity: () -> Unit = {},
    onOpenPlan: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val error = state.error
    val workout = state.workout
    val latestWorkout by rememberUpdatedState(workout)
    fun showRouteExportFailure() {
        Toast.makeText(
            context,
            R.string.activity_route_export_failed,
            Toast.LENGTH_LONG,
        ).show()
    }
    val saveGpxRoute = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ActivityRouteExportFormat.GPX.mimeType),
    ) { uri ->
        val currentWorkout = latestWorkout ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            context.saveActivityRouteExport(
                workout = currentWorkout,
                format = ActivityRouteExportFormat.GPX,
                destination = uri,
            )
                .onSuccess {
                    Toast.makeText(
                        context,
                        R.string.activity_route_export_saved,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                .onFailure { showRouteExportFailure() }
        }
    }
    val saveKmzRoute = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ActivityRouteExportFormat.KMZ.mimeType),
    ) { uri ->
        val currentWorkout = latestWorkout ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            context.saveActivityRouteExport(
                workout = currentWorkout,
                format = ActivityRouteExportFormat.KMZ,
                destination = uri,
            )
                .onSuccess {
                    Toast.makeText(
                        context,
                        R.string.activity_route_export_saved,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                .onFailure { showRouteExportFailure() }
        }
    }
    fun launchRouteExport(format: ActivityRouteExportFormat) {
        val currentWorkout = latestWorkout ?: return
        val fileName = currentWorkout.routeExportFileName(format)
        when (format) {
            ActivityRouteExportFormat.GPX -> saveGpxRoute.launch(fileName)
            ActivityRouteExportFormat.KMZ -> saveKmzRoute.launch(fileName)
        }
    }
    fun shareRoute(format: ActivityRouteExportFormat) {
        val currentWorkout = latestWorkout ?: return
        context.shareActivityRoute(workout = currentWorkout, format = format)
            .onFailure {
                Toast.makeText(
                    context,
                    R.string.activity_route_share_failed,
                    Toast.LENGTH_LONG,
                ).show()
            }
    }

    when {
        state.isLoading -> FullScreenLoading()
        error != null -> ErrorMessage(message = error.resolve().orEmpty())
        workout != null -> ActivityDetailContent(
            workout = workout,
            heartRateSamples = state.heartRateSamples,
            heartRateRecovery = state.heartRateRecovery,
            speedSamples = state.speedSamples,
            cadenceSamples = state.cadenceSamples,
            markers = state.markers,
            coMapsSamples = state.coMapsSamples,
            splits = state.splits,
            splitDistanceMeters = state.splitDistanceMeters,
            slowestSplitPaceSeconds = state.slowestSplitPaceSeconds,
            fastestSplitPaceSeconds = state.fastestSplitPaceSeconds,
            splitSpeedTrace = state.splitSpeedTrace,
            elevationSamples = state.elevationSamples,
            isDeleting = state.isDeleting,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEditActivity = onEditActivity,
            onDeleteActivity = { viewModel.deleteActivity(onDeleteActivity) },
            linkedPlanTitle = state.linkedPlan?.title,
            onOpenPlan = onOpenPlan,
            onOpenRouteInMap = {
                context.openActivityRouteInMap(workout)
                    .onFailure {
                        Toast.makeText(
                            context,
                            R.string.activity_route_open_failed,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
            },
            onSaveRouteAsGpx = { launchRouteExport(ActivityRouteExportFormat.GPX) },
            onSaveRouteAsKmz = { launchRouteExport(ActivityRouteExportFormat.KMZ) },
            onShareRouteAsGpx = { shareRoute(ActivityRouteExportFormat.GPX) },
            onShareRouteAsKmz = { shareRoute(ActivityRouteExportFormat.KMZ) },
        )
    }
}

@Composable
internal fun ActivityDetailContent(
    workout: ExerciseData,
    heartRateSamples: List<HeartRateSample>,
    heartRateRecovery: HeartRateRecoveryReading?,
    speedSamples: List<SpeedSample>,
    cadenceSamples: List<ActivityCadenceSample>,
    markers: List<ActivityRecordingMarker>,
    coMapsSamples: List<CoMapsNavigationSnapshot> = emptyList(),
    splits: ActivitySplits,
    splitDistanceMeters: Double,
    slowestSplitPaceSeconds: Double?,
    fastestSplitPaceSeconds: Double?,
    splitSpeedTrace: ActivitySplitSpeedTrace?,
    elevationSamples: List<ActivityElevationSample>,
    isDeleting: Boolean,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditActivity: (String) -> Unit,
    onDeleteActivity: () -> Unit,
    linkedPlanTitle: String? = null,
    onOpenPlan: (String) -> Unit = {},
    onOpenRouteInMap: () -> Unit,
    onSaveRouteAsGpx: () -> Unit,
    onSaveRouteAsKmz: () -> Unit,
    onShareRouteAsGpx: () -> Unit,
    onShareRouteAsKmz: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            WorkoutSummaryCard(
                workout = workout,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onManageDataSources = { openHealthConnectPermissionSettings(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            MetricsCard(
                workout = workout,
                unitFormatter = unitFormatter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        workout.plannedExerciseSessionId?.let { planId ->
            item {
                ActivityLinkedPlanDetailCard(
                    planTitle = linkedPlanTitle,
                    onOpenPlan = { onOpenPlan(planId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        if (splits.isNotEmpty) {
            item {
                ActivitySplitsCard(
                    splits = splits,
                    splitDistanceMeters = splitDistanceMeters,
                    slowestPaceSeconds = slowestSplitPaceSeconds,
                    fastestPaceSeconds = fastestSplitPaceSeconds,
                    unitFormatter = unitFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        if (heartRateSamples.isNotEmpty()) {
            item {
                ActivityHeartRateChartCard(
                    samples = heartRateSamples,
                    sessionStart = workout.startTime,
                    sessionEnd = workout.endTime,
                    pauses = workout.segments.toSessionPauses(),
                    unitFormatter = unitFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        // Only a reading with a measured peak is worth a card: without one there
        // was no recovery to speak of, and the card would open with a blank.
        if (heartRateRecovery != null && heartRateRecovery.peakBpm != null) {
            item {
                HeartRateRecoveryCard(
                    reading = heartRateRecovery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        // Recorded speed, or the split-derived reconstruction — never both:
        // a measurement beats a reconstruction, and two speed cards on one
        // screen disagreeing by a hair would be worse than either alone.
        if (speedSamples.isNotEmpty()) {
            item {
                ActivitySpeedChartCard(
                    samples = speedSamples,
                    sessionStart = workout.startTime,
                    sessionEnd = workout.endTime,
                    pauses = workout.segments.toSessionPauses(),
                    unitFormatter = unitFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        } else if (splitSpeedTrace != null) {
            item {
                ActivitySplitSpeedChartCard(
                    trace = splitSpeedTrace,
                    source = splits.source,
                    splitDistanceMeters = splitDistanceMeters,
                    sessionStart = workout.startTime,
                    sessionEnd = workout.endTime,
                    pauses = workout.segments.toSessionPauses(),
                    unitFormatter = unitFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        if (elevationSamples.isNotEmpty()) {
            item {
                ActivityElevationChartCard(
                    samples = elevationSamples,
                    sessionStart = workout.startTime,
                    sessionEnd = workout.endTime,
                    pauses = workout.segments.toSessionPauses(),
                    unitFormatter = unitFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        activityCadenceKinds(cadenceSamples).forEach { cadenceKind ->
            item(key = "cadence-$cadenceKind") {
                ActivityCadenceChartCard(
                    samples = cadenceSamples,
                    kind = cadenceKind,
                    sessionStart = workout.startTime,
                    sessionEnd = workout.endTime,
                    pauses = workout.segments.toSessionPauses(),
                    unitFormatter = unitFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        item {
            SessionDetailsCard(
                workout = workout,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            SegmentsCard(
                segments = workout.segments,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            LapsCard(
                laps = workout.laps,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            RouteCard(
                route = workout.route,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenRouteInMap = onOpenRouteInMap,
                onSaveRouteAsGpx = onSaveRouteAsGpx,
                onSaveRouteAsKmz = onSaveRouteAsKmz,
                onShareRouteAsGpx = onShareRouteAsGpx,
                onShareRouteAsKmz = onShareRouteAsKmz,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            ActivityRouteAnalysisCard(
                workout = workout,
                markers = markers,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (coMapsSamples.isNotEmpty()) {
            item {
                ActivityCoMapsNavigationCard(
                    samples = coMapsSamples,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        if (workout.isOpenVitalsEntry && workout.id.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OpenVitalsButton(
                        onClick = { onEditActivity(workout.id) },
                        enabled = !isDeleting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.cd_edit_entry),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    OpenVitalsOutlinedButton(
                        onClick = onDeleteActivity,
                        enabled = !isDeleting,
                        buttonColors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.action_delete),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
