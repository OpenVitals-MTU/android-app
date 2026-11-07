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
 * Puts CoMaps guidance on a Garmin watch as one notification updated in
 * place. Independent of activity recording: it asks [CoMapsGuidanceFeed] on
 * its own behalf whenever the per-watch toggle is on and a Garmin watch is
 * paired. Garmin has no turn-by-turn channel, hence a notification. The
 * notification is withdrawn the moment guidance stops.
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

    /** Starts following the feed. Called once from `onCreate`. Disabled takes the notification down. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            guidanceFeed.guidance.collect { relay(it) }
        }
        // Pairing or forgetting a watch changes the answer as much as the toggle.
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

    /** Tells the feed whether the wrist needs it. The feed runs because a watch waits, not a session. */
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
        /** Above the 31-bit listener ids, so it never collides with a forwarded notification. */
        const val NAVIGATION_NOTIFICATION_ID = 0x8000_0001L
    }
}
