package tech.mmarca.openvitals.domain.preferences

/**
 * The nightly sleep window, in device-local clock hours. A night is captured
 * from [startHour] the previous evening to [endHour] the next morning (default
 * 18:00 → 10:00). Sessions that begin outside it are daytime naps. The night
 * lands on its wake-up date.
 */
data class SleepWindow(
    val startHour: Int,
    val endHour: Int,
) {
    override fun toString(): String =
        "SleepWindow(%02d:00-%02d:00)".format(startHour, endHour)

    companion object {
        val Default = SleepWindow(startHour = 18, endHour = 10)
    }
}
