package tech.mmarca.openvitals.devices.garmin

/**
 * Holds the notifications the watch might ask about, announces new ones, and
 * serves the attribute blobs it requests.
 *
 * **Transport-free by construction**, exactly like [GarminSession]: it is
 * handed a `send` callback that takes a built GFDI frame, so the whole
 * announce → request → chunked answer conversation is exercised over an
 * in-memory pipe with no Bluetooth.
 *
 * The shape of this class is dictated by GNCS being a **pull** protocol.
 * [post] does not send a notification — it sends word that one exists. The
 * text leaves the phone only if and when the watch asks, which may be seconds
 * later (when the wearer raises their wrist), may happen twice (again with a
 * larger length limit when they scroll into the body), or may never happen at
 * all. That last case is not an error and nothing here treats it as one.
 *
 * The concrete implementation of the [GarminNotificationsHandler] seam the
 * session calls into — named for the protocol (GNCS, Garmin's rendering of
 * Apple's ANCS) to keep it distinct from that interface. Ported from the
 * Flutter build's `garmin_notifications_handler.dart`, itself ported from
 * Gadgetbridge's `NotificationsHandler` (AGPLv3), with two deliberate
 * deviations noted at [handleDataStatus].
 */
class GarminGncsHandler(
    /** Hands one built GFDI frame to the transport below. */
    private val send: suspend (ByteArray) -> Unit,
    /**
     * Invoked when the wearer acts on a notification.
     *
     * The handler deliberately does not perform anything itself: firing an
     * action means talking to Android, and this class is transport-free and
     * platform-free so the whole conversation stays testable over an
     * in-memory pipe.
     */
    private val onAction: (suspend (GarminNotificationActionRequest) -> Unit)? = null,
    /**
     * How many notifications stay answerable.
     *
     * Gadgetbridge's number. The queue exists because the watch asks about a
     * notification by id long after it was announced, so a notification that
     * has fallen out simply cannot be answered — the watch renders it blank.
     * Ten is what a wrist realistically has on screen; more would only hold
     * text in memory that nobody will ask for.
     */
    private val maxQueued: Int = 10,
) : GarminNotificationsHandler {

    /**
     * Whether the watch has subscribed. Until it has, everything here is a
     * no-op: announcing to a watch that has not asked to be told is how a sync
     * session ends up sending notification traffic it never meant to.
     */
    var enabled: Boolean = false
        private set

    /** Oldest first, so [evict] drops the one least likely to be asked about. */
    private val queue = ArrayDeque<GarminNotification>()

    /** Announced the moment the watch subscribes. See [setEnabled]. */
    private val awaitingSubscription = mutableListOf<GarminNotification>()

    private var upload: NotificationUpload? = null

    /** The notifications still answerable, oldest first. Diagnostic only. */
    val queued: List<GarminNotification> get() = queue.toList()

    /**
     * Announcements this handler accepted but never got to send, because the
     * watch had not subscribed.
     *
     * A handler lives and dies with one link, so when the watch walks out of
     * range these would vanish with it — losing exactly the notification the
     * link was opened for. The forwarder takes them back and re-queues them
     * for the next link.
     */
    val held: List<GarminNotification> get() = awaitingSubscription.toList()

    /**
     * Records whether the watch is accepting notifications.
     *
     * Deliberately does NOT announce anything — see [flushHeld]. The caller
     * has to answer the watch's subscription message before sending it
     * anything else.
     */
    override fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        GarminLog.log("[GARMIN-NOTIFY] forwarding ${if (enabled) "enabled" else "disabled"}")
        if (!enabled) upload = null
    }

    /**
     * Announces everything that arrived before the watch subscribed.
     *
     * Load-bearing, not a nicety. This app OPENS the link in order to announce
     * something, so the announcement is always ready before the watch has got
     * round to subscribing — the subscription lands a couple of hundred
     * milliseconds after the handshake. Dropping what arrived in that window
     * meant the very notification the link was opened for was the one lost.
     *
     * Called AFTER the subscription status has gone out, never before.
     * Garmin's own ordering is status-for-the-inbound-message first and
     * follow-up second, and announcing ahead of that status means announcing
     * to a watch that has not yet been told its subscription was accepted.
     */
    override suspend fun flushHeld() {
        if (!enabled) return
        val waiting = awaitingSubscription.toList()
        awaitingSubscription.clear()
        for (notification in waiting) {
            // Skip anything evicted from the answerable queue while it waited:
            // the watch could ask about it and we would have nothing to answer
            // with.
            if (find(notification.id) == null) continue
            announce(notification, isUpdate = false)
        }
    }

    /**
     * Announces [notification] to the watch.
     *
     * An id already in the queue is announced as MODIFY rather than ADD — the
     * watch updates the one it is showing instead of buzzing a second time,
     * which is what a progress notification or an edited message needs.
     */
    suspend fun post(notification: GarminNotification) {
        val isUpdate = removeQueued(notification.id)
        queue.addLast(notification)
        evict()

        if (!enabled) {
            // Held, not dropped — see [setEnabled]. Logged because "the watch
            // has not subscribed" is the most likely reason nothing reaches the
            // wrist, and it is not a fault at this end: notifications are
            // switched off ON THE WATCH.
            awaitingSubscription.removeAll { it.id == notification.id }
            awaitingSubscription.add(notification)
            GarminLog.log(
                "[GARMIN-NOTIFY] the watch has not subscribed yet; holding " +
                    "notification ${notification.id}",
            )
            return
        }
        announce(notification, isUpdate = isUpdate)
    }

    private suspend fun announce(notification: GarminNotification, isUpdate: Boolean) {
        // Logged because an announcement carries no text and produces no
        // visible effect on its own: whether one went out, and whether the
        // watch then asked about it, is the only way to tell "the watch never
        // heard" from "the watch heard and did not care".
        GarminLog.log(
            "[GARMIN-NOTIFY] announcing ${notification.id} " +
                "(${if (isUpdate) "modify" else "add"}, ${notification.category.name})",
        )
        send(
            buildNotificationUpdate(
                updateType = if (isUpdate) {
                    GarminNotificationUpdateType.MODIFY
                } else {
                    GarminNotificationUpdateType.ADD
                },
                category = notification.category,
                count = countOf(notification.category),
                notificationId = notification.id,
                // Without this the watch draws no action controls at all,
                // however many the ACTIONS attribute later offers — the
                // announcement is where it decides whether to ask.
                hasActions = notification.hasActions,
            ),
        )
    }

    /**
     * Withdraws a notification the phone has dismissed.
     *
     * Silent for an id the queue no longer holds: that is the normal outcome
     * for anything older than [maxQueued], and telling the watch to remove
     * something it was never told about is noise.
     */
    suspend fun remove(id: Long) {
        if (!enabled) {
            // Withdraw it from what is waiting to be announced, or the watch
            // would be told about a notification the phone has already
            // dismissed. Logged because a run of silent dismissals otherwise
            // reads as a link that opened, did nothing and closed — which is
            // what made one session here impossible to interpret.
            val heldBefore = awaitingSubscription.size
            awaitingSubscription.removeAll { it.id == id }
            removeQueued(id)
            if (awaitingSubscription.size != heldBefore) {
                GarminLog.log("[GARMIN-NOTIFY] $id was dismissed before it was announced")
            }
            return
        }
        val notification = find(id) ?: return
        removeQueued(id)
        send(
            buildNotificationUpdate(
                updateType = GarminNotificationUpdateType.REMOVE,
                category = notification.category,
                count = countOf(notification.category),
                notificationId = id,
            ),
        )
    }

    /**
     * Answers a control request from the watch.
     *
     * The caller is expected to have sent the control status already — see the
     * [GarminNotificationControl] arm of [GarminSession].
     */
    override suspend fun handleControl(message: GarminNotificationControl) {
        if (!enabled) return

        when (message.command) {
            GarminNotificationCommand.GET_NOTIFICATION_ATTRIBUTES -> Unit // handled below
            GarminNotificationCommand.PERFORM_NOTIFICATION_ACTION,
            GarminNotificationCommand.PERFORM_LEGACY_NOTIFICATION_ACTION,
            -> {
                performAction(message)
                return
            }
            GarminNotificationCommand.GET_APP_ATTRIBUTES -> {
                // Gadgetbridge marks this "unknown/untested" and no watch here
                // has sent one. Logged so that stops being true silently.
                GarminLog.log(
                    "[GARMIN-NOTIFY] app attributes requested for " +
                        "${message.appIdentifier}; not implemented",
                )
                return
            }
        }

        val notification = find(message.notificationId)
        if (notification == null) {
            // Nothing to send and nothing to report. The watch asked about
            // something that has aged out of the queue; there is no protocol
            // way to say so, and an error status would abort a transfer that
            // never started.
            GarminLog.log(
                "[GARMIN-NOTIFY] no notification ${message.notificationId} " +
                    "left to answer with",
            )
            return
        }

        val blob = encodeGarminNotificationAttributes(
            notification = notification,
            requested = message.attributes,
        )
        GarminLog.log(
            "[GARMIN-NOTIFY] answering ${message.notificationId} with " +
                "${message.attributes.size} attributes (${blob.size}B)",
        )
        val newUpload = NotificationUpload(blob)
        upload = newUpload
        sendNext(newUpload)
    }

    /** Resolves an action the wearer invoked and hands it to [onAction]. */
    private suspend fun performAction(message: GarminNotificationControl) {
        val notification = find(message.notificationId)
        if (notification == null) {
            GarminLog.log(
                "[GARMIN-NOTIFY] action for unknown notification ${message.notificationId}",
            )
            return
        }
        val action = resolveAction(notification, message)
        if (action == null) {
            GarminLog.log(
                "[GARMIN-NOTIFY] no action matching code " +
                    "${message.actionCode} on ${message.notificationId}",
            )
            return
        }
        GarminLog.log(
            "[GARMIN-NOTIFY] the wearer chose \"${action.label}\" " +
                "(${action.kind.name}) on ${message.notificationId}" +
                if (message.actionText == null) "" else " with a reply",
        )
        onAction?.invoke(
            GarminNotificationActionRequest(
                notificationId = notification.id,
                action = action,
                replyText = message.actionText,
            ),
        )
    }

    /**
     * Matches what the watch invoked to what was offered.
     *
     * A legacy action carries no code this app chose — it is the accept/refuse
     * pair the watch draws from the ACTION_DECLINE category flag, whose
     * ordinals are 0 and 1. Refuse means dismiss, which is what a wearer
     * swiping a card away expects, so it maps onto whatever dismiss action was
     * offered.
     */
    private fun resolveAction(
        notification: GarminNotification,
        message: GarminNotificationControl,
    ): GarminNotificationAction? {
        if (message.command == GarminNotificationCommand.PERFORM_LEGACY_NOTIFICATION_ACTION) {
            val legacyRefuse = 1
            if (message.actionCode != legacyRefuse) return null
            return notification.actions
                .firstOrNull { it.kind == GarminNotificationActionKind.DISMISS }
        }
        val code = message.actionCode ?: return null
        return notification.actions.firstOrNull { it.kind.code == code }
    }

    /**
     * Drives the chunked upload from the watch's per-chunk verdict.
     *
     * Two deliberate deviations from Gadgetbridge:
     *
     * * **RESEND is honoured, once.** Gadgetbridge abandons the upload with a
     *   `TODO`. Repeating the last chunk costs nothing — its offset and CRC
     *   are already held — and the alternative is a notification that arrives
     *   on the wrist with an empty body and no way to tell why.
     * * **OFFSET_MISMATCH abandons rather than seeks.** The status carries no
     *   offset, so there is nothing to recover to; guessing would corrupt the
     *   blob more quietly than failing does.
     */
    override suspend fun handleDataStatus(message: GarminNotificationDataStatus) {
        if (!enabled) return
        val active = upload
        if (active == null) {
            GarminLog.log("[GARMIN-NOTIFY] transfer status with nothing in flight")
            return
        }

        if (message.canProceed) {
            if (active.isComplete) {
                upload = null
                GarminLog.log("[GARMIN-NOTIFY] sent ${active.totalSize}B")
                send(buildNotificationDataFinalAck())
                return
            }
            sendNext(active)
            return
        }

        if (message.transferStatus == GarminNotificationTransferStatus.RESEND &&
            active.canResend
        ) {
            GarminLog.log("[GARMIN-NOTIFY] resending chunk at ${active.lastOffset}")
            resend(active)
            return
        }

        GarminLog.log(
            "[GARMIN-NOTIFY] abandoning transfer: ${message.transferStatus.name}",
        )
        upload = null
    }

    /**
     * Forgets everything. Called when a link ends, so a new one does not
     * answer with a transfer the previous watch conversation left half-sent.
     */
    fun reset() {
        upload = null
        queue.clear()
        awaitingSubscription.clear()
    }

    private suspend fun sendNext(upload: NotificationUpload) {
        val chunk = upload.take()
        send(
            buildNotificationData(
                chunk = chunk,
                totalSize = upload.totalSize,
                dataOffset = upload.lastOffset,
                runningCrc = upload.lastCrc,
            ),
        )
    }

    private suspend fun resend(upload: NotificationUpload) {
        send(
            buildNotificationData(
                chunk = upload.lastChunk,
                totalSize = upload.totalSize,
                dataOffset = upload.lastOffset,
                runningCrc = upload.lastCrc,
            ),
        )
        upload.markResent()
    }

    private fun find(id: Long): GarminNotification? =
        queue.firstOrNull { it.id == id }

    /** Removes [id] if present. Returns whether it was. */
    private fun removeQueued(id: Long): Boolean {
        val before = queue.size
        queue.removeAll { it.id == id }
        return queue.size != before
    }

    /**
     * Drops the oldest until the queue fits.
     *
     * No REMOVE is sent for what falls out — Gadgetbridge notes the same gap.
     * The watch keeps showing it and simply gets nothing back if it ever asks,
     * which is the same outcome as a notification the phone dismissed while
     * the link was down.
     */
    private fun evict() {
        while (queue.size > maxQueued) {
            queue.removeFirst()
        }
    }

    private fun countOf(category: GarminNotificationCategory): Int =
        queue.count { it.category == category }
}

/**
 * One attribute blob being streamed out: the bytes, how far they have got, and
 * the running CRC the watch checks each chunk against.
 *
 * The mirror image of `ActiveDownload` in `GarminSession.kt`, which does the
 * same bookkeeping for a file arriving.
 */
private class NotificationUpload(private val blob: ByteArray) {

    companion object {
        /**
         * The protocol's own ceiling, not the MTU's: the ML layer fragments a
         * frame to fit whatever MTU was negotiated, so this stays 300
         * regardless.
         */
        const val MAX_CHUNK_SIZE = 300

        /**
         * How many times one chunk may be repeated before the upload is
         * abandoned. One retry covers a dropped write; a watch asking twice is
         * telling us something the retry will not fix.
         */
        const val MAX_RESENDS = 1
    }

    private var offset = 0
    private var runningCrc = 0
    private var resends = 0

    /**
     * The chunk last handed to the transport, kept so a RESEND can repeat it
     * byte-for-byte with the CRC the watch was already told to expect.
     */
    var lastChunk: ByteArray = ByteArray(0)
        private set
    var lastOffset: Int = 0
        private set
    var lastCrc: Int = 0
        private set

    val totalSize: Int get() = blob.size
    val isComplete: Boolean get() = offset >= blob.size
    val canResend: Boolean get() = resends < MAX_RESENDS

    /** Takes the next chunk, advancing the offset and the running CRC. */
    fun take(): ByteArray {
        val end = if (offset + MAX_CHUNK_SIZE > blob.size) blob.size else offset + MAX_CHUNK_SIZE
        val chunk = blob.copyOfRange(offset, end)
        runningCrc = GarminCrc.compute(chunk, initialCrc = runningCrc)
        lastChunk = chunk
        lastOffset = offset
        lastCrc = runningCrc
        offset = end
        resends = 0
        return chunk
    }

    fun markResent() {
        resends++
    }
}
