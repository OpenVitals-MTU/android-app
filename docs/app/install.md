# Install

OpenVitals is an Android app that uses Health Connect as the source of truth for health and fitness records.

## Channels

| Channel | Use When |
| --- | --- |
| Google Play | You want the normal Android install and update path. |
| Codeberg releases | You want signed APKs published by the project. |
| Source build | You want to inspect or build the app from this repository. |

## Requirements

- Android only.
- `minSdk` 26.
- `targetSdk` 36.
- Health Connect required for health data access.

## Health Connect

Android 14 and newer include Health Connect as part of the system.

Android 13 and older normally need the separate Health Connect app installed before OpenVitals can read health records.

Health Connect is not supported in Android work profiles, so OpenVitals cannot read Health Connect data from a work profile.

## After Installing

1. Open OpenVitals.
2. Complete onboarding.
3. Grant the Health Connect read permissions you want OpenVitals to use.
4. Grant cycle permissions only if you explicitly want cycle data shown.
5. Use Add entry, imports, or recording only when you want OpenVitals to write records back to Health Connect.

See [Getting started](getting-started.md) for the first-run flow.
