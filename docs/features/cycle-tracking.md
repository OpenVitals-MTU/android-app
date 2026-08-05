# Cycle Tracking

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/cycle`, `data/repository/CycleRepository.kt`.
> **Navigation:** `Screen.Metric`, widget `CYCLE`, Health Connect permission category.
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

Cycle observations can be logged from the app's manual entry grid ("+ Log" → Cycle) through a single day-log screen: period flow (light/medium/heavy), spotting, sexual activity (protected/unprotected/unknown), ovulation tests, cervical mucus (appearance and amount), and basal body temperature. Saving writes only the filled sections, each as its own Health Connect record; Health Connect remains the only store. Entries created by OpenVitals can be edited (pencil) and deleted (swipe) from the cycle screen's entry list; records from other apps stay read-only.

Menstruation period intervals (`MenstruationPeriodRecord`) are derived, not logged: after every flow-day change the app reconciles consecutive bleeding days (tolerating a single-day gap) into period records it owns, leaving overlapping periods written by other apps untouched.

## Cycle Statistics and Prediction

With the menstruation read permission granted, the cycle screen derives statistics over the last 12 months of flow days and period spans (all sources): current cycle day (blank past 99 days), average cycle length, and — after three completed cycles — up to three predicted period windows (mean length, ±1 day when regular, ±2 when the standard deviation reaches 1.5). The first window renders as a "Next period" card and future predicted days are outlined in the calendar. The rules follow drip's published statistical method; no fertility or ovulation inference is performed.

## Privacy

Cycle data stays in Health Connect and on device. OpenVitals does not upload cycle records to an OpenVitals server.
