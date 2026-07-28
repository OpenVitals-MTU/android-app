/// The measurements the app is laid out on: the spacing grid, the corner-radius
/// scale, the emphasis alphas, and the fixed component metrics.
///
/// Colour and type already had a home ([AppColors], [AppTypography]) and charts
/// got one in `chart_tokens.dart`. Everything in THIS file was, until now, typed
/// straight into the widget that needed it — 563 `SizedBox` literals, 481
/// `EdgeInsets`, 71 hand-written alphas, and the card radius written out twice in
/// two files that could have disagreed at any moment without anyone noticing.
///
/// **Plain constants, not a [ThemeExtension].** `ChartTokens` is an extension
/// because its colours must derive from the live `ColorScheme` — dynamic colour
/// and AMOLED make a hard-coded grey wrong. A gap of 12 logical pixels is 12
/// logical pixels in every theme, so an extension would buy nothing and cost a
/// `Theme.of(context)` lookup at every call site. The layout constants at the
/// bottom of `chart_tokens.dart` are declared the same way for the same reason.
///
/// These values are the design system's, not this file's: they mirror
/// `tokens/spacing.css` and `tokens/shape.css` in the **OpenVitals design-system
/// repository** (a sibling checkout — it used to live in `design/` here). Where
/// that system and this code disagree about a *scale*, the design system wins;
/// where they disagree about a colour or a type slot, the code wins, because the
/// palette is contrast-audited and ships. Its `readme.md` states the same rule
/// from the other side.
library;

import 'package:flutter/widgets.dart';

/// The 4dp Material grid.
///
/// Named rather than numbered — `Spacing.md` survives a decision to retune the
/// scale, `12` does not. The app had already converged on this grid by itself:
/// of the spacing literals counted when this file was written, 471 were 4, 8,
/// 12, 16 or 24 and roughly 90 were strays at 2, 6, 10, 14, 20 and 28. Those
/// strays are the migration's job, one screen at a time; nothing here forces
/// them to change today.
abstract final class Spacing {
  const Spacing._();

  /// 4 — hairline separation, icon-to-label inside a chip.
  static const double xs = 4;

  /// 8 — the gap between stacked rows in a card; the standard section gap.
  static const double sm = 8;

  /// 12 — inside a metric tile; the most-used gap in the app.
  static const double md = 12;

  /// 16 — card padding and the screen gutter. The app's default breathing room.
  static const double lg = 16;

  /// 20 — rare; prefer [lg] or [xl] unless a design calls for it explicitly.
  static const double xl = 20;

  /// 24 — between major blocks on a scrolling screen.
  static const double xxl = 24;

  /// 32 — above a screen's first section, below its last.
  static const double xxxl = 32;

  /// 40.
  static const double huge = 40;

  /// 48 — the tallest step; a full action row.
  static const double giant = 48;
}

/// Corner radii.
///
/// [card] is 12, and that is the number the app has always drawn. The design
/// system claimed 16 for years — inherited from the Compose `AppShapes.medium`
/// of the pre-Flutter app, and never true of a shipped pixel. It says 12 now.
///
/// [small] and [card] coincide deliberately: the app does not distinguish an
/// input's corner from a card's. Both names exist so they *can* diverge later
/// without every call site having to be found again.
abstract final class Radii {
  const Radii._();

  /// 8 — progress fills, small chips.
  static const double tiny = 8;

  /// 12 — inputs, list rows.
  static const double small = 12;

  /// 12 — the default card corner ([OpenVitalsCard], [OpenVitalsSurface]).
  static const double card = 12;

  /// 16 — containers that want to read as larger than a standard card.
  static const double medium = 16;

  /// 24 — segmented pills, range selectors.
  static const double large = 24;

  /// 32 — hero and onboarding surfaces.
  static const double extraLarge = 32;

  /// The card corner as a ready-made [BorderRadius], so the two primitives that
  /// draw it cannot drift apart again.
  static const BorderRadius cardBorder =
      BorderRadius.all(Radius.circular(card));
}

/// The alpha values the app tints with.
///
/// Not decoration: each one is a rung on a ladder, and the reason to name them
/// is that a tint chosen freehand lands somewhere between two rungs and reads as
/// a mistake nobody can quite point at. The seventy-odd hand-written alphas
/// counted when this file was written used twelve distinct values between 0.12
/// and 0.85, which is roughly three times more distinctions than the eye makes.
abstract final class Emphasis {
  const Emphasis._();

  /// 0.12 — an accent wash you read *through*: chart grid lines, area fills, the
  /// tint behind an icon chip.
  static const double wash = 0.12;

  /// 0.22 — a line that must be visible but must not compete with the trace on
  /// top of it (a chart baseline).
  static const double subtle = 0.22;

  /// 0.38 — Material's disabled opacity. Do not invent another one.
  static const double disabled = 0.38;

  /// 0.55 — a filled ring or underline over its track.
  static const double fill = 0.55;

  /// 0.8 — near-solid; an axis line against the surface.
  static const double strong = 0.8;
}

/// Fixed sizes that are neither spacing nor radius: the chrome the app is built
/// around.
abstract final class Metrics {
  const Metrics._();

  /// 16 — the inset from the screen edge to content, on every scrolling screen.
  static const double screenGutter = Spacing.lg;

  /// 16 — padding inside [OpenVitalsCard].
  static const double cardPadding = Spacing.lg;

  /// 12 — padding inside a metric tile, which is tighter than a card.
  static const double metricTilePadding = Spacing.md;

  /// 8 — between metric tiles in a grid.
  static const double metricTileGap = Spacing.sm;

  /// 48 — a dashboard quick-action row.
  static const double actionRowHeight = Spacing.giant;

  /// 52 — the round icon-surface button.
  static const double iconSurfaceSize = 52;

  /// 48 — **the minimum touch target.** Anything tappable must reach this in
  /// both directions, whatever its painted size; an icon drawn at 24 still needs
  /// 48 of hit area around it. This is Material's floor and Flutter's own
  /// (`kMinInteractiveDimension`, which is material-scoped and so cited rather
  /// than referenced — this file stays widgets-only). An earlier version of
  /// this token said 44: the iOS HIG number, undersized for an Android app.
  /// Matches `--ov-min-touch-target` in the design-system repo.
  static const double minTouchTarget = 48;

  /// 64 — the top app bar.
  static const double topBarHeight = 64;

  /// 80 — the bottom navigation bar.
  static const double navBarHeight = 80;
}
