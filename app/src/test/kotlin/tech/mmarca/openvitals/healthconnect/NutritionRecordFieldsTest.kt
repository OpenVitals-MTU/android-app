package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.NutritionNutrient

class NutritionRecordFieldsTest {

    @Test
    fun `every nutrient is written to its own field and read back from it`() {
        // A distinct amount per nutrient, so two nutrients sharing a field would show.
        val values = NutritionNutrient.entries.mapIndexed { index, nutrient -> nutrient to (index + 1) / 10.0 }.toMap()
        val start = Instant.parse("2026-10-04T12:00:00Z")

        val record = nutritionRecordOf(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = start.plusSeconds(60),
            endZoneOffset = ZoneOffset.UTC,
            metadata = Metadata.manualEntry(),
            nutrientValues = values,
            name = "Everything",
        )

        val readBack = record.nutritionNutrientValues()
        assertEquals(values.keys, readBack.keys)
        values.forEach { (nutrient, value) -> assertEquals(nutrient.name, value, readBack.getValue(nutrient), 1e-9) }
    }
}
