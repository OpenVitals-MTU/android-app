package tech.mmarca.openvitals.features.watches

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import tech.mmarca.openvitals.core.geo.GeoCoordinateParseResult
import tech.mmarca.openvitals.core.geo.GeoCoordinateParser
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.garmin.GarminCapability
import tech.mmarca.openvitals.devices.garmin.GarminDeviceStateStore
import tech.mmarca.openvitals.devices.garmin.GarminSendFileResult
import tech.mmarca.openvitals.devices.garmin.GarminSendStage
import tech.mmarca.openvitals.devices.garmin.GarminWaypoint
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.navigation.WATCH_DEVICE_ID_ARG
import tech.mmarca.openvitals.navigation.WATCH_POINT_LATITUDE_ARG
import tech.mmarca.openvitals.navigation.WATCH_POINT_LONGITUDE_ARG
import tech.mmarca.openvitals.navigation.WATCH_POINT_NAME_ARG
import tech.mmarca.openvitals.navigation.WATCH_POINT_UNREADABLE_ARG

data class WatchSendPointUiState(
    /** The paired watches that can store a point. */
    val watches: List<BleSensorDevice> = emptyList(),
    val selectedDeviceId: String? = null,
    val name: String = "",
    val coordinates: String = "",
    /** What [coordinates] reads as. Null while the field is empty. */
    val parsed: GeoCoordinateParseResult? = null,
    /** Another app shared something that held no position. */
    val sharedUnreadable: Boolean = false,
    val sendingStage: GarminSendStage? = null,
    val isSending: Boolean = false,
    val result: GarminSendFileResult? = null,
    /** A sync or a find holds the radio. */
    val radioBusy: Boolean = false,
) {
    val position: GeoCoordinateParseResult.Parsed? get() = parsed as? GeoCoordinateParseResult.Parsed

    val canSend: Boolean
        get() = selectedDeviceId != null && position != null && !isSending && !radioBusy
}

/** The send-a-point form. The send itself runs in the controller, so it outlives this screen. */
@HiltViewModel
class WatchSendPointViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    deviceRepository: BleDeviceRepository,
    private val stateStore: GarminDeviceStateStore,
    private val actionsController: GarminWatchActionsController,
    syncController: DeviceSyncController,
) : ViewModel() {

    private data class Form(
        val selectedDeviceId: String?,
        val name: String,
        val coordinates: String,
        val sharedUnreadable: Boolean,
    )

    private val form = MutableStateFlow(
        Form(
            selectedDeviceId = savedStateHandle.get<String>(WATCH_DEVICE_ID_ARG),
            name = savedStateHandle.get<String>(WATCH_POINT_NAME_ARG).orEmpty(),
            coordinates = sharedCoordinates(
                savedStateHandle.get<String>(WATCH_POINT_LATITUDE_ARG)?.toDoubleOrNull(),
                savedStateHandle.get<String>(WATCH_POINT_LONGITUDE_ARG)?.toDoubleOrNull(),
            ),
            sharedUnreadable = savedStateHandle.get<String>(WATCH_POINT_UNREADABLE_ARG) == "true",
        ),
    )

    init {
        // A result from an earlier visit says nothing about this point.
        actionsController.clearPointResult()
    }

    val uiState: StateFlow<WatchSendPointUiState> = combine(
        deviceRepository.devicesFlow,
        form,
        actionsController.pointState,
        actionsController.state,
        syncController.state,
    ) { devices, form, send, find, sync ->
        val watches = devices.filter { it.canStorePoints() }
        // The asked-for watch, else the only one. Several and none asked for: the user picks.
        val selected = form.selectedDeviceId?.takeIf { id -> watches.any { it.id == id } }
            ?: watches.singleOrNull()?.id
        WatchSendPointUiState(
            watches = watches,
            selectedDeviceId = selected,
            name = form.name,
            coordinates = form.coordinates,
            parsed = form.coordinates.takeIf { it.isNotBlank() }?.let(GeoCoordinateParser::parse),
            sharedUnreadable = form.sharedUnreadable,
            sendingStage = send.stage,
            isSending = send.isSending,
            result = send.result,
            radioBusy = sync.isSyncing || find.findingDeviceId != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchSendPointUiState())

    fun selectWatch(deviceId: String) {
        form.update { it.copy(selectedDeviceId = deviceId) }
    }

    fun updateName(name: String) {
        form.update { it.copy(name = name) }
    }

    fun updateCoordinates(coordinates: String) {
        form.update { it.copy(coordinates = coordinates, sharedUnreadable = false) }
        actionsController.clearPointResult()
    }

    fun send() {
        val state = uiState.value
        val deviceId = state.selectedDeviceId ?: return
        val position = state.position ?: return
        if (!state.canSend) return
        val name = state.name.trim().ifEmpty { coordinatesText(position.latitude, position.longitude) }
        actionsController.sendPoint(deviceId, GarminWaypoint(name, position.latitude, position.longitude))
    }

    /** Unknown capabilities count as able: a watch that never synced has no list. */
    private fun BleSensorDevice.canStorePoints(): Boolean {
        if (!enabled || !isGarminGfdi) return false
        val capabilities = stateStore.capabilities(id)
        return capabilities.isEmpty() || GarminCapability.WAYPOINT_TRANSFER in capabilities
    }

    private fun sharedCoordinates(latitude: Double?, longitude: Double?): String =
        if (latitude == null || longitude == null) "" else coordinatesText(latitude, longitude)
}

/** Five decimals is about a metre. A point, never a comma: the parser reads it back. */
internal fun coordinatesText(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
