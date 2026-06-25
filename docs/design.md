Use: Material 3
Compose-native, already fits Android.
Supports dark theme, dynamic color, typography scale, surfaces, tonal elevation.
Works well for health dashboards if you keep it restrained.
But style it more like: Samsung Health / Google Fit / Health Connect
Fewer saturated blocks.
More neutral surfaces.
One strong accent per metric, not every tile competing.
More whitespace and clearer grouping.
Bigger distinction between “primary today status” and “secondary stats”.
For this dashboard, I’d recommend:
Use one primary summary area
Keep Steps/Cardio as the hero, but reduce visual weight. Maybe one wide “Today” card instead of two huge rings side by side.

Turn small metric tiles into quieter list/card rows
Distance, floors, calories, elevation, pushes could use neutral cards with small accent icons instead of full-color pill backgrounds.

Reduce color saturation
Current tiles are visually loud: blue, red, orange, green, teal, yellow all at once. Use mostly dark neutral surfaces, then metric colors only for icons, charts, or small indicators.

Use Material 3 tokens
surface
surfaceContainer
surfaceContainerHigh
primary
secondary
tertiary
outlineVariant

Use consistent card shapes
The current screen has very large rounded corners everywhere. Modern health dashboards usually look cleaner with moderate radius, around 16dp to 24dp, not giant pill cards for everything.

Create a dashboard component set
Not a huge design system, just:
DashboardSummaryCard
MetricStatCard
ActivityPreviewCard
SectionHeader
DashboardActionButton
ProgressRingCard

My recommendation: Material 3 Expressive-inspired, but restrained. Keep the personality in charts and icons, not in every background color.
A cleaner visual target would be:
Dark neutral background, soft elevated cards, one accent color per metric, fewer huge pills, clearer section spacing, and dashboard cards ranked by importance.