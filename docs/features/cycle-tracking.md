# Cycle Tracking

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/cycle/`, `lib/data/repository/contract/cycle_repository.dart` (+ `impl/cycle_repository_impl.dart`).
> **Navigation:** `/metric/CYCLE`; cycle permissions in the app's Health Connect permission set (`lib/domain/health/health_permissions.dart`).
> **Related:** [Feature map](feature-map.md), [Onboarding and permissions](onboarding-and-permissions.md), [Privacy](../app/privacy.md).

Cycle tracking reads supported Health Connect cycle records and presents them in dashboard and period detail views when permissions are granted.

## How to use it

1. **Grant cycle access.** Cycle data is sensitive and opt-in. If the menstruation read permission isn't granted, the screen shows an access gate — grant it from onboarding or **Settings › Health Connect** (the cycle category is separate from activity, body, and vitals).
2. **Open Cycle tracking.** Open it from its dashboard tile (visible only when cycle permissions are available). It uses the shared **Day / Week / Month / Year** controls, calendar, and pull-to-refresh (see [Statistics](statistics.md)).
3. **Read the summary.** Cards show **Period days** and **Entries** for the range; the **Statistics** card adds ovulation-test counts, basal-body-temperature readings, and your latest basal temperature.
4. **Browse observations.** The **Entries** list shows dated records — menstruation period/flow, ovulation tests, cervical mucus, basal body temperature, intermenstrual bleeding, and sexual activity — each decoded into words (for example flow shown as Light / Medium / Heavy).

Cycle tracking is **view-only** in OpenVitals: there is no in-app cycle editor. Records can arrive via [Apple Health import](apple-health-import.md) when compatible write permissions are granted, but you log and edit cycle data in the app that owns it. If a period shows **"No cycle tracking data"**, no records were written for that range.

## Supported Data

OpenVitals can display supported cycle areas such as:

- Menstruation flow entries.
- Menstruation period intervals.
- Ovulation tests.
- Cervical mucus observations.
- Basal body temperature.
- Intermenstrual bleeding.
- Sexual activity entries.

Availability depends on Health Connect support and granted permissions.

## Views

Cycle views can include summary cards, a calendar, basal temperature trends, period-aware statistics, and observation rows with date, time, value, and source.

Cycle data follows the same day, week, month, and year period model used by other metric detail screens.

## Permissions

Cycle permissions are managed separately from broader activity, body, and vitals permissions inside Health Connect settings. OpenVitals shows cycle access only when the relevant Health Connect permission categories are available.

## Write Behavior

Cycle observations are view-only in the app. They may be imported from Apple Health when compatible Health Connect write permissions are granted, but OpenVitals does not provide a manual cycle entry editor.

## Privacy

Cycle data stays in Health Connect and on device. OpenVitals does not upload cycle records to an OpenVitals server.
