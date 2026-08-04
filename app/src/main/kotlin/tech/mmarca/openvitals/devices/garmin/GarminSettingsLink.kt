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
 * An OPEN conversation with the watch's settings service.
 *
 * Held open on purpose. The file sync connects, works and closes in about a
 * second; browsing settings is the opposite shape — a person reads a screen,
 * decides, taps, reads the result — and reconnecting per request would cost a
 * handshake every time and lose the watch's own idea of where it is in the
 * tree.
 *
 * Everything here is correlated by CONTENT rather than request id: the watch
 * answers a settings request under an id of its own, so a reply is
 * indistinguishable from traffic it started, and matching on "is this
 * settings traffic" is not enough either — several arrive unprompted, and the
 * first one seen answered nothing that had been asked.
 *
 * **Radio discipline.** The lease is held for the whole browse and renewed on
 * a timer. A renewal that fails means somebody asked for the radio (a file
 * sync, via [tech.mmarca.openvitals.devices.core.RadioLeases]'s waiter
 * semantics) — the link closes itself at once, which releases the lease and
 * lets the waiter take it. That replaces the Flutter build's explicit
 * `WatchSettingsLinks.release` call from the sync side: here the yield is
 * driven from the holder's end, exactly as the notification forwarder yields.
 *
 * Port of the Flutter build's `garmin_settings_link.dart`.
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
     * Every request runs on [scope]'s dispatcher — the same one the frame
     * pump uses. [GarminProtobufTransport] keeps plain maps, safe exactly as
     * long as everything touching them shares one sequential dispatcher, as
     * the sync, find and notification paths already arrange; without this a
     * caller's own dispatcher (a view-model's Main) would race the pump.
     * [scope] must therefore dispatch sequentially.
     */
    private val confinement = scope.coroutineContext.minusKey(Job)

    /**
     * Settings replies the watch sent under an id of its own — the only way
     * most answers arrive on this watch.
     */
    private val replies = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)

    /**
     * Fires the moment the link goes away, so a request in flight fails at
     * once instead of waiting out a timeout for a reply that can never
     * arrive. A dropped watch left the screen saying "Reading from the
     * watch…" for thirty seconds with nothing on the other end.
     *
     * Replay 1, so a request that subscribes after the drop still learns of
     * it immediately.
     */
    private val gone = MutableSharedFlow<Unit>(replay = 1)

    @Volatile
    private var closed = false

    private val tornDown = AtomicBoolean(false)

    private var renewals: Job? = null
    private var dropJob: Job? = null

    /**
     * Whether the link is still usable. A watch that walks away closes it,
     * and every later request would otherwise wait out its full timeout.
     */
    val isOpen: Boolean get() = !closed

    /**
     * Fetches one screen: its layout AND the values currently behind it.
     *
     * Both, because they answer different questions — the definition says
     * there is a "Repeat" row, the state says it is set to Weekday. A screen
     * built from the definition alone cannot show what anything is set to,
     * and a switch has no value at all outside the state.
     */
    suspend fun screen(screenId: Int): GarminSettingsScreen? {
        val definition = ask(
            GarminSettingsService.screenDefinition(screenId),
            "screen $screenId definition",
            responseField = GarminSettingsService.DEFINITION_RESPONSE_FIELD,
            expectScreen = screenId,
        ) ?: return null

        // Asked twice if need be. The state is what makes a switch a switch —
        // a screen without it renders every toggle inert — and a single
        // dropped reply was enough to leave an alarm looking uncontrollable.
        // One retry costs a round trip; getting it wrong costs the whole
        // point of the screen.
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
            // Every row as parsed, not as raw bytes: a long hex dump is
            // truncated by logcat, and what matters when a control is missing
            // is which KIND each row came out as.
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
     * Flips a switch on the watch, and reports whether the watch agreed.
     *
     * Returns null when the watch did not answer at all, which is
     * deliberately distinct from false: "it refused" and "it never heard"
     * call for different words on screen, and collapsing them would report a
     * lost message as a rejection.
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

    /**
     * Activates a delete row. The answer matters more here than anywhere
     * else: this is the one operation that cannot be undone.
     */
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

    /**
     * Tears the link down once, on whichever path gets there first: an
     * explicit [close], a failed lease renewal, or the watch dropping the
     * connection (which the Dart original left to a later close — leaking the
     * renewal timer, and with it the lease).
     */
    private fun teardown() {
        if (!tornDown.compareAndSet(false, true)) return
        closed = true
        renewals?.cancel()
        dropJob?.cancel()
        signalGone()
        // Fails every request in flight, so nothing waits out a timeout for a
        // reply that can never come.
        session.abort("link closed")
        teardownTransport()
        releaseLease()
        GarminLog.log("[GARMIN-SETTINGS] link closed")
    }

    private fun signalGone() {
        gone.tryEmit(Unit)
    }

    /**
     * Sends a request and waits for the reply that ANSWERS it.
     *
     * Both sources are RACED rather than awaited together: some replies echo
     * the request id and some arrive as the watch's own traffic, and waiting
     * for both made every screen cost the full timeout, because the id-based
     * one never completes on this watch.
     *
     * [expectScreen] is the screen the reply must be ABOUT. Without it, a
     * retransmitted definition for some other screen satisfies the wait, and
     * the caller pairs one screen's layout with another's values — an alarm
     * list whose titles came from the old read and whose times came from the
     * alarm underneath it.
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

            // UNDISPATCHED, so both watchers are subscribed before the
            // request goes out — a reply cannot slip between send and listen.
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
                    // A closed link aborts the transport, which fails the
                    // request — the gone signal already resolves the wait.
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
                    // Somebody asked for the radio — a sync, most likely. The
                    // link yields rather than making them wait out the TTL.
                    GarminLog.log("[GARMIN-SETTINGS] yielding the radio")
                    // Torn down from a sibling job: teardown cancels THIS
                    // job, which must not cancel the teardown under itself.
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

        /**
         * How long to wait for the current radio holder to let go, mirroring
         * `withRadioLease`: the likely holder is the notification forwarder,
         * which yields on its next renew tick.
         */
        private const val HANDOVER_WAIT_MILLIS = 8_000L
        private const val RETRY_STEP_MILLIS = 250L

        /**
         * Connects, completes the handshake, and opens the settings service
         * for a locale.
         *
         * The locale is not cosmetic: the watch translates every title it
         * later sends using it, so this decides what language the whole tree
         * comes back in.
         *
         * [scope] hosts the link's long-lived jobs (frame pump, renewals,
         * drop watch) and must outlive the link.
         *
         * Throws [RadioLeaseBusyException] when the radio cannot be taken,
         * [GarminGattClientException] when the watch cannot be reached, and
         * [kotlinx.coroutines.TimeoutCancellationException] when it connects
         * but never introduces itself.
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
                // Nothing to collect: this link exists to read settings, and
                // a file sync running underneath would fight it for the radio
                // and die when it closes.
                syncFiles = false,
                onHandshakeReady = {
                    if (!ready.isCompleted) ready.complete(Unit)
                },
            )

            // Frames land on the binder thread as they arrive; a channel
            // keeps their order while handing them to the session's
            // suspending handler.
            val frames = Channel<GarminGfdiFrame>(Channel.UNLIMITED)
            val pump = scope.launch { for (frame in frames) session.handleFrame(frame) }

            try {
                transport = gatt.connect(onFrame = { frame -> frames.trySend(frame) })
                session.start()
                // Anything sent before the watch finishes introducing itself
                // is dropped.
                withTimeout(HANDSHAKE_TIMEOUT) { ready.await() }
            } catch (error: Throwable) {
                // Nothing is listening yet, so the transport is the only
                // thing to undo.
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

            // Fired and forgotten: the init reply lands on a field of its own
            // that nothing downstream needs; it is sent to open the service,
            // not to be read.
            scope.launch {
                link.ask(
                    GarminSettingsService.init(language = language, region = region),
                    "init",
                    responseField = GarminSettingsService.DEFINITION_RESPONSE_FIELD,
                )
            }
            return link
        }

        /**
         * Takes the lease, or asks for it and waits briefly — the same
         * handover dance as `withRadioLease`, hand-rolled because the link
         * holds across suspension points rather than around one body.
         */
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

        /**
         * A link over a session that never connected — for tests that need
         * the request/teardown machinery without a watch on the other end.
         */
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
