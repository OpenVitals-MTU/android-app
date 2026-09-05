package tech.mmarca.openvitals.features.manualentry

import org.junit.Assert.assertEquals
import org.junit.Test

class ManualEntryWritePermissionRequesterTest {

    @Test fun `launch asks Health Connect for exactly the given set and ignores an empty one`() {
        val launched = mutableListOf<Set<String>>()
        val requester = ManualEntryWritePermissionRequester(launchDialog = { launched += it })

        requester.launch(setOf("write_weight"))
        requester.launch(emptySet())

        assertEquals(listOf(setOf("write_weight")), launched)
    }
}
