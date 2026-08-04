package tech.mmarca.openvitals.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * The one line a screen reader gets instead of the picture.
 *
 * A chart is a `Canvas`: it draws pixels and publishes no semantics at all, so
 * without this it is not merely hard to read but entirely absent — a
 * screen-reader user swiping through a metric screen goes from the title
 * straight to whatever follows the chart, with no indication that a chart was
 * there or what it showed.
 *
 * The summary is not a description of the drawing ("a line chart trending
 * upwards"). It is the same information a sighted user takes from a two-second
 * glance: what is being plotted, over what span, and the headline number. The
 * shape of the line is detail; the number is the point.
 *
 * Composed here rather than at each call site so the ordering and punctuation
 * stay identical across eighteen charts — a screen reader reads these back to
 * back as a user moves down a screen, and a summary that reorders its clauses
 * per chart is harder to follow than one that is merely terse.
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
    // Comma-separated so a screen reader pauses between clauses rather than
    // running the title into the number.
    return parts.joinToString(separator = ", ")
}

/**
 * Publishes [description] as the chart's accessible name.
 *
 * Deliberately `semantics` and not `clearAndSetSemantics`: several charts are
 * interactive — a day can be selected, a scrubber dragged — and clearing would
 * take those actions away from exactly the users who most need them announced.
 * A `Canvas` has no child semantics to conflict with, so adding is enough.
 */
internal fun Modifier.chartSemantics(description: String): Modifier =
    semantics { contentDescription = description }
