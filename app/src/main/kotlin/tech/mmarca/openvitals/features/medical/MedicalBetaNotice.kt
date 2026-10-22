package tech.mmarca.openvitals.features.medical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.Spacing

/** Shown on the first open of the records home: the area is a beta, and imported data needs a look. */
@Composable
internal fun MedicalBetaNotice(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // A tap beside the dialog does not pass for having read it; Back still closes it.
        properties = DialogProperties(dismissOnClickOutside = false),
        icon = { Icon(Icons.Outlined.MedicalInformation, contentDescription = null) },
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.medical_records_title), modifier = Modifier.weight(1f, fill = false))
                BetaTag()
            }
        },
        text = { Text(stringResource(R.string.medical_beta_notice_body)) },
        confirmButton = {
            OpenVitalsTextButton(onClick = onDismiss) { Text(stringResource(R.string.medical_beta_notice_accept)) }
        },
    )
}

@Composable
private fun BetaTag() {
    val color = MaterialTheme.colorScheme.tertiary
    OpenVitalsSurface(
        containerColor = color.copy(alpha = Emphasis.wash),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(R.string.medical_beta_tag),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}
