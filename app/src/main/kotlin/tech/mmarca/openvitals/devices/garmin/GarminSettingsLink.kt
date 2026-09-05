package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import androidx.annotation.VisibleForTesting
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.devices.core.RadioLeaseBusyException

/**
 * An open conversation with the watch's settings service, held open because browsing
 * is read, tap, read. Replies are correlated by content, not request id: the watch
 * answers under its own ids. A failed lease renewal closes the link at once.
 */
class GarminSettingsLink private constructor(
    private val scope: CoroutineScope,
    private val session: GarminSession,
    /** Closes the transport below. A no-op for a link built by [forTest]. */
    private val teardownTransport: () -> Unit,
    /** Gives the radio back. A no-op for a link built by [forTest]. */
    private val releaseLease: () -> Unit,
) {

    /**
     * Every request runs on [scope]'s dispatcher, which must be sequential:
     * [GarminProtobufTransport] keeps plain maps shared with the frame pump.
     */
    private val confinement = scope.coroutineContext.minusKey(Job)

    /** Settings replies the watch sent under its own id. */
    private val replies = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)

    /**
     * Fires when the link goes away, so a request in flight fails at once
     * rather than waiting out its timeout. Replay 1 for late subscribers.
     */
    private val gone = MutableSharedFlow<Unit>(replay = 1)

    @Volatile
    private var closed = false

    private val tornDown = AtomicBoolean(false)

    private var renewals: Job? = null
    private var dropJob: Job? = null

    /** Whether the link is still usable. */
    val isOpen: Boolean get() = !closed

    /** Fetches one screen: its layout and the values behind it. */
    suspend fun screen(screenId: Int): GarminSettingsScreen? {
        val definition = ask(
            GarminSettingsService.screenDefinition(screenId),
            "screen $screenId definition",
            responseField = GarminSettingsService.DEFINITION_RESPONSE_FIELD,
            expectScreen = screenId,
        ) ?: return null

        // Asked twice if need be: without the state every toggle renders inert.
        var state = ask(
            GarminSettingsService.screenState(screenId),
            "screen $screenId state",
            responseField = GarminSettingsService.STATE_RESPONSE_FIELD,
            expectScreen = screenId,
        )
        if (state == null && isOpen) {
            GarminLog.log("[GARMIN-SETTINGS] no state for $screenId, asking again")
            state = ask(
                GarminSettingsService.screenState(screenId),
                "screen $screenId state (retry)",
                responseField = GarminSettingsService.STATE_RESPONSE_FIELD,
                expectScreen = screenId,
            )
        }
        if (state == null) {
            GarminLog.log(
                "[GARMIN-SETTINGS] screen $screenId has no state — every " +
                    "switch on it will render inert",
            )
        }
        val screen = parseGarminSettingsScreen(definition, stateReply = state)
        if (screen != null) {
            // Every row as parsed: what matters is which kind each came out as.
            GarminLog.log(
                "[GARMIN-SETTINGS] screen $screenId \"${screen.title}\" " +
                    "${screen.entries.size} rows, state=${screen.hasState}",
            )
            for (entry in screen.entries) {
                if (entry.isBlank) continue
                GarminLog.log(
                    "[GARMIN-SETTINGS]   ${entry.id}: ${entry.kind.name} " +
                        "\"${entry.title}\" summary=\"${entry.summary}\" " +
                        "sub=${entry.subscreenId} options=${entry.options.size} " +
                        "targetType=${entry.rawTargetType}",
                )
            }
        }
        return screen
    }

    /**
     * Flips a switch and reports whether the watch agreed. Null means no
     * answer, which is distinct from a refusal.
     */
    suspend fun setSwitch(screenId: Int, entryId: Int, value: Boolean): Boolean? {
        GarminLog.log("[GARMIN-SETTINGS] → switch $screenId/$entryId = $value")
        val reply = ask(
            GarminSettingsService.changeSwitch(
                screenId = screenId,
                entryId = entryId,
                value = value,
            ),
            "change switch",
            responseField = GarminSettingsService.CHANGE_RESPONSE_FIELD,
            expectScreen = screenId,
        )
        val ok = GarminSettingsService.changeSucceeded(reply)
        GarminLog.log("[GARMIN-SETTINGS] ← switch ${ok ?: "no answer"}")
        return ok
    }

    /** Chooses one of the options the watch supplied for an entry. */
    suspend fun setOption(screenId: Int, entryId: Int, index: Int): Boolean? {
        val reply = ask(
            GarminSettingsService.changeOption(
                screenId = screenId,
                entryId = entryId,
                index = index,
            ),
            "change option",
            responseField = GarminSettingsService.CHANGE_RESPONSE_FIELD,
            expectScreen = screenId,
        )
        return GarminSettingsService.changeSucceeded(reply)
    }

    /** Activates a delete row. The one operation that cannot be undone. */
    suspend fun delete(screenId: Int, entryId: Int): Boolean? {
        GarminLog.log("[GARMIN-SETTINGS] → delete $screenId/$entryId")
        val reply = ask(
            GarminSettingsService.changeDelete(screenId = screenId, entryId = entryId),
            "delete",
            responseField = GarminSettingsService.CHANGE_RESPONSE_FIELD,
            expectScreen = screenId,
        )
        val ok = GarminSettingsService.changeSucceeded(reply)
        GarminLog.log("[GARMIN-SETTINGS] ← delete ${ok ?: "no answer"}")
        return ok
    }

    /** Sets a time of day, as seconds since midnight. */
    suspend fun setTime(
        screenId: Int,
        entryId: Int,
        sinceMidnight: Duration,
    ): Boolean? {
        val reply = ask(
            GarminSettingsService.changeTime(
                screenId = screenId,
                entryId = entryId,
                sinceMidnight = sinceMidnight,
            ),
            "change time",
            responseField = GarminSettingsService.CHANGE_RESPONSE_FIELD,
            expectScreen = screenId,
        )
        return GarminSettingsService.changeSucceeded(reply)
    }

    /** Closes the link and releases everything it holds. Idempotent. */
    suspend fun close() {
        teardown()
    }

    /** Tears the link down once, from whichever path gets there first. */
    private fun teardown() {
        if (!tornDown.compareAndSet(false, true)) return
        closed = true
        renewals?.cancel()
        dropJob?.cancel()
        signalGone()
        // Fails every request in flight.
        session.abort("link closed")
        teardownTransport()
        releaseLease()
        GarminLog.log("[GARMIN-SETTINGS] link closed")
    }

    private fun signalGone() {
        gone.tryEmit(Unit)
    }

    /**
     * Sends a request and waits for the reply that answers it. Both reply
     * sources are raced: the id-based one never completes on this watch.
     * [expectScreen] is the screen the reply must be about, or a retransmitted
     * definition for another screen would satisfy the wait.
     */
    private suspend fun ask(
        request: ByteArray,
        label: String,
        responseField: Int,
        expectScreen: Int? = null,
    ): ByteArray? {
        if (closed) return null
        return withContext(confinement) {
            askConfined(request, label, responseField, expectScreen)
        }
    }

    private suspend fun askConfined(
        request: ByteArray,
        label: String,
        responseField: Int,
        expectScreen: Int?,
    ): ByteArray? {
        if (closed) return null
        return coroutineScope {
            val answer = CompletableDeferred<ByteArray?>()

            fun offer(reply: ByteArray?) {
                if (answer.isCompleted || reply == null) return
                if (!GarminSettingsService.carries(reply, responseField)) return
                if (expectScreen != null) {
                    val about = GarminSettingsService.screenIdOf(reply, responseField)
                    if (about != null && about != expectScreen) {
                        GarminLog.log(
                            "[GARMIN-SETTINGS] ignoring a reply about screen " +
                                "$about while waiting for $expectScreen",
                        )
                        return
                    }
                }
                answer.complete(reply)
            }

            // UNDISPATCHED, so both watchers subscribe before the request goes out.
            val replyWatch = launch(start = CoroutineStart.UNDISPATCHED) {
                replies.collect { offer(it) }
            }
            val goneWatch = launch(start = CoroutineStart.UNDISPATCHED) {
                gone.collect { answer.complete(null) }
            }
            val requester = launch {
                try {
                    offer(
                        session.protobuf.request(
                            request,
                            label = label,
                            timeout = GarminSettingsService.REPLY_TIMEOUT,
                        ),
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // A closed link aborts the transport; the gone signal resolves the wait.
                }
            }
            try {
                withTimeoutOrNull(GarminSettingsService.REPLY_TIMEOUT) { answer.await() }
            } finally {
                replyWatch.cancel()
                goneWatch.cancel()
                requester.cancel()
            }
        }
    }

    /** Wires the hooks a freshly opened (or test) link needs. */
    private fun installReplyHook() {
        session.protobuf.onUnsolicited = { payload ->
            if (GarminSettingsService.unwrap(payload) != null) {
                replies.tryEmit(payload)
            }
        }
    }

    private fun startRenewals(address: String, lease: GarminRadioLease) {
        renewals = scope.launch {
            while (isActive) {
                delay(GarminRadioLease.renewInterval)
                if (!lease.renew(address, GarminRadioOwners.SETTINGS)) {
                    // Somebody asked for the radio. Yield rather than make them wait.
                    GarminLog.log("[GARMIN-SETTINGS] yielding the radio")
                    // From a sibling job: teardown cancels this one.
                    scope.launch { teardown() }
                    return@launch
                }
            }
        }
    }

    private fun watchForDrop(gatt: GarminGattClient) {
        dropJob = scope.launch {
            val reason = gatt.onDisconnected.first()
            GarminLog.log("[GARMIN-SETTINGS] link dropped: $reason")
            session.abort(reason)
            closed = true
            signalGone()
            // Full teardown from a sibling job (teardown cancels dropJob).
            scope.launch { teardown() }
        }
    }

    companion object {

        /** How long the watch gets to finish introducing itself. */
        private val HANDSHAKE_TIMEOUT = 15.seconds

        /** How long to wait for the current radio holder to let go. */
        private const val HANDOVER_WAIT_MILLIS = 8_000L
        private const val RETRY_STEP_MILLIS = 250L

        /**
         * Connects, completes the handshake and opens the settings service for
         * a locale, which decides the language of every title.
         *
         * [scope] hosts the link's long-lived jobs and must outlive it. Throws
         * [RadioLeaseBusyException], [GarminGattClientException] or
         * [kotlinx.coroutines.TimeoutCancellationException].
         */
        suspend fun open(
            context: Context,
            scope: CoroutineScope,
            address: String,
            phoneName: String,
            manufacturer: String,
            model: String,
            language: String = "en_US",
            region: String = "us",
            lease: GarminRadioLease = SharedGarminRadioLease,
            weatherProvider: (() -> tech.mmarca.openvitals.devices.weather.WeatherSnapshot?)? = null,
            agpsSource: GarminAgpsSource? = null,
            calendarProvider: ((beginEpochSeconds: Long, endEpochSeconds: Long) -> List<GarminCalendarEvent>?)? = null,
        ): GarminSettingsLink {
            acquireOrWait(lease, address)

            val gatt = GarminGattClient(context, address)
            val ready = CompletableDeferred<Unit>()

            var transport: GarminMlTransport? = null
            val session = GarminSession(
                scope = scope,
                send = { frame ->
                    (transport ?: throw GarminGattClientException("Not connected"))
                        .sendFrame(frame)
                },
                bluetoothName = phoneName,
                manufacturer = manufacturer,
                model = model,
                // A file sync underneath would fight for the radio and die on close.
                syncFiles = false,
                // Any held link should answer the watch's weather fetches.
                weatherProvider = weatherProvider,
                agpsSource = agpsSource,
                calendarProvider = calendarProvider,
                onHandshakeReady = {
                    if (!ready.isCompleted) ready.complete(Unit)
                },
            )

            // Frames land on the binder thread; a channel keeps their order.
            val frames = Channel<GarminGfdiFrame>(Channel.UNLIMITED)
            val pump = scope.launch { for (frame in frames) session.handleFrame(frame) }

            try {
                transport = gatt.connect(onFrame = { frame -> frames.trySend(frame) })
                session.start()
                // Anything sent before the handshake finishes is dropped.
                withTimeout(HANDSHAKE_TIMEOUT) { ready.await() }
            } catch (error: Throwable) {
                // Nothing is listening yet; only the transport needs undoing.
                pump.cancel()
                frames.close()
                session.abort("handshake failed")
                gatt.close()
                lease.release(address, GarminRadioOwners.SETTINGS)
                throw error
            }

            val link = GarminSettingsLink(
                scope = scope,
                session = session,
                teardownTransport = {
                    pump.cancel()
                    frames.close()
                    gatt.close()
                },
                releaseLease = { lease.release(address, GarminRadioOwners.SETTINGS) },
            )
            link.installReplyHook()
            link.startRenewals(address, lease)
            link.watchForDrop(gatt)

            // Fire and forget: the init reply is not needed downstream.
            scope.launch {
                link.ask(
                    GarminSettingsService.init(language = language, region = region),
                    "init",
                    responseField = GarminSettingsService.DEFINITION_RESPONSE_FIELD,
                )
            }
            return link
        }

        /** Takes the lease, or asks and waits briefly, as `withRadioLease` does. */
        private suspend fun acquireOrWait(lease: GarminRadioLease, address: String) {
            if (lease.acquire(address, GarminRadioOwners.SETTINGS)) return
            lease.request(address, GarminRadioOwners.SETTINGS)
            var waited = 0L
            while (waited < HANDOVER_WAIT_MILLIS) {
                delay(RETRY_STEP_MILLIS)
                waited += RETRY_STEP_MILLIS
                if (lease.acquire(address, GarminRadioOwners.SETTINGS)) return
            }
            throw RadioLeaseBusyException(lease.owner(address) ?: "another task")
        }

        /** A link over a session that never connected, for tests. */
        @VisibleForTesting
        fun forTest(scope: CoroutineScope, session: GarminSession): GarminSettingsLink {
            val link = GarminSettingsLink(
                scope = scope,
                session = session,
                teardownTransport = {},
                releaseLease = {},
            )
            link.installReplyHook()
            return link
        }
    }
}
