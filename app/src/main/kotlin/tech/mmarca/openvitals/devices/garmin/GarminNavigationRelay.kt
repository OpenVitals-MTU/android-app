package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import tech.mmarca.openvitals.comaps.CoMapsGuidanceFeed
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState

/**
 * Puts CoMaps guidance onto a paired Garmin watch, as one notification updated
 * in place.
 *
 * A feature in its own right, and nothing to do with activity recording: the
 * wearer who wants the next turn on their wrist has not necessarily started a
 * session, and often does not want to. So this asks [CoMapsGuidanceFeed] for
 * guidance on its own behalf — whenever the per-watch "CoMaps guidance on
 * watch" toggle is on and a Garmin watch is actually paired — and follows what
 * comes back. A recording may be asking for the same feed for its own reasons;
 * neither can switch the other off, and while both are on, the wrist and the
 * phone's turn strip are reading the same state and cannot disagree.
 *
 * The Garmin-specific half is what happens next: Garmin has no turn-by-turn
 * channel a phone can drive, so guidance goes out as a notification. Another
 * vendor would put it somewhere else entirely, and would be another class at
 * this layer asking the same feed the same way — it needs nothing from here
 * and nothing from recording.
 *
 * The notification is withdrawn the moment guidance stops — the route ended,
 * the toggle went off, the watch was forgotten — so a finished route never
 * lingers on the wrist.
 */
@Singleton
class GarminNavigationRelay @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bridge: GarminNotificationBridge,
    private val deviceRepository: BleDeviceRepository,
    private val stateStore: GarminDeviceStateStore,
    private val guidanceFeed: CoMapsGuidanceFeed,
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(1),
    )
    private val policy = GarminNavigationRelayPolicy()
    private var started = false

    /**
     * Starts following the guidance feed, and asking for it. Called once from
     * the app's `onCreate`; Disabled is in the stream too, and is what takes
     * the notification down when a route ends.
     */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            guidanceFeed.guidance.collect { relay(it) }
        }
        // The paired watches are half the answer to "does anyone want this on
        // a wrist", so pairing or forgetting one changes it as much as the
        // toggle does. The flow replays its current value, which is also the
        // first ask.
        scope.launch {
            deviceRepository.devicesFlow.collect { syncGuidanceRequest() }
        }
    }

    /** Called when the toggle changes, so switching it off clears the wrist. */
    fun onEnabledChanged(deviceId: String, enabled: Boolean) {
        stateStore.setNavigationOnWatch(deviceId, enabled)
        syncGuidanceRequest()
        if (!enabled) scope.launch { relay(CoMapsNavigationState.Disabled) }
    }

    /**
     * Tells the guidance feed whether the wrist needs it up. This is what makes
     * the wrist independent of a recording: the feed runs because a watch is
     * waiting for it, not because a session is.
     */
    private fun syncGuidanceRequest() {
        guidanceFeed.request(CoMapsGuidanceFeed.Reason.WATCH, wantsGuidanceOnWatch())
    }

    private fun relay(state: CoMapsNavigationState) {
        val effective = if (wantsGuidanceOnWatch()) state else CoMapsNavigationState.Disabled
        when (val decision = policy.decide(effective, SystemClock.elapsedRealtime())) {
            is GarminNavigationRelayPolicy.Decision.Show -> {
                val notice = decision.notice.notice
                GarminLog.log(
                    "[GARMIN-NAV] ${if (decision.notice.isUpdate) "updating" else "showing"} " +
                        "\"${notice.title}\" ${notice.subtitle}",
                )
                bridge.postNavigation(
                    GarminNotification(
                        id = NAVIGATION_NOTIFICATION_ID,
                        packageName = context.packageName,
                        title = notice.title,
                        subtitle = notice.subtitle,
                        body = notice.body,
                        category = GarminNotificationCategory.LOCATION,
                        postedAt = LocalDateTime.now(),
                    ),
                )
            }
            GarminNavigationRelayPolicy.Decision.Withdraw -> {
                GarminLog.log("[GARMIN-NAV] guidance ended; withdrawing")
                bridge.withdrawNavigation(NAVIGATION_NOTIFICATION_ID)
            }
            GarminNavigationRelayPolicy.Decision.Nothing -> Unit
        }
    }

    private fun wantsGuidanceOnWatch(): Boolean {
        val watch = deviceRepository.devices.firstOrNull { it.isGarminGfdi } ?: return false
        return stateStore.navigationOnWatch(watch.id)
    }

    companion object {
        /**
         * Above the 31-bit ids the notification listener derives, so it can
         * never collide with a forwarded phone notification.
         */
        const val NAVIGATION_NOTIFICATION_ID = 0x8000_0001L
    }
}
