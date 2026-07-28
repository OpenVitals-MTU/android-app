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

1. Tap **Grant Health Connect access**. This asks — in a single request — for every permission the app uses that your device's Health Connect can actually grant.
2. **All of them are required.** The button only becomes **Continue** (which finishes onboarding and opens the dashboard) once nothing is outstanding. If you leave something out of the dialog, tap the button again to be asked once more.
3. Afterwards you are sent to the **Health Connect settings page** once, to finish the things Android's in-app dialog cannot grant: **health history, background access, and exercise routes** (the **Additional data access** row). These never block **Continue**.
4. Two groups are **optional** and listed as their own rows: **Cycle tracking** (menstruation, ovulation, sexual activity and the rest) and **Mindfulness**. Grant them from their row if you want them.

**Mindfulness** additionally has an opt-in switch, and it only appears on devices whose Health Connect reports the feature. It is **off by default**: some Health Connect versions report support for mindfulness but cannot draw its permission row, and asking for it crashes the system Health Connect app — after which nothing can be granted at all. Turning the switch on adds a Mindfulness row that requests it *on its own*, so if your device can't cope, nothing else is affected. If Health Connect crashes, turn the switch back off.

The **per-category rows** under **Health Connect permissions** show what each group covers and how much of it is granted, with a **Grant**, **Review** (partly granted), or **Open** action. You can change any permission later at **Settings › Health Connect**.

If a permission your device's Health Connect doesn't recognise, the app never asks for it — it is filtered out before the dialog opens, and the category is shown as **Not supported** rather than as something you failed to grant.

## First Run

Onboarding introduces the app, checks Health Connect availability, and asks for the permission set the app needs in one request.

Onboarding does not finish until that set is granted. The two optional groups (cycle tracking, mindfulness) and the settings-only ones (history, background access, exercise routes) are excluded from it — requiring something the dialog cannot grant would be an onboarding nobody could leave. Missing optional permissions are surfaced later on the dashboard, detail screens, settings, imports, and entry flows where they matter.

Widening the required set is versioned (`HealthPermissionService.PERMISSION_SET_VERSION`, recorded per user). A user who onboarded under an older, narrower set is routed through onboarding once more rather than left behind permission gates forever.

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
