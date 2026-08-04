package tech.mmarca.openvitals.core.performance

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.features.mindfulness.reminders.MindfulnessReminderController

/**
 * Re-plans both reminder schedules every time the app comes to the foreground.
 *
 * `AlarmManager` alarms are durable but not indestructible, and the app cancels
 * its own on a path it never undoes: when a reminder fires without
 * `POST_NOTIFICATIONS`, both controllers call `clearReminder()` and return
 * before rescheduling. Since Android 12 that permission goes away on its own —
 * "Pause app activity if unused" is on by default — so the sequence is
 * ordinary: reminders on, permission auto-revoked, the next alarm fires and
 * cancels itself, the user re-grants notifications in system settings, and
 * nothing ever arms an alarm again. The toggle still reads "on". A force-stop
 * or an OEM battery killer leaves the same permanent silence by cancelling the
 * alarms directly.
 *
 * The boot receivers only cover reboot, reinstall, and clock or timezone
 * changes — none of which is the case above. Opening the app is the one event
 * that always follows the user noticing something is wrong, so it is where the
 * recovery belongs.
 *
 * Each controller is restored inside its own `runCatching`: a failure in one
 * must not stop the other being re-planned. Restoring is idempotent — it reads
 * the stored config and arms one alarm — so doing it on every foreground costs
 * a preference read and a `setAndAllowWhileIdle`.
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
