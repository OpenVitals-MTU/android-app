Showcasing Health Connect in your app depends on the UX patterns and
conventions established in your UI.

## Focus on the user benefit

The first time you introduce Health Connect to users, provide
a meaningful reason for them to set it up.

Rather than describing Health Connect's features, tailor your
message to how a user benefits from those features.
![Health Connect user benefits](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_user_benefits.png) **Figure 1**: Health Connect user benefits

## Use clear language

Don't ask users to "Connect to Health Connect." This is a jarring sentence
and may also confuse a user's understanding of the relationship between
apps in the Health Connect ecosystem.

Your UX should help users form an idea of how Health Connect interacts with
your app, so it's important to consider which button labels work best toward
achieving that goal.

Try using verb phrases like "set up" or "get started" on your button labels.
Or, if you're launching the permissions view, use more specific button
text, like "Choose data to share."
![Health Connect clear language](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_use_clear_language.png) **Figure 2**: Health Connect clear language

## How to promote Health Connect

Here are a few ways you can promote Health Connect in your app:

- As part of your [app's setup flow](https://developer.android.com/health-and-fitness/health-connect/ui/promote#app-setup-flow).
- With a [card](https://developer.android.com/health-and-fitness/health-connect/ui/promote#card) in your app's home screen.
- Through an entrypoint within a [Settings](https://developer.android.com/health-and-fitness/health-connect/ui/promote#settings) screen.
- With an [Android 13 APK download](https://developer.android.com/health-and-fitness/health-connect/ui/promote#android-13-download) button for Android 13 versions and lower.
- When promoting [new data types](https://developer.android.com/health-and-fitness/health-connect/ui/promote#new-data-types).
- With a modal or dialog when updating the app, similar to the app's setup flow.

### App setup and requesting permissions flow

![App setup and request permissions](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_app_setup_request_permissions.png) **Figure 3**: App setup and request permissions

### Home Screen Promo Card Flow

![Home screen promo flow](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_home_screen_promo.png) **Figure 4**: Home screen promo flow

### Settings

![Settings with entry point](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_settings.png) **Figure 5**: Settings with entry point

### Android 13 APK Download

![Android 13 APK download](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_android_13_apk.png) **Figure 6**: Android 13 APK download

## Onboard new data type(s) and requesting permissions

Apps that already have a Health Connect integration can request new data types
in addition to the already granted ones.

Only the new data type(s) requested should be shown in the permissions screen,
to avoid confusing users.

The following are a few examples of how to promote new data types(s):

- [Reuse existing conventions](https://developer.android.com/health-and-fitness/health-connect/ui/promote#reuse)
- [Emphasize the value proposition](https://developer.android.com/health-and-fitness/health-connect/ui/promote#value)
- [Contextualize permissions](https://developer.android.com/health-and-fitness/health-connect/ui/promote#contextualize)

### Reuse existing conventions

If your app already promotes new data in other ecosystems, we recommend doing
the same for Health Connect in Android, and adapt the language where relevant.

### Emphasize the value proposition

Why would users want to use this data in your app?

- Examples: reading the data
  - Gives users more accurate insights in your app
  - Users can see all data in one place in your app
- Example: writing the data
  - Users can share this new data from your app with other compatible health and fitness apps on their phone

### Contextualize permissions

Ask for new permissions where it makes sense for users, so they know what's
being asked of them.

Examples:

- Ask for exercise routes permission after users finish a workout in your app.
- If your app implements new permissions, show these together in one promo on the home screen.
- If you have a dedicated section in your app, for example **Sleep**, promote the corresponding permission there.

![Requesting new data type](https://developer.android.com/static/health-and-fitness/health-connect/images/hc_requesting_new_data_type.png) **Figure 7**: Requesting new data type