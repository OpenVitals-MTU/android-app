import 'package:flutter/material.dart';

import '../../l10n/app_localizations.dart';
import 'ov_card.dart';

/// An inline permission-request card: a lock icon + title, an explanatory body,
/// and a grant action (plus an optional dismiss). Port of Kotlin
/// `PermissionCallout`.
class PermissionCallout extends StatelessWidget {
  const PermissionCallout({
    super.key,
    required this.title,
    required this.body,
    required this.onGrant,
    this.actionLabel,
    this.onDismiss,
  });

  final String title;
  final String body;
  final VoidCallback onGrant;
  final String? actionLabel;
  final VoidCallback? onDismiss;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    return OpenVitalsCard(
      color: scheme.errorContainer.withValues(alpha: 0.3),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.lock_outline, color: scheme.error, size: 18),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    title,
                    style: theme.textTheme.titleSmall
                        ?.copyWith(color: scheme.onErrorContainer),
                  ),
                ),
              ],
            ),
            Padding(
              padding: const EdgeInsets.only(top: 4, bottom: 12),
              child: Text(
                body,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: scheme.onErrorContainer.withValues(alpha: 0.8),
                ),
              ),
            ),
            Row(
              children: [
                FilledButton.tonal(
                  onPressed: onGrant,
                  child: Text(actionLabel ?? l10n.actionGrantPermission),
                ),
                if (onDismiss != null) ...[
                  const SizedBox(width: 8),
                  TextButton(
                    onPressed: onDismiss,
                    child: Text(l10n.actionNotNow),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }
}
