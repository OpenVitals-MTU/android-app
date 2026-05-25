# Health Connect Dashboard App Plan

## Product goal

Build an Android app that acts like a lightweight, privacy-first Google Fit dashboard:

* reads all available Health Connect data the user chooses to share
* shows tr([developer.android.com](https://developer.android.com/health-and-fitness/health-connect/get-started?utm_source=chatgpt.com))ccount
* does not depend on Google Play services APIs for app functionality
* stores as little extra data as possible

## Important platform constraints

* On Android 14 and higher, Health Connect is part of the system.
* On Android 13 and lower, the user must install the Health Connect app separately.
* The app can be fully account-free because Health Connect is on-device storage.
* Historical reads beyond 30 days need the dedicated history permission.
* Background reads need a separate background permission.
* Some Health Connect features require availability checks.
* Health Connect is not supported in work profiles.

## Product principles

1. Local-first
2. No account
3. No cloud sync in v1
4. Read-only by default; explicit hydration logging writes only to Health Connect
5. Per-metric permissions only
6. Explain exactly why each permission is needed
7. Degrade gracefully when some permissions are denied

## V1 scope

### Included

* daily dashboard
* steps
* distance
* exercise sessions
* sleep sessions
* heart rate summaries
* weight/body measurements if available
* calories if available
* hydration if available
* hydration entry logging, stored only in Health Connect
* source attribution by app/device
* trends for 7d / 30d / 90d
* record browser for raw entries

### Excluded

* write-back to Health Connect outside explicit hydration logging
* social features
* account sync
* wearables integration outside Health Connect
* goal coaching
* medical records UI
* menstrual / sensitive categories unless explicitly enabled later

## Core UX

### 1. Onboarding

* explain that data stays on device
* explain that the app reads from Health Connect only
* show platform check:

  * Health Connect available
  * needs install on Android 13 and lower
  * unsupported state if unavailable
* request permissions in groups

### 2. Home dashboard

Cards:

* Today steps
* Today distance
* Latest workout
* Last night sleep
* Resting / average heart rate if available
* Latest weight if available
* Weekly activity score

### 3. Explore tabs

* Activity
* Sleep
* Body
* Heart
* Nutrition
* Browse

### 4. Detail screens

Each metric gets:

* current value
* trend chart
* daily / weekly / monthly aggregates
* source breakdown
* raw record list

### 5. Settings

* permission manager
* data categories enabled
* refresh controls
* export app settings
* privacy page
* debug screen with feature availability and granted permissions

## Architecture

### Stack

* Kotlin
* Jetpack Compose
* MVVM or MVI
* Health Connect AndroidX client
* Kotlin coroutines + Flow
* MPAndroidChart or Compose-native charts

### Modules

* app
* core-ui
* core-data
* feature-dashboard
* feature-activity
* feature-sleep
* feature-body
* feature-heart
* feature-browse
* feature-settings
* integration-healthconnect

## Data model approach

### Canonical source

Health Connect remains the source of truth.

### Local storage

Store only:

* granted-permission state snapshot
* last refresh timestamps
* cached daily aggregates for fast startup
* UI preferences
* optional bookmarked views

Avoid copying raw health history unless there is a strong UX reason.

## Health Connect integration strategy

### Permissions strategy

Request in phases, not all at once.

Phase 1:

* steps
* distance
* exercise
* sleep

Phase 2:

* heart rate
* weight
* hydration
* calories

Phase 3 optional:

* background read
* history read beyond 30 days

### Availability strategy

At startup check:

* SDK status
* feature availability for optional APIs
* granted permissions
* whether history/background permissions are available and granted

## Reading strategy

### Dashboard reads

Use aggregate reads for most cards:

* today steps
* weekly distance
* average sleep duration
* monthly workout count
* resting heart rate averages where appropriate

### Detail reads

Use raw reads for:

* workout list
* sleep session list
* weight entries
* hydration entries
* source browser

### Time ranges

Default ranges:

* today
* 7 days
* 30 days
* 90 days
* 1 year if history permission exists

## Main screens in more detail

### Dashboard

Sections:

* Today
* This week
* Recent sessions
* Trends
* Missing permissions / missing data hints

### Activity

* steps total by day
* distance by day
* workouts by type
* latest workouts list
* optional route preview only if route permissions and route reads are supported by the chosen design

### Sleep

* last sleep session
* sleep duration trend
* sleep stages if available from connected sources
* raw sleep session browser

### Heart

* resting heart rate trend
* average daily HR
* spot measurements list
* source breakdown

### Body

* weight trend
* body fat if present
* BMI derived locally when enough data exists

### Nutrition

* hydration totals
* calories consumed if present
* calories burned if present

### Browse

A power-user screen:

* choose record type
* choose date range
* filter by source app/device
* inspect raw records

## Source attribution

Health Connect records include data origin/source information.
The UI should show:

* imported from OpenTracks
* imported from Fitbit
* imported from Samsung Health
* android on-device step source

This becomes one of the strongest differentiators versus a generic dashboard.

## Refresh model

### Foreground

* full refresh when app opens
* pull to refresh
* incremental refresh per screen

### Background optional

* daily refresh job for summaries
* only if background read permission is granted
* otherwise no background work

## Privacy model

### V1 privacy promises

* no account
* no cloud backup of health data by the app
* no analytics SDK
* no ads
* no third-party telemetry
* only store cached summaries needed for speed
* clear delete-cache action

## Performance plan

### Avoid heavy reads on launch

Startup should fetch only:

* today aggregates
* last workout
* last sleep
* latest weight

Load detailed history lazily when entering a tab.

### Caching

* cache daily aggregates by metric and day
* invalidate cache on manual refresh or once daily
* keep cache separate from raw records

## UI design direction

### Style

* dark mode first
* simple cards
* dense but readable charts
* emphasis on trends and provenance

### Components

* summary cards
* sparkline charts
* time-range segmented controls
* source chips
* metric filters
* permission callouts

## Engineering phases

### Phase 0: Spike

* create project
* wire Health Connect availability
* request steps + exercise permissions
* read today steps and latest workout

### Phase 1: MVP dashboard

* onboarding
* dashboard cards
* activity details
* sleep details
* settings / permissions screen

### Phase 2: Expanded data

* heart
* body
* hydration
* nutrition basics
* source breakdown

### Phase 3: Power-user features

* raw record browser
* advanced filters
* background refresh
* history reads beyond 30 days
* export screenshots / CSV summaries

## Risk areas

1. Permission fatigue
   The app must avoid asking for every health permission on first launch.

2. Sparse data
   Some users will have only steps and workouts. The UI must still feel complete.

3. Source inconsistency
   Different apps may write overlapping or differently structured records.

4. API availability differences
   Some features vary by Android version or Health Connect version.

5. History limits
   Without history permission, long-term charts need clear messaging.

## Suggested MVP record types

Start with these:

* StepsRecord
* DistanceRecord
* ExerciseSessionRecord
* SleepSessionRecord
* HeartRateRecord
* WeightRecord
* TotalCaloriesBurnedRecord
* HydrationRecord

## Suggested package structure

```text
tech.mmarca.openvitals/
  MainActivity.kt
  navigation/
  ui/
  data/
  healthconnect/
  features/dashboard/
  features/activity/
  features/sleep/
  features/heart/
  features/body/
  features/browse/
  features/settings/
```

## Recommended first milestone

Build a thin but polished v0.1 with only:

* onboarding
* permission flow
* today dashboard
* steps
* workouts
* sleep
* settings/debug page

That is enough to prove the concept before adding every metric.

## Differentiators versus Google Fit

* no sign-in
* local-first
* source transparency
* no dependency on Google Fit cloud
* permission-by-category
* raw record browsing for power users

## Nice future additions

* custom dashboard cards
* widget support
* homescreen quick stats
* markdown-like health journal notes stored locally
* local encrypted export of app preferences
* optional companion importer for GPX and CSV into Health Connect
