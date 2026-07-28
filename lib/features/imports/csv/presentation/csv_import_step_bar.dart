/// The back/next bar every step of the CSV importer shares.
library;

import 'package:flutter/material.dart';

import '../../../../l10n/app_localizations.dart';

class CsvImportStepBar extends StatelessWidget {
  const CsvImportStepBar({
    super.key,
    required this.onBack,
    required this.onNext,
    required this.nextLabel,
    this.backLabel,
  });

  final VoidCallback onBack;
  final VoidCallback? onNext;
  final String nextLabel;
  final String? backLabel;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: onBack,
                child: Text(backLabel ?? l10n.settingsCsvImportBack),
              ),
            ),
            const SizedBox(width: 12),
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

