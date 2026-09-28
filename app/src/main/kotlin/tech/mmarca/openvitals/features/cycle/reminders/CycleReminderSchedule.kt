package tech.mmarca.openvitals.features.cycle.reminders

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.model.CycleReminderConfig
import tech.mmarca.openvitals.domain.model.PillPlan

/** The cycle reminders and the pill. The ordinal is the alarm request code offset, so new ones go last. */
enum class CycleReminderType {
    DAILY_CHECK_IN,
    PERIOD_WINDOW,
    LATE_CYCLE,
    PILL,
}

/** Pure timing rules. Null means no alarm. */
internal object CycleReminderSchedule {
    private val PeriodWindowTime: LocalTime = LocalTime.of(9, 0)
    private val LateCycleTime: LocalTime = LocalTime.of(10, 0)

    /** Today at the set time, unless it passed or today is already logged; then tomorrow. */
    fun nextDailyCheckIn(now: ZonedDateTime, config: CycleReminderConfig, loggedToday: Boolean): ZonedDateTime {
        val today = now.toLocalDate().atTime(config.dailyCheckInTime).atZone(now.zone)
        return if (!loggedToday && today.isAfter(now)) today else today.plusDays(1)
    }

    /** The lead days before the earliest estimated date, at nine. */
    fun periodWindow(now: ZonedDateTime, config: CycleReminderConfig, estimate: CycleEstimateResult): ZonedDateTime? {
        val earliest = estimate.estimateOrNull?.earliestDate ?: return null
        return earliest.minusDays(config.periodWindowLeadDays.toLong()).atTime(PeriodWindowTime).atZone(now.zone)
            .takeIf { it.isAfter(now) }
    }

    /** The grace days after the latest estimated date, at ten. */
    fun lateCycle(now: ZonedDateTime, config: CycleReminderConfig, estimate: CycleEstimateResult): ZonedDateTime? {
        val latest = estimate.estimateOrNull?.latestDate ?: return null
        return latest.plusDays(config.lateCycleGraceDays.toLong()).atTime(LateCycleTime).atZone(now.zone)
            .takeIf { it.isAfter(now) }
    }

    /** Today at the set time on a taking day, unless it passed or today is taken; else the next taking day. Null without a pack start. */
    fun nextPill(now: ZonedDateTime, plan: PillPlan, takenToday: Boolean): ZonedDateTime? {
        val today = now.toLocalDate()
        val todayAt = today.atTime(plan.reminderTime).atZone(now.zone)
        if (plan.dayAt(today)?.isActive == true && !takenToday && todayAt.isAfter(now)) return todayAt
        val next = plan.nextActiveDay(today.plusDays(1)) ?: return null
        return next.atTime(plan.reminderTime).atZone(now.zone)
    }

    fun LocalDate.isToday(now: ZonedDateTime): Boolean = this == now.toLocalDate()
}
