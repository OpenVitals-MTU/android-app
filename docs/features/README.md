# Features

OpenVitals is a local-first Android app for Health Connect data, activity logging, imports, and simple on-device insights. This index points to current implemented behavior. Proposal and future-work notes live in [Proposals](../proposals/README.md).

Use the [feature map](feature-map.md) when you need the route/widget/package mapping. The [root feature inventory](../../Features.md) remains the detailed checklist.

## Dashboard And App Experience

- [Health Connect metrics dashboard](health-connect-metrics-dashboard.md): summary widgets and focused metric detail entry points.
- [Non Health Connect metrics dashboard](non-health-connect-metrics-dashboard.md): local derived views such as cardio load, readiness, Body Energy, and sleep recovery context.
- [Metric detail customization](metric-detail-customization.md): reorder dashboard widgets, manual entry widgets, and metric detail sections.
- [Home screen widgets](home-widgets.md): Android launcher widgets for metric summaries, readiness, Body Energy, Today Vitals, and quick beverage logging.
- [Onboarding and permissions](onboarding-and-permissions.md): first-run Health Connect setup, permission categories, and privacy expectations.
- [Settings and preferences](settings-and-preferences.md): language, units, theme, goals, reminders, Health Connect access, imports, sensors, and diagnostics.
- [Privacy, support, and diagnostics](privacy-support-diagnostics.md): local-first privacy model, diagnostics surfaces, support links, and health disclaimer.
- [Achievements](achievements.md): local badge progress for supported wellness categories.

## Health Metrics

- [Activity metrics](activity-metrics.md): steps, distance, calories, active calories, floors, elevation, wheelchair pushes, workouts, and cardio-load context.
- [Sleep tracking](sleep-tracking.md): sleep period overview and sleep-session detail flow.
- [Sleep score and recovery](sleep-score-and-recovery.md): sleep score, sleep efficiency, recovery details, and confidence.
- [Daily readiness](daily-readiness.md): Body Energy, Training Readiness, HRV status, stress context, and adaptive guidance.
- [Body Energy](body-energy.md): selected-day energy timeline, calibration, confidence, dashboard support, and widgets.
- [Heart and vitals](heart-and-vitals.md): heart rate, resting heart rate, HRV, blood pressure, SpO2, VO2 max, respiratory rate, body temperature, blood glucose, skin temperature, and Today Vitals.
- [Body metrics](body-metrics.md): weight, height, BMI, body fat, lean mass, BMR, bone mass, body water mass, and FFMI context.
- [Nutrition](nutrition.md): calories in, protein, carbohydrates, fat, and selected nutrient totals.
- [Hydration](hydration.md): hydration period detail, entry history, goals, and reminder controls.
- [Mindfulness](mindfulness.md): mindfulness period detail, session history, goals, reminders, and manual-entry relationship.
- [Cycle tracking](cycle-tracking.md): supported Health Connect cycle records in dashboard and period detail views.
- [Statistics](statistics.md): period ranges, comparisons, baselines, confidence, and trends across detail screens.

## Logging, Import, And Recording

- [Manual entry of metrics](manual-entry-metrics.md): explicit user-entered records written back to Health Connect.
- [Beverage logging and caffeine](beverage-logging-and-caffeine.md): drink logging with hydration, caffeine, presets, custom catalog choices, and selected nutrition defaults.
- [Recording of activity](activity-recording.md): GPS and repetition-oriented activity recording before saving to Health Connect.
- [Activity and training plans](activity-training-plans.md): planned workouts, activity setup defaults, favorite activities, repetitions, sets, and review flows.
- [Bluetooth LE sensors](ble-sensors.md): supported heart-rate, cadence, power, and footpod sensors during activity recording.
- [GPX/KML/KMZ route import](route-file-import.md): route file import for review before saving.
- [FIT files import](fit-files-import.md): Settings Data Importers support for FIT activity, course, and workout files.
- [Offline maps support](offline-maps-support.md): PMTiles or Mapsforge packs for local activity maps.
- [Apple Health import](apple-health-import.md): supported Apple Health export records written into Health Connect.
- [Preloaded beverage nutrition reference](preloaded-beverage-nutrition.md): imported caffeine beverage presets, nutrition families, common serving values, and source links.
- [Reminders](reminders.md): local hydration and mindfulness reminders with Android notification handling.
