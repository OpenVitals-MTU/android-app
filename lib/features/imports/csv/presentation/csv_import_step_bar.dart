/// The back/next bar every step of the CSV importer shares.
///
/// The bar itself now lives in `lib/ui/components/step_bar.dart` — onboarding
/// wanted the same thing, and two copies of a footer is how they drift apart.
/// This stays as the CSV-flavoured wrapper: it supplies the importer's own
/// default back label so no call site had to change.
library;

import 'package:flutter/material.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/step_bar.dart';

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
    return StepBar(
      onBack: onBack,
      onNext: onNext,
      nextLabel: nextLabel,
      backLabel: backLabel ?? l10n.settingsCsvImportBack,
    );
  }
}
