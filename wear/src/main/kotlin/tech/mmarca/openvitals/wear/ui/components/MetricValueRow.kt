package tech.mmarca.openvitals.wear.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.ui.theme.Emphasis

/** A value and its optional unit on one baseline. A null [value] renders the "no reading" dash. */
@Composable
fun MetricValueRow(
    value: String?,
    modifier: Modifier = Modifier,
    unit: String? = null,
    valueStyle: TextStyle = MaterialTheme.typography.titleLarge,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = value ?: stringResource(R.string.metric_no_value),
            // Tabular figures, as on the phone: a ticking value must not shift.
            style = valueStyle.copy(fontFeatureSettings = "tnum"),
            color = if (value != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = Emphasis.disabled)
            },
            modifier = Modifier.alignByBaseline(),
        )
        if (unit != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}
