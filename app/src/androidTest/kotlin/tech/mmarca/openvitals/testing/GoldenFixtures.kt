package tech.mmarca.openvitals.testing

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The fixed clock every golden is anchored to. Never `LocalDate.now()`. */
val GoldenDay: LocalDate = LocalDate.of(2026, 6, 22)

/** A wall-clock moment on the device's own zone, so the picture is the same wherever the baseline is recorded. */
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
