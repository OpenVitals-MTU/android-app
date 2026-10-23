package tech.mmarca.openvitals.core.performance

import android.content.ComponentCallbacks2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenSnapshotStoreTest {
    private val week = ScreenSnapshotKey<String>("activities", "week")
    private val month = ScreenSnapshotKey<String>("activities", "month")
    private val steps = ScreenSnapshotKey<String>("activity", "week")

    @Test
    fun `a snapshot comes back only under the fingerprint it was saved with`() {
        val store = ScreenSnapshotStore()
        store.put(week, fingerprint = "granted", value = "walk")

        assertEquals("walk", store.get(week, fingerprint = "granted"))
        assertNull(store.get(week, fingerprint = "revoked"))
        assertNull(store.get(month, fingerprint = "granted"))
    }

    @Test
    fun `past capacity the least recently read snapshot goes`() {
        val store = ScreenSnapshotStore(capacity = 2)
        store.put(week, Unit, "week")
        store.put(month, Unit, "month")
        store.get(week, Unit)

        store.put(steps, Unit, "steps")

        assertEquals("week", store.get(week, Unit))
        assertNull(store.get(month, Unit))
        assertEquals("steps", store.get(steps, Unit))
    }

    @Test
    fun `clearing a screen keeps the others`() {
        val store = ScreenSnapshotStore()
        store.put(week, Unit, "week")
        store.put(month, Unit, "month")
        store.put(steps, Unit, "steps")

        store.clear("activities")

        assertNull(store.get(week, Unit))
        assertNull(store.get(month, Unit))
        assertEquals("steps", store.get(steps, Unit))
    }

    @Test
    fun `memory pressure clears the store, leaving the app does not`() {
        val store = ScreenSnapshotStore()
        store.put(week, Unit, "week")

        store.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        assertEquals("week", store.get(week, Unit))

        store.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        assertNull(store.get(week, Unit))
    }
}
