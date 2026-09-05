package tech.mmarca.openvitals.domain.preferences

/**
 * The nightly sleep window in local clock hours, ending at [endHour] on the
 * wake-up date. Sessions starting outside it are naps.
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
