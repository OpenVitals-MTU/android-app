import '../../../core/time/local_date.dart';

/// Device-local start-of-day instant for [date] (mirrors the Kotlin
/// `date.atStartOfDay(zone).toInstant()`).
DateTime localDayStart(LocalDate date) =>
    DateTime(date.year, date.month, date.day);

/// Device-local end-of-day boundary (start of the next day), matching the
/// Kotlin readers' `end.plusDays(1).atStartOfDay(zone)` exclusive boundary.
DateTime localDayEnd(LocalDate date) => localDayStart(date.plusDays(1));
