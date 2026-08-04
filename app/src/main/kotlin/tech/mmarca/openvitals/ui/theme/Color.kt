package tech.mmarca.openvitals.ui.theme

import androidx.compose.ui.graphics.Color

// Primary brand
val OpenVitalsBlue = Color(0xFF006E8F)
val OpenVitalsGreen = Color(0xFF2F6F4F)
val OpenVitalsCoral = Color(0xFF9B4438)

val Blue80 = Color(0xFF82D2F2)
val BlueGrey80 = Color(0xFFC6C7D0)
val Teal80 = Color(0xFF80D7BE)

val Blue40 = OpenVitalsBlue
val BlueGrey40 = Color(0xFF5E5F68)
val Teal40 = OpenVitalsGreen

/**
 * Metric accent colours — the contrast-audited palette from the OpenVitals
 * design system (`../design-system/tokens/colors.css`).
 *
 * These replace the stock Material-500 swatches this file used to carry, which
 * failed WCAG 1.4.11's 3:1 floor for graphical objects on **eight of the
 * seventeen**: measured against this app's own light (#FCFCFF) and dark
 * (#1A1C1E) surfaces, floors/amber came out at **1.59:1** — the value is drawn
 * as a chart stroke and an icon, so 3:1 is the binding rule, and 1.59 is not
 * close. Steps, sleep, weight, hydration, workout, body fat and elevation
 * failed on one surface each. Every accent below clears 3:1 against BOTH, worst
 * case 3.09.
 *
 * There is barely any headroom, so: **never brighten one of these without
 * re-measuring against both surfaces.** Making an accent "pop" is the exact
 * regression this palette was built to fix. Dynamic colour re-tints the chrome
 * but not these, so the static surfaces stay the binding constraint; AMOLED
 * only ever increases contrast.
 *
 * They belong on data alone — icons, strokes, small indicators — and never on
 * interactive chrome, so a wallpaper-derived `primary` cannot make a control
 * impersonate a metric.
 */
val StepsColor = Color(0xFF3F9A63)
val DistanceColor = Color(0xFF3B7DD8)
val SleepColor = Color(0xFF6C5CD6)
val HeartColor = Color(0xFFD2497B)
val VitalsColor = Color(0xFFC4453E)
val WeightColor = Color(0xFFBE7A2C)
val CaloriesColor = Color(0xFFDD5F3E)
val HydrationColor = Color(0xFF2E97C9)
val NutritionColor = Color(0xFF5C9E4B)
val WorkoutColor = Color(0xFF2AA0A0)
val BodyFatColor = Color(0xFF8A6A55)
val FloorsColor = Color(0xFFA8881F)
val ActiveCaloriesColor = Color(0xFFDE6C39)
val ElevationColor = Color(0xFF6E9440)
val WheelchairPushesColor = Color(0xFF2E8C7F)
val MindfulnessColor = Color(0xFF8A6E9C)
val CycleColor = Color(0xFFBE5C85)

// Surface variants
val SurfaceDark = Color(0xFF1A1C1E)
val SurfaceContainerDark = Color(0xFF2B2D30)

// Activity recording outdoor (sunlight) readability
val RecordingOutdoorAccent = Color(0xFFFFB300)
val RecordingOutdoorLightAccent = Color(0xFFE65100)
val RecordingOutdoorAccentMuted = Color(0xFFFF8F00)
val RecordingOutdoorBackground = Color(0xFF000000)
val RecordingOutdoorSurface = Color(0xFF121212)
