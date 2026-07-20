import 'package:flutter/material.dart';

import 'external_link.dart';

/// One research citation, rendered as a full-width outlined button that opens
/// [url] in the browser via [openExternalUrl].
///
/// Shared by the metric detail screens that show the science behind a
/// calculation to the user (sleep, cardio load, body energy, daily readiness) —
/// see AGENTS.md invariant 8: every internal calculation is backed by a source,
/// and where a detail screen explains it, that source is a tappable link.
class ReferenceLinkButton extends StatelessWidget {
  const ReferenceLinkButton({
    super.key,
    required this.title,
    required this.url,
  });

  final String title;
  final String url;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: () => openExternalUrl(context, url),
            icon: const Icon(Icons.open_in_new, size: 18),
            label: Text(title, maxLines: 2, overflow: TextOverflow.ellipsis),
          ),
        ),
      );
}
