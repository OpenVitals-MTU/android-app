package tech.mmarca.openvitals.domain.model

import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.features.devicesync.store.syncFingerprint
import tech.mmarca.openvitals.features.imports.applehealth.buildStableClientRecordId
import tech.mmarca.openvitals.features.imports.csv.buildCsvClientRecordId

class ImportedClientRecordIdsTest {

    private val instant = Instant.parse("2026-10-04T12:00:00Z")

    @Test
    fun `every import and sync path writes an id read as imported`() {
        // Built by the writers themselves, so a renamed namespace fails here, not on the drink screens.
        val weight = WeightRecord(instant, ZoneOffset.UTC, Mass.kilograms(70.0), Metadata.manualEntry())

        listOf(
            buildStableClientRecordId("nutrition", listOf("source", instant)),
            buildCsvClientRecordId("NutritionRecord", instant),
            syncFingerprint(weight),
        ).forEach { id -> assertTrue(id, isImportedClientRecordId(id)) }
    }

    @Test
    fun `an entry logged in OpenVitals, or one with no id, is not imported`() {
        listOf(
            null,
            "openvitals_nutrition_1_u",
            "openvitals_hydration_nutrition_openvitals_hydration_1_u",
            "openvitals_food_1_banana_u",
            "openvitals_manual_nutrition_1_u",
        ).forEach { id -> assertFalse(id.toString(), isImportedClientRecordId(id)) }
    }
}
