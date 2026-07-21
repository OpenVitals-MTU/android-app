# Local And Connected Editions

OpenVitals keeps the local Health Connect app separate from connected or online app work.

## Local App

The local app is this repository's Health Connect app.

- No OpenVitals account.
- No OpenVitals server dependency.
- No app-level internet permission.
- Reads Health Connect records on device.
- Writes only records the user explicitly saves, imports, records, edits, or deletes.
- Keeps local preferences such as units, language, theme, widget order, goals, calibration, and reminders.
- May move records **directly between two phones over Bluetooth** ([Sync with another phone](../features/device-sync.md)). This is peer-to-peer and still ships **no internet permission** — Bluetooth Classic RFCOMM needs none, unlike any Wi-Fi/socket transport — so it stays inside the local boundary rather than being a connected feature.

This repository should preserve that boundary for the local app.

## Connected App

Connected features should live in a separate app and repository.

Possible connected features include accounts, sharing, and social workflows. Those features should not be mixed into the local app's Health Connect-only runtime.

## Why Separate Them?

The split keeps the local app easier to reason about:

- Users can verify that the local app has no internet access.
- Health Connect data does not silently cross into account or sharing code.
- Release pipelines can prove which app they build.
- Connected features can evolve without weakening the local app's privacy boundary.

## Shared Code

Shared implementation should move only into stable libraries or artifacts when it is mature enough. The local app should not depend on connected-app behavior, accounts, or servers.
