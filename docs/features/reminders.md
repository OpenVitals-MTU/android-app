# Reminders

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/hydration/reminders`, `features/mindfulness/reminders`, `features/cycle/reminders`, `features/settings`.
> **Navigation:** hydration detail, mindfulness detail, Settings → Cycle, reminder-related settings.
> **Related:** [Feature map](feature-map.md), [Hydration](hydration.md), [Mindfulness](mindfulness.md), [Cycle tracking](cycle-tracking.md).

OpenVitals reminders are local device notifications for supported wellness workflows.

## Hydration Reminders

Hydration reminders can use an active time window and interval schedule. They pause after the daily hydration goal is reached and resume on the next day. Saving a hydration entry can automatically hide an active hydration reminder.

The countdown is anchored to the last logged drink rather than to a fixed clock, so logging a drink pushes the next reminder out by a full interval and the user is nudged after a real gap.

The reminder itself shows today's intake against the daily goal with a progress bar. Tapping it opens the app.

## Quick-Add Actions

A hydration reminder carries two one-tap add buttons, so a drink can be logged from the notification without opening the app. The offered volumes are the most recently used container sizes, padded with the last custom amount and then a glass and a bottle, so there are always two valid sizes. Metric volumes are labelled in millilitres; imperial volumes use the app's fluid-ounce formatting.

## Mindfulness Reminders

Mindfulness reminders can help users return to timer or session logging workflows. Reminder settings stay local to the device.

## Cycle Reminders

Cycle reminders are off by default and are configured in Settings → Cycle. Three kinds exist:

- **Daily log** at a chosen time (21:00 by default). It is skipped when the day already has a log.
- **Upcoming range**, 1 to 3 days before the estimated next period range. It exists only while there is an estimate.
- **Late cycle**, 0 to 7 days after the estimated range ends without a new period start.

What the notification shows is a separate choice: **concealed** (a neutral "Reminder" text that reveals nothing, the default), **descriptive** (says why the reminder came, only on an unlocked screen), or **custom** (the user's own title and message). The lock-screen version is always neutral.

The schedule is recomputed after every day-log save, delete, exclusion or backfill and every settings change, so a reminder that no longer applies is cancelled rather than shown late. Turning the reminders on asks for the notification permission first and enables them once it is granted. The estimate is read strictly and only with the background-read grant; without it, or when Health Connect cannot answer, the window and late-cycle alarms already armed stay as they are and are re-planned the next time the app opens, like the other reminders.

## Surviving Reboots And Clock Changes

Reminders are scheduled as wall-clock alarms, so the schedule is re-armed after:

- A device reboot.
- An app update that replaces the installed package.
- A time zone change.
- A manual or network clock change.

Without the re-arm, an alarm set before the change would fire at the old absolute instant, which is the wrong time of day. Hydration, mindfulness, and cycle reminders are restored the same way.

## Android Permissions

On Android versions that require it, OpenVitals asks for notification permission before showing reminders. Boot completion permission is used to restore local schedules after reboot or app update.

## Privacy

Reminder preferences are stored locally. Reminders do not upload health data and do not require an OpenVitals account.
