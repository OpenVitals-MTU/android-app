package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.Spacing

enum class ObservationScaleType {
    PAIN,
    MOOD,
    ENERGY,
}

private val StepMinHeight: Dp = 64.dp
private val GlyphBox: Dp = 30.dp
private val GlyphSize: Dp = 24.dp
private val BatteryWidth: Dp = 16.dp
private val SelectedBorder: Dp = 2.dp
private val RestingBorder: Dp = 1.dp
private val StrokeWidth: Dp = 2.dp

/**
 * A one-to-five scale with a glyph per step: growing dots for pain, mood arcs
 * from a frown to a smile, a battery for energy. Tapping the selected step
 * clears it, so "not recorded" stays reachable.
 */
@Composable
internal fun CycleObservationScale(
    label: String,
    supportingText: String,
    value: Int?,
    type: ObservationScaleType,
    valueDescription: (Int) -> String,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            (1..5).forEach { step ->
                ScaleStep(
                    step = step,
                    selected = value == step,
                    description = valueDescription(step),
                    type = type,
                    enabled = enabled,
                    onClick = { onValueChange(step.takeUnless { value == step }) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ScaleStep(
    step: Int,
    selected: Boolean,
    description: String,
    type: ObservationScaleType,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val markColor = if (selected) scheme.onPrimaryContainer else scheme.outline
    Surface(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = StepMinHeight)
            .semantics {
                role = Role.RadioButton
                contentDescription = description
            },
        shape = MaterialTheme.shapes.small,
        color = if (selected) scheme.primaryContainer else scheme.surface,
        border = if (selected) BorderStroke(SelectedBorder, scheme.primary) else BorderStroke(RestingBorder, scheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Box(modifier = Modifier.height(GlyphBox), contentAlignment = Alignment.Center) {
                when (type) {
                    ObservationScaleType.PAIN -> PainGlyph(step, selected, markColor)
                    ObservationScaleType.MOOD -> MoodGlyph(step, markColor)
                    ObservationScaleType.ENERGY -> EnergyGlyph(step, markColor)
                }
            }
            Text(
                text = step.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            )
        }
    }
}

/** A dot that grows with the step. Decorative: the step number and state carry the value. */
@Composable
private fun PainGlyph(step: Int, selected: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(GlyphSize)) {
        val radius = (3.5f + (step - 1) * 2.2f).dp.toPx()
        drawCircle(
            color = if (selected) color else color.copy(alpha = Emphasis.disabled),
            radius = radius,
            center = Offset(size.width / 2f, size.height / 2f),
        )
    }
}

/** A mouth from a frown to a smile. */
@Composable
private fun MoodGlyph(step: Int, color: Color) {
    Canvas(modifier = Modifier.size(GlyphSize)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = StrokeWidth.toPx(), cap = StrokeCap.Round)
        when (step) {
            1 -> drawArc(color, 180f, 180f, false, Offset(w * 0.15f, h * 0.42f), Size(w * 0.7f, h * 0.45f), style = stroke)
            2 -> drawArc(color, 190f, 160f, false, Offset(w * 0.2f, h * 0.46f), Size(w * 0.6f, h * 0.32f), style = stroke)
            3 -> drawLine(color, Offset(w * 0.2f, h * 0.52f), Offset(w * 0.8f, h * 0.52f), StrokeWidth.toPx(), StrokeCap.Round)
            4 -> drawArc(color, 10f, 160f, false, Offset(w * 0.2f, h * 0.32f), Size(w * 0.6f, h * 0.32f), style = stroke)
            else -> drawArc(color, 0f, 180f, false, Offset(w * 0.15f, h * 0.22f), Size(w * 0.7f, h * 0.45f), style = stroke)
        }
    }
}

/** A battery with one bar lit per step. */
@Composable
private fun EnergyGlyph(step: Int, color: Color) {
    Canvas(modifier = Modifier.size(width = BatteryWidth, height = GlyphSize)) {
        val slots = 5
        val slotHeight = 3.dp.toPx()
        val gap = 2.dp.toPx()
        val startY = size.height - slotHeight
        for (slot in 1..slots) {
            drawRoundRect(
                color = if (slot <= step) color else color.copy(alpha = Emphasis.subtle),
                topLeft = Offset(0f, startY - (slot - 1) * (slotHeight + gap)),
                size = Size(size.width, slotHeight),
                cornerRadius = CornerRadius(1.5.dp.toPx()),
            )
        }
    }
}
