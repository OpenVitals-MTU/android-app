# Home Screen Widgets

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/homewidgets`.
> **Navigation:** Android launcher widget configuration activities; dashboard widgets in `DashboardWidgetId`.
> **Related:** [Feature map](feature-map.md), [Health Connect metrics dashboard](health-connect-metrics-dashboard.md), [Daily readiness](daily-readiness.md).

OpenVitals provides Android home screen widgets for quick health summaries and fast beverage logging.

## Widget Types

- Daily Readiness.
- Body Energy.
- Today Vitals.
- Configurable metric summary widgets.
- Quick beverage logging, in a full-size and a 1x1 one-tap variant.
- Cycle.

Metric widgets are configured when the widget is added. They show selected OpenVitals dashboard metrics without turning the launcher widget into a second dashboard screen.

## Cycle Widget

The cycle widget shows the recorded cycle day, the estimated next period range (or why there is none), and whether today has a log, with a button that opens today's day log. It never shows notes, symptoms, mood, or energy. A hide button swaps the data for a neutral line; the data stays in the widget and shows again at once. Hiding limits casual glances; it does not remove previews the launcher has already cached.

The widget keeps dates, not text, and counts the cycle day when it draws, and a one-off refresh at local midnight redraws every placed widget, so the day never lags. It refreshes after every cycle save, delete, exclusion, backfill or settings change, and at the same triggers as the other Health Connect-backed widgets. The configurable metric widget no longer offers the cycle; a tile placed earlier keeps working and shows the same cycle day and estimate.

## Quick Beverage Logging

The quick beverage widget can be configured for a saved drink choice. It is meant for repeated entries such as water, coffee, tea, or another frequently used beverage. The 1x1 variant is the same thing at launcher-icon size, for a drink logged often enough to deserve its own spot.

When a beverage is logged, OpenVitals writes the supported hydration, caffeine, and nutrition values through the same explicit entry flow boundaries used inside the app.

## Data Source

Widgets read from Health Connect-backed repositories and local derived calculations. Health Connect remains the source of truth for health records, while local preferences store widget configuration and display choices.

## Refresh Behavior

Widgets refresh from the same app-local data paths used by the dashboard and detail screens. If permissions are missing, data is unavailable, or Health Connect cannot be reached, widgets show a limited state instead of inventing values.

## Privacy

Home widgets stay on device. OpenVitals does not upload widget data to an OpenVitals server.
