import 'package:flutter/material.dart';

/// The padding for a screen-level scrolling body.
///
/// The app draws edge to edge — Android enforces it from API 35 — so the system
/// navigation bar is painted OVER the body rather than beside it. Gesture
/// navigation reserves a thin strip and costs almost nothing; a three-button bar
/// is tall enough to swallow the last row of a list, which is how this was found:
/// the foot of the hydration screen sat underneath the buttons and could not be
/// scrolled into view.
///
/// The bar's height is reserved at the foot of the SCROLLABLE rather than around
/// it, so content still scrolls out from under the bar instead of being
/// letterboxed above it.
///
/// Use [MediaQuery.paddingOf], not `viewPaddingOf`: when the keyboard is up it
/// already covers the navigation bar, and padding (unlike viewPadding) drops to
/// zero there, so an entry screen does not reserve room for a bar nobody can see.
EdgeInsets screenScrollPadding(BuildContext context, {double vertical = 8}) =>
    EdgeInsets.only(
      top: vertical,
      bottom: vertical + MediaQuery.paddingOf(context).bottom,
    );
