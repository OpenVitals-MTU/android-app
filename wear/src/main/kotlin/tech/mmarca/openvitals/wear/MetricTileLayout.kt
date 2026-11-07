package tech.mmarca.openvitals.wear

import androidx.wear.protolayout.ColorBuilders.argb as colorProp
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders.CONTENT_SCALE_MODE_FIT
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_BOLD
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.layout.basicText
import androidx.wear.protolayout.layout.column
import androidx.wear.protolayout.layout.spacer
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString

/** What one metric tile shows: today's value in the metric's accent, a caption, and the day's curve. */
internal class MetricTile(
    val title: String,
    /** The number, or a dash when there is none to show; [caption] then says why. */
    val value: String,
    val caption: String,
    val accent: Int,
    /** The whole tile in one sentence, for a screen reader: the chart is a picture. */
    val description: String,
    val chart: TileChart.Png?,
    /** A line under the curve with one more fact about the day, such as its range. */
    val detail: String? = null,
) {
    /** The same tile without its curve, for when the curve cannot be served. */
    fun withoutChart(): MetricTile = MetricTile(title, value, caption, accent, description, chart = null, detail)
}

/**
 * The layout every metric tile shares: the title, the value in the metric's
 * accent as the phone app prints it, the caption, the day's curve, and a
 * detail line under it when the metric has one. A tap anywhere opens the
 * watch app.
 */
internal fun MaterialScope.metricTileLayout(tile: MetricTile, open: Clickable, density: Float): LayoutElement =
    primaryLayout(
        titleSlot = { text(tile.title.layoutString) },
        mainSlot = {
            val parts = buildList {
                add(basicText(tile.value.layoutString, valueStyle(tile.accent), maxLines = 1))
                add(text(tile.caption.layoutString, typography = Typography.BODY_SMALL, color = colorScheme.onSurfaceVariant))
                tile.chart?.let { chart ->
                    add(spacer(height = dp(CHART_GAP_DP)))
                    add(chartImage(chart, density))
                }
                tile.detail?.let { detail ->
                    add(spacer(height = dp(CHART_GAP_DP)))
                    add(text(detail.layoutString, typography = Typography.BODY_SMALL, color = colorScheme.onSurfaceVariant))
                }
            }
            column(
                *parts.toTypedArray(),
                width = expand(),
                horizontalAlignment = HORIZONTAL_ALIGN_CENTER,
                modifier = LayoutModifier.contentDescription(tile.description),
            )
        },
        onClick = open,
    )

/**
 * The curve, by the resource id [MetricTileService] serves it under, at the
 * size it was drawn, so its hour labels and line are never stretched.
 */
private fun chartImage(chart: TileChart.Png, density: Float): LayoutElement =
    Image.Builder()
        .setResourceId(MetricTileService.CHART_RESOURCE_ID)
        .setWidth(dp(chart.widthPx / density))
        .setHeight(dp(chart.heightPx / density))
        .setContentScaleMode(CONTENT_SCALE_MODE_FIT)
        .build()

/**
 * The value: large, in the metric's accent, as the phone prints a metric's
 * headline. A plain font on purpose: the Material 3 display and numeral
 * styles use a variable font that some tile renderers drop the text over.
 */
private fun valueStyle(accent: Int): FontStyle =
    FontStyle.Builder()
        .setSize(sp(VALUE_SIZE_SP))
        .setWeight(FONT_WEIGHT_BOLD)
        .setColor(colorProp(accent))
        .build()

private const val CHART_GAP_DP = 4f
private const val VALUE_SIZE_SP = 36f
