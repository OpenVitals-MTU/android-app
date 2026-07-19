# Metric Detail Customization

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/dashboard/` (ring and tile order), `lib/features/manualentry/presentation/manual_entry_screen.dart` (`ManualEntryWidgetId` order), `lib/core/presentation/metric_detail_sections.dart` + `lib/domain/preferences/metric_detail_section_id.dart` (detail-section order). All ordering is persisted in `SharedPreferences` via `lib/data/prefs/preferences_repository.dart`.
> **Navigation:** Dashboard widget customization, manual entry widget customization, metric detail section editing.
> **Related:** [Feature map](feature-map.md), [Health Connect metrics dashboard](health-connect-metrics-dashboard.md), [Settings and preferences](settings-and-preferences.md).

OpenVitals lets users customize the order and visibility of dashboard widgets, manual entry widgets, and metric detail sections.

## How to use it

There are two independent customization modes: one for the dashboard, one for metric-detail sections.

### Rearrange the dashboard

1. On the dashboard, tap the **pencil (Edit dashboard)** button in the action row. It turns into a check, and a hint appears: **"Hold to drag & reorder · tap ✕ to remove"**.
2. **Reorder:** long-press a ring or tile to pick it up, then drop it onto another to swap places. Dragging a tile to the left or right edge pages the tile grid so you can move it across pages.
3. **Remove:** tap the **✕** on any ring or tile to move it into the tray.
4. **Add back:** in the **"Add widgets"** section below, tap **"+ &lt;widget name&gt;"** to restore a hidden widget. When nothing is hidden it reads **"All widgets are already on the summary."** Widgets your device can't support are never offered.
5. Tap the **check (Done)** to leave edit mode. Your order and hidden state are saved locally.

### Reorder metric-detail sections

1. Open any metric-detail screen (tap a dashboard ring or tile).
2. Tap the **sliders (Edit sections)** icon in the app bar. Each section becomes a draggable tile.
3. Long-press and drag a section onto another to reorder. This order is **shared across every metric screen**, so setting it once applies everywhere.
4. Tap the **check** to finish. There is no per-section remove here — only reordering.

### Reorder the Log hub

The Add-entry hub (opened from **Log**) exposes the same idea for its entry tiles — hydration, activity, carbs, mindfulness, body measurements, and vitals — so your most-used entry flows can sit first.

## Dashboard Widgets

Dashboard widgets can be reordered or removed so the most useful summaries stay prominent. Hidden widgets can be added back from the dashboard customization flow.

Widget customization changes local display preferences only. It does not delete Health Connect records.

## Metric Detail Sections

Metric detail screens can expose reorderable sections such as charts, statistics, entries, guidance, confidence, comparisons, and interpretation cards.

The goal is to keep period-based metric screens consistent while allowing each user to put the most relevant cards first.

## Manual Entry Widgets

The manual entry screen has configurable entry widgets for supported write flows such as hydration, activity, carbohydrates, mindfulness, body measurements, and vitals.

## Stored Preferences

Customization is stored locally. Unknown or obsolete saved IDs are ignored so older preferences do not break newer app versions.

## Boundaries

Customization affects navigation and display order. Health Connect remains the source of truth for health records.
