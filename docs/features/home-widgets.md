# Home Screen Widgets

OpenVitals provides Android home screen widgets for quick health summaries and fast beverage logging.

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
