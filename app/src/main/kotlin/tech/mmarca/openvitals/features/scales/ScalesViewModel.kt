package tech.mmarca.openvitals.features.scales

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.permissions.OsPermissionsService
import tech.mmarca.openvitals.data.repository.contract.ScaleWeighInRepository
import tech.mmarca.openvitals.devices.core.pairing.CompanionDevice
import tech.mmarca.openvitals.devices.xiaomi.ScaleListenerStatus
import tech.mmarca.openvitals.devices.xiaomi.XiaomiScaleListener
import tech.mmarca.openvitals.devices.xiaomi.XiaomiScaleStore
import tech.mmarca.openvitals.domain.model.OsPermissionCatalog

/** The scale as its card shows it. */
@Immutable
data class ScaleCardState(
    val name: String,
    val status: ScaleListenerStatus,
    /** Android wakes the app when someone steps on, so "listening" holds with the app closed. */
    val wokenBySystem: Boolean,
    val lastWeighInAt: Instant?,
)

/** Where the add dialog is: finding the scale through Android, or entering its key. */
enum class AddScaleStep { FIND, KEY }

@Immutable
data class ScalesUiState(
    /** Null until a scale is set up. One scale: the add button goes with it. */
    val scale: ScaleCardState? = null,
    val osPermissions: OsPermissionCatalog = OsPermissionCatalog(),
    val showPermissionsGate: Boolean = false,
    val showAddFlow: Boolean = false,
    val addStep: AddScaleStep = AddScaleStep.FIND,
    /** Android's dialog is open, or its scan is running. */
    val isFinding: Boolean = false,
    /** The last Find ended with no scale: asleep, out of range, or declined. */
    val findFailed: Boolean = false,
    /** What Android found; the key step shows and names it. */
    val found: CompanionDevice? = null,
    val nameInput: String = "",
    val keyInput: String = "",
    val keyInputInvalid: Boolean = false,
)

/** What the screen holds that no store does: the gate and the add dialog. */
private data class ScalesLocalState(
    val osPermissions: OsPermissionCatalog = OsPermissionCatalog(),
    val showPermissionsGate: Boolean = false,
    val showAddFlow: Boolean = false,
    val addStep: AddScaleStep = AddScaleStep.FIND,
    val isFinding: Boolean = false,
    val findFailed: Boolean = false,
    val found: CompanionDevice? = null,
    val nameInput: String = "",
    val keyInput: String = "",
    val keyInputInvalid: Boolean = false,
    val wokenBySystem: Boolean = false,
)

/**
 * The Scales screen: the scale's card and the add flow. Adding goes through
 * Android's companion dialog first, which names the scale and lets Android
 * wake the app for it, then asks for the key. Everything about a scale that
 * is set up lives in [ScaleDeviceViewModel].
 */
@HiltViewModel
class ScalesViewModel @Inject constructor(
    private val store: XiaomiScaleStore,
    private val listener: XiaomiScaleListener,
    weighIns: ScaleWeighInRepository,
    private val osPermissionsService: OsPermissionsService,
) : ViewModel() {

    private val local = MutableStateFlow(ScalesLocalState(osPermissions = osPermissionsService.scaleSetupCatalog()))

    val uiState: StateFlow<ScalesUiState> = combine(
        store.config,
        listener.status,
        weighIns.latest,
        local,
    ) { config, status, latest, local ->
        ScalesUiState(
            scale = if (config.isSetUp) {
                ScaleCardState(
                    name = config.name ?: config.address.orEmpty(),
                    status = status,
                    wokenBySystem = local.wokenBySystem,
                    lastWeighInAt = latest?.time,
                )
            } else {
                null
            },
            osPermissions = local.osPermissions,
            showPermissionsGate = local.showPermissionsGate,
            showAddFlow = local.showAddFlow,
            addStep = local.addStep,
            isFinding = local.isFinding,
            findFailed = local.findFailed,
            found = local.found,
            nameInput = local.nameInput,
            keyInput = local.keyInput,
            keyInputInvalid = local.keyInputInvalid,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScalesUiState())

    /** On every return to the screen: a fresh, fast scan while it is open, another try at pending writes. */
    fun refresh() {
        refreshOsPermissions()
        if (!store.config.value.isSetUp) return
        listener.arm(restart = true)
        listener.retryPending()
        local.update { it.copy(wokenBySystem = listener.isWokenBySystem()) }
    }

    fun refreshOsPermissions() {
        local.update { it.copy(osPermissions = osPermissionsService.scaleSetupCatalog()) }
    }

    /** The add button's entry point. A missing grant gets the checklist first. */
    fun startAdd() {
        val catalog = osPermissionsService.scaleSetupCatalog()
        local.update { it.copy(osPermissions = catalog) }
        if (catalog.allGranted) openAddFlow() else local.update { it.copy(showPermissionsGate = true) }
    }

    fun dismissPermissionsGate() {
        local.update { it.copy(showPermissionsGate = false) }
    }

    fun openAddFlow() {
        local.update {
            it.copy(
                showPermissionsGate = false,
                showAddFlow = true,
                addStep = AddScaleStep.FIND,
                isFinding = false,
                findFailed = false,
                found = null,
                nameInput = "",
                keyInput = "",
                keyInputInvalid = false,
            )
        }
    }

    fun closeAddFlow() {
        if (local.value.isFinding) return
        local.update { it.copy(showAddFlow = false, found = null, keyInput = "") }
    }

    /** Opens Android's dialog. The scale has to be awake: the dialog lists only what it hears. */
    fun findScale() {
        if (local.value.isFinding) return
        local.update { it.copy(isFinding = true, findFailed = false) }
        viewModelScope.launch {
            val found = listener.findScale()
            local.update {
                it.copy(
                    isFinding = false,
                    findFailed = found == null,
                    found = found,
                    addStep = if (found != null) AddScaleStep.KEY else AddScaleStep.FIND,
                    nameInput = found?.name ?: found?.address ?: it.nameInput,
                )
            }
        }
    }

    fun onNameInputChange(text: String) {
        local.update { it.copy(nameInput = text) }
    }

    fun onKeyInputChange(text: String) {
        local.update { it.copy(keyInput = text, keyInputInvalid = false) }
    }

    /** Adds the found scale with the key. Nothing is saved while the key does not parse. */
    fun saveScale() {
        val state = local.value
        val found = state.found ?: return
        val key = XiaomiScaleStore.parseBindKey(state.keyInput)
        if (key == null) {
            local.update { it.copy(keyInputInvalid = true) }
            return
        }
        listener.setUp(found, name = state.nameInput.trim().ifBlank { found.name ?: found.address }, key = key)
        local.update {
            it.copy(showAddFlow = false, found = null, keyInput = "", wokenBySystem = listener.isWokenBySystem())
        }
    }
}
