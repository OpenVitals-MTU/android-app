package tech.mmarca.openvitals.domain.preferences

/**
 * The nightly sleep window, in device-local clock hours, ending at [endHour]
 * on the wake-up date. A window that spans midnight (the 18:00 → 10:00
 * default) starts the previous evening; one that does not (00:00 → 12:00)
 * lies wholly within the wake-up date. Sessions that begin outside it are
 * daytime naps. The night lands on its wake-up date either way.
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
