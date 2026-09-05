package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * The "start over" card for the metrics OpenVitals derives itself — Body
 * energy, recovery and expenditure. The wipe is not undoable and takes up to
 * four months of Body energy history with it, so unlike the tuning reset next
 * to it this one confirms first.
 */
/** Nudges the leading icon down to sit on the title's cap height. */
private val LeadingIconOpticalOffset = 2.dp
private val LeadingIconSize = 20.dp
private val ProgressIndicatorSize = 18.dp
private val ProgressStrokeWidth = 2.dp

@Composable
internal fun DerivedMetricsResetCard(
    isResetting: Boolean,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = LeadingIconOpticalOffset)
                        .size(LeadingIconSize),
                )
                Column(
                    modifier = Modifier
                        .padding(start = Spacing.md)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.settings_derived_metrics_reset_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_derived_metrics_reset_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
            OpenVitalsOutlinedButton(
                onClick = { confirming = true },
                enabled = !isResetting,
                buttonColors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md),
            ) {
                if (isResetting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ProgressIndicatorSize),
                        strokeWidth = ProgressStrokeWidth,
                    )
                } else {
                    Text(stringResource(R.string.settings_derived_metrics_reset_action))
                }
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.settings_derived_metrics_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_derived_metrics_reset_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onReset()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.settings_derived_metrics_reset_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
