import 'package:flutter/material.dart';

/// The standard gutter around one section of a metric detail screen.
///
/// Every scrolling detail screen stacks sections down a page and insets each one
/// by the same amount. That inset was written out as a private `_padded` helper
/// **sixteen times** — and fifteen of them agreed on `vertical: 4` while the
/// vitals overview used `vertical: 8`, which nobody chose and no one could have
/// noticed. There is now one of it.
Widget sectionPadded(Widget child) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: child,
    );
