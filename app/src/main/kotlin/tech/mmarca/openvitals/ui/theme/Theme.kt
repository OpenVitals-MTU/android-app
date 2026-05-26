package tech.mmarca.openvitals.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Color(0xFF003547),
    primaryContainer = Color(0xFF004E66),
    onPrimaryContainer = Color(0xFFBDEAFF),
    secondary = BlueGrey80,
    onSecondary = Color(0xFF30313A),
    secondaryContainer = Color(0xFF474852),
    onSecondaryContainer = Color(0xFFE2E1EC),
    tertiary = Teal80,
    onTertiary = Color(0xFF00382B),
    tertiaryContainer = Color(0xFF00513F),
    onTertiaryContainer = Color(0xFF9CF3D9),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101416),
    onBackground = Color(0xFFE0E3E6),
    surface = SurfaceDark,
    onSurface = Color(0xFFE0E3E6),
    surfaceVariant = Color(0xFF40484D),
    onSurfaceVariant = Color(0xFFC0C8CE),
    outline = Color(0xFF8A9298),
    outlineVariant = Color(0xFF40484D),
    surfaceContainerLowest = Color(0xFF0B0F11),
    surfaceContainerLow = Color(0xFF15191B),
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = Color(0xFF303437),
    surfaceContainerHighest = Color(0xFF3B3F42),
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC3E8FF),
    onPrimaryContainer = Color(0xFF001F2A),
    secondary = BlueGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E1EC),
    onSecondaryContainer = Color(0xFF1B1B23),
    tertiary = Teal40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF9CF3D9),
    onTertiaryContainer = Color(0xFF002117),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDCE3E9),
    onSurfaceVariant = Color(0xFF40484D),
    outline = Color(0xFF70787E),
    outlineVariant = Color(0xFFC0C8CE),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F6F8),
    surfaceContainer = Color(0xFFEFF1F4),
    surfaceContainerHigh = Color(0xFFE9ECEF),
    surfaceContainerHighest = Color(0xFFE3E6E9),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun OpenVitalsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
