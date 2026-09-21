package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow

/** How long one split is, in meters. A workout screen re-cuts its splits when it changes. */
interface ActivitySplitPreferences {

    var activitySplitDistanceMeters: Double

    /** Emits on every change, starting with the current value. */
    val activitySplitDistanceMetersFlow: Flow<Double>
}
