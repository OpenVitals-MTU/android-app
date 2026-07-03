# Reference: Health Connect Matchmaking

> **Status:** External Health Connect API reference. This is background material, not OpenVitals implementation policy.

A guide to the Health Connect Matchmaking API.

## Overview

The Matchmaking API allows your app to discover other apps and devices that can
write health data that your app has permission to read. This helps users
connect their favorite data sources to your app with less friction.

The Matchmaking screen discovers Health Connect-compatible apps and devices.
It then cross-references your app's required read permissions with the write
capabilities of those apps and devices. The screen displays apps and devices
that have declared, but not yet granted, write permissions for at least one
of the specified record types that your app is permitted to read.

> [!IMPORTANT]
> **Important:** Your app must already have read permissions granted for the record types you want to match.

## Before you begin

This guide assumes you have already
[configured Health Connect in your app](https://developer.android.com/health-and-fitness/health-connect/get-started)
and have an instance of `HealthConnectClient` available.

> [!IMPORTANT]
> **Important:** All Matchmaking APIs are experimental. You must opt in to use these APIs by annotating any usage with [`@ExperimentalMatchmakingApi`](https://developer.android.com/reference/kotlin/androidx/health/connect/client/ExperimentalMatchmakingApi).

### Check Health Connect availability

Before attempting to use Health Connect, your app should verify that Health
Connect is available on the user's device. Health Connect may not be installed
on the user's device, or it could be disabled.

Use [`HealthConnectClient.getSdkStatus()`](https://developer.android.com/reference/kotlin/androidx/health/connect/client/HealthConnectClient#getSdkStatus(android.content.Context)) to check for
availability. If Health Connect is not available, prompt the user to
install or update Health Connect from the Google Play Store.

### Check feature availability

To determine whether a user's device supports matchmaking on Health Connect,
check for the availability of `FEATURE_MATCHMAKING`:

    if (healthConnectClient
        .features
        .getFeatureStatus(
        HealthConnectFeatures.FEATURE_MATCHMAKING
        ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE) {

    // Feature is available
    } else {
    // Feature isn't available
    }

## Implementation

The matchmaking flow is launched using a `MatchmakingRequest`. This request
defines which record types you want your app to collect, and lets you include or
exclude specific data sources such as apps or devices:

- `recordTypes`: A set of `Record` classes such as `StepsRecord::class`. If empty, the flow considers all record types your app has read permissions for.
- `includedDataSources`: A set of `DataOrigin` objects to exclusively include.
- `excludedDataSources`: A set of `DataOrigin` objects to exclude.

> [!NOTE]
> **Note:** `includedDataSources` and `excludedDataSources` cannot both be set in the same request.

Follow these steps to integrate the Matchmaking API into your application.

### Check if matchmaking is possible

Before showing a matchmaking entry point, use
`checkIfMatchmakingIsPossible()` to determine if there are any relevant
matching apps or devices for the requested record types:

    suspend fun checkMatchmakingPossible(healthConnectClient: HealthConnectClient) {
        val request = MatchmakingRequest(recordTypes = setOf(StepsRecord::class))
        val response = healthConnectClient.checkIfMatchmakingIsPossible(request)

        if (response.isMatchmakingPossible) {
            // Relevant apps or devices found. Show entry point to launch flow.
        } else {
            // Handle case where no new data sources are available
        }
    }

We recommend checking if matchmaking is possible on an ongoing basis.
Users might later install additional apps or connect devices with mutually
compatible data types. A common approach is to check each time your app
starts. If it returns `true`, show an entry point to launch the matchmaking
flow.

### Launch the Matchmaking flow

If matchmaking is possible, use `createMatchmakingIntent()` to get an `Intent`
to launch the Health Connect flow, then launch it using the Activity Result
API:

    // Create the matchmaking launcher
    val matchmakingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Matchmaking finished successfully.
            // User successfully granted at least one permission.
        } else {
            // User canceled flow or didn't grant permissions.
        }
    }

    fun launchMatchmaking(healthConnectClient: HealthConnectClient) {
        val request = MatchmakingRequest(recordTypes = setOf(StepsRecord::class))
        val intent = healthConnectClient.createMatchmakingIntent(request)
        matchmakingLauncher.launch(intent)
    }

By launching this intent, Health Connect displays a screen where users can see
compatible apps and devices, and choose to connect them to share data with
your app.
![Matchmaking screen showing a list of apps that can share data with Health Connect.](https://developer.android.com/static/health-and-fitness/health-connect/images/mm1.png) **Figure 1.** Users are shown apps that can write data they might want to share.

![Matchmaking screen showing a list of apps that can share data with Health Connect.](https://developer.android.com/static/health-and-fitness/health-connect/images/mm1.png)
![Health Connect permissions screen to allow or disallow data sharing.](https://developer.android.com/static/health-and-fitness/health-connect/images/mm.png) **Figure 2.** Users grant permissions for the app to read data from Health Connect.

![Health Connect permissions screen to allow or disallow data sharing.](https://developer.android.com/static/health-and-fitness/health-connect/images/mm.png)
