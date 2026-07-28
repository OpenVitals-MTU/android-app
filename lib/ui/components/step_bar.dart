/// The back/next bar a stepped flow pins to the bottom of every step.
///
/// Lifted out of the CSV importer, which had it first (`CsvImportStepBar`) and
/// now aliases this. Onboarding is the second flow to want it; a third copy was
/// the point at which it stopped being a CSV detail.
library;

import 'package:flutter/material.dart';

import '../theme/design_tokens.dart';

class StepBar extends StatelessWidget {
  const StepBar({
    super.key,
    required this.onBack,
    required this.onNext,
    required this.nextLabel,
    required this.backLabel,
  });

  /// Null hides the back button entirely — for the first step of a flow, where
  /// there is nowhere to go back TO and an inert button would say otherwise.
  final VoidCallback? onBack;

  /// Null disables Next, which is how a gating step refuses to let go.
  final VoidCallback? onNext;

  final String nextLabel;
  final String backLabel;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.all(Spacing.lg),
        child: Row(
          children: [
            if (onBack != null) ...[
              Expanded(
                child: OutlinedButton(
                  onPressed: onBack,
                  child: Text(backLabel),
                ),
              ),
              const SizedBox(width: Spacing.md),
            ],
            Expanded(
              child: FilledButton(
                onPressed: onNext,
                child: Text(nextLabel),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// How far through a flow the user is.
///
/// The app's only other dots were inline in the dashboard carousel; this is that
/// pattern with a name. Decorative — the step is announced in the heading, so
/// this is excluded from semantics rather than read out as a row of blobs.
class StepDots extends StatelessWidget {
  const StepDots({super.key, required this.count, required this.index});

  final int count;
  final int index;

  @override
  Widget build(BuildContext context) {
    if (count < 2) return const SizedBox.shrink();
    final scheme = Theme.of(context).colorScheme;
    return ExcludeSemantics(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          for (var i = 0; i < count; i++)
            Container(
              width: Spacing.sm,
              height: Spacing.sm,
              margin: const EdgeInsets.symmetric(horizontal: Spacing.xs),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: i == index ? scheme.primary : scheme.outlineVariant,
              ),
            ),
        ],
      ),
    );
  }
}

/// The icon + title + body header a step leads with.
///
/// Extracted from the device-sync wizard, which had it as a private `_hero`.
class StepHero extends StatelessWidget {
  const StepHero({
    super.key,
    required this.icon,
    required this.title,
    required this.body,
  });

  final IconData icon;
  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Icon(icon, size: 40, color: scheme.primary),
        const SizedBox(height: Spacing.lg),
        Text(
          title,
          style: theme.textTheme.titleLarge,
          textAlign: TextAlign.center,
        ),
        Padding(
          padding: const EdgeInsets.only(top: Spacing.sm),
          child: Text(
            body,
            style: theme.textTheme.bodyMedium
                ?.copyWith(color: scheme.onSurfaceVariant),
            textAlign: TextAlign.center,
          ),
        ),
      ],
    );
  }
}
