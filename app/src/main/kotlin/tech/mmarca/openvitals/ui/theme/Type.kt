package tech.mmarca.openvitals.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Every style claims its full line-height box, exactly as the shipped app did.
 *
 * Flutter's `Text` reserves the style's whole line height even for a single
 * line — a 16sp title with a 24sp line height is 24sp tall. Compose trims a
 * lone line to its glyph metrics, so the same title measured ~19sp and every
 * stacked-text card in the app came out ~9dp shorter than the design it was
 * ported from. This was measured, not guessed: the settings category cards
 * were 69dp here against 78dp shipped, and the whole difference was the two
 * text boxes.
 *
 * `Trim.None` restores the Flutter behaviour; centring keeps a single line
 * optically where it was. Applied through one helper so no style can forget.
 */
private val FullLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun TextStyle.withFullLineHeight(): TextStyle = copy(
    lineHeightStyle = FullLineHeight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ).withFullLineHeight(),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontFeatureSettings = "tnum",
    ).withFullLineHeight(),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ).withFullLineHeight(),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ).withFullLineHeight(),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ).withFullLineHeight(),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ).withFullLineHeight(),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ).withFullLineHeight(),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ).withFullLineHeight(),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ).withFullLineHeight(),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ).withFullLineHeight(),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ).withFullLineHeight(),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ).withFullLineHeight(),
)
