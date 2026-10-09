package tech.mmarca.openvitals.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

/**
 * The phones on the status screen: the one asking to be allowed, with
 * Allow and Block; a refused one, with Forget; the trusted ones; and the
 * trusted ones that are no longer paired, with a way to Bluetooth settings.
 */
@Composable
internal fun PhoneCards(
    link: WearLinkUiState,
    onAllow: (PendingPhone) -> Unit,
    onBlock: (PendingPhone) -> Unit,
    onForget: (String) -> Unit,
    onOpenBluetoothSettings: () -> Unit,
) {
    val pending = link.pending
    if (pending != null) {
        Line(stringResource(R.string.status_phone_pending, pending.name), title = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            Button(onClick = { onAllow(pending) }) { Text(stringResource(R.string.action_allow)) }
            Button(onClick = { onBlock(pending) }, colors = androidx.wear.compose.material3.ButtonDefaults.outlinedButtonColors()) {
                Text(stringResource(R.string.action_block))
            }
        }
    }
    val refused = link.refused
    if (refused != null) {
        Line(stringResource(R.string.status_phone_refused, refused.name))
        Button(onClick = { onForget(refused.address) }, modifier = Modifier.padding(top = 4.dp)) {
            Text(stringResource(R.string.action_forget_phone))
        }
    }
    for (phone in link.trusted) {
        if (link.bondLost.any { it.address == phone.address }) {
            Line(stringResource(R.string.status_phone_bond_lost, phone.name))
        } else {
            Line(stringResource(R.string.status_phone_trusted, phone.name))
        }
    }
    if (link.bondLost.isNotEmpty()) {
        Button(onClick = onOpenBluetoothSettings, modifier = Modifier.padding(top = 4.dp)) {
            Text(stringResource(R.string.action_open_bluetooth), style = MaterialTheme.typography.labelSmall)
        }
    }
}
