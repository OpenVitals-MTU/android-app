package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataSourceAttributionTest {

    @Test fun `truncatedDataSourceLabel keeps short labels unchanged`() {
        assertEquals("OpenTracks", truncatedDataSourceLabel("OpenTracks"))
    }

    @Test fun `truncatedDataSourceLabel trims long labels with three dots`() {
        val label = "com.example.super.long.health.data.provider"

        val result = truncatedDataSourceLabel(label)

        assertEquals("com.example.super.lon...", result)
        assertEquals(24, result.length)
        assertTrue(result.endsWith("..."))
    }

    @Test fun `truncatedDataSourceLabel trims whitespace before limiting`() {
        assertEquals("Samsung Health", truncatedDataSourceLabel("  Samsung Health  "))
    }
}
