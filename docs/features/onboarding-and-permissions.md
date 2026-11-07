# Onboarding And Permissions

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/onboarding`, `healthconnect`, `features/settings`.
> **Navigation:** `Screen.Onboarding`, `Screen.SettingsHealthConnect`.
> **Related:** [Feature map](feature-map.md), [Permissions](../app/permissions.md), [Health Connect](../app/health-connect.md).

Onboarding prepares OpenVitals for local Health Connect use without requiring an account or cloud sync.

For the exact permission reference, see [Permissions](../app/permissions.md). For platform behavior, see [Health Connect](../app/health-connect.md).

## First Run

Onboarding introduces the app, checks Health Connect availability, and helps the user grant useful permission groups. It runs as a short sequence of steps, and a step that does not apply to the device is skipped entirely:

1. Permission categories. A "Grant all" button requests every category on this screen in one Health Connect dialog; the categories can still be granted one at a time. The button covers only this first screen; mindfulness, cycle tracking, and the additional-access step keep their own explicit choices.
2. Mindfulness, when the device's Health Connect version supports it.
3. Cycle tracking, when it is available.
4. Additional data access, which explains the settings that have to be changed inside Health Connect itself.

OpenVitals can continue with limited data when some permissions are skipped. Missing permissions are surfaced later on the dashboard, detail screens, settings, imports, and entry flows where they matter.

## Health Connect Availability

OpenVitals handles Health Connect states explicitly:

- Available and ready.
- Available but missing permissions.
- Not installed or not reachable on supported devices.
- Unsupported Android or device environments.

When Health Connect is not available, the app explains the limitation instead of showing misleading health values.

## Read And Write Permissions

Read permissions are used for dashboard widgets, metric detail screens, readiness views, statistics, achievements, and local insights.

Write permissions are requested lazily for explicit write flows such as manual entry, route import, Apple Health import, activity recording, edits, and deletes.

The dashboard remains read-only even when write permissions are granted.

## Optional Areas

Some permission groups, such as cycle data or mindfulness sessions, are shown only when they are relevant and requestable on the device.

Body Energy calibration and the body profile are not part of onboarding. They are set later in settings.

## Android Permissions Beyond Health Connect

Android's own permissions are requested where and when the feature that needs them is used, never at first run:

- Location, for GPS activity recording.
- Nearby devices, for Bluetooth LE sensors, for pairing a watch, and for sync with another phone.
- Notifications, for reminders and for recording, import, and sync progress.
- Notification access, only for forwarding phone notifications to a watch, and only after an in-app disclosure. Android grants it from its own settings screen.

The app declares no internet permission and removes the ones its dependencies would otherwise contribute. See [Permissions](../app/permissions.md).

## Privacy Expectations

OpenVitals stores app preferences locally and reads or writes health records through Health Connect. It does not require an OpenVitals account.
