package tech.mmarca.openvitals.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * The phone app's dark scheme on a black background, which is what its
 * AMOLED mode does and what a watch display wants. The `Dim` roles have no
 * phone counterpart; they are the same hue one step darker.
 *
 * Typography and shapes stay on the Wear defaults: they are sized for a
 * round 1.2" display, and the phone's 12dp card corner reads as square there.
 */
private val WearColorScheme = ColorScheme(
    primary = Blue80,
    primaryDim = Color(0xFF66B6D5),
    primaryContainer = Color(0xFF004E66),
    onPrimary = Color(0xFF003547),
    onPrimaryContainer = Color(0xFFBDEAFF),
    secondary = BlueGrey80,
    secondaryDim = Color(0xFFAAABB4),
    secondaryContainer = Color(0xFF474852),
    onSecondary = Color(0xFF30313A),
    onSecondaryContainer = Color(0xFFE2E1EC),
    tertiary = Teal80,
    tertiaryDim = Color(0xFF64BBA3),
    tertiaryContainer = Color(0xFF00513F),
    onTertiary = Color(0xFF00382B),
    onTertiaryContainer = Color(0xFF9CF3D9),
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = Color(0xFF3B3F42),
    onSurface = Color(0xFFE0E3E6),
    onSurfaceVariant = Color(0xFFC0C8CE),
    outline = Color(0xFF8A9298),
    outlineVariant = Color(0xFF40484D),
    background = Color.Black,
    onBackground = Color(0xFFE0E3E6),
    error = Color(0xFFFFB4AB),
    errorDim = Color(0xFFE39A91),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun OpenVitalsWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WearColorScheme,
        content = content,
    )
}
