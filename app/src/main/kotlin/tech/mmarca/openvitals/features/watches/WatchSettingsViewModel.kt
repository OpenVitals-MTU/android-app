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
 * How long a link outlives the last screen watching it.
 *
 * Long enough to walk from the Alarms list into one alarm without paying for
 * a second handshake, short enough that backing out of settings gives the
 * radio back. There is only ONE link to a watch: while this is held, a file
 * sync cannot connect.
 */
private val LINK_GRACE = 20.seconds

/**
 * Which watches currently have a settings link open — one per watch, shared
 * by every screen browsing it.
 *
 * A watch has one radio, so this is not a cache — it is the record of who
 * holds it. Port of the Flutter build's `WatchSettingsLinks` plus the link
 * provider it guarded: with no Riverpod here, this registry owns opening,
 * sharing, the grace window and closing. The file-sync handoff needs no call
 * into it — a sync's lease request makes the held link's next renewal fail,
 * and the link closes itself (see [GarminSettingsLink]) — but [releaseNow]
 * keeps the Flutter build's take-it-back semantics available to any caller
 * that wants the radio without waiting a renew tick.
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
    ) : this(
        // Sequential on purpose: the link confines its protobuf traffic to
        // this scope's dispatcher (see GarminSettingsLink), the same
        // arrangement GarminNotificationBridge makes for the forwarder.
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
            )
        },
    )

    private class Held(
        var attempt: Deferred<GarminSettingsLink>,
        var refs: Int = 0,
        var graceJob: Job? = null,
    )

    private val held = HashMap<String, Held>()

    /**
     * Declares a screen of [deviceId]'s tree open. Starts connecting at once
     * — the link is the expensive part, and the screen is already waiting —
     * and cancels any pending grace-window close.
     */
    @Synchronized
    fun retain(deviceId: String) {
        val holder = held.getOrPut(deviceId) { Held(newAttempt(deviceId)) }
        holder.refs++
        holder.graceJob?.cancel()
        holder.graceJob = null
    }

    /**
     * The counterpart of [retain]. When the last screen lets go, the link
     * survives for the grace window and then closes: walking from the Alarms
     * list into one alarm must not pay for a second handshake, but a watch
     * should not be left holding a connection because somebody backed out of
     * a menu.
     */
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
     * The open link for [deviceId], connecting or re-connecting as needed.
     *
     * Only valid between [retain] and [release]. A previous attempt that
     * failed — or a link the watch has since dropped — is replaced with a
     * fresh one, which is what "Try again" amounts to.
     *
     * Throws when the watch cannot be reached.
     */
    suspend fun link(deviceId: String): GarminSettingsLink = attemptFor(deviceId).await()

    /** Whether a link is being held (or opened) for [deviceId]. */
    @Synchronized
    fun isHeld(deviceId: String): Boolean = held.containsKey(deviceId)

    /**
     * Closes any link held on [deviceId], and waits for it to be gone.
     *
     * Awaited rather than fired off, because the caller wants the radio.
     */
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

    /**
     * Removes and returns [deviceId]'s attempt — or null when it has been
     * re-retained since [expected] scheduled this close, or is already gone.
     */
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

    /**
     * It never answered. Deliberately distinct from [REFUSED] — the request
     * may or may not have landed, and reporting a lost message as a rejection
     * would be a guess presented as fact.
     */
    UNANSWERED,
}

/** A transient report on the last change, for the screen to word. */
enum class WatchSettingsNotice { REFUSED, UNANSWERED }

/** One-shot instructions to the screen. */
enum class WatchSettingsEvent {
    /**
     * The screen this VM shows described something just deleted; showing it
     * further would be a page for a thing that no longer exists — and the
     * watch answers a dead screen's id with its parent's contents.
     */
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
 * One screen of the watch's own settings tree.
 *
 * Port of the Flutter build's `watch_settings_view_model.dart`. The link is
 * owned by [WatchSettingsLinks] and only BORROWED here — this VM retains it
 * for its lifetime, so pushing deeper into the tree (a new route entry, a new
 * VM) shares the same connection, and the registry's grace window keeps it up
 * across the gap.
 *
 * Every change re-reads the screen rather than assuming it worked: the watch
 * owns these settings and can clamp, round or ignore what it is asked, and
 * showing the value we requested instead of the one it holds would quietly
 * disagree with the wrist.
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

    /**
     * Set once the first read lands, so the resume hook can tell "came back
     * from a subscreen" apart from the screen's own first appearance.
     */
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

    /**
     * Called when the screen comes back to the foreground. Whatever happened
     * in a subscreen changes what belongs here — "Add Alarm" opens a screen
     * that creates one, and a list still showing the rows from before it
     * existed makes the watch and the phone disagree about something the
     * person just did — so returning re-reads. This also covers a delete's
     * "invalidate everything": the popped-to parent re-reads on resume.
     */
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

    /**
     * Activates an [GarminEntryKind.ACTION] row — the watch-marked delete.
     * On success the screen is told to close: it described the thing just
     * removed.
     */
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

    /**
     * The shape every change shares: apply, then RE-READ — see the class doc
     * for why the re-read is not belt and braces.
     */
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
            // Not "if applied": the watch may have half-heard, and the screen
            // must show what the watch holds, not what was asked of it.
            load(clearNotice = false)
        }
    }
}
