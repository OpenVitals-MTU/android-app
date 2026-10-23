package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A grant missing for one part of a card: what the card could not do, and a
 * button that asks for it. A screen that needs the grant as a whole uses
 * [PermissionCallout] or the Health Connect screen shell instead.
 */
@Composable
fun InlineGrantPrompt(
    message: String,
    actionLabel: String,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OpenVitalsTextButton(onClick = onGrant) {
            Text(actionLabel)
        }
    }
}
