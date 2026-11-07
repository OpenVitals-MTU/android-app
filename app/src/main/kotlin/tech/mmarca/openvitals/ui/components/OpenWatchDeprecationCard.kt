package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * Heads the Watches and Scales settings sections: device support is moving
 * to OpenWatch, what stays here is deprecated, and a button opens the
 * OpenWatch page. One card for both sections, so the wording never drifts.
 */
@Composable
fun OpenWatchDeprecationCard(modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.openwatch_deprecation_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.openwatch_deprecation_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReferenceLinkButton(
                title = stringResource(R.string.openwatch_deprecation_action),
                url = stringResource(R.string.openwatch_url),
            )
        }
    }
}
