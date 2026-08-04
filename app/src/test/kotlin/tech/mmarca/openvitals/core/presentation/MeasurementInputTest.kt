package tech.mmarca.openvitals.core.presentation

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.manualentry.body.canonicalBodyMeasurementValue
import tech.mmarca.openvitals.features.manualentry.hydration.hydrationInputAmountText
import tech.mmarca.openvitals.features.manualentry.hydration.hydrationInputMilliliters
import tech.mmarca.openvitals.features.manualentry.vitals.canonicalVitalsValue

/**
 * Ported from the Flutter `test/core/presentation/measurement_input_test.dart`.
 * Kotlin has no shared measurement-input module, so each case drives the entry
 * screen's own canonicalization helper.
 */
class MeasurementInputTest {

    @Test
    fun `volume round-trips a typed imperial amount back to the same text`() {
        val milliliters = hydrationInputMilliliters("12.0", UnitSystem.IMPERIAL)!!

        assertEquals("12.0", hydrationInputAmountText(milliliters, formatter(UnitSystem.IMPERIAL)))
    }

    @Test
    fun `body weight pounds convert to kilograms`() {
        assertEquals(
            70.0,
            canonicalBodyMeasurementValue("70", BodyMeasurementType.WEIGHT, UnitSystem.METRIC)!!,
            0.0,
        )
        assertEquals(
            70.0,
            canonicalBodyMeasurementValue(
                "154.32",
                BodyMeasurementType.WEIGHT,
                UnitSystem.IMPERIAL,
            )!!,
            0.01,
        )
    }

    @Test
    fun `body height inches convert to centimetres`() {
        assertEquals(
            180.0,
            canonicalBodyMeasurementValue("180", BodyMeasurementType.HEIGHT, UnitSystem.METRIC)!!,
            0.0,
        )
        assertEquals(
            177.8,
            canonicalBodyMeasurementValue("70", BodyMeasurementType.HEIGHT, UnitSystem.IMPERIAL)!!,
            1e-9,
        )
    }

    @Test
    fun `temperature Fahrenheit converts to Celsius`() {
        assertEquals(
            37.0,
            canonicalVitalsValue("37", VitalsMeasurementType.BODY_TEMPERATURE, UnitSystem.METRIC)!!,
            0.0,
        )
        assertEquals(
            37.0,
            canonicalVitalsValue(
                "98.6",
                VitalsMeasurementType.BODY_TEMPERATURE,
                UnitSystem.IMPERIAL,
            )!!,
            1e-9,
        )
        assertEquals(
            0.0,
            canonicalVitalsValue(
                "32",
                VitalsMeasurementType.BODY_TEMPERATURE,
                UnitSystem.IMPERIAL,
            )!!,
            1e-9,
        )
    }

    private fun formatter(unitSystem: UnitSystem): UnitFormatter =
        UnitFormatter(
            unitSystemProvider = { unitSystem },
            localeProvider = { Locale.US },
        )
}
