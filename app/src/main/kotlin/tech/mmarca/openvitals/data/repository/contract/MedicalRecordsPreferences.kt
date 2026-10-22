package tech.mmarca.openvitals.data.repository.contract

/** The stored medical records values. No record data is ever stored. */
interface MedicalRecordsPreferences {
    /** The medical area asks for its permissions once, on first open, and never again by itself. */
    var firstPermissionRequestDone: Boolean

    /** The beta notice shows on the first open of the records home, and never again once dismissed. */
    var betaNoticeSeen: Boolean
}
