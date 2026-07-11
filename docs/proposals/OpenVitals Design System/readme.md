# OpenVitals Design System

A design system for **OpenVitals** — a privacy-first Health Connect dashboard,
activity tracker, and manual-entry app for Android. This project lets design
agents build well-branded OpenVitals interfaces and assets — production or
throwaway mocks — grounded in the app's real Material 3 theme.

> **Product in one line:** review Health Connect data, record or import workouts,
> and add manual entries — no account, no cloud sync, no ads, no analytics.
> Health Connect stays the source of truth; the dashboard is read-only by default.

## Sources

Everything here is grounded in the OpenVitals source, not guessed. If you have
access, explore these to build even more faithful designs:

- **GitHub mirror:** https://github.com/mmarca-tech/OpenVitals (canonical theme in
  `app/src/main/kotlin/tech/mmarca/openvitals/ui/theme/` — `Color.kt`, `Theme.kt`,
  `Type.kt`; component primitives in `ui/components/DetailCards.kt`,
  `features/dashboard/components/`)
- **Codeberg (upstream):** https://codeberg.org/OpenVitals/android-app
- **Reference screenshots:** `assets/screens/` (dashboard, onboarding, settings,
  daily readiness, body energy, activity detail, activity recording, beverage entry)
- **App icon:** `assets/openvitals-icon.png`

### A note on color: dynamic vs. canonical
OpenVitals ships with **Material You dynamic color ON by default**, so on a real
device the whole chrome is re-tinted from the user's wallpaper. Every reference
screenshot shows a **warm terracotta** dynamic instance. The app's *own* static
color scheme (used when dynamic color is unavailable) is **blue primary + teal
tertiary on cool neutrals**.

This system carries **both**:
- **Canonical** (`:root`) — the static blue/teal scheme, verbatim from `Theme.kt`.
  Deterministic, brand-owned. Use for production defaults.
- **Warm** (`[data-theme="warm"]`) — the sampled dynamic instance from the
  screenshots. Use when a mock should match the photographed product. Opt in per
  container with `data-theme="warm"`.
- **Dark** (`[data-theme="dark"]`) and **AMOLED** (`[data-theme="amoled"]`) — the
  app's static dark schemes, verbatim from `Theme.kt`.

Metric accent colors, type, shape, and spacing are fixed from source and shared
by both.

---

## Content fundamentals

How OpenVitals writes copy:

- **Voice:** calm, factual, slightly clinical but plain-spoken. It states what the
  data says and what to do, without hype. e.g. *"Do moderate training today, but
  avoid maximal effort."*
- **Person:** addresses the user as **you / your** ("Your health data, on your
  device", "Your signals suggest…"). First person is never used.
- **Casing:** **Sentence case** everywhere — titles, buttons, list items
  ("Daily Readiness", "Sensors & devices", "Grant required Health Connect
  permissions"). ALL-CAPS is reserved for small section labels
  ("HEALTH CONNECT PERMISSIONS") via `labelSmall` tracking.
- **Numbers first:** metric values are the loudest element; units and context are
  secondary ("**19,576** steps of 8,000", "**65**/100", "**2h 46m**").
- **Honesty about data:** copy openly hedges confidence — "Medium confidence ·
  sleep data missing", "low confidence estimate", "Some timeline buckets have
  sparse Health Connect data." This candor is a brand trait; keep it.
- **Privacy framing:** leads with what does *not* happen ("No account required.
  Data stays on your device. No cloud upload, no analytics, no ads.").
- **Emoji:** none. **Icons** carry all visual shorthand.
- **Health disclaimers:** wellness/informational framing, never medical claims.

---

## Visual foundations

- **Type:** Roboto (the Material 3 default — the app declares no custom font), with
  Roboto Mono available for tabular metric contexts. Material 3 type scale verbatim
  from `Type.kt`. Big bold numerals (`headlineLarge` 32/700), semibold titles,
  regular body. `headlineMedium` carries tabular figures.
- **Color:** restrained. Neutral surfaces with **one accent per metric** (steps
  green, distance blue, sleep purple, heart pink, calories red, hydration light
  blue, workout cyan…). Accents appear on **icons, chart strokes, small progress
  indicators** — never as full saturated card backgrounds. Card weight comes from
  surface tone, not color.
- **Surfaces & depth:** cards are **flat (0 elevation)**. Depth is expressed by a
  tonal ladder of `surfaceContainer` steps (lowest → highest), not shadows.
  Shadows appear only on truly lifted surfaces (dialogs, FAB, the phone frame).
- **Corners:** everything is rounded. Cards use **16px** (`medium`); segmented
  pills and selectors **24px** (`large`); hero/onboarding **32px**; small chips/
  inputs **12px**; progress fills **8px**. Icon buttons are full circles.
- **Backgrounds:** solid single-tone surface. **No** gradients, images, textures,
  or patterns behind content. The only "image" is the app icon.
- **Progress motifs:** open **arc rings** (280° sweep, gap at the bottom, round
  caps) for hero stats; **3px accent underlines** pinned to the bottom of stat
  tiles; thin linear bands elsewhere. Ring/underline fills are the accent at
  ~55–72% alpha over an `outlineVariant` track.
- **Icon chips:** small accent-tinted circles (accent at 14–16% alpha, colored
  glyph) mark metrics and list rows.
- **Buttons:** pill-shaped (fully rounded), ~40–48px tall. Filled = primary
  action, tonal = secondary (the dashboard "Log"/"Start" pair), outlined/text =
  low emphasis.
- **Hover / press:** subtle `brightness()` shift on press (~0.93–0.94); cards lift
  by tone on hover. No scale/bounce. Disabled = 38% opacity (Material standard).
- **Motion:** minimal and functional — short 120ms ease transitions on
  press/hover, swipe to change date/period. No decorative or looping animation.
- **Layout:** single scrolling column, **16px screen gutters**, 4dp spacing grid,
  8–12px gaps between cards. Two-up grids for stat tiles and summary rings. Top app
  bar is transparent over the background with no divider.
- **Transparency / blur:** essentially none — surfaces are opaque tones. Accent
  tints are the only alpha usage.

---

## Iconography

- **Icon set:** **Material Symbols Outlined** — the app uses Jetpack Compose
  `androidx.compose.material.icons` (`Icons.Outlined.*`, e.g. `Add`, `DirectionsRun`,
  `Settings`, `ChevronLeft`, `CalendarMonth`, `Edit`, `Bed`, `Bluetooth`). We load
  the matching **Material Symbols Outlined** webfont from Google Fonts (see
  `tokens/fonts.css`) and wrap it in the `Icon` component. This is the genuine
  Material set, not a substitute.
- **Weight/fill:** outlined (FILL 0), weight 500, optical size matched to px size.
- **Usage:** icons are tinted with the metric accent inside chips, or
  `on-surface` / `on-surface-variant` for neutral chrome (top bar, chevrons, list
  rows). One glyph per metric — consistent across dashboard, detail, and settings.
- **Emoji / unicode icons:** never used.
- **Logo:** the OpenVitals app icon (`assets/openvitals-icon.png`) is the only
  brand mark — a teal disc with a peach seated figure + waveform. It is a real
  supplied asset; do not redraw or reconstruct it. Where a wordmark is needed, set
  **"OpenVitals" in Roboto Bold** (see the Brand foundation card).

---

## Index — what's in this project

**Foundations**
- `styles.css` — global entry point (import this one file)
- `tokens/colors.css` — canonical + warm palettes, metric accents
- `tokens/typography.css` — Material 3 type scale + utility classes
- `tokens/shape.css` — corner radii + elevation
- `tokens/spacing.css` — 4dp grid + component metrics
- `tokens/fonts.css` — Roboto, Roboto Mono, Material Symbols Outlined
- `guidelines/*.card.html` — foundation specimen cards (Colors, Type, Spacing, Brand)

**Components** (`components/…`, React primitives — namespace `OpenVitalsDesignSystem_626946`)
- **buttons/** — `Button` (filled/tonal/outlined/text), `IconButton` (plain/surface)
- **cards/** — `Card`, `MetricStatCard`, `SummaryRingCard`, `MetricCard`
- **navigation/** — `TopBar`, `DateNavigator`, `TimeRangeSelector`, `SectionHeader`, `BottomNavBar`
- **data-display/** — `Icon`, `AccentIconChip`, `DetailRow`, `SettingsListItem`, `ReadinessBanner`
- **charts/** — `MetricLineChart`, `MetricBarChart`, `Sparkline`, `PeriodHeatmap`
- **forms/** — `Switch`, `Checkbox`, `RadioGroup`, `Slider`, `TextField`, `Select`
- **insights/** — `DataConfidenceCard`, `AchievementBadge`, `CrossMetricInsightCard`, `SensorStatusCard`

Full component list: `AccentIconChip`, `AchievementBadge`, `BottomNavBar`, `Button`,
`Card`, `Checkbox`, `CrossMetricInsightCard`, `DataConfidenceCard`, `DateNavigator`,
`DetailRow`, `Icon`, `IconButton`, `MetricBarChart`, `MetricCard`, `MetricLineChart`,
`MetricStatCard`, `PeriodHeatmap`, `RadioGroup`, `ReadinessBanner`, `SectionHeader`,
`Select`, `SensorStatusCard`, `SettingsListItem`, `Slider`, `Sparkline`,
`SummaryRingCard`, `Switch`, `TextField`, `TimeRangeSelector`, `TopBar`.

**UI kit**
- `ui_kits/openvitals-app/` — interactive click-through of the app (dashboard,
  daily readiness, settings, beverage entry, activity detail, recording)

**Assets**
- `assets/openvitals-icon.png` — app icon
- `assets/screens/` — 8 reference screenshots

**Intentional additions.** The source's icons are Compose `ImageVector`s, not a
web component. We add a thin **`Icon`** wrapper over the Material Symbols Outlined
webfont so components and mocks have a consistent glyph API. Everything else maps
1:1 to a primitive defined in the OpenVitals source.

## Substitutions & caveats
- **Fonts:** Roboto is the *actual* Material 3 default OpenVitals renders in (no
  custom font in source) — loaded from Google Fonts, not an approximation. If you
  have a specific bundled font, share it and it will be swapped in.
- **Warm palette:** the warm chrome tokens are *sampled from the screenshots*
  (Material You is wallpaper-derived and per-device); structural tokens, type,
  shape, and metric accents are exact from source.
