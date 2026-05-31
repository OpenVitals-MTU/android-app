package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint
import tech.mmarca.openvitals.ui.theme.WorkoutColor

@Composable
internal fun RoutePreview(
    points: List<ExerciseRoutePoint>,
    modifier: Modifier = Modifier,
) {
    val routeGeometry = remember(points) { points.toRoutePreviewGeometry() }
    val routeColor = WorkoutColor
    val startColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val endColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
    val backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val borderColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Canvas(modifier = modifier) {
        val corner = 10.dp.toPx()
        drawRoundRect(
            color = backgroundColor,
            cornerRadius = CornerRadius(corner, corner),
        )

        val padding = 18.dp.toPx()
        val drawWidth = size.width - padding * 2
        val drawHeight = size.height - padding * 2
        if (drawWidth <= 0f || drawHeight <= 0f || routeGeometry.points.isEmpty()) return@Canvas

        fun project(point: ExerciseRoutePoint): Offset {
            val x = padding + (((point.longitude - routeGeometry.minLongitude) / routeGeometry.longitudeSpan).toFloat() * drawWidth)
            val y = padding + (((routeGeometry.maxLatitude - point.latitude) / routeGeometry.latitudeSpan).toFloat() * drawHeight)
            return Offset(x, y)
        }

        val projected = routeGeometry.points.map(::project)
        projected.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = routeColor,
                start = start,
                end = end,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        val markerRadius = 6.dp.toPx()
        drawCircle(color = startColor, radius = markerRadius, center = projected.first())
        drawCircle(color = endColor, radius = markerRadius, center = projected.last())
        drawRoundRect(
            color = borderColor,
            style = Stroke(width = 1.dp.toPx()),
            cornerRadius = CornerRadius(corner, corner),
        )
    }
}

private data class RoutePreviewGeometry(
    val points: List<ExerciseRoutePoint>,
    val maxLatitude: Double,
    val minLongitude: Double,
    val longitudeSpan: Double,
    val latitudeSpan: Double,
)

private fun List<ExerciseRoutePoint>.toRoutePreviewGeometry(): RoutePreviewGeometry {
    val orderedPoints = sortedBy { it.time }
    if (orderedPoints.isEmpty()) {
        return RoutePreviewGeometry(
            points = emptyList(),
            maxLatitude = 0.0,
            minLongitude = 0.0,
            longitudeSpan = 1.0,
            latitudeSpan = 1.0,
        )
    }
    val minLatitude = orderedPoints.minOf { it.latitude }
    val maxLatitude = orderedPoints.maxOf { it.latitude }
    val minLongitude = orderedPoints.minOf { it.longitude }
    val maxLongitude = orderedPoints.maxOf { it.longitude }
    return RoutePreviewGeometry(
        points = orderedPoints,
        maxLatitude = maxLatitude,
        minLongitude = minLongitude,
        longitudeSpan = (maxLongitude - minLongitude).takeIf { it > 0.0 } ?: 0.00001,
        latitudeSpan = (maxLatitude - minLatitude).takeIf { it > 0.0 } ?: 0.00001,
    )
}
