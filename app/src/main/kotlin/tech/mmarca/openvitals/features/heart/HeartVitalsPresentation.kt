package tech.mmarca.openvitals.features.heart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

internal val oxygenColor = Color(0xFF00897B)
internal val respiratoryColor = Color(0xFF5E97F6)
internal val temperatureColor = Color(0xFFFF7043)
internal val vo2Color = Color(0xFF7E57C2)
internal val glucoseColor = Color(0xFF8E5D42)

internal data class SummaryMetric(
    val title: String,
    val value: String,
    val unit: String,
    val icon: ImageVector,
    val color: Color,
    val source: String,
)
