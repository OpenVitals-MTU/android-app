package tech.mmarca.openvitals.core.presentation

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class DateTimeFormatterProvider(
    private val localeProvider: () -> Locale = { Locale.getDefault() },
) {
    fun chartDay(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d", localeProvider())

    fun chartDayOfMonth(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("d", localeProvider())

    fun chartMonth(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("LLL", localeProvider())

    fun mediumDate(): DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(localeProvider())

    fun mediumDateTime(): DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(localeProvider())

    fun shortTime(): DateTimeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(localeProvider())

    fun monthYear(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("LLLL yyyy", localeProvider())

    fun year(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy", localeProvider())
}
