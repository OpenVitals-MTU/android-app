# Manual Entry Of Metrics

Manual entry flows let the user write explicit records to Health Connect. OpenVitals does not keep a separate health database for these records.

## Weight

Weight is a core manual metric flow. The user enters a value and saves it to Health Connect. OpenVitals-created weight entries can later be edited or deleted when the app has the required write permission. Weight data also feeds body detail screens, BMI, body composition context, and statistics.

## Supported Manual Entries

OpenVitals supports explicit logging for:

- Beverages/hydration, including drink/container choices, custom amounts, caffeine-aware presets, and selected nutrition defaults.
- Carbohydrate totals as Health Connect nutrition records.
- Activity sessions, optionally with routes, distance, elevation, calories, repetitions, title, and notes.
- Mindfulness sessions through a timer or manual duration.
- Body measurements such as weight, height, and body fat.
- Vitals such as blood pressure, SpO2, respiratory rate, and body temperature.

## Permission Handling

Write permissions can be granted during onboarding or requested when an entry flow needs them. The dashboard remains read-only even if write permissions have already been granted.

## External Records

Records created by other apps stay read-only in OpenVitals. OpenVitals checks ownership before allowing edits or deletes of records it created.
