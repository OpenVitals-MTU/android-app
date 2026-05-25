<p align="center">
    <img width="160" alt="OpenVitals logo" src="docs/images/readme-logo.png">
</p>

# OpenVitals: a privacy-first Health Connect dashboard

OpenVitals is an Android app for exploring your Health Connect data on-device.

It is built around a simple idea: your health data should stay yours. The app is local-first, dashboard-first, and designed to work without an account, cloud sync, ads, or analytics.

OpenVitals is still in an early stage, but the core product direction is already in place: a daily dashboard, period-based detail screens, feature-first architecture, categorized Health Connect permissions, and local display preferences for units.

## Screenshots

<div>
    <img width="23%" alt="OpenVitals dashboard" src="docs/images/readme-dashboard.png">
    <img width="23%" alt="Steps weekly chart" src="docs/images/readme-steps-week.png">
    <img width="23%" alt="Steps statistics" src="docs/images/readme-steps-statistics.png">
    <img width="23%" alt="Sleep detail" src="docs/images/readme-sleep.png">
</div>

## Features

- Daily dashboard with grouped sections for activity, recovery, intake, body, heart, vitals, mindfulness, records, and opt-in cycle data
- Period-based detail screens with `Day / Week / Month / Year` navigation
- Feature screens for Activity, Activities, Sleep, Heart & Vitals, Body, Hydration, Nutrition, Mindfulness, Cycle, Browse, Onboarding, and Settings
- Categorized Health Connect onboarding permissions, with Activity & sleep required, dashboard categories optional, and cycle tracking behind a separate explicit opt-in
- Health Connect availability checks, including unsupported device/profile handling and provider-update messaging
- Feature-gated Mindfulness support when the installed Health Connect provider exposes `FEATURE_MINDFULNESS_SESSION`
- Opt-in cycle tracking with its own dashboard section, period calendar, flow, ovulation, cervical mucus, and basal body temperature views
- Metric/Imperial unit preference in Settings, backed by shared display formatters
- Shared detail-screen scaffold with pull-to-refresh, range selection, period navigation, and calendar date picking
- Read access to Health Connect data, plus explicit hydration entry logging back to Health Connect

## Current coverage

- Activity: steps, distance, total calories burned, active calories, floors climbed, elevation gain, workout sessions
- Sleep: sessions, duration, and sleep stages
- Heart: heart rate samples and summaries, resting heart rate, HRV
- Vitals: blood pressure, SpO2, respiratory rate, body temperature, VO2 max
- Body: weight, BMI, body fat, lean mass, bone mass, basal metabolic rate
- Hydration: daily and period hydration totals, Health Connect-backed quick entry logging
- Nutrition: calories in, meals, and macros
- Mindfulness: session list and total duration when supported by Health Connect
- Cycle tracking: period days, flow levels, ovulation tests, cervical mucus observations, and basal body temperature when explicitly enabled during onboarding or in Settings
- Browse: workout, sleep, and weight records by selected period

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
- Permissions can be managed later in Settings
- Health Connect remains the source of truth; OpenVitals reads only the data you choose to share

The current manifest does not request the `INTERNET` permission.

## Permission model

OpenVitals keeps normal dashboard access separate from sensitive opt-in cycle tracking:

| Phase | Health Connect access | When requested |
|---|---|---|
| Phase 1 | Steps, distance, exercise, sleep | Required onboarding category |
| Phase 2 | Heart, recovery, body, activity extras, hydration, nutrition, mindfulness | Optional onboarding categories or Settings |
| Phase 3 | Blood pressure, SpO2, respiratory rate, body temperature, VO2 max | Optional onboarding category or Heart & Vitals screen |
| Phase 4 | Menstruation, ovulation, cervical mucus, basal body temperature | Explicit cycle-tracking opt-in during onboarding or Settings |

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
./gradlew assembleDebug
```

To run the same basic checks used by CI:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
git diff --check
```

To install on a connected device or emulator:

```bash
./gradlew installDebug
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
2. Grant Activity & sleep, then optionally grant the dashboard categories you want to expose
3. Enable Cycle tracking only if you want period, ovulation, cervical mucus, and basal temperature data shown
4. Use the dashboard as the main entry point into detail screens

## Architecture at a glance

OpenVitals is intentionally simple today:

- one Android app module
- Jetpack Compose UI
- Navigation Compose
- `ViewModel` + `StateFlow`
- Hilt constructor injection for repositories, services, and ViewModels
- Health Connect AndroidX client wrapped by `HealthConnectManager`
- feature-specific repositories for activity, sleep, heart, body, hydration, nutrition, mindfulness, cycle, and vitals
- local preferences for onboarding completion, acknowledged permissions, unit system, and cycle-tracking opt-in
- shared presentation formatters for units and date/time labels

The current architecture is documented in more detail in [`docs/architecture.md`](docs/architecture.md).

## Project layout

- [`app/`](app): Android app module
- [`app/src/main/kotlin/tech/mmarca/openvitals/features/`](app/src/main/kotlin/tech/mmarca/openvitals/features): feature screens, state, and ViewModels
- [`app/src/main/kotlin/tech/mmarca/openvitals/data/repository/`](app/src/main/kotlin/tech/mmarca/openvitals/data/repository): repositories over Health Connect reads and preferences
- [`app/src/main/kotlin/tech/mmarca/openvitals/core/`](app/src/main/kotlin/tech/mmarca/openvitals/core): shared period, performance, preference, and presentation primitives
- [`app/src/main/kotlin/tech/mmarca/openvitals/ui/components/`](app/src/main/kotlin/tech/mmarca/openvitals/ui/components): shared UI scaffolding and navigation components
- [`docs/`](docs): architecture notes, playbooks, and roadmap

## Documentation

- [`plan.md`](plan.md): product direction and scope
- [`docs/development.md`](docs/development.md): local build, verification, CI, and Windows cleanup notes
- [`docs/architecture.md`](docs/architecture.md): current architecture and target direction
- [`docs/feature-playbook.md`](docs/feature-playbook.md): checklist for adding a new metric feature
- [`docs/metrics-roadmap.md`](docs/metrics-roadmap.md): metric coverage gaps and future feature roadmap
- [`docs/units-localization-plan.md`](docs/units-localization-plan.md): display-unit and localization architecture notes
- [`AGENTS.md`](AGENTS.md): implementation guidance for future coding agents

## Roadmap

Most near-term metric expansion is already implemented. Remaining roadmap items tracked in [`docs/metrics-roadmap.md`](docs/metrics-roadmap.md) include:

- continued period/formatter cleanup
- future localization pass for hardcoded UI text

## License

OpenVitals is licensed under the [`GNU Affero General Public License v3.0 or later`](LICENSE).
Third-party asset notices are listed in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

## Status

OpenVitals is actively being shaped into a consistent Health Connect dashboard app. The current codebase already has the shared period-based detail architecture in place, but the product surface and project documentation are still evolving.
