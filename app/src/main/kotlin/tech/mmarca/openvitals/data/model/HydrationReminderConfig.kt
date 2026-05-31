package tech.mmarca.openvitals.data.model

import java.time.LocalTime

data class HydrationReminderConfig(
    val enabled: Boolean = false,
    val intervalMinutes: Int = DefaultIntervalMinutes,
    val activeStartTime: LocalTime = DefaultActiveStartTime,
    val activeEndTime: LocalTime = DefaultActiveEndTime,
) {
    fun normalized(): HydrationReminderConfig =
        copy(intervalMinutes = normalizeIntervalMinutes(intervalMinutes))

    companion object {
        const val DefaultIntervalMinutes = 120
        const val MinIntervalMinutes = 30
        const val MaxIntervalMinutes = 240
        const val IntervalStepMinutes = 30
        val DefaultActiveStartTime: LocalTime = LocalTime.of(7, 0)
        val DefaultActiveEndTime: LocalTime = LocalTime.of(23, 0)

        fun normalizeIntervalMinutes(value: Int): Int {
            val rounded = (value / IntervalStepMinutes) * IntervalStepMinutes
            return rounded.coerceIn(MinIntervalMinutes, MaxIntervalMinutes)
        }
    }
}
