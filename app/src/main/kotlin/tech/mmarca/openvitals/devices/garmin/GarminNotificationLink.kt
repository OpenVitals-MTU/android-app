package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * An OPEN conversation with the watch's notification service.
 *
 * Held open on purpose, and for a reason peculiar to GNCS: it is a **pull**
 * protocol. Announcing a notification sends no text — the watch asks for the
 * words afterwards, and "afterwards" can be several seconds later, when the
 * wearer raises their wrist. Closing the link as soon as the announcement was
 * acknowledged would deliver a card the watch then renders empty.
 *
 * An interface plus one BLE implementation, where the Dart original was a
 * single class: the forwarder's whole test suite drives the state machine
 * through fake links, and Kotlin cannot `implements` a concrete class.
 *
 * Port of the Flutter build's `garmin_notification_link.dart`. The Dart
 * version also exposed an `onActivity` stream feeding an idle timer; the
 * held-link redesign left it with no consumer, so it is not ported.
 */
interface GarminNotificationLink {

    /**
     * The queue and the chunked upload. Exposed because the forwarder
     * announces through it, and takes back [GarminGncsHandler.held] when the
     * link dies.
     */
    val handler: GarminGncsHandler

    /** Whether the link is still usable. A watch that walks away closes it. */
    val isOpen: Boolean

    /**
     * Whether the watch has actually subscribed.
     *
     * False until it sends its subscription request, which it does about once
     * a second — so this flips within a second of the handshake IF the wearer
     * has notifications switched on. It staying false is the single most
     * likely explanation for a silent watch, and it is not an error this end.
     */
    val subscribed: Boolean

    /**
     * Fires when the link goes away, so a caller waiting on the watch stops
     * waiting instead of timing out.
     */
    val onGone: Flow<Unit>

    /**
     * Announces [notification] to the watch, or updates it.
     *
     * Silently held when the watch has not subscribed — see [subscribed].
     */
    suspend fun push(notification: GarminNotification)

    suspend fun withdraw(notificationId: Long)

    /**
     * Tells the watch the phone's app came to (or left) the foreground — the
     * signal Garmin watches use to decide the companion is paying attention.
     */
    suspend fun setHostForeground(foreground: Boolean)

    /** Opens or closes a live-streaming service on this link. */
    suspend fun setRealtimeService(service: GarminRealtimeService, enabled: Boolean)

    /** Closes the link and releases everything. Idempotent. */
    suspend fun close()
}

/** What [GarminBleNotificationLink.open] (or a fake) needs to build a link. */
data class GarminNotificationLinkRequest(
    val address: String,
    val phoneName: String,
    val manufacturer: String,
    val model: String,
    /** Invoked when the wearer acts on a notification from the wrist. */
    val onAction: (suspend (GarminNotificationActionRequest) -> Unit)? = null,
    /**
     * The watch asking the phone to ring, and to stop. The held link is where
     * these overwhelmingly arrive — it is the connection that exists while
     * the watch sits on a wrist wondering where the phone went.
     */
    val onFindPhone: ((durationSeconds: Int) -> Unit)? = null,
    val onFindPhoneCancel: (() -> Unit)? = null,
    /** The weather to serve when the watch asks over the held link. */
    val weatherProvider: (() -> tech.mmarca.openvitals.devices.weather.WeatherSnapshot?)? = null,
    /** GPS ephemeris to serve while the link is held. */
    val agpsSource: GarminAgpsSource? = null,
    /** Calendar events for the watch's glance; null while sync is off. */
    val calendarProvider: ((beginEpochSeconds: Long, endEpochSeconds: Long) -> List<GarminCalendarEvent>?)? = null,
    /** A finished recording announced over the held link — sync it now. */
    val onFileAnnounced: (() -> Unit)? = null,
    /** The phone's position, for the watch's location asks. */
    val locationProvider: (() -> GarminPhoneLocation?)? = null,
    /** Whether the phone's app is in the foreground right now. */
    val hostForeground: (() -> Boolean)? = null,
    /**
     * Live-streaming services to open once the link is up, and where their
     * readings go. Empty means the watch streams nothing, which is the
     * default: every open service costs watch battery.
     */
    val realtimeServices: Set<GarminRealtimeService> = emptySet(),
    val onRealtimeReading: ((GarminRealtimeReading) -> Unit)? = null,
    /** The watch was just onboarded and needs the pair-flow completion. */
    val setupWizardPending: (() -> Boolean)? = null,
    val onSetupWizardCompleted: (() -> Unit)? = null,
)

/**
 * The real link: [GarminGattClient] below, [GarminSession] (with
 * `syncFiles = false` and a [GarminGncsHandler]) above.
 *
 * A twin of the settings link in shape: connect, finish the handshake, hold,
 * tear down once on every path out.
 */
class GarminBleNotificationLink private constructor(
    private val gatt: GarminGattClient,
    private val session: GarminSession,
    /** Held so live-streaming services can be opened and closed after setup. */
    private val transport: GarminMlTransport,
    override val handler: GarminGncsHandler,
    private val dropJob: Job,
    private val gone: MutableSharedFlow<Unit>,
) : GarminNotificationLink {

    @Volatile
    private var closed = false

    override val isOpen: Boolean get() = !closed

    override val subscribed: Boolean get() = handler.enabled

    override val onGone: Flow<Unit> = gone

    override suspend fun push(notification: GarminNotification) {
        if (closed) return
        handler.post(notification)
    }

    override suspend fun withdraw(notificationId: Long) {
        if (closed) return
        handler.remove(notificationId)
    }

    override suspend fun setRealtimeService(
        service: GarminRealtimeService,
        enabled: Boolean,
    ) {
        if (closed) return
        runCatching {
            if (enabled) transport.openService(service.code) else transport.closeService(service.code)
        }.onFailure { GarminLog.log("[GARMIN-LIVE] could not toggle ${service.name}: $it") }
    }

    override suspend fun setHostForeground(foreground: Boolean) {
        if (closed) return
        runCatching { session.notifyHostForeground(foreground) }
            .onFailure { GarminLog.log("[GARMIN-NOTIFY] could not send foreground state: $it") }
    }

    override suspend fun close() {
        closed = true
        dropJob.cancel()
        handler.reset()
        session.abort("link closed")
        gatt.close()
    }

    internal fun markClosed() {
        closed = true
    }

    companion object {

        /**
         * Connects and completes the handshake.
         *
         * [scope] must dispatch sequentially (the forwarder's single-threaded
         * scope): inbound frames are launched into it, and the session's own
         * mutex only guarantees order if the launches arrive in order.
         *
         * Throws [GarminGattClientException] when the watch cannot be reached,
         * and [TimeoutCancellationException] when it connects but never
         * introduces itself.
         */
        suspend fun open(
            context: Context,
            scope: CoroutineScope,
            request: GarminNotificationLinkRequest,
            handshakeTimeout: Duration = 15.seconds,
        ): GarminNotificationLink {
            val gatt = GarminGattClient(context, request.address)
            val ready = CompletableDeferred<Unit>()

            // The transport exists only after connect, but the handler and the
            // session both need a send callback now — so it goes through a
            // holder, exactly as the Dart link's `mlOrThrow` did.
            var transport: GarminMlTransport? = null
            val sendFrame: suspend (ByteArray) -> Unit = { frame ->
                (transport ?: throw GarminGattClientException("Transport is not open"))
                    .sendFrame(frame)
            }

            val handler = GarminGncsHandler(
                send = sendFrame,
                onAction = request.onAction,
            )
            val session = GarminSession(
                scope = scope,
                send = sendFrame,
                bluetoothName = request.phoneName,
                manufacturer = request.manufacturer,
                model = request.model,
                // Mandatory, not incidental: this link is held indefinitely,
                // and a file transfer dragged along behind it would die
                // mid-flight when the radio is yielded — which can lose a
                // file, since the watch is told to archive only what was
                // safely stored.
                syncFiles = false,
                notifications = handler,
                onFindPhone = request.onFindPhone,
                onFindPhoneCancel = request.onFindPhoneCancel,
                weatherProvider = request.weatherProvider,
                agpsSource = request.agpsSource,
                calendarProvider = request.calendarProvider,
                locationProvider = request.locationProvider,
                hostForeground = request.hostForeground,
                setupWizardPending = request.setupWizardPending?.invoke() == true,
                onSetupWizardCompleted = request.onSetupWizardCompleted,
                onFileAnnounced = { request.onFileAnnounced?.invoke() },
                onHandshakeReady = {
                    if (!ready.isCompleted) ready.complete(Unit)
                },
            )

            try {
                transport = gatt.connect(
                    onFrame = { frame -> scope.launch { session.handleFrame(frame) } },
                    onRealtime = { _, reading -> request.onRealtimeReading?.invoke(reading) },
                )
                session.start()
                // Anything sent before the watch has finished introducing
                // itself is dropped on the floor.
                withTimeout(handshakeTimeout) { ready.await() }
                // Live services are opened AFTER the handshake: the watch
                // ignores control traffic while it is still introducing
                // itself, and a refused registration is silent.
                for (service in request.realtimeServices) {
                    transport.openService(service.code)
                }
            } catch (error: Throwable) {
                // Nothing is listening yet, so the transport is the only thing
                // to undo.
                session.abort("handshake failed")
                gatt.close()
                throw error
            }

            val gone = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
            lateinit var link: GarminBleNotificationLink
            val dropJob = scope.launch {
                val reason = gatt.onDisconnected.first()
                GarminLog.log("[GARMIN-NOTIFY] link dropped: $reason")
                session.abort(reason)
                link.markClosed()
                gone.tryEmit(Unit)
            }
            link = GarminBleNotificationLink(gatt, session, transport, handler, dropJob, gone)
            return link
        }
    }
}
