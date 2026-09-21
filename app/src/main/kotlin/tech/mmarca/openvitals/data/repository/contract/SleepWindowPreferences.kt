package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.domain.preferences.SleepWindow

/** The hours a night covers. Sleep and mindfulness read it to decide which day a session belongs to. */
interface SleepWindowPreferences {

    val sleepWindow: SleepWindow

    /** Emits on every change, starting with the current value. */
    val sleepWindowFlow: Flow<SleepWindow>
}
