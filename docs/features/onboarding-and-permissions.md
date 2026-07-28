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

Onboarding is a four-step guide. **Back** walks between steps; only the first
step gates.

1. **Choose what OpenVitals can read.** Five rows, grouped the way Health
   Connect itself groups records — **Activity, Body measurements, Nutrition,
   Sleep, Vitals** — so each row opens a system dialog with the same heading.
   One tap per row asks to **read and to save**, so entries you create in
   OpenVitals go back to Health Connect. **Only Activity and Sleep are
   required** for **Next** to unlock; the rest show *Optional* and can be
   granted now, later from this screen, or any time from Settings.
2. **Mindfulness** — shown only on devices whose Health Connect supports it,
   behind an **off-by-default switch**. Some Health Connect versions report
   support but crash their own permission screen when asked — after which
   nothing can be granted at all — so OpenVitals never asks on its own
   initiative. Turning the switch on reveals a row that requests mindfulness
   *alone*; if your device can't cope, nothing else is affected.
3. **Cycle tracking** — its own step with a plain description of what it
   covers (including sexual activity), and a **Not now** to skip it.
4. **Finishing touches.** Past-data and background access go through the
   normal dialog. **Exercise route reading** is the one thing no app can
   request: Android keeps it under Health Connect's *Additional access* page,
   so if it's still missing the step shows a three-tap walkthrough ending in
   an **Open Health Connect** button that lands one tap away. (Route *writing*
   is an ordinary toggle and rides along with Activity.) **Finish** opens the
   dashboard.

Each row shows what it covers and how much is granted, with a **Grant**,
**Review** (partly granted), or **Open** action. Anything skipped can be
changed later at **Settings › Health Connect** — the same seven categories,
described the same way.

If your device's Health Connect doesn't recognise a permission, the app never
asks for it: it is filtered out before any dialog opens, and the category shows
as **Not supported** rather than as something you failed to grant.

## First Run

Onboarding introduces the app, checks Health Connect availability, and walks four steps grouped by Health Connect's own data categories.

Only Activity and Sleep block: the dashboard renders nothing without them, and a first run that blocks on everything is one a single stray refusal can trap a user inside. Everything else — the optional read categories, cycle tracking, mindfulness, and the settings-only permissions (history, background access, exercise routes) — is offered and skippable. The dashboard deliberately does **not** prompt for whatever was skipped; a metric without access simply shows no data, and screens ask at the point of use.

Widening the required set is versioned (`HealthPermissionService.PERMISSION_SET_VERSION`, recorded per user). A user who onboarded under an older, narrower set is routed through onboarding once more rather than left behind permission gates forever.

## Health Connect Availability

OpenVitals handles Health Connect states explicitly:

- Available and ready.
- Available but missing permissions (metrics show no data; no nagging).
- Not installed or not reachable on supported devices.
- Unsupported Android or device environments.

When Health Connect is not available, the app explains the limitation instead of showing misleading health values.

## Read And Write Permissions

Read permissions are used for dashboard widgets, metric detail screens, readiness views, statistics, achievements, and local insights.

Write permissions are granted alongside reads — each onboarding category asks for both directions in one tap, so entries created in OpenVitals flow back to Health Connect without a second prompt. Write flows that find their permission missing anyway (a skipped category, a revocation) still ask lazily at the point of use: manual entry, route import, Apple Health import, activity recording, edits, and deletes.

The dashboard remains read-only even when write permissions are granted.

## Optional Areas

Some permission groups, such as cycle data or sensor-related permissions, are shown only when they are relevant and requestable on the device.

Body Energy calibration can be collected during onboarding and adjusted later in settings.

## Privacy Expectations

OpenVitals stores app preferences locally and reads or writes health records through Health Connect. It does not require an OpenVitals account.
