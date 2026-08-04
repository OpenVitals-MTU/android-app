package tech.mmarca.openvitals.testing

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The fixed clock every golden in the chart suite is anchored to — the Flutter
 * suite's "golden day", kept so the two sets of pictures are of the same week.
 *
 * Never `LocalDate.now()`: a golden that depends on today's date fails tomorrow.
 */
val GoldenDay: LocalDate = LocalDate.of(2026, 6, 22)

/**
 * A wall-clock moment resolved on the DEVICE's own zone.
 *
 * Charts place samples by instant and print them back as local clock times, so a
 * fixture built from a fixed UTC instant would draw a different picture on a phone
 * in a different zone. Building from local time instead keeps the picture — "09:00
 * to 09:45" — the same wherever the baseline is recorded.
 */
fun goldenInstant(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int = 0,
): Instant = LocalDateTime.of(year, month, day, hour, minute)
    .atZone(ZoneId.systemDefault())
    .toInstant()

/** [goldenInstant] on the golden day itself. */
fun goldenInstantAt(hour: Int, minute: Int = 0): Instant =
    goldenInstant(GoldenDay.year, GoldenDay.monthValue, GoldenDay.dayOfMonth, hour, minute)

/** Every date in `[from, to]`, inclusive. */
fun goldenDates(from: LocalDate, to: LocalDate): List<LocalDate> =
    generateSequence(from) { date -> date.plusDays(1).takeUnless { it.isAfter(to) } }.toList()
