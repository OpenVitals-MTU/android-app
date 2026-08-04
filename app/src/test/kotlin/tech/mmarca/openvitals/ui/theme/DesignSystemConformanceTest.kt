package tech.mmarca.openvitals.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The app's scales against the OpenVitals design system (`../design-system`).
 *
 * The design system is the golden: `tokens/spacing.css`, `tokens/shape.css`,
 * `tokens/typography.css` and `tokens/motion.css` are the source of these
 * numbers, and `docs/audit-1.md` records why several of them are what they are.
 * The audit's own rule applies here — nothing is guessed; every value below was
 * copied from a token file, not remembered.
 *
 * This is deliberately a scale test, not a screen test. What it protects is the
 * thing that silently rots: a value drifting one step out of the ladder, or a
 * sixth radius appearing because someone typed a number at a call site.
 */
class DesignSystemConformanceTest {

    // ── Spacing: the 4dp Material grid ──────────────────────────────────────

    @Test
    fun `the spacing scale is the design system's 4dp grid`() {
        assertThat(Spacing.xs).isEqualTo(4.dp)
        assertThat(Spacing.sm).isEqualTo(8.dp)
        assertThat(Spacing.md).isEqualTo(12.dp)
        assertThat(Spacing.lg).isEqualTo(16.dp)
        assertThat(Spacing.xl).isEqualTo(20.dp)
        assertThat(Spacing.xxl).isEqualTo(24.dp)
        assertThat(Spacing.xxxl).isEqualTo(32.dp)
        assertThat(Spacing.huge).isEqualTo(40.dp)
        assertThat(Spacing.giant).isEqualTo(48.dp)
    }

    @Test
    fun `every spacing step sits on the 4dp grid`() {
        val steps = listOf(
            Spacing.xs, Spacing.sm, Spacing.md, Spacing.lg, Spacing.xl,
            Spacing.xxl, Spacing.xxxl, Spacing.huge, Spacing.giant,
        )
        for (step in steps) {
            assertWithMessage("$step is off the 4dp grid")
                .that(step.value.toInt() % 4)
                .isEqualTo(0)
        }
        assertWithMessage("the scale must not double back on itself")
            .that(steps)
            .isInStrictOrder()
    }

    // ── Component metrics ───────────────────────────────────────────────────

    @Test
    fun `the touch target is Material's 48dp, not the iOS 44`() {
        // Finding F1. 44 is the iOS HIG floor and this is an Android-only app;
        // Material and Compose both say 48. Visual containers may be smaller so
        // long as the hit area pads out to this.
        assertThat(LayoutMetrics.minTouchTarget).isEqualTo(48.dp)
    }

    @Test
    fun `component metrics match the design system`() {
        assertThat(LayoutMetrics.screenGutter).isEqualTo(16.dp)
        assertThat(LayoutMetrics.cardPadding).isEqualTo(16.dp)
        assertThat(LayoutMetrics.metricTilePadding).isEqualTo(12.dp)
        assertThat(LayoutMetrics.metricTileGap).isEqualTo(8.dp)
        assertThat(LayoutMetrics.actionRowHeight).isEqualTo(48.dp)
        assertThat(LayoutMetrics.iconSurfaceSize).isEqualTo(52.dp)
    }

    // ── Shape ───────────────────────────────────────────────────────────────

    @Test
    fun `the card corner is 12dp`() {
        // Finding F10, and the one value the design system is most explicit
        // about: OpenVitals cards draw a 12dp corner. The 16 this app used to
        // carry is named there as the origin of a stray radius that never
        // matched a shipped pixel.
        assertThat(Radii.md).isEqualTo(12.dp)
    }

    @Test
    fun `the radius scale matches, including the deliberate duplicate`() {
        assertThat(Radii.xs).isEqualTo(8.dp)
        assertThat(Radii.sm).isEqualTo(12.dp)
        assertThat(Radii.md).isEqualTo(12.dp)
        assertThat(Radii.lg).isEqualTo(24.dp)
        assertThat(Radii.xl).isEqualTo(32.dp)
        // sm and md coincide on purpose: the app does not distinguish an input
        // corner from a card corner today. Two names so they CAN diverge later
        // without touching every call site — do not "fix" this into one.
        assertWithMessage("sm and md are documented as coinciding")
            .that(Radii.sm)
            .isEqualTo(Radii.md)
    }

    // ── Typography ──────────────────────────────────────────────────────────

    @Test
    fun `body and title styles carry Material 3 tracking`() {
        // Finding F4.2. These styles used to omit letter spacing entirely while
        // the label styles carried theirs — an accident of this file's origin.
        // The design system settled it in favour of M3's values.
        assertThat(AppTypography.titleMedium.letterSpacing).isEqualTo(0.15.sp)
        assertThat(AppTypography.titleSmall.letterSpacing).isEqualTo(0.1.sp)
        assertThat(AppTypography.bodyLarge.letterSpacing).isEqualTo(0.5.sp)
        assertThat(AppTypography.bodyMedium.letterSpacing).isEqualTo(0.25.sp)
        assertThat(AppTypography.bodySmall.letterSpacing).isEqualTo(0.4.sp)
    }

    @Test
    fun `label tracking is unchanged`() {
        assertThat(AppTypography.labelLarge.letterSpacing).isEqualTo(0.1.sp)
        assertThat(AppTypography.labelMedium.letterSpacing).isEqualTo(0.5.sp)
        assertThat(AppTypography.labelSmall.letterSpacing).isEqualTo(0.5.sp)
    }

    @Test
    fun `the heavier headline and title weights are kept`() {
        // Finding F4.1: heavier than M3's defaults, and deliberately so — the
        // brand is numbers-first with big bold numerals. Pinned so a later
        // "align with Material" sweep has to argue with this test first.
        assertThat(AppTypography.headlineLarge.fontWeight?.weight).isEqualTo(700)
        assertThat(AppTypography.headlineMedium.fontWeight?.weight).isEqualTo(600)
        assertThat(AppTypography.titleLarge.fontWeight?.weight).isEqualTo(600)
    }

    @Test
    fun `the metric headline uses tabular figures`() {
        // A counter that changes width as it counts is the one thing a
        // numbers-first dashboard cannot do.
        assertThat(AppTypography.headlineMedium.fontFeatureSettings).isEqualTo("tnum")
    }

    // ── Motion ──────────────────────────────────────────────────────────────

    @Test
    fun `the motion scale is the design system's four durations`() {
        assertThat(Motion.pressMillis).isEqualTo(120)
        assertThat(Motion.standardMillis).isEqualTo(300)
        assertThat(Motion.revealMillis).isEqualTo(550)
        assertThat(Motion.ambientMillis).isEqualTo(1200)
    }

    @Test
    fun `the named uses resolve to scale steps rather than their own numbers`() {
        assertThat(Motion.chartEntryMillis).isEqualTo(Motion.revealMillis)
        assertThat(Motion.skeletonPulseMillis).isEqualTo(Motion.ambientMillis)
    }

    @Test
    fun `the motion scale is strictly increasing`() {
        assertThat(
            listOf(
                Motion.pressMillis,
                Motion.standardMillis,
                Motion.revealMillis,
                Motion.ambientMillis,
            ),
        ).isInStrictOrder()
    }

    // ── Emphasis ────────────────────────────────────────────────────────────

    @Test
    fun `the emphasis ladder is the tint scale, not Material state layers`() {
        // Finding F11: these are a tint ladder for STATIC content. Material's
        // state layers (hover 8 / focus 10 / press 10 / drag 16) are a
        // different system that Material widgets already apply. They share only
        // the 38% disabled value. Do not merge them.
        assertThat(Emphasis.wash).isEqualTo(0.12f)
        assertThat(Emphasis.subtle).isEqualTo(0.22f)
        assertThat(Emphasis.disabled).isEqualTo(0.38f)
        assertThat(Emphasis.fill).isEqualTo(0.55f)
        assertThat(Emphasis.strong).isEqualTo(0.8f)
    }
}
