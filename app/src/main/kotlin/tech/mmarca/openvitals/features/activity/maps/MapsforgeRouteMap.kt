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
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mapsforge.core.graphics.Cap
import org.mapsforge.core.graphics.Join
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Dimension
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rotation
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.model.common.Observer
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.overlay.FixedPixelCircle
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.activity.RoutePreview
import tech.mmarca.openvitals.ui.theme.Spacing

@Composable
internal fun MapsforgeRouteMap(
    mapPacks: List<OfflineMapPack>,
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
    currentPoint: ExerciseRoutePoint?,
    showRecenterControl: Boolean,
    plannedRoute: CoMapsRoutePolyline? = null,
    modifier: Modifier = Modifier,
) {
    // Same shift as the MapLibre renderer: the walk over a six-figure point
    // buffer happens once per route revision on a worker dispatcher, and the
    // view update only ever receives the finished geometry.
    val plannedRouteDisplay by produceState<MapsforgePlannedRoute?>(null, plannedRoute) {
        value = plannedRoute?.takeUnless { it.isEmpty }?.let { route ->
            withContext(Dispatchers.Default) {
                MapsforgePlannedRoute(
                    points = (0 until route.pointCount).map { index ->
                        LatLong(route.latitudeAt(index), route.longitudeAt(index))
                    },
                    arrows = plannedRouteTurnArrows(route),
                    destination = route.destination?.let { LatLong(it.latitude, it.longitude) },
                )
            }
        }
    }
    val headingDegrees by rememberDeviceHeadingDegrees(enabled = currentPoint != null)
    val context = LocalContext.current
    val mapPacksKey = remember(mapPacks) { mapsforgeMapPacksKey(mapPacks) }
    val renderState = remember(mapPacksKey) { MapsforgeRouteMapRenderState() }
    val mapResult = remember(context, mapPacksKey) {
        runCatching { createMapsforgeMap(context, mapPacks, onUserPan = renderState::onUserPan) }
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
    val resetRotationDescription = stringResource(R.string.cd_reset_map_rotation)

    // Mapsforge has no compass widget of its own, so the reset control is
    // hand-rolled: watch the position model (rotation lives there and every
    // change notifies) and surface a button whenever the map is off north.
    var mapRotationDegrees by remember(mapView) { mutableFloatStateOf(mapView.mapRotation.degrees) }
    DisposableEffect(mapView) {
        val observer = Observer {
            val degrees = mapView.mapRotation.degrees
            // Observers can fire from mapsforge's animator thread.
            mapView.post { mapRotationDegrees = degrees }
        }
        mapView.getModel().mapViewPosition.addObserver(observer)
        onDispose { mapView.getModel().mapViewPosition.removeObserver(observer) }
    }

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
                    plannedRoute = plannedRouteDisplay,
                    headingDegrees = headingDegrees,
                )
            },
        )

        if (abs(mapRotationDegrees) > NorthUpToleranceDegrees) {
            SmallFloatingActionButton(
                onClick = {
                    mapView.rotate(Rotation.NULL_ROTATION)
                    mapView.getLayerManager().redrawLayers()
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.md),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Explore,
                    contentDescription = resetRotationDescription,
                )
            }
        }

        if (showRecenterControl) {
            FloatingActionButton(
                onClick = { renderState.recenter(map, points, currentPoint) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.md),
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

    /**
     * Whether the camera tracks the live fix. On from the first frame — a map
     * with a current point is a recording in progress, and a recording map's
     * job is to keep the user in the middle of it — and off the moment the
     * user pans away, until the recenter button re-engages it. Maps without a
     * live fix (a recorded activity's route) never enter the follow path.
     */
    private var followCurrentPoint = true
    private var followedPoint: LatLong? = null

    /**
     * The planned route's layers outlive the per-tick track rebuilds, and are
     * rebuilt only when the producer hands over new geometry — one per route
     * revision, compared by identity.
     */
    private var plannedLayers: List<Layer> = emptyList()
    private var builtPlannedRoute: MapsforgePlannedRoute? = null

    fun render(
        map: MapsforgeMap,
        mapView: MapView,
        points: List<ExerciseRoutePoint>,
        routeBreakIndexes: List<Int>,
        currentPoint: ExerciseRoutePoint?,
        plannedRoute: MapsforgePlannedRoute? = null,
        headingDegrees: Float? = null,
    ) {
        updateRouteLayers(mapView, points, routeBreakIndexes, currentPoint, plannedRoute, headingDegrees)
        updateCamera(map, points, currentPoint)
    }

    fun onUserPan() {
        followCurrentPoint = false
    }

    fun recenter(
        map: MapsforgeMap,
        points: List<ExerciseRoutePoint>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        val livePoint = currentPoint?.takeIf { it.hasFiniteCoordinates() }
        if (livePoint != null) {
            followCurrentPoint = true
            followedPoint = null
            followCamera(map, livePoint)
        } else {
            fitMapsforgeCamera(map, points, currentPoint)
        }
        didFitInitialCamera = true
    }

    private fun updateRouteLayers(
        mapView: MapView,
        points: List<ExerciseRoutePoint>,
        routeBreakIndexes: List<Int>,
        currentPoint: ExerciseRoutePoint?,
        plannedRoute: MapsforgePlannedRoute?,
        headingDegrees: Float?,
    ) {
        val layers = mapView.getLayerManager().getLayers()
        if (plannedRoute !== builtPlannedRoute) {
            plannedLayers.forEach { layer ->
                layers.remove(layer)
                layer.onDestroy()
            }
            plannedLayers = plannedRoute
                ?.takeIf { it.points.size >= 2 }
                ?.let(::buildPlannedRouteLayers)
                .orEmpty()
            builtPlannedRoute = plannedRoute
        }
        routeLayers.forEach { layer ->
            layers.remove(layer)
            layer.onDestroy()
        }
        // Re-adding the track after the planned layers keeps the record drawn
        // over the plan it is following.
        plannedLayers.forEach { layer ->
            layers.remove(layer)
            layers.add(layer)
        }
        routeLayers = buildMapsforgeRouteLayers(points, routeBreakIndexes, currentPoint, headingDegrees)
        routeLayers.forEach(layers::add)
        mapView.getLayerManager().redrawLayers()
    }

    private fun updateCamera(
        map: MapsforgeMap,
        points: List<ExerciseRoutePoint>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        val livePoint = currentPoint?.takeIf { it.hasFiniteCoordinates() }
        if (livePoint != null && followCurrentPoint) {
            followCamera(map, livePoint)
        } else {
            fitInitialCamera(map, points, currentPoint)
        }
    }

    /**
     * Keeps the live fix in the middle of the viewport. Only the centre moves:
     * the zoom and rotation the user chose ride along untouched.
     */
    private fun followCamera(map: MapsforgeMap, livePoint: ExerciseRoutePoint) {
        val target = livePoint.toLatLong()
        if (target == followedPoint) return
        followedPoint = target
        if (didFitInitialCamera) {
            map.view.getModel().mapViewPosition.animateTo(target)
        } else {
            map.view.setCenter(target)
            map.view.setZoomLevel(map.zoomRange.clamp(FollowZoom))
            didFitInitialCamera = true
        }
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
    onUserPan: () -> Unit,
): MapsforgeMap {
    require(mapPacks.isNotEmpty()) { "At least one Mapsforge map pack is required." }
    val application = context.applicationContext as? Application
        ?: error("Mapsforge requires an application context.")
    ensureMapsforgeGraphicsFactory(application)

    val mapView = MapView(context)
    mapView.disallowAncestorInterceptDuringTouch(onUserPan)
    mapView.touchGestureHandler.isRotationEnabled = true
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

/**
 * Besides keeping scrolling ancestors from stealing the map's touches, this is
 * where a pan is recognised: mapsforge has no gesture callbacks of its own, so
 * a single finger travelling past the touch slop is the signal. Two-finger
 * gestures — pinch and rotate — deliberately do not count as panning.
 */
@SuppressLint("ClickableViewAccessibility")
private fun View.disallowAncestorInterceptDuringTouch(onUserPan: () -> Unit = {}) {
    val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
    var downX = 0f
    var downY = 0f
    setOnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                view.requestAncestorIntercept(disallow = true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> view.requestAncestorIntercept(disallow = true)
            MotionEvent.ACTION_MOVE -> {
                view.requestAncestorIntercept(disallow = true)
                if (event.pointerCount == 1 &&
                    kotlin.math.hypot(event.x - downX, event.y - downY) > touchSlop
                ) {
                    onUserPan()
                }
            }
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

/** The planned route ready to hand to the layers, built off the UI thread. */
internal class MapsforgePlannedRoute(
    val points: List<LatLong>,
    val arrows: List<PlannedRouteArrow>,
    val destination: LatLong?,
)

/**
 * Every point CoMaps served, drawn the way CoMaps draws its own route: a wide
 * fill in a darker casing, with white arrows pointing out of every bend.
 */
private fun buildPlannedRouteLayers(route: MapsforgePlannedRoute): List<Layer> = buildList {
    // Widths are density-scaled: mapsforge paints are raw pixels, and a
    // "wide" line divided by a Pixel's density is a thin one.
    val scale = DisplayModel.getDeviceScaleFactor()
    add(
        Polyline(
            routePaint(PlannedRouteCasingColor, PlannedCasingWidthPx * scale),
            AndroidGraphicFactory.INSTANCE,
        ).apply { setPoints(route.points) },
    )
    add(
        Polyline(
            routePaint(PlannedRouteColor, PlannedRouteLineWidthPx * scale),
            AndroidGraphicFactory.INSTANCE,
        ).apply { setPoints(route.points) },
    )
    add(PlannedRouteArrowsLayer(route.arrows))
    route.destination?.let { add(DestinationFlagLayer(it)) }
}

/**
 * The route's bend arrows, one layer for all of them: projected and drawn
 * only inside the viewport, and only at zooms where a bend is a bend and not
 * a pixel.
 */
private class PlannedRouteArrowsLayer(
    private val arrows: List<PlannedRouteArrow>,
) : Layer() {
    private val shaftPaint =
        routePaint(TurnArrowFillColor, TurnArrowShaftWidthPx * DisplayModel.getDeviceScaleFactor())
    private val headPaint = fillPaint(TurnArrowFillColor)

    override fun draw(
        boundingBox: BoundingBox,
        zoomLevel: Byte,
        canvas: org.mapsforge.core.graphics.Canvas,
        topLeftPoint: Point,
        rotation: Rotation,
    ) {
        if (zoomLevel < MinTurnArrowZoom) return
        val mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.tileSize)
        val headSize = TurnArrowHeadSizePx * displayModel.scaleFactor
        var lastX = Float.NEGATIVE_INFINITY
        var lastY = Float.NEGATIVE_INFINITY
        arrows.forEach { arrow ->
            val cornerLat = arrow.shaft[2]
            val cornerLon = arrow.shaft[3]
            if (cornerLat < boundingBox.minLatitude || cornerLat > boundingBox.maxLatitude ||
                cornerLon < boundingBox.minLongitude || cornerLon > boundingBox.maxLongitude
            ) {
                return@forEach
            }

            fun pixelX(lon: Double): Float =
                (MercatorProjection.longitudeToPixelX(lon, mapSize) - topLeftPoint.x).toFloat()

            fun pixelY(lat: Double): Float =
                (MercatorProjection.latitudeToPixelY(lat, mapSize) - topLeftPoint.y).toFloat()

            val cornerX = pixelX(cornerLon)
            val cornerY = pixelY(cornerLat)
            // Poor man's collision detection: two bends of a switchback do
            // not both need an arrow at this zoom.
            if (kotlin.math.hypot(cornerX - lastX, cornerY - lastY) < headSize * 2.5f) return@forEach
            lastX = cornerX
            lastY = cornerY

            // The shaft, bent along the route through the bend.
            val shaft = AndroidGraphicFactory.INSTANCE.createPath()
            shaft.moveTo(pixelX(arrow.shaft[1]), pixelY(arrow.shaft[0]))
            shaft.lineTo(cornerX, cornerY)
            shaft.lineTo(pixelX(arrow.shaft[5]), pixelY(arrow.shaft[4]))
            canvas.drawPath(shaft, shaftPaint)
            // And the head, at the tip of the exit arm, pointing the way on.
            drawMapsforgeArrowHead(
                canvas,
                pixelX(arrow.headLongitude), pixelY(arrow.headLatitude),
                arrow.bearingDegrees, headSize, headPaint,
            )
        }
    }
}

/** CoMaps' destination: a pennant on a pole, its base on the point itself. */
private class DestinationFlagLayer(private val position: LatLong) : Layer() {
    private val pole = strokePaint(DestinationPoleColor, 5.0f * DisplayModel.getDeviceScaleFactor())
    private val pennantFill = fillPaint(DestinationFlagColor)
    private val pennantStroke = strokePaint("#FFFFFF", 2.5f * DisplayModel.getDeviceScaleFactor())

    override fun draw(
        boundingBox: BoundingBox,
        zoomLevel: Byte,
        canvas: org.mapsforge.core.graphics.Canvas,
        topLeftPoint: Point,
        rotation: Rotation,
    ) {
        if (!boundingBox.contains(position)) return
        val mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.tileSize)
        val x = (MercatorProjection.longitudeToPixelX(position.longitude, mapSize) - topLeftPoint.x).toFloat()
        val y = (MercatorProjection.latitudeToPixelY(position.latitude, mapSize) - topLeftPoint.y).toFloat()
        val height = DestinationFlagHeightPx * displayModel.scaleFactor
        // A billboard, the way mapsforge's own Marker does it: the flag stays
        // upright on a rotated map by counter-rotating the canvas around its
        // base for the duration of the drawing.
        if (!Rotation.noRotation(rotation)) {
            canvas.rotate(Rotation(-rotation.degrees, x, y))
        }
        canvas.drawLine(x.toInt(), y.toInt(), x.toInt(), (y - height).toInt(), pole)
        val pennant = AndroidGraphicFactory.INSTANCE.createPath()
        pennant.moveTo(x, y - height)
        pennant.lineTo(x + height * 0.62f, y - height * 0.81f)
        pennant.lineTo(x, y - height * 0.62f)
        pennant.close()
        canvas.drawPath(pennant, pennantFill)
        canvas.drawPath(pennant, pennantStroke)
        if (!Rotation.noRotation(rotation)) {
            canvas.rotate(Rotation(rotation.degrees, x, y))
        }
    }
}

/** The phone's chevron at the current fix, pointing where the phone points. */
private class DeviceHeadingLayer(
    private val position: LatLong,
    private val bearingDegrees: Float,
) : Layer() {
    private val fill = fillPaint(CurrentLocationColor)
    private val stroke = strokePaint(
        MarkerStrokeColor,
        HeadingStrokeWidthPx * DisplayModel.getDeviceScaleFactor(),
    )

    override fun draw(
        boundingBox: BoundingBox,
        zoomLevel: Byte,
        canvas: org.mapsforge.core.graphics.Canvas,
        topLeftPoint: Point,
        rotation: Rotation,
    ) {
        if (!boundingBox.contains(position)) return
        val mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.tileSize)
        val x = (MercatorProjection.longitudeToPixelX(position.longitude, mapSize) - topLeftPoint.x).toFloat()
        val y = (MercatorProjection.latitudeToPixelY(position.latitude, mapSize) - topLeftPoint.y).toFloat()
        drawMapsforgeChevron(
            canvas, x, y, bearingDegrees,
            HeadingArrowSizePx * displayModel.scaleFactor, fill, stroke,
        )
    }
}

/** A solid triangular head centred on (x, y), rotated to a bearing. */
private fun drawMapsforgeArrowHead(
    canvas: org.mapsforge.core.graphics.Canvas,
    centerX: Float,
    centerY: Float,
    bearingDegrees: Float,
    sizePx: Float,
    fill: Paint,
) {
    val theta = Math.toRadians(bearingDegrees.toDouble())
    val cosTheta = kotlin.math.cos(theta).toFloat()
    val sinTheta = kotlin.math.sin(theta).toFloat()

    fun rotatedX(x: Float, y: Float): Float = centerX + x * cosTheta - y * sinTheta
    fun rotatedY(x: Float, y: Float): Float = centerY + x * sinTheta + y * cosTheta

    val tipY = -0.55f * sizePx
    val baseX = 0.45f * sizePx
    val baseY = 0.4f * sizePx
    val path = AndroidGraphicFactory.INSTANCE.createPath()
    path.moveTo(rotatedX(0f, tipY), rotatedY(0f, tipY))
    path.lineTo(rotatedX(baseX, baseY), rotatedY(baseX, baseY))
    path.lineTo(rotatedX(-baseX, baseY), rotatedY(-baseX, baseY))
    path.close()
    canvas.drawPath(path, fill)
}

/**
 * A chevron centred on (x, y), rotated to a bearing by rotating its model
 * points — no canvas transform, so nothing else on the frame is disturbed.
 */
private fun drawMapsforgeChevron(
    canvas: org.mapsforge.core.graphics.Canvas,
    centerX: Float,
    centerY: Float,
    bearingDegrees: Float,
    sizePx: Float,
    fill: Paint,
    stroke: Paint,
) {
    val theta = Math.toRadians(bearingDegrees.toDouble())
    val cosTheta = kotlin.math.cos(theta).toFloat()
    val sinTheta = kotlin.math.sin(theta).toFloat()

    fun rotatedX(x: Float, y: Float): Float = centerX + x * cosTheta - y * sinTheta
    fun rotatedY(x: Float, y: Float): Float = centerY + x * sinTheta + y * cosTheta

    val tipY = -0.46f * sizePx
    val wingX = 0.37f * sizePx
    val wingY = 0.42f * sizePx
    val notchY = 0.2f * sizePx
    val path = AndroidGraphicFactory.INSTANCE.createPath()
    path.moveTo(rotatedX(0f, tipY), rotatedY(0f, tipY))
    path.lineTo(rotatedX(wingX, wingY), rotatedY(wingX, wingY))
    path.lineTo(rotatedX(0f, notchY), rotatedY(0f, notchY))
    path.lineTo(rotatedX(-wingX, wingY), rotatedY(-wingX, wingY))
    path.close()
    canvas.drawPath(path, fill)
    canvas.drawPath(path, stroke)
}

private fun buildMapsforgeRouteLayers(
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
    currentPoint: ExerciseRoutePoint?,
    headingDegrees: Float? = null,
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
            // A phone that knows which way it faces shows it; one that does
            // not falls back to the dot.
            if (headingDegrees != null) {
                add(DeviceHeadingLayer(point.toLatLong(), headingDegrees))
            } else {
                add(markerCircle(point, CurrentLocationColor, CurrentLocationRadiusPx))
            }
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
// Route blue, not guidance green: the green vanished into park and
// land-use fills. Blue is the one family both base styles reserve for
// water and little else along a street.
private const val PlannedRouteColor = "#1E88E5"
private const val PlannedRouteCasingColor = "#1256A0"
private const val TurnArrowFillColor = "#FFFFFF"
private const val DestinationFlagColor = "#D32F2F"
private const val DestinationPoleColor = "#37474F"
private const val StartMarkerColor = "#1F9D55"
private const val EndMarkerColor = "#6B5DD3"
private const val CurrentLocationColor = "#1D4ED8"
private const val MarkerStrokeColor = "#FFFFFF"
private const val RouteLineWidthPx = 8.0f
private const val PlannedRouteLineWidthPx = 10.0f
private const val PlannedCasingWidthPx = 14.0f
private const val TurnArrowShaftWidthPx = 4.0f
private const val TurnArrowHeadSizePx = 9.0f
private const val HeadingArrowSizePx = 34.0f
private const val HeadingStrokeWidthPx = 3.0f
private const val MinTurnArrowZoom: Byte = 13
private const val DestinationFlagHeightPx = 34.0f
private const val MarkerRadiusPx = 7.0f
private const val CurrentLocationRadiusPx = 8.0f
private const val MarkerStrokeWidthPx = 3.0f
private const val DefaultMapsforgeZoom: Byte = 12

/** The first follow frame's zoom: street level, clamped to what the pack holds. */
private const val FollowZoom: Byte = 16

/** Below this the map counts as facing north and the reset control hides. */
private const val NorthUpToleranceDegrees = 0.5f
