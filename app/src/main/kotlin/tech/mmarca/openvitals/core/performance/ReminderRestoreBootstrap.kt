package tech.mmarca.openvitals.core.performance

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.features.mindfulness.reminders.MindfulnessReminderController

/**
 * Re-plans both reminder schedules every time the app comes to the
 * foreground. A reminder firing without POST_NOTIFICATIONS cancels its own
 * alarm, and Android 12 auto-revokes that permission, so reminders went
 * silent for good. Restoring is idempotent and cheap.
 */
@Singleton
class ReminderRestoreBootstrap @Inject constructor(
    private val hydrationReminderController: HydrationReminderController,
    private val mindfulnessReminderController: MindfulnessReminderController,
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        runCatching { hydrationReminderController.restoreSchedule() }
        runCatching { mindfulnessReminderController.restoreSchedule() }
    }
}
