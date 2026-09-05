package tech.mmarca.openvitals.features.watches

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.garmin.GarminEntryKind
import tech.mmarca.openvitals.devices.garmin.GarminGattClientException
import tech.mmarca.openvitals.devices.garmin.GarminLog
import tech.mmarca.openvitals.devices.garmin.GarminPhoneIdentity
import tech.mmarca.openvitals.devices.garmin.GarminSettingsLink
import tech.mmarca.openvitals.devices.garmin.GarminSettingsScreen
import tech.mmarca.openvitals.devices.garmin.GarminSettingsService
import tech.mmarca.openvitals.navigation.WATCH_DEVICE_ID_ARG
import tech.mmarca.openvitals.navigation.WATCH_SETTINGS_SCREEN_ID_ARG

/**
 * How long a link outlives the last screen watching it. Long enough to walk
 * into one alarm without a second handshake. A held link blocks file sync.
 */
private val LINK_GRACE = 20.seconds

/**
 * Which watches have a settings link open, one per watch, shared by every
 * screen browsing it. Owns opening, sharing, the grace window and closing.
 * A sync's lease request makes the link close itself; [releaseNow] takes
 * the radio back without waiting a renew tick.
 */
@Singleton
class WatchSettingsLinks @VisibleForTesting internal constructor(
    private val scope: CoroutineScope,
    private val grace: Duration,
    private val opener: suspend (CoroutineScope, String) -> GarminSettingsLink,
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        bleDeviceRepository: BleDeviceRepository,
        weatherStore: tech.mmarca.openvitals.devices.weather.WeatherStore,
        agpsStore: tech.mmarca.openvitals.devices.garmin.GarminAgpsStore,
        stateStore: tech.mmarca.openvitals.devices.garmin.GarminDeviceStateStore,
        calendarSource: tech.mmarca.openvitals.devices.garmin.GarminCalendarSource,
    ) : this(
        // Sequential: the link confines its protobuf traffic to this dispatcher.
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1)),
        grace = LINK_GRACE,
        opener = { scope, deviceId ->
            val device = bleDeviceRepository.devices.firstOrNull { it.id == deviceId }
            if (device == null || !device.isGarminGfdi) {
                throw GarminGattClientException("Not a paired Garmin device: $deviceId")
            }
            val phone = GarminPhoneIdentity()
            GarminSettingsLink.open(
                context = context,
                scope = scope,
                address = device.address,
                phoneName = phone.bluetoothName,
                manufacturer = phone.manufacturer,
                model = phone.model,
                weatherProvider = { weatherStore.freshSnapshot() },
                agpsSource = agpsStore.source(),
                calendarProvider = { begin, end ->
                    if (stateStore.calendarSync(deviceId)) {
                        calendarSource.events(begin, end)
                    } else {
                        null
                    }
                },
            )
        },
    )

    private class Held(
        var attempt: Deferred<GarminSettingsLink>,
        var refs: Int = 0,
        var graceJob: Job? = null,
    )

    private val held = HashMap<String, Held>()

    /** Declares a screen of [deviceId]'s tree open. Connects at once and cancels a pending close. */
    @Synchronized
    fun retain(deviceId: String) {
        val holder = held.getOrPut(deviceId) { Held(newAttempt(deviceId)) }
        holder.refs++
        holder.graceJob?.cancel()
        holder.graceJob = null
    }

    /** The counterpart of [retain]. The link survives the grace window, then closes. */
    @Synchronized
    fun release(deviceId: String) {
        val holder = held[deviceId] ?: return
        holder.refs--
        if (holder.refs > 0) return
        holder.graceJob?.cancel()
        holder.graceJob = scope.launch {
            delay(grace)
            val attempt = take(deviceId, expected = holder) ?: return@launch
            closeAttempt(attempt)
        }
    }

    /**
     * The open link for [deviceId], connecting as needed. Valid between
     * [retain] and [release]. A failed or dropped link is replaced.
     */
    suspend fun link(deviceId: String): GarminSettingsLink = attemptFor(deviceId).await()

    /** Whether a link is being held (or opened) for [deviceId]. */
    @Synchronized
    fun isHeld(deviceId: String): Boolean = held.containsKey(deviceId)

    /** Closes any link on [deviceId] and waits for it to be gone. */
    suspend fun releaseNow(deviceId: String) {
        val attempt = take(deviceId, expected = null) ?: return
        GarminLog.log("[GARMIN-SETTINGS] releasing the link for $deviceId")
        closeAttempt(attempt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Synchronized
    private fun attemptFor(deviceId: String): Deferred<GarminSettingsLink> {
        val holder = held.getOrPut(deviceId) { Held(newAttempt(deviceId)) }
        val attempt = holder.attempt
        if (attempt.isCompleted) {
            val stale = attempt.isCancelled ||
                runCatching { attempt.getCompleted() }
                    .fold(onSuccess = { !it.isOpen }, onFailure = { true })
            if (stale) holder.attempt = newAttempt(deviceId)
        }
        return holder.attempt
    }

    private fun newAttempt(deviceId: String): Deferred<GarminSettingsLink> =
        scope.async { opener(scope, deviceId) }

    /** Removes and returns [deviceId]'s attempt, or null if re-retained since [expected]. */
    @Synchronized
    private fun take(deviceId: String, expected: Held?): Deferred<GarminSettingsLink>? {
        val holder = held[deviceId] ?: return null
        if (expected != null && (holder !== expected || holder.refs > 0)) return null
        held.remove(deviceId)
        holder.graceJob?.cancel()
        return holder.attempt
    }

    private suspend fun closeAttempt(attempt: Deferred<GarminSettingsLink>) {
        try {
            attempt.await().close()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // An attempt that never produced a link has nothing to close.
            attempt.cancel()
        }
    }
}

/** What the watch made of a change. */
enum class WatchSettingsChangeResult {
    APPLIED,

    /** It answered, and said no. */
    REFUSED,

    /** It never answered. Distinct from [REFUSED]: the request may or may not have landed. */
    UNANSWERED,
}

/** A transient report on the last change, for the screen to word. */
enum class WatchSettingsNotice { REFUSED, UNANSWERED }

/** One-shot instructions to the screen. */
enum class WatchSettingsEvent {
    /** The screen described something just deleted. The watch answers a dead id with its parent. */
    CLOSE_SCREEN,
}

@Immutable
data class WatchSettingsUiState(
    val loading: Boolean = true,
    /** The link could not be opened, or the read threw. */
    val failed: Boolean = false,
    /** Null after loading (without failure) means the watch sent nothing. */
    val screen: GarminSettingsScreen? = null,
    /** Rows with a change in flight — drawn busy, and not re-tappable. */
    val busyEntryIds: Set<Int> = emptySet(),
    val notice: WatchSettingsNotice? = null,
) {
    val isEmpty: Boolean get() = !loading && !failed && (screen?.isEmpty ?: true)
}

/**
 * One screen of the watch's settings tree. The link is borrowed from
 * [WatchSettingsLinks] and shared with deeper screens. Every change re-reads
 * the screen: the watch may clamp, round or ignore what it is asked.
 */
@HiltViewModel
class WatchSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val links: WatchSettingsLinks,
) : ViewModel() {

    val deviceId: String = savedStateHandle.get<String>(WATCH_DEVICE_ID_ARG).orEmpty()
    val screenId: Int = savedStateHandle.get<String>(WATCH_SETTINGS_SCREEN_ID_ARG)
        ?.toIntOrNull()
        ?: GarminSettingsService.ROOT_SCREEN_ID

    private val state = MutableStateFlow(WatchSettingsUiState())
    val uiState: StateFlow<WatchSettingsUiState> = state.asStateFlow()

    private val eventFlow = MutableSharedFlow<WatchSettingsEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<WatchSettingsEvent> = eventFlow.asSharedFlow()

    /** Set once the first read lands, so resume can tell a return from a first appearance. */
    private var loadedOnce = false

    private var loadJob: Job? = null

    init {
        links.retain(deviceId)
        load()
    }

    /** Re-reads the screen. The retry button, and pull-to-refresh if it comes. */
    fun refresh() {
        load()
    }

    /** Re-reads on return to the foreground: a subscreen may have changed what belongs here. */
    fun onResumed() {
        if (!loadedOnce) return
        load()
    }

    fun setSwitch(entryId: Int, value: Boolean) {
        change(entryId) { link -> link.setSwitch(screenId, entryId, value) }
    }

    /** [index] is a position in the option list the WATCH supplied — never an ordinal. */
    fun chooseOption(entryId: Int, index: Int) {
        change(entryId) { link -> link.setOption(screenId, entryId, index) }
    }

    fun setTime(entryId: Int, hour: Int, minute: Int) {
        change(entryId) { link ->
            link.setTime(screenId, entryId, sinceMidnight = hour.hours + minute.minutes)
        }
    }

    /** Activates an [GarminEntryKind.ACTION] row, the delete. On success the screen closes. */
    fun runAction(entryId: Int) {
        change(entryId, closeOnSuccess = true) { link -> link.delete(screenId, entryId) }
    }

    override fun onCleared() {
        links.release(deviceId)
    }

    private fun load(clearNotice: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            state.update {
                it.copy(
                    loading = true,
                    failed = false,
                    notice = if (clearNotice) null else it.notice,
                )
            }
            try {
                val link = links.link(deviceId)
                val screen = link.screen(screenId)
                loadedOnce = true
                state.update { it.copy(loading = false, failed = false, screen = screen) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-SETTINGS] could not read screen $screenId: $error")
                state.update { it.copy(loading = false, failed = true, screen = null) }
            }
        }
    }

    /** The shape every change shares: apply, then re-read. */
    private fun change(
        entryId: Int,
        closeOnSuccess: Boolean = false,
        apply: suspend (GarminSettingsLink) -> Boolean?,
    ) {
        if (entryId in state.value.busyEntryIds) return
        viewModelScope.launch {
            state.update {
                it.copy(busyEntryIds = it.busyEntryIds + entryId, notice = null)
            }
            val result = try {
                when (apply(links.link(deviceId))) {
                    true -> WatchSettingsChangeResult.APPLIED
                    false -> WatchSettingsChangeResult.REFUSED
                    null -> WatchSettingsChangeResult.UNANSWERED
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-SETTINGS] change failed: $error")
                WatchSettingsChangeResult.UNANSWERED
            }
            state.update {
                it.copy(
                    busyEntryIds = it.busyEntryIds - entryId,
                    notice = when (result) {
                        WatchSettingsChangeResult.APPLIED -> null
                        WatchSettingsChangeResult.REFUSED -> WatchSettingsNotice.REFUSED
                        WatchSettingsChangeResult.UNANSWERED -> WatchSettingsNotice.UNANSWERED
                    },
                )
            }
            if (result == WatchSettingsChangeResult.APPLIED && closeOnSuccess) {
                eventFlow.tryEmit(WatchSettingsEvent.CLOSE_SCREEN)
                return@launch
            }
            // Not "if applied": show what the watch holds, not what was asked.
            load(clearNotice = false)
        }
    }
}
