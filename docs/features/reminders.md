# Reminders

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/hydration/reminders/`, `lib/features/mindfulness/reminders/`, `lib/core/reminders/`, `lib/bootstrap/reminder_bootstrap.dart`. Reminders are **pre-scheduled local notifications**: on app start, on resume, when a drink is logged, and on a settings change, the app computes the upcoming batch of reminder times and schedules them with `flutter_local_notifications`. Its boot receiver re-arms scheduled notifications across a reboot **and** an app update, so a reminder is not lost when the app is replaced (a nightly install used to cancel the old alarm and leave nothing armed). Delivery is exact when `SCHEDULE_EXACT_ALARM` is granted and inexact (Doze-surviving) otherwise -- never `USE_EXACT_ALARM`, which Google restricts to alarm-clock and calendar apps. `android_alarm_manager_plus` remains only for the home-screen-widget refresh.
> **Navigation:** the reminder cards live at the bottom of the metric detail screens — hydration on `/metric/HYDRATION`, mindfulness on `/metric/MINDFULNESS`. They are **not** in a Settings section (only the hydration goal and caffeine model live under `/settings/nutrition`).
> **Related:** [Feature map](feature-map.md), [Hydration](hydration.md), [Mindfulness](mindfulness.md).

OpenVitals reminders are local device notifications for supported wellness workflows.

## How to use it

Both reminders are **off by default** and are turned on from the reminder card at the bottom of their metric's detail screen.

### Turn on beverage (hydration) reminders

1. Open the **Hydration** detail (dashboard tile) and scroll to the reminder card.
2. Flip the master switch on. Set the **Reminder interval** (a stepper, every 30–240 minutes, default 120) and the **Active from** / **Active until** times (default 07:00–23:00).
3. The summary then reads **"Every N min • start–end"**. The countdown is **anchored to your last logged drink** — logging resets it, so you are nudged after a real gap rather than on a fixed clock. Reminders **pause once you hit today's hydration goal** and resume the next day.
4. Today's reminders show your progress (e.g. **"1.3 L / 2.0 L"**) with a progress bar, and **tapping a reminder opens the hydration entry form** so you can log a drink straight away.

### Turn on mindfulness reminders

1. Open the **Mindfulness** detail and scroll to the reminder card.
2. Flip the master switch on and pick a single **Reminder time** (default 18:00). The summary reads **"Daily at HH:MM"** — one reminder a day, no interval or window.

### Permissions

- **Notifications (Android 13+):** turning a reminder on prompts for notification permission first; the switch only sticks if you allow it. If denied, the card shows a **Grant permission** button (and, if permanently denied, an **Open notification settings** link).
- **Exact timing (Android 12+):** Android has no pop-up for this. When exact alarms aren't allowed, reminders still fire but may arrive inside a short window rather than at the minute; the card offers a **Use exact timing** button that opens the system screen. Either way reminders survive Doze/battery optimization.

## Hydration Reminders

Hydration reminders use an active time window and interval schedule, anchored to your last logged drink. They pause after the daily hydration goal is reached and resume on the next day. Logging a drink — in the app or from the 1x1 home-screen widget — re-anchors the reminder to that drink and reschedules, which also clears the reminder that prompted the log. Same-day reminders carry today's progress and a progress bar, and tapping one opens the hydration entry form.

## Mindfulness Reminders

Mindfulness reminders can help users return to timer or session logging workflows. Reminder settings stay local to the device.

## Android Permissions

On Android versions that require it, OpenVitals asks for notification permission before showing reminders. Scheduled reminders are restored automatically after a reboot or an app update by the notification framework's boot receiver, and the app also re-plans them each time it opens.

## Privacy

Reminder preferences are stored locally. Reminders do not upload health data and do not require an OpenVitals account.
