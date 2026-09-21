package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode

/**
 * The period a metric screen shows: the range it was last on, and where its weeks start.
 *
 * A screen needs these four members, not the whole [tech.mmarca.openvitals.data.repository.PreferencesRepository].
 */
interface PeriodPreferences {

    /** Where a week starts. Derived from [activityWeekMode]. */
    val weekPeriodMode: WeekPeriodMode

    /** Emits on every change, starting with the current value. */
    val weekPeriodModeFlow: Flow<WeekPeriodMode>

    var activityWeekMode: ActivityWeekMode

    val activityWeekModeFlow: Flow<ActivityWeekMode>

    /** The range this screen was last on, or the key's default. */
    fun timeRangeFor(key: PeriodRangePreferenceKey): TimeRange

    fun setTimeRangeFor(key: PeriodRangePreferenceKey, range: TimeRange)
}
