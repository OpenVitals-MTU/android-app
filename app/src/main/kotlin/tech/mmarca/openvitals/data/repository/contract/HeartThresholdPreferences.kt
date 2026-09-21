package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.domain.preferences.BloodPressureGuideline

/** What counts as a high or low reading. The Heart screen flags readings against these. */
interface HeartThresholdPreferences {

    /** Clamped to the repository's bounds on write; read back for the stored value. */
    var highHeartRateThresholdBpm: Int

    var lowHeartRateThresholdBpm: Int

    /** Which body's blood pressure bands to classify against. */
    var bloodPressureGuideline: BloodPressureGuideline

    /** Emits on every change, starting with the current value. */
    val bloodPressureGuidelineFlow: Flow<BloodPressureGuideline>
}
