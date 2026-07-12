# Cycle Tracking

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/cycle/`, `lib/data/repository/contract/cycle_repository.dart` (+ `impl/cycle_repository_impl.dart`).
> **Navigation:** `/metric/CYCLE`; cycle permissions in the app's Health Connect permission set (`lib/domain/health/health_permissions.dart`).
> **Related:** [Feature map](feature-map.md), [Onboarding and permissions](onboarding-and-permissions.md), [Privacy](../app/privacy.md).

Cycle tracking reads supported Health Connect cycle records and presents them in dashboard and period detail views when permissions are granted.

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
