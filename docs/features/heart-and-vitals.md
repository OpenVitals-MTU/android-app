# Heart And Vitals

Heart and vitals are related but distinct feature areas.

- `features/heart` owns heart-rate-oriented state, presentation mapping, and route wrappers for average heart rate, resting heart rate, and HRV.
- `features/vitals` owns vitals-facing screens and UI helpers such as blood pressure, SpO2, VO2 max, respiratory rate, body temperature, blood glucose, skin temperature, and the Today Vitals overview.

The current implementation still shares `HeartViewModel` and the heart period loader across heart and vitals routes. That is an intentional transitional boundary: the user-facing vitals UI lives in `features/vitals`, while some loading/state infrastructure remains shared until a deeper split is worth the extra complexity.

## Implemented Metrics

Heart metrics:

- Average heart rate.
- Resting heart rate.
- HRV.

Vitals metrics:

- Blood pressure.
- SpO2.
- VO2 max.
- Respiratory rate.
- Body temperature.
- Blood glucose.
- Skin temperature.

## Detail Pattern

Heart and vitals detail screens use the shared metric detail scaffold:

- Day, week, month, and year ranges.
- Selected anchor date.
- Previous/next period navigation.
- Calendar date picking.
- Pull to refresh.
- Intraday charts where sample data is available.
- Period charts, statistics, thresholds, comparisons, data confidence, source labels, and entry lists.
- Reorderable sections.

OpenVitals-created vitals entries can be edited or deleted when the app has write permission and ownership can be verified. External records stay read-only.

## Today Vitals

The vitals overview groups related metrics into heart, cardiovascular, and respiratory/body-temperature sections. It uses the same period shell as other metric details and links into focused metric screens.

## Data Boundaries

Heart-rate and vitals records are read through feature-facing repository/use-case APIs rather than a global browser. Manual vitals entry lives under `features/manualentry/vitals`; dashboard cards route to focused heart or vitals detail destinations.

