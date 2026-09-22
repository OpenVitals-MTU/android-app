package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import tech.mmarca.openvitals.R

/**
 * The one line a screen reader gets instead of the picture: what is
 * plotted, over what span, and the headline number. Composed here so the
 * ordering stays identical across every chart.
 */
internal fun chartSemanticSummary(
    title: String,
    summaryText: String? = null,
    rangeLabel: String? = null,
    emptyLabel: String? = null,
): String {
    val parts = buildList {
        add(title.trim())
        rangeLabel?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
        val value = summaryText?.trim()?.takeIf { it.isNotEmpty() } ?: emptyLabel?.trim()
        value?.takeIf { it.isNotEmpty() }?.let(::add)
    }
    // Comma-separated so a screen reader pauses between clauses.
    return parts.joinToString(separator = ", ")
}

/**
 * Publishes [description] as the accessible name. `semantics`, not
 * `clearAndSetSemantics`: interactive charts keep their actions.
 */
internal fun Modifier.chartSemantics(description: String): Modifier =
    semantics { contentDescription = description }

/** What a screen reader says for one bucket: the date, then the value, or that there is none. */
internal fun chartBucketDescription(dateText: String, valueText: String?, noDataLabel: String): String =
    "$dateText, ${valueText ?: noDataLabel}"

/** The numbers a plotted series is reduced to when spoken. */
internal data class PlotSummary(
    val count: Int,
    val lowest: Double,
    val highest: Double,
    val latest: Double,
)

/** Null when nothing is plotted, so the caller says nothing rather than "0 points". */
internal fun plotSummary(values: List<Double>): PlotSummary? {
    if (values.isEmpty()) return null
    return PlotSummary(count = values.size, lowest = values.min(), highest = values.max(), latest = values.last())
}

/** [title], then the shape of [values] as one sentence. Null when there is nothing to say. */
@Composable
internal fun plotSemanticSummary(
    title: String?,
    values: List<Double>,
    valueFormatter: (Double) -> String,
): String? {
    val shape = plotSummary(values)?.let { summary ->
        pluralStringResource(
            R.plurals.chart_plot_summary,
            summary.count,
            summary.count,
            valueFormatter(summary.lowest),
            valueFormatter(summary.highest),
            valueFormatter(summary.latest),
        )
    }
    val parts = listOfNotNull(title?.trim()?.takeIf { it.isNotEmpty() }, shape)
    return parts.joinToString(separator = ", ").takeIf { it.isNotEmpty() }
}
