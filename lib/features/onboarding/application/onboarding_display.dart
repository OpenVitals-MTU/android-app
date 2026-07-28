import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../domain/model/onboarding_permission_category.dart';

part 'onboarding_display.freezed.dart';

/// One permission-category row, with everything the row's status line and its
/// action button used to work out for themselves on every rebuild: how much of
/// the category is granted, and what is left to ask for.
@freezed
abstract class OnboardingCategoryRow with _$OnboardingCategoryRow {
  const factory OnboardingCategoryRow({
    required OnboardingPermissionCategory category,
    required int total,
    required int grantedCount,
    required bool fullyGranted,
    required bool partial,

    /// The still-missing permissions the runtime dialog CAN ask for.
    required Set<String> missingRequestable,

    /// The still-missing permissions it cannot (exercise routes, background and
    /// history access): only a trip to the Health Connect page grants those.
    required Set<String> missingManual,

    /// A category whose remaining permissions are all manual-only — it shows
    /// "Open settings" and an "Open" action instead of "Grant" (Kotlin
    /// `isManualGrant`).
    required bool isManualGrant,
  }) = _OnboardingCategoryRow;
}

/// The screen-ready derivation of the permission catalog against the currently
/// granted set: the rows, what the single grant button still has to ask for, and
/// whether the required set is covered (which is what turns it into "Continue").
///
/// Built once per state change by [buildOnboardingDisplay] and stored on the
/// state — the view-model precomputes, the widgets only render.
@freezed
abstract class OnboardingDisplay with _$OnboardingDisplay {
  const factory OnboardingDisplay({
    required List<OnboardingCategoryRow> rows,

    /// The required permissions that are still missing — the whole of what the
    /// single grant button requests, in one go.
    required Set<String> missingRequired,

    /// Nothing required is outstanding: the primary action becomes "Continue".
    /// Until then there is no way past onboarding — the required set is not a
    /// suggestion.
    required bool requiredGranted,
  }) = _OnboardingDisplay;
}

/// Pure derivation from the permission catalog + the granted set to the row
/// model. No ref, no I/O, no localization — unit-testable with a fixture
/// catalog.
OnboardingDisplay buildOnboardingDisplay(
  OnboardingPermissionCatalog catalog,
  Set<String> granted,
) {
  final missingRequired = catalog.requiredPermissions.difference(granted);
  return OnboardingDisplay(
    rows: [
      for (final category in catalog.categories) _row(category, granted),
    ],
    missingRequired: missingRequired,
    requiredGranted: missingRequired.isEmpty,
  );
}

OnboardingCategoryRow _row(
  OnboardingPermissionCategory category,
  Set<String> granted,
) {
  final total = category.permissions.length;
  final grantedCount = category.permissions.where(granted.contains).length;
  final fullyGranted = category.available && grantedCount == total;
  final partial = category.available && grantedCount > 0 && !fullyGranted;
  final missing = category.permissions.difference(granted);
  final missingRequestable = missing.difference(category.manualPermissions);
  final missingManual = missing.intersection(category.manualPermissions);
  return OnboardingCategoryRow(
    category: category,
    total: total,
    grantedCount: grantedCount,
    fullyGranted: fullyGranted,
    partial: partial,
    missingRequestable: missingRequestable,
    missingManual: missingManual,
    isManualGrant: missingRequestable.isEmpty && missingManual.isNotEmpty,
  );
}
