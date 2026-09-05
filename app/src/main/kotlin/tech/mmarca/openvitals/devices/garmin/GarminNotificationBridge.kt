package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.RemoteInput
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.notifications.NotificationMsg
import tech.mmarca.openvitals.devices.notifications.NotificationStore
import tech.mmarca.openvitals.devices.notifications.OpenVitalsNotificationListenerService

/**
 * The seam between Android's notification listener and the Garmin stack:
 * drain the buffer, resolve the paired watch, convert each notification, and
 * perform what the wearer chose. Reads the registry and the buffer, writes
 * nothing. Runs on a single-threaded scope: the forwarder assumes it.
 */
@Singleton
class GarminNotificationBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: BleDeviceRepository,
    private val findPhoneRinger: GarminFindPhoneRinger,
    private val weatherStore: tech.mmarca.openvitals.devices.weather.WeatherStore,
    private val agpsStore: GarminAgpsStore,
    private val calendarSource: GarminCalendarSource,
    private val stateStore: GarminDeviceStateStore,
    private val syncService: GarminWatchSyncService,
    private val locationSource: GarminPhoneLocationSource,
    private val foregroundGate: tech.mmarca.openvitals.core.performance.AppForegroundGate,
    private val realtimeStore: GarminRealtimeStore,
) {

    private companion object {
        const val TAG = "OVNotifyBridge"

        /** What a live link streams. The watch offers more on the same mechanism. */
        val LIVE_SERVICES = setOf(
            GarminRealtimeService.HEART_RATE,
            GarminRealtimeService.STEPS,
        )
    }

    /** Sequential on purpose. `limitedParallelism(1)` is FIFO. */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(1),
    )

    private var forwarder: GarminNotificationForwarder? = null

    /** The address the current forwarder was built for. */
    private var forwarderAddress: String? = null

    /** Whether the current forwarder holds the link companion-style. */
    private var forwarderCompanion: Boolean = false

    private val identity = GarminPhoneIdentity()

    /** Called by the listener service after it buffered something. Thread-safe. */
    fun onNotificationsPending() {
        scope.launch { drain() }
    }

    /** Called after the configuration changed. Off or unpaired tears the link down. */
    fun onConfigChanged() {
        scope.launch {
            val config = NotificationStore.readConfig(context)
            val current = forwarder ?: return@launch
            if (!config.enabled || forwarderAddress != pairedWatchAddress()) {
                GarminLog.log("[GARMIN-NOTIFY] configuration changed; stopping the forwarder")
                dropForwarder()
            }
        }
    }

    /** Moves everything the listener buffered into the forwarder. */
    private fun drain() {
        val pending = NotificationStore.takePending()
        if (pending.isEmpty()) return
        val target = forwarderFor(pairedWatchAddress()) ?: run {
            GarminLog.log("[GARMIN-NOTIFY] no Garmin watch paired; nothing to forward to")
            return
        }
        GarminLog.log("[GARMIN-NOTIFY] draining ${pending.size} notification(s)")
        for (message in pending) {
            if (message.removed) {
                target.withdraw(message.id)
            } else {
                target.post(toNotification(message))
            }
        }
    }

    /** Posts an app-made notification (CoMaps guidance). A stable id makes each repost a MODIFY. */
    fun postNavigation(notification: GarminNotification) {
        scope.launch {
            val target = forwarderFor(pairedWatchAddress()) ?: run {
                GarminLog.log("[GARMIN-NAV] no Garmin watch paired; nothing to show guidance on")
                return@launch
            }
            target.post(notification)
        }
    }

    fun withdrawNavigation(notificationId: Long) {
        scope.launch {
            // Building a forwarder to withdraw nothing would open a link for no reason.
            forwarder?.withdraw(notificationId)
        }
    }

    /** The paired Garmin watch's address, or null. */
    private fun pairedWatchAddress(): String? =
        deviceRepository.devices.firstOrNull { it.isGarminGfdi }?.address

    /** Tears the held link down. Live readings die with the link that produced them. */
    private fun dropForwarder() {
        forwarder?.dispose()
        forwarder = null
        forwarderAddress = null
        forwarderCompanion = false
        realtimeStore.clear()
    }

    private fun forwarderFor(
        address: String?,
        companion: Boolean = false,
    ): GarminNotificationForwarder? {
        if (address == null) return null
        val current = forwarder
        if (current != null && forwarderAddress == address && forwarderCompanion == companion) {
            return current
        }
        // The watch changed under a live forwarder.
        if (current != null) dropForwarder()
        val created = GarminNotificationForwarder(
            scope = scope,
            address = address,
            phoneName = identity.bluetoothName,
            manufacturer = identity.manufacturer,
            model = identity.model,
            lease = SharedGarminRadioLease,
            openLink = { request -> GarminBleNotificationLink.open(context, scope, request) },
            onAction = ::performAction,
            onFindPhone = { seconds -> findPhoneRinger.start(seconds) },
            onFindPhoneCancel = { findPhoneRinger.stop() },
            weatherProvider = { weatherStore.freshSnapshot() },
            agpsSource = agpsStore.source(),
            calendarProvider = { begin, end ->
                val device = deviceRepository.devices
                    .firstOrNull { it.address.equals(address, ignoreCase = true) }
                if (device != null && stateStore.calendarSync(device.id)) {
                    calendarSource.events(begin, end)
                } else {
                    null
                }
            },
            onFileAnnounced = { syncAnnouncedFile(address) },
            locationProvider = { locationSource.lastKnown() },
            hostForeground = { foregroundGate.isForeground },
            realtimeServices = {
                val device = deviceRepository.devices
                    .firstOrNull { it.address.equals(address, ignoreCase = true) }
                if (device != null && stateStore.liveReadings(device.id)) {
                    LIVE_SERVICES
                } else {
                    emptySet()
                }
            },
            onRealtimeReading = { reading -> realtimeStore.record(reading) },
            setupWizardPending = {
                deviceRepository.devices
                    .firstOrNull { it.address.equals(address, ignoreCase = true) }
                    ?.let { stateStore.setupWizardPending(it.id) } == true
            },
            onSetupWizardCompleted = {
                deviceRepository.devices
                    .firstOrNull { it.address.equals(address, ignoreCase = true) }
                    ?.let { stateStore.setSetupWizardPending(it.id, false) }
            },
            // A companion forwarder never idles away: its link is the feature.
            onIdle = if (companion) {
                null
            } else {
                {
                    scope.launch { dropForwarder() }
                }
            },
        )
        forwarder = created
        forwarderCompanion = companion
        forwarderAddress = address
        return created
    }

    // Companion mode: the held link the watch was designed around.

    /** Restores companion mode after a process start. */
    fun onAppStart() {
        // Garmin watches hold their online errands until the companion is active.
        scope.launch {
            foregroundGate.foregroundFlow.collect { foreground ->
                GarminLog.log("[GARMIN-COMPANION] app is in the ${if (foreground) "foreground" else "background"}")
                forwarder?.setHostForeground(foreground)
            }
        }
        scope.launch {
            val device = deviceRepository.devices.firstOrNull {
                it.isGarminGfdi && stateStore.stayConnected(it.id)
            } ?: return@launch
            observePresence(device.address)
            ensureCompanionLink()
        }
    }

    /** Asks the OS to wake this app when the device advertises. The held link is the other half. */
    private fun observePresence(address: String) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
        runCatching {
            val manager = context.getSystemService(
                android.companion.CompanionDeviceManager::class.java,
            ) ?: return
            @Suppress("DEPRECATION")
            manager.startObservingDevicePresence(address)
            GarminLog.log("[GARMIN-COMPANION] observing presence of $address")
        }.onFailure {
            // No association: companion mode still works while the app lives.
            GarminLog.log("[GARMIN-COMPANION] presence observation refused: $it")
        }
    }

    private fun stopObservingPresence(address: String) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
        runCatching {
            val manager = context.getSystemService(
                android.companion.CompanionDeviceManager::class.java,
            ) ?: return
            @Suppress("DEPRECATION")
            manager.stopObservingDevicePresence(address)
        }
    }


    /** Opens or keeps the companion link. Safe to call repeatedly. */
    fun ensureCompanionLink() {
        scope.launch {
            val device = deviceRepository.devices.firstOrNull {
                it.isGarminGfdi && stateStore.stayConnected(it.id)
            } ?: return@launch
            GarminLog.log("[GARMIN-COMPANION] holding the link to ${device.displayName}")
            forwarderFor(device.address, companion = true)?.ensureLink()
        }
    }

    /** Called when the stay-connected toggle changes. Off tears the companion link down. */
    fun onStayConnectedChanged(deviceId: String, enabled: Boolean) {
        stateStore.setStayConnected(deviceId, enabled)
        scope.launch {
            val address = deviceRepository.devices.firstOrNull { it.id == deviceId }?.address
            if (enabled) {
                address?.let(::observePresence)
                ensureCompanionLink()
            } else if (forwarderCompanion) {
                address?.let(::stopObservingPresence)
                GarminLog.log("[GARMIN-COMPANION] released; link no longer held")
                dropForwarder()
            }
        }
    }

    /** Turns the watch's live streams on or off, now and for later links. */
    fun onLiveReadingsChanged(deviceId: String, enabled: Boolean) {
        stateStore.setLiveReadings(deviceId, enabled)
        scope.launch {
            if (!enabled) realtimeStore.clear()
            val current = forwarder ?: return@launch
            for (service in LIVE_SERVICES) {
                current.setRealtimeService(service, enabled)
            }
            GarminLog.log("[GARMIN-LIVE] live readings ${if (enabled) "on" else "off"}")
        }
    }

    fun onWatchAppeared(address: String) {
        GarminLog.log("[GARMIN-COMPANION] $address is in range")
        ensureCompanionLink()
    }

    /** A background sync already running for an announcement. */
    private var announcedSyncRunning = false

    /** The watch finished writing a recording: sync it now, in the background. */
    private fun syncAnnouncedFile(address: String) {
        scope.launch {
            if (announcedSyncRunning) return@launch
            val device = deviceRepository.devices
                .firstOrNull { it.address.equals(address, ignoreCase = true) }
                ?: return@launch
            announcedSyncRunning = true
            try {
                GarminLog.log("[GARMIN-COMPANION] announced file; syncing now")
                val result = syncService.sync(device, listenAfter = Duration.ZERO, onProgress = null)
                GarminLog.log("[GARMIN-COMPANION] announced-file sync: $result")
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-COMPANION] announced-file sync failed: $error")
            } finally {
                announcedSyncRunning = false
            }
        }
    }

    private fun toNotification(message: NotificationMsg): GarminNotification =
        GarminNotification(
            id = message.id,
            packageName = message.packageName,
            title = message.title ?: message.appLabel ?: message.packageName,
            subtitle = message.subtitle ?: "",
            body = message.body ?: "",
            category = categoryOf(message.categoryOrdinal),
            postedAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(message.whenEpochMillis),
                ZoneId.systemDefault(),
            ),
            actions = garminActionsFor(message),
        )

    /** An ordinal outside the enum means the capture side drifted; OTHER is the safe reading. */
    private fun categoryOf(ordinal: Int): GarminNotificationCategory =
        GarminNotificationCategory.entries.getOrElse(ordinal) {
            GarminNotificationCategory.OTHER
        }

    // Acting on a notification from the wrist.

    /**
     * Performs what the wearer chose. Dismiss is ours and maps onto the
     * listener's cancel; everything else is the app's own button, by index.
     */
    private suspend fun performAction(request: GarminNotificationActionRequest) {
        val performed = if (request.action.isSynthetic) {
            dismissNotification(request.notificationId)
        } else {
            performNotificationAction(
                request.notificationId,
                request.action.androidIndex,
                request.replyText,
            )
        }
        if (!performed) {
            // The notification is gone from the phone. Nothing to tell the watch.
            GarminLog.log(
                "[GARMIN-NOTIFY] \"${request.action.label}\" could not be " +
                    "performed on ${request.notificationId}",
            )
        }
    }

    private fun performNotificationAction(
        id: Long,
        actionIndex: Int,
        replyText: String?,
    ): Boolean {
        val sbn = NotificationStore.actionable(id)
        if (sbn == null) {
            Log.w(TAG, "no notification $id left to act on")
            return false
        }
        val notification = sbn.notification
        if (notification == null) {
            Log.w(TAG, "notification $id has no payload")
            return false
        }
        // The same enumeration the actions were captured from.
        val action = OpenVitalsNotificationListenerService
            .wristActions(notification)
            .getOrNull(actionIndex)
        if (action?.actionIntent == null) {
            Log.w(TAG, "notification $id has no action at $actionIndex")
            return false
        }
        return try {
            val remoteInputs = action.remoteInputs
            if (remoteInputs.isNullOrEmpty()) {
                // A plain button: fire its PendingIntent.
                action.actionIntent!!.send()
            } else {
                // A reply must be delivered through RemoteInput's results bundle,
                // or most apps post an empty message.
                val results = Bundle()
                for (remoteInput in remoteInputs) {
                    results.putCharSequence(remoteInput.resultKey, replyText ?: "")
                }
                // Filled in from a service with no task of its own.
                val intent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                RemoteInput.addResultsToIntent(remoteInputs, intent, results)
                // Some apps read the reply as free-form text only.
                RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_FREE_FORM_INPUT)
                action.actionIntent!!.send(context, 0, intent)
            }
            // GarminLog is silent in release builds.
            GarminLog.log("[GARMIN-NOTIFY] performed action $actionIndex on $id")
            GarminLog.log("[GARMIN-NOTIFY]   that action was \"${action.title}\"")
            true
        } catch (error: Throwable) {
            // Normal for a notification the app has already torn down.
            Log.w(TAG, "could not perform action $actionIndex on $id: $error")
            false
        }
    }

    private fun dismissNotification(id: Long): Boolean {
        val sbn = NotificationStore.actionable(id)
        if (sbn == null) {
            Log.w(TAG, "no notification $id left to dismiss")
            return false
        }
        val service = OpenVitalsNotificationListenerService.instance
        if (service == null) {
            // Only when the OS has unbound us, so notification access is off anyway.
            Log.w(TAG, "not bound; cannot dismiss $id")
            return false
        }
        val dismissed = service.dismiss(sbn.key)
        if (dismissed) NotificationStore.forget(id)
        return dismissed
    }
}
