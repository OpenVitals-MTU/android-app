package tech.mmarca.openvitals.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout tokens. Plain constants, not a theme extension — a dp is a dp in
 * every theme. Colour deliberately stays on [androidx.compose.material3.ColorScheme];
 * these cover only the spacing/radius/emphasis decisions that used to be
 * literals at call sites.
 */
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
 * Corner radii, matching the design system's shape scale.
 *
 * [sm] and [md] are both 12dp today: the app does not distinguish an input
 * corner from a card corner. Both names exist so the two CAN diverge later
 * without touching every call site — this duplication is deliberate, not
 * something to tidy away.
 *
 * Prefer `MaterialTheme.shapes` where a Material component takes a `Shape`;
 * these are for the places that build a `RoundedCornerShape` by hand.
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
 * The four animation durations, and nothing else.
 *
 * Naming them is what stops the fifth from being whatever someone types that
 * day — which is exactly what had happened: five unnamed values (140, 450,
 * 650, 1800, 2800) were scattered across call sites while this object named
 * two the design system had settled on.
 *
 * Motion here is minimal and functional: press feedback, and charts drawing in
 * when their data lands. No decorative or looping animation, no scale or
 * bounce. The one exception is documented at its call site.
 */
object Motion {
    /** Press and selection feedback. */
    const val pressMillis = 120

    /** One element changing in place — a toggle, a progress arc settling. */
    const val standardMillis = 300

    /** A chart drawing itself in when its data arrives. */
    const val revealMillis = 550

    /**
     * The one slow mover: an ambient, single-run sweep. Anything this long has
     * to be interruptible.
     */
    const val ambientMillis = 1200

    /** How long a chart takes to draw itself in on first appearance. */
    const val chartEntryMillis = revealMillis

    /** One breath of a loading skeleton's pulse (each direction of the reverse cycle). */
    const val skeletonPulseMillis = ambientMillis

    /**
     * Half a cycle of the edit-mode wiggle — the tilt that says a tile can be
     * dragged. The one looping animation the app keeps, and the documented
     * exception to "no looping motion": it is an affordance rather than
     * decoration, it runs only while edit mode is on, and it stops the moment
     * it is left. Five call sites had this literal; a wiggle that is 140 in one
     * grid and 120 in the next reads as a bug.
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
