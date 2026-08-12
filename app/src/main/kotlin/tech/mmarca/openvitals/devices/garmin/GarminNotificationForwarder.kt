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
 * The forwarder's view of the process-wide radio discipline.
 *
 * A thin seam over [RadioLeases] (devices/core), which is a singleton
 * `object`: the forwarder holds its lease indefinitely and yields on demand,
 * and that whole dance is tested with a fake lease, which an `object` cannot
 * be. Owner tags are plain strings, as in the Flutter build, because they end
 * up in logcat where a readable name is the whole point.
 */
interface GarminRadioLease {
    /** Takes the lease on [address], or false when something else holds it. */
    fun acquire(address: String, owner: String): Boolean

    /**
     * Announces that [owner] wants a lease somebody else holds. Grants
     * nothing — it makes the holder's next [renew] fail.
     */
    fun request(address: String, owner: String)

    /**
     * Extends a held lease. False once it has expired, been taken, or somebody
     * else has asked for it — the last of which is the holder's cue to stop.
     */
    fun renew(address: String, owner: String): Boolean

    /** Releases a lease. A no-op when [owner] is not the holder. */
    fun release(address: String, owner: String)

    /** Who holds [address], or null when it is free. Diagnostic. */
    fun owner(address: String): String?

    companion object {
        /**
         * How long a lease survives without a renewal. Short on purpose: a
         * holder can be killed at any moment, and a lease that outlived its
         * holder would wedge the radio until reboot.
         */
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
 * Holds one link to the watch open for as long as the watch is in range, and
 * announces notifications over it.
 *
 * **Why it is held rather than opened per notification.** The first version of
 * this closed the link after twenty idle seconds, and a real vívoactive 5 made
 * the cost obvious: the watch spent most of its life disconnected and said so
 * on the wrist ("reconnect to phone to refresh data"), and — worse — a link
 * re-opened a few seconds after the last one closed sometimes never
 * re-subscribed at all, silently losing the notification it had been opened
 * for. A Garmin watch is built to have a continuously connected phone; Garmin
 * Connect and Gadgetbridge both give it one.
 *
 * So the only timing left is about recovery, not about closing:
 *
 * * [coalesceWindow] — a burst arriving before the link is up shares one
 *   connect. Bounded by [maxCoalesceWait] so a steady drip cannot postpone it
 *   forever.
 * * [reconnectBackoff] — how long to wait after the watch walks out of range
 *   before trying again, doubling up to [maxReconnectBackoff]. A watch left at
 *   home must not be retried in a tight loop all day.
 *
 * **Yielding.** Holding the radio indefinitely would block the sync, find and
 * settings paths, which are things the user actively asked for. The lease is
 * renewed on a timer, and a renewal that fails means somebody has asked for
 * the radio: the link is dropped at once and re-established after
 * [yieldRetryDelay].
 *
 * Port of the Flutter build's `garmin_notification_forwarder.dart`. Dart
 * `Timer`s became `delay` jobs on the injected [scope], which is how the whole
 * thing runs under `runTest` with no radio and no real time. The scope MUST
 * dispatch sequentially (the bridge supplies a single-threaded one): every
 * piece of state below is guarded by that, exactly as the Dart event loop
 * guarded the original.
 */
class GarminNotificationForwarder(
    private val scope: CoroutineScope,
    private val address: String,
    private val phoneName: String,
    private val manufacturer: String,
    private val model: String,
    /** Guards the radio against everything else in this process. */
    private val lease: GarminRadioLease,
    /** Injected so tests drive the state machine with no Bluetooth. */
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
    /**
     * First delay after the watch goes out of range. Doubles per failed
     * attempt up to [maxReconnectBackoff].
     */
    private val reconnectBackoff: Duration = 15.seconds,
    private val maxReconnectBackoff: Duration = 5.minutes,
    /**
     * How long to stay off the radio after yielding it to a sync or a find,
     * so the handover is not immediately fought over.
     */
    private val yieldRetryDelay: Duration = 20.seconds,
    /** How long to wait before retrying when the radio is held by something else. */
    private val busyRetry: Duration = 10.seconds,
    private val renewInterval: Duration = GarminRadioLease.renewInterval,
    /**
     * Called when the forwarder has given up entirely — no link, nothing
     * queued, and no reconnect pending. With a held link this is reached only
     * on dispose or when there is no watch to reach at all.
     */
    private val onIdle: (() -> Unit)? = null,
    /**
     * Invoked when the wearer acts on a notification from the wrist. Handed to
     * every link this forwarder opens.
     */
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

    /**
     * Grows while the watch cannot be reached, so a watch left at home is not
     * retried in a tight loop all day. Reset the moment a link opens.
     */
    private var backoff: Duration = Duration.ZERO

    /** Whether a link is currently open. Diagnostic and test-facing. */
    val isLinkOpen: Boolean get() = link?.isOpen == true

    /**
     * Opens and holds the link NOW, with nothing queued — companion mode's
     * entry: the link exists for the watch's own errands (weather fetches,
     * find-my-phone, file announcements), not for a pending notification.
     */
    fun ensureLink() {
        scope.launch { reopen() }
    }

    /**
     * Relays a phone foreground/background change to the watch, when a link
     * is up. Nothing to do when it is not: the next link sends the current
     * state during its handshake.
     */
    fun setHostForeground(foreground: Boolean) {
        scope.launch {
            val current = link ?: return@launch
            if (!current.isOpen) return@launch
            current.setHostForeground(foreground)
        }
    }

    /**
     * Opens or closes a live-streaming service on the current link, so a
     * toggle takes effect without waiting for a reconnect. A closed link
     * needs nothing: the next one opens whatever is enabled by then.
     */
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
            // The link is already up — the normal case now that it is held — so
            // there is nothing to coalesce.
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
                // A user-initiated sync or find owns the radio. Notifications
                // wait — this is the right priority, and it is safe precisely
                // because GNCS is pull-based: the watch asks again once we can
                // answer.
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
                // Says what was OBSERVED, not what it was assumed to mean. The
                // transport reports a disconnect; whether the watch walked
                // away, the radio was reset, or power management dropped a
                // background link is not knowable from here, and guessing in
                // the log sent one investigation down the wrong path.
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
            // The queue is KEPT. With a held link the watch is normally
            // reachable, so a failure here means it is momentarily away — and
            // the notification the link was opened for should go out when it
            // comes back, not be discarded.
            connecting = false
            scheduleReconnect()
            return
        } finally {
            connecting = false
        }

        drain()
    }

    /**
     * Renews the lease, and gives the radio up the moment somebody else asks.
     *
     * This is what stops a permanently held link from blocking the things the
     * user actively initiates. A failed renewal means either the lease expired
     * (it should not have) or a sync/find/settings action has requested it;
     * both mean stop.
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

    /**
     * Opens the link again whether or not anything is queued — the point of
     * holding it is that the watch stays connected between notifications.
     */
    private suspend fun reopen() {
        if (disposed || connecting) return
        if (link?.isOpen == true) return
        if (queue.isEmpty()) openHeld() else openAndDrain()
    }

    /**
     * The no-traffic path into [openAndDrain], which otherwise returns early
     * on an empty queue.
     */
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

    /**
     * Drops the link, optionally re-establishing it.
     *
     * [reconnect] false is only for [dispose]: while the feature is on, a link
     * that ends for any other reason is meant to come back.
     */
    private suspend fun closeLink(reconnect: Boolean = false, after: Duration? = null) {
        val current = link
        link = null

        // Take back anything the link accepted but never announced, so it
        // survives into the next one. A handler lives and dies with its link,
        // and the forwarder has already dropped these from its own queue — so
        // without this a watch that walks away between "queued" and
        // "subscribed" loses exactly the notification the link was opened for,
        // which is the failure this whole held-link design exists to remove.
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
        // A closeLink can be running INSIDE the renewal loop or the gone
        // listener, and a job must not cancel itself mid-teardown — the link
        // and the lease would be left held.
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

        /**
         * Carries nothing. It exists so the connect path, which returns early
         * on an empty queue, can be used to re-establish a link the user wants
         * held even when there is no notification waiting.
         */
        data object KeepAlive : Pending
    }
}
