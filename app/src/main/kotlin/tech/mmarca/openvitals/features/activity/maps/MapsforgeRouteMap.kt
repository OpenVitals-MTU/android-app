package tech.mmarca.openvitals.features.activity.maps

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import kotlin.math.max
import kotlin.math.min
import org.mapsforge.core.graphics.Cap
import org.mapsforge.core.graphics.Join
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.overlay.FixedPixelCircle
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.activity.RoutePreview

@Composable
internal fun MapsforgeRouteMap(
    mapPacks: List<OfflineMapPack>,
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
    currentPoint: ExerciseRoutePoint?,
    showRecenterControl: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapPacksKey = remember(mapPacks) {
        mapPacks.joinToString(separator = "|") { pack -> "${pack.id}:${pack.path}" }
    }
    val renderState = remember(mapPacksKey) { MapsforgeRouteMapRenderState() }
    val mapViewResult = remember(context, mapPacksKey) {
        runCatching { createMapsforgeMapView(context, mapPacks) }
    }
    val mapView = mapViewResult.getOrNull()

    if (mapView == null) {
        RoutePreview(
            points = points,
            routeBreakIndexes = routeBreakIndexes,
            modifier = modifier,
        )
        return
    }

    val recenterDescription = stringResource(R.string.cd_recenter_map)

    DisposableEffect(mapView) {
        onDispose {
            mapView.destroyAll()
            AndroidGraphicFactory.clearResourceMemoryCache()
        }
    }

    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            update = { view ->
                renderState.render(
                    mapView = view,
                    points = points,
                    routeBreakIndexes = routeBreakIndexes,
                    currentPoint = currentPoint,
                )
            },
        )

        if (showRecenterControl) {
            FloatingActionButton(
                onClick = { renderState.recenter(mapView, points, currentPoint) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = recenterDescription,
                )
            }
        }
    }
}

private class MapsforgeRouteMapRenderState {
    private var routeLayers: List<Layer> = emptyList()
    private var didFitInitialCamera = false

    fun render(
        mapView: MapView,
        points: List<ExerciseRoutePoint>,
        routeBreakIndexes: List<Int>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        updateRouteLayers(mapView, points, routeBreakIndexes, currentPoint)
        fitInitialCamera(mapView, points, currentPoint)
    }

    fun recenter(
        mapView: MapView,
        points: List<ExerciseRoutePoint>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        fitMapsforgeCamera(mapView, points, currentPoint)
        didFitInitialCamera = true
    }

    private fun updateRouteLayers(
        mapView: MapView,
        points: List<ExerciseRoutePoint>,
        routeBreakIndexes: List<Int>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        val layers = mapView.getLayerManager().getLayers()
        routeLayers.forEach { layer ->
            layers.remove(layer)
            layer.onDestroy()
        }
        routeLayers = buildMapsforgeRouteLayers(points, routeBreakIndexes, currentPoint)
        routeLayers.forEach(layers::add)
        mapView.getLayerManager().redrawLayers()
    }

    private fun fitInitialCamera(
        mapView: MapView,
        points: List<ExerciseRoutePoint>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        if (didFitInitialCamera) return
        if (points.isEmpty() && currentPoint == null) return
        fitMapsforgeCamera(mapView, points, currentPoint)
        didFitInitialCamera = true
    }
}

private fun createMapsforgeMapView(
    context: Context,
    mapPacks: List<OfflineMapPack>,
): MapView {
    require(mapPacks.isNotEmpty()) { "At least one Mapsforge map pack is required." }
    val application = context.applicationContext as? Application
        ?: error("Mapsforge requires an application context.")
    AndroidGraphicFactory.createInstance(application)

    val mapView = MapView(context)
    mapView.disallowAncestorInterceptDuringTouch()
    mapView.getMapScaleBar().setVisible(true)
    mapView.setBuiltInZoomControls(false)

    val model = mapView.getModel()
    val cacheName = "openvitals-${mapPacks.joinToString("|") { it.id }.hashCode()}"
    val tileCache = AndroidUtil.createTileCache(
        context,
        cacheName,
        model.displayModel.getTileSize(),
        1f,
        model.frameBufferModel.getOverdrawFactor(),
    )
    val mapDataStore = MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE).apply {
        mapPacks.forEachIndexed { index, pack ->
            addMapDataStore(
                MapFile(File(pack.path)),
                index == 0,
                index == 0,
            )
        }
    }
    val tileRendererLayer = TileRendererLayer(
        tileCache,
        mapDataStore,
        model.mapViewPosition,
        AndroidGraphicFactory.INSTANCE,
    )
    tileRendererLayer.setXmlRenderTheme(MapsforgeThemes.DEFAULT)
    mapView.getLayerManager().getLayers().add(tileRendererLayer)
    mapView.setCenter(mapDataStore.startPosition())
    mapView.setZoomLevel(mapDataStore.startZoomLevel() ?: DefaultMapsforgeZoom)
    return mapView
}

@SuppressLint("ClickableViewAccessibility")
private fun View.disallowAncestorInterceptDuringTouch() {
    setOnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_POINTER_DOWN -> view.requestAncestorIntercept(disallow = true)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> view.requestAncestorIntercept(disallow = false)
        }
        false
    }
}

private fun View.requestAncestorIntercept(disallow: Boolean) {
    var currentParent = parent
    while (currentParent != null) {
        currentParent.requestDisallowInterceptTouchEvent(disallow)
        currentParent = currentParent.parent
    }
}

private fun buildMapsforgeRouteLayers(
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
    currentPoint: ExerciseRoutePoint?,
): List<Layer> {
    val validPoints = points.filter { point -> point.hasFiniteCoordinates() }
    val routeLineLayers = routeSegments(points, routeBreakIndexes)
        .filter { segment -> segment.size >= 2 }
        .map { segment ->
            Polyline(routePaint(RouteLineColor, RouteLineWidthPx), AndroidGraphicFactory.INSTANCE)
                .apply { setPoints(segment.map { point -> point.toLatLong() }) }
        }
    return buildList {
        addAll(routeLineLayers)
        validPoints.firstOrNull()?.let { point ->
            add(markerCircle(point, StartMarkerColor, MarkerRadiusPx))
        }
        validPoints.lastOrNull()?.let { point ->
            add(markerCircle(point, EndMarkerColor, MarkerRadiusPx))
        }
        currentPoint?.takeIf { point -> point.hasFiniteCoordinates() }?.let { point ->
            add(markerCircle(point, CurrentLocationColor, CurrentLocationRadiusPx))
        }
    }
}

private fun fitMapsforgeCamera(
    mapView: MapView,
    points: List<ExerciseRoutePoint>,
    currentPoint: ExerciseRoutePoint?,
) {
    val cameraPoints = (points + listOfNotNull(currentPoint))
        .filter { point -> point.latitude.isFinite() && point.longitude.isFinite() }
    if (cameraPoints.isEmpty()) return

    var minLatitude = cameraPoints.first().latitude
    var maxLatitude = cameraPoints.first().latitude
    var minLongitude = cameraPoints.first().longitude
    var maxLongitude = cameraPoints.first().longitude
    cameraPoints.forEach { point ->
        minLatitude = min(minLatitude, point.latitude)
        maxLatitude = max(maxLatitude, point.latitude)
        minLongitude = min(minLongitude, point.longitude)
        maxLongitude = max(maxLongitude, point.longitude)
    }

    mapView.setCenter(
        LatLong(
            (minLatitude + maxLatitude) / 2.0,
            (minLongitude + maxLongitude) / 2.0,
        ),
    )
    mapView.setZoomLevel(zoomForBounds(minLatitude, maxLatitude, minLongitude, maxLongitude))
}

private fun zoomForBounds(
    minLatitude: Double,
    maxLatitude: Double,
    minLongitude: Double,
    maxLongitude: Double,
): Byte {
    val span = max(maxLatitude - minLatitude, maxLongitude - minLongitude)
    return when {
        span <= 0.002 -> 16
        span <= 0.005 -> 15
        span <= 0.01 -> 14
        span <= 0.03 -> 13
        span <= 0.07 -> 12
        span <= 0.15 -> 11
        span <= 0.30 -> 10
        span <= 0.70 -> 9
        span <= 1.40 -> 8
        else -> 7
    }.toByte()
}

private fun markerCircle(
    point: ExerciseRoutePoint,
    fillColor: String,
    radius: Float,
): FixedPixelCircle =
    FixedPixelCircle(
        point.toLatLong(),
        radius,
        fillPaint(fillColor),
        strokePaint(MarkerStrokeColor, MarkerStrokeWidthPx),
    )

private fun routePaint(color: String, strokeWidth: Float): Paint =
    strokePaint(color, strokeWidth).apply {
        setStrokeCap(Cap.ROUND)
        setStrokeJoin(Join.ROUND)
    }

private fun fillPaint(color: String): Paint =
    AndroidGraphicFactory.INSTANCE.createPaint().apply {
        setColor(mapsforgeColor(color))
        setStyle(Style.FILL)
    }

private fun strokePaint(color: String, strokeWidth: Float): Paint =
    AndroidGraphicFactory.INSTANCE.createPaint().apply {
        setColor(mapsforgeColor(color))
        setStrokeWidth(strokeWidth)
        setStyle(Style.STROKE)
    }

private fun mapsforgeColor(color: String): Int {
    val parsed = android.graphics.Color.parseColor(color)
    return AndroidGraphicFactory.INSTANCE.createColor(
        android.graphics.Color.alpha(parsed),
        android.graphics.Color.red(parsed),
        android.graphics.Color.green(parsed),
        android.graphics.Color.blue(parsed),
    )
}

private fun ExerciseRoutePoint.toLatLong(): LatLong =
    LatLong(latitude, longitude)

private fun ExerciseRoutePoint.hasFiniteCoordinates(): Boolean =
    latitude.isFinite() && longitude.isFinite()

private const val RouteLineColor = "#D9462F"
private const val StartMarkerColor = "#1F9D55"
private const val EndMarkerColor = "#6B5DD3"
private const val CurrentLocationColor = "#1D4ED8"
private const val MarkerStrokeColor = "#FFFFFF"
private const val RouteLineWidthPx = 8.0f
private const val MarkerRadiusPx = 7.0f
private const val CurrentLocationRadiusPx = 8.0f
private const val MarkerStrokeWidthPx = 3.0f
private const val DefaultMapsforgeZoom: Byte = 12
