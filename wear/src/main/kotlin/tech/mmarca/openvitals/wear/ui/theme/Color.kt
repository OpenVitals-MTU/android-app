package tech.mmarca.openvitals.wear.ui.theme

import androidx.compose.ui.graphics.Color

// Copied from the phone app's ui/theme/Color.kt. The two modules share no
// code yet, so a colour changed there has to be changed here too.

// Primary brand
val Blue80 = Color(0xFF82D2F2)
val BlueGrey80 = Color(0xFFC6C7D0)
val Teal80 = Color(0xFF80D7BE)

/** Metric accent colours. Data only, never interactive chrome. */
val StepsColor = Color(0xFF3F9A63)
val DistanceColor = Color(0xFF3B7DD8)
val HeartColor = Color(0xFFD2497B)
val VitalsColor = Color(0xFFC4453E)
val WorkoutColor = Color(0xFF2AA0A0)
val FloorsColor = Color(0xFFA8881F)
val ActiveCaloriesColor = Color(0xFFDE6C39)
val ElevationColor = Color(0xFF6E9440)

// Surface variants
val SurfaceDark = Color(0xFF1A1C1E)
val SurfaceContainerDark = Color(0xFF2B2D30)

object Emphasis {
    const val subtle = 0.22f
    const val disabled = 0.38f
    const val fill = 0.55f
}
