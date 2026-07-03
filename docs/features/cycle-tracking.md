# Cycle Tracking

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

Cycle permissions are managed separately from broader activity, body, and vitals permissions. OpenVitals shows cycle access only when the relevant Health Connect permission categories are available.

## Write Behavior

Cycle observations are view-only in the app. They may be imported from Apple Health when compatible Health Connect write permissions are granted, but OpenVitals does not provide a manual cycle entry editor.

## Privacy

Cycle data stays in Health Connect and on device. OpenVitals does not upload cycle records to an OpenVitals server.
