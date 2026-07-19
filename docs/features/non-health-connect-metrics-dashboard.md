# Non Health Connect Metrics Dashboard

> **Status:** Current implemented behavior for local derived views.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/activity/presentation/cardio_load_detail_screen.dart`, `lib/features/readiness/`, `lib/features/bodyenergy/`, `lib/features/recovery/`, `lib/domain/insights/`.
> **Navigation:** `/activity/cardio_load` and `/metric/{CARDIO_LOAD,WEEKLY_CARDIO_LOAD}`; `/daily_readiness` and its detail routes; Body Energy opens `/daily_readiness/body_energy/:date` (`/metric/BODY_ENERGY` has no dedicated screen and falls through to a placeholder).
> **Related:** [Feature map](feature-map.md), [Daily readiness](daily-readiness.md), [Body Energy](body-energy.md).

Some OpenVitals dashboard values are not raw Health Connect record types. They are local calculations built from Health Connect data and app preferences.

## How to use it

You reach these derived views from the same dashboard tiles as any other metric — they simply open a bespoke screen instead of a standard metric detail.

1. **Open Cardio load.** Tap the **Weekly cardio load** ring on the dashboard. The Cardio load screen opens with a summary card showing the score, a confidence label (**High / Medium / Low confidence**, or **No data**), and the method line that tells you how it was derived (from heart rate during workouts, from elevated heart rate, estimated from movement, or not enough data).
2. **Read the "Today's numbers" card.** This is a read-only breakdown — method, TRIMP score, heart-rate coverage, resting/max heart-rate context, activity windows and minutes, steps, and active calories — so you can see *why* the score is what it is. Cardio load is a **today-only** screen: there is no Day/Week/Month/Year toggle and no date navigation.
3. **Improve confidence.** Low confidence usually means missing heart-rate coverage. Wear a heart-rate sensor during workouts (see [Bluetooth LE sensors](ble-sensors.md)) and set your resting/max heart rate so the calculation has a reliable reserve to work from.
4. **Other derived views.** Readiness, Body Energy, sleep score, sleep efficiency, and body-composition values (BMI, FFMI) are reached from their own dashboard tiles or from [Daily readiness](daily-readiness.md). Each keeps its confidence and caveat context on screen when input data is incomplete.

## Cardio Load

Cardio load estimates training strain from available activity and heart-rate signals. When heart-rate coverage is good, OpenVitals uses a TRIMP-style calculation based on heart-rate reserve. When heart-rate data is missing or incomplete, the app can fall back to movement-only context with lower confidence.

The detail screen explains:

- Daily and weekly cardio load.
- Calculation method.
- Heart-rate coverage.
- Activity windows and activity minutes.
- Resting and maximum heart-rate context.
- Confidence and method labels.

## Other Derived Views

Derived views also include readiness-style summaries, body energy, sleep score, sleep efficiency, total-calorie estimates, and body composition calculations such as BMI and FFMI where the source records are available.

## Safety Boundaries

These values are wellness and information features. They are not medical diagnosis, treatment, or disease-prevention tools. The UI keeps caveats and confidence context near the derived result when the input data is incomplete.
