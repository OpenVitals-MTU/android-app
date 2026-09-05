package tech.mmarca.openvitals.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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
