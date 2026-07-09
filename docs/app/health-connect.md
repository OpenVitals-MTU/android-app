# Health Connect

Health Connect is Android's on-device health data store. OpenVitals reads records from Health Connect and writes only entries the user explicitly saves, imports, records, edits, or deletes.

OpenVitals does not replace Health Connect and does not keep a separate cloud copy of health records.

## Read Coverage

OpenVitals can show these Health Connect areas when permission and data are available:

- Activity: steps, distance, exercise sessions, floors climbed, elevation gain, wheelchair pushes, active calories, total calories, speed, power, cadence, planned exercise, and workout routes.
- Sleep: sessions and sleep stages.
- Heart: heart rate, resting heart rate, and HRV.
- Body: weight, height, BMI, body fat, lean mass, basal metabolic rate, bone mass, and body water mass.
- Hydration and nutrition: hydration totals, beverages, calories in, meals, macros, caffeine, and supported nutrients.
- Mindfulness: mindfulness sessions when the installed Health Connect provider supports them.
- Vitals: blood pressure, SpO2, respiratory rate, body temperature, VO2 max, blood glucose, and skin temperature.
- Cycle: menstruation, ovulation tests, cervical mucus, basal body temperature, intermenstrual bleeding, and sexual activity when cycle permissions are granted.

## Write Coverage

OpenVitals writes to Health Connect only from explicit entry, recording, edit/delete, or import workflows:

- Hydration and beverage entries.
- Nutrition records for explicitly saved carbohydrate totals, caffeine, and selected beverage nutrition defaults.
- Sleep sessions from supported imports.
- Exercise sessions and optional route, distance, elevation gain, active calories, total calories, speed, power, and cadence records where supported.
- Heart rate and resting heart rate from supported imports or recordings where compatible.
- Weight, height, body fat, lean body mass, BMR, bone mass, body water mass, floors climbed, wheelchair pushes, nutrition, VO2 max, blood glucose, and cycle records from supported imports where Health Connect allows them.
- Mindfulness sessions.
- Blood pressure, SpO2, respiratory rate, and body temperature.

Large Apple Health imports run as explicit user-started background work with progress notifications while records are scanned and written.

## History And Background Access

Health Connect may limit how much historical data an app can read unless Health history access is granted.

OpenVitals can request Health history and background-read access where Android and Health Connect support them. Long reads may still be chunked or retried to avoid Health Connect rate limits.

## Routes

Workout routes are sensitive Health Connect data. Route previews may require manual approval from Health Connect settings.

OpenVitals can import GPX/KML/KMZ route files from Settings Data Importers into activity review, bulk import multiple GPX/KML/KMZ files directly, import FIT activity, course, or workout files from Settings Data Importers into activity review, import PMTiles or Mapsforge map packs for offline route previews, record GPS routes, open saved routes in map apps, and export routes as GPX or KMZ when route data is available.

## Platform Notes

- Android 14 and newer include Health Connect as part of the system.
- Android 13 and older normally need the separate Health Connect app.
- Work profiles do not support Health Connect.
- Mindfulness support depends on the installed Health Connect provider version.
