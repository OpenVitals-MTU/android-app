# Privacy, Support, And Diagnostics

> **Status:** Current implemented behavior and support policy.
> **Audience:** Users and contributors.
> **Implementation:** `features/settings`, diagnostics-gated settings, local support docs.
> **Navigation:** `Screen.SettingsDebugDiagnostics` in diagnostics builds, Settings support links.
> **Related:** [Feature map](feature-map.md), [Privacy](../app/privacy.md), [Support](../app/support.md).

OpenVitals is designed as a local-first Health Connect app.

For the standalone privacy reference, see [Privacy](../app/privacy.md). For support-oriented questions, see [FAQ](../app/faq.md).

## Privacy Model

OpenVitals does not require:

- An OpenVitals account.
- OpenVitals cloud sync.
- OpenVitals analytics.
- Ads.

Health data is read from and written to Health Connect on device. OpenVitals stores local preferences such as widget order, goals, reminders, calibration, and display choices.

A small amount of health data lives in the app's own database rather than in Health Connect, because Health Connect has no record type for it: the watch-only measurements such as stress, Body Battery, and training load. See [Watches](watches.md).

## Internet Boundary

The app does not need app-level internet access for normal health features. Offline map packs, imports, widgets, and Health Connect reads are handled locally.

The connected-device features keep that boundary rather than widening it. A watch is read over Bluetooth, notifications are forwarded to the watch over Bluetooth, and sync with another phone deliberately uses Bluetooth Classic because any Wi-Fi or TCP socket on Android would require the internet permission the app does not declare.

## Notification Access

Forwarding phone notifications to a watch is the only feature that reads notifications, and it is off until it is turned on. OpenVitals shows a prominent disclosure of what it will read and where it goes before sending the user to Android's own notification access screen.

Notification text is used only to display the notification on the paired watch. It is held in memory while it is needed, is never written to a file or a database, and can only reach the paired watch over Bluetooth. Individual apps can be blocked, the feature can be switched off in OpenVitals, and access can be revoked in Android settings.

## Diagnostics

Diagnostics surfaces help users and maintainers understand local issues without sending health data automatically. They can include app version information, debug build separation, import reports, logs, and crash-report email drafts. Each holds structure, not data: counts, types, reasons and masked error text. See [Text that leaves the app is structure, not data](../engineering/architecture.md#6-text-that-leaves-the-app-is-structure-not-data).

Diagnostics builds add two cards that are useful when data appears to be missing:

- A reminder test, which posts the hydration reminder notification immediately and exactly as a scheduled one would.
- Health Connect sources, which lists the apps that contributed heart-rate and sleep records over the last seven days. It is the quickest way to tell whether a paired watch's data is actually landing in Health Connect.

Apple Health import reports are explicit user downloads with the importer summary, logs, grouped diagnostics, worker logs, and on failure the masked exception stack; the per-record diagnostic log is capped per source (see [Apple Health import](apple-health-import.md#reports-and-diagnostics)) so a large repeated import cannot make the report unbounded, but grouped diagnostic counts stay complete. They hold no record's value or time, no source's name and no file location.

Diagnostics builds export the process logcat from Settings → Debug diagnostics. Every line and tag is kept, so Health Connect and sync issues can be traced, and each line is masked: addresses, identifiers, file locations and dates, and an exception's message whole. The crash and Report an issue drafts carry no log in any build: the saved crash report holds the masked crash, is attached for seven days at most, and is deleted once it has gone into an email, a copy or a file. Copied reports are marked sensitive.

## Support Links

Settings and project metadata can point users toward support and community resources such as Zulip, GitHub, or donation links.

## Health Disclaimer

OpenVitals insights are wellness context. Scores, readiness estimates, cardio load, Body Energy, and sleep explanations are not medical diagnosis or treatment advice.

Medical records are shown as they were received. OpenVitals does not interpret them, and the health disclaimer says so.
