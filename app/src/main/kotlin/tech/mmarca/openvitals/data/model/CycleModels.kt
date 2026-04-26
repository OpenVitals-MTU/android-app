package tech.mmarca.openvitals.data.model

import java.time.Instant

data class CycleData(
    val menstruationFlows: List<MenstruationFlowEntry> = emptyList(),
    val menstruationPeriods: List<MenstruationPeriodEntry> = emptyList(),
    val ovulationTests: List<OvulationTestEntry> = emptyList(),
    val cervicalMucus: List<CervicalMucusEntry> = emptyList(),
    val basalBodyTemperature: List<BasalBodyTemperatureEntry> = emptyList(),
) {
    val hasData: Boolean
        get() = menstruationFlows.isNotEmpty() ||
            menstruationPeriods.isNotEmpty() ||
            ovulationTests.isNotEmpty() ||
            cervicalMucus.isNotEmpty() ||
            basalBodyTemperature.isNotEmpty()
}

data class MenstruationFlowEntry(
    val time: Instant,
    val flow: Int,
    val source: String,
)

data class MenstruationPeriodEntry(
    val startTime: Instant,
    val endTime: Instant,
    val source: String,
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()
}

data class OvulationTestEntry(
    val time: Instant,
    val result: Int,
    val source: String,
)

data class CervicalMucusEntry(
    val time: Instant,
    val appearance: Int,
    val sensation: Int,
    val source: String,
)

data class BasalBodyTemperatureEntry(
    val time: Instant,
    val temperatureCelsius: Double,
    val measurementLocation: Int,
    val source: String,
)
