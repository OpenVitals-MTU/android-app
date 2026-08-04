package tech.mmarca.openvitals.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ported from the Flutter `test/navigation/csv_import_route_test.dart`: the CSV
 * importer is reached by a pushed route rather than a settings card, and it must
 * stay under the data-import section it belongs to.
 */
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
