import 'package:flutter/material.dart';

import '../../core/time/local_date.dart';

/// The earliest date the health date picker allows (mirrors the practical range
/// of the Kotlin `HealthDatePickerDialog`, whose `rememberDatePickerState`
/// defaults to a wide historical range).
final LocalDate _defaultFirstDate = LocalDate(LocalDate.now().year - 10, 1, 1);

/// Opens a Material date picker for choosing a period anchor date, capped at
/// today (you cannot navigate into the future). Returns the chosen [LocalDate],
/// or null if dismissed. Port of Kotlin `HealthDatePickerDialog`.
Future<LocalDate?> showHealthDatePicker(
  BuildContext context, {
  required LocalDate selectedDate,
  LocalDate? firstDate,
}) async {
  final today = LocalDate.now();
  final first = firstDate ?? _defaultFirstDate;
  final initial = selectedDate.coerceAtMost(today).coerceAtLeast(first);
  final picked = await showDatePicker(
    context: context,
    initialDate: DateTime(initial.year, initial.month, initial.day),
    firstDate: DateTime(first.year, first.month, first.day),
    lastDate: DateTime(today.year, today.month, today.day),
  );
  if (picked == null) return null;
  return LocalDate(picked.year, picked.month, picked.day).coerceAtMost(today);
}
