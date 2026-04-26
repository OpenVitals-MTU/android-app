# Units and Localization Plan

## Problem

OpenVitals stores health values in canonical Health Connect-derived units, but formats display values directly inside Compose screens.

Examples today:

- distance and elevation are rendered as `m` / `km`
- weight and body mass are rendered as `kg`
- hydration is rendered as `L`
- body temperature is rendered as Celsius
- energy is rendered as `kcal`
- dates and times use fixed English-oriented `DateTimeFormatter` patterns

This makes imperial units, locale-aware formatting, and translated UI text hard to add without repeatedly editing feature screens.

## Goals

- Keep stored and repository values canonical.
- Convert units only at the presentation boundary.
- Add a user-selectable unit preference.
- Keep formatter APIs metric-specific so units cannot be accidentally reused incorrectly.
- Centralize number and date/time formatting.
- Migrate screens incrementally without changing repository contracts.

## Non-goals

- Do not change Health Connect reads.
- Do not rename canonical fields such as `weightKg`, `distanceMeters`, or `temperatureCelsius`.
- Do not add DataStore unless preferences grow beyond the current `SharedPreferences` scope.
- Do not translate every hardcoded string in the first implementation pass.

## Canonical Values

Canonical model fields should keep their current explicit units:

- `distanceMeters`, `elevationGainedMeters`
- `weightKg`, `leanMassKg`, `boneMassKg`
- `hydrationLiters`
- `caloriesKcal`, `energyKcal`
- `temperatureCelsius`
- `avgHeartRateBpm`, `restingHeartRateBpm`
- `systolicMmHg`, `diastolicMmHg`
- `vo2MaxMlPerKgPerMin`

These names are useful guardrails because they prevent double conversion and make repository behavior obvious.

## Unit Preference

Add:

```kotlin
enum class UnitSystem {
    METRIC,
    IMPERIAL,
}
```

Store it in `PreferencesRepository` as `unitSystem`.

Default behavior:

- infer from the current locale/region before a user preference exists
- use `IMPERIAL` for `US`, `LR`, and `MM`
- use `METRIC` elsewhere

The Settings screen should expose a simple unit selector.

## Presentation API

Add:

```kotlin
data class DisplayValue(
    val value: String,
    val unit: String,
)
```

Add `UnitFormatter` with explicit metric methods:

```kotlin
fun count(value: Long): String
fun distance(meters: Double): DisplayValue
fun elevation(meters: Double): DisplayValue
fun weight(kg: Double): DisplayValue
fun bodyMass(kg: Double, decimals: Int = 1): DisplayValue
fun hydration(liters: Double): DisplayValue
fun energy(kcal: Double): DisplayValue
fun temperature(celsius: Double): DisplayValue
fun percent(value: Double, decimals: Int = 1): DisplayValue
fun heartRate(bpm: Long): DisplayValue
fun bloodPressure(systolic: Int, diastolic: Int): DisplayValue
fun respiratoryRate(value: Double): DisplayValue
fun vo2Max(value: Double): DisplayValue
fun duration(durationMs: Long): String
```

Do not expose a generic `formatDoubleWithUnit` as the main API. Health metric semantics determine the correct unit and rounding.

## Localization API

Add `DateTimeFormatterProvider` with locale-aware methods for:

- compact day labels for charts
- medium date labels
- date-time labels
- short time labels
- period titles and subtitles later

The first implementation should centralize new formatting but does not need to convert every string to Android resources at once.

## Wiring

Create formatter instances in `OpenVitalsApp`:

```kotlin
val unitFormatter by lazy { UnitFormatter(preferencesRepository) }
val dateTimeFormatterProvider by lazy { DateTimeFormatterProvider() }
```

Pass them through `AppNavigation` to feature screens.

Formatting should happen in composables, not repositories. This keeps repository APIs stable and avoids reloading data when a display preference changes.

## Migration Order

1. Add `UnitSystem`, `DisplayValue`, `UnitFormatter`, and `DateTimeFormatterProvider`.
2. Extend `PreferencesRepository`.
3. Add a Settings unit selector.
4. Convert Dashboard first.
5. Convert Activity and Activities.
6. Convert Body and Browse.
7. Convert Hydration and Nutrition.
8. Convert Heart and Vitals.
9. Add formatter unit tests.
10. Move remaining hardcoded UI text into `strings.xml` in a later dedicated localization pass.

## Testing

Formatter tests should cover:

- metric and imperial distance
- metric and imperial elevation
- metric and imperial weight/body mass
- metric and imperial hydration
- metric and imperial temperature
- unchanged units: bpm, mmHg, percent, respiratory rate, VO2 max
- count formatting and duration formatting

Existing ViewModel tests should remain mostly unchanged because data state stays canonical.
