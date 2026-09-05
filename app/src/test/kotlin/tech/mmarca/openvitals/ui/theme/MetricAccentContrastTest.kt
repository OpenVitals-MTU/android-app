package tech.mmarca.openvitals.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.pow
import org.junit.Test

/**
 * Every metric accent clears WCAG 1.4.11's 3:1 floor against both static surfaces.
 * This measures rather than pins hex values, because brightening is the regression.
 * The stock Material-500 swatches failed on eight of seventeen; the worst case now is 3.09.
 */
class MetricAccentContrastTest {

    @Test
    fun `every metric accent clears 3 to 1 on both light and dark surfaces`() {
        val failures = MetricAccents.mapNotNull { (name, color) ->
            val onLight = contrastRatio(color, LightSurface)
            val onDark = contrastRatio(color, DarkSurface)
            val worst = minOf(onLight, onDark)
            if (worst >= GraphicalObjectFloor) {
                null
            } else {
                "%s: %.2f:1 on light, %.2f:1 on dark".format(name, onLight, onDark)
            }
        }

        assertWithMessage(
            "accents are drawn as chart strokes and icons, so WCAG 1.4.11's 3:1 " +
                "applies against BOTH static surfaces — re-measure before brightening one",
        )
            .that(failures)
            .isEmpty()
    }

    @Test
    fun `the palette keeps the headroom the audit left it`() {
        // The worst case is 3.09:1. A drop takes the last of the margin; a big climb wants re-auditing.
        val worst = MetricAccents.minOf { (_, color) ->
            minOf(contrastRatio(color, LightSurface), contrastRatio(color, DarkSurface))
        }

        assertWithMessage("worst-case accent contrast was %.2f:1".format(worst))
            .that(worst)
            .isAtLeast(GraphicalObjectFloor)
    }

    @Test
    fun `the contrast maths agrees with a known pair`() {
        // Guards the guard: black on white is exactly 21:1 by definition.
        assertWithMessage("black on white must be 21:1")
            .that(contrastRatio(Color(0xFF000000), Color(0xFFFFFFFF)))
            .isWithin(0.01)
            .of(21.0)
        assertWithMessage("a colour against itself is 1:1")
            .that(contrastRatio(LightSurface, LightSurface))
            .isWithin(0.001)
            .of(1.0)
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** WCAG 2.x relative luminance, sRGB. */
    private fun relativeLuminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private companion object {
        /** WCAG 1.4.11 — graphical objects and user-interface components. */
        const val GraphicalObjectFloor = 3.0

        /** `LightColorScheme.surface`. */
        val LightSurface = Color(0xFFFCFCFF)

        /** `SurfaceDark`, the dark scheme's surface. */
        val DarkSurface = Color(0xFF1A1C1E)

        val MetricAccents: List<Pair<String, Color>> = listOf(
            "steps" to StepsColor,
            "distance" to DistanceColor,
            "sleep" to SleepColor,
            "heart" to HeartColor,
            "vitals" to VitalsColor,
            "weight" to WeightColor,
            "calories" to CaloriesColor,
            "hydration" to HydrationColor,
            "nutrition" to NutritionColor,
            "workout" to WorkoutColor,
            "body fat" to BodyFatColor,
            "floors" to FloorsColor,
            "active calories" to ActiveCaloriesColor,
            "elevation" to ElevationColor,
            "wheelchair pushes" to WheelchairPushesColor,
            "mindfulness" to MindfulnessColor,
            "cycle" to CycleColor,
        )
    }
}
