package tech.mmarca.openvitals.features.imports.csv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.StepBar
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * The two steps the app's own workout export takes in place of the column
 * mapping: what the file's columns become and how its times are read, then a
 * confirmation. The importing and result steps are shared.
 */

/** The recognised and the ignored columns, and the date and time settings a hand-made file may need. */
@Composable
internal fun WorkoutOptionsStep(
    state: CsvImportState,
    viewModel: CsvImportViewModel,
) {
    val workouts = state.workouts ?: return
    val sample = state.sample ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(Spacing.lg),
        ) {
            item {
                OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            text = stringResource(R.string.settings_csv_import_workouts_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.settings_csv_import_workouts_body, state.fileName.orEmpty()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.sm),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.md)) }
            item {
                OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            text = stringResource(R.string.settings_csv_import_workouts_columns_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        workouts.recognisedColumns.keys.forEach { name ->
                            Text(
                                text = workoutColumnLabel(name),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = Spacing.xs),
                            )
                        }
                        WorkoutIgnoredColumns(workouts)
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.md)) }
            item {
                CsvDateTimeSection(
                    settings = workouts.dateTime,
                    firstValue = sample.columnValues(workouts.startTimeColumn).firstOrNull(),
                    onSettingsChange = viewModel::setDateTimeSettings,
                )
            }
        }
        StepBar(
            nextLabel = stringResource(R.string.settings_csv_import_continue),
            onNext = if (state.canContinue) {
                { viewModel.goToStep(CsvImportStep.CONFIRM) }
            } else {
                null
            },
            backLabel = stringResource(R.string.settings_csv_import_back),
            onBack = viewModel::reset,
        )
    }
}

/** The confirm step for a workout export: the sampled rows, their span, and the permissions. */
@Composable
internal fun WorkoutConfirmStep(
    state: CsvImportState,
    viewModel: CsvImportViewModel,
    onGrantPermissions: () -> Unit,
) {
    val workouts = state.workouts ?: return
    val sample = state.sample ?: return
    val missing = state.missingPermissions
    val rowsWithContent = remember(sample, workouts) { workoutSampleRowsWithContent(sample.dataRows, workouts) }
    val range = remember(sample, workouts) { workoutSampleDateRange(sample.dataRows, workouts) }
    val locale = LocalLocale.current.platformLocale
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(Spacing.lg),
        ) {
            item {
                OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            text = stringResource(R.string.settings_csv_import_confirm_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_csv_import_workouts_confirm_summary,
                                state.fileName.orEmpty(),
                                rowsWithContent,
                                sample.dataRows.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = Spacing.sm),
                        )
                        if (range != null) {
                            Spacer(Modifier.height(Spacing.md))
                            // The date span is the last guard against a day/month mix-up.
                            Text(
                                text = stringResource(
                                    R.string.settings_csv_import_confirm_dates,
                                    format.format(range.first),
                                    format.format(range.second),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        WorkoutIgnoredColumns(workouts)
                    }
                }
                if (missing.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.md))
                    PermissionCallout(
                        title = stringResource(R.string.settings_csv_import_permission_title),
                        body = stringResource(R.string.settings_csv_import_workouts_permission_body),
                        onGrant = onGrantPermissions,
                    )
                }
            }
        }
        StepBar(
            nextLabel = stringResource(R.string.settings_csv_import_start),
            onNext = viewModel::startImport,
            backLabel = stringResource(R.string.settings_csv_import_back),
            onBack = { viewModel.goToStep(CsvImportStep.MAPPING) },
        )
    }
}

/** The file's columns a session has no field for, by their header names. Nothing when the file has none. */
@Composable
private fun WorkoutIgnoredColumns(workouts: WorkoutImportPlan) {
    val ignored = workouts.ignoredColumns.keys
    if (ignored.isEmpty()) return
    Spacer(Modifier.height(Spacing.md))
    Text(
        text = stringResource(R.string.settings_csv_import_workouts_ignored_title),
        style = MaterialTheme.typography.titleSmall,
    )
    Text(
        text = ignored.joinToString(", "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.xs),
    )
    Text(
        text = stringResource(R.string.settings_csv_import_workouts_ignored_body),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.xs),
    )
}

/** What a recognised column becomes, in the app's language. Exhaustive over [WorkoutCsvColumns.Imported]. */
@Composable
internal fun workoutColumnLabel(name: String): String = stringResource(
    when (name) {
        WorkoutCsvColumns.Title -> R.string.settings_csv_import_workout_column_title
        WorkoutCsvColumns.ActivityType -> R.string.settings_csv_import_workout_column_activity_type
        WorkoutCsvColumns.StartTime -> R.string.settings_csv_import_workout_column_start_time
        WorkoutCsvColumns.EndTime -> R.string.settings_csv_import_workout_column_end_time
        WorkoutCsvColumns.DurationSeconds -> R.string.settings_csv_import_workout_column_duration
        WorkoutCsvColumns.DistanceMeters -> R.string.settings_csv_import_workout_column_distance
        WorkoutCsvColumns.ElevationGainedMeters -> R.string.settings_csv_import_workout_column_elevation
        WorkoutCsvColumns.Steps -> R.string.settings_csv_import_workout_column_steps
        WorkoutCsvColumns.TotalCaloriesKcal -> R.string.settings_csv_import_workout_column_total_calories
        WorkoutCsvColumns.ActiveCaloriesKcal -> R.string.settings_csv_import_workout_column_active_calories
        WorkoutCsvColumns.AverageHeartRateBpm -> R.string.settings_csv_import_workout_column_average_heart_rate
        WorkoutCsvColumns.FloorsClimbed -> R.string.settings_csv_import_workout_column_floors_climbed
        WorkoutCsvColumns.WheelchairPushes -> R.string.settings_csv_import_workout_column_wheelchair_pushes
        else -> R.string.settings_csv_import_workout_column_notes
    },
)
