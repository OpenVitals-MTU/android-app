package tech.mmarca.openvitals.wear.ui.components

import java.text.NumberFormat
import java.util.Locale

fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)

/** Kilometres with one decimal. Metric only until the unit preference reaches the watch. */
fun formatKilometers(meters: Int): String =
    String.format(Locale.getDefault(), "%.1f", meters / 1000f)
