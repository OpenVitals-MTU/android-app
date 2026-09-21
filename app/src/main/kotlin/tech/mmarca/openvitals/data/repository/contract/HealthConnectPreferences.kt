package tech.mmarca.openvitals.data.repository.contract

/** The switches on the Health Connect section of Settings. */
interface HealthConnectPreferences {

    var healthConnectSyncEnabled: Boolean

    var healthConnectMindfulnessEnabled: Boolean

    var appLockEnabled: Boolean
}
