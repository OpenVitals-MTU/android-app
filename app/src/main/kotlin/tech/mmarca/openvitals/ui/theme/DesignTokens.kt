package tech.mmarca.openvitals.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Layout tokens. Plain constants: a dp is a dp in every theme. Colour stays on the ColorScheme. */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val huge: Dp = 40.dp
    val giant: Dp = 48.dp
}

/**
 * Corner radii. [sm] and [md] are both 12dp today; both names exist so they
 * can diverge later. Prefer `MaterialTheme.shapes` where a component takes one.
 */
object Radii {
    /** Progress fills, tiny chips. */
    val xs: Dp = 8.dp

    /** Inputs, drink rows. */
    val sm: Dp = 12.dp

    /** The default card corner. */
    val md: Dp = 12.dp

    /** Segmented pills, selectors. */
    val lg: Dp = 24.dp

    /** Hero and onboarding cards. */
    val xl: Dp = 32.dp
}

object Emphasis {
    const val wash = 0.12f
    const val subtle = 0.22f
    const val disabled = 0.38f
    const val fill = 0.55f
    const val strong = 0.8f
}

/**
 * The animation durations, and nothing else. Motion is minimal: press
 * feedback and charts drawing in. No decorative or looping animation, bar
 * the one exception documented at its call site.
 */
object Motion {
    /** Press and selection feedback. */
    const val pressMillis = 120

    /** One element changing in place — a toggle, a progress arc settling. */
    const val standardMillis = 300

    /** A chart drawing itself in when its data arrives. */
    const val revealMillis = 550

    /** The one slow mover: an ambient single-run sweep. Must be interruptible. */
    const val ambientMillis = 1200

    /** How long a chart takes to draw itself in on first appearance. */
    const val chartEntryMillis = revealMillis

    /** One breath of a loading skeleton's pulse (each direction of the reverse cycle). */
    const val skeletonPulseMillis = ambientMillis

    /**
     * Half a cycle of the edit-mode wiggle. The one looping animation: an
     * affordance, only while edit mode is on.
     */
    const val editWiggleMillis = 140
}

object LayoutMetrics {
    val screenGutter: Dp = 16.dp
    val cardPadding: Dp = 16.dp
    val metricTilePadding: Dp = 12.dp
    val metricTileGap: Dp = 8.dp
    val actionRowHeight: Dp = 48.dp
    val iconSurfaceSize: Dp = 52.dp
    val minTouchTarget: Dp = 48.dp
}
