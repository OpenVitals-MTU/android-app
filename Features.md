# Features

This document is a functional inventory of the OpenVitals Android app. It is organized by what the user can view, what the user can insert/update/delete, and which settings are available.

For the route, widget, package, and documentation mapping, see [`docs/features/feature-map.md`](docs/features/feature-map.md).

## View Data

### Shared Metric Views

- View most metrics by day, week, month, or year.
- Move to previous and next periods.
- Pick a specific date from the calendar.
- Pull to refresh data from Health Connect.
- See period-aware charts:
  - Day and week views use daily or intraday chart values where available.
  - Month and year views use longer-range history/heatmap style summaries where available.
- See selected-day entry lists from charts where supported.
- See data confidence cards with coverage, sample counts, and source information.
- See period statistics, previous-period comparisons, and personal baseline insights where supported.
- See empty, permission, and error states when data is unavailable.

### Dashboard

- View configurable summary widgets for the main health categories.
- View activity widgets for steps, distance, calories out, active calories, floors, elevation, wheelchair pushes, and workouts.
- View sleep, beverages, hydration, caffeine, nutrition, body, vitals, mindfulness, cycle, and cardio load widgets.
- Reorder or remove dashboard widgets through the dashboard customization flow.
- Open the relevant metric detail screen from supported widgets.

### Achievements

- View unlocked and locked achievement progress.
- Filter achievements by category.
- View summary progress, tracked days, best daily steps, total distance, best daily floors, and total floors.
- View badge progress toward daily steps, lifetime distance, daily floors, and lifetime floors targets.

### Beverages, Hydration, And Caffeine

- View total hydration for the selected period.
- View metric hydration totals with two decimal places in liters.
- View hydration history charts by day, week, month, and year.
- View selected-day hydration entries.
- View each entry's amount, date/time, and source.
- View active caffeine estimates, caffeine intake totals, source and time-of-day insights, and bedtime guidance when caffeine nutrition records are available.
- View configured caffeine sensitivity, daily limit, and bedtime preferences.
- View daily goal progress, goal streaks, goals met, longest streak, success rate, average daily intake, total intake, best day, previous-period comparison, and personal baseline.
- View hydration reminders and daily goal configuration from the hydration detail screen.
- View a cross-metric insight comparing hydration with weight fluctuation.
- Distinguish OpenVitals-created entries from read-only entries created by other sources.

### Activity Metrics

- View steps.
- View distance.
- View calories out.
- View active calories.
- View floors climbed.
- View elevation gained.
- View wheelchair pushes.
- View each metric across day, week, month, and year ranges.
- View intraday charts for steps, calories, and active calories where data exists.
- View daily entries and aggregated daily totals.
- View daily goal progress for supported activity metrics.
- View total, daily average, best day, active days, previous-period comparison, and personal baseline.

### Workouts And Activities

- View workout/activity sessions for the selected period.
- View planned workouts when Health Connect provides them.
- View workout history charts and selected-day workout lists.
- View data confidence and manual-entry counts.
- View workout goal progress in minutes.
- View total workout duration, activity count, average duration, longest workout, previous-period comparison, and personal baseline.
- View guideline/context cards for activity volume.
- View cross-metric insight comparing workouts with resting heart rate.
- View activity details including title, type, start/end time, duration, moving time, source, notes, time zones, recording method, device, record IDs, client record IDs, client record version, planned session ID, and last modified time.
- View activity metrics including steps, distance, pace, speed, recorded speed, power, cadence, calories, wheelchair pushes, floors, and elevation.
- View route previews, route point counts, start/end route points, offline maps from imported PMTiles or Mapsforge packs, map opening, and GPX/KMZ export when route data is available.
- View activity segments, laps, repetitions, and set information where available.
- Configure the activity recording dashboard, use Focus mode, keep the screen awake while recording, and monitor heart rate for supported strength and repetition training recordings.

### Cardio Load

- View daily cardio load and weekly cardio load dashboard/detail values.
- View calculation details for the selected day.
- View TRIMP score, calculation method, heart-rate coverage, expected coverage, resting heart rate, max heart rate, heart-rate sample count, activity windows, activity minutes, and confidence/method labels.
- View explanatory context and references for the cardio load calculation.

### Sleep

- View sleep duration by day, week, month, and year.
- View sleep sessions in the selected period.
- View selected-day sleep session timelines.
- View sleep stages including asleep/sleeping, light, deep, REM, awake, awake in bed, and out of bed when available.
- View data confidence, sleep goal progress, total sleep, daily average, longest sleep, nights logged, previous-period comparison, and personal baseline.
- View sleep target/context cards.
- View cross-metric insight comparing sleep with HRV.
- Review caffeine timing and bedtime guidance in the standalone caffeine detail experience. Direct caffeine insight cards inside sleep detail are planned separately.
- View sleep session details including title, notes, source, recording method, device, IDs, start/end time, duration, and stage event list.

### Sleep Score And Recovery

- View recent sleep score and sleep efficiency details.
- View confidence and non-diagnostic context.
- View expandable formula/calculation details.
- View component values such as duration, efficiency, continuity, regularity, and total sleep.
- Keep missing caffeine data neutral; direct caffeine-aware sleep-score context is planned separately.
- View references used by the scoring/explanation screens.

### Daily Readiness

- View a local Daily Readiness score with confidence context.
- View Body Energy and Training Readiness scores.
- View HRV status, intensity minutes, physiological stress level, recommended activity, activity to avoid, alternatives, strain target, and adaptive goal guidance.
- View detailed Body Energy, Training Readiness, and Stress Tracking explanation screens.
- View signal breakdowns and caveats that explain how available Health Connect data affected the recommendation.
- Move between days, open the calendar, and refresh readiness data.

### Nutrition

- View calories in.
- View protein.
- View total carbohydrate.
- View total fat.
- View additional nutrient totals when present, including fiber, sugar, energy from fat, mono/poly/saturated/trans/unsaturated fat, cholesterol, vitamins, minerals, and caffeine.
- View nutrition trends by day, week, month, and year.
- View selected-day nutrition entries.
- View meals with meal type, name, date/time, calories, macros, fiber, sugar, and source.
- View macro split context.
- View data confidence and metric statistics including total, daily average, best day, logged days, previous-period comparison, and personal baseline.

### Calories

- View calories out.
- View active calories.
- View BMR.
- View total calories, active calories, and BMR trends.
- View daily calorie breakdown rows.
- View calculated total calories when the app is configured to combine active calories and BMR if Health Connect totals are missing.
- View daily averages and BMR reading counts.

### Body

- View weight.
- View height.
- View BMI.
- View body fat percentage.
- View lean body mass.
- View BMR.
- View bone mass.
- View body water mass.
- View body metrics across day, week, month, and year ranges.
- View history charts and entry rows with value, source, and time.
- View latest, average, lowest, highest, reading counts, previous-period comparison, and personal baseline where applicable.
- View BMI interpretation/context.
- Distinguish editable OpenVitals weight/height/body-fat entries from read-only external entries.

### Heart And Vitals

- View average heart rate.
- View resting heart rate.
- View HRV.
- View blood pressure.
- View SpO2.
- View VO2 max.
- View respiratory rate.
- View body temperature.
- View blood glucose.
- View skin temperature.
- View heart/vitals metrics across day, week, month, and year ranges.
- View history charts and entry rows with value, source, and time.
- View data confidence for aggregate and raw data.
- View latest, average, lowest, highest, readings/logged days, previous-period comparison, and personal baseline where applicable.
- View blood pressure latest, average, highest, readings, previous-period comparison, and personal baseline.
- View context cards for blood pressure categories, resting heart rate, oxygen saturation, respiratory rate, and body temperature.
- Distinguish editable OpenVitals vitals entries from read-only external entries.

### Mindfulness

- View mindfulness session totals and session counts for the selected period.
- View mindfulness history charts by day, week, month, and year.
- View selected-day mindfulness sessions.
- View each session's title, date/time, duration, and source.
- View data confidence.
- View daily goal progress, goal statistics, total duration, session count, average duration, longest session, previous-period comparison, and personal baseline.
- View mindfulness reminder status and reminder time.
- View cross-metric insight comparing mindfulness with sleep duration.
- Distinguish editable OpenVitals mindfulness sessions from read-only external sessions.

### Cycle Tracking

- View cycle data when Health Connect permissions are granted.
- View menstruation flow entries.
- View menstruation period intervals.
- View ovulation tests.
- View cervical mucus observations.
- View basal body temperature.
- View intermenstrual bleeding.
- View sexual activity entries.
- View cycle data across day, week, month, and year ranges.
- View cycle summary cards for period days, ovulation tests, and latest basal body temperature.
- View a cycle calendar with period, ovulation test, and basal temperature markers.
- View basal body temperature trend charts.
- View observation rows with date/time, value, and source.
- View data confidence and statistics for period days, ovulation tests, basal body temperature readings, and total entries.

### Health Connect And Sources

- View data from Health Connect-compatible sources.
- View source labels on entries where available.
- View missing-permission callouts and request relevant permissions from metric screens.
- View records imported from Apple Health once written to Health Connect.

## Insert / Update / Delete Data

### General Rules

- Manual data changes are written to Health Connect, not to a separate OpenVitals cloud account.
- Write permissions are requested only for data types that support manual entry.
- Entries created by other apps are read-only in OpenVitals.
- OpenVitals-created entries can be edited or deleted when the app has the required Health Connect write permission.
- Some data types can be inserted through Apple Health import even when there is no manual entry screen.

### Manual Entry Screen

- Open a centralized manual entry area with configurable entry widgets.
- Show entry widgets for beverages/hydration, activity, carbohydrate, mindfulness, weight, height, body fat, blood pressure, SpO2, respiratory rate, and body temperature.
- Reorder, remove, and manage manual entry widgets.

### Beverages And Hydration

- Add beverage/hydration entries.
- Tap a container size to save a beverage entry immediately.
- Select beverage presets such as water, coffee, tea, soft drinks, energy drinks, sports drinks, oral rehydration solution, milk, fruit juice, and custom drinks.
- Save caffeine and selected nutrition defaults with supported beverages as Health Connect nutrition records.
- Select container sizes such as coffee cup, tea cup, small cup, medium glass, large glass, water bottle, and large bottle.
- Use beverage hydration multipliers for effective hydration amount.
- Manage preset/custom drink categories and ordering.
- Add custom container sizes.
- View today's intake against the daily goal while adding an entry.
- Update OpenVitals-created beverage/hydration entries.
- Delete OpenVitals-created beverage/hydration entries.
- Request Health Connect hydration and nutrition write permissions from the entry flow.

### Nutrition

- Add carbohydrate entries.
- Save carbohydrate totals directly to Health Connect nutrition records.
- Request Health Connect nutrition write permission from the entry flow.

### Activity And Workouts

- Add workout/activity sessions manually.
- Update OpenVitals-created workout/activity sessions.
- Delete OpenVitals-created workout/activity sessions.
- Choose activity type, start date, start time, duration, distance, elevation, active calories, total calories, repetitions, title, and notes.
- Enter repetition-based workouts using total repetitions or sets with repetitions and rest minutes.
- Import route/activity files in GPX, KML, KMZ, or FIT formats.
- Bulk import multiple GPX, KML, or KMZ route files from Settings.
- Preview imported routes and inferred activity details before saving.
- Save imported route data with inferred type, title, notes, distance, elevation, time range, and calorie estimates where available.
- Import PMTiles or Mapsforge map packs from Settings for offline activity maps.
- Use imported offline maps while recording activities and previewing saved or imported routes.
- Record route-based activities with GPS.
- Start, pause, resume, finish, or discard a recording.
- Save recorded route points, pause intervals, distance, and elevation.
- Use sensor-assisted repetition flows for supported activities such as treadmill steps, push-ups, pull-ups, rope skipping, and trampoline jumping.
- Request Health Connect activity write permissions from the entry flow.

### Mindfulness

- Add mindfulness sessions with a timer.
- Configure timer duration.
- Configure interval bells.
- Select bell sounds: struck, rubbed, bright, temple, or harmony.
- Select background sounds: none, bowl, meditation, chimes, or dreamscape.
- Start, stop, resume, discard, and save timer sessions.
- Add manual mindfulness minutes.
- Update OpenVitals-created mindfulness sessions.
- Delete OpenVitals-created mindfulness sessions.
- Request Health Connect mindfulness write permission from the entry flow.

### Body

- Add weight measurements.
- Add height measurements.
- Add body fat percentage measurements.
- Update OpenVitals-created weight, height, and body-fat entries.
- Delete OpenVitals-created weight, height, and body-fat entries.
- Request Health Connect body write permissions from the entry flow.
- BMI is calculated from available weight and height data and is not manually inserted.
- Lean body mass, BMR, bone mass, and body water mass are view-only in the manual UI.

### Vitals

- Add blood pressure measurements with systolic and diastolic values.
- Add SpO2 measurements.
- Add respiratory rate measurements.
- Add body temperature measurements.
- Update OpenVitals-created blood pressure, SpO2, respiratory rate, and body temperature entries.
- Delete OpenVitals-created blood pressure, SpO2, respiratory rate, and body temperature entries.
- Request Health Connect vitals write permissions from the entry flow.
- Average heart rate, resting heart rate, HRV, VO2 max, blood glucose, and skin temperature are view-only in the manual UI.

### Apple Health Import

- Import Apple Health `export.xml` or `export.zip` files.
- Request the Health Connect permissions needed for the selected import data.
- Track import phases including queued, parsing, writing, finishing, and complete.
- Continue import work in the background.
- Show import result summaries, unsupported records, skipped records, failures, and copy/saveable reports.
- Deduplicate imported records using stable client record IDs.
- Insert supported Apple Health activity records including steps, distance, active calories, basal energy/BMR, floors, elevation, wheelchair pushes, and workouts.
- Insert supported Apple Health heart and vitals records including heart rate, resting heart rate, oxygen saturation, respiratory rate, body temperature, blood glucose, VO2 max, and blood pressure.
- Insert supported Apple Health body records including weight, height, body fat, lean body mass, bone mass, and body water mass.
- Insert supported Apple Health hydration records.
- Insert supported Apple Health sleep records and stages.
- Insert supported Apple Health mindfulness sessions.
- Insert supported Apple Health nutrition records grouped into Health Connect nutrition records.
- Insert supported Apple Health cycle records when Health Connect write permissions are granted.
- Skip HRV SDNN import because the current Health Connect mapping is incompatible with that Apple Health record type.
- No rollback/delete flow is provided for an Apple Health import after records are written.

### View-Only Or External-Only Data

- Sleep sessions are view-only in the app.
- Meal entries and nutrition fields other than manual carbohydrate totals are view-only in the manual UI.
- Cycle tracking observations are view-only in the app.
- Heart rate, resting heart rate, HRV, VO2 max, blood glucose, and skin temperature are view-only in the manual UI.
- Lean body mass, BMR, bone mass, body water mass, and BMI are view-only in the manual UI.
- Planned workouts are view-only.
- Achievements are computed from activity data and are not manually edited.

## Settings

### Display Settings

- Change language: system default, English, Spanish, German, or Italian.
- Change unit system: metric or imperial.
- Change theme: system, light, dark, or AMOLED.

### Activity Settings

- Change activity week mode: Monday-to-Sunday or last 7 days.
- Choose the favorite/default activity behavior used by activity entry.
- Use the latest activity or a route-capable default activity type as the favorite activity source.

### Calories Settings

- Choose calorie data mode.
- Use Health Connect calorie totals only.
- Or allow OpenVitals to calculate total calories from active calories plus BMR when Health Connect totals are missing.

### Sleep Settings

- Change sleep range mode.
- Supported sleep range modes include rolling 24 hours, noon boundary, and evening 18:00 boundary.

### Cycle Tracking Settings

- Request cycle tracking permissions from the cycle settings area.
- View cycle data access alongside other Health Connect permissions.

### Data Import Settings

- Open Apple Health import.
- Grant import permissions.
- Select Apple Health export files.
- Import one GPX, KML, KMZ, or FIT activity file for review before saving.
- Bulk import multiple GPX, KML, or KMZ route files directly into Health Connect.
- Monitor import progress and read import reports.

### Permission Settings

- View Health Connect permission categories.
- Grant missing requestable permissions from inside the app.
- Open Health Connect when permissions must be granted manually.
- See the all-requestable-permissions-granted state.

### Dashboard And Manual Entry Customization

- Configure dashboard widget order and visibility.
- Configure manual entry widget order and visibility.
- Restore or maintain the default widget sets through stored preferences.

### Goals And Reminders

- Configure daily goals for supported metrics, including hydration, activity metrics, workout minutes, sleep, nutrition metrics, and mindfulness.
- Configure hydration reminders, reminder interval, and active reminder window.
- Configure caffeine sensitivity, daily limit, and bedtime guidance.
- Configure mindfulness reminders and reminder time.
- Store custom hydration container sizes.

### Privacy And App Information

- View privacy notes explaining that OpenVitals uses no account, no cloud sync, no analytics, and no ads.
- View that health data is read from and written to Health Connect on device.
- View the read-only dashboard/privacy positioning and health disclaimer.
- View app version information.
