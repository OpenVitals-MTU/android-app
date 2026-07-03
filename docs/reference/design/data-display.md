# Reference: Data Display

> **Status:** External Health Connect design reference. This is background material, not OpenVitals implementation policy.

To assure users that their data is being read correctly, clearly show how your
app obtains data, which comes from the
[`packageName` property of the `DataOrigin` class](https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/metadata/DataOrigin#packageName()).

There are two ways to achieve this:

1. [Basic attribution](https://developer.android.com/health-and-fitness/health-connect/ui/data#basic)
2. [Attribution with education](https://developer.android.com/health-and-fitness/health-connect/ui/data#education)

## Basic attribution

At a minimum, your user interface (UI) should display the app source icon
and name (or only the app name if the icon can't be shown).
Basic attribution is suitable for the following screens: Home,
Activity log, and Activity details.

To support proper attribution, your application can display the name and icon
of the application that originally recorded the data. This improves user trust
and provides clarity about where health information originated.

You don't need to request any sensitive permission, such as
`QUERY_ALL_PACKAGES`, in order to retrieve this information. The following
example demonstrates how to retrieve the app label and icon for a given package
from the `PackageManager`:

    fun getAppLabelAndIcon(context: Context, packageName: String): Pair<CharSequence?, Drawable?>{
        return try {
          val pm = context.packageManager
          val appInfo = pm.getApplicationInfo(packageName, 0)
          val label = pm.getApplicationLabel(appInfo)
          val icon = pm.getApplicationIcon(appInfo)
          label to icon
        } catch (e: PackageManager.NameNotFoundException){
          null to null
        }
    }

This utility helps verify proper attribution by displaying both the app name
and icon alongside the data.
See the implementation in the [HealthConnectManager.kt sample](https://github.com/android/health-samples/blob/main/health-connect/HealthConnectSample/app/src/main/java/com/example/healthconnectsample/data/HealthConnectManager.kt#L89).
![Basic attribution for reading data](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_basic_attribution.png) **Figure 1**: Basic attribution for reading data

## Attribution with education

Your app should help users obtain information about where data
originates from, with a direct link to the "App permissions" screen in
Health Connect. This type of attribution is suitable for the following screens:
Activity details, Reports and insights.
![Attribution in activity details and report screens in the partner app](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_education_variation.png) ![Attribution in insight screens in the partner app](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_education_variation_3.png) **Figure 2**: Attribution with education variations

![Attribution in activity details and report screens in the partner app](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_education_variation.png)
![Attribution in insight screens in the partner app](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_education_variation_3.png)

## Data Sync

If there's enough latency when syncing your app with Health Connect, show this
notification in your app during sync. This informs the user that the process may
take a while to finish. If you use notifications for syncing, they should be set
to a low priority by default.
![Data sync status shown](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_data_sync.png) **Figure 3**: Data sync status shown
