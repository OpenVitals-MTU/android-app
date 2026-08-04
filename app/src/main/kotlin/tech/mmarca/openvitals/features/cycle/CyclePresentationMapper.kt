package tech.mmarca.openvitals.features.cycle

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.domain.model.CycleData
import java.time.LocalDate
import java.time.ZoneId

object CyclePresentationMapper {

    fun build(
        query: PeriodLoadQuery,
        data: CycleData,
    ): CycleDisplayState {
        val selectedPeriod = displayPeriodFor(
            range = query.range,
            anchorDate = query.selectedDate,
            weekPeriodMode = query.weekPeriodMode,
        )
        val zone = ZoneId.systemDefault()
        val calendarDays = cycleDays(selectedPeriod, data, zone)
        val periodDays = calendarDays.count { day ->
            day.inSelectedPeriod && (day.periodActive || day.flows.isNotEmpty())
        }
        val trackedDates = data.trackedDates(zone)
        val latestBbt = data.basalBodyTemperature.maxByOrNull { it.time }

        return CycleDisplayState(
            selectedPeriod = selectedPeriod,
            hasData = data.hasData,
            summary = CyclePeriodSummary(
                periodDays = periodDays,
                ovulationTestCount = data.ovulationTests.size,
                bbtReadingCount = data.basalBodyTemperature.size,
                totalEntryCount = data.entryCount(),
                latestBbtCelsius = latestBbt?.temperatureCelsius,
                latestBbtMeasurementLocation = latestBbt?.measurementLocation ?: 0,
            ),
            calendarDays = calendarDays,
            trackedDates = trackedDates,
            sampleCount = data.entryCount(),
            sources = data.allSources(),
        )
    }
}

private fun CycleData.trackedDates(zone: ZoneId): List<LocalDate> =
    menstruationFlows.map { it.time.atZone(zone).toLocalDate() } +
        menstruationPeriods.map { it.startTime.atZone(zone).toLocalDate() } +
        ovulationTests.map { it.time.atZone(zone).toLocalDate() } +
        cervicalMucus.map { it.time.atZone(zone).toLocalDate() } +
        basalBodyTemperature.map { it.time.atZone(zone).toLocalDate() } +
        intermenstrualBleeding.map { it.time.atZone(zone).toLocalDate() } +
        sexualActivity.map { it.time.atZone(zone).toLocalDate() }

private fun CycleData.entryCount(): Int =
    menstruationFlows.size +
        menstruationPeriods.size +
        ovulationTests.size +
        cervicalMucus.size +
        basalBodyTemperature.size +
        intermenstrualBleeding.size +
        sexualActivity.size

private fun CycleData.allSources(): List<String> =
    menstruationFlows.map { it.source } +
        menstruationPeriods.map { it.source } +
        ovulationTests.map { it.source } +
        cervicalMucus.map { it.source } +
        basalBodyTemperature.map { it.source } +
        intermenstrualBleeding.map { it.source } +
        sexualActivity.map { it.source }
