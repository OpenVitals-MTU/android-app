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
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings

@Composable
fun ActivityDetailScreen(
    viewModel: ActivityDetailViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditActivity: (String) -> Unit = {},
    onDeleteActivity: () -> Unit = {},
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

    when {
        state.isLoading -> FullScreenLoading()
        error != null -> ErrorMessage(message = error)
        workout != null -> ActivityDetailContent(
            workout = workout,
            heartRateSamples = state.heartRateSamples,
            markers = state.markers,
            isDeleting = state.isDeleting,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEditActivity = onEditActivity,
            onDeleteActivity = { viewModel.deleteActivity(onDeleteActivity) },
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
        )
    }
}

@Composable
private fun ActivityDetailContent(
    workout: ExerciseData,
    heartRateSamples: List<HeartRateSample>,
    markers: List<ActivityRecordingMarker>,
    isDeleting: Boolean,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditActivity: (String) -> Unit,
    onDeleteActivity: () -> Unit,
    onOpenRouteInMap: () -> Unit,
    onSaveRouteAsGpx: () -> Unit,
    onSaveRouteAsKmz: () -> Unit,
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
        if (heartRateSamples.isNotEmpty()) {
            item {
                ActivityHeartRateChartCard(
                    samples = heartRateSamples,
                    sessionStart = workout.startTime,
                    sessionEnd = workout.endTime,
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
