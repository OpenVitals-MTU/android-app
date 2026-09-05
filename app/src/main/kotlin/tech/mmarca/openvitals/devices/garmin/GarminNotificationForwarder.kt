package tech.mmarca.openvitals.devices.garmin

import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.devices.core.RadioLeases

/**
 * The forwarder's view of the radio lease. A seam over [RadioLeases] so the
 * hold-and-yield dance can be tested with a fake. Owner tags are strings
 * because they end up in logcat.
 */
interface GarminRadioLease {
    /** Takes the lease on [address], or false when something else holds it. */
    fun acquire(address: String, owner: String): Boolean

    /** Announces that [owner] wants a held lease. Makes the holder's next [renew] fail. */
    fun request(address: String, owner: String)

    /** Extends a held lease. False once expired, taken, or requested by someone else. */
    fun renew(address: String, owner: String): Boolean

    /** Releases a lease. A no-op when [owner] is not the holder. */
    fun release(address: String, owner: String)

    /** Who holds [address], or null when it is free. Diagnostic. */
    fun owner(address: String): String?

    companion object {
        /** Lease lifetime without renewal. Short, so a killed holder cannot wedge the radio. */
        val ttl: Duration = 15.seconds
        val renewInterval: Duration = 5.seconds
    }
}

/** Owner tags, mirroring the Flutter build's `GarminRadioOwner`. */
object GarminRadioOwners {
    const val SYNC = "sync"
    const val FIND = "find"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
}

/** The real lease: [RadioLeases], with this interface's TTL. */
object SharedGarminRadioLease : GarminRadioLease {
    override fun acquire(address: String, owner: String): Boolean =
        RadioLeases.acquire(address, owner, GarminRadioLease.ttl.inWholeMilliseconds)

    override fun request(address: String, owner: String) =
        RadioLeases.request(address, owner)

    override fun renew(address: String, owner: String): Boolean =
        RadioLeases.renew(address, owner, GarminRadioLease.ttl.inWholeMilliseconds)

    override fun release(address: String, owner: String) =
        RadioLeases.release(address, owner)

    override fun owner(address: String): String? = RadioLeases.owner(address)
}

/**
 * Holds one link to the watch open while it is in range and announces notifications
 * over it. A Garmin watch expects a continuously connected phone.
 *
 * [coalesceWindow] lets a burst share one connect, bounded by [maxCoalesceWait].
 * [reconnectBackoff] doubles up to [maxReconnectBackoff]. A failed lease renewal
 * means someone asked for the radio: the link drops and returns after [yieldRetryDelay].
 * [scope] must dispatch sequentially.
 */
class GarminNotificationForwarder(
    private val scope: CoroutineScope,
    private val address: String,
    private val phoneName: String,
    private val manufacturer: String,
    private val model: String,
    /** Guards the radio against everything else in this process. */
    private val lease: GarminRadioLease,
    /** Injected so tests drive the state machine without Bluetooth. */
    private val openLink: suspend (GarminNotificationLinkRequest) -> GarminNotificationLink,
    private val onFindPhone: ((durationSeconds: Int) -> Unit)? = null,
    private val onFindPhoneCancel: (() -> Unit)? = null,
    private val weatherProvider: (() -> tech.mmarca.openvitals.devices.weather.WeatherSnapshot?)? = null,
    private val agpsSource: GarminAgpsSource? = null,
    private val calendarProvider: ((beginEpochSeconds: Long, endEpochSeconds: Long) -> List<GarminCalendarEvent>?)? = null,
    private val onFileAnnounced: (() -> Unit)? = null,
    private val locationProvider: (() -> GarminPhoneLocation?)? = null,
    private val hostForeground: (() -> Boolean)? = null,
    /** Live-streaming services to open on each link, and where readings go. */
    private val realtimeServices: () -> Set<GarminRealtimeService> = { emptySet() },
    private val onRealtimeReading: ((GarminRealtimeReading) -> Unit)? = null,
    private val setupWizardPending: (() -> Boolean)? = null,
    private val onSetupWizardCompleted: (() -> Unit)? = null,
    private val coalesceWindow: Duration = 1500.milliseconds,
    private val maxCoalesceWait: Duration = 4.seconds,
    /** First delay after the watch goes out of range. Doubles up to [maxReconnectBackoff]. */
    private val reconnectBackoff: Duration = 15.seconds,
    private val maxReconnectBackoff: Duration = 5.minutes,
    /** How long to stay off the radio after yielding it. */
    private val yieldRetryDelay: Duration = 20.seconds,
    /** How long to wait before retrying when the radio is held by something else. */
    private val busyRetry: Duration = 10.seconds,
    private val renewInterval: Duration = GarminRadioLease.renewInterval,
    /** Called when the forwarder has given up: no link, nothing queued, no reconnect pending. */
    private val onIdle: (() -> Unit)? = null,
    /** Invoked when the wearer acts on a notification. Handed to every link. */
    private val onAction: (suspend (GarminNotificationActionRequest) -> Unit)? = null,
) {

    /** Announcements waiting for a link. Bounded by the native buffer upstream. */
    private val queue = mutableListOf<Pending>()

    private var link: GarminNotificationLink? = null
    private var goneJob: Job? = null

    private var coalesceJob: Job? = null
    private var coalesceDeadlineJob: Job? = null
    private var retryJob: Job? = null
    private var renewJob: Job? = null

    private var connecting = false
    private var disposed = false

    /** Grows while the watch cannot be reached. Reset when a link opens. */
    private var backoff: Duration = Duration.ZERO

    /** Whether a link is currently open. Diagnostic and test-facing. */
    val isLinkOpen: Boolean get() = link?.isOpen == true

    /** Opens and holds the link now, with nothing queued. Companion mode's entry. */
    fun ensureLink() {
        scope.launch { reopen() }
    }

    /** Relays a foreground change when a link is up. The next link sends it at handshake. */
    fun setHostForeground(foreground: Boolean) {
        scope.launch {
            val current = link ?: return@launch
            if (!current.isOpen) return@launch
            current.setHostForeground(foreground)
        }
    }

    /** Opens or closes a live-streaming service on the current link. */
    fun setRealtimeService(service: GarminRealtimeService, enabled: Boolean) {
        scope.launch {
            val current = link ?: return@launch
            if (!current.isOpen) return@launch
            current.setRealtimeService(service, enabled)
        }
    }

    /** Queues [notification] for the watch. Callable from any thread. */
    fun post(notification: GarminNotification) {
        scope.launch { enqueue(Pending.Post(notification)) }
    }

    /** Queues a withdrawal for a notification the phone has dismissed. */
    fun withdraw(notificationId: Long) {
        scope.launch { enqueue(Pending.Withdraw(notificationId)) }
    }

    private suspend fun enqueue(item: Pending) {
        if (disposed) return
        queue.add(item)

        val current = link
        if (current != null && current.isOpen) {
            // The link is already up: nothing to coalesce.
            drain()
            return
        }
        if (connecting || retryJob != null) return

        coalesceJob?.cancel()
        coalesceJob = scope.launch {
            delay(coalesceWindow)
            openAndDrain()
        }
        // First arrival starts the ceiling; later ones must not push it out.
        if (coalesceDeadlineJob == null) {
            coalesceDeadlineJob = scope.launch {
                delay(maxCoalesceWait)
                coalesceJob?.cancel()
                openAndDrain()
            }
        }
    }

    private suspend fun openAndDrain() {
        cancelUnlessSelf(coalesceJob)
        coalesceJob = null
        cancelUnlessSelf(coalesceDeadlineJob)
        coalesceDeadlineJob = null
        if (disposed || connecting || queue.isEmpty()) return
        if (link?.isOpen == true) {
            drain()
            return
        }

        connecting = true
        try {
            if (!lease.acquire(address, GarminRadioOwners.NOTIFICATIONS)) {
                // A user-initiated sync or find owns the radio. Notifications wait;
                // GNCS is pull-based, so the watch asks again later.
                val holder = lease.owner(address)
                GarminLog.log(
                    "[GARMIN-NOTIFY] radio held by ${holder ?: "another task"}; " +
                        "retrying in ${busyRetry.inWholeSeconds}s",
                )
                scheduleRetry()
                return
            }

            GarminLog.log(
                "[GARMIN-NOTIFY] opening a link for ${queue.size} notification(s)",
            )
            val opened = openLink(
                GarminNotificationLinkRequest(
                    address = address,
                    phoneName = phoneName,
                    manufacturer = manufacturer,
                    model = model,
                    onAction = onAction,
                    onFindPhone = onFindPhone,
                    onFindPhoneCancel = onFindPhoneCancel,
                    weatherProvider = weatherProvider,
                    agpsSource = agpsSource,
                    calendarProvider = calendarProvider,
                    onFileAnnounced = onFileAnnounced,
                    locationProvider = locationProvider,
                    hostForeground = hostForeground,
                    realtimeServices = realtimeServices(),
                    onRealtimeReading = onRealtimeReading,
                    setupWizardPending = setupWizardPending,
                    onSetupWizardCompleted = onSetupWizardCompleted,
                ),
            )
            if (disposed) {
                runCatching { opened.close() }
                lease.release(address, GarminRadioOwners.NOTIFICATIONS)
                return
            }
            link = opened
            backoff = Duration.ZERO
            goneJob = scope.launch {
                opened.onGone.first()
                // Say what was observed, not what it was assumed to mean.
                GarminLog.log(
                    "[GARMIN-NOTIFY] the transport reported a disconnect; will reconnect",
                )
                closeLink(reconnect = true)
            }
            startRenewals()
            GarminLog.log("[GARMIN-NOTIFY] link held open")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            GarminLog.log("[GARMIN-NOTIFY] could not open a link: $error")
            lease.release(address, GarminRadioOwners.NOTIFICATIONS)
            // The queue is kept: the watch is momentarily away, not gone.
            connecting = false
            scheduleReconnect()
            return
        } finally {
            connecting = false
        }

        drain()
    }

    /**
     * Renews the lease and gives the radio up the moment somebody else asks.
     * A failed renewal means stop, whatever the cause.
     */
    private fun startRenewals() {
        renewJob?.cancel()
        renewJob = scope.launch {
            while (true) {
                delay(renewInterval)
                if (disposed || link == null) continue
                if (lease.renew(address, GarminRadioOwners.NOTIFICATIONS)) continue
                GarminLog.log(
                    "[GARMIN-NOTIFY] the radio was requested by something the user " +
                        "started; yielding",
                )
                closeLink(reconnect = true, after = yieldRetryDelay)
                break
            }
        }
    }

    private fun scheduleRetry() {
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(busyRetry)
            retryJob = null
            openAndDrain()
        }
    }

    /** Re-establishes the link after [after], or after a growing backoff. */
    private fun scheduleReconnect(after: Duration? = null) {
        if (disposed) return
        retryJob?.cancel()
        val delayFor = after ?: nextBackoff()
        GarminLog.log("[GARMIN-NOTIFY] reconnecting in ${delayFor.inWholeSeconds}s")
        retryJob = scope.launch {
            delay(delayFor)
            retryJob = null
            reopen()
        }
    }

    private fun nextBackoff(): Duration {
        backoff = if (backoff == Duration.ZERO) {
            reconnectBackoff
        } else {
            val doubled = backoff * 2
            if (doubled > maxReconnectBackoff) maxReconnectBackoff else doubled
        }
        return backoff
    }

    /** Opens the link again whether or not anything is queued. */
    private suspend fun reopen() {
        if (disposed || connecting) return
        if (link?.isOpen == true) return
        if (queue.isEmpty()) openHeld() else openAndDrain()
    }

    /** The no-traffic path into [openAndDrain]. */
    private suspend fun openHeld() {
        queue.add(Pending.KeepAlive)
        openAndDrain()
    }

    private suspend fun drain() {
        val current = link
        if (current == null || !current.isOpen) return
        while (queue.isNotEmpty()) {
            when (val item = queue.removeAt(0)) {
                Pending.KeepAlive -> continue
                is Pending.Post -> runCatching { current.push(item.notification) }
                    .onFailure { GarminLog.log("[GARMIN-NOTIFY] failed to announce: $it") }
                is Pending.Withdraw -> runCatching { current.withdraw(item.id) }
                    .onFailure { GarminLog.log("[GARMIN-NOTIFY] failed to announce: $it") }
            }
        }
    }

    /** Drops the link. [reconnect] false is only for [dispose]. */
    private suspend fun closeLink(reconnect: Boolean = false, after: Duration? = null) {
        val current = link
        link = null

        // Take back anything the link accepted but never announced, so the
        // notification the link was opened for survives into the next one.
        if (current != null) {
            val unsent = current.handler.held
            if (unsent.isNotEmpty()) {
                GarminLog.log(
                    "[GARMIN-NOTIFY] re-queueing ${unsent.size} notification(s) " +
                        "the watch never subscribed for",
                )
                queue.addAll(0, unsent.map { Pending.Post(it) })
            }
        }
        // closeLink can run inside the renewal loop or the gone listener; a job
        // must not cancel itself mid-teardown.
        cancelUnlessSelf(renewJob)
        renewJob = null
        cancelUnlessSelf(goneJob)
        goneJob = null
        if (current != null) runCatching { current.close() }
        lease.release(address, GarminRadioOwners.NOTIFICATIONS)

        if (!reconnect || disposed) {
            finishIfIdle()
            return
        }
        scheduleReconnect(after)
    }

    private suspend fun cancelUnlessSelf(job: Job?) {
        if (job == null) return
        if (job === coroutineContext[Job]) return
        job.cancel()
    }

    private fun finishIfIdle() {
        if (disposed) return
        if (link != null || connecting) return
        if (retryJob != null) return
        onIdle?.invoke()
    }

    /** Closes everything. Idempotent; the forwarder is unusable afterwards. */
    fun dispose() {
        scope.launch {
            if (disposed) return@launch
            disposed = true
            coalesceJob?.cancel()
            coalesceDeadlineJob?.cancel()
            retryJob?.cancel()
            retryJob = null
            cancelUnlessSelf(renewJob)
            queue.clear()
            closeLink()
        }
    }

    /** One queued instruction: announce a notification, or withdraw one. */
    private sealed interface Pending {
        data class Post(val notification: GarminNotification) : Pending
        data class Withdraw(val id: Long) : Pending

        /** Carries nothing. Lets the connect path re-establish a held link with an empty queue. */
        data object KeepAlive : Pending
    }
}
