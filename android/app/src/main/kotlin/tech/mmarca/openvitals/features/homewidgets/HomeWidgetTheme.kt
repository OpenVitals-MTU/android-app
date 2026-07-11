package tech.mmarca.openvitals.features.homewidgets

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * Shared design tokens for the home-screen widgets, copied verbatim from the
 * Kotlin app (`HomeMetricWidget.kt:705-710`, `HomeQuickBeverageWidget.kt:593`).
 *
 * The widgets are deliberately un-themed: a flat background, no drawables and no
 * dark-mode variant — exactly as the reference app renders them.
 */
internal object HomeWidgetTokens {
    val Background = Color(0xFF101820)
    val PrimaryText = Color(0xFFF7FAFC)
    val MutedText = Color(0xFFC9D7DD)
    val ActionBackground = Color(0xFF20313A)

    /** Kotlin `MaxHomeWidgetRows`. */
    const val MaxRows = 12

    val BackgroundProvider = ColorProvider(Background)
    val PrimaryTextProvider = ColorProvider(PrimaryText)
    val MutedTextProvider = ColorProvider(MutedText)
    val ActionBackgroundProvider = ColorProvider(ActionBackground)
}
