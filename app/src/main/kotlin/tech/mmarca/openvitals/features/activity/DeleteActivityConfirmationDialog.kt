package tech.mmarca.openvitals.features.activity

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton

/** Asked before a workout is deleted, on the dashboard and in the activity list. There is no undo. */
@Composable
internal fun DeleteActivityConfirmationDialog(
    workout: ExerciseData,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dashboard_delete_activity_title)) },
        text = {
            Text(
                stringResource(
                    R.string.dashboard_delete_activity_message,
                    exerciseTypeLabel(workout.exerciseType),
                )
            )
        },
        confirmButton = {
            OpenVitalsTextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
