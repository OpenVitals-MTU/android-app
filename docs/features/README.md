# Features

OpenVitals is a local-first Android app for Health Connect data, activity logging, imports, and simple on-device insights. This guide is a short map of the major feature areas. The [root feature inventory](../../Features.md) remains the detailed checklist.

- [Health Connect metrics dashboard](health-connect-metrics-dashboard.md): read Health Connect records into a configurable dashboard and period detail screens. Steps are the simplest example: OpenVitals reads step totals, shows charts, and links into richer statistics without storing a second health database.
- [Non Health Connect metrics dashboard](non-health-connect-metrics-dashboard.md): show local derived metrics that are calculated from Health Connect data. Cardio load is the main example, using available heart-rate and activity signals to estimate training strain.
- [Manual entry of metrics](manual-entry-metrics.md): save explicit user-entered records back to Health Connect. Weight is one of the core flows, alongside hydration, carbohydrates, body measurements, vitals, mindfulness, and activity entries.
- [Recording of activity](activity-recording.md): record activities in OpenVitals before saving them to Health Connect. This includes GPS activity recording and repetition-oriented activity flows.
- [Offline maps support](offline-maps-support.md): import PMTiles or Mapsforge packs for local activity maps that can be used without app-level internet access.
- [Apple Health import](apple-health-import.md): import supported Apple Health export records into Health Connect with background progress.
- [FIT files import](fit-files-import.md): import FIT activity files, review detected details, and save supported workout data.
- [Activity and training plans](activity-training-plans.md): view planned workouts from Health Connect and use activity setup defaults, favorite activities, repetitions, sets, and review flows.
- [GPX/KML/KMZ/FIT import](route-file-import.md): import route and activity files for review before saving route-backed activities.
- [Daily readiness](daily-readiness.md): calculate a local readiness view with Body Energy, Training Readiness, HRV status, stress context, and adaptive guidance.
- [Achievements](achievements.md): track local badge progress for activity, distance, floors, workouts, hydration, sleep, and mindfulness.
- [Reminders](reminders.md): configure local hydration and mindfulness reminders with Android notification handling.
- [Statistics](statistics.md): use day/week/month/year ranges, comparisons, baselines, confidence, and trends across metric detail screens.
