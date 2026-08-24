package tech.mmarca.openvitals.features.activity.maps

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.gestures.MoveGestureDetector
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.activity.RoutePreview

@Composable
internal fun OfflineRouteMapOrPreview(
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int> = emptyList(),
    currentPoint: ExerciseRoutePoint? = null,
    showRecenterControl: Boolean = false,
    plannedRoute: CoMapsRoutePolyline? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            OfflineMapUiEntryPoint::class.java,
        ).offlineMapRepository()
    }
    val mapState by repository.state.collectAsStateWithLifecycle()
    val activeFormat = mapState.activeFormat
    val activeMapPacks = mapState.activeMapPacks.filter { it.file.exists() }

    if (activeFormat != null && activeMapPacks.isNotEmpty()) {
        when (activeFormat) {
            OfflineMapPackFormat.PMTILES -> MapLibreRouteMap(
                mapPacks = activeMapPacks,
                points = points,
                routeBreakIndexes = routeBreakIndexes,
                currentPoint = currentPoint,
                showRecenterControl = showRecenterControl,
                plannedRoute = plannedRoute,
                modifier = modifier,
            )
            OfflineMapPackFormat.MAPSFORGE -> MapsforgeRouteMap(
                mapPacks = activeMapPacks,
                points = points,
                routeBreakIndexes = routeBreakIndexes,
                currentPoint = currentPoint,
                showRecenterControl = showRecenterControl,
                plannedRoute = plannedRoute,
                modifier = modifier,
            )
        }
    } else {
        RoutePreview(
            points = points,
            routeBreakIndexes = routeBreakIndexes,
            modifier = modifier,
        )
    }
}

@Composable
private fun MapLibreRouteMap(
    mapPacks: List<OfflineMapPack>,
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
    currentPoint: ExerciseRoutePoint?,
    showRecenterControl: Boolean,
    plannedRoute: CoMapsRoutePolyline?,
    modifier: Modifier = Modifier,
) {
    // The polyline-to-geojson conversion walks six figures of points for a
    // cross-country route. Done here, once per route revision on a worker
    // dispatcher, the style callback only ever hands the map finished
    // objects — the UI thread never carries the route, however long it is.
    val plannedRouteDisplay by produceState(
        initialValue = PlannedRouteDisplay.Empty,
        plannedRoute,
    ) {
        value = withContext(Dispatchers.Default) {
            plannedRouteTurnArrows(plannedRoute).let { arrows ->
                PlannedRouteDisplay(
                    line = plannedRouteFeatureCollection(plannedRoute),
                    arrowShafts = plannedRouteArrowShaftFeatureCollection(arrows),
                    arrowHeads = plannedRouteArrowHeadFeatureCollection(arrows),
                    destination = destinationFeatureCollection(plannedRoute?.destination),
                )
            }
        }
    }
    val headingDegrees by rememberDeviceHeadingDegrees(enabled = currentPoint != null)
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val renderState = remember { OfflineRouteMapRenderState() }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    val recenterDescription = stringResource(R.string.cd_recenter_map)

    val mapView = remember(context) {
        initializeOfflineMapLibre(context.applicationContext)
        val options = MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true)
        MapView(context, options).apply {
            disallowAncestorInterceptDuringTouch()
            onCreate(Bundle())
            getMapAsync { map ->
                // The compass doubles as the rotation reset: it appears once the
                // map is turned off north and a tap turns it back.
                map.uiSettings.isCompassEnabled = true
                map.uiSettings.isRotateGesturesEnabled = true
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = true
                // A pan is the user taking the wheel: the camera stops chasing
                // the fix until the recenter button hands it back. Zoom and
                // rotate gestures are not moves, so they leave following alone.
                map.addOnMoveListener(object : MapLibreMap.OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) = renderState.onUserPan()
                    override fun onMove(detector: MoveGestureDetector) = Unit
                    override fun onMoveEnd(detector: MoveGestureDetector) = Unit
                })
                mapLibreMap = map
                renderState.render(
                    context = context,
                    map = map,
                    mapPacks = mapPacks,
                    points = points,
                    routeBreakIndexes = routeBreakIndexes,
                    currentPoint = currentPoint,
                    plannedRouteDisplay = plannedRouteDisplay,
                    headingDegrees = headingDegrees,
                )
            }
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            update = { view ->
                view.getMapAsync { map ->
                    mapLibreMap = map
                    renderState.render(
                        context = context,
                        map = map,
                        mapPacks = mapPacks,
                        points = points,
                        routeBreakIndexes = routeBreakIndexes,
                        currentPoint = currentPoint,
                        plannedRouteDisplay = plannedRouteDisplay,
                        headingDegrees = headingDegrees,
                    )
                }
            },
        )

        if (showRecenterControl) {
            FloatingActionButton(
                onClick = {
                    mapLibreMap?.let { map ->
                        renderState.recenter(map, points, currentPoint)
                    }
                },
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

private fun initializeOfflineMapLibre(applicationContext: Context) {
    MapLibre.getInstance(applicationContext)
    // The app intentionally removes ACCESS_NETWORK_STATE. Pinning MapLibre to
    // offline mode keeps its connectivity receiver from calling ConnectivityManager.
    MapLibre.setConnected(false)
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

private class OfflineRouteMapRenderState {
    private var loadedStyleKey: String? = null
    private var didFitInitialCamera = false

    /**
     * Whether the camera tracks the live fix. On from the first frame — a map
     * with a current point is a recording in progress, and a recording map's
     * job is to keep the user in the middle of it — and off the moment the
     * user pans away, until the recenter button re-engages it. Maps without a
     * live fix (a recorded activity's route) never enter the follow path.
     */
    private var followCurrentPoint = true
    private var followedPoint: LatLng? = null

    /**
     * The planned-route display last written into the style, compared by
     * identity: the producer emits one new object per route revision, and
     * updateStyle runs on every recomposition tick.
     */
    private var writtenPlannedRouteDisplay: PlannedRouteDisplay? = null

    fun render(
        context: Context,
        map: MapLibreMap,
        mapPacks: List<OfflineMapPack>,
        points: List<ExerciseRoutePoint>,
        routeBreakIndexes: List<Int>,
        currentPoint: ExerciseRoutePoint?,
        plannedRouteDisplay: PlannedRouteDisplay,
        headingDegrees: Float?,
    ) {
        val styleKey = mapPacks.joinToString(separator = "|") { pack -> "${pack.id}:${pack.path}" }
        if (loadedStyleKey != styleKey) {
            loadedStyleKey = styleKey
            didFitInitialCamera = false
            val styleJson = context.offlineMapStyleJson(mapPacks)
            writtenPlannedRouteDisplay = null
            map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                updateStyle(style, points, routeBreakIndexes, currentPoint, plannedRouteDisplay, headingDegrees)
                updateCamera(map, points, currentPoint)
            }
        } else {
            map.getStyle { style ->
                updateStyle(style, points, routeBreakIndexes, currentPoint, plannedRouteDisplay, headingDegrees)
                updateCamera(map, points, currentPoint)
            }
        }
    }

    fun onUserPan() {
        followCurrentPoint = false
    }

    fun recenter(
        map: MapLibreMap,
        points: List<ExerciseRoutePoint>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        val livePoint = currentPoint?.takeIf { it.hasFiniteCoordinates() }
        if (livePoint != null) {
            followCurrentPoint = true
            followedPoint = null
            followCamera(map, livePoint)
        } else {
            fitCamera(map, points, currentPoint)
        }
        didFitInitialCamera = true
    }

    private fun updateStyle(
        style: Style,
        points: List<ExerciseRoutePoint>,
        routeBreakIndexes: List<Int>,
        currentPoint: ExerciseRoutePoint?,
        plannedRouteDisplay: PlannedRouteDisplay,
        headingDegrees: Float?,
    ) {
        ensureRouteImages(style)
        ensureRouteSources(style)
        ensureRouteLayers(style)
        if (plannedRouteDisplay !== writtenPlannedRouteDisplay) {
            style.getSourceAs<GeoJsonSource>(PlannedRouteSourceId)
                ?.setGeoJson(plannedRouteDisplay.line)
            style.getSourceAs<GeoJsonSource>(PlannedArrowShaftsSourceId)
                ?.setGeoJson(plannedRouteDisplay.arrowShafts)
            style.getSourceAs<GeoJsonSource>(PlannedArrowsSourceId)
                ?.setGeoJson(plannedRouteDisplay.arrowHeads)
            style.getSourceAs<GeoJsonSource>(DestinationSourceId)
                ?.setGeoJson(plannedRouteDisplay.destination)
            writtenPlannedRouteDisplay = plannedRouteDisplay
        }
        style.getSourceAs<GeoJsonSource>(RouteSourceId)
            ?.setGeoJson(routeLineFeatureCollection(points, routeBreakIndexes))
        style.getSourceAs<GeoJsonSource>(StartSourceId)
            ?.setGeoJson(pointFeatureCollection(points.firstOrNull()))
        style.getSourceAs<GeoJsonSource>(EndSourceId)
            ?.setGeoJson(pointFeatureCollection(points.lastOrNull()))
        style.getSourceAs<GeoJsonSource>(CurrentLocationSourceId)
            ?.setGeoJson(pointFeatureCollection(currentPoint))
        // The dot knows where you are; the arrow also knows which way you
        // face. Only one speaks at a time.
        val headingVisible = currentPoint != null && headingDegrees != null
        style.getSourceAs<GeoJsonSource>(HeadingSourceId)
            ?.setGeoJson(headingFeatureCollection(currentPoint, headingDegrees))
        style.getLayer(CurrentLocationLayerId)?.setProperties(
            visibility(if (headingVisible) Property.NONE else Property.VISIBLE),
        )
        style.getLayer(HeadingLayerId)?.setProperties(
            visibility(if (headingVisible) Property.VISIBLE else Property.NONE),
        )
    }

    private fun updateCamera(
        map: MapLibreMap,
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
     * Keeps the live fix in the middle of the viewport. Only the target moves:
     * the zoom and bearing the user chose ride along untouched.
     */
    private fun followCamera(map: MapLibreMap, livePoint: ExerciseRoutePoint) {
        val target = LatLng(livePoint.latitude, livePoint.longitude)
        if (target == followedPoint) return
        followedPoint = target
        if (didFitInitialCamera) {
            map.easeCamera(CameraUpdateFactory.newLatLng(target))
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, SinglePointZoom))
            didFitInitialCamera = true
        }
    }

    private fun fitInitialCamera(
        map: MapLibreMap,
        points: List<ExerciseRoutePoint>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        if (didFitInitialCamera) return
        if (points.isEmpty() && currentPoint == null) return
        fitCamera(map, points, currentPoint)
        didFitInitialCamera = true
    }
}

private fun ExerciseRoutePoint.hasFiniteCoordinates(): Boolean =
    latitude.isFinite() && longitude.isFinite()

private fun ensureRouteSources(style: Style) {
    if (style.getSource(PlannedRouteSourceId) == null) {
        style.addSource(GeoJsonSource(PlannedRouteSourceId, plannedRouteFeatureCollection(null)))
    }
    if (style.getSource(PlannedArrowShaftsSourceId) == null) {
        style.addSource(
            GeoJsonSource(
                PlannedArrowShaftsSourceId,
                plannedRouteArrowShaftFeatureCollection(emptyList()),
            ),
        )
    }
    if (style.getSource(PlannedArrowsSourceId) == null) {
        style.addSource(
            GeoJsonSource(
                PlannedArrowsSourceId,
                plannedRouteArrowHeadFeatureCollection(emptyList()),
            ),
        )
    }
    if (style.getSource(HeadingSourceId) == null) {
        style.addSource(GeoJsonSource(HeadingSourceId, headingFeatureCollection(null, null)))
    }
    if (style.getSource(DestinationSourceId) == null) {
        style.addSource(GeoJsonSource(DestinationSourceId, destinationFeatureCollection(null)))
    }
    if (style.getSource(RouteSourceId) == null) {
        style.addSource(GeoJsonSource(RouteSourceId, routeLineFeatureCollection(emptyList(), emptyList())))
    }
    if (style.getSource(StartSourceId) == null) {
        style.addSource(GeoJsonSource(StartSourceId, pointFeatureCollection(null)))
    }
    if (style.getSource(EndSourceId) == null) {
        style.addSource(GeoJsonSource(EndSourceId, pointFeatureCollection(null)))
    }
    if (style.getSource(CurrentLocationSourceId) == null) {
        style.addSource(GeoJsonSource(CurrentLocationSourceId, pointFeatureCollection(null)))
    }
}

private fun ensureRouteLayers(style: Style) {
    if (style.getLayer(RouteLayerId) == null) {
        style.addLayer(
            LineLayer(RouteLayerId, RouteSourceId).withProperties(
                lineColor(RouteLineColor),
                lineOpacity(0.94f),
                lineWidth(4.0f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
    }
    // Under the recorded track: the plan is context, what was actually ridden
    // is the record. Drawn the way CoMaps draws its own route — a wide fill
    // in a darker casing, with white arrows pointing out of every bend.
    if (style.getLayer(PlannedCasingLayerId) == null) {
        style.addLayerBelow(
            LineLayer(PlannedCasingLayerId, PlannedRouteSourceId).withProperties(
                lineColor(PlannedRouteCasingColor),
                lineOpacity(0.9f),
                lineWidth(14.0f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ),
            RouteLayerId,
        )
    }
    if (style.getLayer(PlannedRouteLayerId) == null) {
        style.addLayerBelow(
            LineLayer(PlannedRouteLayerId, PlannedRouteSourceId).withProperties(
                lineColor(PlannedRouteColor),
                lineOpacity(0.95f),
                lineWidth(10.0f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ),
            RouteLayerId,
        )
    }
    // The manoeuvre arrows, CoMaps-style: a white shaft bent along the route
    // through the bend, ending in a head that points the way on. City zooms
    // only — below that a bend is a pixel.
    if (style.getLayer(PlannedArrowShaftsLayerId) == null) {
        style.addLayerBelow(
            LineLayer(PlannedArrowShaftsLayerId, PlannedArrowShaftsSourceId).withProperties(
                lineColor(TurnArrowColor),
                lineOpacity(1.0f),
                lineWidth(4.0f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ).apply { minZoom = TurnArrowMinZoom },
            RouteLayerId,
        )
    }
    if (style.getLayer(PlannedArrowsLayerId) == null) {
        style.addLayerBelow(
            SymbolLayer(PlannedArrowsLayerId, PlannedArrowsSourceId).withProperties(
                iconImage(RouteArrowIconId),
                iconRotate(Expression.get("bearing")),
                iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                iconSize(0.55f),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
            ).apply { minZoom = TurnArrowMinZoom },
            RouteLayerId,
        )
    }
    if (style.getLayer(StartLayerId) == null) {
        style.addLayer(
            CircleLayer(StartLayerId, StartSourceId).withProperties(
                circleColor(StartMarkerColor),
                circleRadius(6.0f),
                circleStrokeColor(MarkerStrokeColor),
                circleStrokeWidth(2.0f),
            ),
        )
    }
    if (style.getLayer(EndLayerId) == null) {
        style.addLayer(
            CircleLayer(EndLayerId, EndSourceId).withProperties(
                circleColor(EndMarkerColor),
                circleRadius(6.0f),
                circleStrokeColor(MarkerStrokeColor),
                circleStrokeWidth(2.0f),
            ),
        )
    }
    if (style.getLayer(CurrentLocationLayerId) == null) {
        style.addLayer(
            CircleLayer(CurrentLocationLayerId, CurrentLocationSourceId).withProperties(
                circleColor(CurrentLocationColor),
                circleRadius(7.0f),
                circleStrokeColor(MarkerStrokeColor),
                circleStrokeWidth(2.0f),
            ),
        )
    }
    if (style.getLayer(DestinationLayerId) == null) {
        style.addLayer(
            SymbolLayer(DestinationLayerId, DestinationSourceId).withProperties(
                iconImage(DestinationIconId),
                iconSize(1.0f),
                iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
            ),
        )
    }
    if (style.getLayer(HeadingLayerId) == null) {
        style.addLayer(
            SymbolLayer(HeadingLayerId, HeadingSourceId).withProperties(
                iconImage(HeadingIconId),
                iconRotate(Expression.get("bearing")),
                iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                iconSize(1.0f),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
            ),
        )
    }
}

/** The two chevrons, registered once per style: the route's and the phone's. */
private fun ensureRouteImages(style: Style) {
    if (style.getImage(RouteArrowIconId) == null) {
        style.addImage(RouteArrowIconId, arrowHeadBitmap(sizePx = 34))
    }
    if (style.getImage(DestinationIconId) == null) {
        style.addImage(DestinationIconId, destinationFlagBitmap(sizePx = 56))
    }
    if (style.getImage(HeadingIconId) == null) {
        style.addImage(
            HeadingIconId,
            chevronBitmap(
                sizePx = 52,
                fillColor = android.graphics.Color.parseColor(CurrentLocationColor),
                strokeColor = android.graphics.Color.WHITE,
            ),
        )
    }
}

/** A solid triangular arrowhead pointing north; the layer rotates it. */
private fun arrowHeadBitmap(sizePx: Int): android.graphics.Bitmap {
    val bitmap =
        android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val path = android.graphics.Path().apply {
        moveTo(sizePx * 0.5f, sizePx * 0.05f)
        lineTo(sizePx * 0.95f, sizePx * 0.95f)
        lineTo(sizePx * 0.05f, sizePx * 0.95f)
        close()
    }
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    canvas.drawPath(path, paint)
    return bitmap
}

/** A pennant on a pole, its base on the destination itself. */
private fun destinationFlagBitmap(sizePx: Int): android.graphics.Bitmap {
    val bitmap =
        android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = sizePx * 0.08f
    paint.strokeCap = android.graphics.Paint.Cap.ROUND
    paint.color = android.graphics.Color.parseColor(DestinationPoleColor)
    canvas.drawLine(sizePx * 0.32f, sizePx * 0.96f, sizePx * 0.32f, sizePx * 0.08f, paint)
    val pennant = android.graphics.Path().apply {
        moveTo(sizePx * 0.32f, sizePx * 0.1f)
        lineTo(sizePx * 0.92f, sizePx * 0.28f)
        lineTo(sizePx * 0.32f, sizePx * 0.46f)
        close()
    }
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.parseColor(DestinationFlagColor)
    canvas.drawPath(pennant, paint)
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = sizePx * 0.045f
    paint.color = android.graphics.Color.WHITE
    canvas.drawPath(pennant, paint)
    return bitmap
}

/** A north-pointing chevron; the symbol layers rotate it to any bearing. */
private fun chevronBitmap(sizePx: Int, fillColor: Int, strokeColor: Int): android.graphics.Bitmap {
    val bitmap =
        android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val path = android.graphics.Path().apply {
        moveTo(sizePx * 0.5f, sizePx * 0.06f)
        lineTo(sizePx * 0.87f, sizePx * 0.9f)
        lineTo(sizePx * 0.5f, sizePx * 0.68f)
        lineTo(sizePx * 0.13f, sizePx * 0.9f)
        close()
    }
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = fillColor
    canvas.drawPath(path, paint)
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = sizePx * 0.06f
    paint.color = strokeColor
    canvas.drawPath(path, paint)
    return bitmap
}

/** The planned route ready to hand to the style, built off the UI thread. */
private class PlannedRouteDisplay(
    val line: FeatureCollection,
    val arrowShafts: FeatureCollection,
    val arrowHeads: FeatureCollection,
    val destination: FeatureCollection,
) {
    companion object {
        val Empty = PlannedRouteDisplay(
            line = plannedRouteFeatureCollection(null),
            arrowShafts = plannedRouteArrowShaftFeatureCollection(emptyList()),
            arrowHeads = plannedRouteArrowHeadFeatureCollection(emptyList()),
            destination = destinationFeatureCollection(null),
        )
    }
}

private fun fitCamera(
    map: MapLibreMap,
    points: List<ExerciseRoutePoint>,
    currentPoint: ExerciseRoutePoint?,
) {
    val cameraPoints = (points + listOfNotNull(currentPoint))
        .filter { point -> point.latitude.isFinite() && point.longitude.isFinite() }
    when (cameraPoints.size) {
        0 -> Unit
        1 -> {
            val point = cameraPoints.first()
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(point.latitude, point.longitude),
                    SinglePointZoom,
                ),
            )
        }
        else -> {
            val boundsBuilder = LatLngBounds.Builder()
            cameraPoints.forEach { point ->
                boundsBuilder.include(LatLng(point.latitude, point.longitude))
            }
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), CameraPaddingPx),
            )
        }
    }
}

private fun Context.offlineMapStyleJson(mapPacks: List<OfflineMapPack>): String {
    val root = assets.open(OfflineMapStyleAsset).bufferedReader().use { reader ->
        Json.parseToJsonElement(reader.readText()).jsonObject
    }
    return expandPmtilesStyle(
        root = root,
        packFileUrls = mapPacks.map { pack -> Uri.fromFile(File(pack.path)).toString() },
    ).toString()
}

/**
 * The bundled single-source style, rebound onto one source per imported pack.
 *
 * Pure so it can be exercised without an asset manager or an Android `Uri`:
 * [packFileUrls] are the `file://` URLs of the packs, in display order.
 */
internal fun expandPmtilesStyle(root: JsonObject, packFileUrls: List<String>): JsonObject {
    val sourceIds = packFileUrls.mapIndexed { index, _ -> "$PmtilesSourceIdPrefix$index" }
    return buildJsonObject {
        root.forEach { (key, value) ->
            when (key) {
                SourcesKey -> put(SourcesKey, packFileUrls.toPmtilesSources(sourceIds))
                LayersKey -> put(LayersKey, root[LayersKey]?.jsonArray?.toExpandedPmtilesLayers(sourceIds) ?: JsonArray(emptyList()))
                else -> put(key, value)
            }
        }
    }
}

private fun List<String>.toPmtilesSources(sourceIds: List<String>): JsonObject =
    buildJsonObject {
        forEachIndexed { index, packFileUrl ->
            put(
                sourceIds[index],
                buildJsonObject {
                    put("type", "vector")
                    put("url", "pmtiles://$packFileUrl")
                    put("attribution", PmtilesAttribution)
                },
            )
        }
    }

private fun JsonArray.toExpandedPmtilesLayers(sourceIds: List<String>): JsonArray =
    buildJsonArray {
        forEach { element ->
            val layer = element.jsonObject
            if (layer.string(SourceKey) == TemplatePmtilesSourceId) {
                sourceIds.forEachIndexed { index, sourceId ->
                    add(layer.withSource(sourceId, suffix = "-$index"))
                }
            } else {
                add(layer)
            }
        }
    }

private fun JsonObject.withSource(sourceId: String, suffix: String): JsonObject =
    buildJsonObject {
        forEach { (key, value) ->
            when (key) {
                IdKey -> put(IdKey, "${string(IdKey).orEmpty()}$suffix")
                SourceKey -> put(SourceKey, sourceId)
                else -> put(key, value)
            }
        }
    }

private fun JsonObject.string(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OfflineMapUiEntryPoint {
    fun offlineMapRepository(): OfflineMapRepository
}

private const val OfflineMapStyleAsset = "offline_maps/protomaps_base_style.json"
private const val TemplatePmtilesSourceId = "openvitals_pmtiles"
private const val PmtilesSourceIdPrefix = "openvitals_pmtiles_"
private const val SourcesKey = "sources"
private const val LayersKey = "layers"
private const val IdKey = "id"
private const val SourceKey = "source"
internal const val TemplatePmtilesSourceIdForTests = TemplatePmtilesSourceId
internal const val PmtilesAttribution = "© OpenStreetMap contributors, Protomaps"
private const val RouteSourceId = "openvitals-route"
private const val PlannedRouteSourceId = "openvitals-planned-route"
private const val PlannedArrowsSourceId = "openvitals-planned-route-arrows"
private const val PlannedArrowShaftsSourceId = "openvitals-planned-route-arrow-shafts"
private const val HeadingSourceId = "openvitals-heading"
private const val DestinationSourceId = "openvitals-destination"
private const val StartSourceId = "openvitals-route-start"
private const val EndSourceId = "openvitals-route-end"
private const val CurrentLocationSourceId = "openvitals-current-location"
private const val RouteLayerId = "openvitals-route-line"
private const val PlannedRouteLayerId = "openvitals-planned-route-line"
private const val PlannedCasingLayerId = "openvitals-planned-route-casing"
private const val PlannedArrowsLayerId = "openvitals-planned-route-arrows"
private const val PlannedArrowShaftsLayerId = "openvitals-planned-route-arrow-shafts"
private const val HeadingLayerId = "openvitals-heading"
private const val RouteArrowIconId = "openvitals-route-arrow-icon"
private const val HeadingIconId = "openvitals-heading-icon"
private const val DestinationLayerId = "openvitals-destination"
private const val DestinationIconId = "openvitals-destination-icon"
private const val StartLayerId = "openvitals-route-start"
private const val EndLayerId = "openvitals-route-end"
private const val CurrentLocationLayerId = "openvitals-current-location"
private const val RouteLineColor = "#D9462F"
// Route blue, not guidance green: the green vanished into park and
// land-use fills. Blue is the one family both base styles reserve for
// water and little else along a street.
private const val PlannedRouteColor = "#1E88E5"
private const val PlannedRouteCasingColor = "#1256A0"
private const val DestinationFlagColor = "#D32F2F"
private const val TurnArrowColor = "#FFFFFF"
private const val TurnArrowMinZoom = 13f
private const val DestinationPoleColor = "#37474F"
private const val StartMarkerColor = "#1F9D55"
private const val EndMarkerColor = "#6B5DD3"
private const val CurrentLocationColor = "#1D4ED8"
private const val MarkerStrokeColor = "#FFFFFF"
private const val SinglePointZoom = 15.5
private const val CameraPaddingPx = 64
