package tech.mmarca.openvitals.data.repository.contract

/** The order the user arranged the manual entry widgets and the metric detail sections in. */
interface WidgetOrderPreferences {

    /** Null before the first arrangement: the screen shows its default order. */
    fun manualEntryWidgetOrder(): List<String>?

    fun setManualEntryWidgetOrder(widgetIds: List<String>)

    /** Null before the first arrangement: the screen shows its default order. */
    fun metricDetailSectionOrder(): List<String>?

    fun setMetricDetailSectionOrder(sectionIds: List<String>)
}
