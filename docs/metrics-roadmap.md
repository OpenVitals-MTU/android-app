# Metrics Roadmap

Gap analysis between what the app currently shows and all Health Connect record types that can meaningfully be surfaced to the user, plus a phased implementation plan.

---

## Current coverage

| Category | Metric | HC Record type | Where |
|---|---|---|---|
| Activity | Steps | `StepsRecord` | Activity screen + Dashboard |
| Activity | Distance | `DistanceRecord` | Activity screen + Dashboard |
| Activity | Total calories burned | `TotalCaloriesBurnedRecord` | Activity screen + Dashboard |
| Activity | Hydration | `HydrationRecord` | Hydration screen + Dashboard |
| Nutrition | Calories in + macros | `NutritionRecord` | Nutrition screen + Dashboard |
| Exercise | Workout sessions | `ExerciseSessionRecord` | Activities screen |
| Sleep | Duration + stages | `SleepSessionRecord` | Sleep screen |
| Heart | Avg/min/max heart rate | `HeartRateRecord` | Heart screen + Dashboard |
| Heart | Resting HR | `RestingHeartRateRecord` | Heart screen + Dashboard |
| Heart | HRV RMSSD | `HeartRateVariabilityRmssdRecord` | Heart screen |
| Body | Weight | `WeightRecord` | Body screen |
| Heart / Vitals | Blood pressure, SpO2, respiratory rate, body temp, VO2 max | `BloodPressureRecord`, `OxygenSaturationRecord`, `RespiratoryRateRecord`, `BodyTemperatureRecord`, `Vo2MaxRecord` | Heart & Vitals screen + Dashboard |

---

## Missing data points

### Vitals — widely available from wearables ✓ Covered

| HC Record type | Metric | Source examples |
|---|---|---|
| `Vo2MaxRecord` | VO2 max mL/kg/min | Garmin, Polar |
| `BloodPressureRecord` | Systolic / diastolic mmHg | Smart BP cuffs, some wearables |
| `OxygenSaturationRecord` | SpO2 % | Most modern wearables |
| `RespiratoryRateRecord` | Breaths per minute | Garmin, Polar |
| `BodyTemperatureRecord` | Body temp °C | Some wearables, manual entry |

### Body composition — mostly smart scales

| HC Record type | Metric |
|---|---|
| `HeightRecord` | Height cm (needed for BMI computation) |
| `BodyFatRecord` | Body fat % |
| `LeanBodyMassRecord` | Lean mass kg |
| `BoneMassRecord` | Bone mass kg |
| `BasalMetabolicRateRecord` | BMR kcal/day |

### Activity extras — wearables and fitness apps

| HC Record type | Metric |
|---|---|
| `FloorsClimbedRecord` | Floors climbed |
| `ActiveCaloriesBurnedRecord` | Active calories (distinct from total) |
| `ElevationGainedRecord` | Elevation gain m |

### Nutrition — food diary apps ✓ Covered

| HC Record type | Metric |
|---|---|
| `NutritionRecord` | Calories IN, protein, carbs, fat, fiber, sugar per meal |

### Mindfulness

| HC Record type | Metric |
|---|---|
| `MindfulnessSessionRecord` | Session title + duration |

### Women's health (opt-in)

| HC Record type | Metric |
|---|---|
| `MenstruationFlowRecord` | Flow level per day |
| `MenstruationPeriodRecord` | Cycle period dates |
| `OvulationTestRecord` | LH / ovulation test result |
| `CervicalMucusRecord` | Mucus observation |
| `BasalBodyTemperatureRecord` | BBT for cycle tracking |

---

## Implementation plan

### Phase A — Expand existing screens

Low risk. No new nav destinations. Permissions added to `phase2Permissions`.

#### A1. Heart screen: Resting HR + HRV ✓ Done

Resting HR and HRV are fully implemented: `HeartRepository` loads them, `HeartViewModel` exposes `dayRestingBpm` and `dayHrvMs`, and `HeartScreen` renders day cards and multi-day trend charts for both. `DashboardData` includes `restingHeartRateBpm`.

---

#### A2. Body screen: Height + BMI + body composition ✓ Done (BoneMassRecord added 2026-04-25)

**New permissions:** `HeightRecord`, `BodyFatRecord`, `LeanBodyMassRecord`, `BasalMetabolicRateRecord`, `BoneMassRecord`

`BodyRepository`:
- `loadLatestHeight(): Double?` — cm
- `loadLatestBodyFat(date: LocalDate): Double?` — percent
- `loadBodyFatEntries(start: LocalDate, end: LocalDate): List<BodyFatEntry>`
- `loadLatestLeanBodyMass(): Double?` — kg
- `loadLatestBMR(): Double?` — kcal/day
- `loadLatestBoneMass(): Double?` — kg *(gap: not yet implemented)*

New models:
```kotlin
data class BodyFatEntry(val time: Instant, val percent: Double, val source: String)
```

BMI computed in the ViewModel when both `heightCm` and `weightKg` are present: `weightKg / (heightCm/100)^2`.

`BodyUiState` extended: `heightCm`, `bodyFatPercent`, `leanMassKg`, `bmrKcal`, `boneMassKg`, `bmi` (derived).

`BodyScreen`: add composition tiles (BMI, body fat %, lean mass, BMR, bone mass) below the weight chart. Body fat % gets its own mini trend chart if entries exist over the period.

`DashboardData`: add `bodyFatPercent: Double?` for the Body dashboard card subtitle.

---

#### A3. Activity screen: Floors + Active calories + Elevation

**New permissions:** `FloorsClimbedRecord`, `ActiveCaloriesBurnedRecord`, `ElevationGainedRecord`

Extend `DailySteps`:
```kotlin
data class DailySteps(
    val date: LocalDate,
    val steps: Long,
    val distanceMeters: Double,
    val floorsClimbed: Int? = null,
    val activeCaloriesKcal: Double? = null,
    val elevationGainedMeters: Double? = null,
)
```

`ActivityRepository.loadDailySteps` — add the three new metrics to the existing `aggregateGroupByDuration` call.

`ActivityScreen`: add Floors, Active Calories, and Elevation bar charts alongside Steps and Distance.

`DashboardData`: add `floorsClimbed: Int?`; show in the daily summary section.

---

#### A4. Hydration detail screen ✓ Done (2026-04-26)

**No new permissions needed** — `HydrationRecord` is already in phase2.

Hydration now has a dedicated period-based detail screen consistent with the other metrics.

`HydrationRepository`:
- `loadDailyHydration(start: LocalDate, end: LocalDate): List<DailyHydration>`

New model:
```kotlin
data class DailyHydration(val date: LocalDate, val liters: Double)
```

`HydrationScreen`: daily/period summary cards, logged-day count, and period bar chart.

Navigation: `Screen.Hydration` route and `onOpenHydration` callback from Dashboard.

---

### Phase B — New feature screens

Each follows the established feature pattern: `ViewModel` + `Screen` in `features/<name>/`, using `MetricDetailScaffold` from `ui/components/` for the period shell.

#### B1. Heart & Vitals screen (`features/heart/`) ✓ Done (2026-04-26)

**New permissions (phase 3 — requested on first open of Heart & Vitals):**
`BloodPressureRecord`, `OxygenSaturationRecord`, `RespiratoryRateRecord`, `BodyTemperatureRecord`, `Vo2MaxRecord`

New models:
```kotlin
data class BloodPressureEntry(val time: Instant, val systolicMmHg: Int, val diastolicMmHg: Int, val source: String)
data class SpO2Entry(val time: Instant, val percent: Double, val source: String)
data class RespiratoryRateEntry(val time: Instant, val breathsPerMinute: Double, val source: String)
data class BodyTempEntry(val time: Instant, val temperatureCelsius: Double, val source: String)
data class Vo2MaxEntry(val time: Instant, val vo2MaxMlPerKgPerMin: Double, val source: String)
```

`HeartUiState`: holds lists of each vitals type for the selected period alongside heart rate, resting HR, and HRV state.

`HeartScreen`: renders the heart sections plus cardiovascular vitals (BP chart with systolic/diastolic bands, SpO2 line, VO2 max tile) and respiratory vitals (respiratory rate, body temperature).

`DashboardData`: add `latestSystolicMmHg: Int?`, `latestDiastolicMmHg: Int?`, `latestSpO2Percent: Double?`, `latestVo2Max: Double?`.

Dashboard: merged Heart section with heart rate, resting HR, BP, SpO2, and VO2 max cards/placeholders.

Navigation: all heart and vitals cards open `Screen.Heart`.

---

#### B2. Nutrition screen (`features/nutrition/`) ✓ Done (2026-04-26)

**New permissions (phase 2):** `NutritionRecord`

Note: `NutritionRecord` covers calories IN (food), distinct from `TotalCaloriesBurnedRecord` (calories OUT). The existing `DailyNutrition` model in `ActivityRepository` is calories *burned* — this is a separate record type and a separate screen.

New models:
```kotlin
data class NutritionEntry(
    val time: Instant,
    val mealType: Int,          // MealType constants from HC
    val name: String?,
    val energyKcal: Double?,
    val proteinGrams: Double?,
    val carbsGrams: Double?,
    val fatGrams: Double?,
    val fiberGrams: Double?,
    val sugarGrams: Double?,
    val source: String,
)

data class DailyMacros(
    val date: LocalDate,
    val energyKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)
```

`NutritionScreen`: macro summary bar (protein/carbs/fat proportion), daily energy chart, meal entry list.

`DashboardData`: add `caloriesInKcal: Double?`; show alongside `caloriesKcal` (burned) as a calories in/out pair in the daily summary section.

Navigation: `Screen.Nutrition` route.

---

#### B3. Mindfulness screen (`features/mindfulness/`)

**New permissions (phase 2):** `MindfulnessSessionRecord`

New model:
```kotlin
data class MindfulnessSession(
    val id: String,
    val title: String?,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long,
    val source: String,
) {
    val durationMinutes: Long get() = durationMs / 60_000
}
```

`MindfulnessScreen`: total minutes per period, session list (mirrors Activities screen structure exactly).

`DashboardData`: add `mindfulnessMinutes: Int?`; show as a small tile in a new "Mind" section.

Navigation: add `Screen.Mindfulness` route.

---

### Phase C — Women's health (opt-in)

Gated behind `Settings` → "Track menstrual cycle" boolean pref. Off by default.

**New permissions (phase 4, only requested when opted in):**
`MenstruationFlowRecord`, `MenstruationPeriodRecord`, `OvulationTestRecord`, `CervicalMucusRecord`, `BasalBodyTemperatureRecord`

`CycleScreen` (`features/cycle/`): monthly calendar view marking period days, flow levels, ovulation markers, and BBT trend.

No dashboard card unless opt-in is enabled (avoids surfacing sensitive data unexpectedly).

---

### Cross-cutting concerns

These are not tied to a single phase but must be decided before Phase B ships.

#### Units / localization

No unit preference layer currently exists. Required before B1 (temperature °C/°F), B2 (energy kcal/kJ), and any weight or distance fields that appear in new screens. Proposed approach: a `UserPreferences` datastore key `unitSystem: UnitSystem` (METRIC / IMPERIAL) read by a `UnitFormatter` singleton injected into ViewModels and composables. All stored values remain in SI units; conversion happens at display time only.

#### Dashboard layout evolution

The dashboard currently has a fixed card layout grouped by user mental model:
- Activity & recovery: steps, distance, floors, elevation, workouts, sleep
- Body & intake: calories, hydration, weight, body fat
- Heart: heart rate plus vitals
- Records: raw Health Connect browser

As B3 and later phases add new sections, a plan is still needed for ordering, collapsibility, placeholder states, and graceful degradation when only phase 1 permissions are granted.

---

## Priority order

| Priority | Phase | Effort | Rationale |
|---|---|---|---|
| 1 | ~~A1 — Resting HR + HRV~~ | ~~Low~~ | ✓ Done |
| 2 | ~~A3 — Floors + active calories + elevation~~ | ~~Low~~ | ✓ Done |
| 3 | ~~A2 — Body composition~~ | ~~Medium~~ | ✓ Done |
| 4 | ~~A4 — Hydration detail screen~~ | ~~Low~~ | ✓ Done |
| 5 | ~~B2 — Nutrition~~ | ~~Medium~~ | ✓ Done |
| 6 | ~~B1 — Vitals~~ | ~~Medium~~ | ✓ Done |
| 7 | B3 — Mindfulness | Low | Small scope; mirrors Activities pattern exactly |
| 8 | C — Women's health | High | Niche but important; requires settings gate + dedicated permissions |

---

## Permission phases (updated)

| Phase | Permissions | When requested |
|---|---|---|
| Phase 1 | Steps, Distance, Exercise, Sleep | First launch / onboarding |
| Phase 2 | Heart rate, Resting HR, HRV, Weight, Calories, Hydration, Floors, Active calories, Elevation, Height, Body fat, Lean mass, Bone mass, BMR, Nutrition, Mindfulness | After onboarding |
| Phase 3 | Blood pressure, SpO2, Respiratory rate, Body temperature, VO2 max | On first open of Heart & Vitals |
| Phase 4 | Menstruation, Ovulation, Cervical mucus, BBT | On opt-in in Settings |

---

## Implementation status

Comparison between this roadmap and the actual codebase as of 2026-04-26.

### Phase A

| Item | Roadmap status | Code status | Notes |
|---|---|---|---|
| A1 — Resting HR + HRV | ✓ Done | ✓ Implemented | `HeartRepository`, `HeartScreen`, `HeartViewModel` all present. `restingHeartRateBpm` in `DashboardData`. HRV not in `DashboardData` (roadmap doesn't require it there). |
| A2 — Body composition | ✓ Done | ✓ Implemented | `BodyRepository` reads Height, BodyFat, LeanMass, BMR, BoneMass. `BodyCompositionCard` shows all five. `DashboardData` includes `bodyFatPercent`. |
| A3 — Floors + active cals + elevation | ✓ Done | ✓ Implemented | `DailySteps` extended with 3 optional fields. `HealthConnectManager.readDailySteps` takes permission flags. `ActivityRepository` passes flags. `HealthRepository.loadDashboard` wires `floorsClimbed`. `ActivityScreen` shows bar charts for all 3 metrics. |
| A4 — Hydration detail screen | ✓ Done | ✓ Implemented | `HydrationRepository`, `HydrationViewModel`, and `HydrationScreen` are present. `Screen.Hydration` is registered and the dashboard hydration card opens it. |

### Phase B

| Item | Roadmap status | Code status | Notes |
|---|---|---|---|
| B1 — Heart & Vitals screen | ✓ Done | ✓ Implemented | `VitalsRepository` remains feature-facing data access, while `HeartViewModel` and `HeartScreen` own the combined Heart & Vitals surface. Phase 3 permissions are requested from Heart & Vitals. Dashboard shows merged heart/vitals cards. |
| B2 — Nutrition screen | ✓ Done | ✓ Implemented | `NutritionRecord` is in phase2 permissions. `NutritionRepository`, `NutritionViewModel`, and `NutritionScreen` are present. Dashboard shows calories in/out and `Screen.Nutrition` is registered. |
| B3 — Mindfulness screen | Not started | Not started | No `MindfulnessSessionRecord` permission, no repository, no screen. |

### Phase C

| Item | Roadmap status | Code status | Notes |
|---|---|---|---|
| C — Women's health | Not started | Not started | No cycle-related records, permissions, or screens. |

### DashboardData gaps

Fields described in the roadmap as additions to `DashboardData` that are not yet present in `HealthData.kt`:

| Field | Added by phase |
|---|---|
| `mindfulnessMinutes: Int?` | B3 |

### Navigation gaps

Routes described in the roadmap that are absent from `Screen.kt` and `AppNavigation.kt`:

- `Screen.Mindfulness` (B3)
- `Screen.Cycle` (C)
