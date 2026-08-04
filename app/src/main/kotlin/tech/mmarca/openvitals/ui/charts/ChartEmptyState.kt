package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * What a chart says when there is nothing to draw — one answer, so an absent chart never
 * silently vanishes and takes its card with it.
 *
 * [height] reserves the chart's footprint, so a card that resolves from empty to full
 * does not make the page jump. Null lets the text take only the room it needs.
 */
@Composable
fun ChartEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    height: Dp? = null,
) {
    val sized = if (height == null) {
        modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    } else {
        modifier
            .fillMaxWidth()
            .height(height)
    }
    Box(modifier = sized, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Muted, and small. An empty state is the least important thing on the
            // screen: it should be legible and then get out of the way, not sit there as
            // a large grey exclamation about a day you simply did not log.
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
