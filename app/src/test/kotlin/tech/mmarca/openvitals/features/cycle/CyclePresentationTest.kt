package tech.mmarca.openvitals.features.cycle

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
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
                )
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
        assertEquals("Mouth", measurementLocationLabel(4))
        assertEquals("Vagina", measurementLocationLabel(10))
        assertEquals("Measurement location unknown", measurementLocationLabel(-1))
    }

    private fun instant(value: String): Instant = Instant.parse(value)
}
