package tech.mmarca.openvitals.features.manualentry.cycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CycleEntryKind

class CycleEntrySectionTest {
    @Test fun `route values round-trip and unknown values give the full log`() {
        CycleEntrySection.entries.forEach { section ->
            assertEquals(section, CycleEntrySection.fromRoute(section.routeValue))
        }
        assertNull(CycleEntrySection.fromRoute(null))
        assertNull(CycleEntrySection.fromRoute("no_such_section"))
    }

    @Test fun `every Health Connect kind belongs to exactly one section`() {
        val owners = CycleEntrySection.entries.flatMap { it.writeKinds }.groupingBy { it }.eachCount()
        assertEquals(CycleEntryKind.entries.toSet(), owners.keys)
        assertTrue(owners.values.all { it == 1 })
    }

    @Test fun `a focused day log lacks permission only for the kinds it writes`() {
        val nothingGranted = CycleEntryUiState(grantedKinds = emptySet())
        assertTrue(nothingGranted.lacksWritePermission)
        assertFalse(nothingGranted.copy(section = CycleEntrySection.FEELING).lacksWritePermission)
        assertFalse(nothingGranted.copy(section = CycleEntrySection.PREGNANCY_TEST).lacksWritePermission)
        assertTrue(nothingGranted.copy(section = CycleEntrySection.BLEEDING).lacksWritePermission)

        val flowOnly = nothingGranted.copy(grantedKinds = setOf(CycleEntryKind.MENSTRUATION_FLOW))
        assertFalse(flowOnly.lacksWritePermission)
        assertTrue(flowOnly.copy(section = CycleEntrySection.BLEEDING).lacksWritePermission)
        assertTrue(flowOnly.copy(section = CycleEntrySection.SEXUAL_ACTIVITY).lacksWritePermission)
    }
}
