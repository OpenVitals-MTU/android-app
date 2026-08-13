# Design system conformance

The OpenVitals design system lives in its own repository (`../design-system`,
`codeberg.org/OpenVitals/design-system`). It is the golden: `tokens/*.css` hold
the scales, `docs/audit-1.md` records why several values are what they are, and
`docs/accessibility.md` sets the floors. This page records how this app maps
onto it and what is deliberately outstanding.

The division of authority, from the system's own SKILL.md: **values in the app
win over values there; scales and component contracts there win over bare
numbers here.** So a token file is the authority on *what steps exist*, and this
app is the authority on *what a step measures* — which is why the conformance
tests below assert the ladder rather than re-deriving it.

> Note: the design system's SKILL.md still describes the app as Flutter/Dart in
> the sibling `mobile-app` repository. That was true between 2026-07-09 and
> 2026-08-02; the app is Kotlin/Compose again and `mobile-app` is retired. The
> pointer is stale there, not here.

## Where the tokens live

| Concern | This app | Golden |
|---|---|---|
| Spacing, radii, emphasis, motion, metrics | `ui/theme/DesignTokens.kt` | `tokens/spacing.css`, `shape.css`, `motion.css` |
| Colour scheme + metric accents | `ui/theme/Color.kt`, `Theme.kt` | `tokens/colors.css` |
| Type scale | `ui/theme/Type.kt` | `tokens/typography.css` |
| Chart chrome | `ui/charts/ChartTokens.kt` | `tokens/charts.css` |
| Reduced motion | `ui/theme/ReducedMotion.kt` | `tokens/motion.css` policy block |

Two unit tests keep this honest, and both run in `verifyCi`:

- `DesignSystemConformanceTest` — the scales, step by step, including the
  duplicates and deviations that are deliberate.
- `MetricAccentContrastTest` — **measures** WCAG contrast rather than pinning
  hex values, because a test that pinned hex would pass just as happily on a
  palette someone had brightened.

## Deliberate deviations from Material 3

Both are recorded in the golden's audit as decisions, not drift, and both are
pinned by test so a later "align with Material" sweep has to argue with them:

- **Heavier headline and title weights** (headlineLarge Bold vs M3's Normal,
  titleLarge SemiBold vs Normal). The brand is numbers-first with big bold
  numerals.
- **`Radii.sm` and `Radii.md` both measure 12dp.** The app does not distinguish
  an input corner from a card corner. Two names so they can diverge later
  without touching every call site — not a duplication to tidy away.

## The metric accents are an accessibility floor, not a palette

The seventeen accents are drawn as chart strokes, icons and small indicators, so
WCAG 1.4.11's **3:1 for graphical objects** binds — against *both* static
surfaces, light `#FCFCFF` and dark `#1A1C1E`. Dynamic colour re-tints the chrome
but leaves the accents fixed, so the static surfaces stay the constraint; AMOLED
only ever increases contrast.

This app shipped the stock Material-500 swatches until 2026-08-04. Measured
against its own surfaces, **eight of sixteen fell below 3:1**, floors/amber
worst at **1.59:1**. The audited palette clears 3:1 on both, worst case 3.09 —
which is very little headroom.

**Never brighten an accent without re-measuring against both surfaces.** Making
one "pop" is the exact regression the palette was built to fix.
`MetricAccentContrastTest` fails the build if it happens.

The other half of the rule: accents appear only on **data**, never on
interactive chrome. A wallpaper-derived `primary` can converge on an accent
under Material You, and a control tinted like a metric would then impersonate
one.

## Motion

Four durations and nothing else — `Motion.pressMillis` 120, `standardMillis`
300, `revealMillis` 550, `ambientMillis` 1200. Naming them is what stops the
fifth from being whatever someone types that day, which is what had happened:
five unnamed values were scattered across call sites.

Two documented exceptions, both at their call sites:

- `Motion.editWiggleMillis` (140) — half a cycle of the edit-mode tilt that says
  a tile can be dragged. The one looping animation the app keeps: an affordance
  rather than decoration, running only while edit mode is on.
- The mindfulness timer's pulse and wiggle (1800 / 2800) — a breathing rhythm
  rather than chrome timing, so it is not folded into the scale.

**Reduced motion is a floor, not a preference.** Android has no
`prefers-reduced-motion`; the equivalent is the animator duration scale, which
"Remove animations" drives to zero and `ValueAnimator.areAnimatorsEnabled()`
reports. `OpenVitalsTheme` resolves it once into `LocalReducedMotion`; use
`animationDuration()` for a tween and `loopingMotionAllowed()` before starting
anything that repeats. Repeating animations are the ones this matters most for —
they are the kind that never settles.

## The reader's place on the page is theirs to keep

Two rules, both learned from the same complaint: an entries list expanded to
fifty rows, one row swiped away, and the page ending up somewhere the reader
never scrolled to.

**`PaginatedEntryList` remembers how far it has been expanded across the list
changing.** The expansion answers "how much of this do I want to see" and is not
a property of the current contents; it used to be keyed on the entries
themselves, so deleting a row reset it to the first page. The count is only ever
clamped to what exists, never reset — including across a change of period, on the
grounds that having asked for a longer list once, being given a longer list again
is the lesser surprise.

**Every metric-detail section carries a stable `item(key = sectionId)`.** That is
what lets the list anchor its scroll position to the section the reader is inside
rather than to its index, so an item appearing above the viewport — the syncing
banner, which every delete raises for the length of its reload — costs nothing.
Drop those keys and the page lurches down and back on every delete, on every
metric screen, with no failing assertion anywhere near the change that caused it.
`MetricDetailScaffoldScrollAnchorTest` exists to make that failure loud.

## Outstanding

Backlog in the golden's own priority order. None of these is a silent skip; each
is a measured count as of 2026-08-04.

1. **Sentence-case first-party strings** (audit F3). Several first-party strings
   are title case. Garmin's product names — Body Battery, Sleep Coach, Training
   Readiness, Intensity Minutes, Stress Level — are third-party terms and keep
   their casing. This is l10n churn across five catalogs.
2. **Spacing / radius / alpha literals** (F5). 2,075 bare `dp` literals, 95
   hand-written alphas, 21 hand-built `RoundedCornerShape`s. Explicitly a
   per-screen migration with golden cover, not a sweep. **Rule for all new
   code: no bare numbers for spacing, radius, or alpha.**
3. **Raw colour audit** (F7). 4 `Color.White`/`Color.Black` outside the theme
   (chart scrims, mostly legitimate) and 27 hex `Color(0x…)` literals in feature
   code. Every colour should resolve from the `ColorScheme`, `Color.kt`, or
   `ChartTokens`; a raw one needs a comment saying why the scheme is wrong there.
4. **Chart semantics.** Charts are visual-only; each needs a one-line semantic
   summary for screen readers.
