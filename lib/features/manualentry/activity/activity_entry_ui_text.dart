import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../l10n/app_localizations.dart';
import '../presentation/manual_entry_form_scaffold.dart';
import 'activity_entry_state.dart';

/// Port of the Kotlin `ActivityEntryUiText.kt`: the localized text for
/// per-field validation errors and screen-level entry errors, plus the
/// date/time parsing and formatting the start-time fields round-trip through.

/// Kotlin `ActivityEntryTimeFormatter = DateTimeFormatter.ofPattern("H:mm")`.
///
/// This is the *storage* format for `startTimeText`, deliberately not
/// locale-aware: the state holds `H:mm` and only the picker field renders it in
/// the user's locale.
String formatActivityEntryTime(TimeOfDay time) =>
    '${time.hour}:${time.minute.toString().padLeft(2, '0')}';

/// Kotlin `String.toStartDateOrNull()`. Accepts the ISO `yyyy-MM-dd` the state
/// stores; returns null for anything else.
DateTime? parseActivityStartDate(String text) {
  final trimmed = text.trim();
  final match = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(trimmed);
  if (match == null) return null;
  final year = int.parse(match.group(1)!);
  final month = int.parse(match.group(2)!);
  final day = int.parse(match.group(3)!);
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  final date = DateTime(year, month, day);
  // Reject overflow like 2024-02-31, which DateTime silently rolls forward.
  if (date.year != year || date.month != month || date.day != day) return null;
  return date;
}

/// Kotlin `String.toStartTimeOrNull()`, parsing the `H:mm` storage format.
TimeOfDay? parseActivityStartTime(String text) {
  final match = RegExp(r'^(\d{1,2}):(\d{2})$').firstMatch(text.trim());
  if (match == null) return null;
  final hour = int.parse(match.group(1)!);
  final minute = int.parse(match.group(2)!);
  if (hour > 23 || minute > 59) return null;
  return TimeOfDay(hour: hour, minute: minute);
}

/// Kotlin `LocalDate.localizedDateText()` — `FormatStyle.MEDIUM`.
String localizedActivityDateText(DateTime date, String? locale) =>
    DateFormat.yMMMd(locale).format(date);

/// Kotlin `LocalTime.localizedTimeText()` — `FormatStyle.SHORT`, which follows
/// the locale's 12/24-hour convention.
String localizedActivityTimeText(
  BuildContext context,
  TimeOfDay time,
) =>
    MaterialLocalizations.of(context).formatTimeOfDay(
      time,
      alwaysUse24HourFormat: MediaQuery.alwaysUse24HourFormatOf(context),
    );

/// Kotlin `ActivityEntryFeeling.labelRes`. The enum itself carries an English
/// `label` used for the saved note text; this is the on-screen (and
/// accessibility) label.
String activityFeelingLabel(ActivityEntryFeeling feeling, AppLocalizations l10n) =>
    switch (feeling) {
      ActivityEntryFeeling.great => l10n.activityEntryFeelingGreat,
      ActivityEntryFeeling.good => l10n.activityEntryFeelingGood,
      ActivityEntryFeeling.hard => l10n.activityEntryFeelingHard,
      ActivityEntryFeeling.rough => l10n.activityEntryFeelingRough,
    };

/// Kotlin `ActivityEntryValidationError.validationMessage()`.
String activityValidationMessage(
  ActivityEntryValidationError error,
  AppLocalizations l10n,
) =>
    switch (error) {
      ActivityEntryValidationError.activityTypeDoesNotSupportRoute =>
        l10n.activityEntryErrorActivityTypeRoute,
      ActivityEntryValidationError.trainingPlanTitleRequired =>
        l10n.activityEntryErrorTrainingPlanTitleRequired,
      ActivityEntryValidationError.startDateInvalid =>
        l10n.activityEntryErrorStartDate,
      ActivityEntryValidationError.startTimeInvalid =>
        l10n.activityEntryErrorStartTime,
      ActivityEntryValidationError.startTimeAfterRouteStart =>
        l10n.activityEntryErrorStartTimeAfterRoute,
      ActivityEntryValidationError.durationInvalid =>
        l10n.activityEntryErrorDuration,
      ActivityEntryValidationError.repetitionsInvalid =>
        l10n.activityEntryErrorRepetitions,
      ActivityEntryValidationError.distanceInvalid =>
        l10n.activityEntryErrorDistance,
      ActivityEntryValidationError.distanceUnsupported =>
        l10n.activityEntryErrorDistanceUnsupported,
      ActivityEntryValidationError.elevationInvalid =>
        l10n.activityEntryErrorElevation,
      ActivityEntryValidationError.elevationUnsupported =>
        l10n.activityEntryErrorElevationUnsupported,
      ActivityEntryValidationError.activeCaloriesInvalid =>
        l10n.activityEntryErrorActiveCalories,
      ActivityEntryValidationError.totalCaloriesInvalid =>
        l10n.activityEntryErrorTotalCalories,
      ActivityEntryValidationError.totalCaloriesBelowActive =>
        l10n.activityEntryErrorTotalCaloriesBelowActive,
    };

/// Kotlin `ActivityEntryUiState.validationErrorText(field)`: the message for the
/// first validation error attached to [field], or null when the field is clean.
extension ActivityEntryValidationText on ActivityEntryUiState {
  String? validationErrorText(ActivityEntryField field, AppLocalizations l10n) {
    for (final error in validationErrors) {
      if (error.field == field) return activityValidationMessage(error, l10n);
    }
    return null;
  }
}

/// Kotlin `activityEntryErrorText(error, detailError)`.
String activityEntryErrorText(
  ActivityEntryError error,
  ScreenError? detailError,
  AppLocalizations l10n,
) {
  final detail = screenErrorText(detailError, l10n);
  return switch (error) {
    ActivityEntryError.invalidValue => l10n.activityEntryInvalidValue,
    ActivityEntryError.missingWritePermission =>
      l10n.activityEntryPermissionNeeded,
    ActivityEntryError.routeImportFailed =>
      l10n.activityEntryRouteImportFailed(detail),
    ActivityEntryError.locationPermissionNeeded =>
      l10n.activityEntryLocationPermissionNeeded,
    ActivityEntryError.notificationPermissionNeeded =>
      l10n.activityEntryNotificationPermissionNeeded,
    ActivityEntryError.activityRecognitionPermissionNeeded =>
      l10n.activityEntryActivityRecognitionPermissionNeeded,
    ActivityEntryError.recordingFailed =>
      l10n.activityEntryRecordingFailed(detail),
    ActivityEntryError.writeFailed => l10n.activityEntryWriteFailed(detail),
  };
}

/// Kotlin `FieldErrorText`: the supporting error line under a field. Renders
/// nothing when there is no error.
class FieldErrorText extends StatelessWidget {
  const FieldErrorText(this.errorText, {super.key});

  final String? errorText;

  @override
  Widget build(BuildContext context) {
    final text = errorText;
    if (text == null) return const SizedBox.shrink();
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: Text(
        text,
        style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error),
      ),
    );
  }
}

/// The screen-level error line the Kotlin cards render under their content.
class ActivityEntryErrorText extends StatelessWidget {
  const ActivityEntryErrorText({super.key, required this.state});

  final ActivityEntryUiState state;

  @override
  Widget build(BuildContext context) {
    final error = state.entryError;
    if (error == null) return const SizedBox.shrink();
    final theme = Theme.of(context);
    return Text(
      activityEntryErrorText(error, state.detailError, AppLocalizations.of(context)),
      style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error),
    );
  }
}
