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
 * An open conversation with the watch's notification service. Held open
 * because GNCS is a pull protocol: the watch asks for the text seconds after
 * the announcement. An interface, so the forwarder's tests use fake links.
 */
interface GarminNotificationLink {

    /** The queue and the chunked upload. The forwarder takes back [GarminGncsHandler.held] on death. */
    val handler: GarminGncsHandler

    /** Whether the link is still usable. A watch that walks away closes it. */
    val isOpen: Boolean

    /**
     * Whether the watch has subscribed. It asks about once a second, so
     * this flips soon after the handshake if notifications are on. False is
     * the likeliest reason for a silent watch, and not an error here.
     */
    val subscribed: Boolean

    /** Fires when the link goes away, so waiters stop instead of timing out. */
    val onGone: Flow<Unit>

    /** Announces or updates [notification]. Held while the watch has not subscribed. */
    suspend fun push(notification: GarminNotification)

    suspend fun withdraw(notificationId: Long)

    /** Tells the watch whether the phone's app is in the foreground. */
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
    /** The watch asking the phone to ring, and to stop. Mostly arrives on the held link. */
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
    /** Live services to open once the link is up. Empty by default: each costs battery. */
    val realtimeServices: Set<GarminRealtimeService> = emptySet(),
    val onRealtimeReading: ((GarminRealtimeReading) -> Unit)? = null,
    /** The watch was just onboarded and needs the pair-flow completion. */
    val setupWizardPending: (() -> Boolean)? = null,
    val onSetupWizardCompleted: (() -> Unit)? = null,
)

/**
 * The real link: [GarminGattClient] below, a [GarminSession] with
 * `syncFiles = false` and a [GarminGncsHandler] above.
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
         * Connects and completes the handshake. [scope] must dispatch
         * sequentially. Throws [GarminGattClientException] or
         * [TimeoutCancellationException].
         */
        suspend fun open(
            context: Context,
            scope: CoroutineScope,
            request: GarminNotificationLinkRequest,
            handshakeTimeout: Duration = 15.seconds,
        ): GarminNotificationLink {
            val gatt = GarminGattClient(context, request.address)
            val ready = CompletableDeferred<Unit>()

            // The transport exists only after connect; the send callback goes through a holder.
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
                // Mandatory: a file transfer on a held link dies when the radio is yielded.
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
                // Anything sent before the handshake finishes is dropped.
                withTimeout(handshakeTimeout) { ready.await() }
                // After the handshake: the watch ignores control traffic before it.
                for (service in request.realtimeServices) {
                    transport.openService(service.code)
                }
            } catch (error: Throwable) {
                // Nothing is listening yet; only the transport needs undoing.
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
