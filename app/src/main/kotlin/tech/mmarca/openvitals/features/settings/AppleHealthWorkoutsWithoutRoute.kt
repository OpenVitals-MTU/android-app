package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.features.activity.exerciseTypeLabel
import tech.mmarca.openvitals.features.imports.applehealth.AppleWorkoutWithoutRoute
import tech.mmarca.openvitals.features.imports.applehealth.toExerciseType

private const val CollapsedCount = 5

/**
 * The workouts an Apple Health import brought in without their route, so each
 * can be found and its route imported by hand. Listed here only: the report
 * the user shares counts them by type and never says when they were.
 */
@Composable
internal fun AppleHealthWorkoutsWithoutRoute(
    workouts: List<AppleWorkoutWithoutRoute>,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(workouts) { workouts.sortedBy { it.start } }
    val formatter = remember { DateTimeFormatterProvider().mediumDateTime().withZone(ZoneId.systemDefault()) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.padding(top = 8.dp)) {
        Text(
            text = pluralStringResource(R.plurals.settings_apple_health_workouts_without_route_title, sorted.size, sorted.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_apple_health_workouts_without_route_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        (if (expanded) sorted else sorted.take(CollapsedCount)).forEach { workout ->
            Text(
                text = "${exerciseTypeLabel(workout.appleType.toExerciseType())} · ${formatter.format(workout.start)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (!expanded && sorted.size > CollapsedCount) {
            TextButton(onClick = { expanded = true }) {
                Text(stringResource(R.string.settings_apple_health_workouts_without_route_show_all))
            }
        }
    }
}
