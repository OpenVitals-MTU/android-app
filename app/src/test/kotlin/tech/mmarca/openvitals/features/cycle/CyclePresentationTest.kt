package tech.mmarca.openvitals.features.cycle

import android.content.res.Resources
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.data.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.data.model.CervicalMucusEntry
import tech.mmarca.openvitals.data.model.CycleData
import tech.mmarca.openvitals.data.model.MenstruationFlowEntry
import tech.mmarca.openvitals.data.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.data.model.OvulationTestEntry

class CyclePresentationTest {

    @Test fun `cycleDays builds a Monday to Sunday grid and attaches day data`() {
        val data = CycleData(
            menstruationPeriods = listOf(
                MenstruationPeriodEntry(
                    startTime = instant("2026-04-10T00:00:00Z"),
                    endTime = instant("2026-04-13T00:00:00Z"),
                    source = "period",
                )
            ),
            menstruationFlows = listOf(
                MenstruationFlowEntry(
                    time = instant("2026-04-11T10:00:00Z"),
                    flow = FLOW_MEDIUM,
                    source = "flow",
                )
            ),
            ovulationTests = listOf(
                OvulationTestEntry(
                    time = instant("2026-04-12T10:00:00Z"),
                    result = 1,
                    source = "ovulation",
                )
            ),
            basalBodyTemperature = listOf(
                BasalBodyTemperatureEntry(
                    time = instant("2026-04-11T08:00:00Z"),
                    temperatureCelsius = 36.4,
                    measurementLocation = 4,
                    source = "old",
                ),
                BasalBodyTemperatureEntry(
                    time = instant("2026-04-11T09:00:00Z"),
                    temperatureCelsius = 36.7,
                    measurementLocation = 4,
                    source = "new",
                ),
            ),
        )

        val days = cycleDays(
            period = DatePeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)),
            data = data,
            zone = ZoneOffset.UTC,
        )

        assertEquals(LocalDate.of(2026, 3, 30), days.first().date)
        assertEquals(LocalDate.of(2026, 5, 3), days.last().date)
        assertEquals(35, days.size)
        assertFalse(days.first().inSelectedPeriod)

        val april11 = days.single { it.date == LocalDate.of(2026, 4, 11) }
        assertTrue(april11.inSelectedPeriod)
        assertTrue(april11.periodActive)
        assertEquals(FLOW_MEDIUM, april11.flows.single().flow)
        assertEquals(36.7, april11.basalBodyTemperature?.temperatureCelsius ?: 0.0, 0.0)

        val april12 = days.single { it.date == LocalDate.of(2026, 4, 12) }
        assertTrue(april12.periodActive)
        assertEquals(1, april12.ovulationTests.single().result)
    }

    @Test fun `observationsFor maps cycle records to sorted display observations`() {
        val previousTimeZone = TimeZone.getDefault()
        val resources = resourcesForCycleStrings()
        val observations = try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            observationsFor(
                CycleData(
                    menstruationPeriods = listOf(
                        MenstruationPeriodEntry(
                            startTime = instant("2026-04-10T00:00:00Z"),
                            endTime = instant("2026-04-12T00:00:00Z"),
                            source = "period",
                        )
                    ),
                    menstruationFlows = listOf(
                        MenstruationFlowEntry(
                            time = instant("2026-04-13T10:00:00Z"),
                            flow = FLOW_HEAVY,
                            source = "flow",
                        )
                    ),
                    ovulationTests = listOf(
                        OvulationTestEntry(
                            time = instant("2026-04-14T10:00:00Z"),
                            result = 1,
                            source = "ovulation",
                        )
                    ),
                    cervicalMucus = listOf(
                        CervicalMucusEntry(
                            time = instant("2026-04-15T10:00:00Z"),
                            appearance = 5,
                            sensation = 3,
                            source = "mucus",
                        )
                    ),
                ),
                resources = resources,
            )
        } finally {
            TimeZone.setDefault(previousTimeZone)
        }

        assertEquals(
            listOf("Cervical mucus", "Ovulation test", "Menstruation flow", "Menstruation period"),
            observations.map { it.title },
        )
        assertEquals("Egg white, heavy", observations[0].value)
        assertEquals("Positive", observations[1].value)
        assertEquals("Heavy", observations[2].value)
        assertEquals("2 days", observations[3].value)
    }

    @Test fun `measurementLocationLabel maps known locations and fallback`() {
        assertEquals(R.string.measurement_location_mouth, measurementLocationLabelRes(4))
        assertEquals(R.string.measurement_location_vagina, measurementLocationLabelRes(10))
        assertEquals(R.string.measurement_location_unknown, measurementLocationLabelRes(-1))
    }

    private fun instant(value: String): Instant = Instant.parse(value)

    private fun resourcesForCycleStrings(): Resources {
        val resources = mockk<Resources>()
        every { resources.getString(R.string.cycle_observation_menstruation_period) } returns "Menstruation period"
        every { resources.getString(R.string.cycle_observation_menstruation_flow) } returns "Menstruation flow"
        every { resources.getString(R.string.cycle_observation_ovulation_test) } returns "Ovulation test"
        every { resources.getString(R.string.cycle_observation_cervical_mucus) } returns "Cervical mucus"
        every { resources.getString(R.string.cycle_flow_heavy) } returns "Heavy"
        every { resources.getString(R.string.cycle_ovulation_positive) } returns "Positive"
        every { resources.getString(R.string.cycle_mucus_egg_white) } returns "Egg white"
        every { resources.getString(R.string.cycle_mucus_heavy) } returns "heavy"
        every { resources.getString(R.string.cycle_day_plural) } returns "days"
        every { resources.getString(R.string.cycle_mucus_value, "Egg white", "heavy") } returns "Egg white, heavy"
        every { resources.getString(R.string.cycle_days_value, 2L, "days") } returns "2 days"
        return resources
    }
}
