# Features

OpenVitals is a local-first Android app for Health Connect data, activity logging, imports, and simple on-device insights. This guide is a short map of the major feature areas. The [root feature inventory](../../Features.md) remains the detailed checklist.

- [Health Connect metrics dashboard](health-connect-metrics-dashboard.md): read Health Connect records into a configurable dashboard and reorderable period detail screens. Steps are the simplest example: OpenVitals reads step totals, shows charts, and links into richer statistics without storing a second health database.
- [Activity metrics](activity-metrics.md): document the implemented activity detail package for steps, distance, calories, active calories, floors, elevation, wheelchair pushes, workouts, and cardio-load-adjacent context.
- [Body metrics](body-metrics.md): document the implemented body detail package for weight, height, BMI, body fat, lean mass, BMR, bone mass, and body water mass.
- [Heart and vitals](heart-and-vitals.md): document the implemented heart and vitals packages, including average heart rate, resting heart rate, HRV, blood pressure, SpO2, VO2 max, respiratory rate, body temperature, blood glucose, skin temperature, and Today Vitals.
- [Hydration](hydration.md): document the implemented hydration period detail screen, hydration entry history, goals, and reminder controls.
- [Nutrition](nutrition.md): document the implemented nutrition detail screens for calories in, protein, carbohydrates, and fat.
- [Mindfulness](mindfulness.md): document the implemented mindfulness detail screen, session history, goals, reminders, and manual-entry relationship.
- [Sleep tracking](sleep-tracking.md): document the implemented sleep period overview and sleep-session detail flow.
- [Non Health Connect metrics dashboard](non-health-connect-metrics-dashboard.md): show local derived metrics that are calculated from Health Connect data. Cardio load is the main example, using available heart-rate and activity signals to estimate training strain.
- [Metric detail customization](metric-detail-customization.md): reorder dashboard widgets, manual entry widgets, and metric detail sections without changing Health Connect records.
- [Home screen widgets](home-widgets.md): configure Android launcher widgets for metric summaries, readiness, Body Energy, Today Vitals, and quick beverage logging.
- [Onboarding and permissions](onboarding-and-permissions.md): guide first-run setup, Health Connect availability, read permissions, lazy write permissions, and privacy expectations.
- [Settings and preferences](settings-and-preferences.md): configure language, units, theme, metric preferences, goals, reminders, Health Connect access, imports, sensors, and diagnostics.
- [Manual entry of metrics](manual-entry-metrics.md): save explicit user-entered records back to Health Connect. Weight is one of the core flows, alongside beverage/hydration entries with caffeine and nutrition defaults, carbohydrates, body measurements, vitals, mindfulness, and activity entries.
- [Beverage logging and caffeine](beverage-logging-and-caffeine.md): log drinks with hydration, caffeine, frequently consumed presets, custom catalog choices, and selected nutrition defaults.
- [Recording of activity](activity-recording.md): record activities in OpenVitals before saving them to Health Connect. This includes GPS activity recording and repetition-oriented activity flows.
- [Bluetooth LE sensors](ble-sensors.md): use supported heart-rate, cadence, power, and footpod sensors during activity recording with local status and timeout handling.
- [Activity start flow analysis](activity-start-flow.md): map the current recording, manual activity, and planned-workout flows, then outline simplification options for fewer taps.
- [Activity start flow proposals](activity-start-flow-proposals.md): explain the proposed activity-start simplifications with current/proposed examples and expected impact.
- [Offline maps support](offline-maps-support.md): import PMTiles or Mapsforge packs for local activity maps that can be used without app-level internet access.
- [Apple Health import](apple-health-import.md): import supported Apple Health export records into Health Connect with background progress and chunked processing for large exports.
- [FIT files import](fit-files-import.md): import FIT activity files, review detected details, and save supported workout data.
- [Activity and training plans](activity-training-plans.md): view planned workouts from Health Connect and use activity setup defaults, favorite activities, repetitions, sets, and review flows.
- [GPX/KML/KMZ/FIT import](route-file-import.md): import route and activity files for review before saving route-backed activities.
- [Daily readiness](daily-readiness.md): calculate a local readiness view with Body Energy, Training Readiness, HRV status, stress context, and adaptive guidance.
- [Body Energy](body-energy.md): show a local energy timeline with calibration, confidence, dashboard support, and home widget support.
- [Sleep score and recovery](sleep-score-and-recovery.md): explain sleep score, sleep efficiency, sleep-stage timelines, recovery context, and confidence.
- [Caffeine detail and future sleep insights](caffeine-aware-sleep-insights.md): document the implemented standalone caffeine experience and the planned direct sleep-insight integration.
- [Cycle tracking](cycle-tracking.md): read supported Health Connect cycle records into dashboard and period detail views when permissions are granted.
- [Preloaded beverage nutrition reference](preloaded-beverage-nutrition.md): map the imported caffeine beverage presets to nutrition families, common serving values, and source links for default entry data.
- [Achievements](achievements.md): track implemented local badge progress for activity steps, distance, and floors, with additional wellness categories documented as planned.
- [Reminders](reminders.md): configure local hydration and mindfulness reminders with Android notification handling.
- [Statistics](statistics.md): use day/week/month/year ranges, comparisons, baselines, confidence, and trends across metric detail screens.
- [Privacy, support, and diagnostics](privacy-support-diagnostics.md): summarize the local-first privacy model, diagnostics surfaces, support links, and health disclaimer.

## Feature Package Coverage

This table maps the current `features/*` packages to user-facing docs. It is intentionally package-level, so future gaps are easier to spot when code is added.

| Feature package | Documentation |
|---|---|
| `features/achievements` | [Achievements](achievements.md) |
| `features/activity` | [Activity metrics](activity-metrics.md), [Recording of activity](activity-recording.md), [Activity and training plans](activity-training-plans.md), [GPX/KML/KMZ/FIT import](route-file-import.md), [Offline maps support](offline-maps-support.md) |
| `features/body` | [Body metrics](body-metrics.md) |
| `features/bodyenergy` | [Body Energy](body-energy.md) |
| `features/caffeine` | [Beverage logging and caffeine](beverage-logging-and-caffeine.md), [Caffeine detail and future sleep insights](caffeine-aware-sleep-insights.md) |
| `features/cycle` | [Cycle tracking](cycle-tracking.md) |
| `features/dashboard` | [Health Connect metrics dashboard](health-connect-metrics-dashboard.md), [Non Health Connect metrics dashboard](non-health-connect-metrics-dashboard.md), [Metric detail customization](metric-detail-customization.md) |
| `features/heart` | [Heart and vitals](heart-and-vitals.md) |
| `features/homewidgets` | [Home screen widgets](home-widgets.md) |
| `features/hydration` | [Hydration](hydration.md), [Reminders](reminders.md), [Beverage logging and caffeine](beverage-logging-and-caffeine.md) |
| `features/imports/applehealth` | [Apple Health import](apple-health-import.md) |
| `features/manualentry` | [Manual entry of metrics](manual-entry-metrics.md), [Beverage logging and caffeine](beverage-logging-and-caffeine.md), [GPX/KML/KMZ/FIT import](route-file-import.md), [Activity start flow analysis](activity-start-flow.md) |
| `features/mindfulness` | [Mindfulness](mindfulness.md), [Reminders](reminders.md) |
| `features/nutrition` | [Nutrition](nutrition.md), [Preloaded beverage nutrition reference](preloaded-beverage-nutrition.md) |
| `features/onboarding` | [Onboarding and permissions](onboarding-and-permissions.md) |
| `features/readiness` | [Daily readiness](daily-readiness.md), [Body Energy](body-energy.md) |
| `features/recovery` | [Sleep score and recovery](sleep-score-and-recovery.md) |
| `features/settings` | [Settings and preferences](settings-and-preferences.md), [Privacy, support, and diagnostics](privacy-support-diagnostics.md) |
| `features/sleep` | [Sleep tracking](sleep-tracking.md), [Sleep score and recovery](sleep-score-and-recovery.md) |
| `features/vitals` | [Heart and vitals](heart-and-vitals.md), [Manual entry of metrics](manual-entry-metrics.md) |
