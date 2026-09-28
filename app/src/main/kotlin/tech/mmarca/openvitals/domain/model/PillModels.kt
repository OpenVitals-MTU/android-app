package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * How the pill is taken: a run of taking days, then a pause, repeating from
 * the first day of any pack. Health Connect has no record type for it.
 */
data class PillPlan(
    val enabled: Boolean = false,
    val activeDays: Int = 21,
    val pauseDays: Int = 7,
    /** The first day of a pack. Null until the user picks one. */
    val packStart: LocalDate? = null,
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = DefaultReminderTime,
    /** When the scheme was last edited. Epoch until the user touches it; the newer edit wins across phones. */
    val updatedAt: Instant = Instant.EPOCH,
) {
    val cycleLength: Int
        get() = activeDays + pauseDays

    fun normalized(): PillPlan = copy(
        activeDays = activeDays.coerceIn(ActiveDaysRange),
        pauseDays = pauseDays.coerceIn(PauseDaysRange),
        reminderTime = reminderTime.withSecond(0).withNano(0),
    )

    /** Where [date] falls in the scheme. Null without a pack start, or before the first pack. */
    fun dayAt(date: LocalDate): PillDay? {
        val start = packStart ?: return null
        val offset = ChronoUnit.DAYS.between(start, date)
        if (offset < 0) return null
        val length = cycleLength
        val index = (offset % length).toInt()
        val nextPackStart = start.plusDays((offset / length + 1) * length)
        return if (index < activeDays) {
            PillDay(dayOfPhase = index + 1, phaseLength = activeDays, isActive = true, nextPackStart = nextPackStart)
        } else {
            PillDay(dayOfPhase = index - activeDays + 1, phaseLength = pauseDays, isActive = false, nextPackStart = nextPackStart)
        }
    }

    /** The first taking day on or after [date]. Null without a pack start. */
    fun nextActiveDay(date: LocalDate): LocalDate? {
        val start = packStart ?: return null
        if (date.isBefore(start)) return start
        val day = dayAt(date) ?: return null
        return if (day.isActive) date else day.nextPackStart
    }

    companion object {
        val DefaultReminderTime: LocalTime = LocalTime.of(20, 0)
        val ActiveDaysRange = 1..91
        val PauseDaysRange = 0..14
    }
}

/** One day of a pill scheme: which day of the taking run or the pause it is. */
data class PillDay(
    val dayOfPhase: Int,
    val phaseLength: Int,
    val isActive: Boolean,
    val nextPackStart: LocalDate,
)
