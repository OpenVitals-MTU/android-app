# OpenVitals UI Notes

> **Status:** Local design guidance for OpenVitals. Use alongside the current app UI and the feature map, not as a standalone redesign spec.

OpenVitals uses Material 3 with a restrained health-dashboard style:

- Prefer neutral surfaces with one clear accent per metric.
- Keep the dashboard hierarchy obvious: primary today status first, secondary stats below.
- Use metric colors for icons, chart accents, and small indicators rather than full saturated card backgrounds.
- Keep cards moderately rounded and consistent instead of making every surface a large pill.
- Use Material 3 tokens such as `surface`, `surfaceContainer`, `surfaceContainerHigh`, `primary`, `secondary`, `tertiary`, and `outlineVariant`.

Recommended dashboard component vocabulary:

- `DashboardSummaryCard`
- `MetricStatCard`
- `ActivityPreviewCard`
- `SectionHeader`
- `DashboardActionButton`
- `ProgressRingCard`

The intended direction is a quiet, scan-friendly dashboard: soft elevated cards, clear section spacing, restrained color, and card weight based on importance.
