package tech.mmarca.openvitals.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The CSV importer is reached by a pushed route and stays under the data-import section. */
class CsvImportRouteTest {

    @Test
    fun `the CSV import path sits under the data-import section`() {
        assertTrue(
            Screen.SettingsCsvImport.route,
            Screen.SettingsCsvImport.route.startsWith(Screen.SettingsDataImport.route),
        )
        assertEquals("settings/data_import/csv", Screen.SettingsCsvImport.route)
    }
}
