import 'package:flutter/material.dart';

import '../../../l10n/app_localizations.dart';
import '../../../ui/components/ov_card.dart';
import 'activity_entry_form_fields.dart';
import 'activity_entry_state.dart';
import 'activity_entry_ui_text.dart';

/// Port of the Kotlin `ActivityEntrySourceCard.kt`.

/// Kotlin `ActivityEntrySourceAction`. Each maps to a controller call, but only
/// after the Health Connect write permission is granted. Route-file import is
/// deliberately absent: a route file enters the form only via the Settings Data
/// Import cards (matching Kotlin), never from the source card.
enum ActivityEntrySourceAction {
  manual,
  existingPlan,
  recordGps,
}

class ActivityEntrySourceCard extends StatelessWidget {
  const ActivityEntrySourceCard({
    super.key,
    required this.state,
    required this.onSourceAction,
    required this.onRequestWritePermission,
  });

  final ActivityEntryUiState state;
  final ValueChanged<ActivityEntrySourceAction> onSourceAction;
  final VoidCallback onRequestWritePermission;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    // Kotlin gates every source button on the same three flags. Note it does
    // *not* gate on canWrite: tapping while unpermitted triggers the request.
    final enabled = !state.isCheckingPermission &&
        !state.isImportingRoute &&
        !state.isSavingEntry;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          spacing: 12,
          children: [
            ActivityEntryHeader(
              state: state,
              onRequestWritePermission: onRequestWritePermission,
            ),
            Text(
              l10n.activityEntrySourceBody,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            FilledButton.icon(
              onPressed:
                  enabled ? () => onSourceAction(ActivityEntrySourceAction.manual) : null,
              icon: const Icon(Icons.add, size: 18),
              label: Text(l10n.activityEntryCreateManual),
            ),
            OutlinedButton.icon(
              onPressed: enabled
                  ? () => onSourceAction(ActivityEntrySourceAction.existingPlan)
                  : null,
              icon: const Icon(Icons.folder_open_outlined, size: 18),
              label: Text(l10n.activityEntryCreateFromExistingPlan),
            ),
            OutlinedButton.icon(
              onPressed: enabled
                  ? () => onSourceAction(ActivityEntrySourceAction.recordGps)
                  : null,
              icon: const Icon(Icons.my_location_outlined, size: 18),
              label: Text(l10n.activityEntryRecordGps),
            ),
            ActivityEntryErrorText(state: state),
          ],
        ),
      ),
    );
  }
}
