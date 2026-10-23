package tech.mmarca.openvitals.core.performance

import android.content.ComponentCallbacks2
import android.content.res.Configuration

/**
 * What a screen last loaded for a query, in memory for the life of the process, so a
 * screen reopened moments later draws at once instead of empty.
 *
 * A snapshot is never the answer. The screen shows it while it reads Health Connect as
 * it always does, and the live result replaces it. Nothing is written to disk.
 *
 * A snapshot is only handed back under the [fingerprint] it was saved with: the read
 * grants, the time zone and whether Health Connect may be read at all. A grant revoked
 * since never shows its data again. Memory pressure clears the store.
 */
class ScreenSnapshotStore(
    private val fingerprint: suspend () -> Any = { Unit },
    private val capacity: Int = DefaultCapacity,
) : ComponentCallbacks2 {

    private class Entry(val fingerprint: Any, val value: Any)

    private val entries = object : LinkedHashMap<ScreenSnapshotKey<*>, Entry>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ScreenSnapshotKey<*>, Entry>?): Boolean =
            size > capacity
    }

    /** The fingerprint to look up and save under; read it once per load, before the reads. */
    suspend fun fingerprint(): Any = fingerprint.invoke()

    @Suppress("UNCHECKED_CAST")
    fun <V : Any> get(key: ScreenSnapshotKey<V>, fingerprint: Any): V? = synchronized(entries) {
        entries[key]?.takeIf { it.fingerprint == fingerprint }?.value as V?
    }

    fun <V : Any> put(key: ScreenSnapshotKey<V>, fingerprint: Any, value: V) {
        synchronized(entries) { entries[key] = Entry(fingerprint, value) }
    }

    /** Drops every snapshot of [screen], after a write that changes what all its periods show. */
    fun clear(screen: String) {
        synchronized(entries) { entries.keys.removeAll { it.screen == screen } }
    }

    fun clear() {
        synchronized(entries) { entries.clear() }
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) clear()
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() = clear()

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    private companion object {
        /** A few periods of a few screens. A year of workouts with routes is not small. */
        const val DefaultCapacity = 12
    }
}

/**
 * The query a snapshot answers, typed by what it holds. One [screen] always holds one type,
 * and [query] carries everything the result depends on besides the fingerprint.
 */
data class ScreenSnapshotKey<V : Any>(val screen: String, val query: Any)
