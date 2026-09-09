package tech.mmarca.openvitals.devices.garmin

/** Where a held companion link hands the files it pulls. One per watch. */
interface GarminHeldSyncOwner {
    /** Dedup keys of files already imported, read at handoff time. */
    fun alreadySyncedKeys(): Set<String>

    /** Persists one file. Throwing keeps the watch from archiving it. */
    suspend fun keep(file: GarminDownloadedFile)

    /** A finished batch, every file already kept. May be empty. */
    fun imported(files: List<GarminDownloadedFile>)

    /** The watch has data this link cannot list; run a full sync. */
    fun needsFullSync()
}
