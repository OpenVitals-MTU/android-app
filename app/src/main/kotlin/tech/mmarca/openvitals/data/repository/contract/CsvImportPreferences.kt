package tech.mmarca.openvitals.data.repository.contract

/** The CSV importer's remembered choices. Values are Health Connect's BloodPressureRecord constants. */
interface CsvImportPreferences {
    /** The body position a blood pressure reading takes when its file does not say, or says something unrecognised. */
    var defaultBodyPosition: Int

    /** The cuff location a blood pressure reading takes when its file does not say, or says something unrecognised. */
    var defaultCuffLocation: Int
}
