package tech.mmarca.openvitals.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.settings.OpenWatchMoveNoticeViewModel

@Composable
fun OpenWatchMoveNoticeDialog(
    onGetOpenWatch: () -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.openwatch_move_title)) },
        text = { Text(stringResource(R.string.openwatch_move_body)) },
        confirmButton = {
            OpenVitalsTextButton(onClick = onGetOpenWatch) {
                Text(stringResource(R.string.openwatch_move_action_get))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onClose) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

/** Shows the notice while the ViewModel says it is due; every way out dismisses it. */
@Composable
fun OpenWatchMoveNoticePrompt(viewModel: OpenWatchMoveNoticeViewModel) {
    val context = LocalContext.current
    val visible by viewModel.visible.collectAsStateWithLifecycle()
    val openWatchUrl = stringResource(R.string.openwatch_url)
    val noBrowserMessage = stringResource(R.string.openwatch_move_no_browser, openWatchUrl)

    if (visible) {
        OpenWatchMoveNoticeDialog(
            onGetOpenWatch = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, openWatchUrl.toUri()))
                } catch (_: ActivityNotFoundException) {
                    // This app needs no browser, so some phones that run it have none.
                    Toast.makeText(context, noBrowserMessage, Toast.LENGTH_LONG).show()
                }
                viewModel.onDismissed()
            },
            onClose = viewModel::onDismissed,
        )
    }
}
