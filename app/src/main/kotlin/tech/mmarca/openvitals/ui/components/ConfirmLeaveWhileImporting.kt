package tech.mmarca.openvitals.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R

/**
 * An import runs in its screen's scope, so leaving the screen stops it. Back used to do
 * that without a word, part-way through hundreds of files. While [importing], Back asks
 * first. What is already imported stays, and a second run does not duplicate it.
 */
@Composable
fun ConfirmLeaveWhileImporting(importing: Boolean) {
    var asking by rememberSaveable { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    BackHandler(enabled = importing && !leaving) { asking = true }
    // The handler above is off by the time this runs, so this Back goes through.
    LaunchedEffect(leaving) { if (leaving) backDispatcher?.onBackPressed() }

    if (asking && importing) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(stringResource(R.string.import_leave_title)) },
            text = { Text(stringResource(R.string.import_leave_body)) },
            confirmButton = {
                OpenVitalsTextButton(
                    onClick = {
                        asking = false
                        leaving = true
                    },
                ) {
                    Text(stringResource(R.string.import_leave_confirm))
                }
            },
            dismissButton = {
                OpenVitalsTextButton(onClick = { asking = false }) {
                    Text(stringResource(R.string.import_leave_stay))
                }
            },
        )
    }
}
