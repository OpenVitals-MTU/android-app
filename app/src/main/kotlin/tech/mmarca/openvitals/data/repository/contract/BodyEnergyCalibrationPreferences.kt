package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration

/** The Body Energy zone ladder and the gains the watch learner tunes. */
interface BodyEnergyCalibrationPreferences {

    fun bodyEnergyCalibration(): BodyEnergyCalibration

    /** Emits on every change, starting with the current value. */
    val bodyEnergyCalibrationFlow: Flow<BodyEnergyCalibration>

    fun setBodyEnergyCalibration(calibration: BodyEnergyCalibration)
}
