# Reference: Health Connect Permissions

> **Status:** External Health Connect design reference. This is background material, not OpenVitals implementation policy.

Your app's Settings screen should provide users with options to manage their
connection to Health Connect. This gives users control over data
synchronization and access to their data.
![Revoked and cancelled permissions](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_revoked_cancelled_permissions.png) **Figure 1**: Revoked and cancelled permissions

## Sync with Health Connect

This toggle provides a way for users to pause or resume data
synchronization between your app and Health Connect.

- **When toggled on:** Your app actively reads and writes to Health Connect, as per the permissions granted by the user.
- **When toggled off:** Your app should stop all data synchronization with Health Connect. If you programmatically revoke permissions using [`revokeAllPermissions()`](https://developer.android.com/reference/kotlin/androidx/health/connect/client/PermissionController#revokeAllPermissions()), explain to the user that the changes aren't immediately reflected in Health Connect without an app restart. To avoid a confusing user experience, give users the option to go to Health Connect settings to revoke permissions there.

## Manage access

The **Manage access** button should provide a direct link for the user to manage
your app's permissions from within the Health Connect app. This gives the user
full control and transparency.

## Insufficient access

If your app has insufficient Health Connect access, users should be presented
with the following screen across all entry points:
![App with insufficient access](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_insufficient_access.png) **Figure 2**: App with insufficient access

## Permissions cancelled twice

If the user selects **Cancel** on the permissions request screen twice in a
row, your app should present the user with a screen similar to the following:
![Permissions cancelled twice by user](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_permissions_cancelled_twice.png) **Figure 3**: Permissions cancelled twice by user

> [!NOTE]
> **Note:** Once this screen is displayed, users need to re-enable permissions from within the Health Connect Settings menu.
