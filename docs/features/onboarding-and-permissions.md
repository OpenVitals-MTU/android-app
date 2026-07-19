# Onboarding And Permissions

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/onboarding/`, `lib/data/source/health/` (`health_data_source.dart`), `lib/domain/health/health_permissions.dart`, `lib/ui/components/health_connect_gate.dart`, `lib/features/settings/`.
> **Navigation:** `/onboarding` (the start destination until onboarding is completed), `/settings/health_connect`.
> **Related:** [Feature map](feature-map.md), [Permissions](../app/permissions.md), [Health Connect](../app/health-connect.md).

Onboarding prepares OpenVitals for local Health Connect use without requiring an account or cloud sync.

For the exact permission reference, see [Permissions](../app/permissions.md). For platform behavior, see [Health Connect](../app/health-connect.md).

## How to use it

### First launch

1. OpenVitals opens on the onboarding screen (it stays here until onboarding is complete). Pick your **language** from the top-right dropdown if needed; the app re-renders immediately.
2. Read the three cards — **Privacy first** (no account, data stays on device), **Powered by Health Connect**, and the **Health disclaimer**.
3. If Health Connect isn't usable, you'll see an **Install Health Connect** button (or, on unsupported devices, an explanation) instead of the permission controls.

### Grant permissions

1. Tap **Grant required Health Connect permissions**. This asks only for the minimum to get started — **activity, sleep, and heart rate**.
2. Once the minimum is granted, that button becomes **Continue** (which finishes onboarding and opens the dashboard), and a **Grant remaining available permissions** button appears to allow everything else optional in one step.
3. Prefer to be selective? Use the **per-category rows** under **Health Connect permissions** — each has a **Grant**, **Review** (partly granted), or **Open** action and describes exactly what it will request.
4. **Health history, background access, and exercise routes** live under the **Additional data access** category. Android can't grant these from the in-app dialog, so its **Open settings** action sends you to the Health Connect settings page to enable them manually.

You don't have to grant everything — the app runs with whatever you allow, and screens for ungranted categories simply stay empty. You can add or change permissions any time at **Settings › Health Connect**.

## First Run

Onboarding introduces the app, checks Health Connect availability, and helps the user grant useful permission groups.

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

Some permission groups, such as cycle data or sensor-related permissions, are shown only when they are relevant and requestable on the device.

Body Energy calibration can be collected during onboarding and adjusted later in settings.

## Privacy Expectations

OpenVitals stores app preferences locally and reads or writes health records through Health Connect. It does not require an OpenVitals account.
