package tech.mmarca.openvitals.devices.garmin

/** Which listing protocol a watch has proven. Remembered per device by [GarminDeviceStateStore]. */
enum class GarminSyncProtocol {
    /** Not proven yet. An empty legacy listing probes FileSync again next time. */
    UNKNOWN,

    /** The legacy 16-byte directory listed records. FileSync is not probed. */
    LEGACY,

    /** The protobuf FileSyncService answered a listing. */
    FILE_SYNC,
}
