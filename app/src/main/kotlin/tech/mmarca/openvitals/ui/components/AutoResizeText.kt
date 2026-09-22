package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    minLines: Int = 1,
    softWrap: Boolean = true,
    /**
     * How small the text may get. Unspecified keeps at least [MinShrinkFraction] of the
     * style's size, and never less than [SmallestFontSize]: the user set a size for a reason.
     */
    minFontSize: TextUnit = TextUnit.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    val maxFontSize = when {
        fontSize != TextUnit.Unspecified -> fontSize
        style.fontSize != TextUnit.Unspecified -> style.fontSize
        else -> 16.sp
    }
    val floor = if (minFontSize != TextUnit.Unspecified) minFontSize else autoResizeFloor(maxFontSize)

    Text(
        text = text,
        modifier = modifier,
        color = color,
        autoSize = TextAutoSize.StepBased(
            minFontSize = floor,
            maxFontSize = maxFontSize,
            stepSize = 0.5.sp,
        ),
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        overflow = TextOverflow.Ellipsis,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        style = style,
    )
}

/** The fraction of the style's size that shrinking keeps. */
internal const val MinShrinkFraction = 0.75f

/** No shrunk text goes under this. */
internal val SmallestFontSize = 12.sp

/** The floor for a text of [maxFontSize]. A floor above the size means no shrinking. */
internal fun autoResizeFloor(maxFontSize: TextUnit): TextUnit {
    if (!maxFontSize.isSp) return maxFontSize
    return maxOf(SmallestFontSize.value, maxFontSize.value * MinShrinkFraction).sp
}
