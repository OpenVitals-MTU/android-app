package tech.mmarca.openvitals.data.repository.contract

/** The one stored medical records value. No record data is ever stored. */
interface MedicalRecordsPreferences {
    /** The medical area asks for its permissions once, on first open, and never again by itself. */
    var firstPermissionRequestDone: Boolean
}
