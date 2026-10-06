package tech.mmarca.openvitals.features.scales

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.ScaleWeighInRepository
import tech.mmarca.openvitals.devices.xiaomi.ScaleListenerStatus
import tech.mmarca.openvitals.devices.xiaomi.ScaleWriteFailure
import tech.mmarca.openvitals.devices.xiaomi.ScaleWeighInWriter
import tech.mmarca.openvitals.devices.xiaomi.ScaleWritePermissions
import tech.mmarca.openvitals.devices.xiaomi.XiaomiScaleListener
import tech.mmarca.openvitals.devices.xiaomi.XiaomiScaleStore
import tech.mmarca.openvitals.domain.insights.BodyCompositionInput
import tech.mmarca.openvitals.domain.model.ScaleWeighIn

/** The last weigh-in, ready to show. A null line was not measured, or could not be estimated. */
@Immutable
data class ScaleWeighInDisplay(
    val time: String,
    val weight: String,
    val heartRate: String?,
    val bodyFat: String?,
    val leanMass: String?,
    val bodyWater: String?,
    /** No impedance arrived, so there is nothing to estimate from. */
    val weightOnly: Boolean,
)

@Immutable
data class ScalesUiState(
    val hasKey: Boolean = false,
    /** The key has opened a weigh-in, so the scale is known. */
    val isBound: Boolean = false,
    val keyRejected: Boolean = false,
    val listenerStatus: ScaleListenerStatus = ScaleListenerStatus.OFF,
    val lastWeighIn: ScaleWeighInDisplay? = null,
    val hasPendingWeighIns: Boolean = false,
    val writeFailure: ScaleWriteFailure? = null,
    /** Health Connect write permissions the scale could use and does not have. */
    val missingWritePermissions: Set<String> = emptySet(),
    val missingProfileInputs: Set<BodyCompositionInput> = emptySet(),
    /** The scale filed a weigh-in under another of its user slots. */
    val otherProfileIgnored: Boolean = false,
    /** Android wakes the app when someone steps on the scale, so it is heard with the app closed. */
    val wokenBySystem: Boolean = false,
    /** The last request for that found no scale, or was declined. */
    val systemWakeFailed: Boolean = false,
    val keyInput: String = "",
    val keyInputInvalid: Boolean = false,
    /** The key field is open over a working key. */
    val replacingKey: Boolean = false,
    val screenError: ScreenError? = null,
) {
    val showKeyField: Boolean
        get() = !hasKey || replacingKey
}

/** What the screen holds that no store does. */
private data class ScalesLocalState(
    val keyInput: String = "",
    val keyInputInvalid: Boolean = false,
    val replacingKey: Boolean = false,
    val missingWritePermissions: Set<String> = emptySet(),
    val missingProfileInputs: Set<BodyCompositionInput> = emptySet(),
    val wokenBySystem: Boolean = false,
    val systemWakeFailed: Boolean = false,
    val screenError: ScreenError? = null,
)

/**
 * The Scales screen: the key, whether the phone is listening, and the last
 * weigh-in. It starts nothing itself: the listening and the saving belong
 * to the device layer and go on without this screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScalesViewModel @Inject constructor(
    private val store: XiaomiScaleStore,
    private val listener: XiaomiScaleListener,
    private val writer: ScaleWeighInWriter,
    weighIns: ScaleWeighInRepository,
    private val healthRepository: HealthRepository,
    private val unitFormatter: UnitFormatter,
    private val dateTimeFormatters: DateTimeFormatterProvider,
) : ViewModel() {

    private val local = MutableStateFlow(ScalesLocalState())
    private var latestWeighIn: ScaleWeighIn? = null

    private val lastWeighIn = weighIns.latest.mapLatest { weighIn ->
        latestWeighIn = weighIn
        weighIn?.toDisplay()
    }

    val uiState: StateFlow<ScalesUiState> = combine(
        store.config,
        listener.status,
        lastWeighIn,
        weighIns.pendingCount,
        local,
    ) { config, status, last, pending, local ->
        ScalesUiState(
            hasKey = config.hasKey,
            isBound = config.isBound,
            keyRejected = config.keyRejected,
            listenerStatus = status,
            lastWeighIn = last,
            hasPendingWeighIns = pending > 0,
            writeFailure = config.writeFailure,
            missingWritePermissions = local.missingWritePermissions,
            missingProfileInputs = local.missingProfileInputs,
            otherProfileIgnored = config.ignoredProfile != null,
            wokenBySystem = local.wokenBySystem,
            systemWakeFailed = local.systemWakeFailed,
            keyInput = local.keyInput,
            keyInputInvalid = local.keyInputInvalid,
            replacingKey = local.replacingKey,
            screenError = local.screenError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScalesUiState())

    /**
     * On every return to the screen: a fresh, fast scan while it is open,
     * another try at what Health Connect has not taken, and the grants and
     * the profile as they stand now.
     */
    fun refresh() {
        if (!store.config.value.hasKey) return
        listener.arm(restart = true)
        listener.retryPending()
        loadGrantsAndProfile()
    }

    private fun loadGrantsAndProfile() {
        viewModelScope.launch {
            val granted = try {
                healthRepository.grantedPermissions()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Health Connect cannot say. The write path reports what it then finds.
                ScaleWritePermissions
            }
            val missingInputs = writer.missingProfileInputs()
            local.update {
                it.copy(
                    missingWritePermissions = ScaleWritePermissions - granted,
                    missingProfileInputs = missingInputs,
                    wokenBySystem = listener.isWokenBySystem(),
                )
            }
        }
    }

    fun onKeyInputChange(text: String) {
        local.update { it.copy(keyInput = text, keyInputInvalid = false) }
    }

    fun saveKey() {
        val key = XiaomiScaleStore.parseBindKey(local.value.keyInput)
        if (key == null) {
            local.update { it.copy(keyInputInvalid = true) }
            return
        }
        listener.useKey(key)
        local.update { it.copy(keyInput = "", keyInputInvalid = false, replacingKey = false) }
        loadGrantsAndProfile()
    }

    fun startReplacingKey() {
        local.update { it.copy(replacingKey = true) }
    }

    fun cancelReplacingKey() {
        local.update { it.copy(keyInput = "", keyInputInvalid = false, replacingKey = false) }
    }

    /** Opens Android's own dialog. The scale has to be awake for Android to find it. */
    fun allowSystemWake() {
        viewModelScope.launch {
            val allowed = listener.allowSystemWake()
            local.update { it.copy(wokenBySystem = allowed, systemWakeFailed = !allowed) }
        }
    }

    /** The scale moved this person to another of its user slots. Later weigh-ins from it are theirs. */
    fun claimIgnoredProfile() {
        val ignored = store.config.value.ignoredProfile ?: return
        store.setProfile(ignored.profile)
    }

    fun deleteLastWeighIn() {
        val weighIn = latestWeighIn ?: return
        viewModelScope.launch {
            try {
                writer.delete(weighIn)
                local.update { it.copy(screenError = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                local.update { it.copy(screenError = error.toScreenError(logTag = "Scales")) }
            }
        }
    }

    fun removeScale() {
        listener.forget()
        local.value = ScalesLocalState()
    }

    private suspend fun ScaleWeighIn.toDisplay(): ScaleWeighInDisplay? {
        val weightKg = reading.weightKg ?: return null
        val composition = writer.compositionOf(this)
        return ScaleWeighInDisplay(
            time = dateTimeFormatters.mediumDateTime().format(time.atZone(ZoneId.systemDefault())),
            weight = unitFormatter.weight(weightKg).text,
            heartRate = reading.heartRateBpm?.let { unitFormatter.heartRate(it.toLong()).text },
            bodyFat = composition?.let { unitFormatter.percent(it.bodyFatPercent).text },
            leanMass = composition?.let { unitFormatter.bodyMass(it.fatFreeMassKg).text },
            bodyWater = composition?.let { unitFormatter.bodyMass(it.bodyWaterKg).text },
            weightOnly = reading.impedanceLowOhm == null,
        )
    }
}
