# Reminders

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/hydration/reminders/`, `lib/features/mindfulness/reminders/`, `lib/bootstrap/reminder_bootstrap.dart`, `lib/features/settings/`. Reminders are inexact, Doze-surviving alarms (`setAndAllowWhileIdle`) scheduled with `android_alarm_manager_plus`; the alarm isolate wakes the app, re-checks today's actual intake, and only then notifies. They are inexact on purpose -- exact alarms would require `USE_EXACT_ALARM`, which Google restricts to alarm-clock and calendar apps.
> **Navigation:** hydration detail, mindfulness detail, reminder-related settings.
> **Related:** [Feature map](feature-map.md), [Hydration](hydration.md), [Mindfulness](mindfulness.md).

OpenVitals reminders are local device notifications for supported wellness workflows.

## Hydration Reminders

Hydration reminders can use an active time window and interval schedule. They pause after the daily hydration goal is reached and resume on the next day. Saving a hydration entry can automatically hide an active hydration reminder.

## Mindfulness Reminders

Mindfulness reminders can help users return to timer or session logging workflows. Reminder settings stay local to the device.

## Android Permissions

On Android versions that require it, OpenVitals asks for notification permission before showing reminders. Boot completion permission is used to restore local schedules after reboot or app update.

## Privacy

Reminder preferences are stored locally. Reminders do not upload health data and do not require an OpenVitals account.
