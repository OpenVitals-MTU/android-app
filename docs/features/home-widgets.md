# Home Screen Widgets

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/homewidgets/` (`HomeWidgetId`, `HomeWidgetService`, snapshots, alarm refresh, configure app).
> **Navigation:** Launcher widget configuration is a second entry path in `lib/main.dart` (`ACTION_APPWIDGET_CONFIGURE` → `HomeWidgetConfigureApp`, no router). Widget taps deep-link into app routes. Widget types are `HomeWidgetId`, not the dashboard's `DashboardMetricId`.
> **Related:** [Feature map](feature-map.md), [Health Connect metrics dashboard](health-connect-metrics-dashboard.md), [Daily readiness](daily-readiness.md).

OpenVitals provides Android home screen widgets for quick health summaries and fast beverage logging.

## How to use it

### Add a widget

1. **Long-press** an empty area of your Android home screen and tap **Widgets**.
2. Find **OpenVitals** and drag the widget you want onto the home screen.
3. **Configurable widgets** open a picker as you place them:
   - **Metric summary** → **Choose metric**: tap the metric this tile should show.
   - **Quick beverage** (2×1 or 1×1) → **Choose beverage**: tap a drink (shown as "Name - 330 ml"). Frequently used and saved drinks appear first.
   Backing out of the picker cancels the placement.
4. **Daily Readiness**, **Body Energy**, and **Today Vitals** aren't configurable — they appear with data immediately.

### Use widgets

- **Tapping a widget opens the app** at the matching screen (for example the Body Energy widget opens the Body Energy detail for the day).
- The **quick beverage** widget logs its drink on tap (**"Tap to log"**, confirming **"Saved now"** or **"Saved as nutrition"**); the 2×1 version adds explicit **Add** / **Edit** buttons.
- If a widget shows **"Grant permission in OpenVitals"** or **"Select a metric/beverage"**, open the app to grant the missing Health Connect permission or re-run the widget's configure step.

Widgets read the same on-device data as the app, so they refresh alongside it — there's no separate account or sync.

## Widget Types

- Daily Readiness.
- Body Energy.
- Today Vitals.
- Configurable metric summary widgets.
- Quick beverage logging widgets.

Metric widgets are configured when the widget is added. They show selected OpenVitals dashboard metrics without turning the launcher widget into a second dashboard screen.

## Quick Beverage Logging

The quick beverage widget can be configured for a saved drink choice. It is meant for repeated entries such as water, coffee, tea, or another frequently used beverage.

When a beverage is logged, OpenVitals writes the supported hydration, caffeine, and nutrition values through the same explicit entry flow boundaries used inside the app.

## Data Source

Widgets read from Health Connect-backed repositories and local derived calculations. Health Connect remains the source of truth for health records, while local preferences store widget configuration and display choices.

## Refresh Behavior

Widgets refresh from the same app-local data paths used by the dashboard and detail screens. If permissions are missing, data is unavailable, or Health Connect cannot be reached, widgets show a limited state instead of inventing values.

## Privacy

Home widgets stay on device. OpenVitals does not upload widget data to an OpenVitals server.
