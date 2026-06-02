<p align="center">
    <img width="160" alt="OpenVitals logo" src="docs/images/readme-logo.png">
</p>

# OpenVitals

Privacy-first Health Connect dashboard, activity tracker, and manual entry app for Android.

OpenVitals helps you review Health Connect data, record or import workouts, and add supported manual entries without creating an account or sending health data to an OpenVitals server. The dashboard is read-only by default; writes happen only when you explicitly save an entry back to Health Connect.

## Install

| Channel | Link | Best for |
| --- | --- | --- |
| Google Play | [Install or join testing](https://play.google.com/store/apps/details?id=tech.mmarca.openvitals) |  |
| Codeberg releases | [Download signed APKs](https://codeberg.org/mmarca-tech/OpenVitals/releases) ||
| Source | [Codeberg](https://codeberg.org/mmarca-tech/OpenVitals) / [GitHub mirror](https://github.com/mmarca-tech/OpenVitals) | |

## Why OpenVitals

- No account, no ads, no analytics SDKs, no cloud health-data sync
- No app-level `INTERNET` permission in the current manifest
- Health Connect remains the source of truth
- Manual entries are written only after an explicit save action
- Sensitive cycle data is opt-in and disabled by default
- Open source under AGPL-3.0-or-later

## Highlights

- Daily dashboard for activity, recovery, intake, body, heart, vitals, mindfulness, and optional cycle data
- Period detail screens with `Day / Week / Month / Year` navigation
- Health Connect permission onboarding with clear data categories and a one-tap full setup option
- Manual logging for hydration, body measurements, vitals, mindfulness, and activities
- Opt-in hydration reminders with active hours and daily-goal pause logic
- Achievement badges for activity, distance, floors, workouts, hydration, sleep, and mindfulness
- GPX/KML/KMZ route import and GPS activity recording with review before saving
- Metric and imperial unit support

## Help Improve It

OpenVitals is still early. Useful feedback is specific: device model, Android version, Health Connect provider version, which permissions were granted, and what screen or workflow failed.

- Try the latest beta from Google Play or Codeberg releases
- Report bugs and feature requests on [Codeberg issues](https://codeberg.org/mmarca-tech/OpenVitals/issues)
- Star or follow the project on [Codeberg](https://codeberg.org/mmarca-tech/OpenVitals) or the [GitHub mirror](https://github.com/mmarca-tech/OpenVitals)
- Share screenshots or notes from real Health Connect setups, especially route recording and manual entry flows

## Screenshots

<div>
    <img width="23%" alt="OpenVitals dashboard" src="docs/images/readme-dashboard.png">
    <img width="23%" alt="Steps weekly chart" src="docs/images/readme-steps-week.png">
    <img width="23%" alt="Steps statistics" src="docs/images/readme-steps-statistics.png">
    <img width="23%" alt="Sleep detail" src="docs/images/readme-sleep.png">
</div>

## Features

- Daily dashboard with grouped sections for activity, recovery, intake, body, heart, vitals, mindfulness, and opt-in cycle data
- Material 3 app shell with Settings as a top-bar icon and Add entry exposed as a contextual create action
- Period-based detail screens with `Day / Week / Month / Year` navigation
- Feature screens for Activity, Activities, Sleep, Heart & Vitals, Body, Hydration, Nutrition, Mindfulness, Cycle, Manual entry, Onboarding, and Settings
- Categorized Health Connect onboarding permissions, with one-tap full setup, category-by-category review, and cycle tracking behind a separate explicit opt-in
- Write-permission requests available during one-tap setup or from Add entry and metric entry screens, while dashboard views stay read-only
- Achievement screen with Fitbit-inspired badges and progress for daily steps, lifetime distance, floors, workouts, hydration, sleep, and mindfulness
- Health Connect availability checks, including unsupported device/profile handling and provider-update messaging
- Feature-gated Mindfulness support when the installed Health Connect provider exposes `FEATURE_MINDFULNESS_SESSION`
- Opt-in cycle tracking with its own dashboard section, period calendar, flow, ovulation, cervical mucus, and basal body temperature views
- Metric/Imperial unit preference in Settings, backed by shared display formatters
- Shared detail-screen scaffold with pull-to-refresh, range selection, period navigation, and calendar date picking
- Explicit manual entry logging for hydration, activities with optional GPX/KML/KMZ route import or GPS recording, body measurements, vitals, and mindfulness sessions, written directly to Health Connect

## Current coverage

- Activity: steps, distance, total calories burned, active calories, floors climbed, elevation gain, workout sessions
- Sleep: sessions, duration, and sleep stages
- Heart: heart rate samples and summaries, resting heart rate, HRV
- Vitals: blood pressure, SpO2, respiratory rate, body temperature, VO2 max
- Body: weight, BMI, body fat, lean mass, bone mass, basal metabolic rate
- Manual entry: hydration, activity sessions with optional GPX/KML/KMZ route import or GPS recording, mindfulness sessions, weight, height, body fat, blood pressure, SpO2, respiratory rate, and body temperature
- Hydration: daily and period hydration totals, Health Connect-backed drink logging with drink and serving choices, and optional reminders
- Achievements: badge progress for activity, distance, floors, workouts, hydration, sleep, and mindfulness milestones
- Nutrition: calories in, meals, and macros
- Mindfulness: session list and total duration when supported by Health Connect, plus timer-based and manual session logging with bell previews and optional looping background sounds
- Cycle tracking: period days, flow levels, ovulation tests, cervical mucus observations, and basal body temperature when explicitly enabled during onboarding or in Settings
- Entry and session lists are reached from the relevant metric detail screen rather than a global records browser

## Privacy

- No account required
- No cloud sync of health data
- No ads
- No analytics SDKs
- No Google Play Services dependency for app functionality
- Permissions are requested by clear Health Connect categories:
  - Activity & sleep: required for the dashboard
  - Heart & recovery, Body, Activity extras, Nutrition & hydration, Mindfulness, and Vitals: optional
  - Cycle tracking: sensitive optional access, requested only after explicitly enabling it during onboarding or in Settings
  - Manual entry write access: available from one-tap onboarding or when you use Add entry or a metric entry screen that needs it
- Permissions can be managed later in Settings
- Health Connect remains the source of truth; OpenVitals does not store health records locally

The current manifest does not request the `INTERNET` permission.

## Platform requirements

- Android only
- `minSdk 26`
- `targetSdk 36`
- JDK 17 / Java 17 toolchain
- Health Connect required

Health Connect platform notes:

- On Android 14 and newer, Health Connect is part of the system
- On Android 13 and older, the Health Connect app must be installed separately
- Health Connect is not supported in work profiles
- Mindfulness sessions require a Health Connect provider version that supports `FEATURE_MINDFULNESS_SESSION`
- The app uses `androidx.health.connect:connect-client` 1.1.0 or newer so AndroidX maps mindfulness sessions to the platform `android.permission.health.READ_MINDFULNESS` permission

## Build from source

1. Install a recent Android Studio with Android SDK 36 and JDK 17 support.
2. Clone this repository.
3. Open the project in Android Studio, or build from the command line.

In a complete checkout:

```bash
./gradlew :app:assembleDebug
```

To run the same basic checks used by CI:

```bash
./gradlew verifyLocalApp
git diff --check
```

To install on a connected device or emulator:

```bash
./gradlew :app:installDebug
```

On Windows, Gradle or Android Studio can occasionally keep lint cache jars open under `app/build`. If cleaning fails with a locked `lint-cache` jar, stop Gradle daemons first:

```powershell
.\gradlew.bat --stop
Get-CimInstance Win32_Process |
  Where-Object { $_.CommandLine -like '*org.gradle.launcher.daemon.bootstrap.GradleDaemon*' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Remove-Item -LiteralPath app/build -Recurse -Force
```

More local development notes are in [`docs/development.md`](docs/development.md).

After launching the app:

1. Complete onboarding
2. Use one-tap setup to grant all requestable Health Connect permissions, or grant Activity & sleep first and then choose individual categories
3. Enable Cycle tracking only if you want period, ovulation, cervical mucus, and basal temperature data shown
4. Use Dashboard for read-only summaries and Add entry for explicit Health Connect logging

## Architecture at a glance

OpenVitals is intentionally simple today:

- one local Android app module
- versioned shared Maven artifacts for code reused by separate OpenVitals apps
- Jetpack Compose UI with Material 3 app shell and theming
- Navigation Compose
- `ViewModel` + `StateFlow`
- Hilt constructor injection for repositories, services, and ViewModels
- Health Connect AndroidX client wrapped by `HealthConnectManager`
- feature-specific repositories for activity, sleep, heart, body, hydration, nutrition, mindfulness, cycle, and vitals
- local preferences for onboarding completion, acknowledged permissions, unit system, widget order, timer/background-sound settings, hydration reminders, and cycle-tracking opt-in
- shared presentation formatters for units and date/time labels

The current architecture is documented in more detail in [`docs/architecture.md`](docs/architecture.md).

## Project layout

- [`app/`](app): Android app module
- [`core/period/`](core/period): publishable period/date-window artifact
- [`app/src/main/kotlin/tech/mmarca/openvitals/features/`](app/src/main/kotlin/tech/mmarca/openvitals/features): feature screens, state, and ViewModels
- [`app/src/main/kotlin/tech/mmarca/openvitals/data/repository/`](app/src/main/kotlin/tech/mmarca/openvitals/data/repository): repositories over Health Connect reads and preferences
- [`app/src/main/kotlin/tech/mmarca/openvitals/core/`](app/src/main/kotlin/tech/mmarca/openvitals/core): app-local performance, preference, and presentation primitives
- [`app/src/main/kotlin/tech/mmarca/openvitals/ui/components/`](app/src/main/kotlin/tech/mmarca/openvitals/ui/components): shared UI scaffolding and navigation components
- [`docs/`](docs): architecture notes, playbooks, and roadmap

## Documentation

- [`plan.md`](plan.md): product direction and scope
- [`docs/development.md`](docs/development.md): local build, verification, CI, and Windows cleanup notes
- [`docs/architecture.md`](docs/architecture.md): current architecture and target direction
- [`docs/feature-playbook.md`](docs/feature-playbook.md): checklist for adding a new metric feature
- [`docs/shared-artifacts.md`](docs/shared-artifacts.md): Maven artifact publishing and consumption notes
- [`docs/metrics-roadmap.md`](docs/metrics-roadmap.md): metric coverage gaps and future feature roadmap
- [`docs/units-localization-plan.md`](docs/units-localization-plan.md): display-unit and localization architecture notes
- [`AGENTS.md`](AGENTS.md): implementation guidance for future coding agents


## License

OpenVitals is licensed under the [`GNU Affero General Public License v3.0 or later`](LICENSE).
Project thanks are listed in [`THANKS.md`](THANKS.md), and third-party asset notices are listed in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
