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
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.activity.RoutePreview

@Composable
internal fun OfflineRouteMapOrPreview(
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int> = emptyList(),
    currentPoint: ExerciseRoutePoint? = null,
    showRecenterControl: Boolean = false,
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
                modifier = modifier,
            )
            OfflineMapPackFormat.MAPSFORGE -> MapsforgeRouteMap(
                mapPacks = activeMapPacks,
                points = points,
                routeBreakIndexes = routeBreakIndexes,
                currentPoint = currentPoint,
                showRecenterControl = showRecenterControl,
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
    modifier: Modifier = Modifier,
) {
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
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = true
                mapLibreMap = map
                renderState.render(
                    context = context,
                    map = map,
                    mapPacks = mapPacks,
                    points = points,
                    routeBreakIndexes = routeBreakIndexes,
                    currentPoint = currentPoint,
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

    fun render(
        context: Context,
        map: MapLibreMap,
        mapPacks: List<OfflineMapPack>,
        points: List<ExerciseRoutePoint>,
        routeBreakIndexes: List<Int>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        val styleKey = mapPacks.joinToString(separator = "|") { pack -> "${pack.id}:${pack.path}" }
        if (loadedStyleKey != styleKey) {
            loadedStyleKey = styleKey
            didFitInitialCamera = false
            val styleJson = context.offlineMapStyleJson(mapPacks)
            map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                updateStyle(style, points, routeBreakIndexes, currentPoint)
                fitInitialCamera(map, points, currentPoint)
            }
        } else {
            map.getStyle { style ->
                updateStyle(style, points, routeBreakIndexes, currentPoint)
                fitInitialCamera(map, points, currentPoint)
            }
        }
    }

    fun recenter(
        map: MapLibreMap,
        points: List<ExerciseRoutePoint>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        fitCamera(map, points, currentPoint)
        didFitInitialCamera = true
    }

    private fun updateStyle(
        style: Style,
        points: List<ExerciseRoutePoint>,
        routeBreakIndexes: List<Int>,
        currentPoint: ExerciseRoutePoint?,
    ) {
        ensureRouteSources(style)
        ensureRouteLayers(style)
        style.getSourceAs<GeoJsonSource>(RouteSourceId)
            ?.setGeoJson(routeLineFeatureCollection(points, routeBreakIndexes))
        style.getSourceAs<GeoJsonSource>(StartSourceId)
            ?.setGeoJson(pointFeatureCollection(points.firstOrNull()))
        style.getSourceAs<GeoJsonSource>(EndSourceId)
            ?.setGeoJson(pointFeatureCollection(points.lastOrNull()))
        style.getSourceAs<GeoJsonSource>(CurrentLocationSourceId)
            ?.setGeoJson(pointFeatureCollection(currentPoint))
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

private fun ensureRouteSources(style: Style) {
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
    val sourceIds = mapPacks.mapIndexed { index, _ -> "$PmtilesSourceIdPrefix$index" }
    return buildJsonObject {
        root.forEach { (key, value) ->
            when (key) {
                SourcesKey -> put(SourcesKey, mapPacks.toPmtilesSources(sourceIds))
                LayersKey -> put(LayersKey, root[LayersKey]?.jsonArray?.toExpandedPmtilesLayers(sourceIds) ?: JsonArray(emptyList()))
                else -> put(key, value)
            }
        }
    }.toString()
}

private fun List<OfflineMapPack>.toPmtilesSources(sourceIds: List<String>): JsonObject =
    buildJsonObject {
        forEachIndexed { index, pack ->
            put(
                sourceIds[index],
                buildJsonObject {
                    put("type", "vector")
                    put("url", "pmtiles://${Uri.fromFile(File(pack.path))}")
                    put("attribution", "© OpenStreetMap contributors, Protomaps")
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
private const val RouteSourceId = "openvitals-route"
private const val StartSourceId = "openvitals-route-start"
private const val EndSourceId = "openvitals-route-end"
private const val CurrentLocationSourceId = "openvitals-current-location"
private const val RouteLayerId = "openvitals-route-line"
private const val StartLayerId = "openvitals-route-start"
private const val EndLayerId = "openvitals-route-end"
private const val CurrentLocationLayerId = "openvitals-current-location"
private const val RouteLineColor = "#D9462F"
private const val StartMarkerColor = "#1F9D55"
private const val EndMarkerColor = "#6B5DD3"
private const val CurrentLocationColor = "#1D4ED8"
private const val MarkerStrokeColor = "#FFFFFF"
private const val SinglePointZoom = 15.5
private const val CameraPaddingPx = 64
