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

## Internet Boundary

The app does not need app-level internet access for normal health features. Offline map packs, imports, widgets, and Health Connect reads are handled locally.

The connected-device features keep that boundary rather than widening it. Live BLE sensors are read over Bluetooth, and sync with another phone deliberately uses Bluetooth Classic because any Wi-Fi or TCP socket on Android would require the internet permission the app does not declare.

## Diagnostics

Diagnostics surfaces help users and maintainers understand local issues without sending health data automatically. They can include app version information, debug build separation, import reports, logs, and crash-report email drafts.

Diagnostics builds add two cards that are useful when data appears to be missing:

- A reminder test, which posts the hydration reminder notification immediately and exactly as a scheduled one would.
- Health Connect sources, which lists the apps that contributed heart-rate and sleep records over the last seven days. It is the quickest way to tell whether a watch's companion app (e.g. Gadgetbridge) is actually landing data in Health Connect.

Apple Health import reports are explicit user downloads and intentionally include the full importer summary, logs, grouped diagnostics, worker logs, and full exception stacks on failure; the raw per-record diagnostic log is capped per source (see [Apple Health import](apple-health-import.md#reports-and-diagnostics)) so a large repeated import cannot make the report unbounded, but grouped diagnostic counts stay complete. They are not the same as sanitized debug diagnostics logs and should be shared only when the user is comfortable sharing the included export-derived details.

## Support Links

Settings and project metadata can point users toward support and community resources such as Zulip, Codeberg, or donation links.

## Health Disclaimer

OpenVitals insights are wellness context. Scores, readiness estimates, cardio load, Body Energy, and sleep explanations are not medical diagnosis or treatment advice.
