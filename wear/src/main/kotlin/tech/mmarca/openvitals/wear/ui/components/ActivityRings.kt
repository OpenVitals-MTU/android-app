package tech.mmarca.openvitals.wear.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import tech.mmarca.openvitals.wear.ui.theme.Emphasis

/** One ring of [ActivityRings]: progress towards a daily goal. */
data class ActivityRing(
    val progress: Float,
    val color: Color,
    val icon: ImageVector,
    val value: String,
)

private val RingSize = 88.dp
private val RingStroke = 8.dp
private val RingGap = 2.dp

/**
 * Up to three nested goal rings with their values beside them, the watch
 * equivalent of the activity rings on Fitbit, Pixel Watch and Apple Watch.
 * A ring keeps going past a full turn when the goal is beaten.
 */
@Composable
fun ActivityRings(rings: List<ActivityRing>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(RingSize), contentAlignment = Alignment.Center) {
            rings.take(3).forEachIndexed { index, ring ->
                val inset: Dp = (RingStroke + RingGap) * index * 2
                CircularProgressIndicator(
                    progress = { ring.progress },
                    allowProgressOverflow = true,
                    strokeWidth = RingStroke,
                    colors = ProgressIndicatorDefaults.colors(
                        indicatorColor = ring.color,
                        trackColor = ring.color.copy(alpha = Emphasis.subtle),
                    ),
                    modifier = Modifier.size(RingSize - inset),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            rings.take(3).forEach { ring ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(ring.icon, contentDescription = null, tint = ring.color, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = ring.value, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
