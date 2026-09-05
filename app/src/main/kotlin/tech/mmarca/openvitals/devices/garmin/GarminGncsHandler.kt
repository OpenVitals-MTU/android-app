package tech.mmarca.openvitals.devices.garmin

/**
 * Holds the notifications the watch might ask about, announces new ones and
 * serves the attribute blobs it requests. Transport-free, like
 * [GarminSession]. GNCS is a pull protocol: [post] only announces; the text
 * leaves the phone if and when the watch asks, which may be never.
 * Ported from Gadgetbridge's `NotificationsHandler` (AGPLv3); deviations
 * are noted at [handleDataStatus].
 */
class GarminGncsHandler(
    /** Hands one built GFDI frame to the transport below. */
    private val send: suspend (ByteArray) -> Unit,
    /** Invoked when the wearer acts on a notification. The handler stays platform-free. */
    private val onAction: (suspend (GarminNotificationActionRequest) -> Unit)? = null,
    /**
     * How many notifications stay answerable. Gadgetbridge's number: a wrist
     * shows about ten, and the watch asks by id long after the announcement.
     */
    private val maxQueued: Int = 10,
) : GarminNotificationsHandler {

    /** Whether the watch has subscribed. Until then everything here is a no-op. */
    var enabled: Boolean = false
        private set

    /** Oldest first, so [evict] drops the least likely to be asked about. */
    private val queue = ArrayDeque<GarminNotification>()

    /** Announced the moment the watch subscribes. See [setEnabled]. */
    private val awaitingSubscription = mutableListOf<GarminNotification>()

    private var upload: NotificationUpload? = null

    /** The notifications still answerable, oldest first. Diagnostic only. */
    val queued: List<GarminNotification> get() = queue.toList()

    /**
     * Announcements accepted but never sent because the watch had not
     * subscribed. The forwarder re-queues them for the next link.
     */
    val held: List<GarminNotification> get() = awaitingSubscription.toList()

    /** Records whether the watch accepts notifications. Announces nothing; see [flushHeld]. */
    override fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        GarminLog.log("[GARMIN-NOTIFY] forwarding ${if (enabled) "enabled" else "disabled"}")
        if (!enabled) upload = null
    }

    /**
     * Announces everything that arrived before the watch subscribed. The
     * link is opened to announce, so the announcement is always ready before
     * the subscription lands. Called after the subscription status goes out.
     */
    override suspend fun flushHeld() {
        if (!enabled) return
        val waiting = awaitingSubscription.toList()
        awaitingSubscription.clear()
        for (notification in waiting) {
            // Skip anything evicted while it waited; it could not be answered.
            if (find(notification.id) == null) continue
            announce(notification, isUpdate = false)
        }
    }

    /** Announces [notification]. An id already queued goes out as MODIFY, not ADD. */
    suspend fun post(notification: GarminNotification) {
        val isUpdate = removeQueued(notification.id)
        queue.addLast(notification)
        evict()

        if (!enabled) {
            // Held, not dropped. Logged: notifications switched off on the watch
            // is the most likely reason nothing reaches the wrist.
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
        // Logged: an announcement has no visible effect on its own.
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
                // Without this the watch draws no action controls at all.
                hasActions = notification.hasActions,
            ),
        )
    }

    /** Withdraws a dismissed notification. Silent for an id no longer queued. */
    suspend fun remove(id: Long) {
        if (!enabled) {
            // Withdraw it from the held list too. Logged, or a run of silent
            // dismissals reads as a link that did nothing.
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

    /** Answers a control request. The caller has already sent the control status. */
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
                // Untested in Gadgetbridge; no watch here has sent one. Logged.
                GarminLog.log(
                    "[GARMIN-NOTIFY] app attributes requested for " +
                        "${message.appIdentifier}; not implemented",
                )
                return
            }
        }

        val notification = find(message.notificationId)
        if (notification == null) {
            // The notification aged out of the queue. There is no protocol way
            // to say so, and an error status would abort a transfer never started.
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
     * Matches what the watch invoked to what was offered. A legacy action
     * is the accept/refuse pair (ordinals 0 and 1); refuse maps to dismiss.
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
     * Drives the chunked upload from the watch's per-chunk verdict. Unlike
     * Gadgetbridge, RESEND is honoured once, and OFFSET_MISMATCH abandons
     * rather than guesses: the status carries no offset.
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

    /** Forgets everything. Called when a link ends. */
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

    /** Drops the oldest until the queue fits. No REMOVE is sent, as in Gadgetbridge. */
    private fun evict() {
        while (queue.size > maxQueued) {
            queue.removeFirst()
        }
    }

    private fun countOf(category: GarminNotificationCategory): Int =
        queue.count { it.category == category }
}

/** One attribute blob being streamed out. The mirror of `ActiveDownload` in GarminSession. */
private class NotificationUpload(private val blob: ByteArray) {

    companion object {
        /** The protocol's ceiling, not the MTU's: the ML layer fragments frames. */
        const val MAX_CHUNK_SIZE = 300

        /** Resends before the upload is abandoned. One covers a dropped write. */
        const val MAX_RESENDS = 1
    }

    private var offset = 0
    private var runningCrc = 0
    private var resends = 0

    /** The last chunk sent, so a RESEND repeats it byte for byte. */
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
