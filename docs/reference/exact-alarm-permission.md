# Exact alarm permission (`SCHEDULE_EXACT_ALARM`)

OpenVitals fires hydration and mindfulness reminders at a time the user chooses.
To deliver them at that time — rather than inside Android's inexact-alarm window,
which on Android 14+ was measured at **tens of minutes** wide — the app declares
`android.permission.SCHEDULE_EXACT_ALARM`.

This note records **which** exact-alarm permission we use, **why**, and what (if
anything) has to be declared to Google Play.

## Which permission, and why not the other one

Android offers two permissions that unlock exact alarms. They are **not**
interchangeable for a Play-distributed app:

| | `SCHEDULE_EXACT_ALARM` (what we use) | `USE_EXACT_ALARM` (avoided) |
|---|---|---|
| Granted | By the **user** (Settings). Denied by default on Android 14+. | **Auto-granted**, cannot be revoked. |
| Play policy | Broadly eligible. **No restricted-permission review.** | **Restricted**: allowed only for apps whose *core function* is an alarm clock, timer, or calendar. |
| Fit for a health app | Yes. | No — declaring it risks the app being **rejected or removed**. |

A health dashboard is not an alarm-clock/calendar app, so `USE_EXACT_ALARM` is
off the table. `SCHEDULE_EXACT_ALARM` gives the identical scheduling capability
with none of that risk; the only cost is that the user grants it themselves.

**Never add `USE_EXACT_ALARM` to the manifest.** See the manifest comment and
`AlarmManagerReminderScheduler`.

## Do we owe Google Play a declaration?

**No restricted-permission declaration is required for `SCHEDULE_EXACT_ALARM`.**
The Play "exact alarm" declaration flow — the one that demands a core-function
justification and is subject to review — is triggered by `USE_EXACT_ALARM`, which
we do not declare. `SCHEDULE_EXACT_ALARM` is user-granted and broadly eligible, so
it does not enter that review.

If a future Play Console prompt (or a reviewer) ever asks why the app schedules
exact alarms, this is the ready answer:

> OpenVitals is a personal health-tracking app. Users set a specific time for
> hydration and mindfulness reminders and expect them at that time. The app uses
> `SCHEDULE_EXACT_ALARM` (never the restricted `USE_EXACT_ALARM`) so a reminder set
> for, say, 9:40 is delivered at 9:40 rather than inside the OS inexact-alarm
> window, which we observed to be tens of minutes wide on Android 14+. The
> permission is user-granted and the app degrades gracefully to inexact alarms
> when it is not granted — reminders are never dropped.

## Runtime behaviour

- **Denied by default on Android 14+ (API 34).** On Android 12–13 it is granted by
  default (revocable); below Android 12 exact alarms need no permission at all.
- The reminder settings show a quiet "Use exact timing" nudge when a reminder is
  on but the permission is absent, opening the system screen that grants it
  (`requestExactAlarmsPermission`). There is no in-app grant dialog — Android only
  allows granting it from Settings.
- `AlarmManagerReminderScheduler` consults `canScheduleExactReminders()` on **every**
  schedule and downgrades to an inexact, Doze-surviving alarm when the permission
  is missing. This is mandatory, not cosmetic: `android_alarm_manager_plus`
  **silently drops** an exact alarm it lacks permission for (it logs and schedules
  nothing, with no fallback of its own), which would kill the self-perpetuating
  reminder chain. The scheduler's own downgrade is what keeps reminders alive.

## Data safety

Exact alarms schedule local notifications. They collect, transmit, and store no
data, so they add nothing to the Play Data Safety form.

## Sources

- [Schedule exact alarms are denied by default — Android Developers](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- [Use of exact alarms — Google Play Console Help](https://support.google.com/googleplay/android-developer/answer/16558241)
