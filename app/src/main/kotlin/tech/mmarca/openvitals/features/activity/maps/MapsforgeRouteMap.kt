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
import org.mapsforge.core.graphics.Cap
import org.mapsforge.core.graphics.Join
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Dimension
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
    val mapPacksKey = remember(mapPacks) { mapsforgeMapPacksKey(mapPacks) }
    val renderState = remember(mapPacksKey) { MapsforgeRouteMapRenderState() }
    val mapResult = remember(context, mapPacksKey) {
        runCatching { createMapsforgeMap(context, mapPacks) }
    }
    val map = mapResult.getOrNull()
    val mapView = map?.view

    if (map == null || mapView == null) {
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
            // This map's own layers, caches and data stores only. The resource
            // memory cache used to be cleared here too, but it is GLOBAL: a
            // second map still on screen — the detail screen's, while a
            // recording map goes away — lost its symbols and labels with it.
            mapView.destroyAll()
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
                    map = map,
                    mapView = view,
                    points = points,
                    routeBreakIndexes = routeBreakIndexes,
                    currentPoint = currentPoint,
                )
            },
        )

        if (showRecenterControl) {
            FloatingActionButton(
                onClick = { renderState.recenter(map, points, currentPoint) },
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
        map: MapsforgeMap,
        mapView: MapView,
        points: List<ExerciseRoutePoint>,
        routeBreakIndexes: List<Int>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        updateRouteLayers(mapView, points, routeBreakIndexes, currentPoint)
        fitInitialCamera(map, points, currentPoint)
    }

    fun recenter(
        map: MapsforgeMap,
        points: List<ExerciseRoutePoint>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        fitMapsforgeCamera(map, points, currentPoint)
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
        map: MapsforgeMap,
        points: List<ExerciseRoutePoint>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        if (didFitInitialCamera) return
        if (points.isEmpty() && currentPoint == null) return
        fitMapsforgeCamera(map, points, currentPoint)
        didFitInitialCamera = true
    }
}

/** A built map, with the zoom range its packs actually carry tiles for. */
private class MapsforgeMap(
    val view: MapView,
    val zoomRange: MapsforgeZoomRange,
)

private fun createMapsforgeMap(
    context: Context,
    mapPacks: List<OfflineMapPack>,
): MapsforgeMap {
    require(mapPacks.isNotEmpty()) { "At least one Mapsforge map pack is required." }
    val application = context.applicationContext as? Application
        ?: error("Mapsforge requires an application context.")
    ensureMapsforgeGraphicsFactory(application)

    val mapView = MapView(context)
    mapView.disallowAncestorInterceptDuringTouch()
    mapView.getMapScaleBar().setVisible(true)
    mapView.setBuiltInZoomControls(false)

    val model = mapView.getModel()
    val tileCache = AndroidUtil.createTileCache(
        context,
        mapsforgeTileCacheName(mapPacks),
        model.displayModel.getTileSize(),
        1f,
        model.frameBufferModel.getOverdrawFactor(),
    )
    val mapFiles = mapPacks.map { pack -> MapFile(File(pack.path)) }
    val mapDataStore = MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE).apply {
        mapFiles.forEachIndexed { index, mapFile ->
            addMapDataStore(mapFile, index == 0, index == 0)
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

    // What the packs themselves hold, rather than a fixed guess: a city extract
    // carries detail a country one does not, and asking for a zoom outside the
    // range renders blank.
    val zoomRange = MapsforgeZoomRange(
        min = mapFiles.minOf { it.mapFileInfo.zoomLevelMin },
        max = mapFiles.maxOf { it.mapFileInfo.zoomLevelMax },
    )
    mapView.setZoomLevelMin(zoomRange.min)
    mapView.setZoomLevelMax(zoomRange.max)
    mapView.setZoomLevel(
        zoomRange.clamp(mapDataStore.startZoomLevel() ?: DefaultMapsforgeZoom),
    )
    return MapsforgeMap(view = mapView, zoomRange = zoomRange)
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
    map: MapsforgeMap,
    points: List<ExerciseRoutePoint>,
    currentPoint: ExerciseRoutePoint?,
) {
    val boundingBox = routeBoundingBox(points, currentPoint) ?: return
    val mapView = map.view
    mapView.setCenter(boundingBox.centerPoint)

    val viewport = mapView.getModel().mapViewDimension.getDimension()
    if (viewport == null) {
        // The view has not been measured yet, and a zoom chosen against a
        // viewport of unknown size is the bug being fixed. Retry once the
        // layout pass has run; the centre above is already correct.
        mapView.post { applyMapsforgeZoom(map, boundingBox) }
        return
    }
    applyMapsforgeZoom(map, boundingBox, viewport)
}

private fun applyMapsforgeZoom(
    map: MapsforgeMap,
    boundingBox: BoundingBox,
    viewport: Dimension? = map.view.getModel().mapViewDimension.getDimension(),
) {
    val mapView = map.view
    val measured = viewport ?: return
    mapView.setZoomLevel(
        mapsforgeZoomForBounds(
            boundingBox = boundingBox,
            viewport = measured,
            tileSize = mapView.getModel().displayModel.getTileSize(),
            zoomRange = map.zoomRange,
        ),
    )
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
