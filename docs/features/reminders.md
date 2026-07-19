# Reminders

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/hydration/reminders/`, `lib/features/mindfulness/reminders/`, `lib/bootstrap/reminder_bootstrap.dart`, `lib/features/settings/`. Reminders are inexact, Doze-surviving alarms (`setAndAllowWhileIdle`) scheduled with `android_alarm_manager_plus`; the alarm isolate wakes the app, re-checks today's actual intake, and only then notifies. They are inexact on purpose -- exact alarms would require `USE_EXACT_ALARM`, which Google restricts to alarm-clock and calendar apps.
> **Navigation:** the reminder cards live at the bottom of the metric detail screens — hydration on `/metric/HYDRATION`, mindfulness on `/metric/MINDFULNESS`. They are **not** in a Settings section (only the hydration goal and caffeine model live under `/settings/nutrition`).
> **Related:** [Feature map](feature-map.md), [Hydration](hydration.md), [Mindfulness](mindfulness.md).

OpenVitals reminders are local device notifications for supported wellness workflows.

## How to use it

Both reminders are **off by default** and are turned on from the reminder card at the bottom of their metric's detail screen.

### Turn on beverage (hydration) reminders

1. Open the **Hydration** detail (dashboard tile) and scroll to the reminder card.
2. Flip the master switch on. Set the **Reminder interval** (a stepper, every 30–240 minutes, default 120) and the **Active from** / **Active until** times (default 07:00–23:00).
3. The summary then reads **"Every N min • start–end"**. Reminders **pause once you hit today's hydration goal** and resume the next day, and logging a drink hides an active reminder.

### Turn on mindfulness reminders

1. Open the **Mindfulness** detail and scroll to the reminder card.
2. Flip the master switch on and pick a single **Reminder time** (default 18:00). The summary reads **"Daily at HH:MM"** — one reminder a day, no interval or window.

### Permissions

- **Notifications (Android 13+):** turning a reminder on prompts for notification permission first; the switch only sticks if you allow it. If denied, the card shows a **Grant permission** button (and, if permanently denied, an **Open notification settings** link).
- **Exact timing (Android 12+):** Android has no pop-up for this. If exact alarms aren't allowed, reminders still fire but may arrive a few minutes late; the card offers a **Use exact timing** button that opens the system screen. OpenVitals uses inexact, Doze-friendly alarms on purpose, so reminders survive battery optimization.

## Hydration Reminders

Hydration reminders can use an active time window and interval schedule. They pause after the daily hydration goal is reached and resume on the next day. Saving a hydration entry can automatically hide an active hydration reminder.

## Mindfulness Reminders

Mindfulness reminders can help users return to timer or session logging workflows. Reminder settings stay local to the device.

## Android Permissions

On Android versions that require it, OpenVitals asks for notification permission before showing reminders. Boot completion permission is used to restore local schedules after reboot or app update.

## Privacy

Reminder preferences are stored locally. Reminders do not upload health data and do not require an OpenVitals account.
