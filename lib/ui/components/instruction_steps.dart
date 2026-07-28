/// A numbered list of things the user has to do somewhere else.
///
/// It exists because of exercise routes. Health Connect will not let any intent
/// deep-link to the "Additional access" page — `MANAGE_HEALTH_PERMISSIONS` lands
/// one screen above it and that is as close as Android allows — so the last
/// stretch has to be walked by hand, and a walk-through is only useful if it
/// looks like one.
///
/// The house alternative was an arrow-separated path crammed into a single
/// string ("Settings → Watch Sensors → Broadcast", app_en.arb). That reads
/// tolerably in English and badly everywhere else: translators get one long
/// sentence with no structure to preserve, and screen readers get it as a run-on.
/// Passing the steps as a list keeps each one a separate translatable string and
/// numbers them for free.
library;

import 'package:flutter/material.dart';

import '../theme/design_tokens.dart';

class InstructionSteps extends StatelessWidget {
  const InstructionSteps({super.key, required this.steps});

  /// One string per step, in order. Numbering is generated — do not write "1."
  /// into the strings themselves or every locale has to remember to.
  final List<String> steps;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (var i = 0; i < steps.length; i++)
          Padding(
            padding: EdgeInsets.only(
              bottom: i == steps.length - 1 ? 0 : Spacing.md,
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // A numbered disc rather than a bullet: the order is the
                // instruction here, not just a list style.
                Container(
                  width: Spacing.xxl,
                  height: Spacing.xxl,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: scheme.primaryContainer,
                  ),
                  child: Text(
                    '${i + 1}',
                    style: theme.textTheme.labelMedium
                        ?.copyWith(color: scheme.onPrimaryContainer),
                  ),
                ),
                const SizedBox(width: Spacing.md),
                Expanded(
                  child: Padding(
                    // Optical alignment: the text's first line sits against the
                    // disc's centre rather than its top edge.
                    padding: const EdgeInsets.only(top: Spacing.xs),
                    child: Text(
                      steps[i],
                      style: theme.textTheme.bodyMedium
                          ?.copyWith(color: scheme.onSurface),
                    ),
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
